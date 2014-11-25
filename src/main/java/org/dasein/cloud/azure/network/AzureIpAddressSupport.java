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
import org.bouncycastle.util.IPAddress;
import org.dasein.cloud.*;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.azure.AzureMethod;
import org.dasein.cloud.azure.network.model.PersistentVMRoleModel;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.network.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.Future;

/**
 * Created by Vlad_Munthiu on 7/31/2014.
 */
public class AzureIpAddressSupport implements IpAddressSupport{
    static private final Logger logger = Logger.getLogger(AzureIpAddressSupport.class);
    static private final Logger wire   = Azure.getWireLogger(AzureIpAddressSupport.class);

    private Azure provider;

    public static final String RESOURCE_ROLE = "/services/hostedservices/%s/deployments/%s/roles/%s";

    public AzureIpAddressSupport(Azure provider)
    {
        this.provider = provider;
    }
    /**
     * Assigns the specified address to the target server. This method should be called only if
     * {@link #isAssigned(AddressType)} for the specified address's address type is <code>true</code>.
     * If it is not, you will see the {@link RuntimeException} {@link org.dasein.cloud.OperationNotSupportedException}
     * thrown.
     *
     * @param addressId the unique identifier of the address to be assigned
     * @param serverId  the unique ID of the server to which the address is being assigned
     * @throws org.dasein.cloud.InternalException              an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException                 an error occurred processing the request in the cloud
     * @throws org.dasein.cloud.OperationNotSupportedException this cloud provider does not support address assignment of the specified address type
     */
    @Override
    public void assign(@Nonnull String addressId, @Nonnull String serverId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("AzureIpAddressSupport#assign not supported");
    }

    /**
     * Assigns the specified address to the specified network interface.
     *
     * @param addressId the unique ID of the IP address to assign
     * @param nicId     the unique ID of the network interface to which the address is being assigned
     * @throws org.dasein.cloud.InternalException an error occurred locally while performing the assignment
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud provider while performing the assignment
     */
    @Override
    public void assignToNetworkInterface(@Nonnull String addressId, @Nonnull String nicId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("AzureIpAddressSupport#assignToNetworkInterface not supported");
    }

    /**
     * Forwards the specified public IP address traffic on the specified public port over to the
     * specified private port on the specified server. If the server goes away, you will generally
     * still have traffic being forwarded to the private IP formally associated with the server, so
     * it is best to stop forwarding before terminating a server.
     * <p>
     * You should check {@link #isForwarding()} before calling this method. The implementation should
     * throw a {@link org.dasein.cloud.OperationNotSupportedException} {@link RuntimeException} if the underlying
     * cloud does not support IP address forwarding.
     * </p>
     *
     * @param addressId   the unique ID of the public IP address to be forwarded
     * @param publicPort  the public port of traffic to be forwarded
     * @param protocol    the network protocol being forwarded (not all clouds support ICMP)
     * @param privatePort the private port on the server to which traffic should be forwarded
     * @param onServerId  the unique ID of the server to which traffic is to be forwarded
     * @return the rule ID for the forwarding rule that is created
     * @throws org.dasein.cloud.InternalException              an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException                 an error occurred processing the request in the cloud
     * @throws org.dasein.cloud.OperationNotSupportedException this cloud provider does not support address forwarding
     */
    @Nonnull
    @Override
    public String forward(@Nonnull String addressId, int publicPort, @Nonnull Protocol protocol, int privatePort, @Nonnull String onServerId) throws InternalException, CloudException {
        VirtualMachine server = provider.getComputeServices().getVirtualMachineSupport().getVirtualMachine(onServerId);
        if(server == null)
            throw new InternalException("Cannot find Azure virtual machine with id: " + onServerId);

        String[] parts = server.getProviderVirtualMachineId().split(":");
        String sName, deploymentName, roleName;

        if (parts.length == 3)    {
            sName = parts[0];
            deploymentName = parts[1];
            roleName= parts[2];
        }
        else if( parts.length == 2 ) {
            sName = parts[0];
            deploymentName = parts[1];
            roleName = sName;
        }
        else {
            sName = server.getProviderVirtualMachineId();
            deploymentName = server.getProviderVirtualMachineId();
            roleName = server.getProviderVirtualMachineId();
        }

        AzureMethod azureMethod = new AzureMethod(this.provider);
        PersistentVMRoleModel persistentVMRoleModel = azureMethod.get(PersistentVMRoleModel.class, String.format(RESOURCE_ROLE, sName, deploymentName,roleName ));

        PersistentVMRoleModel.InputEndpoint inputEndpoint = new PersistentVMRoleModel.InputEndpoint();
        inputEndpoint.setLocalPort(String.valueOf(privatePort));
        inputEndpoint.setPort(String.valueOf(publicPort));
        inputEndpoint.setProtocol(protocol.toString());
        inputEndpoint.setName(protocol.toString() + String.valueOf(publicPort));

        if(persistentVMRoleModel.getConfigurationSets().get(0).getInputEndpoints() == null)
            persistentVMRoleModel.getConfigurationSets().get(0).setInputEndpoints(new ArrayList<PersistentVMRoleModel.InputEndpoint>());

        persistentVMRoleModel.getConfigurationSets().get(0).getInputEndpoints().add(inputEndpoint);

        try {
            azureMethod.put(String.format(RESOURCE_ROLE, sName, deploymentName,roleName), persistentVMRoleModel);
        } catch (JAXBException e) {
            logger.error(e.getMessage());
            throw new InternalException(e);
        }

        return inputEndpoint.getName();
    }

