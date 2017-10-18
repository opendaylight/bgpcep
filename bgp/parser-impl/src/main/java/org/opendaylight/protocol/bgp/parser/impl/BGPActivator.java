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
import org.opendaylight.protocol.bgp.parser.impl.message.BGPRouteRefreshMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.open.AddPathCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.open.As4CapabilityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.open.BgpExtendedMessageCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.open.CapabilityParameterParser;
import org.opendaylight.protocol.bgp.parser.impl.message.open.GracefulCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.open.MultiProtocolCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.open.RouteRefreshCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AS4AggregatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AS4PathAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AggregatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AigpAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AsPathAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AtomicAggregateAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.BgpPrefixSidAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ClusterIdAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.CommunitiesAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ExtendedCommunitiesAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.LocalPreferenceAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MPReachAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MPUnreachAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MultiExitDiscriminatorAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.NextHopAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginatorIdAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.UnrecognizedAttributesSerializer;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.AsTwoOctetSpecificEcHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.EncapsulationEC;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.Ipv4SpecificEcHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.LinkBandwidthEC;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.OpaqueEcHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.RouteOriginAsTwoOctetEcHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.RouteOriginIpv4EcHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.RouteTargetAsTwoOctetEcHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.RouteTargetIpv4EcHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.four.octect.as.specific.Generic4OctASEcHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.four.octect.as.specific.RouteOrigin4OctectASEcHandler;
import org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.four.octect.as.specific.RouteTarget4OctectASEcHandler;
import org.opendaylight.protocol.bgp.parser.spi.AbstractBGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.open.message.bgp.parameters.optional.capabilities.c.parameters.BgpExtendedMessageCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Aggregator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Aigp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.AtomicAggregate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.BgpPrefixSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.ClusterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.AddPathCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.GracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.MultiprotocolCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.RouteRefreshCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.As4GenericSpecExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.As4RouteOriginExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.As4RouteTargetExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.AsSpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.EncapsulationCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.LinkBandwidthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.OpaqueExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginIpv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetIpv4Case;

public final class BGPActivator extends AbstractBGPExtensionProviderActivator {

    private static final int IPV4_AFI = 1;
    private static final int IPV6_AFI = 2;

    private static final int UNICAST_SAFI = 1;
    private static final int VPN_SAFI = 128;

    @Override
    protected List<AutoCloseable> startImpl(final BGPExtensionProviderContext context) {
        final List<AutoCloseable> regs = new ArrayList<>();

        regs.add(context.registerAddressFamily(Ipv4AddressFamily.class, IPV4_AFI));
        regs.add(context.registerAddressFamily(Ipv6AddressFamily.class, IPV6_AFI));

        regs.add(context.registerSubsequentAddressFamily(UnicastSubsequentAddressFamily.class, UNICAST_SAFI));
        regs.add(context.registerSubsequentAddressFamily(MplsLabeledVpnSubsequentAddressFamily.class, VPN_SAFI));

        registerExtendedCommunities(regs, context);
        registerCapabilityParsers(regs, context);
        registerAttributeParsers(regs, context);
        registerMessageParsers(regs, context);
        return regs;
    }

    private static void registerCapabilityParsers(final List<AutoCloseable> regs, final BGPExtensionProviderContext context) {
        final AddressFamilyRegistry afiReg = context.getAddressFamilyRegistry();
        final SubsequentAddressFamilyRegistry safiReg = context.getSubsequentAddressFamilyRegistry();

        final MultiProtocolCapabilityHandler multi = new MultiProtocolCapabilityHandler(afiReg, safiReg);
        regs.add(context.registerCapabilityParser(MultiProtocolCapabilityHandler.CODE, multi));
        regs.add(context.registerCapabilitySerializer(MultiprotocolCapability.class, multi));

        final AddPathCapabilityHandler addPath = new AddPathCapabilityHandler(afiReg, safiReg);
        regs.add(context.registerCapabilityParser(AddPathCapabilityHandler.CODE, addPath));
        regs.add(context.registerCapabilitySerializer(AddPathCapability.class, addPath));

        final RouteRefreshCapabilityHandler routeRefresh = new RouteRefreshCapabilityHandler();
        regs.add(context.registerCapabilityParser(RouteRefreshCapabilityHandler.CODE, routeRefresh));
        regs.add(context.registerCapabilitySerializer(RouteRefreshCapability.class, routeRefresh));

        final As4CapabilityHandler as4 = new As4CapabilityHandler();
        regs.add(context.registerCapabilityParser(As4CapabilityHandler.CODE, as4));
        regs.add(context.registerCapabilitySerializer(As4BytesCapability.class, as4));

        final GracefulCapabilityHandler grace = new GracefulCapabilityHandler(afiReg, safiReg);
        regs.add(context.registerCapabilitySerializer(GracefulRestartCapability.class, grace));
        regs.add(context.registerCapabilityParser(GracefulCapabilityHandler.CODE, grace));

        final CapabilityParameterParser cpp = new CapabilityParameterParser(context.getCapabilityRegistry());
        regs.add(context.registerParameterParser(CapabilityParameterParser.TYPE, cpp));
        regs.add(context.registerParameterSerializer(BgpParameters.class, cpp));

        final BgpExtendedMessageCapabilityHandler bgpextmessage = new BgpExtendedMessageCapabilityHandler();
        regs.add(context.registerCapabilityParser(BgpExtendedMessageCapabilityHandler.CODE, bgpextmessage));
        regs.add(context.registerCapabilitySerializer(BgpExtendedMessageCapability.class, bgpextmessage));
    }

