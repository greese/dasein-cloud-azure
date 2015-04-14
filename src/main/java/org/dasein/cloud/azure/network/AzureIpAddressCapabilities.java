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

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.network.IPAddressCapabilities;
import org.dasein.cloud.network.IPVersion;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Locale;

/**
 * Created by Vlad_Munthiu on 8/8/2014.
 */
public class AzureIpAddressCapabilities extends AbstractCapabilities<Azure> implements IPAddressCapabilities {

    public AzureIpAddressCapabilities(@Nonnull Azure provider) {
        super(provider);
    }

    /**
     * The cloud provider-specific term for an IP address. It's hard to fathom what other
     * than "IP address" anyone could use.
     *
     * @param locale the locale into which the term should be translated
     * @return the cloud provider-specific term for an IP address
     */
    @Nonnull
    @Override
    public String getProviderTermForIpAddress(@Nonnull Locale locale) {
        return "IP Address";
    }

    /**
     * Indicates whether you need to specify which VLAN you are tying a static IP address to when creating an
     * IP address for use in a VLAN. REQUIRED means you must specify the VLAN, OPTIONAL means you may, and NONE
     * means you do not specify a VLAN.
     *
     * @return the level of requirement for specifying a VLAN when creating a VLAN IP address
     * @throws org.dasein.cloud.CloudException    an error occurred processing the request in the cloud
     * @throws org.dasein.cloud.InternalException an internal error occurred inside the Dasein Cloud implementation
     */
    @Nonnull
    @Override
    public Requirement identifyVlanForVlanIPRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public @Nonnull Requirement identifyVlanForIPRequirement() throws CloudException, InternalException {return Requirement.NONE;}

    @Nonnull
    @Override
    public Requirement identifyVMForPortForwarding() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

   /* *//**
     * Indicates whether you need to specify which VLAN you are tying a static IP address to when creating any
     * IP address. REQUIRED means you must specify the VLAN, OPTIONAL means you may, and NONE
     * means you do not specify a VLAN.
     *
     * @return the level of requirement for specifying a VLAN when creating an IP address
     * @throws org.dasein.cloud.CloudException    an error occurred processing the request in the cloud
     * @throws org.dasein.cloud.InternalException an internal error occurred inside the Dasein Cloud implementation
     *//*
    @Nonnull
    @Override
    public Requirement identifyVlanForIPRequirement() throws CloudException, InternalException {
        return null;
    }*/

    /**
     * Indicates whether the underlying cloud supports the assignment of addresses of the specified version
     *
     * @param version the IP version being checked
     * @return true if the addresses of the specified version are assignable to cloud resources for public routing
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider determining support
     * @throws org.dasein.cloud.InternalException a local error occurred determining support
     */
    @Override
    public boolean isAssigned(@Nonnull IPVersion version) throws CloudException, InternalException {
        return false;
    }

    /**
     * Indicates whether the IPAddress can be assigned to a VM in the given state
     *
     * @param vmState the queried state of the VM
     * @return true if the IPAddress can be assigned when a VM is in the specified state
     * @throws org.dasein.cloud.CloudException
     * @throws org.dasein.cloud.InternalException
     */
    @Override
    public boolean canBeAssigned(@Nonnull VmState vmState) throws CloudException, InternalException {
        return false;
    }

    /**
     * When addresses are assignable, they may be assigned at launch, post-launch, or both.
     * {@link org.dasein.cloud.compute.VirtualMachineSupport#identifyStaticIPRequirement()} will tell you what must be done
     * at launch time. This method indicates whether or not assignable IPs may be assigned after launch. This
     * method should never return true when {@link #isAssigned(org.dasein.cloud.network.IPVersion)} returns false.
     *
     * @param version the IP version being checked
     * @return true if IP addresses of the specified version can be assigned post launch
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider determining support
     * @throws org.dasein.cloud.InternalException a local error occurred determining support
     */
    @Override
    public boolean isAssignablePostLaunch(@Nonnull IPVersion version) throws CloudException, InternalException {
        return false;
    }

    /**
     * Indicates whether the underlying cloud supports the forwarding of traffic on individual ports
     * targeted to addresses of the specified version on to resources in the cloud.
     *
     * @param version the IP version being checked
     * @return true if forwarding is supported
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider determining support
     * @throws org.dasein.cloud.InternalException a local error occurred determining support
     */
    @Override
    public boolean isForwarding(IPVersion version) throws CloudException, InternalException {
        return false;
    }

    /**
     * Indicates whether or not you can request static IP addresses of the specified Internet Protocol version.
     *
     * @param version the IP version you may want to request
     * @return true if you can make requests from the cloud provider to add addresses of this version to your pool
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider while determining if your account has support
     * @throws org.dasein.cloud.InternalException a local exception occurred while determining support
     */
    @Override
    public boolean isRequestable(@Nonnull IPVersion version) throws CloudException, InternalException {
        return false;
    }

    /**
     * Lists all IP protocol versions supported for static IP addresses in this cloud.
     *
     * @return a list of supported versions
     * @throws org.dasein.cloud.CloudException    an error occurred checking support for IP versions with the cloud provider
     * @throws org.dasein.cloud.InternalException a local error occurred preparing the supported version
     */
    @Nonnull
    @Override
    public Iterable<IPVersion> listSupportedIPVersions() throws CloudException, InternalException {
        return Collections.emptyList();
    }

    /**
     * Indicates whether or not IP addresses can be allocated for VLAN use. Only makes sense when the cloud
     * actually supports VLANS.
     *
     * @param ofVersion the version of public IP address that might be routed to a VLAN resource
     * @return true if an IP address may be allocated for use by VLANs
     * @throws org.dasein.cloud.InternalException a local error occurred determining support
     * @throws org.dasein.cloud.CloudException    an error occurred with the cloud provider in determining support
     */
    @Override
    public boolean supportsVLANAddresses(@Nonnull IPVersion ofVersion) throws InternalException, CloudException {
        return false;
    }
}
