/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.parser.message;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AigpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AtomicAggregateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.CommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.aigp.AigpTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.update.message.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.InitiationMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.MirrorInformationCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.PeerDownNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.PeerUpNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.Reason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.RouteMirroringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.RouteMirroringMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.RouteMonitoringMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.StatsReportsMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.TerminationMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.description.tlv.DescriptionTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.initiation.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.mirror.information.tlv.MirrorInformationTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.name.tlv.NameTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.peer.down.data.FsmEventCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.peer.down.data.NotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.peer.header.PeerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.peer.up.InformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.peer.up.ReceivedOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.peer.up.SentOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.peer.up.SentOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.reason.tlv.ReasonTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.route.monitoring.message.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.route.monitoring.message.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.AdjRibsInRoutesTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.DuplicatePrefixAdvertisementsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.DuplicateUpdatesTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.DuplicateWithdrawsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.InvalidatedAsConfedLoopTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.InvalidatedAsPathLoopTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.InvalidatedClusterListLoopTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.InvalidatedOriginatorIdTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.LocRibRoutesTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.PerAfiSafiAdjRibInTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.PerAfiSafiLocRibTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.PrefixesTreatedAsWithdrawTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.RejectedPrefixesTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat.tlvs.UpdatesTreatedAsWithdrawTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.string.informations.StringInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.string.informations.StringInformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.string.tlv.StringTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.AccumulatedIgpMetric;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class TestUtil {
    public static final Ipv4Address IPV4_ADDRESS_10 = new Ipv4Address("10.10.10.10");
    private static final Ipv4Address IPV4_ADDRESS_20 = new Ipv4Address("20.20.20.20");
    private static final Ipv4Address IPV4_ADDRESS_30 = new Ipv4Address("30.30.30.30");
    private static final Ipv4Address IPV4_ADDRESS_40 = new Ipv4Address("40.40.40.40");
    private static final Ipv4Address IPV4_ADDRESS_12 = new Ipv4Address("12.12.12.12");
    private static final Ipv4Address IPV4_ADDRESS_100 = new Ipv4Address("100.100.100.100");
    public static final AsNumber PEER_AS = new AsNumber(Uint32.valueOf(72L));
    public static final PortNumber PEER_LOCAL_PORT = new PortNumber(Uint16.valueOf(220));
    public static final PortNumber PEER_REMOTE_PORT = new PortNumber(Uint16.valueOf(5000));

    private TestUtil() {
        // Hidden on purpose
    }

    public static InitiationMessage createInitMsg(final String sysDescr, final String sysName, final String info) {
        final InitiationMessageBuilder initMsgBuilder = new InitiationMessageBuilder();
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        tlvsBuilder.setDescriptionTlv(new DescriptionTlvBuilder().setDescription(sysDescr).build());
        tlvsBuilder.setNameTlv(new NameTlvBuilder().setName(sysName).build());
        tlvsBuilder.setStringInformation(Lists.newArrayList(createStringInfo(info)));
        return initMsgBuilder.setTlvs(tlvsBuilder.build()).build();
    }

    private static StringInformation createStringInfo(final String string) {
        return new StringInformationBuilder()
                .setStringTlv(new StringTlvBuilder().setStringInfo(string).build()).build();
    }

    public static TerminationMessage createTerminationMsg() {
        final TerminationMessageBuilder terminatMsgBuilder = new TerminationMessageBuilder();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.termination
                .TlvsBuilder tlvsBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .bmp.message.rev180329.termination.TlvsBuilder();
        tlvsBuilder.setReasonTlv(new ReasonTlvBuilder().setReason(Reason.AdministrativelyClosed).build());
        tlvsBuilder.setStringInformation(Lists.newArrayList(createStringInfo("error1"), createStringInfo("error1")));
        return terminatMsgBuilder.setTlvs(tlvsBuilder.build()).build();
    }

    private static PeerHeader createPeerHeader(final Ipv4Address bgpId, final AdjRibInType ribType) {
        final PeerHeaderBuilder peerHeaderBuilder = new PeerHeaderBuilder()
            .setAddress(new IpAddress(IPV4_ADDRESS_10))
            .setAs(PEER_AS)
            .setBgpId(new Ipv4Address(bgpId))
            .setAdjRibInType(ribType)
            .setTimestampMicro(new Timestamp(Uint32.TEN))
            .setTimestampSec(new Timestamp(Uint32.valueOf(5)))
            .setIpv4(true)
            .setType(PeerType.forValue(0));
        return peerHeaderBuilder.build();
    }

    private static PeerHeader createPeerHeader(final Ipv4Address bgpId) {
        return createPeerHeader(bgpId, AdjRibInType.PrePolicy);
    }

    private static PeerHeader createPeerHeader() {
        return createPeerHeader(IPV4_ADDRESS_10);
    }

    public static PeerUpNotification createPeerUpNotification(final Ipv4Address bgpId, final boolean multiprotocol) {
        final PeerUpNotificationBuilder peerUpNotifBuilder = new PeerUpNotificationBuilder()
            .setLocalAddress(new IpAddress(IPV4_ADDRESS_10))
            .setLocalPort(PEER_LOCAL_PORT)
            .setPeerHeader(createPeerHeader(bgpId))
            .setReceivedOpen(new ReceivedOpenBuilder(createOpen(multiprotocol)).build())
            .setRemotePort(PEER_REMOTE_PORT)
            .setSentOpen((SentOpen) createOpen(multiprotocol))
            .setInformation(new InformationBuilder().setStringInformation(
                ImmutableList.<StringInformation>builder().add(
                    new StringInformationBuilder().setStringTlv(
                        new StringTlvBuilder().setStringInfo("aaaa")
            .build()).build()).build()).build());

        return peerUpNotifBuilder.build();
    }

    static PeerUpNotification createPeerUpNotification() {
        return createPeerUpNotification(IPV4_ADDRESS_10, false);
    }

    public static PeerDownNotification createPeerDownFSM() {
        final PeerDownNotificationBuilder peerDownNotifBuilder = new PeerDownNotificationBuilder()
            .setData(new FsmEventCodeBuilder().setFsmEventCode(Uint16.valueOf(24)).build())
            .setLocalSystemClosed(true)
            .setPeerHeader(TestUtil.createPeerHeader());
        return peerDownNotifBuilder.build();
    }

    public static PeerDownNotification createPeerDownNotification(final Ipv4Address bgpId) {
        final NotificationBuilder notifBuilder = new NotificationBuilder()
            .setNotification(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message
                    .rev180329.peer.down.data.notification.NotificationBuilder()
            .setErrorCode(Uint8.ONE)
            .setErrorSubcode(Uint8.ONE).build());
        final PeerDownNotificationBuilder peerDownNotifBuilder = new PeerDownNotificationBuilder()
            .setData(notifBuilder.build())
            .setLocalSystemClosed(true)
            .setPeerHeader(TestUtil.createPeerHeader(bgpId));
        return peerDownNotifBuilder.build();
    }

    static PeerDownNotification createPeerDownNotification() {
        return createPeerDownNotification(IPV4_ADDRESS_10);
    }

    private static List<BgpParameters> createBgpParameters(final boolean multiprotocol) {
        final BgpParametersBuilder bgpParamBuilder = new BgpParametersBuilder()
            .setOptionalCapabilities(createOptionalCapabilities(multiprotocol));
        final List<BgpParameters> bgpParameters = Lists.newArrayList();
        bgpParameters.add(bgpParamBuilder.build());

        return bgpParameters;
    }

    private static List<OptionalCapabilities> createOptionalCapabilities(final boolean multiprotocol) {
        final OptionalCapabilitiesBuilder optCapabilitiesBuilder = new OptionalCapabilitiesBuilder()
            .setCParameters(new CParametersBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder()
                    .setAsNumber(new AsNumber(Uint32.valueOf(70))).build()).build());
        final CParametersBuilder paramsBuilder = new CParametersBuilder();
        if (multiprotocol) {
            final CParameters1Builder params1Builder = new CParameters1Builder();
            params1Builder.setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                .setAfi(Ipv4AddressFamily.class)
                .setSafi(UnicastSubsequentAddressFamily.class).build());
            paramsBuilder.addAugmentation(CParameters1.class, params1Builder.build());
        }
        final OptionalCapabilitiesBuilder optCapabilitiesBuilder2 = new OptionalCapabilitiesBuilder()
            .setCParameters(paramsBuilder
                    .setAs4BytesCapability(new As4BytesCapabilityBuilder()
                            .setAsNumber(new AsNumber(Uint32.valueOf(80))).build()).build());
        final List<OptionalCapabilities> optCapabilities = Lists.newArrayList();
        optCapabilities.add(optCapabilitiesBuilder.build());
        optCapabilities.add(optCapabilitiesBuilder2.build());

        return optCapabilities;
    }

    private static OpenMessage createOpen(final boolean mutiprotocol) {
        final SentOpenBuilder sentOpenBuilder = new SentOpenBuilder()
            .setBgpIdentifier(new Ipv4Address(IPV4_ADDRESS_20))
            .setHoldTimer(Uint16.valueOf(1000))
            .setMyAsNumber(Uint16.valueOf(72))
            .setBgpParameters(createBgpParameters(mutiprotocol));

        return sentOpenBuilder.build();
    }

    public static RouteMonitoringMessage createRouteMonitMsg(final boolean withNormalizedIpv4Prefixes) {
        return createRouteMonitMsg(withNormalizedIpv4Prefixes, IPV4_ADDRESS_10, AdjRibInType.PrePolicy);
    }

    public static RouteMonitoringMessage createRouteMonitMsg(final boolean withNormalizedIpv4Prefixes,
            final Ipv4Address bgpId, final AdjRibInType ribType) {
        final RouteMonitoringMessageBuilder routeMonitMsgBuilder = new RouteMonitoringMessageBuilder()
            .setPeerHeader(createPeerHeader(bgpId, ribType))
            .setUpdate(createUpdate(withNormalizedIpv4Prefixes));
        return routeMonitMsgBuilder.build();
    }

    public static RouteMirroringMessage createRouteMirrorMsg(final Ipv4Address bgpId) {
        final RouteMirroringMessageBuilder routeMirrorMsgBuilder = new RouteMirroringMessageBuilder()
            .setPeerHeader(createPeerHeader(bgpId));
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.mirror
                .TlvsBuilder tlvsBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .bmp.message.rev180329.mirror.TlvsBuilder();
        tlvsBuilder.setMirrorInformationTlv(new MirrorInformationTlvBuilder()
                .setCode(MirrorInformationCode.forValue(1)).build());
        routeMirrorMsgBuilder.setTlvs(tlvsBuilder.build());
        return routeMirrorMsgBuilder.build();
    }

    private static Update createUpdate(final boolean withNormalizedIpv4Prefixes) {
        final UpdateBuilder updateBuilder = new UpdateBuilder()
            .setAttributes(createAttributes())
            .setWithdrawnRoutes(createWithdrawnRoutes());
        if (withNormalizedIpv4Prefixes) {
            updateBuilder.setNlri(createNlriWitNormalizedIpv4Prefixes());
        } else {
            updateBuilder.setNlri(createNlri());
        }

        return updateBuilder.build();
    }

    private static Attributes createAttributes() {
        final List<AsNumber> asSequences = Lists.newArrayList(new AsNumber(Uint32.valueOf(72)),
            new AsNumber(Uint32.valueOf(82)), new AsNumber(Uint32.valueOf(92)));
        final List<Segments> segments = Lists.newArrayList();
        final SegmentsBuilder segmentsBuild = new SegmentsBuilder();
        segmentsBuild.setAsSequence(asSequences).build();

        final AttributesBuilder attribBuilder = new AttributesBuilder()
            .setAggregator(new AggregatorBuilder().setAsNumber(new AsNumber(Uint32.valueOf(72)))
                    .setNetworkAddress(new Ipv4Address(IPV4_ADDRESS_20)).build())
            .setAigp(new AigpBuilder().setAigpTlv(new AigpTlvBuilder()
                    .setMetric(new AccumulatedIgpMetric(Uint64.ONE)).build()).build())
            .setAsPath(new AsPathBuilder().setSegments(segments).build())
            .setAtomicAggregate(new AtomicAggregateBuilder().build())
            .setClusterId(new ClusterIdBuilder().setCluster(Lists.newArrayList(new ClusterIdentifier(IPV4_ADDRESS_30),
                    new ClusterIdentifier(IPV4_ADDRESS_40))).build())
            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(
                    IPV4_ADDRESS_100).build()).build())
            .setCommunities(createCommunities())
            .setLocalPref(new LocalPrefBuilder().setPref(Uint32.valueOf(2)).build())
            .setMultiExitDisc(new MultiExitDiscBuilder().setMed(Uint32.valueOf(123)).build())
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setOriginatorId(new OriginatorIdBuilder().setOriginator(IPV4_ADDRESS_12).build())
            .setUnrecognizedAttributes(new ArrayList<>());
        return attribBuilder.build();
    }

    private static List<Communities> createCommunities() {
        final List<Communities> communities = Lists.newArrayList();
        final CommunitiesBuilder commBuilder = new CommunitiesBuilder()
            .setAsNumber(new AsNumber(Uint32.valueOf(65535)))
            .setSemantics(Uint16.valueOf(65381));
        final CommunitiesBuilder commBuilder2 = new CommunitiesBuilder()
            .setAsNumber(new AsNumber(Uint32.valueOf(65535)))
            .setSemantics(Uint16.valueOf(65382));
        communities.add(commBuilder.build());
        communities.add(commBuilder2.build());
        return communities;
    }

    private static List<Nlri> createNlri() {
        final Nlri n1 = new NlriBuilder().setPrefix(new Ipv4Prefix("10.10.10.10/24")).build();
        final Nlri n2 = new NlriBuilder().setPrefix(new Ipv4Prefix("20.20.20.20/24")).build();
        final Nlri n3 = new NlriBuilder().setPrefix(new Ipv4Prefix("30.30.30.30/24")).build();
        return Lists.newArrayList(n1, n2, n3);
    }

    private static List<Nlri> createNlriWitNormalizedIpv4Prefixes() {
        final Nlri n1 = new NlriBuilder().setPrefix(new Ipv4Prefix("10.10.10.0/24")).build();
        final Nlri n2 = new NlriBuilder().setPrefix(new Ipv4Prefix("20.20.20.0/24")).build();
        final Nlri n3 = new NlriBuilder().setPrefix(new Ipv4Prefix("30.30.30.0/24")).build();
        return Lists.newArrayList(n1, n2, n3);
    }

    private static List<WithdrawnRoutes> createWithdrawnRoutes() {
        final WithdrawnRoutes w1 = new WithdrawnRoutesBuilder()
                .setPrefix(new Ipv4Prefix("10.10.20.0/24")).build();
        final WithdrawnRoutes w2 = new WithdrawnRoutesBuilder()
                .setPrefix(new Ipv4Prefix("20.20.10.0/24")).build();
        final WithdrawnRoutes w3 = new WithdrawnRoutesBuilder()
                .setPrefix(new Ipv4Prefix("30.10.10.0/24")).build();
        return Lists.newArrayList(w1, w2, w3);
    }

    public static StatsReportsMessage createStatsReportMsg(final Ipv4Address bgpId) {
        final StatsReportsMessageBuilder statsReportMsgBuilder = new StatsReportsMessageBuilder();
        statsReportMsgBuilder.setPeerHeader(TestUtil.createPeerHeader(bgpId));
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat
                .TlvsBuilder tlvsBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev180329.stat
                        .TlvsBuilder();
        tlvsBuilder.setAdjRibsInRoutesTlv(new AdjRibsInRoutesTlvBuilder()
                .setCount(new Gauge64(Uint64.TEN)).build());
        tlvsBuilder.setDuplicatePrefixAdvertisementsTlv(new DuplicatePrefixAdvertisementsTlvBuilder()
                .setCount(new Counter32(Uint32.valueOf(16L))).build());
        tlvsBuilder.setDuplicateWithdrawsTlv(new DuplicateWithdrawsTlvBuilder()
                .setCount(new Counter32(Uint32.valueOf(11L))).build());
        tlvsBuilder.setInvalidatedAsConfedLoopTlv(new InvalidatedAsConfedLoopTlvBuilder()
                .setCount(new Counter32(Uint32.valueOf(55L))).build());
        tlvsBuilder.setInvalidatedAsPathLoopTlv(new InvalidatedAsPathLoopTlvBuilder()
                .setCount(new Counter32(Uint32.valueOf(66L))).build());
        tlvsBuilder.setInvalidatedClusterListLoopTlv(new InvalidatedClusterListLoopTlvBuilder()
                .setCount(new Counter32(Uint32.valueOf(53L))).build());
        tlvsBuilder.setInvalidatedOriginatorIdTlv(new InvalidatedOriginatorIdTlvBuilder()
                .setCount(new Counter32(Uint32.valueOf(70L))).build());
        tlvsBuilder.setLocRibRoutesTlv(new LocRibRoutesTlvBuilder()
                .setCount(new Gauge64(Uint64.valueOf(100L))).build());
        tlvsBuilder.setRejectedPrefixesTlv(new RejectedPrefixesTlvBuilder()
                .setCount(new Counter32(Uint32.valueOf(8L))).build());
        tlvsBuilder.setPerAfiSafiAdjRibInTlv(new PerAfiSafiAdjRibInTlvBuilder()
                .setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
                .setCount(new Gauge64(Uint64.valueOf(9L))).build());
        tlvsBuilder.setPerAfiSafiLocRibTlv(new PerAfiSafiLocRibTlvBuilder()
                .setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
                .setCount(new Gauge64(Uint64.TEN)).build());
        tlvsBuilder.setUpdatesTreatedAsWithdrawTlv(new UpdatesTreatedAsWithdrawTlvBuilder()
                .setCount(new Counter32(Uint32.valueOf(11L))).build());
        tlvsBuilder.setPrefixesTreatedAsWithdrawTlv(new PrefixesTreatedAsWithdrawTlvBuilder()
                .setCount(new Counter32(Uint32.valueOf(12L))).build());
        tlvsBuilder.setDuplicateUpdatesTlv(new DuplicateUpdatesTlvBuilder()
                .setCount(new Counter32(Uint32.valueOf(13L))).build());
        return statsReportMsgBuilder.setTlvs(tlvsBuilder.build()).build();
    }

    static StatsReportsMessage createStatsReportMsg() {
        return createStatsReportMsg(IPV4_ADDRESS_10);
    }


    public static RouteMonitoringMessage createRouteMonMsgWithEndOfRibMarker(final Ipv4Address bgpId,
            final AdjRibInType ribType) {
        return new RouteMonitoringMessageBuilder().setPeerHeader(createPeerHeader(bgpId, ribType))
                .setUpdate(createEndOfRibMarker()).build();
    }

    private static Update createEndOfRibMarker() {
        return new UpdateBuilder().build();
    }

}
