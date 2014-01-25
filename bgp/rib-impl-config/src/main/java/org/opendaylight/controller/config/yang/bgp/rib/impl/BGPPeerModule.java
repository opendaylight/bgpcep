/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Generated file

 * Generated from: yang module name: bgp-rib-impl  yang module local name: bgp-peer
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Sat Jan 25 11:00:14 CET 2014
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import java.net.InetSocketAddress;
import java.util.List;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.bgp.rib.impl.BGPPeer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.as4.bytes._case.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.MultiprotocolCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.multiprotocol._case.MultiprotocolCapabilityBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

/**
 *
 */
public final class BGPPeerModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPPeerModule
{
	private static final Logger LOG = LoggerFactory.getLogger(BGPPeerModule.class);

	public BGPPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public BGPPeerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			final BGPPeerModule oldModule, final java.lang.AutoCloseable oldInstance) {

		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	protected void customValidation(){
		JmxAttributeValidationException.checkNotNull(getHost(),
				"value is not set.", hostJmxAttribute);
		JmxAttributeValidationException.checkNotNull(getPort(),
				"value is not set.", portJmxAttribute);

		if (getHost().getIpv4Address() == null) {
			JmxAttributeValidationException.checkNotNull(getBgpIdentifier(),
					"value is not set.", bgpIdentifierJmxAttribute);
		}
	}

	private InetSocketAddress createAddress() {
		final IpAddress ip = getHost();
		if (ip.getIpv4Address() != null) {
			return new InetSocketAddress(InetAddresses.forString(ip.getIpv4Address().getValue()), getPort().getValue());
		} else if (ip.getIpv6Address() != null) {
			return new InetSocketAddress(InetAddresses.forString(ip.getIpv6Address().getValue()), getPort().getValue());
		} else {
			throw new IllegalStateException("Failed to handle host " + getHost());
		}
	}

	private static String peerName(final IpAddress host) {
		if (host.getIpv4Address() != null) {
			return host.getIpv4Address().getValue();
		}
		if (host.getIpv6Address() != null) {
			return host.getIpv6Address().getValue();
		}

		return null;
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		final RIB r = getRibDependency();

		Ipv4Address bgpId = getBgpIdentifier();
		if (bgpId == null) {
			// Validation phase makes this safe
			bgpId = getHost().getIpv4Address();
		}

		final List<BgpParameters> tlvs = Lists.newArrayList();
		tlvs.add(new BgpParametersBuilder().setCParameters(
				new As4BytesCaseBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(r.getLocalAs()).build()).build()).build());

		for (final BgpTableType t : getAdvertizedTableDependency()) {
			if (!r.getLocalTables().contains(t)) {
				LOG.info("RIB instance does not list {} in its local tables. Incoming data will be dropped.", t);
			}

			tlvs.add(new BgpParametersBuilder().setCParameters(
					new MultiprotocolCaseBuilder().setMultiprotocolCapability(
							new MultiprotocolCapabilityBuilder(t).build()).build()).build());
		}

		return new BGPPeer(peerName(getHost()), createAddress(),
				new BGPSessionPreferences(r.getLocalAs(), getHoldtimer(), bgpId, tlvs), r);
	}
}
