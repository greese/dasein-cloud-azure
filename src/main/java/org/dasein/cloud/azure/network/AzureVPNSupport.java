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

package org.dasein.cloud.azure.network;


import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.compute.disk.AzureDisk;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.network.VPN;
import org.dasein.cloud.network.VPNCapabilities;
import org.dasein.cloud.network.VPNConnection;
import org.dasein.cloud.network.VPNGateway;
import org.dasein.cloud.network.VPNGatewayState;
import org.dasein.cloud.network.VPNProtocol;
import org.dasein.cloud.network.VPNState;
import org.dasein.cloud.network.VPNSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AzureVPNSupport implements VPNSupport {	
    static private final Logger logger = Azure.getLogger(AzureVPNSupport.class);

	static private final String NETWORKING_SERVICES = "/services/networking";
	/**Azure gateway, vlan does not have an Id. Therefore,gateway id would be 
	 *  local network name + _&_ + gateway endpoint
	*/
	static private final String OBJECT_NAME_SPLIT = "_&_";
	static public final String VPN_ID_KEY = "VPNID";
	/**
	 *  The enstratus default VPN is used when create VLANs, subnet, local network and gateway 
	 *  as all of the operations on VLAN, subnet etc requires an VPN;
	 */
	static public final String ENSTRATUS_DEFAULT_VPN = "Enstratus_VPN";
	
    private Azure provider;

    public AzureVPNSupport(Azure provider) { this.provider = provider; }
	

	@Override
	public String[] mapServiceAction(ServiceAction action) {
		return new String[0];
	}

	/**
	 * Vlan was created when the VPN created
	 */
	@Override
	public void attachToVLAN(String providerVpnId, String providerVlanId) throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectToGateway(String providerVpnId, String toGatewayId) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVPNSupport.class.getName() + ".connectToGateway()");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }
           
            
            AzureMethod method = new AzureMethod(provider);
                  
            String resourceDir = NETWORKING_SERVICES + "/" + providerVpnId + "/gateway";
           
            method.post(ctx.getAccountNumber(), resourceDir, null);
     
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVPNSupport.class.getName() + ".connectToGateway()");
            }
        }
	}

	@Override
	public VPN createVPN(String inProviderDataCenterId, String name,String description, VPNProtocol protocol) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVPNSupport.class.getName() + ".createVPN()");
        }
        
        //Require a default cidr
        String cidr = "10.0.0.0/8";
        
        try {        	
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }
                       
            AzureMethod method = new AzureMethod(provider);
            StringBuilder xml = new StringBuilder();
           
            xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");             
            xml.append("<NetworkConfiguration xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration\">");
            xml.append("<VirtualNetworkConfiguration>");
            xml.append("<VirtualNetworkSites>");
            xml.append("VirtualNetworkSite name=\"" + name+ "\" Location=\"" +  ctx.getRegionId() +"\">");
            xml.append("<AddressSpace>");
            xml.append("<AddressPrefix>"+ cidr +"</AddressPrefix>");
            xml.append("</AddressSpace>");
            xml.append("</VirtualNetworkSite>");
            xml.append("</VirtualNetworkSites>");                        
            xml.append("</VirtualNetworkConfiguration>");
            xml.append("</NetworkConfiguration>");

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

            String resourceDir = NETWORKING_SERVICES + "/media";
            method.post(ctx.getAccountNumber(),resourceDir, xml.toString());
            // TODO: return VLAN
            return null;
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVlanSupport.class.getName() + ".createVlan()");
            }
        }
	}
	private String getAffinityGroup(String vlanName){
		//TODO
		return "Group1";
	}
	
	// Create local network for connecting VPN?
	@Override
	public VPNGateway createVPNGateway(String endpoint, String name,String description, VPNProtocol protocol, String bgpAsn)throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVPNSupport.class.getName() + ".createVPNGateway()");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }
           
            
            AzureMethod method = new AzureMethod(provider);
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            xml.append("<NetworkConfiguration  xmlns=\"http://schemas.microsoft.com/windowsazure\" xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">");
            xml.append("<VirtualNetworkConfiguration>");            
            xml.append("<LocalNetworkSites>");
            
            xml.append("<LocalNetworkSite name=\"" + name +  "\">");
            xml.append("<AddressSpace>");
            xml.append("<AddressPrefix>"+ endpoint +"/32" + "</AddressPrefix>");
            xml.append("</AddressSpace>");
            xml.append("<VPNGatewayAddress>"+ endpoint + "</VPNGatewayAddress>");
            xml.append("</LocalNetworkSite>");
            
            xml.append("</LocalNetworkSites>");
            xml.append("</VirtualNetworkConfiguration>");
            xml.append("</NetworkConfiguration>");
               

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

            String resourceDir = NETWORKING_SERVICES + "/media";
            method.post(ctx.getAccountNumber(),resourceDir , xml.toString());
            // TODO: VPNGateway
            return null;
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVPNSupport.class.getName() + ".createVPNGateway()");
            }
        }
		
	}
	
	@Override
	public void deleteVPN(String providerVpnId) throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteVPNGateway(String providerVPNGatewayId) throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVPNSupport.class.getName() + ".deleteVPNGateway(" + providerVPNGatewayId+")");
        }
        try {
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }
            
            if(!providerVPNGatewayId.contains(OBJECT_NAME_SPLIT)){
            	logger.trace(" The GateWay ID does not have the patern ->" + OBJECT_NAME_SPLIT );
            	return;
            }
           	String[] VPNAndGatewayName = providerVPNGatewayId.split(OBJECT_NAME_SPLIT);
         	String resourceDir = NETWORKING_SERVICES + "/" + VPNAndGatewayName[0] + "/gateway" ;
         	                                
            AzureMethod method = new AzureMethod(provider);
    
            method.invoke("DELETE",ctx.getAccountNumber(), resourceDir, null);
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureDisk.class.getName() + ".deattach()");
            }
        }

	}

	@Override
	public void detachFromVLAN(String providerVpnId, String providerVlanId) throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnectFromGateway(String providerVpnId, String fromGatewayId) throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

    private transient volatile AzureVPNCapabilities capabilities;
    @Nonnull
    @Override
    public VPNCapabilities getCapabilities() throws CloudException, InternalException {
        if( capabilities == null ) {
            capabilities = new AzureVPNCapabilities(provider);
        }
        return capabilities;
    }

    @Override
	public VPNGateway getGateway(String gatewayId) throws CloudException,InternalException {
		ArrayList<VPNGateway> list = (ArrayList<VPNGateway>) listGateways();
		if(list != null){ 
			for(VPNGateway gateway: list){
				if(gateway.getProviderVpnGatewayId().equals(gatewayId)){
					return gateway;
				}			
			}
		}
		return null;		
	}

	@Override
	public VPN getVPN(String providerVpnId) throws CloudException,InternalException {
		ArrayList<VPN> list = (ArrayList<VPN>) listVPNs();
		if(list != null){ 
			for(VPN vpn: list){
				if(vpn.getProviderVpnId().equals(providerVpnId)){
					return vpn;
				}			
			}
		}
		return null;		
	}

	@Override
	public Requirement getVPNDataCenterConstraint() throws CloudException,InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<VPNConnection> listGatewayConnections(String toGatewayId)throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

    @Nonnull
    @Override
    public Iterable<ResourceStatus> listGatewayStatus() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public Iterable<VPNGateway> listGateways() throws CloudException,InternalException {
	    ArrayList<VPNGateway> gateways = new ArrayList<VPNGateway>();

		ArrayList<VPN> list = (ArrayList<VPN>) listVPNs();
		if(list != null){ 
			for(VPN vpn: list){
				 ArrayList<VPNGateway> items = (ArrayList<VPNGateway>) listGateways(vpn.getProviderVpnId());
				 if(items != null){
					 gateways.addAll(items);
				 }
			}
		}		
		return gateways;
	}
	
	private Iterable<VPNGateway> listGateways(String vpnId) throws CloudException,InternalException {
		// TODO Auto-generated method stub
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        String resourceDir = NETWORKING_SERVICES + "/" + vpnId + "/gateway";
        AzureMethod method = new AzureMethod(provider);
        Document doc = method.getAsXML(ctx.getAccountNumber(), resourceDir);
                
        NodeList entries = doc.getElementsByTagName("Gateway");
        ArrayList<VPNGateway> list = new ArrayList<VPNGateway>();

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            VPNGateway gateway= toVPNGateway(ctx, entry,vpnId );
            
            if( gateway != null ) {
            	list.add(gateway);
            }
        }        
        return list;
	}

	@Override
	public Iterable<VPNGateway> listGatewaysWithBgpAsn(String bgpAsn) throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<VPNConnection> listVPNConnections(String toVpnId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

    @Nonnull
    @Override
    public Iterable<ResourceStatus> listVPNStatus() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public Iterable<VPN> listVPNs() throws CloudException, InternalException {

        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), NETWORKING_SERVICES+"/virtualnetwork");
                
        NodeList entries = doc.getElementsByTagName("VirtualNetworkSite");
        ArrayList<VPN> list = new ArrayList<VPN>();

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            VPN vpn = toVPN(ctx, entry);
            if( vpn != null ) {
            	list.add(vpn);
            }
        }        
        return list;
	}

	@Override
	public Iterable<VPNProtocol> listSupportedVPNProtocols()throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true;
	}
	
    private @Nullable VPN toVPN(@Nonnull ProviderContext ctx, @Nullable Node entry) throws CloudException, InternalException {
        if( entry == null ) {
            return null;
        }
        VPN vpn= new VPN();
        vpn.setCurrentState(VPNState.AVAILABLE);

        HashMap<String,String> tags = new HashMap<String, String>();
      
        NodeList attributes = entry.getChildNodes();

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;
            String nodeName = attribute.getNodeName();
            
            if( nodeName.equalsIgnoreCase("name") && attribute.hasChildNodes() ) {
            	vpn.setProviderVpnId(attribute.getFirstChild().getNodeValue().trim());
            	//vpn.setName(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("label") && attribute.hasChildNodes() ) {
            	vpn.setDescription(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("Id") && attribute.hasChildNodes() ) {
            	//vpn.setProviderVpnId(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("State") && attribute.hasChildNodes() ) {
            	String status = attribute.getFirstChild().getNodeValue().trim();
            	
            	if("Created".equalsIgnoreCase(status)){
            		vpn.setCurrentState(VPNState.AVAILABLE);
            	}        
            }            
            else if( nodeName.equalsIgnoreCase("AffinityGroup") && attribute.hasChildNodes() ) {
            	tags.put("AffinityGroup", attribute.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("AddressSpace") && attribute.hasChildNodes() ) {
                NodeList addressSpaces = attribute.getChildNodes();
                
                for( int k=0; k<addressSpaces.getLength(); k++ ) {
                    Node addressSpace = addressSpaces.item(k);
                    
                    if( addressSpace.getNodeName().equalsIgnoreCase("AddressPrefixes") && addressSpace.hasChildNodes() ) {
                       
                    	NodeList addressPrefixes  = addressSpace.getChildNodes();
                    	
                    	ArrayList<String> vlanIds = new ArrayList<String>();
                    	
                        for( int l=0; l<addressPrefixes.getLength(); l++ ) {
                            Node addressPrefix = addressPrefixes.item(l);                               

                            if( addressPrefix.getNodeName().equalsIgnoreCase("AddressPrefix") && addressPrefix.hasChildNodes() ) {
                            	vlanIds.add(addressPrefix.getFirstChild().getNodeValue().trim());
                            }                            
                        }
                        vpn.setProviderVlanIds(vlanIds.toArray(new String[vlanIds.size()]));
                        
                    }
                }
            }
        }
        if( vpn.getProviderVpnId() == null ) {
            return null;
        }
        if( vpn.getName() == null ) {
        	vpn.setName(vpn.getProviderVpnId());
        }
        if( vpn.getDescription() == null ) {
        	vpn.setDescription(vpn.getName());
        }       
        vpn.setTags(tags);
        
        return vpn;
    }
    
    private @Nullable VPNGateway toVPNGateway(@Nonnull ProviderContext ctx, @Nullable Node entry, String vpnId) throws CloudException, InternalException {
        if( entry == null ) {
            return null;
        }      
        VPNGateway gateway= new VPNGateway();       
     
        gateway.setCurrentState(VPNGatewayState.PENDING);
        HashMap<String,String> tags = new HashMap<String, String>();
        tags.put(VPN_ID_KEY, vpnId);
        NodeList attributes = entry.getChildNodes();
       
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;
            String nodeName = attribute.getNodeName();
            
            if( nodeName.equalsIgnoreCase("State") && attribute.hasChildNodes() ) { 
            	String status = attribute.getFirstChild().getNodeValue().trim();
            	
            	if("Provisioning".equalsIgnoreCase(status) || "Provisioned".equalsIgnoreCase(status)){
            		gateway.setCurrentState(VPNGatewayState.AVAILABLE);
            	}
            	else if("Deprovisioning".equalsIgnoreCase(status)){
            		gateway.setCurrentState(VPNGatewayState.DELETING);
            	}
            	else if("NotProvisioned".equalsIgnoreCase(status)){
            		gateway.setCurrentState(VPNGatewayState.DELETED);
            	}
            }           
            else if( nodeName.equalsIgnoreCase("VIPAddress") && attribute.hasChildNodes() ) {
            	gateway.setEndpoint(attribute.getFirstChild().getNodeValue().trim());            	
            }
        }
        if( vpnId == null || gateway.getEndpoint() == null ) {        	
        	return null;
        }else{
        	gateway.setProviderVpnGatewayId(vpnId + OBJECT_NAME_SPLIT + gateway.getEndpoint());
        	
        }
        if( gateway.getName() == null ) {
        	gateway.setName(gateway.getEndpoint());
        }
        if( gateway.getDescription() == null ) {
        	gateway.setDescription(gateway.getName());
        }       
        gateway.setTags(tags);
        
        return gateway;
    }	

}
