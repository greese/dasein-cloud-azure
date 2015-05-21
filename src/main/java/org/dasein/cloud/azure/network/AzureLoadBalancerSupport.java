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

package org.dasein.cloud.azure.network;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureConfigException;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.compute.AzureComputeServices;
import org.dasein.cloud.azure.network.model.*;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.network.*;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.time.Minute;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by Vlad_Munthiu on 6/10/2014.
 */
public class AzureLoadBalancerSupport extends AbstractLoadBalancerSupport<Azure>{
    static private final Logger logger = Logger.getLogger(AzureLoadBalancerSupport.class);
    static private final Logger wire   = Azure.getWireLogger(AzureLoadBalancerSupport.class);

    public static final String RESOURCE_PROFILES = "/services/WATM/profiles";
    public static final String RESOURCE_PROFILE = "/services/WATM/profiles/%s";
    public static final String RESOURCE_DEFINITIONS = "/services/WATM/profiles/%s/definitions";
    public static final String RESOURCE_DEFINITION = "/services/WATM/profiles/%s/definitions/1";

    private volatile transient AzureLoadBalancerCapabilities capabilities;
    public static final String TRAFFIC_MANAGER_DNS_NAME = "trafficmanager.net";

    public AzureLoadBalancerSupport(@Nonnull Azure provider) {
        super(provider);

        capabilities = new AzureLoadBalancerCapabilities(provider);
    }

    /**
     * Provides access to meta-data about load balancer capabilities in the current region of this cloud.
     *
     * @return a description of the features supported by this region of this cloud
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nonnull
    @Override
    public LoadBalancerCapabilities getCapabilities() throws CloudException, InternalException {
        return capabilities;
    }

    /**
     * Indicates whether the current account has access to load balancer services in the current region.
     *
     * @return true if the current account has access to load balancer services in the current region
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while performing this action
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation while performing this action
     */
    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Override
    public LoadBalancerHealthCheck createLoadBalancerHealthCheck(@Nullable String name, @Nullable String description, @Nullable String host, @Nullable LoadBalancerHealthCheck.HCProtocol protocol, int port, @Nullable String path, int interval, int timeout, int healthyCount, int unhealthyCount) throws CloudException, InternalException{
        throw new OperationNotSupportedException("Health check should be created only when creating the load balancer for Microsoft Azure cloud");
    }

    @Override
    public LoadBalancerHealthCheck createLoadBalancerHealthCheck(@Nonnull HealthCheckOptions options) throws CloudException, InternalException{
        throw new OperationNotSupportedException("Health check should be created only when creating the load balancer for Microsoft Azure cloud");
    }


