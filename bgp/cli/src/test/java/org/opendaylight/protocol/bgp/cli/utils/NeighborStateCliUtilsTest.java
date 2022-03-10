/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli.utils;

import static org.junit.Assert.assertEquals;

import com.google.common.io.Resources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.bgp.neighbor.prefix.counters_state.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ADDPATHS;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.BgpNeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborTimersStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.NeighborTransportStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.messages.ReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev180329.network.instance.protocol.bgp.neighbor_state.augmentation.messages.SentBuilder;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public class NeighborStateCliUtilsTest {

    static final String NEIGHBOR_ADDRESS = "127.0.0.2";
    private static final IpAddress NEIGHBOR_IP_ADDRESS = new IpAddress(new Ipv4Address(NEIGHBOR_ADDRESS));
    private static final String  NO_SESSION_FOUND = "No BgpSessionState found for [" + NEIGHBOR_ADDRESS + "]\n";
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(output);

    static Neighbor createBasicNeighbor() {
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi
                .list.afi.safi.StateBuilder builder = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
                .multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder();

        builder.addAugmentation(new NeighborAfiSafiStateAugmentationBuilder().setActive(false).build());
        final AfiSafi afiSafi = new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
                .setState(builder.build()).build();

        return new NeighborBuilder()
                .setNeighborAddress(NEIGHBOR_IP_ADDRESS)
                .setState(new StateBuilder().build())
                .setAfiSafis(new AfiSafisBuilder().setAfiSafi(Map.of(afiSafi.key(), afiSafi)).build())
                .build();
    }

    @Test
    public void testNeighborStateWO_StateCli() {
        NeighborStateCliUtils.displayNeighborOperationalState(NEIGHBOR_ADDRESS,
            new NeighborBuilder().setNeighborAddress(new IpAddress(new Ipv4Address("1.2.3.4"))).build(),  stream);
        assertEquals(NO_SESSION_FOUND, output.toString());
    }

    @Test
    public void testEmptyNeighborStateCli() throws IOException {
        NeighborStateCliUtils.displayNeighborOperationalState(NEIGHBOR_ADDRESS, createBasicNeighbor(), stream);

        final String expected = Resources.toString(getClass().getClassLoader().getResource("empty-neighbor.txt"),
            StandardCharsets.UTF_8);
        assertEquals(expected, output.toString());
    }

    @Test
    public void testFullNeighborStateCli() throws IOException {
        NeighborStateCliUtils.displayNeighborOperationalState(NEIGHBOR_ADDRESS, new NeighborBuilder()
            .setNeighborAddress(new IpAddress(new Ipv4Address("1.2.3.4")))
            .setState(new StateBuilder()
                .addAugmentation(new NeighborStateAugmentationBuilder()
                    .setSupportedCapabilities(Set.of(ADDPATHS.class))
                    .setSessionState(BgpNeighborState.SessionState.ACTIVE)
                    .build())
                .addAugmentation(new BgpNeighborStateAugmentationBuilder()
                    .setMessages(new MessagesBuilder()
                        .setReceived(new ReceivedBuilder().setNOTIFICATION(Uint64.ONE).setUPDATE(Uint64.TEN).build())
                        .setSent(new SentBuilder().setNOTIFICATION(Uint64.TEN).setUPDATE(Uint64.ONE).build())
                        .build())
                    .build())
                .build())
            .setAfiSafis(new AfiSafisBuilder()
                .setAfiSafi(BindingMap.of(new AfiSafiBuilder()
                    .setAfiSafiName(IPV4UNICAST.class)
                    .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp
                        .common.afi.safi.list.afi.safi.StateBuilder()
                        .addAugmentation(new NeighborAfiSafiStateAugmentationBuilder()
                            .setActive(Boolean.TRUE)
                            .setPrefixes(new PrefixesBuilder()
                                .setInstalled(Uint32.ONE)
                                .setReceived(Uint32.ONE)
                                .setSent(Uint32.TWO).build())
                            .build())
                        .build())
                    .build()))
                .build())
            .setTransport(new TransportBuilder()
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                    .transport.StateBuilder()
                    .addAugmentation(new NeighborTransportStateAugmentationBuilder()
                        .setRemoteAddress(NEIGHBOR_IP_ADDRESS)
                        .setLocalPort(new PortNumber(Uint16.valueOf(1234)))
                        .setRemotePort(new PortNumber(Uint16.valueOf(4321)))
                        .build())
                    .build())
                .build())
            .setTimers(new TimersBuilder()
                .setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group
                    .timers.StateBuilder()
                    .addAugmentation(new NeighborTimersStateAugmentationBuilder()
                        .setNegotiatedHoldTime(Decimal64.valueOf("10.00"))
                        .setUptime(new Timeticks(Uint32.valueOf(600)))
                        .build())
                    .build())
                .build())
            .build(), stream);

        final String expected = Resources.toString(getClass().getClassLoader().getResource("neighbor.txt"),
            StandardCharsets.UTF_8);
        assertEquals(expected, output.toString());
    }
}