    private static void registerAttributeParsers(final List<AutoCloseable> regs, final BGPExtensionProviderContext context) {
        final BgpPrefixSidAttributeParser prefixSidAttributeParser = new BgpPrefixSidAttributeParser(context.getBgpPrefixSidTlvRegistry());
        regs.add(context.registerAttributeSerializer(BgpPrefixSid.class, prefixSidAttributeParser));
        regs.add(context.registerAttributeParser(BgpPrefixSidAttributeParser.TYPE, prefixSidAttributeParser));

        final OriginAttributeParser originAttributeParser = new OriginAttributeParser();
        regs.add(context.registerAttributeSerializer(Origin.class, originAttributeParser));
        regs.add(context.registerAttributeParser(OriginAttributeParser.TYPE, originAttributeParser));

        final AigpAttributeParser aigpAttributeParser = new AigpAttributeParser();
        regs.add(context.registerAttributeSerializer(Aigp.class, aigpAttributeParser));
        regs.add(context.registerAttributeParser(AigpAttributeParser.TYPE, aigpAttributeParser));

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

        final ExtendedCommunitiesAttributeParser extendedCommunitiesAttributeParser = new ExtendedCommunitiesAttributeParser(context.getExtendedCommunityRegistry());
        regs.add(context.registerAttributeSerializer(ExtendedCommunities.class, extendedCommunitiesAttributeParser));
        regs.add(context.registerAttributeParser(ExtendedCommunitiesAttributeParser.TYPE, extendedCommunitiesAttributeParser));

        regs.add(context.registerAttributeParser(AS4AggregatorAttributeParser.TYPE, new AS4AggregatorAttributeParser()));
        regs.add(context.registerAttributeParser(AS4PathAttributeParser.TYPE, new AS4PathAttributeParser()));

        regs.add(context.registerAttributeSerializer(UnrecognizedAttributes.class, new UnrecognizedAttributesSerializer()));
    }

    private static void registerMessageParsers(final List<AutoCloseable> regs, final BGPExtensionProviderContext context) {
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

        final AddressFamilyRegistry afiReg = context.getAddressFamilyRegistry();
        final SubsequentAddressFamilyRegistry safiReg = context.getSubsequentAddressFamilyRegistry();
        final BGPRouteRefreshMessageParser rrmp = new BGPRouteRefreshMessageParser(afiReg, safiReg);
        regs.add(context.registerMessageParser(BGPRouteRefreshMessageParser.TYPE, rrmp));
        regs.add(context.registerMessageSerializer(RouteRefresh.class, rrmp));
    }