    @Override
    public @Nonnull String createLoadBalancer(@Nonnull LoadBalancerCreateOptions options) throws CloudException, InternalException {
        if(options.getHealthCheckOptions() == null)
            throw new InternalException("Cannot create load balancer without health check options");

        if(options.getName() == null || options.getName().isEmpty())
            throw new InternalException("Cannot create load balancer without a name");

        if(options.getHealthCheckOptions().getProtocol() != LoadBalancerHealthCheck.HCProtocol.HTTP && options.getHealthCheckOptions().getProtocol() != LoadBalancerHealthCheck.HCProtocol.HTTPS)
            throw new InternalException("Azure only supports HTTP and HTTPS protocol for HealthCheckOptions");

        ProfileModel profileModel = new ProfileModel();
        profileModel.setDomainName(String.format("%s.%s", options.getName(), TRAFFIC_MANAGER_DNS_NAME));
        profileModel.setName(options.getName());

        AzureMethod azureMethod = new AzureMethod(this.getProvider());
        try {
            azureMethod.post(RESOURCE_PROFILES, profileModel);
        }
        catch (JAXBException e)
        {
            logger.error(e.getMessage());
            throw new InternalException(e);
        }
        DefinitionModel definition = new DefinitionModel();

        //add dnsOptions
        DefinitionModel.DnsOptions dnsOptions = new DefinitionModel.DnsOptions();
        dnsOptions.setTimeToLiveInSeconds("300");
        definition.setDnsOptions(dnsOptions);
        //add monitor
        DefinitionModel.MonitorModel monitor = new DefinitionModel.MonitorModel();
        monitor.setIntervalInSeconds("30");
        monitor.setTimeoutInSeconds("10");
        monitor.setToleratedNumberOfFailures("3");
        monitor.setProtocol(options.getHealthCheckOptions().getProtocol().toString());
        monitor.setPort(String.valueOf(options.getHealthCheckOptions().getPort()));
        DefinitionModel.HttpOptionsModel httpOptions = new DefinitionModel.HttpOptionsModel();
        httpOptions.setVerb("GET");
        httpOptions.setRelativePath(options.getHealthCheckOptions().getPath());
        httpOptions.setExpectedStatusCode("200");
        monitor.setHttpOptions(httpOptions);

        ArrayList<DefinitionModel.MonitorModel> monitors = new ArrayList<DefinitionModel.MonitorModel>();
        monitors.add(monitor);
        definition.setMonitors(monitors);

        //add policy
        DefinitionModel.PolicyModel policy = new DefinitionModel.PolicyModel();
        String loadBalancingMethod = "RoundRobin";

        LbListener lbListener =  options.getListeners()[0];
        if(lbListener != null) {
            if (lbListener.getAlgorithm() != null && lbListener.getAlgorithm() == LbAlgorithm.SOURCE) {
                loadBalancingMethod = "Performance";
            }
            else if(lbListener.getAlgorithm() != null && lbListener.getAlgorithm() == LbAlgorithm.LEAST_CONN) {
                loadBalancingMethod = "Failover";
            }
            //default is RoundRobin
        }
        policy.setLoadBalancingMethod(loadBalancingMethod);

        ArrayList<DefinitionModel.EndPointModel> endPointsToAdd = new ArrayList<DefinitionModel.EndPointModel>();
        for(LoadBalancerEndpoint endPoint : options.getEndpoints())
        {
            DefinitionModel.EndPointModel endPointModel = new DefinitionModel.EndPointModel();
            endPointModel.setDomainName(endPoint.getEndpointValue());
            endPointModel.setStatus("Enabled");
            endPointModel.setType("CloudService");
            endPointsToAdd.add(endPointModel);
        }

        policy.setEndPoints(endPointsToAdd);
        definition.setPolicy(policy);

        try {
            azureMethod.post(String.format(RESOURCE_DEFINITIONS, options.getName()), definition);
        }
        catch (Exception ex)
        {
            try {
                //try to remove previously created profile from TrafficManager
                azureMethod.invoke("DELETE", this.getContext().getAccountNumber(),
                        String.format(RESOURCE_PROFILE, options.getName()), null);
                logger.error(ex.getMessage());
                throw new CloudException(ex);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage());
                throw new CloudException(e);
            }
        }

