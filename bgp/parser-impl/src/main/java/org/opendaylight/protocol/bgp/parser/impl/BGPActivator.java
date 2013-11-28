/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.bgp.parser.impl.message.BGPKeepAliveMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPNotificationMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPOpenMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.open.As4CapabilityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.open.CapabilityParameterParser;
import org.opendaylight.protocol.bgp.parser.impl.message.open.GracefulCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.open.MultiProtocolCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AS4AggregatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AS4PathAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AggregatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AtomicAggregateAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ClusterIdAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunitiesAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ExtendedCommunitiesAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.Ipv4NlriParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.Ipv6NlriParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LocalPreferenceAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MPReachAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MPUnreachAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MultiExitDiscriminatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.NextHopAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginatorIdAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.CAs4Bytes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.CGracefulRestart;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class BGPActivator implements BGPExtensionProviderActivator {
	private static final Logger logger = LoggerFactory.getLogger(BGPActivator.class);
	private List<AutoCloseable> registrations;

	@Override
	public synchronized void start(final BGPExtensionProviderContext context) {
		Preconditions.checkState(this.registrations == null);
		final List<AutoCloseable> regs = new ArrayList<>();

		final AddressFamilyRegistry afiReg = context.getAddressFamilyRegistry();
		regs.add(context.registerAddressFamily(Ipv4AddressFamily.class, 1));
		regs.add(context.registerAddressFamily(Ipv6AddressFamily.class, 2));

		final SubsequentAddressFamilyRegistry safiReg = context.getSubsequentAddressFamilyRegistry();
		regs.add(context.registerSubsequentAddressFamily(UnicastSubsequentAddressFamily.class, 1));
		regs.add(context.registerSubsequentAddressFamily(MplsLabeledVpnSubsequentAddressFamily.class, 128));

		final NlriRegistry nlriReg = context.getNlriRegistry();
		regs.add(context.registerNlriParser(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, new Ipv4NlriParser()));
		regs.add(context.registerNlriParser(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, new Ipv6NlriParser()));

		final AttributeRegistry attrReg = context.getAttributeRegistry();
		regs.add(context.registerAttributeParser(OriginAttributeParser.TYPE, new OriginAttributeParser()));
		regs.add(context.registerAttributeParser(AsPathAttributeParser.TYPE, new AsPathAttributeParser()));
		regs.add(context.registerAttributeParser(NextHopAttributeParser.TYPE, new NextHopAttributeParser()));
		regs.add(context.registerAttributeParser(MultiExitDiscriminatorAttributeParser.TYPE, new MultiExitDiscriminatorAttributeParser()));
		regs.add(context.registerAttributeParser(LocalPreferenceAttributeParser.TYPE, new LocalPreferenceAttributeParser()));
		regs.add(context.registerAttributeParser(AtomicAggregateAttributeParser.TYPE, new AtomicAggregateAttributeParser()));
		regs.add(context.registerAttributeParser(AggregatorAttributeParser.TYPE, new AggregatorAttributeParser()));
		regs.add(context.registerAttributeParser(CommunitiesAttributeParser.TYPE, new CommunitiesAttributeParser()));
		regs.add(context.registerAttributeParser(OriginatorIdAttributeParser.TYPE, new OriginatorIdAttributeParser()));
		regs.add(context.registerAttributeParser(ClusterIdAttributeParser.TYPE, new ClusterIdAttributeParser()));
		regs.add(context.registerAttributeParser(MPReachAttributeParser.TYPE, new MPReachAttributeParser(nlriReg)));
		regs.add(context.registerAttributeParser(MPUnreachAttributeParser.TYPE, new MPUnreachAttributeParser(nlriReg)));
		regs.add(context.registerAttributeParser(ExtendedCommunitiesAttributeParser.TYPE, new ExtendedCommunitiesAttributeParser()));
		regs.add(context.registerAttributeParser(AS4AggregatorAttributeParser.TYPE, new AS4AggregatorAttributeParser()));
		regs.add(context.registerAttributeParser(AS4PathAttributeParser.TYPE, new AS4PathAttributeParser()));

		final CapabilityRegistry capReg = context.getCapabilityRegistry();
		final MultiProtocolCapabilityHandler multi = new MultiProtocolCapabilityHandler(afiReg, safiReg);
		regs.add(context.registerCapabilityParser(MultiProtocolCapabilityHandler.CODE, multi));
		regs.add(context.registerCapabilitySerializer(CMultiprotocol.class, multi));

		final As4CapabilityHandler as4 = new As4CapabilityHandler();
		regs.add(context.registerCapabilityParser(As4CapabilityHandler.CODE, as4));
		regs.add(context.registerCapabilitySerializer(CAs4Bytes.class, as4));

		final GracefulCapabilityHandler grace = new GracefulCapabilityHandler(afiReg, safiReg);
		regs.add(context.registerCapabilitySerializer(CGracefulRestart.class, grace));
		regs.add(context.registerCapabilityParser(GracefulCapabilityHandler.CODE, grace));

		final ParameterRegistry paramReg = context.getParameterRegistry();
		final CapabilityParameterParser cpp = new CapabilityParameterParser(capReg);
		regs.add(context.registerParameterParser(CapabilityParameterParser.TYPE, cpp));
		regs.add(context.registerParameterSerializer(BgpParameters.class, cpp));

		final BGPOpenMessageParser omp = new BGPOpenMessageParser(paramReg);
		regs.add(context.registerMessageParser(BGPOpenMessageParser.TYPE, omp));
		regs.add(context.registerMessageSerializer(Open.class, omp));

		final BGPUpdateMessageParser ump = new BGPUpdateMessageParser(attrReg);
		regs.add(context.registerMessageParser(BGPUpdateMessageParser.TYPE, ump));
		// Serialization of Update message is not supported

		final BGPNotificationMessageParser nmp = new BGPNotificationMessageParser();
		regs.add(context.registerMessageParser(BGPNotificationMessageParser.TYPE, nmp));
		regs.add(context.registerMessageSerializer(Notify.class, nmp));

		final BGPKeepAliveMessageParser kamp = new BGPKeepAliveMessageParser();
		regs.add(context.registerMessageParser(BGPKeepAliveMessageParser.TYPE, kamp));
		regs.add(context.registerMessageSerializer(Keepalive.class, kamp));

		this.registrations = regs;
	}

	@Override
	public synchronized void stop() {
		Preconditions.checkState(this.registrations != null);

		for (final AutoCloseable r : this.registrations) {
			try {
				r.close();
			} catch (final Exception e) {
				logger.warn("Failed to close registration", e);
			}
		}

		this.registrations = null;
	}
}
