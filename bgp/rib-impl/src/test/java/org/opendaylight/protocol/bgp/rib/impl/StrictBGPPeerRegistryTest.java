/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.ReusableBGPPeer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;

public class StrictBGPPeerRegistryTest {

    private static final AsNumber LOCAL_AS = new AsNumber(1234L);
    private static final AsNumber REMOTE_AS = new AsNumber(1235L);
    private static final Ipv4Address FROM = new Ipv4Address("0.0.0.1");
    private static final IpAddress REMOTE_IP = new IpAddress(FROM);
    private static final Ipv4Address TO = new Ipv4Address("255.255.255.255");
    private static final Open OPEN = new OpenBuilder().setMyAsNumber(1235).setHoldTimer(1).setVersion(
            new ProtocolVersion((short) 4)).setBgpIdentifier(FROM).build();

    private final ReusableBGPPeer peer1 = getMockSession();
    private StrictBGPPeerRegistry peerRegistry;
    private BGPSessionPreferences mockPreferences;

    @Before
    public void setUp() throws Exception {
        this.peerRegistry = new StrictBGPPeerRegistry();
        this.peerRegistry.addRib(TO, LOCAL_AS);
        this.mockPreferences = new BGPSessionPreferences(LOCAL_AS, 1,  FROM, Collections.<BgpParameters>emptyList(), REMOTE_AS);
    }

    private static ReusableBGPPeer getMockSession() {
        final ReusableBGPPeer mock = Mockito.mock(ReusableBGPPeer.class);
        Mockito.doNothing().when(mock).releaseConnection();
        return mock;
    }

    @Test
    public void testPeerAlreadyPresent() {
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        try {
            this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);
        } catch (final IllegalArgumentException e) {
            assertEquals("Peer for " + REMOTE_IP + " already present", e.getMessage());
            return;
        }
        fail("Adding same peer twice must be refused.");
    }

    @Test
    public void testIpAddressConstruction() throws Exception {
        final InetSocketAddress v4address= new InetSocketAddress("127.0.0.1", 179);
        final IpAddress ipv4Adr = StrictBGPPeerRegistry.getIpAddress(v4address);
        assertEquals("127.0.0.1", ipv4Adr.getIpv4Address().getValue());

        final InetSocketAddress v6adr = new InetSocketAddress("2001::01", 179);
        final IpAddress ipv6Adr = StrictBGPPeerRegistry.getIpAddress(v6adr);
        assertEquals("2001:0:0:0:0:0:0:1", ipv6Adr.getIpv6Address().getValue());
    }

    @Test
    public void testDuplicatePeerConnection() throws Exception {
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(REMOTE_IP, OPEN, true);
        try {
            this.peerRegistry.getPeer(REMOTE_IP, OPEN, true);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testPeerNotConfigured() throws Exception {
        try {
            this.peerRegistry.getPeer(REMOTE_IP, OPEN, true);
        } catch (final IllegalStateException e) {
            return;
        }
        fail("Unknown peer cannot be connected");
    }

    @Test
    public void testPeerConnectionSuccessful() throws Exception {
        final Ipv4Address to2 = new Ipv4Address("255.255.255.254");
        final IpAddress remoteIp2 = new IpAddress(to2);

        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);
        final ReusableBGPPeer peer2 = getMockSession();
        this.peerRegistry.addPeer(remoteIp2, peer2, this.mockPreferences);

        final BGPSessionListener returnedSession1 = this.peerRegistry.getPeer(REMOTE_IP, OPEN, true);
        assertSame(this.peer1, returnedSession1);
        final BGPSessionListener returnedSession2 = this.peerRegistry.getPeer(remoteIp2, OPEN, true);
        assertSame(peer2, returnedSession2);

        Mockito.verifyZeroInteractions(this.peer1);
        Mockito.verifyZeroInteractions(peer2);
    }

    @Test
    public void testDropFirstPeer() throws Exception {
        final Ipv4Address higher = new Ipv4Address("123.123.123.123");
        final Ipv4Address lower = new Ipv4Address("123.123.123.122");
        final IpAddress remoteIp = new IpAddress(lower);
        final Open open_h = new OpenBuilder().setMyAsNumber(1235).setHoldTimer(1).setVersion(
                new ProtocolVersion((short) 4)).setBgpIdentifier(higher).build();
        final Open open_l = new OpenBuilder().setMyAsNumber(1235).setHoldTimer(1).setVersion(
                new ProtocolVersion((short) 4)).setBgpIdentifier(lower).build();

        this.peerRegistry.addPeer(remoteIp, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(remoteIp, open_l, true);
        this.peerRegistry.getPeer(remoteIp, open_h, true);
        Mockito.verify(this.peer1).releaseConnection();
    }

    @Test
    public void testDropSecondPeer() throws Exception {
        final Ipv4Address higher = new Ipv4Address("192.168.200.200");
        final Ipv4Address lower = new Ipv4Address("10.10.10.10");
        final IpAddress remoteIp = new IpAddress(lower);
        final Open open_h = new OpenBuilder().setMyAsNumber(1235).setHoldTimer(1).setVersion(
                new ProtocolVersion((short) 4)).setBgpIdentifier(higher).build();
        final Open open_l = new OpenBuilder().setMyAsNumber(1235).setHoldTimer(1).setVersion(
                new ProtocolVersion((short) 4)).setBgpIdentifier(lower).build();

        this.peerRegistry.addPeer(remoteIp, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(remoteIp, open_h, true);
        try {
            this.peerRegistry.getPeer(remoteIp, open_l, true);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testDuplicatePeersWDifferentIds() throws Exception {
        final Open open = new OpenBuilder().setMyAsNumber(1235).setHoldTimer(1).setVersion(
                new ProtocolVersion((short) 4)).setBgpIdentifier(TO).build();
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(REMOTE_IP, OPEN, true);
        try {
            this.peerRegistry.getPeer(REMOTE_IP, open, true);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testDuplicatePeersHigherAs() throws Exception {
        final Open open = new OpenBuilder().setMyAsNumber(10000).setHoldTimer(1).setVersion(
                new ProtocolVersion((short) 4)).setBgpIdentifier(FROM).build();
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(REMOTE_IP, OPEN, true);
        this.peerRegistry.getPeer(REMOTE_IP, open, true);
        Mockito.verify(this.peer1).releaseConnection();
    }

    @Test
    public void testDuplicatePeersLowerAs() throws Exception {
        final Open open = new OpenBuilder().setMyAsNumber(3).setHoldTimer(1).setVersion(
                new ProtocolVersion((short) 4)).setBgpIdentifier(FROM).build();
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(REMOTE_IP, OPEN, true);
        try {
            this.peerRegistry.getPeer(REMOTE_IP, open, true);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testAsMismatch() throws Exception {
        final Open open = new OpenBuilder().setMyAsNumber(3).setHoldTimer(1).setVersion(
                new ProtocolVersion((short) 4)).setBgpIdentifier(FROM).build();
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        try {
            this.peerRegistry.getPeer(REMOTE_IP, open, true);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.BAD_PEER_AS, e.getError());
            return;
        }
        fail("Peer AS number mismatch");
    }
}