    /**
     * Provides access to meta-data about IP Address capabilities in the current region of this cloud.
     *
     * @return a description of the features supported by this region of this cloud
     * @throws org.dasein.cloud.InternalException an error occurred within the Dasein Cloud API implementation
     * @throws org.dasein.cloud.CloudException    an error occurred within the cloud provider
     */
    @Nonnull
    @Override
    public IPAddressCapabilities getCapabilities() throws CloudException, InternalException {
        return new AzureIpAddressCapabilities(this.provider);
    }

    /**
     * Provides the {@link org.dasein.cloud.network.IpAddress} identified by the specified unique address ID.
     *
     * @param addressId the unique ID of the IP address being requested
     * @return the matching {@link org.dasein.cloud.network.IpAddress}
     * @throws org.dasein.cloud.InternalException an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException    an error occurred processing the request in the cloud
     */
    @Nullable
    @Override
    public IpAddress getIpAddress(@Nonnull String addressId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("AzureIpAddressSupport#getIpAddress not supported");
    }

    /**
     * The cloud provider-specific term for an IP address. It's hard to fathom what other
     * than "IP address" anyone could use.
     *
     * @param locale the locale into which the term should be translated
     * @return the cloud provider-specific term for an IP address
     * @deprecated use {@link org.dasein.cloud.network.IPAddressCapabilities#getProviderTermForIpAddress(java.util.Locale)}
     */
    @Nonnull
    @Override
    public String getProviderTermForIpAddress(@Nonnull Locale locale) {
        return "";
    }

    /**
     * Indicates whether you need to specify which VLAN you are tying a static IP address to when creating an
     * IP address for use in a VLAN. REQUIRED means you must specify the VLAN, OPTIONAL means you may, and NONE
     * means you do not specify a VLAN.
     *
     * @return the level of requirement for specifying a VLAN when creating a VLAN IP address
     * @throws org.dasein.cloud.CloudException    an error occurred processing the request in the cloud
     * @throws org.dasein.cloud.InternalException an internal error occurred inside the Dasein Cloud implementation
     * @deprecated use {@link org.dasein.cloud.network.IPAddressCapabilities#identifyVlanForVlanIPRequirement()}
     */
    @Nonnull
    @Override
    public Requirement identifyVlanForVlanIPRequirement() throws CloudException, InternalException {
        throw new OperationNotSupportedException("AzureIpAddressSupport#identifyVlanForVlanIPRequirement not supported");
    }

    /**
     * Indicates whether the underlying cloud supports the assignment of addresses of the specified
     * type.
     *
     * @param type the type of address being checked (public or private)
     * @return <code>true</code> if addresses of the specified type are assignable to servers
     * @deprecated use {@link #isAssigned(org.dasein.cloud.network.IPVersion)}
     */
    @Override
    public boolean isAssigned(@Nonnull AddressType type) {
        return false;
    }

    /**
     * Indicates whether the underlying cloud supports the assignment of addresses of the specified version
     *
     * @param version the IP version being checked
     * @return true if the addresses of the specified version are assignable to cloud resources for public routing
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider determining support
     * @throws org.dasein.cloud.InternalException a local error occurred determining support
     * @deprecated use {@link org.dasein.cloud.network.IPAddressCapabilities#isAssigned(org.dasein.cloud.network.IPVersion)}
     */
    @Override
    public boolean isAssigned(@Nonnull IPVersion version) throws CloudException, InternalException {
        return false;
    }

