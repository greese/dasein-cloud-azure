/**
 * Copyright (C) 2012 enStratus Networks Inc
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.azure.compute.image;

import org.apache.log4j.Logger;
import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.AzureService;
import org.dasein.cloud.azure.compute.image.model.OSImageModel;
import org.dasein.cloud.azure.compute.image.model.OSImagesModel;
import org.dasein.cloud.azure.compute.image.model.VMImageModel;
import org.dasein.cloud.azure.compute.image.model.VMImagesModel;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.azure.compute.vm.model.Operation;
import org.dasein.cloud.compute.AbstractImageSupport;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageCapabilities;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.Jiterator;
import org.dasein.util.JiteratorPopulator;
import org.dasein.util.PopulatorThread;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.util.*;

/**
 * Implements support for Azure OS images through the Dasein Cloud machine image API.
 * @author George Reese (george.reese@imaginary.com)
 * @since 2012.04.1
 * @version 2012.04.1
 */
public class AzureOSImage extends AbstractImageSupport {
    static private final Logger logger = Azure.getLogger(AzureOSImage.class);

    static private final String IMAGES = "/services/images";
    static private final String RESOURCE_VMIMAGES = "/services/vmimages";
    static private final String MICROSOFT = "--microsoft--";

    private Azure provider;
    
    public AzureOSImage(Azure provider) {
        super(provider);
        this.provider = provider;
    }

    @Override
    public void bundleVirtualMachineAsync(@Nonnull String virtualMachineId, @Nonnull MachineImageFormat format, @Nonnull String bucket, @Nonnull String name, @Nonnull AsynchronousTask<String> trackingTask) throws CloudException, InternalException {
        throw new OperationNotSupportedException("No ability to bundle vms");
    }

    private transient volatile OSImageCapabilities capabilities;
    @Override
    public ImageCapabilities getCapabilities() throws CloudException, InternalException {
        if( capabilities == null ) {
            capabilities = new OSImageCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    protected @Nonnull MachineImage capture(@Nonnull ImageCreateOptions options, @Nullable AsynchronousTask<MachineImage> task) throws CloudException, InternalException {
        logger.debug("Capture image of "+options.getVirtualMachineId()+" with new name "+options.getName());
        try {
            if( task != null ) {
                task.setStartTime(System.currentTimeMillis());
            }

            String vmid = options.getVirtualMachineId();
            String name = options.getName();

            VirtualMachine vm;

            vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(options.getVirtualMachineId());
            if (vm == null) {
                throw new CloudException("Virtual machine not found: " + options.getVirtualMachineId());
            }
            if (!vm.getCurrentState().equals(VmState.STOPPED)) {
                logger.debug("Stopping server");
                provider.getComputeServices().getVirtualMachineSupport().stop(vmid, false);
                try {
                    long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);
                    vm = null;
                    while (timeout > System.currentTimeMillis()) {
                        vm =  provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(options.getVirtualMachineId());
                        if (vm.getCurrentState().equals(VmState.STOPPED)) {
                            logger.debug("Server stopped");
                            break;
                        }
                        try { Thread.sleep(15000L); }
                        catch( InterruptedException ignore ) { }
                    }
                }
                catch (Throwable ignore) {
                }
                if (!vm.getCurrentState().equals(VmState.STOPPED)) {
                   throw new CloudException("Server still not stopped after 10 minutes.  Please try again later");
                }
            }
            try {
                ProviderContext ctx = provider.getContext();

                if( ctx == null ) {
                    throw new AzureConfigException("No context was set for this request");
                }

                Operation.CaptureRoleAsVMImageOperation captureVMImageOperation = new Operation.CaptureRoleAsVMImageOperation();
                captureVMImageOperation.setOsState("Specialized");
                captureVMImageOperation.setVmImageName(name);
                captureVMImageOperation.setVmImageLabel(name);

                String operationUrl = String.format(AzureVM.OPERATIONS_RESOURCES, vm.getTag("serviceName").toString(),
                        vm.getTag("deploymentName").toString(), vm.getTag("roleName").toString());
                AzureMethod method = new AzureMethod(provider);
                try {
                    method.post(operationUrl, captureVMImageOperation);
                }
                catch (JAXBException e)
                {
                    logger.error(e.getMessage());
                    throw new InternalException(e);
                }

                MachineImage img = null;
                try {
                    long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);
                    while (timeout > System.currentTimeMillis()) {
                        img = getImage(name);
                        if (img != null) {
                            logger.debug("Found image "+name);
                            break;
                        }
                        try { Thread.sleep(15000L); }
                        catch( InterruptedException ignore ) { }
                    }
                }
                catch (Throwable ignore) {
                }

                if (img == null) {
                    throw new CloudException("Drive cloning completed, but no ID was provided for clone");
                }if( task != null ) {
                    task.completeWithResult(img);
                }

                //restart VM when imaging complete
                provider.getComputeServices().getVirtualMachineSupport().start(vmid);

                return img;
            }
            finally {
                if( logger.isTraceEnabled() ) {
                    logger.trace("EXIT: " + AzureOSImage.class.getName() + ".launch()");
                }
            }
        } finally {
            provider.release();
        }
    }

