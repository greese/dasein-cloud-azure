/**
 * Copyright (C) 2013-2014 Dell, Inc
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

package org.dasein.cloud.azure.compute.disk;

import org.apache.log4j.Logger;
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
import org.dasein.cloud.azure.compute.disk.model.DataVirtualHardDiskModel;
import org.dasein.cloud.azure.compute.vm.AzureRoleDetails;
import org.dasein.cloud.azure.compute.vm.model.DeploymentModel;
import org.dasein.cloud.compute.*;

import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.util.CalendarWrapper;
import org.dasein.util.uom.storage.Gigabyte;
import org.dasein.util.uom.storage.Storage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * Support for Azure block storage disks via the Dasein Cloud volume API.
 * <p>Created by George Reese: 6/19/12 9:25 AM</p>
 * @author George Reese (george.reese@imaginary.com)
 * @version 2012-06
 * @since 2012-06
 */
public class AzureDisk extends AbstractVolumeSupport {
    static private final Logger logger = Azure.getLogger(AzureDisk.class);

    static private final String DISK_SERVICES = "/services/disks";
    static private final String HOSTED_SERVICES = "/services/hostedservices";
    static public final String DEPLOYMENT_RESOURCE = "/services/hostedservices/%s/deployments/%s";
    static public final String DATA_DISK_LUN = "/services/hostedservices/%s/deployments/%s/roles/%s/DataDisks/%s";
    static public final String DATA_DISK_RESOURCE = "/services/hostedservices/%s/deployments/%s/roles/%s/DataDisks";
    
    private Azure provider;

    public AzureDisk(Azure provider) {
        super(provider);
        this.provider = provider;
    }
    