        return options.getName();

    }

    @Override
    public void removeLoadBalancer(@Nonnull String loadBalancerId) throws CloudException, InternalException
    {
        if(loadBalancerId == null || loadBalancerId.isEmpty())
        throw new InternalException("Cannot remove load balancer. Please specify the id for load balancer to remove");

        AzureMethod method = new AzureMethod(this.getProvider());
        method.invoke("DELETE", this.getContext().getAccountNumber(), String.format(RESOURCE_PROFILE, loadBalancerId), null);
    }

    @Override
    public @Nonnull Iterable<LoadBalancer> listLoadBalancers() throws CloudException, InternalException
    {
        ProviderContext ctx = this.getProvider().getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        ArrayList<LoadBalancer> loadBalancers = new ArrayList<LoadBalancer>();

        ProfilesModel profileResponseModels = getProfiles();

        if(profileResponseModels == null)
            return Collections.emptyList();

        if(profileResponseModels.getProfiles() == null)
            return Collections.emptyList();

        for (ProfileModel profile : profileResponseModels.getProfiles())
        {
            DefinitionModel definitionModel = getDefinition(profile.getName());

            LoadBalancer loadBalancer = toLoadBalancer(ctx, profile, definitionModel);

            loadBalancers.add(loadBalancer);
        }

        return loadBalancers;
    }

    @Override
    public LoadBalancer getLoadBalancer(@Nonnull String loadBalancerId) throws CloudException, InternalException
    {
        ProviderContext ctx = this.getProvider().getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }
        ProfileModel profileModel = getProfile(loadBalancerId);

        if(profileModel == null)
            return null;

        DefinitionModel definitionModel = getDefinition(loadBalancerId);

        return toLoadBalancer(ctx, profileModel, definitionModel);
    }

    private LoadBalancer toLoadBalancer(ProviderContext ctx, ProfileModel profileModel, DefinitionModel definitionModel) {

        LoadBalancerState lbState = definitionModel.getStatus().equalsIgnoreCase("enabled") ? LoadBalancerState.ACTIVE : LoadBalancerState.TERMINATED;
        String lbId = profileModel.getDomainName().substring(0, profileModel.getDomainName().indexOf("."));

        LoadBalancer loadBalancer = LoadBalancer.getInstance(ctx.getAccountNumber(), null, lbId, lbState,
                profileModel.getName(), profileModel.getName(), LoadBalancerAddressType.DNS, profileModel.getDomainName());
        loadBalancer.setProviderLBHealthCheckId(lbId);

        LbAlgorithm lbAlgorithm = LbAlgorithm.ROUND_ROBIN;
        if(definitionModel.getPolicy().getLoadBalancingMethod().equalsIgnoreCase("performance"))
            lbAlgorithm = LbAlgorithm.SOURCE;
        else if(definitionModel.getPolicy().getLoadBalancingMethod().equalsIgnoreCase("failover"))
            lbAlgorithm = LbAlgorithm.LEAST_CONN;

        LbListener lbListener = LbListener.getInstance(lbAlgorithm, "", null, 0, 0);

        loadBalancer.withListeners(lbListener);

        return loadBalancer;
    }

    @Override
    public void addIPEndpoints(@Nonnull String toLoadBalancerId, @Nonnull String ... ipAddresses) throws CloudException, InternalException {
        throw new OperationNotSupportedException("Adding IP endpoints to an existing load balancer is not currently supported by Microsoft Azure cloud");
    }

    @Override
    public void addServers(@Nonnull String toLoadBalancerId, @Nonnull String ... serverIdsToAdd) throws CloudException, InternalException
    {
        DefinitionModel definitionModel = getDefinition(toLoadBalancerId);

        for (String serverToAddId : serverIdsToAdd)
        {
            if(serverToAddId == null)
                throw new InternalException("Cannot add server to load balancer. Server ID must not be null.");

            String[] parts = serverToAddId.split(":");
            DefinitionModel.EndPointModel endPointModel = new DefinitionModel.EndPointModel();
            endPointModel.setDomainName(parts[0] + ".cloudapp.net");
            endPointModel.setStatus("Enabled");
            endPointModel.setType("CloudService");
            if(definitionModel.getPolicy().getEndPoints() == null)
                definitionModel.getPolicy().setEndPoints(new ArrayList<DefinitionModel.EndPointModel>());

            definitionModel.getPolicy().getEndPoints().add(endPointModel);
        }


        try {
            AzureMethod method = new AzureMethod(this.getProvider());
            method.post(String.format(RESOURCE_DEFINITIONS, toLoadBalancerId), definitionModel);
        } catch (JAXBException e) {
            logger.error(e.getMessage());
            throw new InternalException(e);
        }
    }

    @Override
    public @Nonnull Iterable<LoadBalancerEndpoint> listEndpoints(@Nonnull String forLoadBalancerId) throws CloudException, InternalException
    {
        final Azure provider = getProvider();

        // this list VM call can be very slow, so we cache for a short time to help with clients calling
        // listEndpoints() on many LBs in a short period of time.
        Cache<VirtualMachine> cache = Cache.getInstance(provider, "LoadBalancerVMs", VirtualMachine.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Minute>(1, TimePeriod.MINUTE));
        Collection<VirtualMachine> virtualMachines = (Collection<VirtualMachine>)cache.get(provider.getContext());
        if (virtualMachines == null) {
            virtualMachines = new ArrayList<VirtualMachine>();
            for (VirtualMachine vm : provider.getComputeServices().getVirtualMachineSupport().listVirtualMachines()) {
                virtualMachines.add(vm);
            }
            cache.put(provider.getContext(), virtualMachines);
        }

        DefinitionModel definitionModel = getDefinition(forLoadBalancerId);

        ArrayList<LoadBalancerEndpoint> endpoints = new ArrayList<LoadBalancerEndpoint>();

        if(definitionModel != null && definitionModel.getPolicy() != null && definitionModel.getPolicy().getEndPoints() != null) {
            for (DefinitionModel.EndPointModel endPoint : definitionModel.getPolicy().getEndPoints()) {
                LbEndpointState lbState = endPoint.getStatus().equalsIgnoreCase("enabled") ? LbEndpointState.ACTIVE : LbEndpointState.INACTIVE;

                ArrayList<VirtualMachine> vmsWithEndpointDNS = findVMsForDNS(virtualMachines, endPoint.getDomainName());
                for (VirtualMachine vm : vmsWithEndpointDNS) {
                    LoadBalancerEndpoint lbEndpoint = LoadBalancerEndpoint.getInstance(LbEndpointType.VM, vm.getProviderVirtualMachineId(), lbState);
                    endpoints.add(lbEndpoint);
                }
            }
        }

        return endpoints;
    }

    private ArrayList<VirtualMachine> findVMsForDNS(Iterable<VirtualMachine> virtualMachines, String dnsName)
    {
        ArrayList<VirtualMachine> virtualMachinesFound = new ArrayList<VirtualMachine>();
        for (VirtualMachine virtualMachine : virtualMachines)
        {
            if(virtualMachine.getPublicDnsAddress() != null && virtualMachine.getPublicDnsAddress().equalsIgnoreCase(dnsName))
                virtualMachinesFound.add(virtualMachine);
        }
        return virtualMachinesFound;
    }

    @Override
    public void removeServers(@Nonnull String fromLoadBalancerId, @Nonnull String... serverIdsToRemove) throws CloudException, InternalException
    {
        DefinitionModel definitionModel = getDefinition(fromLoadBalancerId);

        Collection<DefinitionModel.EndPointModel> itemsToRemove = new ArrayList<DefinitionModel.EndPointModel>();
        for (String serverToRemoveId : serverIdsToRemove) {
            String[] parts = serverToRemoveId.split(":");
            for (DefinitionModel.EndPointModel endPoint : definitionModel.getPolicy().getEndPoints()) {
                if(endPoint.getDomainName().startsWith(parts[0] + ".")){
                    itemsToRemove.add(endPoint);
                }
            }
        }

        definitionModel.getPolicy().getEndPoints().removeAll(itemsToRemove);

        try {
            AzureMethod method = new AzureMethod(this.getProvider());
            method.post(String.format(RESOURCE_DEFINITIONS, fromLoadBalancerId), definitionModel);
        } catch (JAXBException e) {
            logger.error(e.getMessage());
            throw new InternalException(e);
        }
    }

    @Override
    public Iterable<LoadBalancerHealthCheck> listLBHealthChecks(@Nullable HealthCheckFilterOptions opts) throws CloudException, InternalException
    {
        ProviderContext ctx = this.getProvider().getContext();

        if( ctx == null ) {
            throw new AzureConfigException("No context was specified for this request");
        }

        ProfilesModel profilesModel = getProfiles();

        if(profilesModel == null)
            return Collections.emptyList();

        if(profilesModel.getProfiles() == null)
            return Collections.emptyList();

        ArrayList<LoadBalancerHealthCheck> loadBalancerHealthChecks = new ArrayList<LoadBalancerHealthCheck>();

        for (ProfileModel profile : profilesModel.getProfiles())
        {
            DefinitionModel definitionModel = getDefinition(profile.getName());

            DefinitionModel.MonitorModel currentMonitor = definitionModel.getMonitors().get(0);
            LoadBalancerHealthCheck.HCProtocol protocol =
                    currentMonitor.getProtocol().equalsIgnoreCase("http") ? LoadBalancerHealthCheck.HCProtocol.HTTP : LoadBalancerHealthCheck.HCProtocol.HTTPS;

            LoadBalancerHealthCheck loadBalancerHealthCheck =  LoadBalancerHealthCheck.getInstance(profile.getName(),
                                                                    protocol,
                                                                    Integer.parseInt(currentMonitor.getPort()),
                                                                    currentMonitor.getHttpOptions().getRelativePath(),
                                                                    Integer.parseInt(currentMonitor.getIntervalInSeconds()),
                                                                    Integer.parseInt(currentMonitor.getTimeoutInSeconds()),
                                                                    1,
                                                                    Integer.parseInt(currentMonitor.getToleratedNumberOfFailures()));
            loadBalancerHealthCheck.addProviderLoadBalancerId(profile.getName());

            if(opts == null || (opts.matches(loadBalancerHealthCheck)))
                loadBalancerHealthChecks.add(loadBalancerHealthCheck);
        }

        return loadBalancerHealthChecks;
    }

    @Override
    public LoadBalancerHealthCheck getLoadBalancerHealthCheck(@Nonnull String providerLBHealthCheckId, @Nullable String providerLoadBalancerId)throws CloudException, InternalException
    {
        if(providerLBHealthCheckId == null)
            throw new InternalException("Cannot retrieve Load Balancer Health Check when providerLBHealthCheckId is not provided");

        String profileId = providerLoadBalancerId != null ? providerLoadBalancerId : providerLBHealthCheckId;

        DefinitionModel definitionModel = getDefinition(profileId);

        DefinitionModel.MonitorModel currentMonitor = definitionModel.getMonitors().get(0);
        LoadBalancerHealthCheck.HCProtocol protocol =
                currentMonitor.getProtocol().equalsIgnoreCase("http") ? LoadBalancerHealthCheck.HCProtocol.HTTP : LoadBalancerHealthCheck.HCProtocol.HTTPS;

        LoadBalancerHealthCheck loadBalancerHealthCheck =  LoadBalancerHealthCheck.getInstance(profileId,
                protocol,
                Integer.parseInt(currentMonitor.getPort()),
                currentMonitor.getHttpOptions().getRelativePath(),
                Integer.parseInt(currentMonitor.getIntervalInSeconds()),
                Integer.parseInt(currentMonitor.getTimeoutInSeconds()),
                1,
                Integer.parseInt(currentMonitor.getToleratedNumberOfFailures()));
        loadBalancerHealthCheck.addProviderLoadBalancerId(profileId);

        return loadBalancerHealthCheck;
    }

    @Override
    public LoadBalancerHealthCheck modifyHealthCheck(@Nonnull String providerLBHealthCheckId, @Nonnull HealthCheckOptions options) throws InternalException, CloudException
    {
        if(options == null)
            throw new InternalException("Cannot modify Health Check definition when HealthCheckOptions are not provided");

        String loadBalancerId = providerLBHealthCheckId;
        if(options.getProviderLoadBalancerId() != null)
            loadBalancerId = options.getProviderLoadBalancerId();

        if(loadBalancerId == null)
            throw new InternalException("Load balancer id not provided");

        if(options.getProtocol() != LoadBalancerHealthCheck.HCProtocol.HTTP && options.getProtocol() != LoadBalancerHealthCheck.HCProtocol.HTTPS)
            throw new InternalException("Azure health check monitor supports only HTTP and HTTPS protocols");

        DefinitionModel.MonitorModel monitorModel = new DefinitionModel.MonitorModel();
        monitorModel.setProtocol(options.getProtocol().toString());
        monitorModel.setPort(String.valueOf(options.getPort()));
        monitorModel.setIntervalInSeconds("30");
        monitorModel.setTimeoutInSeconds("10");
        monitorModel.setToleratedNumberOfFailures("3");
        DefinitionModel.HttpOptionsModel httpOptions = new DefinitionModel.HttpOptionsModel();
        httpOptions.setVerb("GET");
        httpOptions.setRelativePath(options.getPath());
        httpOptions.setExpectedStatusCode("200");
        monitorModel.setHttpOptions(httpOptions);

        DefinitionModel definitionModel = getDefinition(loadBalancerId);

        definitionModel.getMonitors().set(0, monitorModel);

        try {
            AzureMethod method = new AzureMethod(this.getProvider());
            method.post(String.format(RESOURCE_DEFINITIONS, loadBalancerId), definitionModel);
        } catch (JAXBException e) {
            logger.error(e.getMessage());
            throw new InternalException(e);
        }

        return getLoadBalancerHealthCheck(loadBalancerId, null);
    }

    private DefinitionModel getDefinition(String profileName) throws CloudException, InternalException {
        AzureMethod method = new AzureMethod(this.getProvider());
        return method.get(DefinitionModel.class, String.format(RESOURCE_DEFINITION, profileName));
    }

    private ProfileModel getProfile(String profileName) throws CloudException, InternalException {
        AzureMethod method = new AzureMethod(this.getProvider());
        return method.get(ProfileModel.class, String.format(RESOURCE_PROFILE, profileName));
    }

    private ProfilesModel getProfiles() throws CloudException, InternalException {
        AzureMethod method = new AzureMethod(this.getProvider());
        return method.get(ProfilesModel.class, RESOURCE_PROFILES);
    }
}