    @Override
    public MachineImage getImage(@Nonnull String machineImageId) throws CloudException, InternalException {
        if(machineImageId == null)
            throw new InternalException("The parameter machineImageId cannot be null");

        final Iterable<MachineImage> allImages = getAllImages();
        for( MachineImage img : allImages ) {
            if( machineImageId.equals(img.getProviderMachineImageId()) ) {
                logger.debug("Found image i'm looking for "+machineImageId);
                img.setImageClass(ImageClass.MACHINE);
                return img;
            }
        }
        return null;
    }

    @Override
    public @Nonnull String getProviderTermForImage(@Nonnull Locale locale) {
        return "OS image";
    }

    @Nonnull
    @Override
    public String getProviderTermForImage(@Nonnull Locale locale, @Nonnull ImageClass cls) {
        return "OS image";
    }

    @Nonnull
    @Override
    public String getProviderTermForCustomImage(@Nonnull Locale locale, @Nonnull ImageClass cls) {
        return "OS image";
    }

    @Override
    public boolean hasPublicLibrary() {
        return true;
    }

    @Nonnull
    @Override
    public Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public boolean isImageSharedWithPublic(@Nonnull String machineImageId) throws CloudException, InternalException {
        MachineImage img = getMachineImage(machineImageId);
        
        return (img != null &&
                (MICROSOFT.equals(img.getProviderOwnerId())
                    || "--public--".equals(img.getProviderOwnerId())
                    || "--Canonical--".equals(img.getProviderOwnerId())
                    || "--RightScaleLinux--".equals(img.getProviderOwnerId())
                    || "--RightScaleWindows--".equals(img.getProviderOwnerId())
                    || "--OpenLogic--".equals(img.getProviderOwnerId())
                    || "--SUSE--".equals(img.getProviderOwnerId())
                )
                );
    }

    private boolean isImageSharedWithPublic(@Nonnull MachineImage img) {
        return (img != null && !getProvider().getContext().getAccountNumber().equals(img.getProviderOwnerId()));
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return provider.getDataCenterServices().isSubscribed(AzureService.COMPUTE);
    }

    @Nonnull
    @Override
    public Iterable<ResourceStatus> listImageStatus(@Nonnull ImageClass cls) throws CloudException, InternalException {
        if (!cls.equals(ImageClass.MACHINE) ) {
             return Collections.emptyList();
        }

        final ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        ArrayList<ResourceStatus> list = new ArrayList<ResourceStatus>();

        final Iterable<MachineImage> allImages = getAllImages();
        for( MachineImage image : allImages)
        {
            if("user".equalsIgnoreCase(image.getProviderOwnerId().toLowerCase()) && ctx.getRegionId().equalsIgnoreCase(image.getProviderRegionId().toLowerCase()))
            {
                list.add(new ResourceStatus(image.getProviderMachineImageId(), MachineImageState.ACTIVE));
            }
        }
        return list;
    }

    @Nonnull
    @Override
    public Iterable<MachineImage> listImages(@Nullable ImageFilterOptions imageFilterOptions) throws CloudException, InternalException {
        if (!imageFilterOptions.getImageClass().equals(ImageClass.MACHINE)) {
            return Collections.emptyList();
        }

        final ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        ArrayList<MachineImage> images = new ArrayList<MachineImage>();

        final Iterable<MachineImage> allImages = getAllImages();

        for (MachineImage image : allImages)
        {
            if (image != null) {
                image.setImageClass(ImageClass.MACHINE);

                if (imageFilterOptions.matches(image)) {
                    if (imageFilterOptions.getAccountNumber() == null) {
                        if (ctx.getAccountNumber().equals(image.getProviderOwnerId())) {
                            images.add(image);
                        }
                    }
                    else if (image.getProviderOwnerId().equalsIgnoreCase(imageFilterOptions.getAccountNumber())) {
                        images.add(image);
                    }
                }
            }
        }
        return images;
    }

