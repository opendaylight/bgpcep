/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.util.concurrent.Futures;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistrySessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint32;

class StrictBGPPeerRegistryTest {

    private static final AsNumber LOCAL_AS = new AsNumber(Uint32.valueOf(1234));
    private static final AsNumber REMOTE_AS = new AsNumber(Uint32.valueOf(1235));
    private static final Ipv4AddressNoZone FROM = new Ipv4AddressNoZone("0.0.0.1");
    private static final IpAddressNoZone REMOTE_IP = new IpAddressNoZone(FROM);
    private static final Ipv4AddressNoZone TO = new Ipv4AddressNoZone("255.255.255.255");

    private final BGPSessionListener peer1 = getMockSession();
    private final Open classicOpen = createOpen(TO, LOCAL_AS);
    private StrictBGPPeerRegistry peerRegistry;
    private BGPSessionPreferences mockPreferences;

    private static Open createOpen(final Ipv4AddressNoZone bgpId, final AsNumber as) {
        return new OpenBuilder()
            .setBgpIdentifier(bgpId)
            .setBgpParameters(List.of(new BgpParametersBuilder()
                .setOptionalCapabilities(List.of(new OptionalCapabilitiesBuilder()
                    .setCParameters(new CParametersBuilder()
                        .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(as).build())
                        .build())
                    .build()))
                .build()))
            .build();
    }

    @BeforeEach
    void setUp() {
        peerRegistry = new StrictBGPPeerRegistry();
        mockPreferences = new BGPSessionPreferences(LOCAL_AS, 1, new BgpId("0.0.0.1"), LOCAL_AS,
                Collections.emptyList());
    }

    private static BGPSessionListener getMockSession() {
        final BGPSessionListener mock = mock(BGPSessionListener.class);
        doReturn(Futures.immediateFuture(null)).when(mock).releaseConnection();
        return mock;
    }

    private static PeerRegistrySessionListener getMockSessionListener() {
        final PeerRegistrySessionListener mock = mock(PeerRegistrySessionListener.class);
        doNothing().when(mock).onSessionCreated(any(IpAddressNoZone.class));
        doNothing().when(mock).onSessionRemoved(any(IpAddressNoZone.class));
        return mock;
    }

    @Test
    void testIpAddressConstruction() throws BGPDocumentedException {
        final InetSocketAddress adr = new InetSocketAddress("127.0.0.1", 179);
        final IpAddressNoZone ipAdr = StrictBGPPeerRegistry.getIpAddress(adr);
        assertEquals("127.0.0.1", ipAdr.getIpv4AddressNoZone().getValue());
    }

    @Test
    void testDuplicatePeerConnection() throws BGPDocumentedException {
        peerRegistry.addPeer(REMOTE_IP, peer1, mockPreferences);
        peerRegistry.getPeer(REMOTE_IP, FROM, TO, classicOpen);
        final var ex = assertThrows(BGPDocumentedException.class,
            () -> peerRegistry.getPeer(REMOTE_IP, FROM, TO, classicOpen));
        assertEquals(BGPError.CEASE, ex.getError());
    }

    @Test
    void testPeerNotConfigured() {
        assertThrows(IllegalStateException.class,
            () -> peerRegistry.getPeer(REMOTE_IP, FROM, TO, classicOpen));
    }

    @Test
    void testPeerConnectionSuccessful() throws Exception {
        final Ipv4AddressNoZone to2 = new Ipv4AddressNoZone("255.255.255.254");
        final IpAddressNoZone remoteIp2 = new IpAddressNoZone(to2);

        peerRegistry.addPeer(REMOTE_IP, peer1, mockPreferences);
        final BGPSessionListener session2 = getMockSession();
        peerRegistry.addPeer(remoteIp2, session2, mockPreferences);

        final BGPSessionListener returnedSession1 = peerRegistry.getPeer(REMOTE_IP, FROM, TO, classicOpen);
        assertSame(peer1, returnedSession1);
        final BGPSessionListener returnedSession2 = peerRegistry.getPeer(remoteIp2, FROM, to2, classicOpen);
        assertSame(session2, returnedSession2);

        verifyNoMoreInteractions(peer1);
        verifyNoMoreInteractions(session2);
    }

    @Test
    void testDropSecondPeer() throws BGPDocumentedException {
        final Ipv4AddressNoZone higher = new Ipv4AddressNoZone("192.168.200.200");
        final Ipv4AddressNoZone lower = new Ipv4AddressNoZone("10.10.10.10");
        final IpAddressNoZone remoteIp = new IpAddressNoZone(lower);

        peerRegistry.addPeer(remoteIp, peer1, mockPreferences);

        peerRegistry.getPeer(remoteIp, higher, lower, createOpen(lower, LOCAL_AS));
        final var ex = assertThrows(BGPDocumentedException.class,
            () -> peerRegistry.getPeer(remoteIp, lower, higher, createOpen(higher, LOCAL_AS)));
        assertEquals(BGPError.CEASE, ex.getError());
    }

    @Test
    void testDropFirstPeer() throws Exception {
        final Ipv4AddressNoZone higher = new Ipv4AddressNoZone("123.123.123.123");
        final Ipv4AddressNoZone lower = new Ipv4AddressNoZone("123.123.123.122");
        final IpAddressNoZone remoteIp = new IpAddressNoZone(lower);

        peerRegistry.addPeer(remoteIp, peer1, mockPreferences);

        peerRegistry.getPeer(remoteIp, lower, higher, createOpen(higher, LOCAL_AS));
        peerRegistry.getPeer(remoteIp, higher, lower, createOpen(lower, LOCAL_AS));
        verify(peer1).releaseConnection();
    }

    @Test
    void testDuplicatePeersWDifferentIds() throws BGPDocumentedException {
        peerRegistry.addPeer(REMOTE_IP, peer1, mockPreferences);

        peerRegistry.getPeer(REMOTE_IP, FROM, TO, classicOpen);
        final var ex = assertThrows(BGPDocumentedException.class,
            () -> peerRegistry.getPeer(REMOTE_IP, TO, TO, classicOpen));
        assertEquals(BGPError.CEASE, ex.getError());
    }

    @Test
    void testDuplicatePeersHigherAs() throws BGPDocumentedException {
        peerRegistry.addPeer(REMOTE_IP, peer1, mockPreferences);

        peerRegistry.getPeer(REMOTE_IP, FROM, TO, classicOpen);
        peerRegistry.getPeer(REMOTE_IP, FROM, TO, createOpen(TO, REMOTE_AS));
        verify(peer1).releaseConnection();
    }

    @Test
    void testDuplicatePeersLowerAs() throws Exception {
        final AsNumber as2 = new AsNumber(Uint32.valueOf(3));

        peerRegistry.addPeer(REMOTE_IP, peer1, mockPreferences);

        peerRegistry.getPeer(REMOTE_IP, FROM, TO, classicOpen);
        final var ex = assertThrows(BGPDocumentedException.class,
            () -> peerRegistry.getPeer(REMOTE_IP, FROM, TO, createOpen(TO, as2)));
        assertEquals(BGPError.CEASE, ex.getError());
    }

    @Test
    void testAsMismatch() {
        final AsNumber as2 = new AsNumber(Uint32.valueOf(3));

        peerRegistry.addPeer(REMOTE_IP, peer1, mockPreferences);
        final var ex = assertThrows(BGPDocumentedException.class,
            () -> peerRegistry.getPeer(REMOTE_IP, FROM, TO, createOpen(TO, as2)));
        assertEquals(BGPError.BAD_PEER_AS, ex.getError());
    }

    @Test
    void testRegisterPeerSessionListener() throws Exception {
        final PeerRegistrySessionListener sessionListener1 = getMockSessionListener();
        peerRegistry.registerPeerSessionListener(sessionListener1);

        final PeerRegistrySessionListener sessionListener2 = getMockSessionListener();
        peerRegistry.registerPeerSessionListener(sessionListener2);

        peerRegistry.addPeer(REMOTE_IP, peer1, mockPreferences);
        peerRegistry.getPeer(REMOTE_IP, FROM, TO, classicOpen);
        verify(sessionListener1, times(1)).onSessionCreated(REMOTE_IP);
        verify(sessionListener2, times(1)).onSessionCreated(REMOTE_IP);

        peerRegistry.removePeerSession(REMOTE_IP);
        verify(sessionListener1, times(1)).onSessionRemoved(REMOTE_IP);
        verify(sessionListener2, times(1)).onSessionRemoved(REMOTE_IP);
    }

    @Test
    void testClosePeerSessionOneListener() throws BGPDocumentedException {
        final PeerRegistrySessionListener sessionListener1 = getMockSessionListener();
        final Registration registration1 = peerRegistry.registerPeerSessionListener(sessionListener1);

        final PeerRegistrySessionListener sessionListener2 = getMockSessionListener();
        peerRegistry.registerPeerSessionListener(sessionListener2);

        peerRegistry.addPeer(REMOTE_IP, peer1, mockPreferences);
        peerRegistry.getPeer(REMOTE_IP, FROM, TO, classicOpen);
        peerRegistry.removePeerSession(REMOTE_IP);

        registration1.close();
        peerRegistry.getPeer(REMOTE_IP, FROM, TO, classicOpen);
        peerRegistry.removePeerSession(REMOTE_IP);

        verify(sessionListener1, times(1)).onSessionCreated(REMOTE_IP);
        verify(sessionListener2, times(2)).onSessionCreated(REMOTE_IP);
        verify(sessionListener1, times(1)).onSessionRemoved(REMOTE_IP);
        verify(sessionListener2, times(2)).onSessionRemoved(REMOTE_IP);
    }
}
