package org.dasein.cloud.azure.compute.image;

import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.AzureService;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.Platform;
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
    static private final String IMAGES = "/services/images";
    
    private Azure provider;
    
    public AzureOSImage(Azure provider) { this.provider = provider; }
    
    @Override
    public void downloadImage(@Nonnull String machineImageId, @Nonnull OutputStream toOutput) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Not currently supported");
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
                    populateImages(ctx, iterator, "--microsoft--", ctx.getAccountNumber(), "--public--");
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

    @Override
    public boolean hasPublicLibrary() {
        return true;
    }

    @Override
    public @Nonnull AsynchronousTask<String> imageVirtualMachine(String vmId, String name, String description) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Not currently supported");
    }

    @Override
    public @Nonnull AsynchronousTask<String> imageVirtualMachineToStorage(String vmId, String name, String description, String directory) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Not currently supported");
    }

    @Override
    public @Nonnull String installImageFromUpload(@Nonnull MachineImageFormat format, @Nonnull InputStream imageStream) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Not currently supported");
    }

    @Override
    public boolean isImageSharedWithPublic(@Nonnull String machineImageId) throws CloudException, InternalException {
        MachineImage img = getMachineImage(machineImageId);
        
        return (img != null && ("--microsoft--".equals(img.getProviderOwnerId()) || "--public--".equals(img.getProviderOwnerId())) );
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return provider.getDataCenterServices().isSubscribed(AzureService.COMPUTE);
    }

    @Override
    public @Nonnull Iterable<MachineImage> listMachineImages() throws CloudException, InternalException {
        final ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        PopulatorThread<MachineImage> populator;

        provider.hold();
        populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws CloudException, InternalException {
                try {
                    populateImages(ctx, iterator);
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
        final ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        PopulatorThread<MachineImage> populator;
        final String acct = accountId;

        provider.hold();
        populator = new PopulatorThread<MachineImage>(new JiteratorPopulator<MachineImage>() {
            public void populate(@Nonnull Jiterator<MachineImage> iterator) throws CloudException, InternalException {
                try {
                    populateImages(ctx, iterator, acct);
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

    @Override
    public @Nonnull Iterable<String> listShares(@Nonnull String forMachineImageId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    private void populateImages(@Nonnull ProviderContext ctx, @Nonnull Jiterator<MachineImage> iterator, @Nullable String ... accounts) throws CloudException, InternalException {
        if( accounts == null ) {
            accounts = new String[] { ctx.getAccountNumber() };
        }
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
                for( String accountNumber : accounts ) {
                    if( accountNumber.equalsIgnoreCase(image.getProviderOwnerId()) ) {
                        iterator.push(image);
                        break;
                    }
                }
            }

        }
    }
    
    @Override
    public @Nonnull String registerMachineImage(String atStorageLocation) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image registration is not required in Azure");
    }

    @Override
    public void remove(@Nonnull String machineImageId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Not yet supported"); // TODO: implement me
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
                    populateImages(ctx, iterator, "--microsoft--", ctx.getAccountNumber(), "--public--");
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

    @Override
    public void shareMachineImage(@Nonnull String machineImageId, @Nonnull String withAccountId, boolean allow) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Image sharing is not supported in Azure");
    }

    @Override
    public boolean supportsCustomImages() {
        return false; 
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
    public @Nonnull String transfer(@Nonnull CloudProvider fromCloud, @Nonnull String machineImageId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("You cannot transfer Azure images");
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0]; // not applicable in Azure
    }
    
    private @Nullable AzureMachineImage toImage(@Nonnull ProviderContext ctx, @Nullable Node entry) throws CloudException, InternalException {
        if( entry == null ) {
            return null;
        }
        AzureMachineImage image= new AzureMachineImage();

        image.setCurrentState(MachineImageState.ACTIVE);
        image.setProviderRegionId(ctx.getRegionId());
        image.setProviderOwnerId("--microsoft--");
        image.setArchitecture(Architecture.I64);

        NodeList attributes = entry.getChildNodes();

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
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
                    image.setProviderOwnerId("--microsoft--");
                }
                else if( "partner".equalsIgnoreCase(c) ) {
                    image.setProviderOwnerId("--public--");
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
        image.setTags(new HashMap<String, String>());
        image.setType(MachineImageType.STORAGE);
        return image;
    }
}