    @Nonnull
    @Override
    public Iterable<MachineImage> listImages(@Nonnull ImageClass cls) throws CloudException, InternalException {
        if (!cls.equals(ImageClass.MACHINE)) {
            return Collections.emptyList();
        }

        ProviderContext ctx = provider.getContext();

        String me = ctx.getAccountNumber();
        ArrayList<MachineImage> allImages = listMachineImages();

        ArrayList<MachineImage> list = new ArrayList<MachineImage>();

        for (MachineImage img : allImages) {
            if (img.getProviderOwnerId().equalsIgnoreCase(me)) {
                img.setImageClass(ImageClass.MACHINE);
                list.add(img);
            }
        }
        return list;
    }

    @Nonnull
    @Override
    public Iterable<MachineImage> listImages(@Nonnull ImageClass cls, @Nonnull String ownedBy) throws CloudException, InternalException {
        if (!cls.equals(ImageClass.MACHINE)) {
            return Collections.emptyList();
        }

        ProviderContext ctx = provider.getContext();

        ArrayList<MachineImage> allImages = listMachineImages();

        ArrayList<MachineImage> list = new ArrayList<MachineImage>();

        for (MachineImage img : allImages) {
            if (img.getProviderOwnerId().equalsIgnoreCase(ownedBy)) {
                img.setImageClass(ImageClass.MACHINE);
                list.add(img);
            }
        }
        return list;
    }

    @Override
    public @Nonnull ArrayList<MachineImage> listMachineImages() throws CloudException, InternalException {
        final ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        ArrayList<MachineImage> list = new ArrayList<MachineImage>();

        final Iterable<MachineImage> allImages = getAllImages();
        for (MachineImage image : allImages)
        {
            if( image != null ) {
                if( ctx.getAccountNumber().equalsIgnoreCase(image.getProviderOwnerId())) {
                    list.add(image);
                }
            }
        }
        return list;
    }

    @Override
    public @Nonnull Iterable<MachineImage> listMachineImagesOwnedBy(String accountId) throws CloudException, InternalException {
        ArrayList<MachineImage> images = new ArrayList<MachineImage>();
        final Iterable<MachineImage> allImages = getAllImages();
        for (MachineImage image : allImages) {
            if (accountId != null && accountId.equalsIgnoreCase(image.getProviderOwnerId())) {
                images.add(image);
            }
        }
        return images;
    }

