/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.bgp.parser.BGPSession;
import org.opendaylight.protocol.bgp.rib.impl.spi.GlobalBGPSessionRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;

public class DroppingBGPSessionRegistryTest {

    private DroppingBGPSessionRegistry droppingBGPSessionRegistry;

    @Before
    public void setUp() throws Exception {
        droppingBGPSessionRegistry = new DroppingBGPSessionRegistry();
    }

    @Test
    public void testDuplicate() throws Exception {
        final Ipv4Address from = new Ipv4Address("0.0.0.1");
        final Ipv4Address to = new Ipv4Address("255.255.255.255");

        final BGPSession session1 = getMockSession();
        assertEquals(GlobalBGPSessionRegistry.RegistrationResult.SUCCESS, droppingBGPSessionRegistry.addSession(session1, from, to));
        final BGPSession session2 = getMockSession();
        assertEquals(GlobalBGPSessionRegistry.RegistrationResult.DUPLICATE, droppingBGPSessionRegistry.addSession(session2, from, to));
        Mockito.verifyZeroInteractions(session1);
        Mockito.verify(session2).close();
    }

    @Test
    public void testOk() throws Exception {
        final Ipv4Address from = new Ipv4Address("0.0.0.1");
        final Ipv4Address to = new Ipv4Address("255.255.255.255");
        final Ipv4Address to2 = new Ipv4Address("255.255.255.254");

        final BGPSession session1 = getMockSession();
        assertEquals(GlobalBGPSessionRegistry.RegistrationResult.SUCCESS, droppingBGPSessionRegistry.addSession(session1, from, to));
        final BGPSession session2 = getMockSession();
        assertEquals(GlobalBGPSessionRegistry.RegistrationResult.SUCCESS, droppingBGPSessionRegistry.addSession(session2, from, to2));
        Mockito.verifyZeroInteractions(session1);
        Mockito.verifyZeroInteractions(session2);
    }

    @Test
    public void testDropSecond() throws Exception {
        final Ipv4Address higher = new Ipv4Address("192.168.200.200");
        final Ipv4Address lower = new Ipv4Address("10.10.10.10");

        final BGPSession session1 = getMockSession();
        assertEquals(GlobalBGPSessionRegistry.RegistrationResult.SUCCESS, droppingBGPSessionRegistry.addSession(session1, higher, lower));
        final BGPSession session2 = getMockSession();
        assertEquals(GlobalBGPSessionRegistry.RegistrationResult.DROPPED, droppingBGPSessionRegistry.addSession(session2, lower, higher));
        Mockito.verifyZeroInteractions(session1);
        Mockito.verify(session2).close();
    }

    @Test
    public void testDropFirst() throws Exception {
        final Ipv4Address from = new Ipv4Address("127.0.0.1");
        final Ipv4Address to = new Ipv4Address("127.0.0.2");

        final BGPSession session1 = getMockSession();
        assertEquals(GlobalBGPSessionRegistry.RegistrationResult.SUCCESS, droppingBGPSessionRegistry.addSession(session1, from, to));
        final BGPSession session2 = getMockSession();
        assertEquals(GlobalBGPSessionRegistry.RegistrationResult.DROPPED_PREVIOUS, droppingBGPSessionRegistry.addSession(session2, to, from));
        Mockito.verifyZeroInteractions(session2);
        Mockito.verify(session1).close();
    }

    private BGPSession getMockSession() {
        final BGPSession mock = Mockito.mock(BGPSession.class);
        Mockito.doNothing().when(mock).close();
        return mock;
    }
}