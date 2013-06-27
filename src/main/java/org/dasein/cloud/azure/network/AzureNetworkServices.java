package org.dasein.cloud.azure.network;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.network.*;

public class AzureNetworkServices implements NetworkServices {
	
	private Azure provider;

	public AzureNetworkServices(@Nonnull Azure provider) { this.provider = provider; }

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
		return null;
	}

	@Override
	public LoadBalancerSupport getLoadBalancerSupport() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
	public AzureVlanSupport getVlanSupport() {
		return new AzureVlanSupport(provider);
	}

	@Override
	public AzureVPNSupport getVpnSupport() {
		return new AzureVPNSupport(provider);
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
		// TODO Auto-generated method stub
		return false;
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