    @Override
    public @Nonnull Iterable<String> listShares(@Nonnull String forMachineImageId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public MachineImage registerImageBundle(@Nonnull ImageCreateOptions options) throws CloudException, InternalException {
        throw new OperationNotSupportedException("No image registering is currently supported");
    }

    @Override
    public void remove(@Nonnull String machineImageId) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureOSImage.class.getName() + ".remove(" + machineImageId + ")");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }

            MachineImage image = getMachineImage(machineImageId);

            if( image == null ) {
                throw new CloudException("No such machine image: " + machineImageId);
            }

            AzureMethod method = new AzureMethod(provider);

            //dmayne 20130425: delete image blob too
            method.invoke("DELETE",ctx.getAccountNumber(), IMAGES + "/" + machineImageId+"?comp=media", null);
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureOSImage.class.getName() + ".launch()");
            }
        }
    }

    @Override
    public void remove(@Nonnull String providerImageId, boolean checkState) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeAllImageShares(@Nonnull String providerImageId) throws CloudException, InternalException {
        //No-OP
    }

    @Override
    public void removeImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
        throw new OperationNotSupportedException("No ability to share images");
    }

    @Override
    public void removePublicShare(@Nonnull String providerImageId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("No ability to share images");
    }

    @Override
    public @Nonnull Iterable<MachineImage> searchMachineImages(@Nullable String keyword, @Nullable Platform platform, @Nullable Architecture architecture) throws CloudException, InternalException {
        ArrayList<MachineImage> images = new ArrayList<MachineImage>();

        final Iterable<MachineImage> allImages = getAllImages();
        for( MachineImage img : allImages) {
            if( architecture != null ) {
                if( !architecture.equals(img.getArchitecture()) ) {
                    continue;
                }
            }
            if( platform != null && !platform.equals(Platform.UNKNOWN) ) {
                Platform p = img.getPlatform();

                if( p.equals(Platform.UNKNOWN) ) {
                    continue;
                }
                else if( platform.isWindows() ) {
                    if( !p.isWindows() ) {
                        continue;
                    }
                }
                else if( platform.equals(Platform.UNIX) ) {
                    if( !p.isUnix() ) {
                        continue;
                    }
                }
                else if( !platform.equals(p) ) {
                    continue;
                }
            }
            if( keyword != null && !keyword.isEmpty()) {
                if( !img.getName().matches(keyword) ) {
                    if( !img.getDescription().matches(keyword) ) {
                        if( !img.getProviderMachineImageId().matches(keyword) ) {
                            continue;
                        }
                    }
                }
            }
            images.add(img);
        }
        return images;
    }

    @Nonnull
    @Override
    public Iterable<MachineImage> searchPublicImages(@Nonnull ImageFilterOptions imageFilterOptions) throws InternalException, CloudException {
        Platform platform = imageFilterOptions.getPlatform();
        ImageClass cls = imageFilterOptions.getImageClass();
        return searchPublicImages(null, platform, null, cls);
    }

    @Nonnull
    @Override
    public Iterable<MachineImage> searchImages(@Nullable String accountNumber, @Nullable String keyword, @Nullable Platform platform, @Nullable Architecture architecture, @Nullable ImageClass... imageClasses) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public Iterable<MachineImage> searchPublicImages(@Nullable String keyword, @Nullable Platform platform, @Nullable Architecture architecture, @Nullable ImageClass... imageClasses) throws CloudException, InternalException {
        ArrayList<MachineImage> list = new ArrayList<MachineImage>();
        if (imageClasses.length < 1) {
            // return all images
            Iterable<MachineImage> images = searchMachineImages(keyword, platform, architecture);
            for (MachineImage img : images) {
                if (isImageSharedWithPublic(img)) {
                    list.add(img);
                }
            }
        }
        else {
            for (ImageClass cls : imageClasses) {
                if (cls.equals(ImageClass.MACHINE)) {
                    Iterable<MachineImage> images = searchMachineImages(keyword, platform, architecture);
                    for (MachineImage img : images) {
                        if (isImageSharedWithPublic(img)) {
                            list.add(img);
                        }
                    }
                }
            }
        }
        return list;
    }

    @Override
    public boolean supportsCustomImages() {
        return true;
    }

    @Override
    public void updateTags(@Nonnull String imageId, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateTags(@Nonnull String[] strings, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeTags(@Nonnull String s, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeTags(@Nonnull String[] strings, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }

    private Iterable<MachineImage> getAllImages() throws CloudException, InternalException
    {
        ArrayList<MachineImage> images = new ArrayList<MachineImage>();

        ArrayList<MachineImage> osImages = getOSImages();
        images.addAll(osImages);

        ArrayList<MachineImage> vmImages = getVMImages();
        images.addAll(vmImages);

        return images;
    }

    private String getCategory(String category) throws CloudException, InternalException
    {
        if(category == null)
            return null;

        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        if (category.equalsIgnoreCase("user")) {
            return ctx.getAccountNumber();
        } else if (category.toLowerCase().contains("microsoft")){
            return MICROSOFT;
        } else if( category.toLowerCase().contains("partner") ) {
            return "--public--";
        } else if( category.toLowerCase().contains("canonical") ) {
            return "--Canonical--";
        } else if( category.toLowerCase().contains("rightscale with linux") ) {
            return "--RightScaleLinux--";
        } else if( category.toLowerCase().contains("rightscale with windows") ) {
            return "--RightScaleWindows--";
        } else if( category.toLowerCase().contains("openlogic") ) {
            return "--OpenLogic--";
        } else if( category.toLowerCase().contains("suse") ) {
            return "--SUSE--";
        }

        return null;
    }

    private ArrayList<MachineImage> getOSImages() throws CloudException, InternalException
    {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        ArrayList<MachineImage> images = new ArrayList<MachineImage>();
        AzureMethod azureMethod = new AzureMethod(provider);
        OSImagesModel osImagesModel = azureMethod.get(OSImagesModel.class, IMAGES);

        for(OSImageModel osImageModel : osImagesModel.getImages())
        {
            if(osImageModel.getLocation() != null && Arrays.asList(osImageModel.getLocation().trim().toLowerCase().split(";")).contains(ctx.getRegionId().toLowerCase())) {
                AzureMachineImage azureMachineImage = new AzureMachineImage();
                azureMachineImage.setCurrentState(MachineImageState.ACTIVE);
                azureMachineImage.setProviderRegionId(ctx.getRegionId());
                azureMachineImage.setArchitecture(Architecture.I64);
                azureMachineImage.setProviderMachineImageId(osImageModel.getName());
                azureMachineImage.setProviderOwnerId(getCategory(osImageModel.getCategory()));
                azureMachineImage.setName(osImageModel.getLabel());
                azureMachineImage.setDescription(osImageModel.getDescription());
                azureMachineImage.setMediaLink(osImageModel.getMediaLink());
                azureMachineImage.setPlatform(osImageModel.getOs().equalsIgnoreCase("windows") ? Platform.WINDOWS : Platform.UNIX);
                azureMachineImage.setTags(new HashMap<String,String>());
                azureMachineImage.setType(MachineImageType.VOLUME);

                if( azureMachineImage.getName() == null ) {
                    azureMachineImage.setName(azureMachineImage.getProviderMachineImageId());
                }
                else {
                    int versionIdx = azureMachineImage.getProviderMachineImageId().indexOf("__");
                    if(versionIdx > 0)
                    {
                        String fullName = null;
                        try {
                            fullName = azureMachineImage.getProviderMachineImageId().substring(versionIdx + 2);
                        } catch (Throwable ignore) {
                        }
                        if (fullName != null) {
                            azureMachineImage.setName(fullName);
                        }
                    }
                }
                if( azureMachineImage.getDescription() == null ) {
                    azureMachineImage.setDescription(azureMachineImage.getName());
                }
                String descriptor = azureMachineImage.getProviderMachineImageId() + " " + azureMachineImage.getName() + " " + azureMachineImage.getDescription();

                if( azureMachineImage.getPlatform() == null || azureMachineImage.getPlatform().equals(Platform.UNIX) ) {
                    Platform p = Platform.guess(descriptor);

                    if( azureMachineImage.getPlatform() == null || !Platform.UNKNOWN.equals(p) ) {
                        azureMachineImage.setPlatform(p);
                    }
                }
                azureMachineImage.setSoftware(descriptor.contains("SQL Server") ? "SQL Server" : "");
                azureMachineImage.setAzureImageType("OSImage");
                images.add(azureMachineImage);
            }
        }

        return images;
    }

    private ArrayList<MachineImage> getVMImages() throws CloudException, InternalException
    {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        ArrayList<MachineImage> images = new ArrayList<MachineImage>();
        AzureMethod azureMethod = new AzureMethod(provider);
        VMImagesModel vmImagesModel = azureMethod.get(VMImagesModel.class, RESOURCE_VMIMAGES);

        for (VMImageModel vmImageModel : vmImagesModel.getVmImages())
        {
            AzureMachineImage azureMachineImage = new AzureMachineImage();
            azureMachineImage.setProviderOwnerId(getCategory(vmImageModel.getCategory()));
            azureMachineImage.setProviderRegionId(vmImageModel.getLocation());
            azureMachineImage.setProviderMachineImageId(vmImageModel.getName());
            azureMachineImage.setName(vmImageModel.getLabel());
            azureMachineImage.setDescription(vmImageModel.getDescription());
            azureMachineImage.setArchitecture(Architecture.I64);
            azureMachineImage.setPlatform(vmImageModel.getOsDiskConfiguration().getOs().equalsIgnoreCase("windows") ? Platform.WINDOWS : Platform.UNIX);
            azureMachineImage.setCurrentState(MachineImageState.ACTIVE);
            azureMachineImage.setImageClass(ImageClass.MACHINE);
            azureMachineImage.setType(MachineImageType.VOLUME);
            azureMachineImage.setDescription(vmImageModel.getDescription() != null ? vmImageModel.getDescription() : vmImageModel.getName() );

            if(vmImageModel.getOsDiskConfiguration().getMediaLink() != null)
                azureMachineImage.setMediaLink(vmImageModel.getOsDiskConfiguration().getMediaLink());

            String descriptor = azureMachineImage.getProviderMachineImageId() + " " + azureMachineImage.getName() + " " + azureMachineImage.getDescription();
            azureMachineImage.setSoftware(descriptor.contains("SQL Server") ? "SQL Server" : "");

            azureMachineImage.setAzureImageType("VMImage");
            azureMachineImage.setMediaLink(vmImageModel.getOsDiskConfiguration().getMediaLink());

            if(ctx.getRegionId().equalsIgnoreCase(azureMachineImage.getProviderRegionId()))
                images.add(azureMachineImage);
        }

        return images;
    }
}
