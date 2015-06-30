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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.BgpParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.OptionalCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.c.parameters.As4BytesCapabilityBuilder;

public class StrictBGPPeerRegistryTest {

    private static final AsNumber LOCAL_AS = new AsNumber(1234L);
    private static final AsNumber REMOTE_AS = new AsNumber(1235L);
    private static final Ipv4Address FROM = new Ipv4Address("0.0.0.1");
    private static final Ipv4Address TO = new Ipv4Address("255.255.255.255");
    private static final IpAddress REMOTE_IP = new IpAddress(TO);

    private final ReusableBGPPeer peer1 = getMockSession();
    private SocketAddress remoteSocket;
    private Open open;
    private StrictBGPPeerRegistry peerRegistry;
    private BGPSessionPreferences mockPreferences;
    private final AsNumber AS1 = new AsNumber(1234L);

    private Open createOpen(final Ipv4Address bgpId, final AsNumber as) {
        final List<BgpParameters> params = Lists.newArrayList(new BgpParametersBuilder()
            .setOptionalCapabilities(Lists.newArrayList(new OptionalCapabilitiesBuilder()
                .setCParameters(new CParametersBuilder()
                    .setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(as).build()).build()).build())).build());
        return new OpenBuilder().setBgpIdentifier(bgpId).setBgpParameters(params).build();

    private List<BgpParameters> addAs4B(final AsNumber as) {
        return Lists.newArrayList(new BgpParametersBuilder().setOptionalCapabilities(Lists.newArrayList(new OptionalCapabilitiesBuilder().setCParameters(new CParametersBuilder().setAs4BytesCapability(new As4BytesCapabilityBuilder().setAsNumber(as).build()).build()).build())).build());
    }

    @Before
    public void setUp() throws Exception {
        this.peerRegistry = new StrictBGPPeerRegistry();
        this.mockPreferences = new BGPSessionPreferences(LOCAL_AS, 1, FROM, Collections.<BgpParameters> emptyList(), REMOTE_AS);
        this.remoteSocket = new InetSocketAddress(InetAddress.getByName(TO.getValue()), 179);
        this.open = new OpenBuilder().setBgpIdentifier(TO).setBgpParameters(addAs4B(REMOTE_AS)).build();
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

        this.peerRegistry.getPeer(this.remoteSocket, this.open);
        try {
            this.peerRegistry.getPeer(this.remoteSocket, this.open);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testPeerNotConfigured() throws Exception {
        try {
            this.peerRegistry.getPeer(this.remoteSocket, this.open);
        } catch (final IllegalStateException e) {
            return;
        }
        fail("Unknown peer cannot be connected");
    }

    @Test
    public void testPeerConnectionSuccessful() throws Exception {
        final Ipv4Address to2 = new Ipv4Address("255.255.255.254");
        final IpAddress remoteIp2 = new IpAddress(to2);
        final SocketAddress remoteSocket2 = new InetSocketAddress(InetAddress.getByName(to2.getValue()), 179);

        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);
        final ReusableBGPPeer peer2 = getMockSession();
        this.peerRegistry.addPeer(remoteIp2, peer2, this.mockPreferences);

        final BGPSessionListener returnedSession1 = this.peerRegistry.getPeer(this.remoteSocket, this.open);
        assertSame(this.peer1, returnedSession1);
        final BGPSessionListener returnedSession2 = this.peerRegistry.getPeer(remoteSocket2, this.open);
        assertSame(peer2, returnedSession2);

        Mockito.verifyZeroInteractions(this.peer1);
        Mockito.verifyZeroInteractions(peer2);
    }

    @Test
    public void testDropFirstPeer() throws Exception {
        final Ipv4Address higher = new Ipv4Address("123.123.123.123");
        final Ipv4Address lower = new Ipv4Address("123.123.123.122");
        final SocketAddress remoteSocket = new InetSocketAddress(InetAddress.getByName(lower.getValue()), 179);
        final Open oHigher = new OpenBuilder().setBgpIdentifier(higher).setBgpParameters(addAs4B(REMOTE_AS)).build();
        final Open oLower = new OpenBuilder().setBgpIdentifier(lower).setBgpParameters(addAs4B(REMOTE_AS)).build();

        this.peerRegistry.addPeer(new IpAddress(lower), this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(remoteSocket, oHigher);
        this.peerRegistry.getPeer(remoteSocket, oLower);
        Mockito.verify(this.peer1).releaseConnection();
    }

    @Test
    public void testDropSecondPeer() throws Exception {
        final Ipv4Address higher = new Ipv4Address("192.168.200.200");
        final Ipv4Address lower = new Ipv4Address("10.10.10.10");
        final SocketAddress remoteSocket = new InetSocketAddress(InetAddress.getByName(lower.getValue()), 179);
        final Open oHigher = new OpenBuilder().setBgpIdentifier(higher).setBgpParameters(addAs4B(REMOTE_AS)).build();
        final Open oLower = new OpenBuilder().setBgpIdentifier(lower).setBgpParameters(addAs4B(REMOTE_AS)).build();

        this.peerRegistry.addPeer(new IpAddress(lower), this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(remoteSocket, oHigher);
        try {
            this.peerRegistry.getPeer(remoteSocket, oLower);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testDuplicatePeersWDifferentIds() throws Exception {
        final Open to = new OpenBuilder().setBgpIdentifier(TO).setBgpParameters(addAs4B(LOCAL_AS)).build();
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(this.remoteSocket, this.open);
        try {
            this.peerRegistry.getPeer(this.remoteSocket, to);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testDuplicatePeersHigherAs() throws Exception {
        final Open to = new OpenBuilder().setBgpIdentifier(TO).setBgpParameters(addAs4B(new AsNumber(1000L))).build();
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(this.remoteSocket, this.open);
        this.peerRegistry.getPeer(this.remoteSocket, to);
        Mockito.verify(this.peer1).releaseConnection();
    }

    @Test
    public void testDuplicatePeersLowerAs() throws Exception {
        final Open to = new OpenBuilder().setBgpIdentifier(TO).setBgpParameters(addAs4B(new AsNumber(3L))).build();
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        this.peerRegistry.getPeer(this.remoteSocket, to);
        try {
            this.peerRegistry.getPeer(this.remoteSocket, this.open);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.CEASE, e.getError());
            return;
        }
        fail("Same peer cannot be connected twice");
    }

    @Test
    public void testAsMismatch() throws Exception {
        this.peerRegistry.addPeer(REMOTE_IP, this.peer1, this.mockPreferences);

        try {
            this.peerRegistry.getPeer(this.remoteSocket, this.open);
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.BAD_PEER_AS, e.getError());
            return;
        }
        fail("Peer AS number mismatch");
    }
}
