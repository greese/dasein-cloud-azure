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

package org.dasein.cloud.azure.compute.vm;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.AzureService;
import org.dasein.cloud.azure.compute.image.AzureMachineImage;
import org.dasein.cloud.compute.*;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.network.RawAddress;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Megabyte;
import org.dasein.util.uom.storage.Storage;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.TimePeriod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Implements virtual machine support for Microsoft Azure.
 * @author George Reese (george.reese@imaginary.com)
 * @author Qunying Huang
 * @since 2012.04.1 initial version
 * @version 2012.04.1
 * @version 2012.09 updated for model changes
 */
public class AzureVM extends AbstractVMSupport {
    static private final Logger logger = Azure.getLogger(AzureVM.class);

    static public final String HOSTED_SERVICES = "/services/hostedservices";

    private Azure provider;

    public AzureVM(Azure provider) {
        super(provider);
        this.provider = provider;
    }

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
        String serviceName, deploymentName, roleName;

        serviceName = vm.getTag("serviceName").toString();
        deploymentName = vm.getTag("deploymentName").toString();
        roleName = vm.getTag("roleName").toString();

        String resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  deploymentName + "/roleInstances/" + roleName + "/Operations";
        logger.debug("_______________________________________________________");
        logger.debug("Start operation - "+resourceDir);

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

