/**
 * Generated file

 * Generated from: yang module name: config-bgp-topology-provider  yang module local name: bgp-linkstate-topology
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Tue Nov 19 15:22:41 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.bgp.rib.spi;

import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.bgpcep.topology.provider.bgp.AbstractTopologyBuilder;
import org.opendaylight.bgpcep.topology.provider.bgp.LinkstateTopologyBuilder;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 *
 */
public final class LinkstateTopologyBuilderModule extends org.opendaylight.controller.config.yang.bgp.rib.spi.AbstractLinkstateTopologyBuilderModule
{

	public LinkstateTopologyBuilderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public LinkstateTopologyBuilderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final LinkstateTopologyBuilderModule oldModule, final java.lang.AutoCloseable oldInstance) {
		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void validate(){
		super.validate();
		// Add custom validation for module attributes here.
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		final LinkstateTopologyBuilder b = new LinkstateTopologyBuilder(getDataProviderDependency(), getTopologyId());
		final InstanceIdentifier<Tables> i = AbstractTopologyBuilder.tableInstanceIdentifier(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);
		final ListenerRegistration<DataChangeListener> r = getDataProviderDependency().registerDataChangeListener(i, b);

		final class TopologyReferenceAutocloseable extends DefaultTopologyReference implements AutoCloseable {
			public TopologyReferenceAutocloseable(final InstanceIdentifier<Topology> instanceIdentifier) {
				super(instanceIdentifier);
			}

			@Override
			public void close() throws Exception {
				try {
					r.close();
				} finally {
					b.close();
				}
			}
		}

		return new TopologyReferenceAutocloseable(b.getInstanceIdentifier());
	}
}
