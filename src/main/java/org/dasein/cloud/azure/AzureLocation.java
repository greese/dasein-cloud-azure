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

package org.dasein.cloud.azure;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.compute.image.AzureMachineImage;
import org.dasein.cloud.dc.*;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.time.Minute;
import org.dasein.util.uom.time.TimePeriod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
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
    static private final Logger logger = Azure.getLogger(AzureLocation.class);
    static private final String LOCATIONS = "/locations";
    
    private Azure provider;
    
    AzureLocation(Azure provider) { this.provider = provider; }

    private transient volatile AzureLocationCapabilities capabilities;
    @Nonnull
    @Override
    public DataCenterCapabilities getCapabilities() throws InternalException, CloudException {
        if( capabilities == null ) {
            capabilities = new AzureLocationCapabilities(provider);
        }
        return capabilities;
    }

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

        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        Cache<DataCenter> cache = Cache.getInstance(provider, "dataCenters", DataCenter.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Minute>(15, TimePeriod.MINUTE));
        Collection<DataCenter> dcs = (Collection<DataCenter>)cache.get(ctx);

        if( dcs == null ) {
            dcs = new ArrayList<DataCenter>();
            logger.info("Get affinity group for "+providerRegionId+" for account "+ctx.getAccountNumber());
            AzureMethod method = new AzureMethod(provider);

            Document doc = method.getAsXML(ctx.getAccountNumber(), "/affinitygroups");

            NodeList entries = doc.getElementsByTagName("AffinityGroup");

            String affinityGroup = "";
            String affinityRegion = "";

            for (int i = 0; i<entries.getLength(); i++) {
                Node entry = entries.item(i);

                NodeList attributes = entry.getChildNodes();

                for( int j=0; j<attributes.getLength(); j++ ) {
                    Node attribute = attributes.item(j);
                    if(attribute.getNodeType() == Node.TEXT_NODE) continue;
                    String nodeName = attribute.getNodeName();

                    if (nodeName.equalsIgnoreCase("name") && attribute.hasChildNodes() ) {
                        affinityGroup = attribute.getFirstChild().getNodeValue().trim();
                    }
                    else if (nodeName.equalsIgnoreCase("location") && attribute.hasChildNodes()) {
                        affinityRegion = attribute.getFirstChild().getNodeValue().trim();
                        if (providerRegionId.equalsIgnoreCase(affinityRegion)) {
                            if (affinityGroup != null && !affinityGroup.equals("")) {
                                DataCenter dc = new DataCenter();

                                dc.setActive(true);
                                dc.setAvailable(true);
                                dc.setName(affinityGroup);
                                dc.setProviderDataCenterId(affinityGroup);
                                dc.setRegionId(providerRegionId);
                                dcs.add(dc);
                            }
                        }
                        else {
                            affinityGroup = null;
                            affinityRegion = null;
                        }
                    }
                }
            }
            cache.put(ctx, dcs);
            if (dcs.isEmpty()) {
                logger.info("Create new affinity group for "+providerRegionId);
                //create new affinityGroup
                String name = "Affinity"+(providerRegionId.replaceAll(" ", ""));
                logger.info(name);
                String label;
                try {
                    StringBuilder xml = new StringBuilder();

                    try {
                        label = new String(Base64.encodeBase64(name.getBytes("utf-8")));
                    }
                    catch( UnsupportedEncodingException e ) {
                        throw new InternalException(e);
                    }

                    xml.append("<CreateAffinityGroup xmlns=\"http://schemas.microsoft.com/windowsazure\">") ;
                    xml.append("<Name>").append(name).append("</Name>");
                    xml.append("<Label>").append(label).append("</Label>");
                    xml.append("<Location>").append(providerRegionId).append("</Location>");
                    xml.append("</CreateAffinityGroup>");
                    method.post(ctx.getAccountNumber(),"/affinitygroups", xml.toString());
                }
                catch (CloudException e) {
                    logger.error("Unable to create affinity group",e);
                    throw new CloudException(e);
                }
                affinityGroup = name;
                DataCenter dc = new DataCenter();

                dc.setActive(true);
                dc.setAvailable(true);
                dc.setName(affinityGroup);
                dc.setProviderDataCenterId(affinityGroup);
                dc.setRegionId(providerRegionId);
                dcs.add(dc);
                cache.put(ctx, dcs);
            }
        }
        return dcs;
    }

    @Override
    public @Nonnull Collection<Region> listRegions() throws InternalException, CloudException {
        ProviderContext ctx = provider.getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        Cache<Region> cache = Cache.getInstance(provider, "regions", Region.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Minute>(15, TimePeriod.MINUTE));
        Collection<Region> regions = (Collection<Region>)cache.get(ctx);

        if( regions == null ) {
            regions = new ArrayList<Region>();
            AzureMethod method = new AzureMethod(provider);

            Document doc = method.getAsXML(ctx.getAccountNumber(), LOCATIONS);

            if( doc == null ) {
                return Collections.emptyList();
            }
            NodeList entries = doc.getElementsByTagName("Location");

            for( int i=0; i<entries.getLength(); i++ ) {
                Node entry = entries.item(i);
                Region region = toRegion(entry);

                if( region != null ) {
                    regions.add(region);
                }

            }
            cache.put(ctx, regions);
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

    @Override
    public Collection<ResourcePool> listResourcePools(String providerDataCenterId) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    @Override
    public ResourcePool getResourcePool(String providerResourcePoolId) throws InternalException, CloudException {
        return null;
    }
}
