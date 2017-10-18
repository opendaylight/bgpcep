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
import java.math.BigInteger;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AggregatorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AigpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AtomicAggregateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ClusterIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.CommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.aigp.AigpTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.Nlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.WithdrawnRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.update.message.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ClusterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.InitiationMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.MirrorInformationCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerDownNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerUpNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.Reason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.RouteMirroringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.RouteMirroringMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.RouteMonitoringMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.StatsReportsMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.StatsReportsMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.TerminationMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.description.tlv.DescriptionTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.initiation.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.mirror.information.tlv.MirrorInformationTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.name.tlv.NameTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.FsmEventCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.NotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.header.PeerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.up.InformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.up.ReceivedOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.up.SentOpen;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.up.SentOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.reason.tlv.ReasonTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.route.monitoring.message.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.route.monitoring.message.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.AdjRibsInRoutesTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.DuplicatePrefixAdvertisementsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.DuplicateUpdatesTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.DuplicateWithdrawsTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedAsConfedLoopTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedAsPathLoopTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedClusterListLoopTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.InvalidatedOriginatorIdTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.LocRibRoutesTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.PerAfiSafiAdjRibInTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.PerAfiSafiLocRibTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.PrefixesTreatedAsWithdrawTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.RejectedPrefixesTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.tlvs.UpdatesTreatedAsWithdrawTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.informations.StringInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.informations.StringInformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.tlv.StringTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.AccumulatedIgpMetric;

public final class TestUtil {

    private TestUtil() {
        throw new UnsupportedOperationException();
    }

    public static final Ipv4Address IPV4_ADDRESS_10 = new Ipv4Address("10.10.10.10");
    public static final Ipv4Address IPV4_ADDRESS_20 = new Ipv4Address("20.20.20.20");
    public static final Ipv4Address IPV4_ADDRESS_30 = new Ipv4Address("30.30.30.30");
    public static final Ipv4Address IPV4_ADDRESS_40 = new Ipv4Address("40.40.40.40");
    public static final Ipv4Address IPV4_ADDRESS_12 = new Ipv4Address("12.12.12.12");
    public static final Ipv4Address IPV4_ADDRESS_100 = new Ipv4Address("100.100.100.100");
    public static final AsNumber PEER_AS = new AsNumber(72L);
    public static final PortNumber PEER_LOCAL_PORT = new PortNumber(220);
    public static final PortNumber PEER_REMOTE_PORT = new PortNumber(5000);

    public static InitiationMessage createInitMsg(final String sysDescr, final String sysName, final String info) {
        final InitiationMessageBuilder initMsgBuilder = new InitiationMessageBuilder();
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        tlvsBuilder.setDescriptionTlv(new DescriptionTlvBuilder().setDescription(sysDescr).build());
        tlvsBuilder.setNameTlv(new NameTlvBuilder().setName(sysName).build());
        tlvsBuilder.setStringInformation(Lists.newArrayList(createStringInfo(info)));
        return initMsgBuilder.setTlvs(tlvsBuilder.build()).build();
    }

    public static StringInformation createStringInfo(final String string) {
        return new StringInformationBuilder().setStringTlv(new StringTlvBuilder().setStringInfo(string).build()).build();
    }

    public static TerminationMessage createTerminationMsg() {
        final TerminationMessageBuilder terminatMsgBuilder = new TerminationMessageBuilder();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.termination.TlvsBuilder tlvsBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.termination.TlvsBuilder();
        tlvsBuilder.setReasonTlv(new ReasonTlvBuilder().setReason(Reason.AdministrativelyClosed).build());
        tlvsBuilder.setStringInformation(Lists.newArrayList(createStringInfo("error1"), createStringInfo("error1")));
        return terminatMsgBuilder.setTlvs(tlvsBuilder.build()).build();
    }

    public static PeerHeader createPeerHeader(final Ipv4Address bgpId, final AdjRibInType ribType) {
        final PeerHeaderBuilder peerHeaderBuilder = new PeerHeaderBuilder()
            .setAddress(new IpAddress(IPV4_ADDRESS_10))
            .setAs(PEER_AS)
            .setBgpId(new Ipv4Address(bgpId))
            .setAdjRibInType(ribType)
            .setTimestampMicro(new Timestamp(10L))
            .setTimestampSec(new Timestamp(5L))
            .setIpv4(true)
            .setType(PeerType.forValue(0));
        return peerHeaderBuilder.build();
    }

    public static PeerHeader createPeerHeader(final Ipv4Address bgpId) {
        return createPeerHeader(bgpId, AdjRibInType.PrePolicy);
    }

