package org.dasein.cloud.azure.network;

import org.dasein.cloud.*;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.network.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by Vlad_Munthiu on 6/10/2014.
 */
public class AzureLoadBalancerCapabilities extends AbstractCapabilities<Azure> implements LoadBalancerCapabilities {
    public AzureLoadBalancerCapabilities(@Nonnull Azure provider) {
        super(provider);
    }

    /**
     * Indicates the type of load balancer supported by this cloud.
     *
     * @return the load balancer type
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while performing this action
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation while performing this action
     */
    @Nonnull
    @Override
    public LoadBalancerAddressType getAddressType() throws CloudException, InternalException {
        return LoadBalancerAddressType.DNS;
    }

    /**
     * @return the maximum number of public ports on which the load balancer can listen
     * @throws org.dasein.cloud.CloudException    an error occurred while communicating with the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation
     */
    @Override
    public int getMaxPublicPorts() throws CloudException, InternalException {
        return 0;
    }

    /**
     * Gives the cloud provider's term for a load balancer (for example, "ELB" in AWS).
     *
     * @param locale the locale for which the term should be translated
     * @return the provider term for a load balancer
     */
    @Nonnull
    @Override
    public String getProviderTermForLoadBalancer(@Nonnull Locale locale) {
        return "Traffic Manager";
    }

    /**
     * Returns the visible scope of the load balancer or null if not applicable for the specific cloud
     *
     * @return The Visible Scope for the load balancer
     */
    @Nullable
    @Override
    public VisibleScope getLoadBalancerVisibleScope() {
        return VisibleScope.ACCOUNT_GLOBAL;
    }

    /**
     * Indicates whether a health check can be created independantly of a load balancer
     *
     * @return false if a health check can exist without having been assigned to a load balancer
     * @throws org.dasein.cloud.CloudException
     * @throws org.dasein.cloud.InternalException
     */
    @Override
    public boolean healthCheckRequiresLoadBalancer() throws CloudException, InternalException {
        return true;
    }

    /**
     * Indicates whether a name is required when creating a health check
     *
     * @return Requirement for health check name
     * @throws org.dasein.cloud.CloudException
     * @throws org.dasein.cloud.InternalException
     */
    @Override
    public Requirement healthCheckRequiresName() throws CloudException, InternalException {
        return null;
    }

    /**
     * @return the degree to which endpoints should or must be part of the load balancer creation process
     * @throws org.dasein.cloud.CloudException    an error occurred while communicating with the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation
     */
    @Nonnull
    @Override
    public Requirement identifyEndpointsOnCreateRequirement() throws CloudException, InternalException {
        return Requirement.OPTIONAL;
    }

    /**
     * Indicates the degree to which listeners should or must be specified when creating a load balancer.
     *
     * @return the degree to which listeners must be specified during load balancer creation
     * @throws org.dasein.cloud.CloudException    an error occurred while communicating with the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation
     */
    @Nonnull
    @Override
    public Requirement identifyListenersOnCreateRequirement() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    /**
     * @return whether or not you are expected to provide an address as part of the create process or one gets assigned
     * by the provider
     * @throws org.dasein.cloud.CloudException    an error occurred while communicating with the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation
     */
    @Override
    public boolean isAddressAssignedByProvider() throws CloudException, InternalException {
        return true;
    }

    /**
     * Indicates whether or not VM endpoints for this load balancer should be constrained to specific data centers in
     * its region. It should be false for load balancers handling non-VM endpoints or load balancers that are free
     * to balance across any data center. When a load balancer is data-center limited, the load balancer tries to
     * balance
     * traffic equally across the data centers. It is therefore up to you to try to keep the data centers configured
     * with equal capacity.
     *
     * @return whether or not VM endpoints are constrained to specific data centers associated with the load balancer
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while performing this action
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation while performing this action
     */
    @Override
    public boolean isDataCenterLimited() throws CloudException, InternalException {
        return false;
    }

    /**
     * Lists the load balancing algorithms from which you can choose when setting up a load balancer listener.
     *
     * @return a list of one or more supported load balancing algorithms
     * @throws org.dasein.cloud.CloudException    an error occurred while communicating with the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation
     */
    @Nonnull
    @Override
    public Iterable<LbAlgorithm> listSupportedAlgorithms() throws CloudException, InternalException {
        return Collections.unmodifiableList(Arrays.asList(LbAlgorithm.LEAST_CONN, LbAlgorithm.ROUND_ROBIN, LbAlgorithm.SOURCE));
    }

    /**
     * Describes what kind of endpoints may be added to a load balancer.
     *
     * @return a list of one or more supported endpoint types
     * @throws org.dasein.cloud.CloudException    an error occurred while communicating with the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation
     */
    @Nonnull
    @Override
    public Iterable<LbEndpointType> listSupportedEndpointTypes() throws CloudException, InternalException {
        return Collections.unmodifiableList(Arrays.asList(LbEndpointType.VM));
    }

    /**
     * Lists all IP protocol versions supported for load balancers in this cloud.
     *
     * @return a list of supported versions
     * @throws org.dasein.cloud.CloudException    an error occurred checking support for IP versions with the cloud provider
     * @throws org.dasein.cloud.InternalException a local error occurred preparing the supported version
     */
    @Nonnull
    @Override
    public Iterable<IPVersion> listSupportedIPVersions() throws CloudException, InternalException {
        return null;
    }

    /**
     * Lists the various options for session stickiness with load balancers in this cloud.
     *
     * @return a list of one or more load balancer persistence options for session stickiness
     * @throws org.dasein.cloud.CloudException    an error occurred checking support for IP versions with the cloud provider
     * @throws org.dasein.cloud.InternalException a local error occurred preparing the supported version
     */
    @Nonnull
    @Override
    public Iterable<LbPersistence> listSupportedPersistenceOptions() throws CloudException, InternalException {
        return null;
    }

    /**
     * Lists the network protocols supported for load balancer listeners.
     *
     * @return a list of one or more supported network protocols for load balancing
     * @throws org.dasein.cloud.CloudException    an error occurred while communicating with the cloud provider
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation
     */
    @Nonnull
    @Override
    public Iterable<LbProtocol> listSupportedProtocols() throws CloudException, InternalException {
        return Collections.unmodifiableList(Arrays.asList(LbProtocol.HTTP, LbProtocol.HTTPS));
    }

    /**
     * Indicates whether or not endpoints may be added to or removed from a load balancer once the load balancer has
     * been created.
     *
     * @return true if you can modify the endpoints post-create
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while performing this action
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation while performing this action
     */
    @Override
    public boolean supportsAddingEndpoints() throws CloudException, InternalException {
        return true;
    }

    /**
     * Indicates whether or not the underlying cloud monitors the balanced endpoints and provides health status
     * information.
     *
     * @return true if monitoring is supported
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while performing this action
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation while performing this action
     */
    @Override
    public boolean supportsMonitoring() throws CloudException, InternalException {
        return true;
    }

    /**
     * Indicates whether a single load balancer is limited to either IPv4 or IPv6 (false) or can support both IPv4 and
     * IPv6 traffic (true)
     *
     * @return true if a load balancer can be configured to support simultaneous IPv4 and IPv6 traffic
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while performing this action
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud implementation while performing this action
     */
    @Override
    public boolean supportsMultipleTrafficTypes() throws CloudException, InternalException {
        return false;
    }
}
