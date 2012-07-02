package org.dasein.cloud.azure;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterServices;
import org.dasein.cloud.dc.Region;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * Displays the available locations for Microsoft Azure services.
 * @author George Reese (george.reese@imaginary.com)
 * @since 2012.04.1
 * @version 2012.04.1
 */
public class AzureLocation implements DataCenterServices {
    static private final String LOCATIONS = "/locations";
    
    private Azure provider;
    
    AzureLocation(Azure provider) { this.provider = provider; }

    @Override
    public @Nullable DataCenter getDataCenter(@Nonnull String providerDataCenterId) throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();
        
        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        String regionId = ctx.getRegionId();
        
        if( regionId == null ) {
            throw new AzureConfigException("No region was specified for this request");
        }
        for( DataCenter dc : listDataCenters(regionId) ) {
            if( dc.getProviderDataCenterId().equals(providerDataCenterId) ) {
                return dc;
            }
        }
        return null;
    }

    @Override
    public @Nonnull String getProviderTermForDataCenter(@Nonnull Locale locale) {
        return "data center";
    }

    @Override
    public @Nonnull String getProviderTermForRegion(@Nonnull Locale locale) {
        return "region";
    }

    @Override
    public @Nullable Region getRegion(@Nonnull String providerRegionId) throws InternalException, CloudException {
        for( Region region : listRegions() ) {
            if( region.getProviderRegionId().equals(providerRegionId) ) {
                return region;
            }
        }
        return null;
    }

    public boolean isSubscribed(AzureService toService) throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), LOCATIONS);

        if( doc == null ) {
            return false;
        }
        NodeList entries = doc.getElementsByTagName("Location");

        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);

            if( entry != null ) {
                NodeList attributes = entry.getChildNodes();
                String regionId = null;
                boolean subscribed = false;
                
                for( int j=0; j<attributes.getLength(); j++ ) {
                    Node attribute = attributes.item(j);

                    if( attribute.getNodeName().equalsIgnoreCase("name") && attribute.hasChildNodes() ) {
                        regionId = attribute.getFirstChild().getNodeValue().trim();
                    }
                    else if( attribute.getNodeName().equalsIgnoreCase("availableservices") && attribute.hasChildNodes() ) {
                        NodeList services = attribute.getChildNodes();

                        for( int k=0; k<services.getLength(); k++ ) {
                            Node service = services.item(k);
                            
                            if( service != null && service.getNodeName().equalsIgnoreCase("availableservice") && service.hasChildNodes() ) {
                                String serviceName = service.getFirstChild().getNodeValue().trim();
                                
                                if( toService.toString().equalsIgnoreCase(serviceName) ) {
                                    subscribed = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if( regionId != null && regionId.equalsIgnoreCase(ctx.getRegionId()) ) {
                    return subscribed;
                }
            }
        }
        return false;
    }
    
    @Override
    public @Nonnull Collection<DataCenter> listDataCenters(@Nonnull String providerRegionId) throws InternalException, CloudException {
        Region region = getRegion(providerRegionId);
        
        if( region == null ) {
            return Collections.emptyList();
        }
        DataCenter dc = new DataCenter();
        
        dc.setActive(true);
        dc.setAvailable(true);
        dc.setName(region.getName() + " (A)");
        dc.setProviderDataCenterId(providerRegionId);
        dc.setRegionId(providerRegionId);
        return Collections.singletonList(dc);
    }

    @Override
    public @Nonnull Collection<Region> listRegions() throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        AzureMethod method = new AzureMethod(provider);

        Document doc = method.getAsXML(ctx.getAccountNumber(), LOCATIONS);

        if( doc == null ) {
            return Collections.emptyList();
        }
        NodeList entries = doc.getElementsByTagName("Location");
        ArrayList<Region> regions = new ArrayList<Region>();
        
        for( int i=0; i<entries.getLength(); i++ ) {
            Node entry = entries.item(i);
            Region region = toRegion(entry);
            
            if( region != null ) {
                regions.add(region);
            }
            
        }
        return regions;
    }
    
    private @Nullable Region toRegion(@Nullable Node entry) {
        if( entry == null ) {
            return null;
        }
        Region region = new Region();
        
        NodeList attributes = entry.getChildNodes();
        
        for( int i=0; i<attributes.getLength(); i++ ) {
            Node attribute = attributes.item(i);
            String nodeName = attribute.getNodeName();
            
            if( nodeName.equalsIgnoreCase("name") && attribute.hasChildNodes() ) {
                region.setProviderRegionId(attribute.getFirstChild().getNodeValue().trim());
            }
            else if( nodeName.equalsIgnoreCase("displayName") && attribute.hasChildNodes() ) {
                region.setName(attribute.getFirstChild().getNodeValue().trim());
            }
        }
        if( region.getProviderRegionId() == null ) {
            return null;
        }
        if( region.getName() == null ) {
            region.setName(region.getProviderRegionId());
        }
        region.setActive(true);
        region.setAvailable(true);
        region.setJurisdiction("US");
        return region;
    }
}
