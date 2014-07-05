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
import org.opendaylight.protocol.bgp.parser.impl.message.update.Ipv4NlriParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.Ipv6NlriParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LocalPreferenceAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MPReachAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MPUnreachAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MultiExitDiscriminatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.NextHopAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginatorIdAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.WithdrawnRoutesSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AtomicAggregate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.GracefulRestartCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.c.parameters.MultiprotocolCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    private static final int IPV4_AFI = 1;
    private static final int IPV6_AFI = 2;

    private static final int UNICAST_SAFI = 1;
    private static final int VPN_SAFI = 128;

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerNlriSerializer(AdvertizedRoutes.class, new AdvertizedRoutesSerializer()));
        regs.add(context.registerNlriSerializer(WithdrawnRoutes.class, new WithdrawnRoutesSerializer()));

        regs.add(context.registerAddressFamily(Ipv4AddressFamily.class, IPV4_AFI));
        regs.add(context.registerAddressFamily(Ipv6AddressFamily.class, IPV6_AFI));

        regs.add(context.registerSubsequentAddressFamily(UnicastSubsequentAddressFamily.class, UNICAST_SAFI));
        regs.add(context.registerSubsequentAddressFamily(MplsLabeledVpnSubsequentAddressFamily.class, VPN_SAFI));

        regs.add(context.registerNlriParser(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, new Ipv4NlriParser()));
        regs.add(context.registerNlriParser(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, new Ipv6NlriParser()));

        this.registerCapabilityParsers(regs, context);
        this.registerAttributeParsers(regs, context);
        this.registerMessageParsers(regs, context);
        return regs;
    }

    private void registerCapabilityParsers(final List<AutoCloseable> regs, final BGPExtensionProviderContext context) {
        final AddressFamilyRegistry afiReg = context.getAddressFamilyRegistry();
        final SubsequentAddressFamilyRegistry safiReg = context.getSubsequentAddressFamilyRegistry();

        final MultiProtocolCapabilityHandler multi = new MultiProtocolCapabilityHandler(afiReg, safiReg);
        regs.add(context.registerCapabilityParser(MultiProtocolCapabilityHandler.CODE, multi));
        regs.add(context.registerCapabilitySerializer(MultiprotocolCase.class, multi));

        final As4CapabilityHandler as4 = new As4CapabilityHandler();
        regs.add(context.registerCapabilityParser(As4CapabilityHandler.CODE, as4));
        regs.add(context.registerCapabilitySerializer(As4BytesCase.class, as4));

        final GracefulCapabilityHandler grace = new GracefulCapabilityHandler(afiReg, safiReg);
        regs.add(context.registerCapabilitySerializer(GracefulRestartCase.class, grace));
        regs.add(context.registerCapabilityParser(GracefulCapabilityHandler.CODE, grace));

        final CapabilityParameterParser cpp = new CapabilityParameterParser(context.getCapabilityRegistry());
        regs.add(context.registerParameterParser(CapabilityParameterParser.TYPE, cpp));
        regs.add(context.registerParameterSerializer(BgpParameters.class, cpp));
    }

    private void registerAttributeParsers(final List<AutoCloseable> regs, final BGPExtensionProviderContext context) {
        final OriginAttributeParser originAttributeParser = new OriginAttributeParser();
        regs.add(context.registerAttributeSerializer(Origin.class, originAttributeParser));
        regs.add(context.registerAttributeParser(OriginAttributeParser.TYPE, originAttributeParser));

        final AsPathAttributeParser asPathAttributeParser = new AsPathAttributeParser(context.getReferenceCache());
        regs.add(context.registerAttributeSerializer(AsPath.class, asPathAttributeParser));
        regs.add(context.registerAttributeParser(AsPathAttributeParser.TYPE, asPathAttributeParser));

        final NextHopAttributeParser nextHopAttributeParser = new NextHopAttributeParser();
        regs.add(context.registerAttributeSerializer(NextHop.class, nextHopAttributeParser));
        regs.add(context.registerAttributeParser(NextHopAttributeParser.TYPE, nextHopAttributeParser));

        final MultiExitDiscriminatorAttributeParser multiExitDiscriminatorAttributeParser = new MultiExitDiscriminatorAttributeParser();
        regs.add(context.registerAttributeSerializer(MultiExitDisc.class, multiExitDiscriminatorAttributeParser));
        regs.add(context.registerAttributeParser(MultiExitDiscriminatorAttributeParser.TYPE, multiExitDiscriminatorAttributeParser));

        final LocalPreferenceAttributeParser localPreferenceAttributeParser = new LocalPreferenceAttributeParser();
        regs.add(context.registerAttributeSerializer(LocalPref.class, localPreferenceAttributeParser));
        regs.add(context.registerAttributeParser(LocalPreferenceAttributeParser.TYPE, localPreferenceAttributeParser));

        final AtomicAggregateAttributeParser atomicAggregateAttributeParser = new AtomicAggregateAttributeParser();
        regs.add(context.registerAttributeSerializer(AtomicAggregate.class, atomicAggregateAttributeParser));
        regs.add(context.registerAttributeParser(AtomicAggregateAttributeParser.TYPE, atomicAggregateAttributeParser));

        final AggregatorAttributeParser as4AggregatorAttributeParser = new AggregatorAttributeParser(context.getReferenceCache());
        regs.add(context.registerAttributeSerializer(Aggregator.class, as4AggregatorAttributeParser));
        regs.add(context.registerAttributeParser(AggregatorAttributeParser.TYPE, as4AggregatorAttributeParser));

        final CommunitiesAttributeParser communitiesAttributeParser = new CommunitiesAttributeParser(context.getReferenceCache());
        regs.add(context.registerAttributeSerializer(Communities.class, communitiesAttributeParser));
        regs.add(context.registerAttributeParser(CommunitiesAttributeParser.TYPE, communitiesAttributeParser));

        final OriginatorIdAttributeParser originatorIdAttributeParser = new OriginatorIdAttributeParser();
        regs.add(context.registerAttributeSerializer(OriginatorId.class, originatorIdAttributeParser));
        regs.add(context.registerAttributeParser(OriginatorIdAttributeParser.TYPE, originatorIdAttributeParser));

        final ClusterIdAttributeParser clusterIdAttributeParser = new ClusterIdAttributeParser();
        regs.add(context.registerAttributeSerializer(ClusterId.class, clusterIdAttributeParser));
        regs.add(context.registerAttributeParser(ClusterIdAttributeParser.TYPE, clusterIdAttributeParser));

        final NlriRegistry nlriReg = context.getNlriRegistry();

        final MPReachAttributeParser mpReachAttributeParser = new MPReachAttributeParser(nlriReg);
        regs.add(context.registerAttributeSerializer(MpReachNlri.class, mpReachAttributeParser));
        regs.add(context.registerAttributeParser(MPReachAttributeParser.TYPE, mpReachAttributeParser));

        final MPUnreachAttributeParser mpUnreachAttributeParser = new MPUnreachAttributeParser(nlriReg);
        regs.add(context.registerAttributeSerializer(MpUnreachNlri.class, mpUnreachAttributeParser));
        regs.add(context.registerAttributeParser(MPUnreachAttributeParser.TYPE, mpUnreachAttributeParser));

        final ExtendedCommunitiesAttributeParser extendedCommunitiesAttributeParser = new ExtendedCommunitiesAttributeParser(context.getReferenceCache());
        regs.add(context.registerAttributeSerializer(ExtendedCommunities.class, extendedCommunitiesAttributeParser));
        regs.add(context.registerAttributeParser(ExtendedCommunitiesAttributeParser.TYPE, extendedCommunitiesAttributeParser));

        regs.add(context.registerAttributeParser(AS4AggregatorAttributeParser.TYPE, new AS4AggregatorAttributeParser()));
        regs.add(context.registerAttributeParser(AS4PathAttributeParser.TYPE, new AS4PathAttributeParser()));
    }

    private void registerMessageParsers(final List<AutoCloseable> regs, final BGPExtensionProviderContext context) {
        final BGPOpenMessageParser omp = new BGPOpenMessageParser(context.getParameterRegistry());
        regs.add(context.registerMessageParser(BGPOpenMessageParser.TYPE, omp));
        regs.add(context.registerMessageSerializer(Open.class, omp));

        final BGPUpdateMessageParser ump = new BGPUpdateMessageParser(context.getAttributeRegistry());
        regs.add(context.registerMessageParser(BGPUpdateMessageParser.TYPE, ump));
        regs.add(context.registerMessageSerializer(Update.class, ump));

        final BGPNotificationMessageParser nmp = new BGPNotificationMessageParser();
        regs.add(context.registerMessageParser(BGPNotificationMessageParser.TYPE, nmp));
        regs.add(context.registerMessageSerializer(Notify.class, nmp));

        final BGPKeepAliveMessageParser kamp = new BGPKeepAliveMessageParser();
        regs.add(context.registerMessageParser(BGPKeepAliveMessageParser.TYPE, kamp));
        regs.add(context.registerMessageSerializer(Keepalive.class, kamp));
    }
}
