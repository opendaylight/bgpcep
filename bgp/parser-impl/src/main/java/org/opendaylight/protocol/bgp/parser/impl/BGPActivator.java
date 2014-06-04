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
import org.opendaylight.protocol.bgp.parser.impl.message.update.AdvertizedRoutesSerializer;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AggregatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AtomicAggregateAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ClusterIdAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunitiesAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ExtendedCommunitiesAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.Ipv4NextHopAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.Ipv4NlriParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.Ipv6NextHopAttributeSerializer;
import org.opendaylight.protocol.bgp.parser.impl.message.update.Ipv6NlriParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LocalPreferenceAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MPReachAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MPUnreachAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MultiExitDiscriminatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginatorIdAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.PathAttributeSerializer;
import org.opendaylight.protocol.bgp.parser.impl.message.update.WithdrawnRoutesSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.GracefulRestartCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.MultiprotocolCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {
	@Override
	protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
		final List<AutoCloseable> regs = new ArrayList<>();

        context.registerNlriSerializer(AdvertizedRoutes.class, new AdvertizedRoutesSerializer());
        context.registerNlriSerializer(WithdrawnRoutes.class, new WithdrawnRoutesSerializer());

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

        PathAttributeSerializer pathAttributeSerializer = new PathAttributeSerializer();
        OriginAttributeParser originAttributeParser = new OriginAttributeParser();
        pathAttributeSerializer.registerSerializer(originAttributeParser);
		regs.add(context.registerAttributeParser(OriginAttributeParser.TYPE, originAttributeParser));

        AsPathAttributeParser asPathAttributeParser = new AsPathAttributeParser(context.getReferenceCache());
        pathAttributeSerializer.registerSerializer(asPathAttributeParser);
		regs.add(context.registerAttributeParser(AsPathAttributeParser.TYPE, asPathAttributeParser));

        Ipv4NextHopAttributeParser ipv4NextHopAttributeParser = new Ipv4NextHopAttributeParser();
        pathAttributeSerializer.registerSerializer(ipv4NextHopAttributeParser);
		regs.add(context.registerAttributeParser(Ipv4NextHopAttributeParser.TYPE, ipv4NextHopAttributeParser));

        Ipv6NextHopAttributeSerializer ipv6NextHopAttributeSerializer = new Ipv6NextHopAttributeSerializer();
        pathAttributeSerializer.registerSerializer(ipv6NextHopAttributeSerializer);
        regs.add(context.registerAttributeParser(Ipv4NextHopAttributeParser.TYPE, ipv4NextHopAttributeParser));


        MultiExitDiscriminatorAttributeParser multiExitDiscriminatorAttributeParser = new  MultiExitDiscriminatorAttributeParser();
        pathAttributeSerializer.registerSerializer(multiExitDiscriminatorAttributeParser);
		regs.add(context.registerAttributeParser(MultiExitDiscriminatorAttributeParser.TYPE, multiExitDiscriminatorAttributeParser));

        LocalPreferenceAttributeParser localPreferenceAttributeParser = new LocalPreferenceAttributeParser();
        pathAttributeSerializer.registerSerializer(localPreferenceAttributeParser);
		regs.add(context.registerAttributeParser(LocalPreferenceAttributeParser.TYPE, localPreferenceAttributeParser));

        AtomicAggregateAttributeParser atomicAggregateAttributeParser = new AtomicAggregateAttributeParser();
        pathAttributeSerializer.registerSerializer(atomicAggregateAttributeParser);
        regs.add(context.registerAttributeParser(AtomicAggregateAttributeParser.TYPE, atomicAggregateAttributeParser));

        AggregatorAttributeParser as4AggregatorAttributeParser = new AggregatorAttributeParser(context.getReferenceCache());
        pathAttributeSerializer.registerSerializer(as4AggregatorAttributeParser);
		regs.add(context.registerAttributeParser(AggregatorAttributeParser.TYPE, as4AggregatorAttributeParser));

        CommunitiesAttributeParser communitiesAttributeParser = new CommunitiesAttributeParser(context.getReferenceCache());
        pathAttributeSerializer.registerSerializer(communitiesAttributeParser);
		regs.add(context.registerAttributeParser(CommunitiesAttributeParser.TYPE, communitiesAttributeParser));

        OriginatorIdAttributeParser originatorIdAttributeParser = new OriginatorIdAttributeParser();
        pathAttributeSerializer.registerSerializer(originatorIdAttributeParser);
		regs.add(context.registerAttributeParser(OriginatorIdAttributeParser.TYPE, originatorIdAttributeParser));

        ClusterIdAttributeParser clusterIdAttributeParser = new ClusterIdAttributeParser();
        pathAttributeSerializer.registerSerializer(clusterIdAttributeParser);
		regs.add(context.registerAttributeParser(ClusterIdAttributeParser.TYPE, clusterIdAttributeParser));

        MPReachAttributeParser mpReachAttributeParser = new MPReachAttributeParser(nlriReg);
        pathAttributeSerializer.registerSerializer(mpReachAttributeParser);
        regs.add(context.registerAttributeParser(MPReachAttributeParser.TYPE, mpReachAttributeParser));


        MPUnreachAttributeParser mpUnreachAttributeParser = new MPUnreachAttributeParser(nlriReg);
        pathAttributeSerializer.registerSerializer(mpUnreachAttributeParser);
        regs.add(context.registerAttributeParser(MPUnreachAttributeParser.TYPE, mpUnreachAttributeParser));


        ExtendedCommunitiesAttributeParser extendedCommunitiesAttributeParser = new ExtendedCommunitiesAttributeParser(context.getReferenceCache());
        pathAttributeSerializer.registerSerializer(extendedCommunitiesAttributeParser);
        regs.add(context.registerAttributeParser(ExtendedCommunitiesAttributeParser.TYPE, extendedCommunitiesAttributeParser));

		regs.add(context.registerAttributeParser(AS4AggregatorAttributeParser.TYPE, new AS4AggregatorAttributeParser()));
		regs.add(context.registerAttributeParser(AS4PathAttributeParser.TYPE, new AS4PathAttributeParser()));

        regs.add(context.registerAttributeSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes.class,pathAttributeSerializer));

		final CapabilityRegistry capReg = context.getCapabilityRegistry();
		final MultiProtocolCapabilityHandler multi = new MultiProtocolCapabilityHandler(afiReg, safiReg);
		regs.add(context.registerCapabilityParser(MultiProtocolCapabilityHandler.CODE, multi));
		regs.add(context.registerCapabilitySerializer(MultiprotocolCase.class, multi));

		final As4CapabilityHandler as4 = new As4CapabilityHandler();
		regs.add(context.registerCapabilityParser(As4CapabilityHandler.CODE, as4));
		regs.add(context.registerCapabilitySerializer(As4BytesCase.class, as4));

		final GracefulCapabilityHandler grace = new GracefulCapabilityHandler(afiReg, safiReg);
		regs.add(context.registerCapabilitySerializer(GracefulRestartCase.class, grace));
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
        regs.add(context.registerMessageSerializer(Update.class,ump));

		final BGPNotificationMessageParser nmp = new BGPNotificationMessageParser();
		regs.add(context.registerMessageParser(BGPNotificationMessageParser.TYPE, nmp));
		regs.add(context.registerMessageSerializer(Notify.class, nmp));

		final BGPKeepAliveMessageParser kamp = new BGPKeepAliveMessageParser();
		regs.add(context.registerMessageParser(BGPKeepAliveMessageParser.TYPE, kamp));
		regs.add(context.registerMessageSerializer(Keepalive.class, kamp));

		return regs;
	}
}
