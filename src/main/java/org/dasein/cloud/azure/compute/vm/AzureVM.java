package org.dasein.cloud.azure.compute.vm;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudErrorType;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.Tag;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.AzureService;
import org.dasein.cloud.azure.compute.image.AzureMachineImage;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VmStatistics;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

/**
 * Implements virtual machine support for Microsoft Azure.
 * @author George Reese (george.reese@imaginary.com)
 * @since 2012.04.1
 * @version 2012.04.1
 */
public class AzureVM implements VirtualMachineSupport {
    static private final String HOSTED_SERVICES = "/services/hostedservices";

    private Azure provider;

    public AzureVM(Azure provider) { this.provider = provider; }

    @Override
    public void boot(@Nonnull String vmId) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public @Nonnull VirtualMachine clone(@Nonnull String vmId, @Nonnull String intoDcId, @Nonnull String name, @Nonnull String description, boolean powerOn, @Nullable String... firewallIds) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Not supported in Microsoft Azure");
    }

    @Override
    public void disableAnalytics(String vmId) throws InternalException, CloudException {
        // NO-OP
    }

    @Override
    public void enableAnalytics(String vmId) throws InternalException, CloudException {
        // NO-OP
    }

    @Override
    public @Nonnull String getConsoleOutput(@Nonnull String vmId) throws InternalException, CloudException {
        return "";
    }

    @Override
    public int getMaximumVirtualMachineCount() throws CloudException, InternalException {
        return -2;
    }

    @Override
    public @Nullable VirtualMachineProduct getProduct(@Nonnull String productId) throws InternalException, CloudException {
        for( VirtualMachineProduct product : listProducts(Architecture.I64) ) {
            if( product.getProviderProductId().equals(productId) ) {
                return product;
            }
        }
        return null;
    }

    @Override
    public @Nonnull String getProviderTermForServer(@Nonnull Locale locale) {
        return "virtual machine";
    }

    @Override
    public @Nullable VirtualMachine getVirtualMachine(@Nonnull String vmId) throws InternalException, CloudException {
        for( VirtualMachine vm : listVirtualMachines() ) {
            if( vmId.equals(vm.getProviderVirtualMachineId()) ) {
                return vm;
            }
        }
        return null;
    }

    public @Nullable VirtualMachine getVirtualMachine(@Nonnull String vmId, @Nullable String user, @Nullable String password) throws InternalException, CloudException {
        VirtualMachine vm = getVirtualMachine(vmId);
        
        if( vm == null ) {
            return null;
        }
        vm.setRootUser(user);
        vm.setRootPassword(password);
        return vm;
    }
    
    @Override
    public @Nullable VmStatistics getVMStatistics(String vmId, long from, long to) throws InternalException, CloudException {
        return null;
    }

    @Override
    public @Nonnull Iterable<VmStatistics> getVMStatisticsForPeriod(@Nonnull String vmId, @Nonnegative long from, @Nonnegative long to) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Requirement identifyPasswordRequirement() throws CloudException, InternalException {
        return Requirement.OPTIONAL;
    }

    @Override
    public @Nonnull Requirement identifyRootVolumeRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public @Nonnull Requirement identifyShellKeyRequirement() throws CloudException, InternalException {
        return Requirement.OPTIONAL;
    }

    @Override
    public @Nonnull Requirement identifyVlanRequirement() throws CloudException, InternalException {
        return Requirement.OPTIONAL;
    }

    @Override
    public boolean isAPITerminationPreventable() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isBasicAnalyticsSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isExtendedAnalyticsSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return provider.getDataCenterServices().isSubscribed(AzureService.PERSISTENT_VM_ROLE);
    }

    @Override
    public boolean isUserDataSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public @Nonnull VirtualMachine launch(VMLaunchOptions options) throws CloudException, InternalException {
        Logger logger = Azure.getLogger(AzureVM.class, "std");

        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".launch(" + options + ")");
        }
        try {
            AzureMachineImage image = provider.getComputeServices().getImageSupport().getMachineImage(options.getMachineImageId());

            if( image == null ) {
                throw new CloudException("No such image: " + options.getMachineImageId());
            }
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }
            String label;

            try {
                label = new String(Base64.encodeBase64(options.getFriendlyName().getBytes("utf-8")));
            }
            catch( UnsupportedEncodingException e ) {
                throw new InternalException(e);
            }
            AzureMethod method = new AzureMethod(provider);
            StringBuilder xml = new StringBuilder();
            String id = toUniqueId(options.getHostName());

            xml.append("<CreateHostedService xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("<ServiceName>" + id + "</ServiceName>");
            xml.append("<Label>" + label + "</Label>");
            xml.append("<Description>" + options.getDescription() + "</Description>");
            xml.append("<Location>" + ctx.getRegionId() + "</Location>");
            xml.append("</CreateHostedService>");

            if( logger.isDebugEnabled() ) {
                try {
                    method.parseResponse(xml.toString(), false);
                }
                catch( Exception e ) {
                    logger.warn("Unable to parse outgoing XML locally: " + e.getMessage());
                    logger.warn("XML:");
                    logger.warn(xml.toString());
                }
            }

            method.post(ctx.getAccountNumber(), HOSTED_SERVICES, xml.toString());

            xml = new StringBuilder();
            xml.append("<Deployment xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("<Name>" + id + "</Name>");
            xml.append("<DeploymentSlot>Production</DeploymentSlot>");
            xml.append("<Label>" + label + "</Label>");
            xml.append("<RoleList>");
            xml.append("<Role i:type=\"PersistentVMRole\">");
            xml.append("<RoleName>" + id + "Role</RoleName>");
            xml.append("<RoleType>PersistentVMRole</RoleType>");
            xml.append("<ConfigurationSets>");

            String password = (options.getBootstrapPassword() == null ? provider.generateToken(8, 15) : options.getBootstrapPassword());

            if( image.getPlatform().isWindows() ) {
                xml.append("<WindowsProvisioningConfigurationSet>");
                xml.append("<ComputerName>" + id + "</ComputerName>");
                xml.append("<AdminPassword></AdminPassword>");
                xml.append("<ResetPasswordOnFirstLogon>true</ResetPasswordOnFirstLogon>");
                xml.append("<EnableAutomaticUpdate>true</EnableAutomaticUpdate>");
                xml.append("<TimeZone>UTC</TimeZone>");
                if( options.getBootstrapKey() != null ) {
                    xml.append("<StoredCertificateSettings>");
                    xml.append("<CertificateSetting>");
                    xml.append("<StoreLocation>LocalMachine</StoreLocation>");
                    xml.append("<StoreName>" + id + "-kp</StoreName>");
                    xml.append("<Thumbprint>" + options.getBootstrapKey() + "</Thumbprint>");
                    xml.append("</CertificateSetting");
                    xml.append("</StoredCertificateSettings>");
                }
                xml.append("</WindowsProvisioningConfigurationSet>");
            }
            else {
                xml.append("<ConfigurationSet i:type=\"LinuxProvisioningConfigurationSet\">");
                xml.append("<HostName>" + id + "</HostName>");
                if( options.getBootstrapKey() == null && options.getBootstrapUser() == null ) {
                    xml.append("<UserName>dasein</UserName>");
                    xml.append("<UserPassword>" + password + "</UserPassword>");
                    xml.append("<DisableSshPasswordAuthentication>false</DisableSshPasswordAuthentication>");
                }
                else if( options.getBootstrapUser() != null ) {
                    xml.append("<UserName>" + options.getBootstrapUser() + "</UserName>");
                    xml.append("<UserPassword>" + password + "</UserPassword>");
                    xml.append("<DisableSshPasswordAuthentication>false</DisableSshPasswordAuthentication>");
                }
                else {
                    xml.append("<DisableSshPasswordAuthentication>true</DisableSshPasswordAuthentication>");
                    xml.append("<SSH><PublicKeys><PublicKey><FingerPrint>");
                    xml.append(options.getBootstrapKey());
                    xml.append("</FingerPrint><Path>/etc/ssh/root</Path></PublicKey></PublicKeys></SSH>");
                }
                xml.append("</ConfigurationSet>");
            }
            xml.append("<ConfigurationSet i:type=\"NetworkConfigurationSet\">");
            xml.append("<InputEndpoints><InputEndpoint>");
            xml.append("<EnableDirectServerReturn>false</EnableDirectServerReturn>");
            xml.append("<LocalPort>22</LocalPort>");
            xml.append("<Name>SSH</Name>");
            xml.append("<PublicPort>60256</PublicPort>");
            xml.append("<Protocol>tcp</Protocol>");
            xml.append("</InputEndpoint></InputEndpoints>");
            xml.append("</ConfigurationSet>");
            xml.append("</ConfigurationSets>");
            xml.append("<DataVirtualHardDisks/>");
            xml.append("<OSVirtualHardDisk>");
            xml.append("<HostCaching>ReadWrite</HostCaching>");
            xml.append("<DiskLabel>OS</DiskLabel>");
            xml.append("<MediaLink>http://auxpreview226imagestore.blob.core.azure-preview.com/vhds/" + id + ".vhd</MediaLink>");
            xml.append("<SourceImageName>" + options.getMachineImageId() + "</SourceImageName>");
            xml.append("</OSVirtualHardDisk>");
            xml.append("<RoleSize>" + options.getStandardProductId() + "</RoleSize>");
            xml.append("</Role>");
            xml.append("</RoleList>");
            xml.append("</Deployment>");

            if( logger.isDebugEnabled() ) {
                try {
                    method.parseResponse(xml.toString(), false);
                }
                catch( Exception e ) {
                    logger.warn("Unable to parse outgoing XML locally: " + e.getMessage());
                    logger.warn("XML:");
                    logger.warn(xml.toString());
                }
            }

            method.post(ctx.getAccountNumber(), HOSTED_SERVICES + "/" + id + "/deployments", xml.toString());
            return getVirtualMachine(id, "dasein", password);
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".launch()");
            }
        }
    }

    @Override
    public @Nonnull VirtualMachine launch(@Nonnull String fromMachineImageId, @Nonnull VirtualMachineProduct product, @Nonnull String dataCenterId, @Nonnull String name, @Nonnull String description, @Nullable String withKeypairId, @Nullable String inVlanId, boolean withAnalytics, boolean asSandbox, @Nullable String... firewallIds) throws InternalException, CloudException {
        VMLaunchOptions options = VMLaunchOptions.getInstance(product.getProviderProductId(), fromMachineImageId, name, description);
        
        if( inVlanId == null ) {
            options.inDataCenter(dataCenterId);
        }
        else {
            options.inVlan(null, dataCenterId, inVlanId);
        }
        if( withKeypairId != null ) {
            options.withBoostrapKey(withKeypairId);
        }
        if( withAnalytics ) {
            options.withExtendedAnalytics();
        }
        if( firewallIds != null ) {
            options.behindFirewalls(firewallIds);
        }
        return launch(options);
    }

    @Override
    public @Nonnull VirtualMachine launch(@Nonnull String fromMachineImageId, @Nonnull VirtualMachineProduct product, @Nonnull String dataCenterId, @Nonnull String name, @Nonnull String description, @Nullable String withKeypairId, @Nullable String inVlanId, boolean withAnalytics, boolean asSandbox, @Nullable String[] firewallIds, @Nullable Tag... tags) throws InternalException, CloudException {
        VMLaunchOptions options = VMLaunchOptions.getInstance(product.getProviderProductId(), fromMachineImageId, name, description);

        if( inVlanId == null ) {
            options.inDataCenter(dataCenterId);
        }
        else {
            options.inVlan(null, dataCenterId, inVlanId);
        }
        if( withKeypairId != null ) {
            options.withBoostrapKey(withKeypairId);
        }
        if( withAnalytics ) {
            options.withExtendedAnalytics();
        }
        if( firewallIds != null ) {
            options.behindFirewalls(firewallIds);
        }
        if( tags != null && tags.length > 0 ) {
            HashMap<String,Object> md = new HashMap<String, Object>();
            
            for( Tag t : tags ) {
                md.put(t.getKey(), t.getValue());
            }
            options.withMetaData(md);
        }
        return launch(options);
    }

    @Override
    public @Nonnull Iterable<String> listFirewalls(@Nonnull String vmId) throws InternalException, CloudException {
        // TODO: implement me
        return null;
    }

    @Override
    public @Nonnull Iterable<VirtualMachineProduct> listProducts(@Nonnull Architecture architecture) throws InternalException, CloudException {
        if( architecture.equals(Architecture.I32) ) {
            return Collections.emptyList();
        }
        VirtualMachineProduct product = new VirtualMachineProduct();

        product.setCpuCount(1);
        product.setDescription("Small");
        product.setRootVolumeSize(new Storage<Gigabyte>(15, Storage.GIGABYTE));
        product.setName("Small");
        product.setProviderProductId("Small");
        product.setRamSize(new Storage<Gigabyte>(2, Storage.GIGABYTE));
        return Collections.singletonList(product);
    }

    @Override
    public Iterable<Architecture> listSupportedArchitectures() throws InternalException, CloudException {
        return Collections.singletonList(Architecture.I64);
    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), HOSTED_SERVICES);

        if( doc == null ) {
            return Collections.emptyList();
        }
        NodeList entries = doc.getElementsByTagName("HostedService");
        ArrayList<VirtualMachine> vms = new ArrayList<VirtualMachine>();

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            VirtualMachine vm = toVirtualMachine(ctx, entry);

            if( vm != null ) {
                vms.add(vm);
            }
        }
        return vms;
    }

    @Override
    public void pause(@Nonnull String vmId) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void reboot(@Nonnull String vmId) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsAnalytics() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void terminate(@Nonnull String vmId) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Nonnull
    @Override
    public String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
    
    private @Nullable VirtualMachine toVirtualMachine(@Nonnull ProviderContext ctx, @Nullable Node entry) throws CloudException, InternalException {
        if( entry == null ) {
            return null;
        }
        String regionId = ctx.getRegionId();

        if( regionId == null ) {
            throw new AzureConfigException("No region ID was specified for this request");
        }
        VirtualMachine vm = new VirtualMachine();

        vm.setArchitecture(Architecture.I64);
        vm.setClonable(false);
        vm.setCurrentState(VmState.TERMINATED);
        vm.setImagable(false);
        vm.setPersistent(true);
        vm.setPlatform(Platform.UNKNOWN);
        vm.setProviderDataCenterId(regionId + "-a");
        vm.setProviderOwnerId(ctx.getAccountNumber());
        vm.setProviderRegionId(regionId);
        NodeList attributes = entry.getChildNodes();
        String uri = null;
        
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if( attribute.getNodeName().equalsIgnoreCase("url") && attribute.hasChildNodes() ) {
                uri = attribute.getFirstChild().getNodeValue().trim();
            }
            else if( attribute.getNodeName().equalsIgnoreCase("servicename") && attribute.hasChildNodes() ) {
                vm.setProviderVirtualMachineId(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("hostedserviceproperties") && attribute.hasChildNodes() ) {
                NodeList properties = attribute.getChildNodes();
                
                for( int j=0; j<properties.getLength(); j++ ) {
                    Node property = properties.item(j);

                    if( property.getNodeName().equalsIgnoreCase("location") && property.hasChildNodes() ) {
                        if( !regionId.equals(property.getFirstChild().getNodeValue().trim()) ) {
                            return null;
                        }
                    }
                    else if( property.getNodeName().equalsIgnoreCase("datecreated") && property.hasChildNodes() ) {
                        vm.setCreationTimestamp(provider.parseTimestamp(property.getFirstChild().getNodeValue().trim()));
                    }
                }
            }
        }
        if( uri == null ) {
            return null;
        }
        if( vm.getProviderVirtualMachineId() == null ) {
            return null;
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), HOSTED_SERVICES + "/" + vm.getProviderVirtualMachineId() + "?embed-detail=true");

        if( doc == null ) {
            return null;
        }
        NodeList entries = doc.getElementsByTagName("Deployment");

        if( entries.getLength() < 1 ) {
            return null;
        }
        HashMap<String,String> tags = new HashMap<String, String>();
        
        for( int i=0; i<entries.getLength(); i++ ) {
            Node detail = entries.item(i);
            String status = null;

            attributes = detail.getChildNodes();
            for( int j=0; j<attributes.getLength(); j++ ) {
                Node attribute = attributes.item(j);
                
                if( attribute.getNodeName().equalsIgnoreCase("deploymentslot") && attribute.hasChildNodes() ) {
                    tags.put("DeploymentSlot", attribute.getFirstChild().getNodeValue().trim());
                }
                else if( attribute.getNodeName().equalsIgnoreCase("privateid") && attribute.hasChildNodes() ) {
                    tags.put("DeploymentID", attribute.getFirstChild().getNodeValue().trim());
                }
                else if( attribute.getNodeName().equalsIgnoreCase("status") && attribute.hasChildNodes() ) {
                    status = attribute.getFirstChild().getNodeValue().trim();
                }
                else if( attribute.getNodeName().equalsIgnoreCase("url") && attribute.hasChildNodes() ) {
                    try {
                        URI u = new URI(attribute.getFirstChild().getNodeValue().trim());
                        
                        vm.setPublicDnsAddress(u.getHost());
                    }
                    catch( URISyntaxException e ) {
                        // ignore
                    }
                }
                else if( attribute.getNodeName().equalsIgnoreCase("roleinstancelist") && attribute.hasChildNodes() ) {
                    NodeList roleInstances = attribute.getChildNodes();
                    
                    for( int k=0; k<roleInstances.getLength(); k++ ) {
                        Node roleInstance = roleInstances.item(k);
                        
                        if( roleInstance.getNodeName().equalsIgnoreCase("roleinstance") && roleInstance.hasChildNodes() ) {
                            NodeList roleAttributes = roleInstance.getChildNodes();
                            
                            for( int l=0; l<roleAttributes.getLength(); l++ ) {
                                Node roleAttribute = roleAttributes.item(l);

                                if( roleAttribute.getNodeName().equalsIgnoreCase("instancesize") && roleAttribute.hasChildNodes() ) {
                                    vm.setProductId(roleAttribute.getFirstChild().getNodeValue().trim());
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("instanceupgradedomain") && roleAttribute.hasChildNodes() ) {
                                    tags.put("UpgradeDomain", roleAttribute.getFirstChild().getNodeValue().trim());
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("instanceerrorcode") && roleAttribute.hasChildNodes() ) {
                                    tags.put("ErrorCode", roleAttribute.getFirstChild().getNodeValue().trim());
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("instancefaultdomain") && roleAttribute.hasChildNodes() ) {
                                    tags.put("FaultDomain", roleAttribute.getFirstChild().getNodeValue().trim());
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("fqdn") && roleAttribute.hasChildNodes() ) {
                                    vm.setPrivateDnsAddress(roleAttribute.getFirstChild().getNodeValue().trim());
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("ipaddress") && roleAttribute.hasChildNodes() ) {
                                    vm.setPrivateIpAddresses(new String[] { roleAttribute.getFirstChild().getNodeValue().trim() });
                                }
                            }
                        }
                    }
                }
                else if( attribute.getNodeName().equalsIgnoreCase("rolelist") && attribute.hasChildNodes() ) {
                    NodeList roles = attribute.getChildNodes();

                    for( int k=0; k<roles.getLength(); k++ ) {
                        Node role = roles.item(k);

                        if( role.getNodeName().equalsIgnoreCase("role") && role.hasChildNodes() ) {
                            if( role.hasAttributes() ) {
                                Node n = role.getAttributes().getNamedItem("i:type");
                            
                                if( n != null ) {
                                    String val = n.getNodeValue();
                                    
                                    if( !"PersistentVMRole".equalsIgnoreCase(val) ) {
                                        return null;
                                    }
                                }
                            }
                            NodeList roleAttributes = role.getChildNodes();

                            for( int l=0; l<roleAttributes.getLength(); l++ ) {
                                Node roleAttribute = roleAttributes.item(l);

                                if( roleAttribute.getNodeName().equalsIgnoreCase("osvirtualharddisk") && roleAttribute.hasChildNodes() ) {
                                    NodeList diskAttributes = roleAttribute.getChildNodes();

                                    for( int m=0; m<diskAttributes.getLength(); m++ ) {
                                        Node diskAttribute = diskAttributes.item(m);

                                        if( diskAttribute.getNodeName().equalsIgnoreCase("SourceImageName") && diskAttribute.hasChildNodes() ) {
                                            vm.setProviderMachineImageId(diskAttribute.getFirstChild().getNodeValue().trim());
                                        }
                                        else if( diskAttribute.getNodeName().equalsIgnoreCase("OS") && diskAttribute.hasChildNodes() ) {
                                            String os = diskAttribute.getFirstChild().getNodeValue().trim();

                                            if( os.equalsIgnoreCase("linux") ) {
                                                vm.setPlatform(Platform.UNIX);
                                            }
                                            else if( os.equalsIgnoreCase("windows") ) {
                                                vm.setPlatform(Platform.WINDOWS);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if( status != null ) {
                if( "running".equalsIgnoreCase(status) ) {
                    vm.setCurrentState(VmState.RUNNING);
                    break;
                }
                else {
                    if( "suspended".equalsIgnoreCase(status) ) {
                        vm.setCurrentState(VmState.PAUSED);
                    }
                    else if( "runningtransitioning".equalsIgnoreCase(status)  || "starting".equalsIgnoreCase(status) || "deploying".equalsIgnoreCase(status) || "deleting".equalsIgnoreCase(status) ) {
                        vm.setCurrentState(VmState.PENDING);
                    }
                    else if( "suspendedtransitioning".equalsIgnoreCase(status) || "suspending".equalsIgnoreCase(status) ) {
                        vm.setCurrentState(VmState.STOPPING);
                    }
                    else {
                        System.out.println("Unknown VM status: " + status);
                    }
                }
            }
        }
        if( vm.getName() == null ) {
            vm.setName(vm.getProviderVirtualMachineId());
        }
        if( vm.getDescription() == null ) {
            vm.setDescription(vm.getName());
        }
        if( vm.getPlatform().equals(Platform.UNKNOWN) ) {
            String descriptor = (vm.getProviderVirtualMachineId() + " " + vm.getName() + " " + vm.getDescription() + " " + vm.getProviderMachineImageId()).replaceAll("_", " ");

            vm.setPlatform(Platform.guess(descriptor));
        }
        else if( vm.getPlatform().equals(Platform.UNIX) ) {
            String descriptor = (vm.getProviderVirtualMachineId() + " " + vm.getName() + " " + vm.getDescription() + " " + vm.getProviderMachineImageId()).replaceAll("_", " ");
            Platform p = Platform.guess(descriptor);
            
            if( p.isUnix() ) {
                vm.setPlatform(p);
            }
        }
        vm.setTags(tags);
        return vm;
    }
    
    private @Nonnull String toUniqueId(String name) throws CloudException, InternalException {
        String base = name.toLowerCase().replaceAll(" ", "");
        VirtualMachine vm = null;
        int i = 0;
        
        do {
            if( i > 0 ) {
                name = base + i;
            }
            i++;
            vm = getVirtualMachine(name);
        } while( vm != null );
        return name;
    }
}
