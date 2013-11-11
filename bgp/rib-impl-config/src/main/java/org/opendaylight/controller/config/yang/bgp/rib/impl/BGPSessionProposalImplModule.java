/**
 * Generated file

 * Generated from: yang module name: bgp-rib-impl  yang module local name: bgp-proposal-impl
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Wed Nov 06 13:02:32 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import org.opendaylight.protocol.bgp.rib.impl.BGPSessionProposalImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposal;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

/**
*
*/
public final class BGPSessionProposalImplModule
		extends
		org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPSessionProposalImplModule {

	public BGPSessionProposalImplModule(
			org.opendaylight.controller.config.api.ModuleIdentifier name,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(name, dependencyResolver);
	}

	public BGPSessionProposalImplModule(
			org.opendaylight.controller.config.api.ModuleIdentifier name,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			BGPSessionProposalImplModule oldModule,
			java.lang.AutoCloseable oldInstance) {
		super(name, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void validate() {
		super.validate();
		// Add custom validation for module attributes here.
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		final Ipv4Address bgpId = new Ipv4Address(getBgpId());
		final BGPSessionProposalImpl bgpSessionProposal = new BGPSessionProposalImpl(
				getHoldtimer(), getAsNumber(), bgpId);
		return new BgpSessionProposalCloseable(bgpSessionProposal);
	}

	private static final class BgpSessionProposalCloseable implements
			BGPSessionProposal, AutoCloseable {

		private final BGPSessionProposalImpl inner;

		public BgpSessionProposalCloseable(
				BGPSessionProposalImpl bgpSessionProposal) {
			this.inner = bgpSessionProposal;
		}

		@Override
		public void close() throws Exception {
			// NOOP
		}

		@Override
		public BGPSessionPreferences getProposal() {
			return inner.getProposal();
		}
	}
}
