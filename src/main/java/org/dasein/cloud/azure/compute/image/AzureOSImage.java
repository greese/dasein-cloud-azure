package org.dasein.cloud.azure.compute.image;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
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
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.util.Jiterator;
import org.dasein.util.JiteratorPopulator;
import org.dasein.util.PopulatorThread;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

/**
 * Implements support for Azure OS images through the Dasein Cloud machine image API.
 * @author George Reese (george.reese@imaginary.com)
 * @since 2012.04.1
 * @version 2012.04.1
 */
public class AzureOSImage implements MachineImageSupport {
    static private final Logger logger = Azure.getLogger(AzureOSImage.class);

    static private final String IMAGES = "/services/images";
    static private final String MICROSOFT = "--microsoft--";

    private Azure provider;
    
    public AzureOSImage(Azure provider) { this.provider = provider; }

    @Override
    public void addImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addPublicShare(@Nonnull String providerImageId) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public String bundleVirtualMachine(@Nonnull String virtualMachineId, @Nonnull MachineImageFormat format, @Nonnull String bucket, @Nonnull String name) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void bundleVirtualMachineAsync(@Nonnull String virtualMachineId, @Nonnull MachineImageFormat format, @Nonnull String bucket, @Nonnull String name, @Nonnull AsynchronousTask<String> trackingTask) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public MachineImage captureImage(@Nonnull ImageCreateOptions options) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void captureImageAsync(@Nonnull ImageCreateOptions options, @Nonnull AsynchronousTask<MachineImage> taskTracker) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MachineImage getImage(@Nonnull String providerImageId) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AzureMachineImage getMachineImage(@Nonnull String machineImageId) throws CloudException, InternalException {
        final ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        PopulatorThread<MachineImage> populator;

        provider.hold();
        populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws CloudException, InternalException {
                try {
                    populateImages(ctx, iterator, MICROSOFT, ctx.getAccountNumber(), "--public--");
                }
                finally {
                    provider.release();
                }
            }
        });
        populator.populate();
        for( MachineImage img : populator.getResult() ) {
            if( machineImageId.equals(img.getProviderMachineImageId()) ) {
                return (AzureMachineImage)img;
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public String getProviderTermForCustomImage(@Nonnull Locale locale, @Nonnull ImageClass cls) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasPublicLibrary() {
        return true;
    }

    @Nonnull
    @Override
    public Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public @Nonnull AsynchronousTask<String> imageVirtualMachine(String vmId, String name, String description) throws CloudException, InternalException {
        @SuppressWarnings("ConstantConditions") final VirtualMachine server = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(vmId);

        if( server == null ) {
            throw new CloudException("No such virtual machine: " + vmId);
        }
        if( !server.getCurrentState().equals(VmState.STOPPED) ) {
            throw new CloudException("The server must be paused in order to create an image.");
        }
        final AsynchronousTask<String> task = new AsynchronousTask<String>();
        final String fname = name;
        final String fdesc = description;
        
        Thread t = new Thread() {
            public void run() {
                try {
                    String imageId = imageVirtualMachine(server, fname, fdesc, task);
                
                    task.completeWithResult(imageId);
                }
                catch( Throwable t ) {
                    task.complete(t);
                }
            }
        };

        t.start();
        return task;
    }

    private String imageVirtualMachine(@Nonnull VirtualMachine vm, @Nonnull String name, @Nonnull String description, @Nonnull AsynchronousTask<String> task) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureOSImage.class.getName() + ".Boot()");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was set for this request");
            }
            String label;

            try {
                label = new String(Base64.encodeBase64(description.getBytes("utf-8")));
            }
            catch( UnsupportedEncodingException e ) {
                throw new InternalException(e);
            }

            String vmId = vm.getProviderVirtualMachineId();
            String[] parts = vmId.split(":");
            String serviceName, roleName;

            if( parts.length == 2 ) {
                serviceName = parts[0];
                roleName = parts[1];
            }
            else {
                serviceName = vmId;
                roleName = vmId;
            }
            String resourceDir = AzureVM.HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  serviceName + "/roleInstances/" + roleName + "/Operations";

            AzureMethod method = new AzureMethod(provider);
            StringBuilder xml = new StringBuilder();

            xml.append("<CaptureRoleOperation xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("<OperationType>CaptureRoleOperation</OperationType>\n");
            xml.append("<PostCaptureAction>Delete</PostCaptureAction>\n");
            xml.append("<TargetImageLabel>").append(label).append("</TargetImageLabel>\n");
            xml.append("<TargetImageName>").append(name).append("</TargetImageName>\n");
            xml.append("</CaptureRoleOperation>\n");

            task.setPercentComplete(2.0);
            method.post(ctx.getAccountNumber(), resourceDir, xml.toString());
            return name;
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureOSImage.class.getName() + ".launch()");
            }
        }
    }
    
    /*
    @Override
    public @Nonnull AsynchronousTask<String> imageVirtualMachineToStorage(String vmId, String name, String description, String directory) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Not currently supported");
    }

    @Override
    public @Nonnull String installImageFromUpload(@Nonnull MachineImageFormat format, @Nonnull InputStream imageStream) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Not currently supported");
    }
    */
    @Override
    public boolean isImageSharedWithPublic(@Nonnull String machineImageId) throws CloudException, InternalException {
        MachineImage img = getMachineImage(machineImageId);
        
        return (img != null && (MICROSOFT.equals(img.getProviderOwnerId()) || "--public--".equals(img.getProviderOwnerId())) );
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return provider.getDataCenterServices().isSubscribed(AzureService.COMPUTE);
    }

    @Nonnull
    @Override
    public Iterable<ResourceStatus> listImageStatus(@Nonnull ImageClass cls) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public Iterable<MachineImage> listImages(@Nonnull ImageClass cls) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public Iterable<MachineImage> listImages(@Nonnull ImageClass cls, @Nonnull String ownedBy) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public @Nonnull Iterable<MachineImage> listMachineImages() throws CloudException, InternalException {
        final ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        PopulatorThread<MachineImage> populator;
        final String owner = ctx.getAccountNumber();

        provider.hold();
        populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws CloudException, InternalException {
                try {
                    populateImages(ctx, iterator,owner);
                }
                finally {
                    provider.release();
                }
            }
        });
        populator.populate();
        return populator.getResult();
    }

    @Override
    public @Nonnull Iterable<MachineImage> listMachineImagesOwnedBy(String accountId) throws CloudException, InternalException {
        final String[] accounts = (accountId == null ? new String[] { MICROSOFT, "--public--"} : new String[] { accountId });
        final ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        PopulatorThread<MachineImage> populator;

        provider.hold();
        populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws CloudException, InternalException {
                try {
                    populateImages(ctx, iterator, accounts);
                }
                finally {
                    provider.release();
                }
            }
        });
        populator.populate();
        return populator.getResult();
    }

    @Override
    public @Nonnull Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
        return Collections.singletonList(MachineImageFormat.AWS); // nonsense, I know
    }

    @Nonnull
    @Override
    public Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public @Nonnull Iterable<String> listShares(@Nonnull String forMachineImageId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public MachineImage registerImageBundle(@Nonnull ImageCreateOptions options) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private void populateImages(@Nonnull ProviderContext ctx, @Nonnull Jiterator<MachineImage> iterator, @Nullable String ... accounts) throws CloudException, InternalException {
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), IMAGES);

        if( doc == null ) {
            throw new CloudException(CloudErrorType.AUTHENTICATION, HttpServletResponse.SC_FORBIDDEN, "Illegal Access", "Illegal access to requested resource");
        }
        NodeList entries = doc.getElementsByTagName("OSImage");

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            AzureMachineImage image = toImage(ctx, entry);

            if( image != null ) {            	
            	if(accounts != null){            		
            		for( String accountNumber : accounts ) {
            			if( accountNumber.equalsIgnoreCase(image.getProviderOwnerId())) {
            				iterator.push(image);
                            break;
            			}
            		}            	
            	}
                else if( image.getProviderOwnerId() == null || MICROSOFT.equals(image.getProviderOwnerId()) || "--public--".equals(image.getProviderOwnerId()) ) {
            		iterator.push(image);
            	}         
            }
        }        
    }

    /*
    @Override
    public @Nonnull String registerMachineImage(String atStorageLocation) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image registration is not required in Azure");
    }
    */

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
            String imageLabel = image.getName();
            StringBuilder xml = new StringBuilder();

            xml.append("<OSImage xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("<Label>").append(imageLabel).append("</Label>");
            xml.append("</OSImage>");

            AzureMethod method = new AzureMethod(provider);

            method.invoke("DELETE",ctx.getAccountNumber(), IMAGES + "/" + machineImageId, xml.toString());
            //TODO need to delete the image disk blob?             
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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeImageShare(@Nonnull String providerImageId, @Nonnull String accountNumber) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removePublicShare(@Nonnull String providerImageId) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public @Nonnull Iterable<MachineImage> searchMachineImages(@Nullable String keyword, @Nullable Platform platform, @Nullable Architecture architecture) throws CloudException, InternalException {
        ArrayList<MachineImage> images = new ArrayList<MachineImage>();

        final ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        PopulatorThread<MachineImage> populator;

        provider.hold();
        populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws CloudException, InternalException {
                try {
                    populateImages(ctx, iterator, MICROSOFT, ctx.getAccountNumber(), "--public--");
                }
                finally {
                    provider.release();
                }
            }
        });
        populator.populate();
        for( MachineImage img : populator.getResult() ) {
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
            if( keyword != null ) {
                if( !img.getName().contains(keyword) ) {
                    if( !img.getDescription().contains(keyword) ) {
                        if( !img.getProviderMachineImageId().contains(keyword) ) {
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
    public Iterable<MachineImage> searchImages(@Nullable String accountNumber, @Nullable String keyword, @Nullable Platform platform, @Nullable Architecture architecture, @Nullable ImageClass... imageClasses) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public Iterable<MachineImage> searchPublicImages(@Nullable String keyword, @Nullable Platform platform, @Nullable Architecture architecture, @Nullable ImageClass... imageClasses) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void shareMachineImage(@Nonnull String machineImageId, @Nonnull String withAccountId, boolean allow) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image sharing is not supported in Azure");
    }

    @Override
    public boolean supportsCustomImages() {
        return true;
    }

    @Override
    public boolean supportsDirectImageUpload() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsImageCapture(@Nonnull MachineImageType type) throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsImageSharing() {
        return false; 
    }

    @Override
    public boolean supportsImageSharingWithPublic() {
        return false;  
    }

    @Override
    public boolean supportsPublicLibrary(@Nonnull ImageClass cls) throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateTags(@Nonnull String imageId, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /*
@Override
public @Nonnull String transfer(@Nonnull CloudProvider fromCloud, @Nonnull String machineImageId) throws CloudException, InternalException {
throw new OperationNotSupportedException("You cannot transfer Azure images");
}
    */
    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }
    
    private @Nullable AzureMachineImage toImage(@Nonnull ProviderContext ctx, @Nullable Node entry) throws CloudException, InternalException {
        if( entry == null ) {
            return null;
        }
        AzureMachineImage image= new AzureMachineImage();
        
        HashMap<String,String> tags = new HashMap<String,String>();

        image.setCurrentState(MachineImageState.ACTIVE);
        image.setProviderRegionId(ctx.getRegionId());
        image.setProviderOwnerId(MICROSOFT);
        image.setArchitecture(Architecture.I64);

        NodeList attributes = entry.getChildNodes();

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;
            String nodeName = attribute.getNodeName();
            
            if( nodeName.equalsIgnoreCase("name") && attribute.hasChildNodes() ) {
                image.setProviderMachineImageId(attribute.getFirstChild().getNodeValue().trim());
            }
            if( nodeName.equalsIgnoreCase("category") && attribute.hasChildNodes() ) {
                String c = attribute.getFirstChild().getNodeValue().trim();
                
                if( "user".equalsIgnoreCase(c) ) {
                    image.setProviderOwnerId(ctx.getAccountNumber());
                }
                else if( "microsoft".equalsIgnoreCase(c) ) {
                    image.setProviderOwnerId(MICROSOFT);
                }
                else if( "partner".equalsIgnoreCase(c) ) {
                    image.setProviderOwnerId("--public--");
                }
                else if( "Canonical".equalsIgnoreCase(c) ) {
                    image.setProviderOwnerId("--Canonical--");
                }
            }
            else if( nodeName.equalsIgnoreCase("label") && attribute.hasChildNodes() ) {
                image.setName(attribute.getFirstChild().getNodeValue().trim());
                
            }
            else if( nodeName.equalsIgnoreCase("description") && attribute.hasChildNodes() ) {
                image.setDescription(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("medialink") && attribute.hasChildNodes() ) {
                image.setMediaLink(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("os") && attribute.hasChildNodes() ) {
                String os = attribute.getFirstChild().getNodeValue().trim();
                
                if( os.equalsIgnoreCase("windows") ) {
                    image.setPlatform(Platform.WINDOWS);
                }
                else if( os.equalsIgnoreCase("linux") ) {
                    image.setPlatform(Platform.UNIX);
                }
            }
        }
        if( image.getProviderMachineImageId() == null ) {
            return null;
        }
        if( image.getName() == null ) {
            image.setName(image.getProviderMachineImageId());
        }
        if( image.getDescription() == null ) {
            image.setDescription(image.getName());
        }
        String descriptor = image.getProviderMachineImageId() + " " + image.getName() + " " + image.getDescription();
        
        if( image.getPlatform() == null || image.getPlatform().equals(Platform.UNIX) ) {
            Platform p = Platform.guess(descriptor);
            
            if( image.getPlatform() == null || !Platform.UNKNOWN.equals(p) ) {
                image.setPlatform(p);
            }
        }
        image.setSoftware(descriptor.contains("SQL Server") ? "SQL Server" : "");
        image.setTags(tags);
        image.setType(MachineImageType.STORAGE);
        return image;
    }
}
