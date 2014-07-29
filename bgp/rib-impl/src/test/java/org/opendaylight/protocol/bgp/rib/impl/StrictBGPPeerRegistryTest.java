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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.ReusableBGPPeer;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

public class StrictBGPPeerRegistryTest {

    private StrictBGPPeerRegistry droppingBGPSessionRegistry;
    private BGPSessionPreferences mockPreferences;

    @Before
    public void setUp() throws Exception {
        droppingBGPSessionRegistry = new StrictBGPPeerRegistry();
        mockPreferences = getMockPreferences();
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
        droppingBGPSessionRegistry.addPeer(remoteIp, session1, mockPreferences);

        droppingBGPSessionRegistry.getPeer(remoteIp, from, to);
        try {
            droppingBGPSessionRegistry.getPeer(remoteIp, from, to);
        } catch (final IllegalStateException e) {
            Mockito.verifyZeroInteractions(session1);
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
            droppingBGPSessionRegistry.getPeer(remoteIp, from, to);
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
        droppingBGPSessionRegistry.addPeer(remoteIp, session1, mockPreferences);
        final ReusableBGPPeer session2 = getMockSession();
        droppingBGPSessionRegistry.addPeer(remoteIp2, session2, mockPreferences);

        final BGPSessionListener returnedSession1 = droppingBGPSessionRegistry.getPeer(remoteIp, from, to);
        assertSame(session1, returnedSession1);
        final BGPSessionListener returnedSession2 = droppingBGPSessionRegistry.getPeer(remoteIp2, from, to2);
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
        droppingBGPSessionRegistry.addPeer(remoteIp, session1, mockPreferences);

        droppingBGPSessionRegistry.getPeer(remoteIp, higher, lower);
        try {
            droppingBGPSessionRegistry.getPeer(remoteIp, lower, higher);
        } catch (final BGPDocumentedException e) {
            Mockito.verifyZeroInteractions(session1);
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
        droppingBGPSessionRegistry.addPeer(remoteIp, session1, mockPreferences);

        droppingBGPSessionRegistry.getPeer(remoteIp, lower, higher);
        droppingBGPSessionRegistry.getPeer(remoteIp, higher, lower);
        Mockito.verify(session1).releaseConnection();
    }

    private ReusableBGPPeer getMockSession() {
        final ReusableBGPPeer mock = Mockito.mock(ReusableBGPPeer.class);
        Mockito.doNothing().when(mock).releaseConnection();
        return mock;
    }

    public BGPSessionPreferences getMockPreferences() {
        return new BGPSessionPreferences(null, 1, null, null);
    }
}
