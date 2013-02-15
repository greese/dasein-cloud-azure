package org.dasein.cloud.azure.compute.vm;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
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
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineProduct;
import org.dasein.cloud.compute.VirtualMachineSupport;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.compute.VmStatistics;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Implements virtual machine support for Microsoft Azure.
 * @author George Reese (george.reese@imaginary.com)
 * @author Qunying Huang
 * @since 2012.04.1 initial version
 * @version 2012.04.1
 * @version 2012.09 updated for model changes
 */
public class AzureVM implements VirtualMachineSupport {
    static private final Logger logger = Azure.getLogger(AzureVM.class);

    static public final String HOSTED_SERVICES = "/services/hostedservices";

    private Azure provider;

    public AzureVM(Azure provider) { this.provider = provider; }

    @Override
    public void start(@Nonnull String vmId) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".Boot()");
        }
        VirtualMachine vm = getVirtualMachine(vmId);

        if( vm == null ) {
            throw new CloudException("No such virtual machine: " + vmId);
        }
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was set for this request");
        }
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
        String resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  serviceName + "/roleInstances/" + roleName + "/Operations";

        try{
            AzureMethod method = new AzureMethod(provider);
           
        	StringBuilder xml = new StringBuilder();
        	xml.append("<StartRoleOperation xmlns=\"http://schemas.microsoft.com/windowsazure\"");
        	xml.append(" ");
        	xml.append("xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
        	xml.append("\n");
        	xml.append("<OperationType>StartRoleOperation</OperationType>");
        	xml.append("\n");
        	xml.append("</StartRoleOperation>");
        	xml.append("\n");
          	method.post(ctx.getAccountNumber(), resourceDir, xml.toString());
        	
        }finally {
        	if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".launch()");
            }
        }
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
        // TODO: this can be optimized
        for( VirtualMachine vm : listVirtualMachines() ) {
            if( vmId.equals(vm.getProviderVirtualMachineId()) ) {
                return vm;
            }
        }
        return null;
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
            String hostName = toUniqueId(options.getHostName());
            String deploymentSlot = (String)options.getMetaData().get("environment");

            if( deploymentSlot == null ) {
                deploymentSlot = "Production";
            }
            else if( !deploymentSlot.equalsIgnoreCase("Production") && !deploymentSlot.equalsIgnoreCase("Staging") ) {
                deploymentSlot = "Production";
            }
            xml.append("<CreateHostedService xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("<ServiceName>").append(hostName).append("</ServiceName>");
            xml.append("<Label>").append(label).append("</Label>");
            xml.append("<Description>").append(options.getDescription()).append("</Description>");
            xml.append("<Location>").append(ctx.getRegionId()).append("</Location>");
            xml.append("</CreateHostedService>");
            method.post(ctx.getAccountNumber(), HOSTED_SERVICES, xml.toString());

            xml = new StringBuilder();
            xml.append("<Deployment xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("<Name>").append(hostName).append("</Name>");
            xml.append("<DeploymentSlot>").append(deploymentSlot).append("</DeploymentSlot>");
            xml.append("<Label>").append(label).append("</Label>");
            xml.append("<RoleList>");
            xml.append("<Role i:type=\"PersistentVMRole\">");
            xml.append("<RoleName>").append(hostName).append("</RoleName>");
            xml.append("<RoleType>PersistentVMRole</RoleType>");
            xml.append("<ConfigurationSets>");

            String password = (options.getBootstrapPassword() == null ? provider.generateToken(8, 15) : options.getBootstrapPassword());

            if( image.getPlatform().isWindows() ) {
                xml.append("<WindowsProvisioningConfigurationSet>");
                xml.append("<ComputerName>").append(hostName).append("</ComputerName>");
                xml.append("<AdminPassword>").append(password).append("</AdminPassword>");
                xml.append("<ResetPasswordOnFirstLogon>true</ResetPasswordOnFirstLogon>");
                xml.append("<EnableAutomaticUpdate>true</EnableAutomaticUpdate>");
                xml.append("<TimeZone>UTC</TimeZone>");
                /*
                if( options.getBootstrapKey() != null ) {
                    xml.append("<StoredCertificateSettings>");
                    xml.append("<CertificateSetting>");
                    xml.append("<StoreLocation>LocalMachine</StoreLocation>");
                    xml.append("<StoreName>").append(hostName).append("-kp</StoreName>");
                    xml.append("<Thumbprint>").append(options.getBootstrapKey()).append("</Thumbprint>");
                    xml.append("</CertificateSetting");
                    xml.append("</StoredCertificateSettings>");
                }
                */
                xml.append("</WindowsProvisioningConfigurationSet>");
            }
            else {
                xml.append("<ConfigurationSet i:type=\"LinuxProvisioningConfigurationSet\">");
                xml.append("<HostName>").append(hostName).append("</HostName>");
                if( options.getBootstrapUser() == null ) {
                    xml.append("<UserName>dasein</UserName>");
                    xml.append("<UserPassword>").append(password).append("</UserPassword>");
                    xml.append("<DisableSshPasswordAuthentication>false</DisableSshPasswordAuthentication>");
                }
                else {
                    xml.append("<UserName>").append(options.getBootstrapUser()).append("</UserName>");
                    xml.append("<UserPassword>").append(password).append("</UserPassword>");
                    xml.append("<DisableSshPasswordAuthentication>false</DisableSshPasswordAuthentication>");
                }
                /*
                else {
                    xml.append("<DisableSshPasswordAuthentication>true</DisableSshPasswordAuthentication>");
                    xml.append("<SSH><PublicKeys><PublicKey><FingerPrint>");
                    xml.append(options.getBootstrapKey());
                    xml.append("</FingerPrint><Path>/etc/ssh/root</Path></PublicKey></PublicKeys></SSH>");
                }
                */
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
            xml.append("<MediaLink>").append(provider.getStorageEndpoint()).append("vhds/").append(hostName).append(".vhd</MediaLink>");
            xml.append("<SourceImageName>").append(options.getMachineImageId()).append("</SourceImageName>");
            xml.append("</OSVirtualHardDisk>");
            xml.append("<RoleSize>").append(options.getStandardProductId()).append("</RoleSize>");
            xml.append("</Role>");
            xml.append("</RoleList>");
            xml.append("</Deployment>");
            method.post(ctx.getAccountNumber(), HOSTED_SERVICES + "/" + hostName + "/deployments", xml.toString());

            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);

            VirtualMachine vm = null ;

            while( timeout > System.currentTimeMillis() ) {
                try { vm = getVirtualMachine(hostName + ":" + hostName); }
                catch( Throwable ignore ) { }
                if( vm != null ) {
                    vm.setRootUser("dasein");
                    vm.setRootPassword(password);
                    break;
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
            }
            if( vm == null ) {
                throw new CloudException("System timed out waiting for virtual machine to appear");
            }
            if( VmState.STOPPED.equals(vm.getCurrentState()) ) {
                start(vm.getProviderVirtualMachineId());
            }
            return vm;
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
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<VirtualMachineProduct> listProducts(@Nonnull Architecture architecture) throws InternalException, CloudException {
        if( architecture.equals(Architecture.I32) ) {
            return Collections.emptyList();
        }
        
        ArrayList<VirtualMachineProduct> list = new ArrayList<VirtualMachineProduct>();
        VirtualMachineProduct product = new VirtualMachineProduct();

        product.setCpuCount(1);
        product.setDescription("Extra Small");
        product.setRootVolumeSize(new Storage<Gigabyte>(15, Storage.GIGABYTE));
        product.setName("Extra Small");
        product.setProviderProductId("Extra Small");
        product.setRamSize(new Storage<Gigabyte>(1, Storage.GIGABYTE));
        list.add(product);
        
        product = new VirtualMachineProduct();

        product.setCpuCount(1);
        product.setDescription("Small");
        product.setRootVolumeSize(new Storage<Gigabyte>(15, Storage.GIGABYTE));
        product.setName("Small");
        product.setProviderProductId("Small");
        product.setRamSize(new Storage<Gigabyte>(2, Storage.GIGABYTE));
        list.add(product);
        
        product = new VirtualMachineProduct();

        product.setCpuCount(2);
        product.setDescription("Medium");
        product.setRootVolumeSize(new Storage<Gigabyte>(15, Storage.GIGABYTE));
        product.setName("Medium");
        product.setProviderProductId("Medium");
        product.setRamSize(new Storage<Gigabyte>(4, Storage.GIGABYTE)); //3.5G
        list.add(product);
        
        product = new VirtualMachineProduct();

        product.setCpuCount(4);
        product.setDescription("Large");
        product.setRootVolumeSize(new Storage<Gigabyte>(15, Storage.GIGABYTE));
        product.setName("Large");
        product.setProviderProductId("Large");
        product.setRamSize(new Storage<Gigabyte>(7, Storage.GIGABYTE)); //3.5G
        list.add(product);
        
        product = new VirtualMachineProduct();

        product.setCpuCount(8);
        product.setDescription("Extra Large");
        product.setRootVolumeSize(new Storage<Gigabyte>(15, Storage.GIGABYTE));
        product.setName("Extra Large");
        product.setProviderProductId("Extra Large");
        product.setRamSize(new Storage<Gigabyte>(14, Storage.GIGABYTE)); //3.5G
        list.add(product);
        
        return Collections.unmodifiableList(list);
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
            parseHostedService(ctx, entries.item(i), null, vms);
        }
        return vms;
    }

    private void parseDeployment(@Nonnull ProviderContext ctx, @Nonnull String regionId, @Nonnull String serviceName, @Nonnull Node node, @Nonnull List<VirtualMachine> virtualMachines) {
        ArrayList<VirtualMachine> list = new ArrayList<VirtualMachine>();
        NodeList attributes = node.getChildNodes();
        String deploymentSlot = null;
        String deploymentId = null;
        String dnsName = null;
        String vmRoleName = null;
        String imageId = null;
        String mediaLink = null;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if( attribute.getNodeType() == Node.TEXT_NODE) {
                continue;
            }
            if( attribute.getNodeName().equalsIgnoreCase("deploymentslot") && attribute.hasChildNodes() ) {
                deploymentSlot = attribute.getFirstChild().getNodeValue().trim();
            }
            else if( attribute.getNodeName().equalsIgnoreCase("privateid") && attribute.hasChildNodes() ) {
                deploymentId = attribute.getFirstChild().getNodeValue().trim();
            }
            else if( attribute.getNodeName().equalsIgnoreCase("url") && attribute.hasChildNodes() ) {
                try {
                    URI u = new URI(attribute.getFirstChild().getNodeValue().trim());

                    dnsName = u.getHost();
                }
                catch( URISyntaxException e ) {
                    // ignore
                }
            }
            else if( attribute.getNodeName().equalsIgnoreCase("roleinstancelist") && attribute.hasChildNodes() ) {
                NodeList roleInstances = attribute.getChildNodes();

                for( int j=0; j<roleInstances.getLength(); j++ ) {
                    Node roleInstance = roleInstances.item(j);

                    if(roleInstance.getNodeType() == Node.TEXT_NODE) {
                        continue;
                    }
                    if( roleInstance.getNodeName().equalsIgnoreCase("roleinstance") && roleInstance.hasChildNodes() ) {
                        VirtualMachine role = new VirtualMachine();

                        role.setArchitecture(Architecture.I64);
                        role.setClonable(false);
                        role.setCurrentState(VmState.TERMINATED);
                        role.setImagable(false);
                        role.setPersistent(true);
                        role.setPlatform(Platform.UNKNOWN);
                        role.setProviderOwnerId(ctx.getAccountNumber());
                        role.setProviderRegionId(regionId);
                        role.setProviderDataCenterId(regionId);

                        NodeList roleAttributes = roleInstance.getChildNodes();

                        for( int l=0; l<roleAttributes.getLength(); l++ ) {
                            Node roleAttribute = roleAttributes.item(l);

                            if( roleAttribute.getNodeType() == Node.TEXT_NODE ) {
                                continue;
                            }
                            if( roleAttribute.getNodeName().equalsIgnoreCase("RoleName") && roleAttribute.hasChildNodes() ) {
                                String vmId  = roleAttribute.getFirstChild().getNodeValue().trim();

                                role.setProviderVirtualMachineId(serviceName + ":" + vmId);
                                role.setName(vmId);
                            }
                            //else if( roleAttribute.getNodeName().equalsIgnoreCase("InstanceStatus") && roleAttribute.hasChildNodes() ) {
                            //    String status = roleAttribute.getFirstChild().getNodeValue().trim();

                            //    System.out.println("INSTANCE STATUS=" + status);
                            //}
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("instancesize") && roleAttribute.hasChildNodes() ) {
                                role.setProductId(roleAttribute.getFirstChild().getNodeValue().trim());
                            }
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("instanceupgradedomain") && roleAttribute.hasChildNodes() ) {
                                role.setTag("UpgradeDomain", roleAttribute.getFirstChild().getNodeValue().trim());
                            }
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("instanceerrorcode") && roleAttribute.hasChildNodes() ) {
                                role.setTag("ErrorCode", roleAttribute.getFirstChild().getNodeValue().trim());
                            }
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("instancefaultdomain") && roleAttribute.hasChildNodes() ) {
                                role.setTag("FaultDomain", roleAttribute.getFirstChild().getNodeValue().trim());
                            }
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("fqdn") && roleAttribute.hasChildNodes() ) {
                                role.setPrivateDnsAddress(roleAttribute.getFirstChild().getNodeValue().trim());
                            }
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("ipaddress") && roleAttribute.hasChildNodes() ) {
                                role.setPrivateIpAddresses(new String[] { roleAttribute.getFirstChild().getNodeValue().trim() });
                            }
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("instanceendpoints") && roleAttribute.hasChildNodes() ) {
                                NodeList endpoints = roleAttribute.getChildNodes();

                                for( int m=0; m<endpoints.getLength(); m++ ) {
                                    Node endpoint = endpoints.item(m);

                                    if( endpoint.hasChildNodes() ) {
                                        NodeList ea = endpoint.getChildNodes();

                                        for( int n=0; n<ea.getLength(); n++ ) {
                                            Node a = ea.item(n);

                                            if( a.getNodeName().equalsIgnoreCase("vip") && a.hasChildNodes() ) {
                                                String addr = a.getFirstChild().getNodeValue().trim();
                                                String[] ips = role.getPublicIpAddresses();

                                                if( ips == null || ips.length < 1 ) {
                                                    role.setPublicIpAddresses(new String[] { addr });
                                                }
                                                else {
                                                    boolean found = false;

                                                    for( String ip : ips ) {
                                                        if( ip.equals(addr) ) {
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                    if( !found ) {
                                                        String[] tmp = new String[ips.length + 1];

                                                        System.arraycopy(ips, 0, tmp, 0, ips.length);
                                                        tmp[tmp.length-1] = addr;
                                                        role.setPublicIpAddresses(tmp);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("PowerState") && roleAttribute.hasChildNodes() ) {
                                String powerStatus = roleAttribute.getFirstChild().getNodeValue().trim();

                                if( "Started".equalsIgnoreCase(powerStatus)){
                                    role.setCurrentState(VmState.RUNNING);
                                }
                                else if( "Stopped".equalsIgnoreCase(powerStatus)){
                                    role.setCurrentState(VmState.STOPPED);
                                    role.setImagable(true);
                                }
                                else if( "Stopping".equalsIgnoreCase(powerStatus)){
                                    role.setCurrentState(VmState.STOPPING);
                                }
                                else if( "Starting".equalsIgnoreCase(powerStatus)){
                                    role.setCurrentState(VmState.PENDING);
                                }
                                else {
                                    logger.warn("DEBUG: Unknown Azure status: " + powerStatus);
                                    System.out.println("DEBUG: Unknown Azure status: " + powerStatus);
                                }
                            }
                        }
                        if( role.getProviderVirtualMachineId() == null ) {
                            continue;
                        }
                        if( role.getName() == null ) {
                            role.setName(role.getProviderVirtualMachineId());
                        }
                        if( role.getDescription() == null ) {
                            role.setDescription(role.getName());
                        }
                        if( role.getPlatform().equals(Platform.UNKNOWN) ) {
                            String descriptor = (role.getProviderVirtualMachineId() + " " + role.getName() + " " + role.getDescription() + " " + role.getProviderMachineImageId()).replaceAll("_", " ");

                            role.setPlatform(Platform.guess(descriptor));
                        }
                        else if( role.getPlatform().equals(Platform.UNIX) ) {
                            String descriptor = (role.getProviderVirtualMachineId() + " " + role.getName() + " " + role.getDescription() + " " + role.getProviderMachineImageId()).replaceAll("_", " ");
                            Platform p = Platform.guess(descriptor);

                            if( p.isUnix() ) {
                                role.setPlatform(p);
                            }
                        }
                        list.add(role);
                    }
                }
            }
            //Contain the information about disk and firewall;
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
                                    continue;
                                }
                            }
                        }
                        NodeList roleAttributes = role.getChildNodes();

                        for( int l=0; l<roleAttributes.getLength(); l++ ) {
                            Node roleAttribute = roleAttributes.item(l);

                            if( roleAttribute.getNodeType() == Node.TEXT_NODE ) {
                                continue;
                            }
                            if( roleAttribute.getNodeName().equalsIgnoreCase("osvirtualharddisk") && roleAttribute.hasChildNodes() ) {
                                NodeList diskAttributes = roleAttribute.getChildNodes();

                                for( int m=0; m<diskAttributes.getLength(); m++ ) {
                                    Node diskAttribute = diskAttributes.item(m);

                                    if( diskAttribute.getNodeName().equalsIgnoreCase("SourceImageName") && diskAttribute.hasChildNodes() ) {
                                        imageId = diskAttribute.getFirstChild().getNodeValue().trim();
                                    }
                                    else if( diskAttribute.getNodeName().equalsIgnoreCase("medialink") && diskAttribute.hasChildNodes() ) {
                                        mediaLink = diskAttribute.getFirstChild().getNodeValue().trim();
                                    }
                                }
                            }
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("RoleName") && roleAttribute.hasChildNodes() ) {
                                vmRoleName = roleAttribute.getFirstChild().getNodeValue().trim();
                            }
                        }
                    }
                }
            }
        }
        if( vmRoleName != null ) {
            for( VirtualMachine vm : list ) {
                if( deploymentSlot != null ) {
                    vm.setTag("environment", deploymentSlot);
                }
                if( deploymentId != null ) {
                    vm.setTag("deploymentId", deploymentId);
                }
                if( dnsName != null ) {
                    vm.setPublicDnsAddress(dnsName);
                }
                if( imageId != null ) {
                    Platform fallback = vm.getPlatform();

                    vm.setProviderMachineImageId(imageId);
                    vm.setPlatform(Platform.guess(vm.getProviderMachineImageId()));
                    if( vm.getPlatform().equals(Platform.UNKNOWN) ) {
                        try {
                            MachineImage img = provider.getComputeServices().getImageSupport().getMachineImage(vm.getProviderMachineImageId());

                            if( img != null ) {
                                vm.setPlatform(img.getPlatform());
                            }
                        }
                        catch( Throwable t ) {
                            logger.warn("Error loading machine image: " + t.getMessage());
                        }
                        if( vm.getPlatform().equals(Platform.UNKNOWN) ) {
                            vm.setPlatform(fallback);
                        }
                    }
                }
                vm.setTag("serviceName", serviceName);
                if( mediaLink != null ) {
                    vm.setTag("mediaLink", mediaLink);
                }
                virtualMachines.add(vm);
            }
        }
    }

    private void parseHostedService(@Nonnull ProviderContext ctx, @Nonnull Node entry, @Nullable String serviceName, @Nonnull List<VirtualMachine> virtualMachines) throws CloudException, InternalException {
        String regionId = ctx.getRegionId();

        if( regionId == null ) {
            throw new AzureConfigException("No region ID was specified for this request");
        }

        NodeList attributes = entry.getChildNodes();
        String uri = null;
        long created = 0L;
        String service = null;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if(attribute.getNodeType() == Node.TEXT_NODE) {
                continue;
            }
            if( attribute.getNodeName().equalsIgnoreCase("url") && attribute.hasChildNodes() ) {
                uri = attribute.getFirstChild().getNodeValue().trim();
            }
            else if( attribute.getNodeName().equalsIgnoreCase("servicename") && attribute.hasChildNodes() ) {
                service = attribute.getFirstChild().getNodeValue().trim();
                if( serviceName != null && !service.equals(serviceName) ) {
                    return;
                }
            }
            else if( attribute.getNodeName().equalsIgnoreCase("hostedserviceproperties") && attribute.hasChildNodes() ) {
                NodeList properties = attribute.getChildNodes();

                for( int j=0; j<properties.getLength(); j++ ) {
                    Node property = properties.item(j);

                    if(property.getNodeType() == Node.TEXT_NODE) {
                        continue;
                    }
                    if( property.getNodeName().equalsIgnoreCase("location") && property.hasChildNodes() ) {
                        if( !regionId.equals(property.getFirstChild().getNodeValue().trim()) ) {
                            return;
                        }
                    }
                    else if( property.getNodeName().equalsIgnoreCase("datecreated") && property.hasChildNodes() ) {
                        created = provider.parseTimestamp(property.getFirstChild().getNodeValue().trim());
                    }
                }
            }
        }
        if( uri == null || service == null ) {
            return;
        }

        AzureMethod method = new AzureMethod(provider);

        String resourceDir =  HOSTED_SERVICES + "/"+ service + "/deployments/" + service;
        Document doc = method.getAsXML(ctx.getAccountNumber(),resourceDir);

        if( doc == null ) {
            return;
        }
        NodeList entries = doc.getElementsByTagName("Deployment");

        for( int i=0; i<entries.getLength(); i++ ) {
            parseDeployment(ctx, regionId, service, entries.item(i), virtualMachines);
        }
        for( VirtualMachine vm : virtualMachines ) {
            if( vm.getCreationTimestamp() < 1L ) {
                vm.setCreationTimestamp(created);
            }
        }
    }

    @Override
    public void reboot(@Nonnull String vmId) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".reboot()");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was set for this request");
            }
            VirtualMachine vm = getVirtualMachine(vmId);

            if( vm == null ) {
                throw new CloudException("No such virtual machine: " + vmId);
            }
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
            String resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  serviceName + "/roleInstances/" + roleName + "/Operations";

            AzureMethod method = new AzureMethod(provider);
           
        	StringBuilder xml = new StringBuilder();
        	xml.append("<RestartRoleOperation xmlns=\"http://schemas.microsoft.com/windowsazure\"");
        	xml.append(" ");
        	xml.append("xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
        	xml.append("\n");
        	xml.append("<OperationType>RestartRoleOperation</OperationType>");
        	xml.append("\n");
        	xml.append("</RestartRoleOperation>");
        	xml.append("\n");

            if( logger.isInfoEnabled() ) {
                logger.info("Rebooting " + vmId);
            }
          	method.post(ctx.getAccountNumber(), resourceDir, xml.toString());
        	
        }
        finally {
        	if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".reboot()");
            }
        }
    
    }

    @Override
    public void resume(@Nonnull String vmId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Suspend/resume is not supported in Microsoft Azure");
    }

    @Override
    public void pause(@Nonnull String vmId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("Pause/unpause is not supported in Microsoft Azure");
    }

    @Override
    public void stop(@Nonnull String vmId) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".Boot()");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was set for this request");
            }
            VirtualMachine vm = getVirtualMachine(vmId);

            if( vm == null ) {
                throw new CloudException("No such virtual machine: " + vmId);
            }
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
            String resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  serviceName + "/roleInstances/" + roleName + "/Operations";

            AzureMethod method = new AzureMethod(provider);

            StringBuilder xml = new StringBuilder();
            xml.append("<ShutdownRoleOperation  xmlns=\"http://schemas.microsoft.com/windowsazure\"");
            xml.append(" ");
            xml.append("xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("\n");
            xml.append("<OperationType>ShutdownRoleOperation </OperationType>");
            xml.append("\n");
            xml.append("</ShutdownRoleOperation>");
            xml.append("\n");

            if( logger.isInfoEnabled() ) {
                logger.info("Stopping the " + provider.getCloudName() + " virtual machine: " + vmId);
            }
            method.post(ctx.getAccountNumber(), resourceDir, xml.toString());
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".launch()");
            }
        }
    }

    @Override
    public boolean supportsAnalytics() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean supportsPauseUnpause(@Nonnull VirtualMachine vm) {
        return false;
    }

    @Override
    public boolean supportsStartStop(@Nonnull VirtualMachine vm) {
        return true;
    }

    @Override
    public boolean supportsSuspendResume(@Nonnull VirtualMachine vm) {
        return false;
    }

    @Override
    public void suspend(@Nonnull String vmId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Suspend/resume is not supported in Microsoft Azure");
    }

    @Override
    public void terminate(@Nonnull String vmId) throws InternalException, CloudException {
    	if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".terminate()");
        }
        try {
            VirtualMachine vm = getVirtualMachine(vmId);

            if( vm == null ) {
                throw new CloudException("No such virtual machine: " + vmId);
            }
            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);

            while( timeout > System.currentTimeMillis() ) {
                if( vm == null || VmState.TERMINATED.equals(vm.getCurrentState()) ) {
                    return;
                }
                if( !VmState.PENDING.equals(vm.getCurrentState()) && !VmState.STOPPING.equals(vm.getCurrentState()) ) {
                    break;
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
                try { vm = getVirtualMachine(vmId); }
                catch( Throwable ignore ) { }
            }
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was set for this request");
            }
            String[] parts = vmId.split(":");
            String serviceName;

            if( parts.length == 2 ) {
                serviceName = parts[0];
            }
            else {
                serviceName = vmId;
            }
            String resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  serviceName;
            AzureMethod method = new AzureMethod(provider);

            timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*10L);
            while( timeout > System.currentTimeMillis() ) {
                if( logger.isInfoEnabled() ) {
                    logger.info("Deleting deployments for " + serviceName);
                }
                try {
                    method.invoke("DELETE", ctx.getAccountNumber(), resourceDir, "");
                    break;
                }
                catch( CloudException e ) {
                    if( e.getProviderCode() != null && e.getProviderCode().equals("ConflictError") ) {
                        logger.warn("Conflict error, maybe retrying in 30 seconds");
                        try { Thread.sleep(30000L); }
                        catch( InterruptedException ignore ) { }
                        continue;
                    }
                    throw e;
                }
            }

            timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE*10L);
            while( timeout > System.currentTimeMillis() ) {
                if( vm == null || VmState.TERMINATED.equals(vm.getCurrentState()) ) {
                    break;
                }
                try { Thread.sleep(15000L); }
                catch( InterruptedException ignore ) { }
                try { vm = getVirtualMachine(vmId); }
                catch( Throwable ignore ) { }
            }

            resourceDir = HOSTED_SERVICES + "/" + serviceName;
            timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);
            while( timeout > System.currentTimeMillis() ) {
                try{
                    if( logger.isInfoEnabled() ) {
                        logger.info("Deleting hosted service " + serviceName);
                    }
                    method.invoke("DELETE", ctx.getAccountNumber(), resourceDir, "");
                    return;
                }
                catch( CloudException e ) {
                    if( e.getProviderCode() != null && e.getProviderCode().equals("ConflictError") ) {
                        logger.warn("Conflict error, maybe retrying in 30 seconds");
                        try { Thread.sleep(30000L); }
                        catch( InterruptedException ignore ) { }
                        continue;
                    }
                    logger.warn("Unable to delete hosted service for " + serviceName + ": " + e.getMessage());
                    return;
                }
                catch( Throwable t ) {
                    logger.warn("Unable to delete hosted service for " + serviceName + ": " + t.getMessage());
                    return;
                }
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".terminate()");
            }
        }
    }

    @Override
    public void unpause(@Nonnull String vmId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Pause/unpause is not supported in Microsoft Azure");
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }

    private @Nonnull String toUniqueId(@Nonnull String name) throws CloudException, InternalException {
        name = name.toLowerCase().replaceAll(" ", "");

        String id = name;
        int i = 0;

        while( getVirtualMachine(id) != null ) {
            i++;
            id = name + "-" + i;
        }
        return id;
    }
}
