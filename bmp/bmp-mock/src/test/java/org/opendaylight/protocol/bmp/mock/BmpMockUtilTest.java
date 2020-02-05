/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.RouteMonitoringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.initiation.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.route.monitoring.message.Update;

public class BmpMockUtilTest {

    private static final Ipv4AddressNoZone PEER_IP = new Ipv4AddressNoZone("127.0.0.1");
    private static final InetAddress LOCAL_ADDRESS = InetAddresses.forString("127.0.0.2");
    private static final Ipv4Prefix PREFIX = new Ipv4Prefix("1.2.3.4/32");

    @Test
    public void testCreateInitiation() {
        final InitiationMessage initiation = BmpMockUtil.createInitiation();
        final Tlvs tlvs = initiation.getTlvs();
        assertEquals("OpenDaylight", tlvs.getDescriptionTlv().getDescription());
        assertEquals("BMP mock", tlvs.getNameTlv().getName());
        assertNull(tlvs.getStringInformation());
    }

    @Test
    public void testCreatePeerUp() {
        final PeerUpNotification peerUp = BmpMockUtil.createPeerUp(PEER_IP, LOCAL_ADDRESS);
        final PeerHeader peerHeader = peerUp.getPeerHeader();
        assertEquals(PEER_IP, peerHeader.getAddress().getIpv4AddressNoZone());
        assertEquals(65431L, peerHeader.getAs().getValue().longValue());
        assertEquals(PEER_IP, peerHeader.getBgpId());
        assertEquals(PeerType.Global, peerHeader.getType());
        assertNull(peerUp.getInformation());
        assertEquals(LOCAL_ADDRESS.getHostAddress(), peerUp.getLocalAddress().getIpv4AddressNoZone().getValue());
        assertEquals(179, peerUp.getLocalPort().getValue().intValue());
        assertEquals(179, peerUp.getRemotePort().getValue().intValue());
        assertNotNull(peerUp.getReceivedOpen());
        assertNotNull(peerUp.getSentOpen());
    }

    @Test
    public void testCreateRouteMonitoringPrePolicy() {
        final RouteMonitoringMessage routeMonitoring = BmpMockUtil
                .createRouteMonitoring(PEER_IP, AdjRibInType.PrePolicy, PREFIX);
        final PeerHeader peerHeader = routeMonitoring.getPeerHeader();
        assertEquals(PEER_IP, peerHeader.getAddress().getIpv4AddressNoZone());
        assertEquals(65431L, peerHeader.getAs().getValue().longValue());
        assertEquals(PEER_IP, peerHeader.getBgpId());
        assertEquals(PeerType.Global, peerHeader.getType());
        assertEquals(AdjRibInType.PrePolicy, peerHeader.getAdjRibInType());
        final Update update = routeMonitoring.getUpdate();
        assertEquals(PREFIX, update.getNlri().get(0).getPrefix());
        assertEquals("1.2.3.4", ((Ipv4NextHopCase)update.getAttributes()
                .getCNextHop()).getIpv4NextHop().getGlobal().getValue());
    }

    @Test
    public void testCreateRouteMonitoringPostPolicy() {
        final RouteMonitoringMessage routeMonitoring = BmpMockUtil
                .createRouteMonitoring(PEER_IP, AdjRibInType.PostPolicy, PREFIX);
        assertEquals(AdjRibInType.PostPolicy, routeMonitoring.getPeerHeader().getAdjRibInType());
    }
}
