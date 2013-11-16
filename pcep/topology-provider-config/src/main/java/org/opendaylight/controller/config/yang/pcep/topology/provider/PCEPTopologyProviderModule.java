/**
 * Generated file

 * Generated from: yang module name: config-pcep-topology-provider  yang module local name: pcep-topology-impl
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Mon Nov 18 21:08:29 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.pcep.topology.provider;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.opendaylight.bgpcep.pcep.topology.provider.PCEPTopologyProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

/**
 *
 */
public final class PCEPTopologyProviderModule extends org.opendaylight.controller.config.yang.pcep.topology.provider.AbstractPCEPTopologyProviderModule
{
	private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProviderModule.class);

	public PCEPTopologyProviderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public PCEPTopologyProviderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final PCEPTopologyProviderModule oldModule, final java.lang.AutoCloseable oldInstance) {
		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void validate(){
		super.validate();
		// Add custom validation for module attributes here.
	}

	private InetAddress listenAddress() {
		final IpAddress a = getListenAddress();
		if (a.getIpv4Address() != null) {
			return InetAddresses.forString(a.getIpv4Address().getValue());
		} else if (a.getIpv6Address() != null) {
			return InetAddresses.forString(a.getIpv6Address().getValue());
		} else {
			throw new IllegalArgumentException("Address " + a + " not supported ");
		}
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		final InstanceIdentifier<Topology> topology =
				InstanceIdentifier.builder().node(NetworkTopology.class).child(Topology.class, new TopologyKey(getTopologyId())).toInstance();
		final InetSocketAddress address = new InetSocketAddress(listenAddress(), getListenPort().getValue());
		try {
			return PCEPTopologyProvider.create(getDispatcherDependency(), address,
					getSchedulerDependency(), getDataProviderDependency(), topology);
		} catch (InterruptedException | ExecutionException e) {
			LOG.error("Failed to instantiate topology provider at {}", address, e);
			throw new RuntimeException("Failed to instantiate provider", e);
		}
	}
}
