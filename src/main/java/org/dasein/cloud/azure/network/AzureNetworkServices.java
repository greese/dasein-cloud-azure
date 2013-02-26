package org.dasein.cloud.azure.network;

import javax.annotation.Nonnull;

import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.network.AbstractNetworkServices;
import org.dasein.cloud.network.DNSSupport;
import org.dasein.cloud.network.FirewallSupport;
import org.dasein.cloud.network.IpAddressSupport;
import org.dasein.cloud.network.LoadBalancerSupport;

public class AzureNetworkServices extends AbstractNetworkServices {
	
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasVpnSupport() {
		// TODO Auto-generated method stub
		return false;
	}

}
