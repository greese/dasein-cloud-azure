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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
    static private final Logger logger = Azure.getLogger(AzureVM.class);

    static private final String HOSTED_SERVICES = "/services/hostedservices";
    
    /**
     * As the operation for VM requires to the service name and virtual machine name
     * , therefore to handle the VM easily, the virtual machine id is equal to
     * "hostedservice name" + "_&_" + "virtual machine name"
     */
    static public final String SERVICE_VM_NAME_SPLIT = "_&_";
    static private final String VM_ROLE_SERVICES= "/roleInstances";
    static private final String SERVICE_NAME_KEY = "Servcie_NAME";


    private Azure provider;

    public AzureVM(Azure provider) { this.provider = provider; }

    @Override
    public void start(@Nonnull String vmId) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".Boot()");
        }
        
        if(!vmId.contains(SERVICE_VM_NAME_SPLIT)){
        	logger.trace(" No such VM");
        	return;
        }
       	String[] serviceVMName = vmId.split(SERVICE_VM_NAME_SPLIT);
     	String resourceDir = HOSTED_SERVICES + "/" + serviceVMName[0] + "/deployments" + "/" +  serviceVMName[0] + VM_ROLE_SERVICES+"/" + serviceVMName[1] + "/Operations";
     	
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
          	method.post(provider.getContext().getAccountNumber(), resourceDir, xml.toString());
        	
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

    private @Nonnull Iterable<VirtualMachine> getVirtualMachines(String serviceName) throws InternalException, CloudException {
       ArrayList<VirtualMachine> list = new ArrayList<VirtualMachine>();
       
       for( VirtualMachine vm : toVirtualMachines(provider.getContext(), serviceName)) {
        	if( vm.getProviderVirtualMachineId().startsWith(serviceName) ) {
            	list.add(vm);
            }
        }      
        return list;
    }
    
    private @Nullable VirtualMachine getVirtualMachine(@Nonnull String vmId, String serviceName) throws InternalException, CloudException {
    	 
    	 if(serviceName == null){
    		return this.getVirtualMachine(vmId);
    	 }
    	 for( VirtualMachine vm : getVirtualMachines(serviceName)) {        	
            if( vmId.equals(vm.getProviderVirtualMachineId()) ) {
                return vm;
            }
        }
        return null;
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
    public @Nullable VirtualMachine getVirtualMachine(@Nonnull String vmId, @Nonnull String serviceName,  @Nullable String user, @Nullable String password) throws InternalException, CloudException {
        VirtualMachine vm = getVirtualMachine(vmId,serviceName);
        
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
            
            String VMId =  id + SERVICE_VM_NAME_SPLIT+ id + "Role";
            VirtualMachine vm = null ;
            int atemptTime = 10;
            try {            	
            	while(atemptTime >0){
            		vm = getVirtualMachine(VMId, id, "dasein", password);
                	if(vm == null){            	
    					Thread.sleep(10000L);
    					atemptTime--;
                	}else{
                		return vm;
                	}
            	}
            } catch (InterruptedException e) {
            	logger.trace("interupt error while wating the VM to start: " + e.getMessage());
            	atemptTime--;
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
        // TODO: implement me
        return null;
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
            Node entry = entries.item(i);
            ArrayList<VirtualMachine> vmRoles = (ArrayList<VirtualMachine>) toVirtualMachines(ctx, entry);

            if( vmRoles != null ) {
                vms.addAll(vmRoles);
            }
        }
        return vms;
    }

    @Override
    public void stop(@Nonnull String vmId) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".Boot()");
        }
        
        if(!vmId.contains(SERVICE_VM_NAME_SPLIT)){
        	logger.trace(" No such VM");
        	return;
        }
       	String[] serviceVMName = vmId.split(SERVICE_VM_NAME_SPLIT);
     	String resourceDir = HOSTED_SERVICES + "/" + serviceVMName[0] + "/deployments" + "/" +  serviceVMName[0] + VM_ROLE_SERVICES+"/" + serviceVMName[1] + "/Operations";
     	
        try{
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
          	method.post(provider.getContext().getAccountNumber(), resourceDir, xml.toString());
        	
        }finally {
        	if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".launch()");
            }
        }
    }

    @Override
    public void reboot(@Nonnull String vmId) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".reboot()");
        }
        
        if(!vmId.contains(SERVICE_VM_NAME_SPLIT)){
        	logger.trace(" No such VM");
        	return;
        }
       	String[] serviceVMName = vmId.split(SERVICE_VM_NAME_SPLIT);
     	String resourceDir = HOSTED_SERVICES + "/" + serviceVMName[0] + "/deployments" + "/" +  serviceVMName[0] + VM_ROLE_SERVICES+"/" + serviceVMName[1] + "/Operations";
     	
        try{
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
          	method.post(provider.getContext().getAccountNumber(), resourceDir, xml.toString());
        	
        }finally {
        	if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".reboot()");
            }
        }
    
    }

    @Override
    public void resume(@Nonnull String vmId) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void pause(@Nonnull String vmId) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsAnalytics() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsPauseUnpause(@Nonnull VirtualMachine vm) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsStartStop(@Nonnull VirtualMachine vm) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsSuspendResume(@Nonnull VirtualMachine vm) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void suspend(@Nonnull String vmId) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void terminate(@Nonnull String vmId) throws InternalException, CloudException {
    	if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".terminate()");
        }
        
        if(!vmId.contains(SERVICE_VM_NAME_SPLIT)){
        	logger.trace(" No such VM");
        	return;
        }
       	String[] serviceVMName = vmId.split(SERVICE_VM_NAME_SPLIT);     	
       	String resourceDir = HOSTED_SERVICES + "/" + serviceVMName[0] + "/deployments" + "/" +  serviceVMName[0] + "/roles"+"/" + serviceVMName[1] ;
     	
        ArrayList<VirtualMachine> list = (ArrayList<VirtualMachine>) getVirtualMachines(serviceVMName[0]);
       	if(list == null ){
       		logger.trace(" No VM found under the services !!!!");       		
       	}else{
       		if(list.size()== 1){
       			logger.trace(" Directly delete deployment " + serviceVMName[0] + "/deployments" + "/" +  serviceVMName[0]);
       			resourceDir = HOSTED_SERVICES + "/" + serviceVMName[0]+ "/deployments" + "/" +  serviceVMName[0] ;
       		}
       	}        
        try{        	
            AzureMethod method = new AzureMethod(provider);
           
          	method.invoke("DELETE", provider.getContext().getAccountNumber(), resourceDir, null);
        	
        }finally {
        	if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".terminate()");
            }
        }    
    }

    @Override
    public void unpause(@Nonnull String vmId) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Nonnull
    @Override
    public String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
    
      
    private @Nullable Iterable<VirtualMachine> toVirtualMachines(@Nonnull ProviderContext ctx, String serviceName) throws CloudException, InternalException {
       
        String regionId = ctx.getRegionId();

        if( regionId == null ) {
            throw new AzureConfigException("No region ID was specified for this request");
        }
        
        ArrayList<VirtualMachine> list = new ArrayList<VirtualMachine>();
        
        ArrayList<VirtualMachine> tempList = new ArrayList<VirtualMachine>();       	
    	
        VirtualMachine vm = new VirtualMachine();

        NodeList attributes;
        HashMap<String,String> tags = new HashMap<String, String>();
          
        if( serviceName == null ) { 
            return null;
        }
        AzureMethod method = new AzureMethod(provider);
        
        String resourceDir =  HOSTED_SERVICES + "/"+ serviceName + "/deployments" + "/" + serviceName;
        Document doc = method.getAsXML(ctx.getAccountNumber(),resourceDir);

        if( doc == null ) {
            return null;
        }
        NodeList entries = doc.getElementsByTagName("Deployment");

        if( entries.getLength() < 1 ) {
            return null;
        }      
        
        for( int i=0; i<entries.getLength(); i++ ) {
            Node detail = entries.item(i);

            attributes = detail.getChildNodes();
            for( int j=0; j<attributes.getLength(); j++ ) {
                Node attribute = attributes.item(j);
                if(attribute.getNodeType() == Node.TEXT_NODE) continue;
                
                if( attribute.getNodeName().equalsIgnoreCase("deploymentslot") && attribute.hasChildNodes() ) {
                    tags.put("DeploymentSlot", attribute.getFirstChild().getNodeValue().trim());
                }
                else if( attribute.getNodeName().equalsIgnoreCase("privateid") && attribute.hasChildNodes() ) {
                    tags.put("DeploymentID", attribute.getFirstChild().getNodeValue().trim());
                }
                else if( attribute.getNodeName().equalsIgnoreCase("status") && attribute.hasChildNodes() ) {
                   // status = attribute.getFirstChild().getNodeValue().trim();
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
                        if(roleInstance.getNodeType() == Node.TEXT_NODE) continue;
                       
                        if( roleInstance.getNodeName().equalsIgnoreCase("roleinstance") && roleInstance.hasChildNodes() ) {
                        	VirtualMachine role = new VirtualMachine();
 
                        	role.setArchitecture(Architecture.I64);
                        	role.setClonable(false);
                        	role.setCurrentState(VmState.TERMINATED);
                        	role.setImagable(false);
                        	role.setPersistent(true);
                        	role.setPlatform(Platform.UNKNOWN);
                        	role.setProviderDataCenterId(regionId + "-a");
                        	role.setProviderOwnerId(ctx.getAccountNumber());
                        	role.setProviderRegionId(regionId);
                        	
                        	//obtain value from vm
                        	role.setCreationTimestamp(vm.getCreationTimestamp());
                        	role.setPublicDnsAddress(vm.getPublicDnsAddress());
                        	
                        	NodeList roleAttributes = roleInstance.getChildNodes();
                            
                            for( int l=0; l<roleAttributes.getLength(); l++ ) {
                                Node roleAttribute = roleAttributes.item(l);                               
                                if(roleAttribute.getNodeType() == Node.TEXT_NODE) continue;
                                
                                if( roleAttribute.getNodeName().equalsIgnoreCase("RoleName") && roleAttribute.hasChildNodes() ) {
                                  
                                    String vmId  = serviceName + SERVICE_VM_NAME_SPLIT + roleAttribute.getFirstChild().getNodeValue().trim();                                   
                                    
                                    role.setProviderVirtualMachineId(vmId);
                                    role.setName(roleAttribute.getFirstChild().getNodeValue().trim());
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("InstanceStatus") && roleAttribute.hasChildNodes() ) {
                                	//TODO
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("instancesize") && roleAttribute.hasChildNodes() ) {
                                	role.setProductId(roleAttribute.getFirstChild().getNodeValue().trim());
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
                                	role.setPrivateDnsAddress(roleAttribute.getFirstChild().getNodeValue().trim());
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("ipaddress") && roleAttribute.hasChildNodes() ) {
                                	role.setPrivateIpAddresses(new String[] { roleAttribute.getFirstChild().getNodeValue().trim() });
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("PowerState") && roleAttribute.hasChildNodes() ) {
                                	String powerStatus = roleAttribute.getFirstChild().getNodeValue().trim();
                                	
                                	if("Started".equalsIgnoreCase(powerStatus)){
                                		role.setCurrentState(VmState.RUNNING);                                		
                                	}
                                	else if("Stopped".equalsIgnoreCase(powerStatus)){
                                		role.setCurrentState(VmState.PAUSED);  
                                	}
                                }                               
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
                            role.setTags(tags);
                            
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
                                        return null;
                                    }
                                }
                            }
                            NodeList roleAttributes = role.getChildNodes();                         
                            VirtualMachine roleVM = new VirtualMachine();
                            for( int l=0; l<roleAttributes.getLength(); l++ ) {
                                Node roleAttribute = roleAttributes.item(l);
                                if(roleAttribute.getNodeType() == Node.TEXT_NODE) continue;
                                
                                if( roleAttribute.getNodeName().equalsIgnoreCase("osvirtualharddisk") && roleAttribute.hasChildNodes() ) {
                                    NodeList diskAttributes = roleAttribute.getChildNodes();

                                    for( int m=0; m<diskAttributes.getLength(); m++ ) {
                                        Node diskAttribute = diskAttributes.item(m);

                                        if( diskAttribute.getNodeName().equalsIgnoreCase("SourceImageName") && diskAttribute.hasChildNodes() ) {
                                        	roleVM.setProviderMachineImageId(diskAttribute.getFirstChild().getNodeValue().trim());
                                        }
                                        else if( diskAttribute.getNodeName().equalsIgnoreCase("MediaLink") && diskAttribute.hasChildNodes() ) {
                                        	roleVM.setTag(Azure.RESOURCE_MEDIA_LINK_KEY, diskAttribute.getFirstChild().getNodeValue().trim());
                                        } 
                                    }
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("RoleName") && roleAttribute.hasChildNodes() ) {
                                    
                                    String vmId  = serviceName + SERVICE_VM_NAME_SPLIT + roleAttribute.getFirstChild().getNodeValue().trim();
                                    roleVM.setProviderVirtualMachineId(vmId);                                    
                                }
                            }
                            tempList.add(roleVM); 
                        }                        
                    }                    
                }
            }
        }
        // Obtain the properties from tempList
        if(list != null && tempList != null){
        	for(VirtualMachine finalVM : list){
        		for(VirtualMachine tempVM : tempList){
        			if(finalVM.getProviderVirtualMachineId().equals(tempVM.getProviderVirtualMachineId())){
        				finalVM.setProviderMachineImageId(tempVM.getProviderMachineImageId());
        				finalVM.getTags().putAll(tempVM.getTags());
        				break;
        			}
        		}        		
        	}       	
        }        
        return list;
    }
    
    private @Nullable Iterable<VirtualMachine> toVirtualMachines(@Nonnull ProviderContext ctx, @Nullable Node entry) throws CloudException, InternalException {
        if( entry == null ) {
            return null;
        }
        String regionId = ctx.getRegionId();

        if( regionId == null ) {
            throw new AzureConfigException("No region ID was specified for this request");
        }
        
        ArrayList<VirtualMachine> list = new ArrayList<VirtualMachine>();
        
        ArrayList<VirtualMachine> tempList = new ArrayList<VirtualMachine>();       	
    	
        VirtualMachine vm = new VirtualMachine();

        NodeList attributes = entry.getChildNodes();
        String uri = null;
        String serviceName = null;
        
        HashMap<String,String> tags = new HashMap<String, String>();
        
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);            
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;

            if( attribute.getNodeName().equalsIgnoreCase("url") && attribute.hasChildNodes() ) {
                uri = attribute.getFirstChild().getNodeValue().trim();
                
            }
            else if( attribute.getNodeName().equalsIgnoreCase("servicename") && attribute.hasChildNodes() ) {
            	//vm.setProviderVirtualMachineId(attribute.getFirstChild().getNodeValue().trim());
            	serviceName = attribute.getFirstChild().getNodeValue().trim();
            	tags.put(SERVICE_NAME_KEY, serviceName);
            }
            else if( attribute.getNodeName().equalsIgnoreCase("hostedserviceproperties") && attribute.hasChildNodes() ) {
                NodeList properties = attribute.getChildNodes();
                
                for( int j=0; j<properties.getLength(); j++ ) {
                    Node property = properties.item(j);
                    if(property.getNodeType() == Node.TEXT_NODE) continue;
                    
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
        if( serviceName == null ) { 
            return null;
        }
        AzureMethod method = new AzureMethod(provider);
        
        String resourceDir =  HOSTED_SERVICES + "/"+ serviceName + "/deployments" + "/" + serviceName;
        Document doc = method.getAsXML(ctx.getAccountNumber(),resourceDir);

        if( doc == null ) {
            return null;
        }
        NodeList entries = doc.getElementsByTagName("Deployment");

        if( entries.getLength() < 1 ) {
            return null;
        }      
        
        for( int i=0; i<entries.getLength(); i++ ) {
            Node detail = entries.item(i);

            attributes = detail.getChildNodes();
            for( int j=0; j<attributes.getLength(); j++ ) {
                Node attribute = attributes.item(j);
                if(attribute.getNodeType() == Node.TEXT_NODE) continue;
                
                if( attribute.getNodeName().equalsIgnoreCase("deploymentslot") && attribute.hasChildNodes() ) {
                    tags.put("DeploymentSlot", attribute.getFirstChild().getNodeValue().trim());
                }
                else if( attribute.getNodeName().equalsIgnoreCase("privateid") && attribute.hasChildNodes() ) {
                    tags.put("DeploymentID", attribute.getFirstChild().getNodeValue().trim());
                }
                else if( attribute.getNodeName().equalsIgnoreCase("status") && attribute.hasChildNodes() ) {
                   // status = attribute.getFirstChild().getNodeValue().trim();
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
                        if(roleInstance.getNodeType() == Node.TEXT_NODE) continue;
                       
                        if( roleInstance.getNodeName().equalsIgnoreCase("roleinstance") && roleInstance.hasChildNodes() ) {
                        	VirtualMachine role = new VirtualMachine();
 
                        	role.setArchitecture(Architecture.I64);
                        	role.setClonable(false);
                        	role.setCurrentState(VmState.TERMINATED);
                        	role.setImagable(false);
                        	role.setPersistent(true);
                        	role.setPlatform(Platform.UNKNOWN);
                        	role.setProviderDataCenterId(regionId + "-a");
                        	role.setProviderOwnerId(ctx.getAccountNumber());
                        	role.setProviderRegionId(regionId);
                        	
                        	//obtain value from vm
                        	role.setCreationTimestamp(vm.getCreationTimestamp());
                        	role.setPublicDnsAddress(vm.getPublicDnsAddress());
                        	
                        	NodeList roleAttributes = roleInstance.getChildNodes();
                            
                            for( int l=0; l<roleAttributes.getLength(); l++ ) {
                                Node roleAttribute = roleAttributes.item(l);                               
                                if(roleAttribute.getNodeType() == Node.TEXT_NODE) continue;
                                
                                if( roleAttribute.getNodeName().equalsIgnoreCase("RoleName") && roleAttribute.hasChildNodes() ) {
                                  
                                    String vmId  = serviceName + SERVICE_VM_NAME_SPLIT + roleAttribute.getFirstChild().getNodeValue().trim();                                   
                                    
                                    role.setProviderVirtualMachineId(vmId);
                                    role.setName(roleAttribute.getFirstChild().getNodeValue().trim());
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("InstanceStatus") && roleAttribute.hasChildNodes() ) {
                                	//TODO
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("instancesize") && roleAttribute.hasChildNodes() ) {
                                	role.setProductId(roleAttribute.getFirstChild().getNodeValue().trim());
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
                                	role.setPrivateDnsAddress(roleAttribute.getFirstChild().getNodeValue().trim());
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("ipaddress") && roleAttribute.hasChildNodes() ) {
                                	role.setPrivateIpAddresses(new String[] { roleAttribute.getFirstChild().getNodeValue().trim() });
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("PowerState") && roleAttribute.hasChildNodes() ) {
                                	String powerStatus = roleAttribute.getFirstChild().getNodeValue().trim();
                                	
                                	if("Started".equalsIgnoreCase(powerStatus)){
                                		role.setCurrentState(VmState.RUNNING);                                		
                                	}
                                	else if("Stopped".equalsIgnoreCase(powerStatus)){
                                		role.setCurrentState(VmState.PAUSED);  
                                	}
                                }                               
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
                            role.setTags(tags);
                            
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
                                        return null;
                                    }
                                }
                            }
                            NodeList roleAttributes = role.getChildNodes();                         
                            VirtualMachine roleVM = new VirtualMachine();
                            for( int l=0; l<roleAttributes.getLength(); l++ ) {
                                Node roleAttribute = roleAttributes.item(l);
                                if(roleAttribute.getNodeType() == Node.TEXT_NODE) continue;
                                
                                if( roleAttribute.getNodeName().equalsIgnoreCase("osvirtualharddisk") && roleAttribute.hasChildNodes() ) {
                                    NodeList diskAttributes = roleAttribute.getChildNodes();

                                    for( int m=0; m<diskAttributes.getLength(); m++ ) {
                                        Node diskAttribute = diskAttributes.item(m);

                                        if( diskAttribute.getNodeName().equalsIgnoreCase("SourceImageName") && diskAttribute.hasChildNodes() ) {
                                        	roleVM.setProviderMachineImageId(diskAttribute.getFirstChild().getNodeValue().trim());
                                        }
                                        else if( diskAttribute.getNodeName().equalsIgnoreCase("MediaLink") && diskAttribute.hasChildNodes() ) {
                                        	roleVM.setTag(Azure.RESOURCE_MEDIA_LINK_KEY, diskAttribute.getFirstChild().getNodeValue().trim());
                                        } 
                                    }
                                }
                                else if( roleAttribute.getNodeName().equalsIgnoreCase("RoleName") && roleAttribute.hasChildNodes() ) {
                                    
                                    String vmId  = serviceName + SERVICE_VM_NAME_SPLIT + roleAttribute.getFirstChild().getNodeValue().trim();
                                    roleVM.setProviderVirtualMachineId(vmId);                                    
                                }
                            }
                            tempList.add(roleVM); 
                        }                        
                    }                    
                }
            }
        }
        // Obtain the properties from tempList
        if(list != null && tempList != null){
        	for(VirtualMachine finalVM : list){
        		for(VirtualMachine tempVM : tempList){
        			if(finalVM.getProviderVirtualMachineId().equals(tempVM.getProviderVirtualMachineId())){
        				finalVM.setProviderMachineImageId(tempVM.getProviderMachineImageId());
        				finalVM.getTags().putAll(tempVM.getTags());
        				break;
        			}
        		}        		
        	}       	
        }        
        return list;
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
