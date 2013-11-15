/**
* Generated file

* Generated from: yang module name: bgp-rib-impl  yang module local name: rib-impl
* Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
* Generated at: Wed Nov 06 13:02:32 CET 2013
*
* Do not modify this file unless it is present under src/main directory
*/
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.RIB;
import org.opendaylight.protocol.bgp.rib.RIBEvent;
import org.opendaylight.protocol.bgp.rib.RIBEventListener;
import org.opendaylight.protocol.bgp.rib.impl.BGP;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.protocol.concepts.InitialListenerEvents;
import org.opendaylight.protocol.concepts.ListenerRegistration;

/**
*
*/
public final class RIBImplModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractRIBImplModule
{

    public RIBImplModule(org.opendaylight.controller.config.api.ModuleIdentifier name, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(name, dependencyResolver);
    }

    public RIBImplModule(org.opendaylight.controller.config.api.ModuleIdentifier name, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, RIBImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(name, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate(){
        super.validate();
		JmxAttributeValidationException.checkNotNull(getRibName(),
				"value is not set.", ribNameJmxAttribute);
		JmxAttributeValidationException.checkCondition(!getRibName().isEmpty(),
				"should not be empty string.", ribNameJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
		RIBImpl rib = new RIBImpl(getRibName());
		BGP bgp = getBgpDependency();
		final BGPPeer peer = new BGPPeer(rib, "peer-" + bgp.toString());

		ListenerRegistration<BGPSessionListener> reg = bgp.registerUpdateListener(peer, null, getReconnectStrategyDependency());
		return new RibImpl(reg, rib);
	}

	private static final class RibImpl implements RIB, AutoCloseable {
		private final ListenerRegistration<BGPSessionListener> reg;
		private final RIB innerRib;

		private RibImpl(ListenerRegistration<BGPSessionListener> reg, RIB innerRib) {
			this.reg = reg;
			this.innerRib = innerRib;
		}

		@Override
		public void close() throws Exception {
			reg.close();
		}

		@Override
		public InitialListenerEvents<RIBEventListener, RIBEvent> registerListener(RIBEventListener ribEventListener) {
		    return innerRib.registerListener(ribEventListener);
		}
	}
}