    public static PeerHeader createPeerHeader() {
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

    public static PeerUpNotification createPeerUpNotification() {
        return createPeerUpNotification(IPV4_ADDRESS_10, false);
    }

    public static PeerDownNotification createPeerDownFSM() {
        final PeerDownNotificationBuilder peerDownNotifBuilder = new PeerDownNotificationBuilder()
            .setData(new FsmEventCodeBuilder().setFsmEventCode(24).build())
            .setLocalSystemClosed(true)
            .setPeerHeader(TestUtil.createPeerHeader());
        return peerDownNotifBuilder.build();
    }

    public static PeerDownNotification createPeerDownNotification(final Ipv4Address bgpId) {
        final NotificationBuilder notifBuilder = new NotificationBuilder()
            .setNotification(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.notification.NotificationBuilder()
            .setErrorCode((short) 1)
            .setErrorSubcode((short) 1).build());
        final PeerDownNotificationBuilder peerDownNotifBuilder = new PeerDownNotificationBuilder()
            .setData(notifBuilder.build())
            .setLocalSystemClosed(true)
            .setPeerHeader(TestUtil.createPeerHeader(bgpId));
        return peerDownNotifBuilder.build();
    }

    public static PeerDownNotification createPeerDownNotification() {
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
            .setCParameters(new CParametersBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(70L)).build()).build());
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
                    .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(80L)).build()).build());
        final List<OptionalCapabilities> optCapabilities = Lists.newArrayList();
        optCapabilities.add(optCapabilitiesBuilder.build());
        optCapabilities.add(optCapabilitiesBuilder2.build());

        return optCapabilities;
    }

    private static OpenMessage createOpen(final boolean mutiprotocol) {
        final SentOpenBuilder sentOpenBuilder = new SentOpenBuilder()
            .setBgpIdentifier(new Ipv4Address(IPV4_ADDRESS_20))
            .setHoldTimer(1000)
            .setMyAsNumber(72)
            .setBgpParameters(createBgpParameters(mutiprotocol));

        return sentOpenBuilder.build();
    }

    public static RouteMonitoringMessage createRouteMonitMsg(final boolean withNormalizedIpv4Prefixes) {
        return createRouteMonitMsg(withNormalizedIpv4Prefixes, IPV4_ADDRESS_10, AdjRibInType.PrePolicy);
    }

    public static RouteMonitoringMessage createRouteMonitMsg(final boolean withNormalizedIpv4Prefixes, final Ipv4Address bgpId, final AdjRibInType ribType) {
        final RouteMonitoringMessageBuilder routeMonitMsgBuilder = new RouteMonitoringMessageBuilder()
            .setPeerHeader(createPeerHeader(bgpId, ribType))
            .setUpdate(createUpdate(withNormalizedIpv4Prefixes));
        return routeMonitMsgBuilder.build();
    }

    public static RouteMirroringMessage createRouteMirrorMsg(final Ipv4Address bgpId) {
        final RouteMirroringMessageBuilder routeMirrorMsgBuilder = new RouteMirroringMessageBuilder()
            .setPeerHeader(createPeerHeader(bgpId));
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.mirror.TlvsBuilder tlvsBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.mirror.TlvsBuilder();
        tlvsBuilder.setMirrorInformationTlv(new MirrorInformationTlvBuilder().setCode(MirrorInformationCode.forValue(1)).build());
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
        final List<AsNumber> asSequences = Lists.newArrayList(new AsNumber(72L), new AsNumber(82L), new AsNumber(92L));
        final List<Segments> segments = Lists.newArrayList();
        final SegmentsBuilder segmentsBuild = new SegmentsBuilder();
        segmentsBuild.setAsSequence(asSequences).build();

        final AttributesBuilder attribBuilder = new AttributesBuilder()
            .setAggregator(new AggregatorBuilder().setAsNumber(new AsNumber(72L)).setNetworkAddress(new Ipv4Address(IPV4_ADDRESS_20)).build())
            .setAigp(new AigpBuilder().setAigpTlv(new AigpTlvBuilder().setMetric(new AccumulatedIgpMetric(BigInteger.ONE)).build()).build())
            .setAsPath(new AsPathBuilder().setSegments(segments).build())
            .setAtomicAggregate(new AtomicAggregateBuilder().build())
            .setClusterId(new ClusterIdBuilder().setCluster(Lists.newArrayList(new ClusterIdentifier(IPV4_ADDRESS_30),
                    new ClusterIdentifier(IPV4_ADDRESS_40))).build())
            .setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(
                    IPV4_ADDRESS_100).build()).build())
            .setCommunities(createCommunities())
            .setLocalPref(new LocalPrefBuilder().setPref(2L).build())
            .setMultiExitDisc(new MultiExitDiscBuilder().setMed(123L).build())
            .setOrigin(new OriginBuilder().setValue(BgpOrigin.Igp).build())
            .setOriginatorId(new OriginatorIdBuilder().setOriginator(IPV4_ADDRESS_12).build())
            .setUnrecognizedAttributes(new ArrayList<>());
        return attribBuilder.build();
    }

    private static List<Communities> createCommunities() {
        final List<Communities> communities = Lists.newArrayList();
        final CommunitiesBuilder commBuilder = new CommunitiesBuilder()
            .setAsNumber(new AsNumber(65535L))
            .setSemantics(65381);
        final CommunitiesBuilder commBuilder2 = new CommunitiesBuilder()
            .setAsNumber(new AsNumber(65535L))
            .setSemantics(65382);
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
        final WithdrawnRoutes w1 = new WithdrawnRoutesBuilder().setPrefix(new Ipv4Prefix("10.10.20.0/24")).build();
        final WithdrawnRoutes w2 = new WithdrawnRoutesBuilder().setPrefix(new Ipv4Prefix("20.20.10.0/24")).build();
        final WithdrawnRoutes w3 = new WithdrawnRoutesBuilder().setPrefix(new Ipv4Prefix("30.10.10.0/24")).build();
        return Lists.newArrayList(w1, w2, w3);
    }

    public static StatsReportsMessage createStatsReportMsg(final Ipv4Address bgpId) {
        final StatsReportsMessageBuilder statsReportMsgBuilder = new StatsReportsMessageBuilder();
        statsReportMsgBuilder.setPeerHeader(TestUtil.createPeerHeader(bgpId));
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.TlvsBuilder tlvsBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.stat.TlvsBuilder();
        tlvsBuilder.setAdjRibsInRoutesTlv(new AdjRibsInRoutesTlvBuilder().setCount(new Gauge64(BigInteger.valueOf(10L))).build());
        tlvsBuilder.setDuplicatePrefixAdvertisementsTlv(new DuplicatePrefixAdvertisementsTlvBuilder().setCount(new Counter32(16L)).build());
        tlvsBuilder.setDuplicateWithdrawsTlv(new DuplicateWithdrawsTlvBuilder().setCount(new Counter32(11L)).build());
        tlvsBuilder.setInvalidatedAsConfedLoopTlv(new InvalidatedAsConfedLoopTlvBuilder().setCount(new Counter32(55L)).build());
        tlvsBuilder.setInvalidatedAsPathLoopTlv(new InvalidatedAsPathLoopTlvBuilder().setCount(new Counter32(66L)).build());
        tlvsBuilder.setInvalidatedClusterListLoopTlv(new InvalidatedClusterListLoopTlvBuilder().setCount(new Counter32(53L)).build());
        tlvsBuilder.setInvalidatedOriginatorIdTlv(new InvalidatedOriginatorIdTlvBuilder().setCount(new Counter32(70L)).build());
        tlvsBuilder.setLocRibRoutesTlv(new LocRibRoutesTlvBuilder().setCount(new Gauge64(BigInteger.valueOf(100L))).build());
        tlvsBuilder.setRejectedPrefixesTlv(new RejectedPrefixesTlvBuilder().setCount(new Counter32(8L)).build());
        tlvsBuilder.setPerAfiSafiAdjRibInTlv(new PerAfiSafiAdjRibInTlvBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setCount(new Gauge64(BigInteger.valueOf(9L))).build());
        tlvsBuilder.setPerAfiSafiLocRibTlv(new PerAfiSafiLocRibTlvBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setCount(new Gauge64(BigInteger.valueOf(10L))).build());
        tlvsBuilder.setUpdatesTreatedAsWithdrawTlv(new UpdatesTreatedAsWithdrawTlvBuilder().setCount(new Counter32(11L)).build());
        tlvsBuilder.setPrefixesTreatedAsWithdrawTlv(new PrefixesTreatedAsWithdrawTlvBuilder().setCount(new Counter32(12L)).build());
        tlvsBuilder.setDuplicateUpdatesTlv(new DuplicateUpdatesTlvBuilder().setCount(new Counter32(13L)).build());
        return statsReportMsgBuilder.setTlvs(tlvsBuilder.build()).build();
    }

    public static StatsReportsMessage createStatsReportMsg() {
        return createStatsReportMsg(IPV4_ADDRESS_10);
    }


    public static RouteMonitoringMessage createRouteMonMsgWithEndOfRibMarker(final Ipv4Address bgpId, final AdjRibInType ribType) {
        return new RouteMonitoringMessageBuilder().setPeerHeader(createPeerHeader(bgpId, ribType)).setUpdate(createEndOfRibMarker()).build();
    }

    private static Update createEndOfRibMarker() {
        return new UpdateBuilder().build();
    }

}
