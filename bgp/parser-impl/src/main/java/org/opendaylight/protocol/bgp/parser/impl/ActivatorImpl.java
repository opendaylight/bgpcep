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

import org.opendaylight.protocol.bgp.linkstate.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.linkstate.LinkstateNlriParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPKeepAliveMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPNotificationMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPOpenMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.open.As4CapabilityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.open.CapabilityParameterParser;
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
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.ProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev130918.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.CAs4Bytes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class ActivatorImpl implements ProviderActivator {
	private static final Logger logger = LoggerFactory.getLogger(ActivatorImpl.class);
	private List<AutoCloseable> registrations;

	@Override
	public synchronized void start(final ProviderContext context) {
		Preconditions.checkState(registrations == null);
		final List<AutoCloseable> regs = new ArrayList<>();

		final AddressFamilyRegistry afiReg = context.getAddressFamilyRegistry();
		regs.add(afiReg.registerAddressFamily(Ipv4AddressFamily.class, 1));
		regs.add(afiReg.registerAddressFamily(Ipv6AddressFamily.class, 2));
		regs.add(afiReg.registerAddressFamily(LinkstateAddressFamily.class, 16388));

		final SubsequentAddressFamilyRegistry safiReg = context.getSubsequentAddressFamilyRegistry();
		regs.add(safiReg.registerSubsequentAddressFamily(UnicastSubsequentAddressFamily.class, 1));
		regs.add(safiReg.registerSubsequentAddressFamily(LinkstateSubsequentAddressFamily.class, 71));
		regs.add(safiReg.registerSubsequentAddressFamily(MplsLabeledVpnSubsequentAddressFamily.class, 128));

		final NlriRegistry nlriReg = context.getNlriRegistry();
		regs.add(nlriReg.registerNlriParser(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class,
				new Ipv4NlriParser()));
		regs.add(nlriReg.registerNlriParser(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class,
				new Ipv6NlriParser()));
		regs.add(nlriReg.registerNlriParser(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
				new LinkstateNlriParser(false)));
		regs.add(nlriReg.registerNlriParser(LinkstateAddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
				new LinkstateNlriParser(true)));

		final AttributeRegistry attrReg = context.getAttributeRegistry();
		regs.add(attrReg.registerAttributeParser(OriginAttributeParser.TYPE, new OriginAttributeParser()));
		regs.add(attrReg.registerAttributeParser(AsPathAttributeParser.TYPE, new AsPathAttributeParser()));
		regs.add(attrReg.registerAttributeParser(NextHopAttributeParser.TYPE, new NextHopAttributeParser()));
		regs.add(attrReg.registerAttributeParser(MultiExitDiscriminatorAttributeParser.TYPE,
				new MultiExitDiscriminatorAttributeParser()));
		regs.add(attrReg.registerAttributeParser(LocalPreferenceAttributeParser.TYPE, new LocalPreferenceAttributeParser()));
		regs.add(attrReg.registerAttributeParser(AtomicAggregateAttributeParser.TYPE, new AtomicAggregateAttributeParser()));
		regs.add(attrReg.registerAttributeParser(AggregatorAttributeParser.TYPE, new AggregatorAttributeParser()));
		regs.add(attrReg.registerAttributeParser(CommunitiesAttributeParser.TYPE, new CommunitiesAttributeParser()));
		regs.add(attrReg.registerAttributeParser(OriginatorIdAttributeParser.TYPE, new OriginatorIdAttributeParser()));
		regs.add(attrReg.registerAttributeParser(ClusterIdAttributeParser.TYPE, new ClusterIdAttributeParser()));
		regs.add(attrReg.registerAttributeParser(MPReachAttributeParser.TYPE, new MPReachAttributeParser(nlriReg)));
		regs.add(attrReg.registerAttributeParser(MPUnreachAttributeParser.TYPE, new MPUnreachAttributeParser(nlriReg)));
		regs.add(attrReg.registerAttributeParser(ExtendedCommunitiesAttributeParser.TYPE, new ExtendedCommunitiesAttributeParser()));
		regs.add(attrReg.registerAttributeParser(AS4AggregatorAttributeParser.TYPE, new AS4AggregatorAttributeParser()));
		regs.add(attrReg.registerAttributeParser(AS4PathAttributeParser.TYPE, new AS4PathAttributeParser()));
		regs.add(attrReg.registerAttributeParser(LinkstateAttributeParser.TYPE, new LinkstateAttributeParser()));

		final CapabilityRegistry capReg = context.getCapabilityRegistry();
		final MultiProtocolCapabilityHandler multi = new MultiProtocolCapabilityHandler(afiReg, safiReg);
		regs.add(capReg.registerCapabilityParser(MultiProtocolCapabilityHandler.CODE, multi));
		regs.add(capReg.registerCapabilitySerializer(CMultiprotocol.class, multi));

		final As4CapabilityHandler as4 = new As4CapabilityHandler();
		regs.add(capReg.registerCapabilityParser(As4CapabilityHandler.CODE, as4));
		regs.add(capReg.registerCapabilitySerializer(CAs4Bytes.class, as4));

		final ParameterRegistry paramReg = context.getParameterRegistry();
		final CapabilityParameterParser cpp = new CapabilityParameterParser(capReg);
		regs.add(paramReg.registerParameterParser(CapabilityParameterParser.TYPE, cpp));
		regs.add(paramReg.registerParameterSerializer(BgpParameters.class, cpp));

		final MessageRegistry msgReg = context.getMessageRegistry();
		final BGPOpenMessageParser omp = new BGPOpenMessageParser(paramReg);
		regs.add(msgReg.registerMessageParser(BGPOpenMessageParser.TYPE, omp));
		regs.add(msgReg.registerMessageSerializer(Open.class, omp));

		final BGPUpdateMessageParser ump = new BGPUpdateMessageParser(attrReg);
		regs.add(msgReg.registerMessageParser(BGPUpdateMessageParser.TYPE, ump));
		// Serialization of Update message is not supported
		// regs.add(msgReg.registerMessageSerializer(Update.class, ump));

		final BGPNotificationMessageParser nmp = new BGPNotificationMessageParser();
		regs.add(msgReg.registerMessageParser(BGPNotificationMessageParser.TYPE, nmp));
		regs.add(msgReg.registerMessageSerializer(Notify.class, nmp));

		final BGPKeepAliveMessageParser kamp = new BGPKeepAliveMessageParser();
		regs.add(msgReg.registerMessageParser(BGPKeepAliveMessageParser.TYPE, kamp));
		regs.add(msgReg.registerMessageSerializer(Keepalive.class, kamp));

		registrations = regs;
	}

	@Override
	public synchronized void stop() {
		Preconditions.checkState(registrations != null);

		for (AutoCloseable r : registrations) {
			try {
				r.close();
			} catch (Exception e) {
				logger.warn("Failed to close registration", e);
			}
		}

		registrations = null;
	}
}