            logger.debug(xml);
            logger.debug("___________________________________________________");
          	method.post(ctx.getAccountNumber(), resourceDir, xml.toString());
        	
        }finally {
        	if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".launch()");
            }
        }
    }

    @Override
    public VirtualMachine alterVirtualMachine(@Nonnull String vmId, @Nonnull VMScalingOptions options) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".alterVM()");
        }

        if (vmId == null || options.getProviderProductId() == null) {
            throw new AzureConfigException("No vmid and/or product id set for this operation");
        }

        String[] parts = options.getProviderProductId().split(":");
        String productId = null;
        String disks = "";
        if (parts.length == 1)  {
            productId=parts[0];
        }
        else if (parts.length == 2) {
            productId = parts[0];
            disks = parts[1].replace("[","").replace("]","");
        }
        else {
            throw new InternalException("Invalid product id string. Product id format is PRODUCT_NAME or PRODUCT_NAME:[disk_0_size,disk_1_size,disk_n_size]");
        }
        //check the product id is legitimate
        boolean found = false;
        Iterable<VirtualMachineProduct> products = listProducts(Architecture.I64);
        for (VirtualMachineProduct p : products) {
            if (p.getProviderProductId().equals(productId)) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new InternalException("Product id invalid: should be one of ExtraSmall, Small, Medium, Large, ExtraLarge");
        }

        String[] diskSizes = disks.split(",");

        VirtualMachine vm = getVirtualMachine(vmId);

        if( vm == null ) {
            throw new CloudException("No such virtual machine: " + vmId);
        }
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was set for this request");
        }
        String serviceName, deploymentName, roleName;

        serviceName = vm.getTag("serviceName").toString();
        deploymentName = vm.getTag("deploymentName").toString();
        roleName = vm.getTag("roleName").toString();

        String resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  deploymentName + "/roles/" + roleName;

        try{
            AzureMethod method = new AzureMethod(provider);

            Document doc = method.getAsXML(ctx.getAccountNumber(), resourceDir);
            StringBuilder xml = new StringBuilder();

            NodeList roles = doc.getElementsByTagName("PersistentVMRole");
            Node role = roles.item(0);

            NodeList entries = role.getChildNodes();
            boolean changeProduct = false;

            for (int i = 0; i<entries.getLength(); i++) {
                Node vn = entries.item(i);
                String vnName = vn.getNodeName();

                if( vnName.equalsIgnoreCase("RoleSize") && vn.hasChildNodes() ) {
                    if (!productId.equals(vn.getFirstChild().getNodeValue())) {
                        vn.getFirstChild().setNodeValue(productId);
                        changeProduct=true;
                    }
                    else {
                        logger.info("No product change required");
                    }
                    break;
                }
            }

            String requestId=null;

            if (changeProduct) {
                String output="";
                try{
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    StringWriter writer = new StringWriter();
                    transformer.transform(new DOMSource(doc), new StreamResult(writer));
                    output = writer.getBuffer().toString().replaceAll("\n|\r", "");
                }
                catch (Exception e){
                    System.err.println(e);
                }
                xml.append(output);

                logger.debug(xml);
                logger.debug("___________________________________________________");

                resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  deploymentName + "/roles/" + roleName;
                requestId = method.invoke("PUT", ctx.getAccountNumber(), resourceDir, xml.toString());
            }
            else {
                requestId="noChange";
            }

            if (requestId != null) {
                int httpCode = -1;
                if (!requestId.equals("noChange")) {
                    httpCode = method.getOperationStatus(requestId);
                    while (httpCode == -1) {
                        try {
                            Thread.sleep(15000L);
                        }
                        catch (InterruptedException ignored){}
                        httpCode = method.getOperationStatus(requestId);
                    }
                }
                if (httpCode == HttpServletResponse.SC_OK || requestId.equals("noChange")) {

                    String storageEndpoint = provider.getStorageEndpoint();
                    if( storageEndpoint == null || storageEndpoint.isEmpty()) {
                        throw new CloudException("Cannot find blob storage endpoint in the current region");
                    }

                    //attach any disks as appropriate
                    for (int i = 0; i < diskSizes.length; i++) {
                        if (!diskSizes[i].equals("")) {
                            xml = new StringBuilder();
                            xml.append("<DataVirtualHardDisk  xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
                            xml.append("<HostCaching>ReadWrite</HostCaching>");
                            xml.append("<LogicalDiskSizeInGB>").append(diskSizes[i]).append("</LogicalDiskSizeInGB>");
                            xml.append("<MediaLink>").append(storageEndpoint).append("vhds/").append(roleName).append(System.currentTimeMillis()%10000).append(".vhd</MediaLink>");
                            xml.append("</DataVirtualHardDisk>");
                            logger.debug(xml);
                            resourceDir = HOSTED_SERVICES + "/" +serviceName+ "/deployments" + "/" +  deploymentName + "/roles"+"/" + roleName+ "/DataDisks";
                            requestId = method.post(ctx.getAccountNumber(), resourceDir, xml.toString());
                            if (requestId != null) {
                                httpCode = method.getOperationStatus(requestId);
                                while (httpCode == -1) {
                                    try {
                                        Thread.sleep(15000L);
                                    }
                                    catch (InterruptedException ignored){}
                                    httpCode = method.getOperationStatus(requestId);
                                }
                            }
                        }
                    }
                }
            }

            return getVirtualMachine(vmId);

        }finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".alterVM()");
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

    private transient volatile VMCapabilities capabilities;
    @Nonnull
    @Override
    public VirtualMachineCapabilities getCapabilities() throws InternalException, CloudException {
        if( capabilities == null ) {
            capabilities = new VMCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    public @Nonnull String getConsoleOutput(@Nonnull String vmId) throws InternalException, CloudException {
        return "";
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
    public @Nullable VirtualMachine getVirtualMachine(@Nonnull String vmId) throws InternalException, CloudException {
        String[] parts = vmId.split(":");
        String sName, deploymentName, roleName;

        if (parts.length == 3)    {
            sName = parts[0];
            deploymentName = parts[1];
            roleName= parts[2];
        }
        else if( parts.length == 2 ) {
            sName = parts[0];
            deploymentName = parts[1];
            roleName = sName;
        }
        else {
            sName = vmId;
            deploymentName = vmId;
            roleName = vmId;
        }
        DataCenter dc = null;
        AffinityGroup ag = null;
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), HOSTED_SERVICES + "/"+ sName+"?embed-detail=true");
        if (doc == null) {
            return null;
        }
        NodeList entries = doc.getElementsByTagName("HostedService");
        for (int h = 0; h < entries.getLength(); h++) {
            Node entry = entries.item(h);
            NodeList attributes = entry.getChildNodes();

            boolean mediaLocationFound = false;

            for( int i=0; i<attributes.getLength(); i++ ) {
                Node attribute = attributes.item(i);

                if(attribute.getNodeType() == Node.TEXT_NODE) {
                    continue;
                }
                if( attribute.getNodeName().equalsIgnoreCase("hostedserviceproperties") && attribute.hasChildNodes() ) {
                    NodeList properties = attribute.getChildNodes();

                    for( int j=0; j<properties.getLength(); j++ ) {
                        Node property = properties.item(j);

                        if(property.getNodeType() == Node.TEXT_NODE) {
                            continue;
                        }
                        if( property.getNodeName().equalsIgnoreCase("AffinityGroup") && property.hasChildNodes() ) {
                            //get the region for this affinity group
                            String affinityGroup = property.getFirstChild().getNodeValue().trim();
                            if (affinityGroup != null && !affinityGroup.equals("")) {
                                ag = provider.getComputeServices().getAffinityGroupSupport().get(affinityGroup);
                                if(ag == null)
                                    return null;

                                dc = provider.getDataCenterServices().getDataCenter(ag.getDataCenterId());
                                if (dc != null && dc.getRegionId().equals(ctx.getRegionId())) {
                                    mediaLocationFound = true;
                                }
                                else {
                                    // not correct region/datacenter
                                    return null;
                                }
                            }
                        }
                        else if( property.getNodeName().equalsIgnoreCase("location") && property.hasChildNodes() ) {
                            if( !mediaLocationFound && !ctx.getRegionId().equals(property.getFirstChild().getNodeValue().trim()) ) {
                                return null;
                            }
                        }
                    }
                }
            }

        }

        ArrayList<VirtualMachine> list = new ArrayList<VirtualMachine>();
        NodeList deployments = doc.getElementsByTagName("Deployments");
        for (int i = 0; i < deployments.getLength(); i++) {
            Node deployNode = deployments.item(i);
            NodeList deployAttributes = deployNode.getChildNodes();

            String depName = "";
            for (int j = 0; j<deployAttributes.getLength(); j++) {
                Node deployment = deployAttributes.item(j);


                if(deployment.getNodeType() == Node.TEXT_NODE) {
                    continue;
                }

                if( deployment.getNodeName().equalsIgnoreCase("Deployment") && deployment.hasChildNodes() ) {
                    NodeList dAttribs = deployment.getChildNodes();
                    for (int k = 0; k<dAttribs.getLength(); k++) {
                        Node mynode = dAttribs.item(k);

                        if ( mynode.getNodeName().equalsIgnoreCase("name") && mynode.hasChildNodes() ) {
                            depName = mynode.getFirstChild().getNodeValue().trim();
                            if (depName.equals(deploymentName)) {
                                parseDeployment(ctx, ctx.getRegionId(), sName + ":" + deploymentName, deployment, list);
                                if (list != null && list.size() > 0) {
                                    VirtualMachine vm = list.get(0);
                                    if (dc != null) {
                                        vm.setProviderDataCenterId(dc.getProviderDataCenterId());
                                    }
                                    else {
                                        Collection<DataCenter> dcs = provider.getDataCenterServices().listDataCenters(ctx.getRegionId());
                                        vm.setProviderDataCenterId(dcs.iterator().next().getProviderDataCenterId());
                                    }
                                    if(ag != null)
                                    {
                                        vm.setAffinityGroupId(ag.getAffinityGroupId());
                                    }
                                    return vm;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable VmStatistics getVMStatistics(String vmId, long from, long to) throws InternalException, CloudException {
        return new VmStatistics();
    }

    @Override
    public @Nonnull Iterable<VmStatistics> getVMStatisticsForPeriod(@Nonnull String vmId, @Nonnegative long from, @Nonnegative long to) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return provider.getDataCenterServices().isSubscribed(AzureService.PERSISTENT_VM_ROLE);
    }

    @Override
    public @Nonnull VirtualMachine launch(VMLaunchOptions options) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".launch(" + options + ")");
        }
        try {
            String storageEndpoint = provider.getStorageEndpoint();

            if(storageEndpoint == null || storageEndpoint.isEmpty()) {
                provider.createDefaultStorageService();
                storageEndpoint = provider.getStorageEndpoint();
            }

            logger.debug("----------------------------------------------------------");
            logger.debug("launching vm "+options.getHostName()+" with machine image id: "+options.getMachineImageId());
            AzureMachineImage image = (AzureMachineImage)provider.getComputeServices().getImageSupport().getMachineImage(options.getMachineImageId());

            if( image == null ) {
                throw new CloudException("No such image: " + options.getMachineImageId());
            }
            logger.debug("----------------------------------------------------------");

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
            String hostName = toUniqueId(options.getHostName(), method, ctx);
            String deploymentSlot = (String)options.getMetaData().get("environment");

            String affinityGroupId = options.getAffinityGroupId();

            // Determine if this is a VM Image belonging to an account by examining the imageType metadata.
            // This value determines the launch parameters that are used.
            boolean isPrivateImage = false;
            if (options.getMetaData() != null) {
                Object imageType = options.getMetaData().get("imageType");
                if (imageType != null) {
                    isPrivateImage = imageType.toString().equalsIgnoreCase("private");
                }
            }

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
            if(affinityGroupId != null) {
                xml.append("<AffinityGroup>").append(affinityGroupId).append("</AffinityGroup>");
            }
            else{
                xml.append("<Location>").append(ctx.getRegionId()).append("</Location>");
            }
            xml.append("</CreateHostedService>");
            method.post(ctx.getAccountNumber(), HOSTED_SERVICES, xml.toString());

            String requestId = null;
            String password = null;
            try {
                xml = new StringBuilder();
                xml.append("<Deployment xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
                xml.append("<Name>").append(hostName).append("</Name>");
                xml.append("<DeploymentSlot>").append(deploymentSlot).append("</DeploymentSlot>");
                xml.append("<Label>").append(label).append("</Label>");
                xml.append("<RoleList>");
                xml.append("<Role>");
                xml.append("<RoleName>").append(hostName).append("</RoleName>");
                xml.append("<RoleType>PersistentVMRole</RoleType>");
                xml.append("<ConfigurationSets>");

                password = (options.getBootstrapPassword() == null ? provider.generateToken(8, 15) : options.getBootstrapPassword());

                if( image.getPlatform().isWindows() ) {
                    xml.append("<ConfigurationSet>");
                    xml.append("<ConfigurationSetType>WindowsProvisioningConfiguration</ConfigurationSetType>");
                    xml.append("<ComputerName>").append(hostName).append("</ComputerName>");
                    xml.append("<AdminPassword>").append(password).append("</AdminPassword>");
                    // A username of Administrator, Admin, and Admin1 causes provisioning to fail.
                    String username = (options.getBootstrapUser() == null || options.getBootstrapUser().trim().length() == 0 || options.getBootstrapUser().equalsIgnoreCase("Administrator") 
                            || options.getBootstrapUser().equalsIgnoreCase("Admin") || options.getBootstrapUser().equalsIgnoreCase("Admin1") ? "dasein" : options.getBootstrapUser());
                    xml.append("<AdminUsername>").append(username).append("</AdminUsername>");
                    xml.append("<EnableAutomaticUpdate>true</EnableAutomaticUpdate>");
                    xml.append("<TimeZone>UTC</TimeZone>");
                    xml.append("</ConfigurationSet>");
                }
                else {
                    xml.append("<ConfigurationSet>");
                    xml.append("<ConfigurationSetType>LinuxProvisioningConfiguration</ConfigurationSetType>");
                    xml.append("<HostName>").append(hostName).append("</HostName>");

                    //dmayne using root causes vm to fail provisioning
                    String username = (options.getBootstrapUser() == null || options.getBootstrapUser().trim().length() == 0 || options.getBootstrapUser().equals("root") 
                          ? "dasein" : options.getBootstrapUser());
                    xml.append("<UserName>").append(username).append("</UserName>");
                    xml.append("<UserPassword>").append(password).append("</UserPassword>");
                    xml.append("<DisableSshPasswordAuthentication>false</DisableSshPasswordAuthentication>");
                    xml.append("</ConfigurationSet>");
                }
                xml.append("<ConfigurationSet>");
                xml.append("<ConfigurationSetType>NetworkConfiguration</ConfigurationSetType>") ;
                xml.append("<InputEndpoints><InputEndpoint>");
                if( image.getPlatform().isWindows() ) {
                    xml.append("<LocalPort>3389</LocalPort>");
                    xml.append("<Name>RemoteDesktop</Name>");
                    xml.append("<Port>58622</Port>");
                }
                else {
                    xml.append("<LocalPort>22</LocalPort>");
                    xml.append("<Name>SSH</Name>");
                    xml.append("<Port>22</Port>");
                }
                xml.append("<Protocol>TCP</Protocol>");
                xml.append("</InputEndpoint></InputEndpoints>");
                //dmayne assuming this is a subnet
                Subnet subnet = null;
                String vlanName = null;
                if (options.getVlanId() != null) {
                    subnet = provider.getNetworkServices().getVlanSupport().getSubnet(options.getSubnetId());
                    if (subnet != null) {
                        xml.append("<SubnetNames>");
                        xml.append("<SubnetName>").append(subnet.getName()).append("</SubnetName>");
                        xml.append("</SubnetNames>");

                        //dmayne needed for virtual network name later
                        vlanName = subnet.getTags().get("vlanName");
                    }
                    else {
                        VLAN vlan = provider.getNetworkServices().getVlanSupport().getVlan(options.getVlanId());
                        if (vlan != null) {
                            vlanName = vlan.getName();
                        }
                    }
                }
                xml.append("</ConfigurationSet>");
                xml.append("</ConfigurationSets>");
                if (isPrivateImage) {
                   xml.append("<VMImageName>").append(options.getMachineImageId()).append("</VMImageName>");
                }
                else {
                   xml.append("<DataVirtualHardDisks/>");
                   xml.append("<OSVirtualHardDisk>");
                   xml.append("<HostCaching>ReadWrite</HostCaching>");
                   xml.append("<DiskLabel>OS</DiskLabel>");
                   xml.append("<MediaLink>").append(storageEndpoint).append("vhds/").append(hostName).append(".vhd</MediaLink>");
                   xml.append("<SourceImageName>").append(options.getMachineImageId()).append("</SourceImageName>");
                   xml.append("</OSVirtualHardDisk>");
                }
                xml.append("<RoleSize>").append(options.getStandardProductId()).append("</RoleSize>");
                xml.append("</Role>");
                xml.append("</RoleList>");

                if (options.getVlanId() != null) {
                    xml.append("<VirtualNetworkName>").append(vlanName).append("</VirtualNetworkName>");
                }
                xml.append("</Deployment>");

                requestId = method.post(ctx.getAccountNumber(), HOSTED_SERVICES + "/" + hostName + "/deployments", xml.toString());
            }
            catch (CloudException e) {
                logger.error("Launch server failed - now cleaning up service");
                String resourceDir = HOSTED_SERVICES + "/" + hostName;
                long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);
                while( timeout > System.currentTimeMillis() ) {
                    try{
                        if( logger.isInfoEnabled() ) {
                            logger.info("Deleting hosted service " + hostName);
                        }
                        method.invoke("DELETE", ctx.getAccountNumber(), resourceDir, "");
                        break;
                    }
                    catch( CloudException err ) {
                        logger.error("Unable to delete hosted service for " + hostName + ": " + err.getMessage());
                        logger.error("Retrying...");
                        try { Thread.sleep(30000L); }
                        catch( InterruptedException ignore ) { }
                        continue;
                    }
                }
                throw e;
            }

            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);
            VirtualMachine vm = null ;

            if (requestId != null) {
                int httpCode = method.getOperationStatus(requestId);
                while (httpCode == -1) {
                    try {
                        Thread.sleep(15000L);
                    }
                    catch (InterruptedException ignored){}
                    httpCode = method.getOperationStatus(requestId);
                }
                if (httpCode == HttpServletResponse.SC_OK) {
                    try { vm = getVirtualMachine(hostName + ":" + hostName+":"+hostName); }
                    catch( Throwable ignore ) { }
                    if( vm != null ) {
                        vm.setRootUser("dasein");
                        vm.setRootPassword(password);
                    }
                }
            }
            else {
                while( timeout > System.currentTimeMillis() ) {
                    try { vm = getVirtualMachine(hostName + ":" + hostName+":"+hostName); }
                    catch( Throwable ignore ) { }
                    if( vm != null ) {
                        vm.setRootUser("dasein");
                        vm.setRootPassword(password);
                        break;
                    }
                    try { Thread.sleep(15000L); }
                    catch( InterruptedException ignore ) { }
                }
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
    public @Nonnull Iterable<String> listFirewalls(@Nonnull String vmId) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    @Override
    public @Nonnull Iterable<VirtualMachineProduct> listProducts(@Nonnull Architecture architecture) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "listVMProducts");
        try {
            Cache<VirtualMachineProduct> cache = Cache.getInstance(getProvider(), "products" + architecture.name(), VirtualMachineProduct.class, CacheLevel.REGION, new TimePeriod<Day>(1, TimePeriod.DAY));
            Iterable<VirtualMachineProduct> products = cache.get(getContext());

            if( products == null ) {
                ArrayList<VirtualMachineProduct> list = new ArrayList<VirtualMachineProduct>();

                try {
                    String resource = ((Azure)getProvider()).getVMProductsResource();
                    InputStream input = AzureVM.class.getResourceAsStream(resource);

                    if( input != null ) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                        StringBuilder json = new StringBuilder();
                        String line;

                        while( (line = reader.readLine()) != null ) {
                            json.append(line);
                            json.append("\n");
                        }
                        JSONArray arr = new JSONArray(json.toString());
                        JSONObject toCache = null;

                        for( int i=0; i<arr.length(); i++ ) {
                            JSONObject productSet = arr.getJSONObject(i);
                            String cloud, provider;

                            if( productSet.has("cloud") ) {
                                cloud = productSet.getString("cloud");
                            }
                            else {
                                continue;
                            }
                            if( productSet.has("provider") ) {
                                provider = productSet.getString("provider");
                            }
                            else {
                                continue;
                            }
                            if( !productSet.has("products") ) {
                                continue;
                            }
                            if( toCache == null || (provider.equals("default") && cloud.equals("default")) ) {
                                toCache = productSet;
                            }
                            if( provider.equalsIgnoreCase(getProvider().getProviderName()) && cloud.equalsIgnoreCase(getProvider().getCloudName()) ) {
                                toCache = productSet;
                                break;
                            }
                        }
                        if( toCache == null ) {
                            logger.warn("No products were defined");
                            return Collections.emptyList();
                        }
                        JSONArray plist = toCache.getJSONArray("products");

                        for( int i=0; i<plist.length(); i++ ) {
                            JSONObject product = plist.getJSONObject(i);
                            boolean supported = false;

                            if( product.has("architectures") ) {
                                JSONArray architectures = product.getJSONArray("architectures");

                                for( int j=0; j<architectures.length(); j++ ) {
                                    String a = architectures.getString(j);

                                    if( architecture.name().equals(a) ) {
                                        supported = true;
                                        break;
                                    }
                                }
                            }
                            if( !supported ) {
                                continue;
                            }
                            if( product.has("excludesRegions") ) {
                                JSONArray regions = product.getJSONArray("excludesRegions");

                                for( int j=0; j<regions.length(); j++ ) {
                                    String r = regions.getString(j);

                                    if( r.equals(getContext().getRegionId()) ) {
                                        supported = false;
                                        break;
                                    }
                                }
                            }
                            if( !supported ) {
                                continue;
                            }
                            VirtualMachineProduct prd = toProduct(product);

                            if( prd != null ) {
                                list.add(prd);
                            }
                        }
                    }
                    else {
                        logger.warn("No standard products resource exists for " + resource);
                    }

                    products = list;
                    cache.put(getContext(), products);
                }
                catch( IOException e ) {
                    throw new InternalException(e);
                }
                catch( JSONException e ) {
                    throw new InternalException(e);
                }
            }
            return products;
        }
        finally {
            APITrace.end();
        }
    }

    @Nonnull
    @Override
    public Iterable<ResourceStatus> listVirtualMachineStatus() throws InternalException, CloudException {
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
        ArrayList<ResourceStatus> status = new ArrayList<ResourceStatus>();

        for( int i=0; i<entries.getLength(); i++ ) {
            parseHostedServiceForStatus(ctx, entries.item(i), null, status);
        }
        return status;
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

    @Nonnull
    @Override
    public Iterable<VirtualMachine> listVirtualMachines(@Nullable VMFilterOptions vmFilterOptions) throws InternalException, CloudException {
        Iterable<VirtualMachine> vms = listVirtualMachines();
        ArrayList<VirtualMachine> list = new ArrayList<VirtualMachine>();
        for (VirtualMachine vm : vms) {
            if (vm.getName().matches(vmFilterOptions.getRegex())) {
                list.add(vm);
            }
        }
        return list;
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
        String vlan = null;
        String subnetName = null;

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
                                role.setPrivateAddresses(new RawAddress[]{new RawAddress(roleAttribute.getFirstChild().getNodeValue().trim())});
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
                                                RawAddress[] ips = role.getPublicAddresses();

                                                if( ips == null || ips.length < 1 ) {
                                                    role.setPublicAddresses(new RawAddress[] {new RawAddress(addr)});
                                                }
                                                else {
                                                    boolean found = false;

                                                    for( RawAddress ip : ips ) {
                                                        if( ip.getIpAddress().equals(addr) ) {
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                    if( !found ) {
                                                        RawAddress[] tmp = new RawAddress[ips.length + 1];

                                                        System.arraycopy(ips, 0, tmp, 0, ips.length);
                                                        tmp[tmp.length-1] = new RawAddress(addr);
                                                        role.setPublicAddresses(tmp);
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
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("ConfigurationSets") && roleAttribute.hasChildNodes() ) {
                                NodeList configs = ((Element) roleAttribute).getElementsByTagName("ConfigurationSet");

                                for (int n = 0; n<configs.getLength();n++) {
                                    boolean foundNetConfig = false;
                                    Node config = configs.item(n);

                                    if( config.hasAttributes() ) {
                                        Node c = config.getAttributes().getNamedItem("i:type");

                                        if( c != null ) {
                                            String val = c.getNodeValue();

                                            if( !"NetworkConfigurationSet".equalsIgnoreCase(val) ) {
                                                continue;
                                            }
                                        }
                                    }

                                    if (config.hasChildNodes()) {
                                        NodeList configAttribs = config.getChildNodes();

                                        for (int o = 0; o<configAttribs.getLength();o++) {
                                            Node attrib = configAttribs.item(o);
                                            if (attrib.getNodeName().equalsIgnoreCase("SubnetNames")&& attrib.hasChildNodes()) {
                                                NodeList subnets = attrib.getChildNodes();

                                                for (int p=0;p<subnets.getLength();p++) {
                                                    Node subnet = subnets.item(p);
                                                    if (subnet.getNodeName().equalsIgnoreCase("SubnetName") && subnet.hasChildNodes()) {
                                                        subnetName = subnet.getFirstChild().getNodeValue().trim();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else if (attribute.getNodeName().equalsIgnoreCase("virtualnetworkname") && attribute.hasChildNodes() ) {
                vlan = attribute.getFirstChild().getNodeValue().trim();
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

                if (vlan != null) {
                    String providerVlanId = null;

                    try {
                        providerVlanId = provider.getNetworkServices().getVlanSupport().getVlan(vlan).getProviderVlanId();
                        vm.setProviderVlanId(providerVlanId);
                    }
                    catch (CloudException e) {
                        logger.error("Error getting vlan id for vlan "+vlan);
                        continue;
                    }
                    catch (InternalException ie){
                        logger.error("Error getting vlan id for vlan "+vlan);
                        continue;
                    }

                    if (subnetName != null) {
                        vm.setProviderSubnetId(subnetName + "_" + providerVlanId);
                    }
                }

                String[] parts = vm.getProviderVirtualMachineId().split(":");
                String sName, deploymentName, roleName;

                if (parts.length == 3)    {
                    sName = parts[0];
                    deploymentName = parts[1];
                    roleName= parts[2];
                }
                else if( parts.length == 2 ) {
                    sName = parts[0];
                    deploymentName = parts[1];
                    roleName = deploymentName;
                }
                else {
                    sName = serviceName;
                    deploymentName = serviceName;
                    roleName = serviceName;
                }
                vm.setTag("serviceName", sName);
                vm.setTag("deploymentName", deploymentName);
                vm.setTag("roleName", roleName);
                if( mediaLink != null ) {
                    vm.setTag("mediaLink", mediaLink);
                }
                virtualMachines.add(vm);
            }
        }
    }

    private void parseStatus(@Nonnull ProviderContext ctx, @Nonnull String regionId, @Nonnull String serviceName, @Nonnull Node node, @Nonnull List<ResourceStatus> status) {
        ArrayList<ResourceStatus> list = new ArrayList<ResourceStatus>();
        NodeList attributes = node.getChildNodes();
        String id = "";
        ResourceStatus s = null;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);

            if( attribute.getNodeType() == Node.TEXT_NODE) {
                continue;
            }
            if( attribute.getNodeName().equalsIgnoreCase("roleinstancelist") && attribute.hasChildNodes() ) {
                NodeList roleInstances = attribute.getChildNodes();

                for( int j=0; j<roleInstances.getLength(); j++ ) {
                    Node roleInstance = roleInstances.item(j);

                    if(roleInstance.getNodeType() == Node.TEXT_NODE) {
                        continue;
                    }
                    if( roleInstance.getNodeName().equalsIgnoreCase("roleinstance") && roleInstance.hasChildNodes() ) {
                        NodeList roleAttributes = roleInstance.getChildNodes();

                        for( int l=0; l<roleAttributes.getLength(); l++ ) {
                            Node roleAttribute = roleAttributes.item(l);

                            if( roleAttribute.getNodeType() == Node.TEXT_NODE ) {
                                continue;
                            }
                            if( roleAttribute.getNodeName().equalsIgnoreCase("RoleName") && roleAttribute.hasChildNodes() ) {
                                String vmId  = roleAttribute.getFirstChild().getNodeValue().trim();

                                id = serviceName + ":" + vmId;
                            }
                            else if( roleAttribute.getNodeName().equalsIgnoreCase("PowerState") && roleAttribute.hasChildNodes() ) {
                                String powerStatus = roleAttribute.getFirstChild().getNodeValue().trim();

                                if( "Started".equalsIgnoreCase(powerStatus)){
                                     s = new ResourceStatus(id, VmState.RUNNING);
                                }
                                else if( "Stopped".equalsIgnoreCase(powerStatus)){
                                     s = new ResourceStatus(id, VmState.STOPPED);
                                }
                                else if( "Stopping".equalsIgnoreCase(powerStatus)){
                                     s = new ResourceStatus(id, VmState.STOPPING);
                                }
                                else if( "Starting".equalsIgnoreCase(powerStatus)){
                                     s = new ResourceStatus(id, VmState.PENDING);
                                }
                                else {
                                    logger.warn("DEBUG: Unknown Azure status: " + powerStatus);
                                }
                            }
                        }
                        if( id == null ) {
                            continue;
                        }

                        if (s != null) {
                            list.add(s);
                            s = null;
                        }
                    }
                }
            }
        }
        for (ResourceStatus rs : list) {
            status.add(rs);
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

        boolean mediaLocationFound = false;
        DataCenter dc = null;

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
                    if( property.getNodeName().equalsIgnoreCase("AffinityGroup") && property.hasChildNodes() ) {
                        //get the region for this affinity group
                        String affinityGroup = property.getFirstChild().getNodeValue().trim();
                        if (affinityGroup != null && !affinityGroup.equals("")) {
                            AffinityGroup affinityGroupModel = provider.getComputeServices().getAffinityGroupSupport().get(affinityGroup);
                            if(affinityGroupModel == null)
                                return;

                            dc = provider.getDataCenterServices().getDataCenter(affinityGroupModel.getDataCenterId());
                            if (dc != null && dc.getRegionId().equals(regionId)) {
                                mediaLocationFound = true;
                            }
                            else {
                                // not correct region/datacenter
                                return;
                            }
                        }
                    }
                    else if( property.getNodeName().equalsIgnoreCase("location") && property.hasChildNodes() ) {
                        if( !mediaLocationFound && !regionId.equals(property.getFirstChild().getNodeValue().trim()) ) {
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

        //dmayne 20130416: get the deployment names for each hosted service so we can then extract the detail
        String deployURL = HOSTED_SERVICES + "/"+ service+"?embed-detail=true";
        Document deployDoc = method.getAsXML(ctx.getAccountNumber(), deployURL);

        if (deployDoc == null) {
            return;
        }
        NodeList deployments = deployDoc.getElementsByTagName("Deployments");
        for (int i = 0; i < deployments.getLength(); i++) {
            Node deployNode = deployments.item(i);
            NodeList deployAttributes = deployNode.getChildNodes();

            String deploymentName = "";
            for (int j = 0; j<deployAttributes.getLength(); j++) {
                Node deployment = deployAttributes.item(j);

                if(deployment.getNodeType() == Node.TEXT_NODE) {
                    continue;
                }

                if( deployment.getNodeName().equalsIgnoreCase("Deployment") && deployment.hasChildNodes() ) {
                    NodeList dAttribs = deployment.getChildNodes();
                    for (int k = 0; k<dAttribs.getLength(); k++) {
                        Node mynode = dAttribs.item(k);

                        if ( mynode.getNodeName().equalsIgnoreCase("name") && mynode.hasChildNodes() ) {
                            deploymentName = mynode.getFirstChild().getNodeValue().trim();

                            parseDeployment(ctx, regionId, service+":"+deploymentName, deployment, virtualMachines);
                            for (VirtualMachine vm : virtualMachines) {
                                if (vm.getCreationTimestamp() < 1L) {
                                    vm.setCreationTimestamp(created);
                                }
                                if (dc != null) {
                                    vm.setProviderDataCenterId(dc.getProviderDataCenterId());
                                }
                                else {
                                    Collection<DataCenter> dcs = provider.getDataCenterServices().listDataCenters(regionId);
                                    vm.setProviderDataCenterId(dcs.iterator().next().getProviderDataCenterId());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseHostedServiceForStatus(@Nonnull ProviderContext ctx, @Nonnull Node entry, @Nullable String serviceName, @Nonnull List<ResourceStatus> status) throws CloudException, InternalException {
        String regionId = ctx.getRegionId();

        if( regionId == null ) {
            throw new AzureConfigException("No region ID was specified for this request");
        }

        NodeList attributes = entry.getChildNodes();
        String uri = null;
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

                boolean mediaLocationFound = false;
                for( int j=0; j<properties.getLength(); j++ ) {
                    Node property = properties.item(j);

                    if(property.getNodeType() == Node.TEXT_NODE) {
                        continue;
                    }
                    if( property.getNodeName().equalsIgnoreCase("AffinityGroup") && property.hasChildNodes() ) {
                        //get the region for this affinity group
                        String affinityGroup = property.getFirstChild().getNodeValue().trim();
                        if (affinityGroup != null && !affinityGroup.equals("")) {
                            AffinityGroup affinityGroupModel = provider.getComputeServices().getAffinityGroupSupport().get(affinityGroup);
                            if(affinityGroupModel == null)
                                return;

                            DataCenter dc = provider.getDataCenterServices().getDataCenter(affinityGroupModel.getDataCenterId());
                            if (dc != null && dc.getRegionId().equals(regionId)) {
                                mediaLocationFound = true;
                            }
                            else {
                                // not correct region/datacenter
                                return;
                            }
                        }
                    }
                    else if( property.getNodeName().equalsIgnoreCase("location") && property.hasChildNodes() ) {
                        if( !mediaLocationFound && !regionId.equals(property.getFirstChild().getNodeValue().trim()) ) {
                            return;
                        }
                    }
                }
            }
        }
        if( uri == null || service == null ) {
            return;
        }

        AzureMethod method = new AzureMethod(provider);

        //dmayne 20130416: get the deployment names for each hosted service so we can then extract the detail
        String deployURL = HOSTED_SERVICES + "/"+ service+"?embed-detail=true";
        Document deployDoc = method.getAsXML(ctx.getAccountNumber(), deployURL);

        if (deployDoc == null) {
            return;
        }
        NodeList deployments = deployDoc.getElementsByTagName("Deployments");
        for (int i = 0; i < deployments.getLength(); i++) {
            Node deployNode = deployments.item(i);
            NodeList deployAttributes = deployNode.getChildNodes();

            String deploymentName = "";
            for (int j = 0; j<deployAttributes.getLength(); j++) {
                Node deployment = deployAttributes.item(j);

                if(deployment.getNodeType() == Node.TEXT_NODE) {
                    continue;
                }

                if( deployment.getNodeName().equalsIgnoreCase("Deployment") && deployment.hasChildNodes() ) {
                    NodeList dAttribs = deployment.getChildNodes();
                    for (int k = 0; k<dAttribs.getLength(); k++) {
                        Node mynode = dAttribs.item(k);

                        if ( mynode.getNodeName().equalsIgnoreCase("name") && mynode.hasChildNodes() ) {
                            deploymentName = mynode.getFirstChild().getNodeValue().trim();

                            parseStatus(ctx, regionId, service + ":" + deploymentName, deployment, status);
                        }
                    }
                }
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
            String serviceName, deploymentName, roleName;

            serviceName = vm.getTag("serviceName").toString();
            deploymentName = vm.getTag("deploymentName").toString();
            roleName = vm.getTag("roleName").toString();

            String resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  deploymentName + "/roleInstances/" + roleName + "/Operations";

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
    public void stop(@Nonnull String vmId, boolean force) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".Boot()");
        }
        try {
            // TODO: force vs not force
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was set for this request");
            }
            VirtualMachine vm = getVirtualMachine(vmId);

            if( vm == null ) {
                throw new CloudException("No such virtual machine: " + vmId);
            }
            String serviceName, deploymentName, roleName;

            serviceName = vm.getTag("serviceName").toString();
            deploymentName = vm.getTag("deploymentName").toString();
            roleName = vm.getTag("roleName").toString();

            String resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  deploymentName + "/roleInstances/" + roleName + "/Operations";
            logger.debug("__________________________________________________________");
            logger.debug("Stop vm "+resourceDir);

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
            logger.debug(xml);
            logger.debug("__________________________________________________________");
            method.post(ctx.getAccountNumber(), resourceDir, xml.toString());
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".launch()");
            }
        }
    }

    @Override
    public void suspend(@Nonnull String vmId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Suspend/resume is not supported in Microsoft Azure");
    }

    @Override
    public void terminate(@Nonnull String vmId, @Nullable String explanation) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVM.class.getName() + ".terminate()");
        }
        try {
            VirtualMachine vm = getVirtualMachine(vmId);

            if( vm == null ) {
                throw new CloudException("No such virtual machine: " + vmId);
            }

            ArrayList<String> disks = getAttachedDisks(vm);
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
            String serviceName, deploymentName;

            serviceName = vm.getTag("serviceName").toString();
            deploymentName = vm.getTag("deploymentName").toString();

            String resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  deploymentName;
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

            terminateService(serviceName, explanation);

            //now delete the orphaned disks
            for (String disk : disks) {
                provider.getComputeServices().getVolumeSupport().remove(disk);
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVM.class.getName() + ".terminate()");
            }
        }
    }

    public void terminateService(String serviceName, String explanation) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "VM.terminateService");
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was set for this request");
            }
            AzureMethod method = new AzureMethod(provider);
            String resourceDir = HOSTED_SERVICES + "/" + serviceName;
            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);
            while( timeout > System.currentTimeMillis() ) {
                try{
                    if( logger.isInfoEnabled() ) {
                        logger.info("Deleting hosted service " + serviceName+": "+explanation);
                    }
                    method.invoke("DELETE", ctx.getAccountNumber(), resourceDir, "");
                    break;
                }
                catch( CloudException e ) {
                    logger.error("Unable to delete hosted service for " + serviceName + ": " + e.getMessage());
                    logger.error("Retrying...");
                    try { Thread.sleep(30000L); }
                    catch( InterruptedException ignore ) { }
                    continue;
                }
                catch( Throwable t ) {
                    logger.warn("Unable to delete hosted service for " + serviceName + ": " + t.getMessage());
                    return;
                }
            }
        }
        finally {
            APITrace.end();
        }
    }


    private ArrayList<String> getAttachedDisks(VirtualMachine vm) throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was set for this request");
        }

        ArrayList<String> list = new ArrayList<String>();
        boolean diskFound = false;
        String serviceName, deploymentName;

        serviceName = vm.getTag("serviceName").toString();
        deploymentName = vm.getTag("deploymentName").toString();

        String resourceDir = HOSTED_SERVICES + "/" + serviceName + "/deployments/" +  deploymentName;
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(),resourceDir);

        NodeList entries = doc.getElementsByTagName("Deployment");
        for (int i = 0; i < entries.getLength(); i++) {
            Node entry = entries.item(i);
            NodeList attributes = entry.getChildNodes();

            for (int j = 0; j < attributes.getLength(); j++){
                Node attribute = attributes.item(j);

                if (attribute.getNodeName().equalsIgnoreCase("RoleList") && attribute.hasChildNodes()) {
                    NodeList instances = attribute.getChildNodes();

                    for (int k = 0; k <instances.getLength(); k++){
                        Node instance = instances.item(k);

                        if (instance.getNodeName().equalsIgnoreCase("Role") && instance.hasChildNodes()){
                            NodeList roles = instance.getChildNodes();

                            for (int l = 0; l<roles.getLength(); l++) {
                                Node role = roles.item(l);

                                if (role.getNodeName().equalsIgnoreCase("OSVirtualHardDisk") && role.hasChildNodes()) {
                                    NodeList disks = role.getChildNodes();

                                    for (int m = 0; m<disks.getLength(); m++) {
                                        Node disk = disks.item(m);

                                        if (disk.getNodeName().equalsIgnoreCase("DiskName") && disk.hasChildNodes()) {
                                            String name = disk.getFirstChild().getNodeValue();
                                            list.add(name);
                                            diskFound = true;
                                            break;
                                        }
                                    }
                                }
                                if (diskFound) {
                                    diskFound = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return list;
    }

    @Override
    public void unpause(@Nonnull String vmId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Pause/unpause is not supported in Microsoft Azure");
    }

    @Override
    public void updateTags(@Nonnull String vmId, @Nonnull Tag... tags) throws CloudException, InternalException {
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

    private @Nonnull String toUniqueId(@Nonnull String name, @Nonnull AzureMethod method, ProviderContext ctx) throws CloudException, InternalException {
        name = name.toLowerCase().replaceAll(" ", "");

        String id = name;
        int i = 0;

        while( method.getAsXML(ctx.getAccountNumber(), HOSTED_SERVICES+ "/"+id) != null ) {
            i++;
            id = name + "-" + i;
        }
        return id;
    }

    private @Nullable VirtualMachineProduct toProduct(@Nonnull JSONObject json) throws InternalException {
        VirtualMachineProduct prd = new VirtualMachineProduct();

        try {
            if( json.has("id") ) {
                prd.setProviderProductId(json.getString("id"));
            }
            else {
                return null;
            }
            if( json.has("name") ) {
                prd.setName(json.getString("name"));
            }
            else {
                prd.setName(prd.getProviderProductId());
            }
            if( json.has("description") ) {
                prd.setDescription(json.getString("description"));
            }
            else {
                prd.setDescription(prd.getName());
            }
            if( json.has("cpuCount") ) {
                prd.setCpuCount(json.getInt("cpuCount"));
            }
            else {
                prd.setCpuCount(1);
            }
            if( json.has("rootVolumeSizeInGb") ) {
                prd.setRootVolumeSize(new Storage<Gigabyte>(json.getInt("rootVolumeSizeInGb"), Storage.GIGABYTE));
            }
            else {
                prd.setRootVolumeSize(new Storage<Gigabyte>(1, Storage.GIGABYTE));
            }
            if( json.has("ramSizeInMb") ) {
                prd.setRamSize(new Storage<Megabyte>(json.getInt("ramSizeInMb"), Storage.MEGABYTE));
            }
            else {
                prd.setRamSize(new Storage<Megabyte>(512, Storage.MEGABYTE));
            }
            if( json.has("standardHourlyRates") ) {
                JSONArray rates = json.getJSONArray("standardHourlyRates");

                for( int i=0; i<rates.length(); i++ ) {
                    JSONObject rate = rates.getJSONObject(i);

                    if( rate.has("rate") ) {
                        prd.setStandardHourlyRate((float)rate.getDouble("rate"));
                    }
                }
            }
        }
        catch( JSONException e ) {
            throw new InternalException(e);
        }
        return prd;
    }

    @Override
    public Iterable<VirtualMachineProduct> listProducts(VirtualMachineProductFilterOptions options, Architecture architecture) throws InternalException, CloudException{
        return listProducts(architecture);
    }
}
