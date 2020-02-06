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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpId;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint32;

public class StrictBGPPeerRegistryTest {

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
        final List<BgpParameters> params = Lists.newArrayList(new BgpParametersBuilder()
            .setOptionalCapabilities(Lists.newArrayList(new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder()
                    .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(as).build())
                    .build()).build())).build());
        return new OpenBuilder().setBgpIdentifier(bgpId).setBgpParameters(params).build();
    }

    @Before
    public void setUp() {
        this.peerRegistry = new StrictBGPPeerRegistry();
        this.mockPreferences = new BGPSessionPreferences(LOCAL_AS, 1, new BgpId("0.0.0.1"), LOCAL_AS,
                Collections.emptyList());
    }

    private static BGPSessionListener getMockSession() {
        final BGPSessionListener mock = Mockito.mock(BGPSessionListener.class);
        Mockito.doReturn(Futures.immediateFuture(null)).when(mock).releaseConnection();
        return mock;
    }

    private static PeerRegistrySessionListener getMockSessionListener() {
        final PeerRegistrySessionListener mock = Mockito.mock(PeerRegistrySessionListener.class);
        Mockito.doNothing().when(mock).onSessionCreated(Mockito.any(IpAddressNoZone.class));
        Mockito.doNothing().when(mock).onSessionRemoved(Mockito.any(IpAddressNoZone.class));
        return mock;
    }

    @Test
    public void testIpAddressConstruction() throws BGPDocumentedException {
        final InetSocketAddress adr = new InetSocketAddress("127.0.0.1", 179);
        final IpAddressNoZone ipAdr = StrictBGPPeerRegistry.getIpAddress(adr);
        assertEquals("127.0.0.1", ipAdr.getIpv4AddressNoZone().getValue());
    }

    @Test
    public void testDuplicatePeerConnection() throws BGPDocumentedException {
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);
        this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, this.classicOpen);
        try {
            this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, this.classicOpen);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testPeerNotConfigured() throws BGPDocumentedException {
        try {
            this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, this.classicOpen);
        } catch (final IllegalStateException e) {
            return;
        }
        fail("Unknown peer cannot be connected");
    }

    @Test
    public void testPeerConnectionSuccessfull() throws Exception {
        final Ipv4AddressNoZone to2 = new Ipv4AddressNoZone("255.255.255.254");
        final IpAddressNoZone remoteIp2 = new IpAddressNoZone(to2);

        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);
        final BGPSessionListener session2 = getMockSession();
        this.peerRegistry.addPeer(remoteIp2, session2, this.mockPreferences);

        final BGPSessionListener returnedSession1 = this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, this.classicOpen);
        assertSame(this.peer1, returnedSession1);
        final BGPSessionListener returnedSession2 = this.peerRegistry.getPeer(remoteIp2, FROM, to2, this.classicOpen);
        assertSame(session2, returnedSession2);

        Mockito.verifyZeroInteractions(this.peer1);
        Mockito.verifyZeroInteractions(session2);
    }

    @Test
    public void testDropSecondPeer() throws BGPDocumentedException {
        final Ipv4AddressNoZone higher = new Ipv4AddressNoZone("192.168.200.200");
        final Ipv4AddressNoZone lower = new Ipv4AddressNoZone("10.10.10.10");
        final IpAddressNoZone remoteIp = new IpAddressNoZone(lower);

        this.peerRegistry.addPeer(remoteIp, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(remoteIp, higher, lower, createOpen(lower, LOCAL_AS));
        try {
            this.peerRegistry.getPeer(remoteIp, lower, higher, createOpen(higher, LOCAL_AS));
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testDropFirstPeer() throws Exception {
        final Ipv4AddressNoZone higher = new Ipv4AddressNoZone("123.123.123.123");
        final Ipv4AddressNoZone lower = new Ipv4AddressNoZone("123.123.123.122");
        final IpAddressNoZone remoteIp = new IpAddressNoZone(lower);

        this.peerRegistry.addPeer(remoteIp, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(remoteIp, lower, higher, createOpen(higher, LOCAL_AS));
        this.peerRegistry.getPeer(remoteIp, higher, lower, createOpen(lower, LOCAL_AS));
        Mockito.verify(this.peer1).releaseConnection();
    }

    @Test
    public void testDuplicatePeersWDifferentIds() throws BGPDocumentedException {
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, this.classicOpen);
        try {
            this.peerRegistry.getPeer(REMOTE_IP, TO, TO, this.classicOpen);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testDuplicatePeersHigherAs() throws BGPDocumentedException {
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, this.classicOpen);
        this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, createOpen(TO, REMOTE_AS));
        Mockito.verify(this.peer1).releaseConnection();
    }

    @Test
    public void testDuplicatePeersLowerAs() throws Exception {
        final AsNumber as2 = new AsNumber(Uint32.valueOf(3));

        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, this.classicOpen);
        try {
            this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, createOpen(TO, as2));
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testAsMismatch() {
        final AsNumber as2 = new AsNumber(Uint32.valueOf(3));

        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);
        try {
            this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, createOpen(TO, as2));
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.BAD_PEER_AS, e.getError());
            return;
        }
        fail("Peer AS number mismatch");
    }

    @Test
    public void testRegisterPeerSessionListener() throws Exception {
        final PeerRegistrySessionListener sessionListener1 = getMockSessionListener();
        this.peerRegistry.registerPeerSessionListener(sessionListener1);

        final PeerRegistrySessionListener sessionListener2 = getMockSessionListener();
        this.peerRegistry.registerPeerSessionListener(sessionListener2);

        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);
        this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, this.classicOpen);
        Mockito.verify(sessionListener1, Mockito.times(1)).onSessionCreated(REMOTE_IP);
        Mockito.verify(sessionListener2, Mockito.times(1)).onSessionCreated(REMOTE_IP);

        this.peerRegistry.removePeerSession(REMOTE_IP);
        Mockito.verify(sessionListener1, Mockito.times(1)).onSessionRemoved(REMOTE_IP);
        Mockito.verify(sessionListener2, Mockito.times(1)).onSessionRemoved(REMOTE_IP);
    }

    @Test
    public void testClosePeerSessionOneListener() throws BGPDocumentedException {
        final PeerRegistrySessionListener sessionListener1 = getMockSessionListener();
        final Registration registration1 = this.peerRegistry.registerPeerSessionListener(sessionListener1);

        final PeerRegistrySessionListener sessionListener2 = getMockSessionListener();
        this.peerRegistry.registerPeerSessionListener(sessionListener2);

        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);
        this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, this.classicOpen);
        this.peerRegistry.removePeerSession(REMOTE_IP);

        registration1.close();
        this.peerRegistry.getPeer(REMOTE_IP, FROM, TO, this.classicOpen);
        this.peerRegistry.removePeerSession(REMOTE_IP);

        Mockito.verify(sessionListener1, Mockito.times(1)).onSessionCreated(REMOTE_IP);
        Mockito.verify(sessionListener2, Mockito.times(2)).onSessionCreated(REMOTE_IP);
        Mockito.verify(sessionListener1, Mockito.times(1)).onSessionRemoved(REMOTE_IP);
        Mockito.verify(sessionListener2, Mockito.times(2)).onSessionRemoved(REMOTE_IP);
    }
}
