package org.dasein.cloud.azure.network;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;

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
		// TODO Auto-generated method stub
		return new String[0];
	}

	@Override
	public void addRouteToAddress(String toRoutingTableId, IPVersion version,String destinationCidr, String address) throws CloudException,InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addRouteToGateway(String toRoutingTableId, IPVersion version,String destinationCidr, String gatewayId) throws CloudException,InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addRouteToNetworkInterface(String toRoutingTableId,
			IPVersion version, String destinationCidr, String nicId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addRouteToVirtualMachine(String toRoutingTableId,
			IPVersion version, String destinationCidr, String vmId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean allowsNewNetworkInterfaceCreation() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean allowsNewVlanCreation() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return false;
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
		// TODO Auto-generated method stub

	}

	@Override
	public void assignRoutingTableToVlan(String vlanId, String routingTableId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void attachNetworkInterface(String nicId, String vmId, int index)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NetworkInterface createNetworkInterface(NICCreateOptions options)throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Subnet createSubnet(String cidr, String inProviderVlanId,String name, String description) throws CloudException,InternalException {
		// TODO Auto-generated method stub
		return null;
	}

    @Nonnull
    @Override
    public Subnet createSubnet(@Nonnull SubnetCreateOptions subnetCreateOptions) throws CloudException, InternalException {
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
            xml.append("VirtualNetworkSite name=\"" + AzureVPNSupport.ENSTRATUS_DEFAULT_VPN+ "\" AffinityGroup=\"" +  this.getAffinityGroup(name) +"\"");
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

	@Override
	public void detachNetworkInterface(String nicId) throws CloudException,InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMaxNetworkInterfaceCount() throws CloudException,InternalException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxVlanCount() throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getProviderTermForNetworkInterface(Locale locale) {
		return "network interface";
	}

	@Override
	public String getProviderTermForSubnet(Locale locale) {
		// TODO Auto-generated method stub
		return "Subnet";
	}

	@Override
	public String getProviderTermForVlan(Locale locale) {
		// TODO Auto-generated method stub
		return "Address Space";
	}

	@Override
	public NetworkInterface getNetworkInterface(String nicId) throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RoutingTable getRoutingTableForSubnet(String subnetId)throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Requirement getRoutingTableSupport() throws CloudException,InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RoutingTable getRoutingTableForVlan(String vlanId)throws CloudException, InternalException {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VLAN getVlan(String vlanId) throws CloudException, InternalException {
		ArrayList<VLAN> list = (ArrayList<VLAN>) listVlans();
		if(list != null){ 
			for(VLAN vlan: list){
				if(vlan.getProviderVlanId().equals(vlanId)){
					return vlan;
				}			
			}
		}
		return null;		
	}

    @Nonnull
    @Override
    public Requirement identifySubnetDCRequirement() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isConnectedViaInternetGateway(@Nonnull String s) throws CloudException, InternalException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public boolean isNetworkInterfaceSupportEnabled() throws CloudException,InternalException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isSubnetDataCenterConstrained() throws CloudException,InternalException {
		// TODO Auto-generated method stub
		return false;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<NetworkInterface> listNetworkInterfacesForVM(String forVmId)throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<NetworkInterface> listNetworkInterfacesInSubnet(
			String subnetId) throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<NetworkInterface> listNetworkInterfacesInVLAN(String vlanId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

    @Nonnull
    @Override
    public Iterable<Networkable> listResources(@Nonnull String inVlanId) throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public Iterable<RoutingTable> listRoutingTables(String inVlanId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<Subnet> listSubnets(String inVlanId) throws CloudException,InternalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<IPVersion> listSupportedIPVersions() throws CloudException,
			InternalException {
		// TODO Auto-generated method stub
		return null;
	}

    @Nonnull
    @Override
    public Iterable<ResourceStatus> listVlanStatus() throws CloudException, InternalException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
		// TODO Auto-generated method stub

	}

	@Override
	public void removeRoute(String inRoutingTableId, String destinationCidr)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeRoutingTable(String routingTableId)
			throws CloudException, InternalException {
		// TODO Auto-generated method stub

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

        HashMap<String,String> tags = new HashMap<String, String>();
        NodeList attributes = entry.getChildNodes();
        String vpnName = null;
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            if(attribute.getNodeType() == Node.TEXT_NODE) continue;
            String nodeName = attribute.getNodeName();
            
            if( nodeName.equalsIgnoreCase("name") && attribute.hasChildNodes() ) {            	
            	vpnName = attribute.getFirstChild().getNodeValue().trim();
            	tags.put(AzureVPNSupport.VPN_ID_KEY, vpnName);
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
                            	VLAN vlan = new VLAN();
                            	vlan.setProviderVlanId(addressPrefix.getFirstChild().getNodeValue().trim());
                            	                               
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
}
