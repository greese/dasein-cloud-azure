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

package org.dasein.cloud.azure;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
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

        final String cacheName = "AzureLocation.isSubscribed." + toService;
        Cache<Boolean> cache = org.dasein.cloud.util.Cache.getInstance(provider, cacheName, Boolean.class, CacheLevel.REGION_ACCOUNT);
        final Iterable<Boolean> cachedIsSubscribed = cache.get(ctx);
        if (cachedIsSubscribed != null && cachedIsSubscribed.iterator().hasNext()) {
            final Boolean isSubscribed = cachedIsSubscribed.iterator().next();
            if (isSubscribed != null) {
                return isSubscribed;
            }
        }

        Document doc = method.getAsXML(ctx.getAccountNumber(), LOCATIONS);

        if( doc == null ) {
            cache.put(ctx, Collections.singleton(false));
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
                    cache.put(ctx, Collections.singleton(subscribed));
                    return subscribed;
                }
            }
        }
        cache.put(ctx, Collections.singleton(false));
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

        Collection<DataCenter> dcs = new ArrayList<DataCenter>();
        DataCenter dc = new DataCenter();

        dc.setActive(true);
        dc.setAvailable(true);
        dc.setName(region.getName());
        dc.setProviderDataCenterId(providerRegionId);
        dc.setRegionId(providerRegionId);
        dcs.add(dc);

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

    /**
     * Lists all storage pools
     *
     * @return all storage pools supported for this cloud in the context region
     * @throws org.dasein.cloud.InternalException an error occurred locally in processing the request
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider or the cloud provider did not approve of the request
     */
    @Nonnull
    @Override
    public Collection<StoragePool> listStoragePools() throws InternalException, CloudException {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public StoragePool getStoragePool(String providerStoragePoolId) throws InternalException, CloudException {
        return null;
    }

    @Nonnull
    @Override
    public Collection<Folder> listVMFolders() throws InternalException, CloudException {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Folder getVMFolder(String providerVMFolderId) throws InternalException, CloudException {
        return null;
    }
}