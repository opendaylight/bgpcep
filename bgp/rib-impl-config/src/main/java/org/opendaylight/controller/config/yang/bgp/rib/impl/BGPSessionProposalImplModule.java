/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Generated file

 * Generated from: yang module name: bgp-rib-impl  yang module local name: bgp-proposal-impl
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Wed Nov 06 13:02:32 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.bgp.rib.impl.BGPSessionProposalImpl;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionProposal;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev131125.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

/**
*
*/
public final class BGPSessionProposalImplModule extends
		org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPSessionProposalImplModule {

	public BGPSessionProposalImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier name,
			final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(name, dependencyResolver);
	}

	public BGPSessionProposalImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier name,
			final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			final BGPSessionProposalImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
		super(name, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void validate() {
		super.validate();
		JmxAttributeValidationException.checkNotNull(getBgpId(), "value is not set.", this.bgpIdJmxAttribute);
		JmxAttributeValidationException.checkCondition(isValidIPv4Address(getBgpId()),
				"value " + getBgpId() + " is not valid IPv4 address", this.bgpIdJmxAttribute);

		JmxAttributeValidationException.checkNotNull(getAsNumber(), "value is not set.", this.asNumberJmxAttribute);
		JmxAttributeValidationException.checkCondition(getAsNumber().intValue() > 0, "value must be greater than 0",
				this.asNumberJmxAttribute);

		JmxAttributeValidationException.checkNotNull(getHoldtimer(), "value is not set.", this.holdtimerJmxAttribute);
		JmxAttributeValidationException.checkCondition((getHoldtimer() == 0) || (getHoldtimer() >= 3), "value must be 0 or 3 and more",
				this.holdtimerJmxAttribute);
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		final Ipv4Address bgpId = new Ipv4Address(getBgpId());
		final Map<Class<? extends AddressFamily>, Class<? extends SubsequentAddressFamily>> tables = new HashMap<>();
		tables.put(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);
		tables.put(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);
		tables.put(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class);
		final BGPSessionProposalImpl bgpSessionProposal = new BGPSessionProposalImpl(getHoldtimer(), new AsNumber(getAsNumber()), bgpId, tables);
		return new BgpSessionProposalCloseable(bgpSessionProposal);
	}

	private static final class BgpSessionProposalCloseable implements BGPSessionProposal, AutoCloseable {

		private final BGPSessionProposalImpl inner;

		public BgpSessionProposalCloseable(final BGPSessionProposalImpl bgpSessionProposal) {
			this.inner = bgpSessionProposal;
		}

		@Override
		public void close() throws Exception {
			// NOOP
		}

		@Override
		public BGPSessionPreferences getProposal() {
			return this.inner.getProposal();
		}
	}

	private boolean isValidIPv4Address(final String address) {
		final Pattern pattern = Pattern.compile(Ipv4Address.PATTERN_CONSTANTS.get(0));
		final Matcher matcher = pattern.matcher(address);
		return matcher.matches();
	}
}
