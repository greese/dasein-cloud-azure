/**
 * ========= CONFIDENTIAL =========
 *
 * Copyright (C) 2012 enStratus Networks Inc - ALL RIGHTS RESERVED
 *
 * ====================================================================
 *  NOTICE: All information contained herein is, and remains the
 *  property of enStratus Networks Inc. The intellectual and technical
 *  concepts contained herein are proprietary to enStratus Networks Inc
 *  and may be covered by U.S. and Foreign Patents, patents in process,
 *  and are protected by trade secret or copyright law. Dissemination
 *  of this information or reproduction of this material is strictly
 *  forbidden unless prior written permission is obtained from
 *  enStratus Networks Inc.
 * ====================================================================
 */
package org.dasein.cloud.azure.compute.disk;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.compute.vm.AzureVM;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.Volume;
import org.dasein.cloud.compute.VolumeCreateOptions;
import org.dasein.cloud.compute.VolumeProduct;
import org.dasein.cloud.compute.VolumeState;
import org.dasein.cloud.compute.VolumeSupport;
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
import java.util.ArrayList;
import java.util.Locale;

/**
 * Support for Azure block storage disks via the Dasein Cloud volume API.
 * <p>Created by George Reese: 6/19/12 9:25 AM</p>
 * @author George Reese (george.reese@imaginary.com)
 * @version 2012-06
 * @since 2012-06
 */
public class AzureDisk implements VolumeSupport {
    static private final Logger logger = Azure.getLogger(AzureDisk.class);

    static private final String DISK_SERVICES = "/services/disks";
    static private final String HOSTED_SERVICES = "/services/hostedservices";
    
    private Azure provider;

    public AzureDisk(Azure provider) { this.provider = provider; }
    
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
                      
            Volume disk ;
            if(volumeId != null){
            	 disk = getVolume(volumeId);
            	 if(disk == null ){
            		throw new InternalException("Can not find the source snapshot !"); 
            	 }            	
            }else{
            	throw new InternalException("volumeId is null !");
            }
            
           	//dsn2260-dsn2260Role-0-20120619044615
            if(!toServer.contains(AzureVM.SERVICE_VM_NAME_SPLIT)){
            	logger.trace(" No such VM");
            	return;
            }
           	String[] serviceVMName = toServer.split(AzureVM.SERVICE_VM_NAME_SPLIT);
         	String resourceDir = HOSTED_SERVICES + "/" + serviceVMName[0] + "/deployments" + "/" +  serviceVMName[0] + "/roles"+"/" + serviceVMName[1] + "/DataDisks";
         	                                
            AzureMethod method = new AzureMethod(provider);
            StringBuilder xml = new StringBuilder();    
                        
