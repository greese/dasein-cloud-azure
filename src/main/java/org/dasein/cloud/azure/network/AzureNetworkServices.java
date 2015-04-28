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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.network.AbstractNetworkServices;
import org.dasein.cloud.network.DNSSupport;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.LoadBalancerSupport;
import org.dasein.cloud.network.NetworkFirewallSupport;

public class AzureNetworkServices extends AbstractNetworkServices<Azure> {

    public AzureNetworkServices(Azure provider) {
        super(provider);
    }

    @Override
	public DNSSupport getDnsSupport() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FirewallSupport getFirewallSupport() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IpAddressSupport getIpAddressSupport() {
		// TODO Auto-generated method stub
		return new AzureIpAddressSupport(getProvider());
	}

	@Override
	public LoadBalancerSupport getLoadBalancerSupport() {
		// TODO Auto-generated method stub
		return new AzureLoadBalancerSupport(getProvider());
	}

    @Nullable
    @Override
    public NetworkFirewallSupport getNetworkFirewallSupport() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public AzureVlanSupport getVlanSupport() {
		return new AzureVlanSupport(getProvider());
	}

	@Override
	public AzureVPNSupport getVpnSupport() {
		return new AzureVPNSupport(getProvider());
	}

	@Override
	public boolean hasDnsSupport() {
		return false;
	}

	@Override
	public boolean hasFirewallSupport() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasIpAddressSupport() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasLoadBalancerSupport() {
		return true;
	}

    @Override
    public boolean hasNetworkFirewallSupport() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public boolean hasVlanSupport() {
		return true;
	}

	@Override
	public boolean hasVpnSupport() {
		// TODO Auto-generated method stub
		return false;
	}

}
