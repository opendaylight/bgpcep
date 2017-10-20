/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.cli.utils;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bgp.cli.utils.PeerGroupStateCliUtilsTest.UTF8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.BgpNeighborState;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.operational.rev151009.bgp.neighbor.prefix.counters_state.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.StateBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Timers;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TimersBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.Transport;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.TransportBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.ADDPATHS;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timeticks;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.BgpNeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborAfiSafiStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTimersStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTimersStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTransportStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.NeighborTransportStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.MessagesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.Received;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.ReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.Sent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.network.instances.network.instance.protocols.protocol.bgp.neighbors.neighbor.state.messages.SentBuilder;

public class NeighborStateCliUtilsTest {

    static final String NEIGHBOR_ADDRESS = "127.0.0.2";
    private static final IpAddress NEIGHBOR_IP_ADDRESS = new IpAddress(new Ipv4Address(NEIGHBOR_ADDRESS));
    private static final String  NO_SESSION_FOUND = "No BgpSessionState found for [" + NEIGHBOR_ADDRESS + "]\n";
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream stream = new PrintStream(this.output);

    static Neighbor createBasicNeighbor() {
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi
                .list.afi.safi.StateBuilder builder = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
                .multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder();

        builder.addAugmentation(NeighborAfiSafiStateAugmentation.class, new NeighborAfiSafiStateAugmentationBuilder()
                .setActive(false).build());
        final AfiSafi afiSafi = new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
                .setState(builder.build()).build();

        return new NeighborBuilder()
                .setNeighborAddress(NEIGHBOR_IP_ADDRESS)
                .setState(new StateBuilder().build())
                .setAfiSafis(new AfiSafisBuilder().setAfiSafi(Collections.singletonList(afiSafi)).build())
                .build();
    }

    @Test
    public void testNeighborStateWO_StateCli() {
        NeighborStateCliUtils.displayNeighborOperationalState(NEIGHBOR_ADDRESS,
                new NeighborBuilder().build(), this.stream);
        assertEquals(NO_SESSION_FOUND, this.output.toString());
    }

    @Test
    public void testEmptyNeighborStateCli() throws IOException {
        final Neighbor neighbor = createBasicNeighbor();
        NeighborStateCliUtils.displayNeighborOperationalState(NEIGHBOR_ADDRESS,
                neighbor, this.stream);

        final String expected = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("empty-neighbor.txt"), UTF8);
        assertEquals(expected, this.output.toString());
    }

    @Test
    public void testFullNeighborStateCli() throws IOException {
        final org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi
                .list.afi.safi.StateBuilder builder = new org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp
                .multiprotocol.rev151009.bgp.common.afi.safi.list.afi.safi.StateBuilder();

        builder.addAugmentation(NeighborAfiSafiStateAugmentation.class, new NeighborAfiSafiStateAugmentationBuilder()
                .setActive(true)
                .setPrefixes(new PrefixesBuilder().setInstalled(1L).setReceived(1L).setSent(2L).build())
                .build());
        final AfiSafi afiSafi = new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class)
                .setState(builder.build()).build();


        final StateBuilder stateBuilder = new StateBuilder();
        stateBuilder.addAugmentation(NeighborStateAugmentation.class,
                new NeighborStateAugmentationBuilder()
                        .setSupportedCapabilities(Collections.singletonList(ADDPATHS.class))
                        .setSessionState(BgpNeighborState.SessionState.ACTIVE).build());

        final Received received = new ReceivedBuilder()
                .setNOTIFICATION(BigInteger.ONE)
                .setUPDATE(BigInteger.TEN)
                .build();

        final Sent sent = new SentBuilder()
                .setNOTIFICATION(BigInteger.TEN)
                .setUPDATE(BigInteger.ONE)
                .build();

        stateBuilder.addAugmentation(BgpNeighborStateAugmentation.class,
                new BgpNeighborStateAugmentationBuilder()
                        .setMessages(new MessagesBuilder()
                                .setReceived(received)
                                .setSent(sent)
                                .build())
                        .build());

        final Transport transport = new TransportBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net
                .yang.bgp.rev151009.bgp.neighbor.group.transport.StateBuilder()
                .addAugmentation(NeighborTransportStateAugmentation.class,
                        new NeighborTransportStateAugmentationBuilder()
                                .setRemoteAddress(NEIGHBOR_IP_ADDRESS)
                                .setLocalPort(new PortNumber(1234))
                                .setRemotePort(new PortNumber(4321))
                                .build())
                .build()).build();
        final Timers timers = new TimersBuilder().setState(new org.opendaylight.yang.gen.v1.http.openconfig.net.yang
                .bgp.rev151009.bgp.neighbor.group.timers.StateBuilder()
                .addAugmentation(NeighborTimersStateAugmentation.class,
                        new NeighborTimersStateAugmentationBuilder()
                                .setNegotiatedHoldTime(BigDecimal.TEN)
                                .setUptime(new Timeticks(600L))
                                .build())
                .build()).build();
        final Neighbor neighbor = new NeighborBuilder()
                .setState(stateBuilder.build())
                .setAfiSafis(new AfiSafisBuilder().setAfiSafi(Collections.singletonList(afiSafi)).build())
                .setTransport(transport)
                .setTimers(timers)
                .build();
        NeighborStateCliUtils.displayNeighborOperationalState(NEIGHBOR_ADDRESS,
                neighbor, this.stream);

        final String expected = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("neighbor.txt"), UTF8);
        assertEquals(expected, this.output.toString());
    }
}