    private static void registerExtendedCommunities(final List<AutoCloseable> regs, final BGPExtensionProviderContext context) {
        final AsTwoOctetSpecificEcHandler twoOctetSpecificEcHandler = new AsTwoOctetSpecificEcHandler();
        regs.add(context.registerExtendedCommunityParser(twoOctetSpecificEcHandler.getType(true), twoOctetSpecificEcHandler.getSubType(),
                twoOctetSpecificEcHandler));
        regs.add(context.registerExtendedCommunityParser(twoOctetSpecificEcHandler.getType(false), twoOctetSpecificEcHandler.getSubType(),
                twoOctetSpecificEcHandler));
        regs.add(context.registerExtendedCommunitySerializer(AsSpecificExtendedCommunityCase.class, twoOctetSpecificEcHandler));

        final Ipv4SpecificEcHandler ipv4SpecificEcHandler = new Ipv4SpecificEcHandler();
        regs.add(context.registerExtendedCommunityParser(ipv4SpecificEcHandler.getType(true), ipv4SpecificEcHandler.getSubType(),
                ipv4SpecificEcHandler));
        regs.add(context.registerExtendedCommunityParser(ipv4SpecificEcHandler.getType(false), ipv4SpecificEcHandler.getSubType(),
                ipv4SpecificEcHandler));
        regs.add(context.registerExtendedCommunitySerializer(Inet4SpecificExtendedCommunityCase.class, ipv4SpecificEcHandler));

        final OpaqueEcHandler opaqueEcHandler = new OpaqueEcHandler();
        regs.add(context.registerExtendedCommunityParser(opaqueEcHandler.getType(true), opaqueEcHandler.getSubType(),
                opaqueEcHandler));
        regs.add(context.registerExtendedCommunityParser(opaqueEcHandler.getType(false), opaqueEcHandler.getSubType(),
                opaqueEcHandler));
        regs.add(context.registerExtendedCommunitySerializer(OpaqueExtendedCommunityCase.class, opaqueEcHandler));

        final RouteOriginAsTwoOctetEcHandler routeOriginAS2bEcHandler = new RouteOriginAsTwoOctetEcHandler();
        regs.add(context.registerExtendedCommunityParser(routeOriginAS2bEcHandler.getType(true), routeOriginAS2bEcHandler.getSubType(),
                routeOriginAS2bEcHandler));
        regs.add(context.registerExtendedCommunityParser(routeOriginAS2bEcHandler.getType(false), routeOriginAS2bEcHandler.getSubType(),
                routeOriginAS2bEcHandler));
        regs.add(context.registerExtendedCommunitySerializer(RouteOriginExtendedCommunityCase.class, routeOriginAS2bEcHandler));

        final RouteTargetAsTwoOctetEcHandler routeTargetAS2bEcHandler = new RouteTargetAsTwoOctetEcHandler();
        regs.add(context.registerExtendedCommunityParser(routeTargetAS2bEcHandler.getType(true), routeTargetAS2bEcHandler.getSubType(),
                routeTargetAS2bEcHandler));
        regs.add(context.registerExtendedCommunityParser(routeTargetAS2bEcHandler.getType(false), routeTargetAS2bEcHandler.getSubType(),
                routeTargetAS2bEcHandler));
        regs.add(context.registerExtendedCommunitySerializer(RouteTargetExtendedCommunityCase.class, routeTargetAS2bEcHandler));

        final RouteOriginIpv4EcHandler routeOriginIpv4EcHandler = new RouteOriginIpv4EcHandler();
        regs.add(context.registerExtendedCommunityParser(routeOriginIpv4EcHandler.getType(true), routeOriginIpv4EcHandler.getSubType(),
                routeOriginIpv4EcHandler));
        regs.add(context.registerExtendedCommunityParser(routeOriginIpv4EcHandler.getType(false), routeOriginIpv4EcHandler.getSubType(),
                routeOriginIpv4EcHandler));
        regs.add(context.registerExtendedCommunitySerializer(RouteOriginIpv4Case.class, routeOriginIpv4EcHandler));

        final RouteTargetIpv4EcHandler routeTargetIpv4EcHandler = new RouteTargetIpv4EcHandler();
        regs.add(context.registerExtendedCommunityParser(routeTargetIpv4EcHandler.getType(true), routeTargetIpv4EcHandler.getSubType(),
                routeTargetIpv4EcHandler));
        regs.add(context.registerExtendedCommunityParser(routeTargetIpv4EcHandler.getType(false), routeTargetIpv4EcHandler.getSubType(),
                routeTargetIpv4EcHandler));
        regs.add(context.registerExtendedCommunitySerializer(RouteTargetIpv4Case.class, routeTargetIpv4EcHandler));

        final LinkBandwidthEC linkBandwidthECHandler = new LinkBandwidthEC();
        regs.add(context.registerExtendedCommunityParser(linkBandwidthECHandler.getType(false), linkBandwidthECHandler.getSubType(),
                linkBandwidthECHandler));
        regs.add(context.registerExtendedCommunitySerializer(LinkBandwidthCase.class, linkBandwidthECHandler));

        final Generic4OctASEcHandler gen4OctASEcHandler = new Generic4OctASEcHandler();
        regs.add(context.registerExtendedCommunityParser(gen4OctASEcHandler.getType(true), gen4OctASEcHandler.getSubType(), gen4OctASEcHandler));
        regs.add(context.registerExtendedCommunityParser(gen4OctASEcHandler.getType(false), gen4OctASEcHandler.getSubType(), gen4OctASEcHandler));
        regs.add(context.registerExtendedCommunitySerializer(As4GenericSpecExtendedCommunityCase.class, gen4OctASEcHandler));

        final RouteTarget4OctectASEcHandler rt4ASHandler = new RouteTarget4OctectASEcHandler();
        regs.add(context.registerExtendedCommunityParser(rt4ASHandler.getType(true), rt4ASHandler.getSubType(), rt4ASHandler));
        regs.add(context.registerExtendedCommunitySerializer(As4RouteTargetExtendedCommunityCase.class, rt4ASHandler));

        final RouteOrigin4OctectASEcHandler rOrig4Oct = new RouteOrigin4OctectASEcHandler();
        regs.add(context.registerExtendedCommunityParser(rOrig4Oct.getType(true), rOrig4Oct.getSubType(), rOrig4Oct));
        regs.add(context.registerExtendedCommunitySerializer(As4RouteOriginExtendedCommunityCase.class, rOrig4Oct));

        final EncapsulationEC encapsulationECHandler = new EncapsulationEC();
        regs.add(context.registerExtendedCommunityParser(encapsulationECHandler.getType(true), encapsulationECHandler.getSubType(),
                encapsulationECHandler));
        regs.add(context.registerExtendedCommunitySerializer(EncapsulationCase.class, encapsulationECHandler));
    }
}