    @Override
    public void attach(@Nonnull String volumeId, @Nonnull String toServer, @Nonnull String device) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureDisk.class.getName() + ".attach(" + volumeId + "," + toServer+ "," +  device+  ")");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }

            if(volumeId == null || volumeId.isEmpty())
                throw new InternalException("No volumeId was specified to be attached");

            Volume disk = getVolume(volumeId);
            if(disk == null ){
                throw new InternalException("The volumeId specified is not a valid one");
            }

            if(toServer == null)
                throw new InternalException("The virtual machine id cannot be empty");

            VirtualMachine server = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(toServer);
            if(server == null)
                throw new InternalException("The specified virtual machine id is invalid");


            DataVirtualHardDiskModel dataVirtualHardDiskModel = new DataVirtualHardDiskModel();
            dataVirtualHardDiskModel.setHostCaching("ReadWrite");
            dataVirtualHardDiskModel.setDiskName(disk.getName());
            dataVirtualHardDiskModel.setMediaLink(disk.getMediaLink());

            AzureMethod method = new AzureMethod(provider);
            method.post(String.format(DATA_DISK_RESOURCE, server.getTag("serviceName"), server.getTag("deploymentName"), server.getTag("roleName")), dataVirtualHardDiskModel);
        } catch (JAXBException e) {
            logger.error(e.getMessage());
            throw new InternalException(e);
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureDisk.class.getName() + ".attach()");
            }
        }
    }

    @Override
    public @Nonnull String createVolume(@Nonnull VolumeCreateOptions options) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureDisk.class.getName() + ".createVolume(" + options + ")");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }

            String storageEndpoint = provider.getStorageEndpoint();
            if( storageEndpoint == null || storageEndpoint.isEmpty()) {
                throw new CloudException("Cannot find blob storage endpoint in the current region");
            }

            if(options.getProviderVirtualMachineId() == null || options.getProviderVirtualMachineId().isEmpty())
                throw new InternalException("VolumeCreateOptions does not specify a virtual machine id");

            if(options.getVolumeSize() == null)
                throw new InternalException("VolumeCreateOptions should specify a volume size");

            VirtualMachine server = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(options.getProviderVirtualMachineId());
            if(server == null)
                throw new InternalException("The specified virtual machine id is invalid");

            Integer dataDiskCount = getDataDisksCount(server);
            if(dataDiskCount == 2)
                throw new InternalException("The maximum number of data disks currently permitted is 2. The current number of data disks is 2. The operation is attempting to add 1 additional data disks.");

            String diskName = String.format("%s-%s-%s-%s", server.getTag("serviceName"), server.getTag("deploymentName"), server.getTag("roleName"), new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
            String storageMediaLink = String.format("%s/vhds/%s.vhd", storageEndpoint, diskName);
            String lun = String.valueOf(dataDiskCount + 1);

            DataVirtualHardDiskModel dataVirtualHardDiskModel = new DataVirtualHardDiskModel();
            dataVirtualHardDiskModel.setHostCaching("ReadWrite");
            dataVirtualHardDiskModel.setLun(lun);
            dataVirtualHardDiskModel.setLogicalDiskSizeInGB(Integer.toString(options.getVolumeSize().intValue()));
            dataVirtualHardDiskModel.setMediaLink(storageMediaLink);

            AzureMethod method = new AzureMethod(provider);
            String requestId = method.post(String.format(DATA_DISK_RESOURCE, server.getTag("serviceName"), server.getTag("deploymentName"), server.getTag("roleName")), dataVirtualHardDiskModel);
            waitForOperation(requestId);

            return getDataDiskName(server, lun);
        }catch (JAXBException e) {
            logger.error(e.getMessage());
            throw new InternalException(e);
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureDisk.class.getName() + ".launch()");
            }
        }
    }

    private void waitForOperation(String requestId) throws CloudException, InternalException {
        if(requestId == null)
            return;

        AzureMethod method = new AzureMethod(provider);
        int httpCode = method.getOperationStatus(requestId);
        while (httpCode == -1) {
            try {
                Thread.sleep(15000L);
            }
            catch (InterruptedException ignored){}
            httpCode = method.getOperationStatus(requestId);
        }
    }

    private String getDataDiskName(VirtualMachine virtualMachine, String lun) throws CloudException, InternalException {
        AzureMethod azureMethod = new AzureMethod(provider);
        DataVirtualHardDiskModel dataVirtualHardDiskModel = azureMethod.get(DataVirtualHardDiskModel.class, String.format(DATA_DISK_LUN, virtualMachine.getTag("serviceName"), virtualMachine.getTag("deploymentName"), virtualMachine.getTag("roleName"), lun));

        if(dataVirtualHardDiskModel == null)
            return null;

        return dataVirtualHardDiskModel.getDiskName();
    }


    private Integer getDataDisksCount(VirtualMachine virtualMachine) throws CloudException, InternalException {
        AzureMethod azureMethod = new AzureMethod(provider);
        DeploymentModel deploymentModel = azureMethod.get(DeploymentModel.class, String.format(DEPLOYMENT_RESOURCE, virtualMachine.getTag("serviceName"), virtualMachine.getTag("deploymentName")));

        if(deploymentModel.getRoles() == null)
            return 0;

        for(DeploymentModel.RoleModel role : deploymentModel.getRoles()){
            if(role.getRoleName().equalsIgnoreCase((String) virtualMachine.getTag("roleName"))){
                if(role.getDataVirtualDisks() == null)
                    return 0;

                return role.getDataVirtualDisks().size();
            }
        }

        return 0;
    }

    @Override
    public void detach(@Nonnull String volumeId, boolean force) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureDisk.class.getName() + ".detach(" + volumeId+")");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }

            Volume disk ;
            if(volumeId != null){
                disk = getVolume(volumeId);
                if(disk == null ){
                    throw new InternalException("Can not find the source snapshot !");
                }
            }else{
                throw new InternalException("volumeId is null !");
            }

            String providerVirtualMachineId = disk.getProviderVirtualMachineId();
            VirtualMachine vm = null;

            if( providerVirtualMachineId != null ) {
                vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(providerVirtualMachineId);
            }
            if( vm == null ) {
                logger.trace("Sorry, the disk is not attached to the VM with id " + providerVirtualMachineId  + " or the VM id is not in the desired format !!!");
                throw new InternalException("Sorry, the disk is not attached to the VM with id " + providerVirtualMachineId  + " or the VM id is not in the desired format !!!");
            }
            String lun = getDiskLun(disk.getProviderVolumeId(), providerVirtualMachineId);

            if(lun == null){
                logger.trace("Can not identify the lun number");
                throw new InternalException("logical unit number of disk is null, detach operation can not be continue!");
            }

            AzureRoleDetails azureRoleDetails = AzureRoleDetails.fromString(providerVirtualMachineId);
            String resourceDir = HOSTED_SERVICES + "/" + azureRoleDetails.getServiceName() + "/deployments" + "/" +  azureRoleDetails.getDeploymentName() + "/roles"+"/" + azureRoleDetails.getRoleName() + "/DataDisks" + "/" + lun;

            AzureMethod method = new AzureMethod(provider);

            method.invoke("DELETE",ctx.getAccountNumber(), resourceDir, null);
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureDisk.class.getName() + ".detach()");
            }
        }
    }

    private transient volatile AzureDiskCapabilities capabilities;
    @Override
    public VolumeCapabilities getCapabilities() throws CloudException, InternalException {
        if( capabilities == null ) {
            capabilities = new AzureDiskCapabilities(provider);
        }
        return capabilities;
    }

    private String getDiskLun(String providerVolumeId, String providerVirtualMachineId) throws InternalException, CloudException {
    	
        AzureMethod method = new AzureMethod(provider);

        VirtualMachine vm = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(providerVirtualMachineId);
        if(vm == null)
            return null;

        AzureRoleDetails azureRoleDetails = AzureRoleDetails.fromString(providerVirtualMachineId);

     	String resourceDir =  HOSTED_SERVICES + "/"+ azureRoleDetails.getServiceName() + "/deployments" + "/" + azureRoleDetails.getDeploymentName();
     	
     	Document doc = method.getAsXML(provider.getContext().getAccountNumber(),resourceDir);
		
        if( doc == null ) {
            return null;
        }
        NodeList entries = doc.getElementsByTagName("DataVirtualHardDisk");

        if( entries.getLength() < 1 ) {
            return null;
        }      
        
        for( int i=0; i<entries.getLength(); i++ ) {
            Node detail = entries.item(i);

            NodeList attributes = detail.getChildNodes();
            String diskName = null, lunValue = null;
            for( int j=0; j<attributes.getLength(); j++ ) {
                Node attribute = attributes.item(j);
               
                if(attribute.getNodeType() == Node.TEXT_NODE) continue;
                
                if( attribute.getNodeName().equalsIgnoreCase("DiskName") && attribute.hasChildNodes() ) {
                	diskName = attribute.getFirstChild().getNodeValue().trim();
                }
                else if( attribute.getNodeName().equalsIgnoreCase("Lun") && attribute.hasChildNodes() ) {
                    if (diskName != null && diskName.equalsIgnoreCase(providerVolumeId)) {
                        lunValue = attribute.getFirstChild().getNodeValue().trim();
                    }
                }
            }
            if(diskName != null && diskName.equalsIgnoreCase(providerVolumeId)){
            	if(lunValue == null){
            		lunValue = "0";	
            	}            	
            	return lunValue;
            }           
        }
        
        return null;
    }

    @Override
    public int getMaximumVolumeCount() throws InternalException, CloudException {
        return 16;
    }
       

    @Override
    public @Nullable Storage<Gigabyte> getMaximumVolumeSize() throws InternalException, CloudException {
        return new Storage<Gigabyte>(1024, Storage.GIGABYTE);
    }

    @Override
    public @Nonnull Storage<Gigabyte> getMinimumVolumeSize() throws InternalException, CloudException {
        return new Storage<Gigabyte>(1, Storage.GIGABYTE);
    }

    @Override
    public @Nonnull String getProviderTermForVolume(@Nonnull Locale locale) {
        return "disk";
    }

    @Override
    public @Nullable Volume getVolume(@Nonnull String volumeId) throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
    	
    	ArrayList<Volume> list = (ArrayList<Volume>) listVolumes();
    	if(list == null)
    		return null;
    	for(Volume disk : list){
    		if(disk.getProviderVolumeId().equals(volumeId)){
    			return disk;
    		}    		
    	}
  		return null;
    }

    @Nonnull
    @Override
    public Requirement getVolumeProductRequirement() throws InternalException, CloudException {
        return Requirement.NONE;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isVolumeSizeDeterminedByProduct() throws InternalException, CloudException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public @Nonnull Iterable<String> listPossibleDeviceIds(@Nonnull Platform platform) throws InternalException, CloudException {
       //To change body of implemented methods use File | Settings | File Templates.
    	ArrayList<String> list = new ArrayList<String>();
    	for(int i= 0;i < this.getMaximumVolumeCount();i++){
    		list.add(String.valueOf(i));    		
    	}
    	return list;
    }

    @Nonnull
    @Override
    public Iterable<VolumeFormat> listSupportedFormats() throws InternalException, CloudException {
        return Collections.singletonList(VolumeFormat.BLOCK);
    }

    @Nonnull
    @Override
    public Iterable<VolumeProduct> listVolumeProducts() throws InternalException, CloudException {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Iterable<ResourceStatus> listVolumeStatus() throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), DISK_SERVICES);


        NodeList entries = doc.getElementsByTagName("Disk");
        ArrayList<ResourceStatus> list = new ArrayList<ResourceStatus>();

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            ResourceStatus status = toStatus(ctx, entry);
            if( status != null ) {
                list.add(status);
            }
        }
        return list;
    }

    @Override
    public @Nonnull Iterable<Volume> listVolumes() throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), DISK_SERVICES);

        NodeList entries = doc.getElementsByTagName("Disk");
        ArrayList<Volume> disks = new ArrayList<Volume>();

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            Volume disk = toVolume(ctx, entry);
            if( disk != null ) {
            	disks.add(disk);
            }
        }
        return disks;
    }

    @Nonnull
    @Override
    public Iterable<Volume> listVolumes(@Nullable VolumeFilterOptions volumeFilterOptions) throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), DISK_SERVICES);


        NodeList entries = doc.getElementsByTagName("Disk");
        ArrayList<Volume> disks = new ArrayList<Volume>();

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            Volume disk = toVolume(ctx, entry);
            if( disk != null ) {
                disks.add(disk);
            }
        }
        return disks;
    }

    private boolean isWithinDeviceList(String device) throws InternalException, CloudException{
    	ArrayList<String> list = (ArrayList<String>) listPossibleDeviceIds(Platform.UNIX);
    	
    	for(String id : list){
    		if(id.equals(device)){
    			return true;
    		}
    	}
    	return false;
    	
    }
    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void remove(@Nonnull String volumeId) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureDisk.class.getName() + ".remove(" + volumeId + ")");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }                      
            
            AzureMethod method = new AzureMethod(provider);

            long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 10L);
            while( timeout > System.currentTimeMillis() ) {
                try {
                    method.invoke("DELETE",ctx.getAccountNumber(), DISK_SERVICES+"/" + volumeId+"?comp=media", null);
                    break;
                }
                catch (CloudException e) {
                    if( e.getProviderCode() != null && e.getProviderCode().equals("BadRequest") ) {
                        logger.warn("Conflict error, maybe retrying in 30 seconds");
                        try { Thread.sleep(30000L); }
                        catch( InterruptedException ignore ) { }
                        continue;
                    }
                    logger.warn("Unable to delete volume " + volumeId + ": " + e.getMessage());
                    throw e;
                }
            }
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureDisk.class.getName() + ".remove()");
            }
        }
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
    public void updateTags(@Nonnull String s, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateTags(@Nonnull String[] strings, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public @Nonnull String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }

    private @Nullable Volume toVolume(@Nonnull ProviderContext ctx, @Nullable Node volumeNode) throws InternalException, CloudException {
        if( volumeNode == null ) {
            return null;
        }
          
        String regionId = ctx.getRegionId();

        if( regionId == null ) {
            throw new AzureConfigException("No region ID was specified for this request");
        }
        Volume disk = new Volume();
        disk.setProviderRegionId(regionId);
        disk.setCurrentState(VolumeState.AVAILABLE);
        disk.setType(VolumeType.HDD);
        boolean mediaLocationFound = false;

        NodeList attributes = volumeNode.getChildNodes();
        
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;

            if( attribute.getNodeName().equalsIgnoreCase("AttachedTo") && attribute.hasChildNodes() ) {
            	NodeList attachAttributes = attribute.getChildNodes();
                String hostedServiceName = null;
                String deploymentName = null;
                String vmRoleName = null;
            	for( int k=0; k<attachAttributes.getLength(); k++ ) {
            		Node attach = attachAttributes.item(k);
            		if(attach.getNodeType() == Node.TEXT_NODE) continue; 
            		
            		if(attach.getNodeName().equalsIgnoreCase("HostedServiceName") && attach.hasChildNodes() ) {	                 
            			hostedServiceName = attach.getFirstChild().getNodeValue().trim();	              
            		}
                    else if(attach.getNodeName().equalsIgnoreCase("DeploymentName") && attach.hasChildNodes() ) {
                        deploymentName = attach.getFirstChild().getNodeValue().trim();
                    }
            		else if(attach.getNodeName().equalsIgnoreCase("RoleName") && attach.hasChildNodes() ) {	                 
            			vmRoleName = attach.getFirstChild().getNodeValue().trim();	              
            		}
            	} 
            	
            	if(hostedServiceName != null && deploymentName != null && vmRoleName != null){
            		disk.setProviderVirtualMachineId(hostedServiceName+":"+deploymentName+":"+vmRoleName);
            	}
            }
            else if( attribute.getNodeName().equalsIgnoreCase("OS") && attribute.hasChildNodes() ) {
                // return root volumes
                disk.setGuestOperatingSystem(Platform.guess(attribute.getFirstChild().getNodeValue().trim()));
            }

            // disk may have either affinity group or location depending on how storage account is set up
            else if( attribute.getNodeName().equalsIgnoreCase("AffinityGroup") && attribute.hasChildNodes() ) {
                //get the region for this affinity group
                String affinityGroup = attribute.getFirstChild().getNodeValue().trim();
                if (affinityGroup != null && !affinityGroup.equals("")) {
                    AffinityGroup affinityGroupModel = provider.getComputeServices().getAffinityGroupSupport().get(affinityGroup);
                    if(affinityGroupModel == null)
                        return null;

                    DataCenter dc = provider.getDataCenterServices().getDataCenter(affinityGroupModel.getDataCenterId());
                    if (dc != null && dc.getRegionId() != null && dc.getRegionId().equals(disk.getProviderRegionId())) {
                        disk.setProviderDataCenterId(dc.getProviderDataCenterId());
                        mediaLocationFound = true;
                    }
                    else {
                        // not correct region/datacenter
                        return null;
                    }
                }
            }
            else if( attribute.getNodeName().equalsIgnoreCase("Location") && attribute.hasChildNodes() ) {
            	if( !mediaLocationFound && !regionId.equals(attribute.getFirstChild().getNodeValue().trim()) ) {
                     return null;
                }
            }
            else if( attribute.getNodeName().equalsIgnoreCase("LogicalDiskSizeInGB") && attribute.hasChildNodes() ) {
            	disk.setSize(Storage.valueOf(Integer.valueOf(attribute.getFirstChild().getNodeValue().trim()), "gigabyte"));
            }
            else if( attribute.getNodeName().equalsIgnoreCase("MediaLink") && attribute.hasChildNodes() ) {
            	disk.setMediaLink(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("Name") && attribute.hasChildNodes() ) {
            	disk.setProviderVolumeId(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("SourceImageName") && attribute.hasChildNodes() ) {
            	disk.setProviderSnapshotId(attribute.getFirstChild().getNodeValue().trim());            	
            } 
        }

        if (disk.getProviderVirtualMachineId() != null) {
            // attached to vm - now populate device id
            String lun = getDiskLun(disk.getProviderVolumeId(), disk.getProviderVirtualMachineId());
            disk.setDeviceId(lun);
        }

        if(disk.getGuestOperatingSystem() == null){
        	disk.setGuestOperatingSystem(Platform.UNKNOWN);        	
        }
        if( disk.getName() == null ) {
        	disk.setName(disk.getProviderVolumeId());
        }
        if( disk.getDescription() == null ) {
        	disk.setDescription(disk.getName());
        }
        if (disk.getProviderDataCenterId() == null) {
            DataCenter dc = provider.getDataCenterServices().listDataCenters(regionId).iterator().next();
            disk.setProviderDataCenterId(dc.getProviderDataCenterId());
        }
       
        return disk;
    }

    private @Nullable ResourceStatus toStatus(@Nonnull ProviderContext ctx, @Nullable Node volumeNode) throws InternalException, CloudException {
        if( volumeNode == null ) {
            return null;
        }

        String regionId = ctx.getRegionId();

        if( regionId == null ) {
            throw new AzureConfigException("No region ID was specified for this request");
        }

        String id = "";
        boolean mediaLocationFound = false;

        NodeList attributes = volumeNode.getChildNodes();

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;

            if( attribute.getNodeName().equalsIgnoreCase("Name") && attribute.hasChildNodes() ) {
                id = attribute.getFirstChild().getNodeValue().trim();
            }
            else if( attribute.getNodeName().equalsIgnoreCase("AffinityGroup") && attribute.hasChildNodes() ) {
                //get the region for this affinity group
                String affinityGroup = attribute.getFirstChild().getNodeValue().trim();
                if (affinityGroup != null && !affinityGroup.equals("")) {
                    AffinityGroup affinityGroupModel = provider.getComputeServices().getAffinityGroupSupport().get(affinityGroup);
                    if(affinityGroupModel == null)
                        return null;

                    DataCenter dc = provider.getDataCenterServices().getDataCenter(affinityGroupModel.getDataCenterId());
                    if (dc.getRegionId().equals(regionId)) {
                        mediaLocationFound = true;
                    }
                    else {
                        // not correct region/datacenter
                        return null;
                    }
                }
            }
            else if( attribute.getNodeName().equalsIgnoreCase("Location") && attribute.hasChildNodes() ) {
                if( !mediaLocationFound && !regionId.equals(attribute.getFirstChild().getNodeValue().trim()) ) {
                    return null;
                }
            }
            else if( attribute.getNodeName().equalsIgnoreCase("OS") && attribute.hasChildNodes() ) {
                // not a volume so should not be returned here
                return null;
            }
        }

        ResourceStatus status = new ResourceStatus(id, VolumeState.AVAILABLE);

        return status;
    }
     
}