            xml.append("<DataVirtualHardDisk  xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("<HostCaching>ReadWrite</HostCaching>");
            xml.append("<DiskLabel>" + disk.getName() + "</DiskLabel>");
            xml.append("<DiskName>" + disk.getName() + "</DiskName>");
            if(device != null && isWithinDeviceList(device)){
            	xml.append("<Lun>" + device + "</Lun>");
            }            
            xml.append("<LogicalDiskSizeInGB>" + disk.getSizeInGigabytes() + "</LogicalDiskSizeInGB>");
            xml.append("<MediaLink>" + disk.getMediaLink()+"</MediaLink>");
            xml.append("<SourceMediaLink>" + "" + "</SourceMediaLink>");
            xml.append("</DataVirtualHardDisk>");      

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
            method.post(ctx.getAccountNumber(), resourceDir, xml.toString());

        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureDisk.class.getName() + ".attach()");
            }
        }
    	
    }

   // static private final Random random = new Random();
    
    @Override
    public @Nonnull String create(@Nullable String fromSnapshot, @Nonnegative int sizeInGb, @Nonnull String inZone) throws InternalException, CloudException {
        String name = "dsn" + System.currentTimeMillis();
        VolumeCreateOptions options;

        if( fromSnapshot == null ) {
            options = VolumeCreateOptions.getInstance(new Storage<Gigabyte>(sizeInGb, Storage.GIGABYTE), name, name).inDataCenter(inZone);
        }
        else {
            options = VolumeCreateOptions.getInstanceForSnapshot(fromSnapshot,new Storage<Gigabyte>(sizeInGb, Storage.GIGABYTE), name, name).inDataCenter(inZone);
        }
        return createVolume(options);
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
            
            String fromVolumeId = options.getSnapshotId();
            Volume disk ;
            if(fromVolumeId != null){
            	 disk = getVolume(fromVolumeId);
            	 if(disk == null ){
            		throw new InternalException("Can not find the source snapshot !"); 
            	 }            	
            }else{
            	throw new InternalException("Azure needs a source snapshot Id to create a new disk volume !");
            }
                        
            AzureMethod method = new AzureMethod(provider);
            StringBuilder xml = new StringBuilder();

            String label;

            try {
                label = new String(Base64.encodeBase64(options.getName().getBytes("utf-8")));
            }
            catch( UnsupportedEncodingException e ) {
                throw new InternalException(e);
            }
                        
            xml.append("<Disk xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("<HasOperatingSystem>false</HasOperatingSystem>");
            xml.append("<Label>" + label + "</Label>");
            xml.append("<MediaLink>" + disk.getMediaLink()+"</MediaLink>");
            xml.append("<Name>" + options.getName() + "</Name>");
            //<OS>Linux|Windows</OS>
            Platform platform = disk.getGuestOperatingSystem();
            if(platform.isWindows()){
            	xml.append("<OS>Windows</OS>");
            }else{
            	xml.append("<OS>Linux</OS>");
            }
                        
            xml.append("</Disk>");
      

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

            method.post(ctx.getAccountNumber(), DISK_SERVICES, xml.toString());
            // TODO: return ID
            return "";
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureDisk.class.getName() + ".launch()");
            }
        }
    }
    
    @Override
    public void detach(@Nonnull String volumeId) throws InternalException, CloudException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureDisk.class.getName() + ".deattach(" + volumeId+")");
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
            if(providerVirtualMachineId == null || !providerVirtualMachineId.contains(AzureVM.SERVICE_VM_NAME_SPLIT)){ 
            	logger.trace("Sorry, the disk is not attached to the VM with id " + providerVirtualMachineId  + "Or the VM id is not having the desired format !!!");
            	throw new InternalException("Sorry, the disk is not attached to the VM with id " + providerVirtualMachineId  + "Or the VM id is not having the desired format !!!");
            }
            String lun = getDiskLun(disk.getProviderVolumeId(), providerVirtualMachineId);
            
            if(lun == null){
            	logger.trace("Can not identify the lun number");
            	throw new InternalException("logical unit number of disk is null, detach operation can not be continue!");
           	}
            String[] serviceVMName = providerVirtualMachineId.split(AzureVM.SERVICE_VM_NAME_SPLIT);
         	String resourceDir = HOSTED_SERVICES + "/" + serviceVMName[0] + "/deployments" + "/" +  serviceVMName[0] + "/roles"+"/" + serviceVMName[1] + "/DataDisks" + "/" + lun;
         	                                
            AzureMethod method = new AzureMethod(provider);
            
            method.invoke("DELETE",ctx.getAccountNumber(), resourceDir, null);
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureDisk.class.getName() + ".deattach()");
            }
        }
    }
    private String getDiskLun(String providerVolumeId, String providerVirtualMachineId) throws InternalException, CloudException {
    	
        AzureMethod method = new AzureMethod(provider);
        
        if(!providerVirtualMachineId.contains(AzureVM.SERVICE_VM_NAME_SPLIT)){
        	return null;
        }
       	String[] serviceVMName = providerVirtualMachineId.split(AzureVM.SERVICE_VM_NAME_SPLIT);
     	String resourceDir =  HOSTED_SERVICES + "/"+ serviceVMName[0] + "/deployments" + "/" + serviceVMName[0];
     	
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
                	lunValue = attribute.getFirstChild().getNodeValue().trim();
                }
                
            }
            if(diskName != null){
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
    public Iterable<VolumeProduct> listVolumeProducts() throws InternalException, CloudException {
        //To change body of implemented methods use File | Settings | File Templates.
       return null;
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
 
            method.post(ctx.getAccountNumber(), DISK_SERVICES+"/" + volumeId, null);
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureDisk.class.getName() + ".launch()");
            }
        }
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
        disk.setProviderDataCenterId(provider.getDataCenterId(regionId));
        disk.setCurrentState(VolumeState.AVAILABLE);
                
        NodeList attributes = volumeNode.getChildNodes();
        
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;

            if( attribute.getNodeName().equalsIgnoreCase("AttachedTo") && attribute.hasChildNodes() ) {
            	NodeList attachAttributes = attribute.getChildNodes();
                String hostedServiceName = null;
                String vmRoleName = null;
            	for( int k=0; k<attachAttributes.getLength(); k++ ) {
            		Node attach = attachAttributes.item(k);
            		if(attach.getNodeType() == Node.TEXT_NODE) continue; 
            		
            		if(attach.getNodeName().equalsIgnoreCase("HostedServiceName") && attach.hasChildNodes() ) {	                 
            			hostedServiceName = attach.getFirstChild().getNodeValue().trim();	              
            		}
            		else if(attach.getNodeName().equalsIgnoreCase("RoleName") && attach.hasChildNodes() ) {	                 
            			vmRoleName = attach.getFirstChild().getNodeValue().trim();	              
            		}
            	} 
            	
            	/**
            	 * VM ID = hostedServiceName +  AzureVM.SERVICE_VM_NAME_SPLIT + roleName
            	 */
            	if(hostedServiceName != null && vmRoleName != null){
            		disk.setProviderVirtualMachineId(hostedServiceName + AzureVM.SERVICE_VM_NAME_SPLIT + vmRoleName);
            	}
            }
            else if( attribute.getNodeName().equalsIgnoreCase("OS") && attribute.hasChildNodes() ) {            	
            	disk.setGuestOperatingSystem(Platform.guess(attribute.getFirstChild().getNodeValue().trim()));            	
            }
            else if( attribute.getNodeName().equalsIgnoreCase("Label") && attribute.hasChildNodes() ) {
            	disk.setDescription(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( attribute.getNodeName().equalsIgnoreCase("Location") && attribute.hasChildNodes() ) {
            	if( !regionId.equals(attribute.getFirstChild().getNodeValue().trim()) ) {
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
   
        if(disk.getGuestOperatingSystem() == null){
        	disk.setGuestOperatingSystem(Platform.UNKNOWN);        	
        }
        if( disk.getName() == null ) {
        	disk.setName(disk.getProviderVolumeId());
        }
        if( disk.getDescription() == null ) {
        	disk.setDescription(disk.getName());
        }
       
        return disk;
    }
     
}