    /**
     * When addresses are assignable, they may be assigned at launch, post-launch, or both.
     * {@link VirtualMachineSupport#identifyStaticIPRequirement()} will tell you what must be done
     * at launch time. This method indicates whether or not assignable IPs may be assigned after launch. This
     * method should never return true when {@link #isAssigned(org.dasein.cloud.network.IPVersion)} returns false.
     *
     * @param version the IP version being checked
     * @return true if IP addresses of the specified version can be assigned post launch
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider determining support
     * @throws org.dasein.cloud.InternalException a local error occurred determining support
     * @deprecated use {@link org.dasein.cloud.network.IPAddressCapabilities#isAssignablePostLaunch(org.dasein.cloud.network.IPVersion)}
     */
    @Override
    public boolean isAssignablePostLaunch(@Nonnull IPVersion version) throws CloudException, InternalException {
        return false;
    }

    /**
     * Indicates whether the underlying cloud supports the forwarding individual port traffic on
     * public IP addresses to hosts private IPs. These addresses may also be used for load
     * balancers in some clouds as well.
     *
     * @return <code>true</code> if public IPs may be forwarded on to private IPs
     * @deprecated use {@link #isForwarding(org.dasein.cloud.network.IPVersion)}
     */
    @Override
    public boolean isForwarding() {
        return true;
    }

    /**
     * Indicates whether the underlying cloud supports the forwarding of traffic on individual ports
     * targeted to addresses of the specified version on to resources in the cloud.
     *
     * @param version the IP version being checked
     * @return true if forwarding is supported
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider determining support
     * @throws org.dasein.cloud.InternalException a local error occurred determining support
     * @deprecated use {@link org.dasein.cloud.network.IPAddressCapabilities#isForwarding(org.dasein.cloud.network.IPVersion)}
     */
    @Override
    public boolean isForwarding(IPVersion version) throws CloudException, InternalException {
        return true;
    }

    /**
     * Indicates whether the underlying cloud allows you to make programmatic requests for
     * new IP addresses of the specified type
     *
     * @param type the type of address being checked (public or private)
     * @return <code>true</code> if there are programmatic mechanisms for allocating new IPs of the specified type
     * @deprecated use {@link #isRequestable(org.dasein.cloud.network.IPVersion)}
     */
    @Override
    public boolean isRequestable(@Nonnull AddressType type) {
        return false;
    }

    /**
     * Indicates whether or not you can request static IP addresses of the specified Internet Protocol version.
     *
     * @param version the IP version you may want to request
     * @return true if you can make requests from the cloud provider to add addresses of this version to your pool
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while determining if your account has support
     * @throws org.dasein.cloud.InternalException a local exception occurred while determining support
     * @deprecated use {@link org.dasein.cloud.network.IPAddressCapabilities#isRequestable(org.dasein.cloud.network.IPVersion)}
     */
    @Override
    public boolean isRequestable(@Nonnull IPVersion version) throws CloudException, InternalException {
        return false;
    }

