/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import java.net.InetAddress;
import java.util.Collections;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMonitoringMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.initiation.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.name.tlv.NameTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeaderBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.ReceivedOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.SentOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring.message.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.route.monitoring.message.UpdateBuilder;

final class BmpMockUtil {

    private static final String DESCRIPTION = "OpenDaylight";
    private static final String NAME = "BMP mock";
    private static final int HOLD_TIMER = 180;
    private static final AsNumber ASN = new AsNumber(65431L);
    private static final Ipv4Address NEXT_HOP = new Ipv4Address("1.2.3.4");
    private static final PortNumber PEER_PORT = new PortNumber(179);
    private static final ProtocolVersion PROTOCOL_VERSION = new ProtocolVersion((short) 4);
    private static final Origin ORIGIN = new OriginBuilder().setValue(BgpOrigin.Igp).build();
    private static final AsPath AS_PATH = new AsPathBuilder().setSegments(Collections.emptyList()).build();

    private BmpMockUtil() {
        throw new UnsupportedOperationException();
    }

    public static InitiationMessage createInitiation() {
        final InitiationMessageBuilder msgBuilder = new InitiationMessageBuilder();
        msgBuilder.setTlvs(
            new TlvsBuilder()
                .setDescriptionTlv(new DescriptionTlvBuilder().setDescription(DESCRIPTION).build())
                .setNameTlv(new NameTlvBuilder().setName(NAME).build())
                .build());
        return msgBuilder.build();
    }

    public static PeerUpNotification createPeerUp(final Ipv4Address peerIp, final InetAddress localAddress) {
        final PeerUpNotificationBuilder msgBuilder = new PeerUpNotificationBuilder();
        msgBuilder.setLocalAddress(new IpAddress(new Ipv4Address(localAddress.getHostAddress())));
        msgBuilder.setLocalPort(PEER_PORT);
        msgBuilder.setRemotePort(PEER_PORT);
        msgBuilder.setReceivedOpen(new ReceivedOpenBuilder(createOpen(peerIp)).build());
        msgBuilder.setSentOpen(new SentOpenBuilder(createOpen(new Ipv4Address(localAddress.getHostAddress()))).build());
        msgBuilder.setPeerHeader(createPeerHeader(peerIp, AdjRibInType.PrePolicy));
        return msgBuilder.build();
    }

    private static OpenMessage createOpen(final Ipv4Address address) {
        final OpenBuilder msgBuilder = new OpenBuilder();
        msgBuilder.setBgpIdentifier(address);
        msgBuilder.setHoldTimer(HOLD_TIMER);
        msgBuilder.setMyAsNumber(ASN.getValue().intValue());
        msgBuilder.setVersion(PROTOCOL_VERSION);
        return msgBuilder.build();
    }

    private static PeerHeader createPeerHeader(final Ipv4Address bgpId, final AdjRibInType ribType) {
        return new PeerHeaderBuilder()
            .setAddress(new IpAddress(bgpId))
            .setAdjRibInType(ribType)
            .setAs(new AsNumber(ASN))
            .setBgpId(bgpId)
            .setIpv4(true)
            .setType(PeerType.Global)
            .build();
    }

    public static RouteMonitoringMessage createRouteMonitoring(final Ipv4Address bgpId, final AdjRibInType ribType, final Ipv4Prefix prefix) {
        final RouteMonitoringMessageBuilder routeMonitMsgBuilder = new RouteMonitoringMessageBuilder()
            .setPeerHeader(createPeerHeader(bgpId, ribType))
            .setUpdate(createUpdate(prefix));
        return routeMonitMsgBuilder.build();
    }

    private static Update createUpdate(final Ipv4Prefix prefix) {
        final UpdateBuilder updateBuilder = new UpdateBuilder()
            .setAttributes(new AttributesBuilder().setOrigin(ORIGIN).setAsPath(AS_PATH).setCNextHop(
                new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(NEXT_HOP).build()).build()).build())
            .setNlri(new NlriBuilder().setNlri(Collections.singletonList(prefix)).build());
        return updateBuilder.build();
    }
}
