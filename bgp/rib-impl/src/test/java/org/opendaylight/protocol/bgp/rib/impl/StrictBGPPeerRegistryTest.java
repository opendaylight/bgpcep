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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;

public class StrictBGPPeerRegistryTest {

    private StrictBGPPeerRegistry droppingBGPSessionRegistry;
    private BGPSessionPreferences mockPreferences;
    private final AsNumber AS1 = new AsNumber(1234L);
    private final AsNumber AS2 = new AsNumber(1235L);
    @Before
    public void setUp() throws Exception {
        this.droppingBGPSessionRegistry = new StrictBGPPeerRegistry();
        this.mockPreferences = getMockPreferences();
    }

    @Test
    public void testIpAddressConstruction() throws Exception {
        final InetSocketAddress adr = new InetSocketAddress("127.0.0.1", 179);
        final IpAddress ipAdr = StrictBGPPeerRegistry.getIpAddress(adr);
        assertEquals("127.0.0.1", ipAdr.getIpv4Address().getValue());
    }

    @Test
    public void testDuplicate() throws Exception {
        final Ipv4Address from = new Ipv4Address("0.0.0.1");
        final IpAddress remoteIp = new IpAddress(from);
        final Ipv4Address to = new Ipv4Address("255.255.255.255");

        final ReusableBGPPeer session1 = getMockSession();
        this.droppingBGPSessionRegistry.addPeer(remoteIp, session1, this.mockPreferences);

        this.droppingBGPSessionRegistry.getPeer(remoteIp, from, to, AS1);
        try {
            this.droppingBGPSessionRegistry.getPeer(remoteIp, from, to, AS1);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }

        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testNotAllowed() throws Exception {
        final Ipv4Address from = new Ipv4Address("0.0.0.1");
        final IpAddress remoteIp = new IpAddress(from);
        final Ipv4Address to = new Ipv4Address("255.255.255.255");

        try {
            this.droppingBGPSessionRegistry.getPeer(remoteIp, from, to, AS1);
        } catch (final IllegalStateException e) {
            return;
        }
        fail("Unknown peer cannot be connected");
    }

    @Test
    public void testOk() throws Exception {
        final Ipv4Address from = new Ipv4Address("0.0.0.1");

        final Ipv4Address to = new Ipv4Address("255.255.255.255");
        final IpAddress remoteIp = new IpAddress(to);
        final Ipv4Address to2 = new Ipv4Address("255.255.255.254");
        final IpAddress remoteIp2 = new IpAddress(to2);

        final ReusableBGPPeer session1 = getMockSession();
        this.droppingBGPSessionRegistry.addPeer(remoteIp, session1, this.mockPreferences);
        final ReusableBGPPeer session2 = getMockSession();
        this.droppingBGPSessionRegistry.addPeer(remoteIp2, session2, this.mockPreferences);

        final BGPSessionListener returnedSession1 = this.droppingBGPSessionRegistry.getPeer(remoteIp, from, to, AS1);
        assertSame(session1, returnedSession1);
        final BGPSessionListener returnedSession2 = this.droppingBGPSessionRegistry.getPeer(remoteIp2, from, to2, AS1);
        assertSame(session2, returnedSession2);

        Mockito.verifyZeroInteractions(session1);
        Mockito.verifyZeroInteractions(session2);
    }

    @Test
    public void testDropSecond() throws Exception {
        final Ipv4Address higher = new Ipv4Address("192.168.200.200");
        final Ipv4Address lower = new Ipv4Address("10.10.10.10");
        final IpAddress remoteIp = new IpAddress(lower);

        final ReusableBGPPeer session1 = getMockSession();
        this.droppingBGPSessionRegistry.addPeer(remoteIp, session1, this.mockPreferences);

        this.droppingBGPSessionRegistry.getPeer(remoteIp, higher, lower, AS1);
        try {
            this.droppingBGPSessionRegistry.getPeer(remoteIp, lower, higher, AS1);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }

        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testDropFirst() throws Exception {
        final Ipv4Address higher = new Ipv4Address("123.123.123.123");
        final Ipv4Address lower = new Ipv4Address("123.123.123.122");
        final IpAddress remoteIp = new IpAddress(lower);

        final ReusableBGPPeer session1 = getMockSession();
        this.droppingBGPSessionRegistry.addPeer(remoteIp, session1, this.mockPreferences);

        this.droppingBGPSessionRegistry.getPeer(remoteIp, lower, higher, AS1);
        this.droppingBGPSessionRegistry.getPeer(remoteIp, higher, lower, AS1);
        Mockito.verify(session1).releaseConnection();
    }

    @Test
    public void testDuplicateDifferentIds() throws Exception {
        final Ipv4Address from = new Ipv4Address("0.0.0.1");
        final IpAddress remoteIp = new IpAddress(from);
        final Ipv4Address to = new Ipv4Address("255.255.255.255");

        final ReusableBGPPeer session1 = getMockSession();
        this.droppingBGPSessionRegistry.addPeer(remoteIp, session1, this.mockPreferences);

        this.droppingBGPSessionRegistry.getPeer(remoteIp, from, to, AS1);
        try {
            this.droppingBGPSessionRegistry.getPeer(remoteIp, to, to, AS1);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }

        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testDuplicateHigerAs() throws Exception {
        final Ipv4Address from = new Ipv4Address("0.0.0.1");
        final IpAddress remoteIp = new IpAddress(from);
        final Ipv4Address to = new Ipv4Address("255.255.255.255");
        final AsNumber as2 = new AsNumber(1235L);

        final ReusableBGPPeer session1 = getMockSession();
        this.droppingBGPSessionRegistry.addPeer(remoteIp, session1, this.mockPreferences);

        this.droppingBGPSessionRegistry.getPeer(remoteIp, from, to, AS1);
        this.droppingBGPSessionRegistry.getPeer(remoteIp, from, to, as2);
        Mockito.verify(session1).releaseConnection();
    }

    @Test
    public void testDuplicateLowerAs() throws Exception {
        final Ipv4Address from = new Ipv4Address("0.0.0.1");
        final IpAddress remoteIp = new IpAddress(from);
        final Ipv4Address to = new Ipv4Address("255.255.255.255");
        final AsNumber as2 = new AsNumber(3L);

        final ReusableBGPPeer session1 = getMockSession();
        this.droppingBGPSessionRegistry.addPeer(remoteIp, session1, this.mockPreferences);

        this.droppingBGPSessionRegistry.getPeer(remoteIp, from, to, AS1);
        try {
            this.droppingBGPSessionRegistry.getPeer(remoteIp, from, to, as2);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }

        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testAsMismatch() throws Exception {
        final Ipv4Address from = new Ipv4Address("0.0.0.1");
        final IpAddress remoteIp = new IpAddress(from);
        final Ipv4Address to = new Ipv4Address("255.255.255.255");
        final AsNumber as2 = new AsNumber(3L);

        final ReusableBGPPeer session1 = getMockSession();
        this.droppingBGPSessionRegistry.addPeer(remoteIp, session1, this.mockPreferences);

        try {
            this.droppingBGPSessionRegistry.getPeer(remoteIp, from, to, as2);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.BAD_PEER_AS, e.getError());
            return;
        }

        fail("Peer AS number mismatch");
    }

    private static ReusableBGPPeer getMockSession() {
        final ReusableBGPPeer mock = Mockito.mock(ReusableBGPPeer.class);
        Mockito.doNothing().when(mock).releaseConnection();
        return mock;
    }

    public BGPSessionPreferences getMockPreferences() {
        return new BGPSessionPreferences(AS1, 1,  new Ipv4Address("0.0.0.1"), Collections.<BgpParameters>emptyList(), AS2);
    }
}