    /**
     * Indicates whether this account is subscribed to leverage IP address services in the
     * target cloud.
     *
     * @return <code>true</code> if the account holder is subscribed
     * @throws org.dasein.cloud.InternalException an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException    an error occurred processing the request in the cloud
     */
    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return false;
    }

    /**
     * Lists all (or unassigned) private IP addresses from the account holder's private IP address
     * pool. This method is safe to call even if private IP forwarding is not supported. It will
     * simply return {@link java.util.Collections#emptyList()}.
     *
     * @param unassignedOnly indicates that only unassigned addresses are being sought
     * @return all private IP addresses or the unassigned ones from the pool
     * @throws org.dasein.cloud.InternalException an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException    an error occurred processing the request in the cloud
     * @throws OperationNotSupportedException     the requested version is not supported
     * @deprecated private IP pools no longer make sense, use the {@link org.dasein.cloud.network.VLANSupport} class
     */
    @Nonnull
    @Override
    public Iterable<IpAddress> listPrivateIpPool(boolean unassignedOnly) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    /**
     * Lists all (or unassigned) public IP addresses from the account holder's public IP address
     * pool. This method is safe to call even if public IP forwarding is not supported. It will
     * simply return {@link java.util.Collections#emptyList()}.
     *
     * @param unassignedOnly indicates that only unassigned addresses are being sought
     * @return all public IP addresses or the unassigned ones from the pool
     * @throws org.dasein.cloud.InternalException an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException    an error occurred processing the request in the cloud
     * @throws OperationNotSupportedException     the requested version is not supported
     * @deprecated use {@link #listIpPool(org.dasein.cloud.network.IPVersion, boolean)}
     */
    @Nonnull
    @Override
    public Iterable<IpAddress> listPublicIpPool(boolean unassignedOnly) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    /**
     * Lists all IP addresses of the specified IP version that are allocated to the account holder's IP address pool. If
     * the specified version is not supported, an empty list should be returned.
     *
     * @param version        the version of the IP protocol for which you are looking for IP addresses
     * @param unassignedOnly show only IP addresses that have yet to be assigned to cloud resources
     * @return all matching IP addresses from the IP address pool
     * @throws org.dasein.cloud.InternalException a local error occurred loading the IP addresses
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while requesting the IP addresses
     */
    @Nonnull
    @Override
    public Iterable<IpAddress> listIpPool(@Nonnull IPVersion version, boolean unassignedOnly) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    /**
     * Lists all IP addresses of the specified IP version that are allocated to the account holder's IP address pool. If
     * the specified version is not supported, an empty list should be returned.  This method implements a callable so
     * it can be called concurrently.
     *
     * @param version        the version of the IP protocol for which you are looking for IP addresses
     * @param unassignedOnly show only IP addresses that have yet to be assigned to cloud resources
     * @return all matching IP addresses from the IP address pool
     * @throws org.dasein.cloud.InternalException a local error occurred loading the IP addresses
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while requesting the IP addresses
     */
    @Nonnull
    @Override
    public Future<Iterable<IpAddress>> listIpPoolConcurrently(@Nonnull IPVersion version, boolean unassignedOnly) throws InternalException, CloudException {
        return null;
    }

    /**
     * Lists the status of all IP addresses of the specified IP version that are allocated to the account holder's IP
     * address pool. If the specified version is not supported, an empty list should be returned.
     *
     * @param version the version of the IP protocol for which you are looking for IP addresses
     * @return the status of all matching IP addresses from the IP address pool
     * @throws org.dasein.cloud.InternalException a local error occurred loading the IP addresses
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while requesting the IP addresses
     */
    @Nonnull
    @Override
    public Iterable<ResourceStatus> listIpPoolStatus(@Nonnull IPVersion version) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    /**
     * Lists the IP forwarding rules associated with the specified public IP address. This method
     * is safe to call even when requested on a private IP address or when IP forwarding is not supported.
     * In those situations, {@link java.util.Collections#emptyList()} will be returned.
     *
     * @param addressId the unique ID of the public address whose forwarding rules will be sought
     * @return all IP forwarding rules for the specified IP address
     * @throws org.dasein.cloud.InternalException an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException    an error occurred processing the request in the cloud
     */
    @Nonnull
    @Override
    public Iterable<IpForwardingRule> listRules(@Nonnull String addressId) throws InternalException, CloudException {
        return Collections.emptyList();
    }

    /**
     * Lists all IP protocol versions supported for static IP addresses in this cloud.
     *
     * @return a list of supported versions
     * @throws org.dasein.cloud.CloudException    an error occurred checking support for IP versions with the cloud provider
     * @throws org.dasein.cloud.InternalException a local error occurred preparing the supported version
     * @deprecated use {@link org.dasein.cloud.network.IPAddressCapabilities#listSupportedIPVersions()}
     */
    @Nonnull
    @Override
    public Iterable<IPVersion> listSupportedIPVersions() throws CloudException, InternalException {
        return Collections.emptyList();
    }

    /**
     * When a cloud allows for programmatic requesting of new IP addresses, you may also programmaticall
     * release them ({@link #isRequestable(org.dasein.cloud.network.AddressType)}). This method will release the specified IP
     * address from your pool and you will no longer be able to use it for assignment or forwarding.
     *
     * @param addressId the unique ID of the address to be release
     * @throws org.dasein.cloud.InternalException              an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException                 an error occurred processing the request in the cloud
     * @throws org.dasein.cloud.OperationNotSupportedException this cloud provider does not support address requests
     */
    @Override
    public void releaseFromPool(@Nonnull String addressId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("AzureIpAddressSupport#releaseFromPool not supported");
    }

    /**
     * Releases an IP address assigned to a server so that it is unassigned in the address pool.
     * You should call this method only when {@link #isAssigned(org.dasein.cloud.network.AddressType)} is <code>true</code>
     * for addresses of the target address's type.
     *
     * @param addressId the address ID to release from its server assignment
     * @throws org.dasein.cloud.InternalException              an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException                 an error occurred processing the request in the cloud
     * @throws org.dasein.cloud.OperationNotSupportedException this cloud provider does not support address assignment for addresses of the specified type
     */
    @Override
    public void releaseFromServer(@Nonnull String addressId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("AzureIpAddressSupport#releaseFromServer not supported");
    }

    /**
     * When requests for new IP addresses may be handled programmatically, this method allocates
     * a new IP address of the specified type. You should call it only if
     * {@link #isRequestable(org.dasein.cloud.network.AddressType)} is <code>true</code> for the address's type.
     *
     * @param typeOfAddress the type of address being requested
     * @return the newly allocated IP address
     * @throws org.dasein.cloud.InternalException              an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException                 an error occurred processing the request in the cloud
     * @throws org.dasein.cloud.OperationNotSupportedException this cloud provider does not support address requests
     * @deprecated use {@link #request(org.dasein.cloud.network.IPVersion)}
     */
    @Nonnull
    @Override
    public String request(@Nonnull AddressType typeOfAddress) throws InternalException, CloudException {
        return "";
    }

    /**
     * Requests an IP address of the specified version for the flat (non-VLAN) network space.
     *
     * @param version the IP version of the address to be requested
     * @return the unique ID of the newly provisioned static IP address
     * @throws org.dasein.cloud.InternalException a local error occurred while preparing the request
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud while provisioning the new address
     */
    @Nonnull
    @Override
    public String request(@Nonnull IPVersion version) throws InternalException, CloudException {
        return "";
    }

    /**
     * Requests a public IP address that may be used with a VLAN. This version may be used only when
     * {@link #identifyVlanForVlanIPRequirement()} is not {@link org.dasein.cloud.Requirement#REQUIRED}.
     *
     * @param version the IP version of the address to be requested
     * @return the unique ID of a newly provisioned public IP address
     * @throws org.dasein.cloud.InternalException an error occurred locally while attempting to provision the IP address
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud provider while provisioning the IP address
     * @throws OperationNotSupportedException     either VLAN IPs are not supported or they must be explicitly associated with a VLAN
     */
    @Nonnull
    @Override
    public String requestForVLAN(@Nonnull IPVersion version) throws InternalException, CloudException {
        return "";
    }

    /**
     * Requests a public IP address that must be used with a specific VLAN. This version may be used only when
     * {@link #identifyVlanForVlanIPRequirement()} is not {@link org.dasein.cloud.Requirement#NONE}.
     *
     * @param version the IP version of the address to be requested
     * @param vlanId  the unique ID of the VLAN to which the IP address will be assigned
     * @return the unique ID of a newly provisioned public IP address
     * @throws org.dasein.cloud.InternalException an error occurred locally while attempting to provision the IP address
     * @throws org.dasein.cloud.CloudException    an error occurred in the cloud provider while provisioning the IP address
     * @throws OperationNotSupportedException     either VLAN IPs are not supported or they cannot be explicitly associated with a VLAN
     */
    @Nonnull
    @Override
    public String requestForVLAN(@Nonnull IPVersion version, @Nonnull String vlanId) throws InternalException, CloudException {
        return "";
    }

    /**
     * Removes the specified forwarding rule from the address with which it is associated.
     *
     * @param ruleId the rule to be removed
     * @throws org.dasein.cloud.InternalException              an internal error occurred inside the Dasein Cloud implementation
     * @throws org.dasein.cloud.CloudException                 an error occurred processing the request in the cloud
     * @throws org.dasein.cloud.OperationNotSupportedException this cloud provider does not support address forwarding
     */
    @Override
    public void stopForward(@Nonnull String ruleId) throws InternalException, CloudException {
        throw new OperationNotSupportedException("AzureIpAddressSupport#stopForward not supported");
    }

    /**
     * Indicates whether or not IP addresses can be allocated for VLAN use. Only makes sense when the cloud
     * actually supports VLANS.
     *
     * @param ofVersion the version of public IP address that might be routed to a VLAN resource
     * @return true if an IP address may be allocated for use by VLANs
     * @throws org.dasein.cloud.InternalException a local error occurred determining support
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider in determining support
     * @deprecated use {@link org.dasein.cloud.network.IPAddressCapabilities#supportsVLANAddresses(org.dasein.cloud.network.IPVersion)}
     */
    @Override
    public boolean supportsVLANAddresses(@Nonnull IPVersion ofVersion) throws InternalException, CloudException {
        return false;
    }

    /**
     * Maps the specified Dasein Cloud service action to an identifier specific to an underlying cloud. If there is
     * no mapping that makes any sense, the method will return an empty array.
     *
     * @param action the Dasein Cloud service action
     * @return a list of cloud-specific IDs (e.g. iam:ListGroups) representing an action with this cloud provider
     */
    @Nonnull
    @Override
    public String[] mapServiceAction(@Nonnull ServiceAction action) {
        return new String[0];
    }
}
