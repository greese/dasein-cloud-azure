package org.dasein.cloud.azure.network;


import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.dasein.cloud.*;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.network.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AzureVlanSupport implements VLANSupport {
    static private final Logger logger = Azure.getLogger(AzureVlanSupport.class);

	
	static private final String NETWORKING_SERVICES = "/services/networking";

    private Azure provider;

    public AzureVlanSupport(Azure provider) { this.provider = provider; }
	

	@Override
	public String[] mapServiceAction(ServiceAction action) {
		return new String[0];
	}

	@Override
	public void addRouteToAddress(String toRoutingTableId, IPVersion version,String destinationCidr, String address) throws CloudException,InternalException {
        throw new OperationNotSupportedException("Routing tables not supported");

	}

	@Override
	public void addRouteToGateway(String toRoutingTableId, IPVersion version,String destinationCidr, String gatewayId) throws CloudException,InternalException {
        throw new OperationNotSupportedException("Routing tables not supported");

	}

	@Override
	public void addRouteToNetworkInterface(String toRoutingTableId,
			IPVersion version, String destinationCidr, String nicId)
			throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables not supported");

	}

	@Override
	public void addRouteToVirtualMachine(String toRoutingTableId,
			IPVersion version, String destinationCidr, String vmId)
			throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables not supported");

	}

	@Override
	public boolean allowsNewNetworkInterfaceCreation() throws CloudException,
			InternalException {
		return false;
	}

	@Override
	public boolean allowsNewVlanCreation() throws CloudException,
			InternalException {
		return true;
	}

	@Override
	public boolean allowsNewSubnetCreation() throws CloudException,InternalException {
		return true;
	}

    @Override
    public boolean allowsMultipleTrafficTypesOverSubnet() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean allowsMultipleTrafficTypesOverVlan() throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public void assignRoutingTableToSubnet(String subnetId,String routingTableId) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables not supported");

	}

	@Override
	public void assignRoutingTableToVlan(String vlanId, String routingTableId)
			throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables not supported");

	}

	@Override
	public void attachNetworkInterface(String nicId, String vmId, int index)
			throws CloudException, InternalException {
        throw new OperationNotSupportedException("Network interfaces not supported");

	}

	@Override
	public String createInternetGateway(String forVlanId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String createRoutingTable(String forVlanId, String name,
			String description) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables not supported");
	}

	@Override
	public NetworkInterface createNetworkInterface(NICCreateOptions options)throws CloudException, InternalException {
        throw new OperationNotSupportedException("Network interfaces not supported");
	}

	@Override
	public Subnet createSubnet(String cidr, String inProviderVlanId,String name, String description) throws CloudException,InternalException {
		// TODO Auto-generated method stub
		return null;
	}

    @Nonnull
    @Override
    public Subnet createSubnet(@Nonnull SubnetCreateOptions subnetCreateOptions) throws CloudException, InternalException {
        //todo
         return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public VLAN createVlan(String cidr, String name, String description, String domainName, String[] dnsServers, String[] ntpServers)throws CloudException, InternalException {
        if( logger.isTraceEnabled() ) {
            logger.trace("ENTER: " + AzureVlanSupport.class.getName() + ".createVlan()");
        }
        		
		int mask = 32;
		if(cidr != null){
			String[] ipInfo = cidr.split("/");
			
			if(ipInfo != null && ipInfo.length >1){
				mask = Integer.valueOf(ipInfo[1]);				
			}			
		}
		if(mask < 8 || mask > 29){
			logger.error("Azure address prefix size has to between /8 and /29");
			throw new InternalException("Azure address prefix size has to between /8 and /29");
		}
        
        try {
        	
            ProviderContext ctx = provider.getContext();

            if( ctx == null ) {
                throw new AzureConfigException("No context was specified for this request");
            }
                       
            AzureMethod method = new AzureMethod(provider);
            StringBuilder xml = new StringBuilder();
                        
            xml.append("<NetworkConfiguration xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns=\"http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration\"");
            xml.append("<VirtualNetworkConfiguration>");
            xml.append("<VirtualNetworkSites>");
            //todo: why don't we pass in the name?  i am passing name parameter for now
            // xml.append("VirtualNetworkSite name=\"" + AzureVPNSupport.ENSTRATUS_DEFAULT_VPN+ "\" AffinityGroup=\"" +  this.getAffinityGroup(name) +"\"");
            xml.append("VirtualNetworkSite name=\"" + name+ "\" AffinityGroup=\"" +  this.getAffinityGroup(name) +"\"");
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
            
            System.out.println("body -> " + xml.toString());
            String resourceDir = NETWORKING_SERVICES + "/media";
            method.post(ctx.getAccountNumber(),resourceDir, xml.toString());

            //dmayne 201230429: list vlans and find the one we need
            return getVlan(name);
        }
        finally {
            if( logger.isTraceEnabled() ) {
                logger.trace("EXIT: " + AzureVlanSupport.class.getName() + ".createVlan()");
            }
        }
	}
	
	private String getAffinityGroup(String vlanName) throws InternalException,CloudException{
		return provider.getAffinityGroup();
		//return "Group1";
	}

	@Override
	public void detachNetworkInterface(String nicId) throws CloudException,InternalException {
        throw new OperationNotSupportedException("Network interfaces not supported");

	}

	@Override
	public int getMaxNetworkInterfaceCount() throws CloudException,InternalException {
		return 0;
	}

	@Override
	public int getMaxVlanCount() throws CloudException, InternalException {
		return 5;
	}

	@Override
	public String getProviderTermForNetworkInterface(Locale locale) {
		return "network interface";
	}

	@Override
	public String getProviderTermForSubnet(Locale locale) {
		return "Subnet";
	}

	@Override
	public String getProviderTermForVlan(Locale locale) {
		// TODO Auto-generated method stub
		return "Address Space";
	}

	@Override
	public NetworkInterface getNetworkInterface(String nicId) throws CloudException, InternalException {
		return null;
	}

	@Override
	public RoutingTable getRoutingTableForSubnet(String subnetId)throws CloudException, InternalException {
		return null;
	}

	@Override
	public Requirement getRoutingTableSupport() throws CloudException,InternalException {
        return Requirement.NONE;
	}

	@Override
	public RoutingTable getRoutingTableForVlan(String vlanId)throws CloudException, InternalException {
        return null;
	}

	@Override
	public Subnet getSubnet(String subnetId) throws CloudException,InternalException {
		ArrayList<VLAN> list = (ArrayList<VLAN>) listVlans();
		if(list != null){ 
			for(VLAN vlan: list){
				ArrayList<Subnet> subnets  = (ArrayList<Subnet>) listSubnets(vlan.getProviderVlanId());
				for(Subnet subnet: subnets){
					if(subnet.getProviderSubnetId().equals(subnetId)){
						return subnet;
					}				
				}		
			}
		}
		return null;
	}

	@Override
	public Requirement getSubnetSupport() throws CloudException,InternalException {
		return Requirement.REQUIRED;
	}

	@Override
	public VLAN getVlan(String vlanId) throws CloudException, InternalException {
		ArrayList<VLAN> list = (ArrayList<VLAN>) listVlans();
		if(list != null){ 
			for(VLAN vlan: list){
				if(vlan.getProviderVlanId().equals(vlanId) || vlan.getName().equalsIgnoreCase(vlanId)){
					return vlan;
				}			
			}
		}
		return null;		
	}

    @Nonnull
    @Override
    public Requirement identifySubnetDCRequirement() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Override
    public boolean isConnectedViaInternetGateway(@Nonnull String s) throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public boolean isNetworkInterfaceSupportEnabled() throws CloudException,InternalException {
		return false;
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true;
	}

	@Override
	public boolean isSubnetDataCenterConstrained() throws CloudException,InternalException {
		return true;
	}

	@Override
	public boolean isVlanDataCenterConstrained() throws CloudException,InternalException {
		return true;
	}

	@Override
	public Collection<String> listFirewallIdsForNIC(String nicId)throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

    @Nonnull
    @Override
    public Iterable<ResourceStatus> listNetworkInterfaceStatus() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public Iterable<NetworkInterface> listNetworkInterfaces()throws CloudException, InternalException {
        return Collections.emptyList();
	}

	@Override
	public Iterable<NetworkInterface> listNetworkInterfacesForVM(String forVmId)throws CloudException, InternalException {
        return Collections.emptyList();
	}

	@Override
	public Iterable<NetworkInterface> listNetworkInterfacesInSubnet(
			String subnetId) throws CloudException, InternalException {
        return Collections.emptyList();
	}

	@Override
	public Iterable<NetworkInterface> listNetworkInterfacesInVLAN(String vlanId)
			throws CloudException, InternalException {
        return Collections.emptyList();
	}

    @Nonnull
    @Override
    public Iterable<Networkable> listResources(@Nonnull String inVlanId) throws CloudException, InternalException {
        return Collections.emptyList();
    }

    @Override
	public Iterable<RoutingTable> listRoutingTables(String inVlanId)
			throws CloudException, InternalException {
        return Collections.emptyList();
	}

	@Override
	public Iterable<Subnet> listSubnets(String inVlanId) throws CloudException,InternalException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), NETWORKING_SERVICES+"/virtualnetwork");

        NodeList entries = doc.getElementsByTagName("VirtualNetworkSite");
        ArrayList<Subnet> list = new ArrayList<Subnet>();

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            NodeList attributes = entry.getChildNodes();

            String vlanId;
            boolean found = false;

            for( int j=0; j<attributes.getLength(); j++ ) {
                Node attribute = attributes.item(j);
                if(attribute.getNodeType() == Node.TEXT_NODE) continue;
                String nodeName = attribute.getNodeName();

                if (nodeName.equalsIgnoreCase("id") && attribute.hasChildNodes() ) {
                    vlanId = attribute.getFirstChild().getNodeValue().trim();
                    if (vlanId.equalsIgnoreCase(inVlanId)) {
                        found = true;
                        continue;
                    }
                }

                //hopefully we have found the right vlan
                if (found) {
                    if (nodeName.equalsIgnoreCase("subnets") && attribute.hasChildNodes()) {
                        NodeList sNets = attribute.getChildNodes();
                        for (int k=0; k<sNets.getLength(); k++) {
                            Node sAttrib = sNets.item(k);

                            Subnet subnet = toSubnet(ctx, sAttrib, inVlanId);
                            if( subnet != null ) {
                                list.add(subnet);
                            }
                        }
                    }
                }
            }
        }
        return list;
	}

	@Override
	public Iterable<IPVersion> listSupportedIPVersions() throws CloudException,
			InternalException {
        return Collections.singletonList(IPVersion.IPV4);
	}

    @Nonnull
    @Override
    public Iterable<ResourceStatus> listVlanStatus() throws CloudException, InternalException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), NETWORKING_SERVICES+"/virtualnetwork");

        NodeList entries = doc.getElementsByTagName("VirtualNetworkSite");
        ArrayList<ResourceStatus> list = new ArrayList<ResourceStatus>();

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            ResourceStatus status = toVLANStatus(ctx, entry);
            if( status != null ) {
                list.add(status);
            }
        }
        return list;
    }

    @Override
	public Iterable<VLAN> listVlans() throws CloudException, InternalException {

        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), NETWORKING_SERVICES+"/virtualnetwork");
                
        NodeList entries = doc.getElementsByTagName("VirtualNetworkSite");
        ArrayList<VLAN> list = new ArrayList<VLAN>();

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            ArrayList<VLAN> vlans = (ArrayList<VLAN>) toVLAN(ctx, entry);
            if( vlans != null ) {
            	list.addAll(vlans);
            }
        }        
        return list;
	}
	
	public Iterable<VLAN> listVlans(String vpnId) throws CloudException, InternalException {

        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), NETWORKING_SERVICES+"/virtualnetwork");
                
        NodeList entries = doc.getElementsByTagName("VirtualNetworkSite");
        ArrayList<VLAN> list = new ArrayList<VLAN>();

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            ArrayList<VLAN> vlans = (ArrayList<VLAN>) toVLAN(ctx, entry);
            if( vlans != null ) {
            	list.addAll(vlans);
            }
        }        
        return list;
	}
		

	@Override
	public void removeInternetGateway(String forVlanId) throws CloudException,InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeNetworkInterface(String nicId) throws CloudException,
			InternalException {
        throw new OperationNotSupportedException("Network interfaces not supported");

	}

	@Override
	public void removeRoute(String inRoutingTableId, String destinationCidr)
			throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables not supported");

	}

	@Override
	public void removeRoutingTable(String routingTableId)
			throws CloudException, InternalException {
        throw new OperationNotSupportedException("Routing tables not supported");

	}

	@Override
	public void removeSubnet(String providerSubnetId) throws CloudException,InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeVlan(String vlanId) throws CloudException,InternalException {
		// TODO Auto-generated method stub

	}

    @Override
    public void removeVLANTags(@Nonnull String s, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeVLANTags(@Nonnull String[] strings, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public boolean supportsInternetGatewayCreation() throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsRawAddressRouting() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
    public void updateVLANTags(@Nonnull String s, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateVLANTags(@Nonnull String[] strings, @Nonnull Tag... tags) throws CloudException, InternalException {
        //To change body of implemented methods use File | Settings | File Templates.
    }




    private @Nullable Iterable<VLAN> toVLAN(@Nonnull ProviderContext ctx, @Nullable Node entry) throws CloudException, InternalException {
        if( entry == null ) {
            return null;
        }

        ArrayList<VLAN> list= new ArrayList<VLAN>();

        VLAN vlan = new VLAN();
        vlan.setProviderOwnerId(ctx.getAccountNumber());
        vlan.setProviderRegionId(ctx.getRegionId());
        vlan.setProviderDataCenterId(ctx.getRegionId());
        vlan.setSupportedTraffic(IPVersion.IPV4);

        HashMap<String,String> tags = new HashMap<String, String>();
        NodeList attributes = entry.getChildNodes();
        String id;
        String value;
        VLANState state;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;
            String nodeName = attribute.getNodeName();

            if( nodeName.equalsIgnoreCase("name") && attribute.hasChildNodes() ) {
                vlan.setName(attribute.getFirstChild().getNodeValue().trim());
            }
            else if (nodeName.equalsIgnoreCase("label") && attribute.hasChildNodes() ) {
                vlan.setDescription(attribute.getFirstChild().getNodeValue().trim());
            }
            else if (nodeName.equalsIgnoreCase("id") && attribute.hasChildNodes() ) {
                id = attribute.getFirstChild().getNodeValue().trim();
                tags.put(AzureVPNSupport.VPN_ID_KEY, id);
                vlan.setProviderVlanId(id);
            }
            else if (nodeName.equalsIgnoreCase("affinitygroup") && attribute.hasChildNodes() ) {
                if (!ctx.getRegionId().equalsIgnoreCase(attribute.getFirstChild().getNodeValue().trim())) {
                    return null;
                }
                tags.put("AffinityGroup", attribute.getFirstChild().getNodeValue().trim());
            }
            else if (nodeName.equalsIgnoreCase("state") && attribute.hasChildNodes() ) {
                value = attribute.getFirstChild().getNodeValue().trim();

                if( value.equalsIgnoreCase("created") || value.equalsIgnoreCase("updating")) {
                    state = VLANState.AVAILABLE;
                }
                else if( value.equalsIgnoreCase("creating") ) {
                    state = VLANState.PENDING;
                }
                else {
                    logger.warn("Unknown VLAN state: " + value);
                    state = null;
                }
                vlan.setCurrentState(state);
            }
            else if( nodeName.equalsIgnoreCase("AddressSpace") && attribute.hasChildNodes() ) {
                NodeList addressSpaces = attribute.getChildNodes();

                for( int k=0; k<addressSpaces.getLength(); k++ ) {
                    Node addressSpace = addressSpaces.item(k);

                    if( addressSpace.getNodeName().equalsIgnoreCase("AddressPrefixes") && addressSpace.hasChildNodes() ) {

                        NodeList addressPrefixes  = addressSpace.getChildNodes();

                        for( int l=0; l<addressPrefixes.getLength(); l++ ) {
                            Node addressPrefix = addressPrefixes.item(l);

                            if( addressPrefix.getNodeName().equalsIgnoreCase("AddressPrefix") && addressPrefix.hasChildNodes() ) {
                                //vlan.setProviderVlanId(addressPrefix.getFirstChild().getNodeValue().trim());
                                vlan.setCidr(addressPrefix.getFirstChild().getNodeValue().trim());

                                if( vlan.getName() == null ) {
                                    vlan.setName(vlan.getProviderVlanId());
                                }
                                if( vlan.getDescription() == null ) {
                                    vlan.setDescription(vlan.getName());
                                }
                                vlan.setTags(tags);

                                list.add(vlan);
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    private @Nullable ResourceStatus toVLANStatus(@Nonnull ProviderContext ctx, @Nullable Node entry) throws CloudException, InternalException {
        if( entry == null ) {
            return null;
        }
        String id= null;
        String value = null;
        VLANState state = null;


        NodeList attributes = entry.getChildNodes();
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;
            String nodeName = attribute.getNodeName();

            if (nodeName.equalsIgnoreCase("id") && attribute.hasChildNodes() ) {
                id = attribute.getFirstChild().getNodeValue().trim();
            }
            else if (nodeName.equalsIgnoreCase("state") && attribute.hasChildNodes() ) {
                value = attribute.getFirstChild().getNodeValue().trim();

                if( value.equalsIgnoreCase("created") || value.equalsIgnoreCase("updating")) {
                    state = VLANState.AVAILABLE;
                }
                else if( value.equalsIgnoreCase("creating") ) {
                    state = VLANState.PENDING;
                }
                else {
                    logger.warn("Unknown VLAN state: " + value);
                    state = null;
                }
            }
            else if (nodeName.equalsIgnoreCase("affinitygroup") && attribute.hasChildNodes() ) {
                if (!ctx.getRegionId().equalsIgnoreCase(attribute.getFirstChild().getNodeValue().trim())) {
                    return null;
                }
            }

        }
        ResourceStatus status = new ResourceStatus(id, state);
        return status;
    }

    private @Nullable Subnet toSubnet(@Nonnull ProviderContext ctx, @Nullable Node entry, @Nonnull String vlanId) throws CloudException, InternalException {
        if( entry == null ) {
            return null;
        }

        NodeList attributes = entry.getChildNodes();
        String name = null;
        String cidr= null;

        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;

            String nodeName = attribute.getNodeName();

            if( nodeName.equalsIgnoreCase("name") && attribute.hasChildNodes() ) {
                name = attribute.getFirstChild().getNodeValue().trim();
            }
            else if( nodeName.equalsIgnoreCase("AddressPrefix") && attribute.hasChildNodes() ) {
                cidr = attribute.getFirstChild().getNodeValue().trim();
            }
        }

        Subnet subnet = Subnet.getInstance(ctx.getAccountNumber(), ctx.getRegionId(), vlanId, cidr, SubnetState.AVAILABLE, name, name, cidr);
        subnet.constrainedToDataCenter(ctx.getRegionId());
        return subnet;
    }
}
