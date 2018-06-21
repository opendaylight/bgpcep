/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.bgp.concepts.RouteDistinguisherUtil.extractRouteDistinguisher;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.flowspec.handlers.AbstractNumericOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.AbstractOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.BitmaskOperandParser;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.AbstractFlowspecL3vpnNlriParser;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4.FlowspecL3vpnIpv4NlriParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.DestinationPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.DscpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.FragmentCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.IcmpCodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.IcmpTypeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.PacketLengthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.PacketLengthCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.PortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.SourcePortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.TcpFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.TcpFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.destination.port._case.DestinationPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.destination.port._case.DestinationPortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.dscp._case.Dscps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.dscp._case.DscpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.fragment._case.Fragments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.fragment._case.FragmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.icmp.code._case.Codes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.icmp.code._case.CodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.icmp.type._case.Types;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.icmp.type._case.TypesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.packet.length._case.PacketLengths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.packet.length._case.PacketLengthsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.port._case.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.port._case.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.source.port._case.SourcePorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.source.port._case.SourcePortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.DestinationPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.ProtocolIpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.SourcePrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.protocol.ip._case.ProtocolIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.group.ipv4.flowspec.flowspec.type.protocol.ip._case.ProtocolIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;

public class FlowspecL3vpnIpv4NlriParserTest {

    private static final NodeIdentifier RD_NID = new NodeIdentifier(QName.create(Flowspec.QNAME.getNamespace(), Flowspec.QNAME.getRevision(), "route-distinguisher"));
    private static final NodeIdentifier PROTOCOL_IP_NID = new NodeIdentifier(ProtocolIps.QNAME);

    private static final String ROUTE_DISTINGUISHER = "1.2.3.4:10";

    private static final PathId PATH_ID = new PathId(1L);

    @Mock
    private PeerSpecificParserConstraint constraint;
    @Mock
    private MultiPathSupport muliPathSupport;

    private static final byte[] REACHED_NLRI = new byte[] {
        0x29,   // NLRI length: 8+33=41
        0, 1, 1, 2, 3, 4, 0, 10,    // route distinguisher: 1.2.3.4:10
        0x01, 0x20, 0x0a, 0x00, 0x01, 0x00,
        0x02, 0x20, 0x01, 0x02, 0x03, 0x04,
        0x03, (byte) 0x81, 0x06,
        0x04, 0x03, (byte) 0x89, 0x45, (byte) 0x8b, (byte) 0x91, 0x1f, (byte) 0x90,
        0x05, 0x12, 0x0f, (byte) 0xf9, (byte) 0x81, (byte) 0xb3,
        0x06, (byte) 0x91, 0x1f, (byte) 0x90
    };

    private static final byte[] REACHED_NLRI_ADD_PATH = new byte[] {
        0x0, 0x0, 0x0, 0x1,
        0x29,   // NLRI length: 8+33=41
        0, 1, 1, 2, 3, 4, 0, 10,    // route distinguisher: 1.2.3.4:10
        0x01, 0x20, 0x0a, 0x00, 0x01, 0x00,
        0x02, 0x20, 0x01, 0x02, 0x03, 0x04,
        0x03, (byte) 0x81, 0x06,
        0x04, 0x03, (byte) 0x89, 0x45, (byte) 0x8b, (byte) 0x91, 0x1f, (byte) 0x90,
        0x05, 0x12, 0x0f, (byte) 0xf9, (byte) 0x81, (byte) 0xb3,
        0x06, (byte) 0x91, 0x1f, (byte) 0x90
    };

    private static final byte[] UNREACHED_NLRI = new byte[] {
        0x23,   // NLRI length: 8+33=41
        0, 1, 1, 2, 3, 4, 0, 10,    // route distinguisher: 1.2.3.4:10
        0x07, 4, 2, (byte) 0x84, 3,
        0x08, 4, 4, (byte) 0x80, 5,
        0x09, 0x12, 4, 1, (byte) 0x91, 0x56, (byte) 0xb1,
        0x0a, (byte) 0x94, (byte) 0xde, (byte) 0xad,
        0x0b, (byte) 0x82, 0x2a,
        0x0c, (byte) 0x81, (byte) 0x0e
    };

    private static final byte[] UNREACHED_NLRI_ADD_PATH = new byte[] {
        0x0, 0x0, 0x0, 0x1,
        0x23,   // NLRI length: 8+27=35
        0, 1, 1, 2, 3, 4, 0, 10,    // route distinguisher: 1.2.3.4:10
        0x07, 4, 2, (byte) 0x84, 3,
        0x08, 4, 4, (byte) 0x80, 5,
        0x09, 0x12, 4, 1, (byte) 0x91, 0x56, (byte) 0xb1,
        0x0a, (byte) 0x94, (byte) 0xde, (byte) 0xad,
        0x0b, (byte) 0x82, 0x2a,
        0x0c, (byte) 0x81, (byte) 0x0e
    };

    private final SimpleFlowspecExtensionProviderContext flowspecContext = new SimpleFlowspecExtensionProviderContext();
    private final FlowspecActivator fsa = new FlowspecActivator(this.flowspecContext);
    private final FlowspecL3vpnIpv4NlriParser FS_PARSER = new FlowspecL3vpnIpv4NlriParser(this.flowspecContext.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN));

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(Optional.of(this.muliPathSupport)).when(this.constraint).getPeerConstraint(Mockito.any());
        Mockito.doReturn(true).when(this.muliPathSupport).isTableTypeSupported(Mockito.any());
    }

    @Test
    public void testParseMpReachNlri() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpReachNlriBuilder mp = new MpReachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();
        final DestinationPrefixCase destinationPrefix = new DestinationPrefixCaseBuilder().setDestinationPrefix(new Ipv4Prefix("10.0.1.0/32")).build();
        builder.setFlowspecType(destinationPrefix);
        fs.add(builder.build());
        final SourcePrefixCase sourcePrefix = new SourcePrefixCaseBuilder().setSourcePrefix(new Ipv4Prefix("1.2.3.4/32")).build();
        builder.setFlowspecType(sourcePrefix);
        fs.add(builder.build());

        final FlowspecType prots = createProts();
        builder.setFlowspecType(prots);
        fs.add(builder.build());

        final PortCase ps = createPorts();
        builder.setFlowspecType(ps);
        fs.add(builder.build());

        final FlowspecType dps = createDps();
        builder.setFlowspecType(dps);
        fs.add(builder.build());

        final FlowspecType sps = createSps();
        builder.setFlowspecType(sps);
        fs.add(builder.build());

        final FlowspecL3vpnIpv4NlriParser parser = new FlowspecL3vpnIpv4NlriParser(this.flowspecContext.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN));

        final MpReachNlriBuilder result = new MpReachNlriBuilder();
        result.setAfi(Ipv4AddressFamily.class);
        result.setSafi(FlowspecL3vpnSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.wrappedBuffer(REACHED_NLRI), result);

        final DestinationFlowspecL3vpnIpv4 flowspecDst = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv4Case) result.getAdvertizedRoutes().getDestinationType())
            .getDestinationFlowspecL3vpnIpv4();
        final List<Flowspec> flows = flowspecDst.getFlowspec();
        final RouteDistinguisher rd = flowspecDst.getRouteDistinguisher();

        testFlows(flows, destinationPrefix, sourcePrefix, prots, rd, ps, dps, sps);

        mp.setAdvertizedRoutes(
            new AdvertizedRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv4CaseBuilder()
                    .setDestinationFlowspecL3vpnIpv4(
                        new DestinationFlowspecL3vpnIpv4Builder()
                            .setRouteDistinguisher(rd)
                            .setFlowspec(fs)
                            .build()
                    ).build()
            ).build()
        );

        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mp.setAfi(Ipv4AddressFamily.class).build()).build()).build(), buffer);
        assertArrayEquals(REACHED_NLRI, ByteArray.readAllBytes(buffer));

        assertEquals("all packets to 10.0.1.0/32 AND from 1.2.3.4/32 AND where IP protocol equals to 6 AND where port is greater than or equals to 137 and is less than or equals to 139 or equals to 8080 AND where destination port is greater than 4089 or equals to 179 AND where source port equals to 8080 ", this.FS_PARSER.stringNlri(flows));
    }

    private static void testFlows(
        final List<Flowspec> flows,
        final DestinationPrefixCase destinationPrefix,
        final SourcePrefixCase sourcePrefix,
        final FlowspecType prots,
        final RouteDistinguisher rd,
        final PortCase ps,
        final FlowspecType dps,
        final FlowspecType sps
    ) {
        assertEquals(6, flows.size());
        assertEquals(ROUTE_DISTINGUISHER, new String(rd.getValue()));
        assertEquals(destinationPrefix, flows.get(0).getFlowspecType());
        assertEquals(sourcePrefix, flows.get(1).getFlowspecType());
        assertEquals(prots, flows.get(2).getFlowspecType());
        assertEquals(ps, flows.get(3).getFlowspecType());
        assertEquals(dps, flows.get(4).getFlowspecType());
        assertEquals(sps, flows.get(5).getFlowspecType());
    }

    private static FlowspecType createSps() {
        final List<SourcePorts> sports = Lists.newArrayList(new SourcePortsBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue(8080).build());
        return new SourcePortCaseBuilder().setSourcePorts(sports).build();
    }

    private static FlowspecType createProts() {
        final List<ProtocolIps> protocols = Lists.newArrayList(new ProtocolIpsBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue((short) 6).build());
        return new ProtocolIpCaseBuilder().setProtocolIps(protocols).build();
    }

    private static FlowspecType createDps() {
        final List<DestinationPorts> destports = Lists.newArrayList(new DestinationPortsBuilder().setOp(new NumericOperand(false, false, false, true, false)).setValue(4089).build(),
            new DestinationPortsBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue(179).build());
        return new DestinationPortCaseBuilder().setDestinationPorts(destports).build();
    }

    @Test
    public void testParseMpReachNlriConstraint() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpReachNlriBuilder mp = new MpReachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();
        final DestinationPrefixCase destinationPrefix = new DestinationPrefixCaseBuilder().setDestinationPrefix(new Ipv4Prefix("10.0.1.0/32")).build();
        builder.setFlowspecType(destinationPrefix);
        fs.add(builder.build());
        final SourcePrefixCase sourcePrefix = new SourcePrefixCaseBuilder().setSourcePrefix(new Ipv4Prefix("1.2.3.4/32")).build();
        builder.setFlowspecType(sourcePrefix);
        fs.add(builder.build());

        final FlowspecType prots = createProts();
        builder.setFlowspecType(prots);
        fs.add(builder.build());

        final PortCase ps = createPorts();
        builder.setFlowspecType(ps);
        fs.add(builder.build());

        final FlowspecType dps = createDps();
        builder.setFlowspecType(dps);
        fs.add(builder.build());

        final FlowspecType sps = createSps();
        builder.setFlowspecType(sps);
        fs.add(builder.build());

        final FlowspecL3vpnIpv4NlriParser parser = new FlowspecL3vpnIpv4NlriParser(this.flowspecContext.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN));

        final MpReachNlriBuilder result = new MpReachNlriBuilder();
        result.setAfi(Ipv4AddressFamily.class);
        result.setSafi(FlowspecL3vpnSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.wrappedBuffer(REACHED_NLRI_ADD_PATH), result, this.constraint);

        final DestinationFlowspecL3vpnIpv4 flowspecDst = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv4Case) result.getAdvertizedRoutes().getDestinationType())
            .getDestinationFlowspecL3vpnIpv4();
        final List<Flowspec> flows = flowspecDst.getFlowspec();
        final RouteDistinguisher rd = flowspecDst.getRouteDistinguisher();

        testFlows(flows, destinationPrefix, sourcePrefix, prots, rd, ps, dps, sps);

        mp.setAdvertizedRoutes(
            new AdvertizedRoutesBuilder().setDestinationType(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv4CaseBuilder()
                    .setDestinationFlowspecL3vpnIpv4(
                        new DestinationFlowspecL3vpnIpv4Builder()
                            .setRouteDistinguisher(rd)
                            .setPathId(PATH_ID)
                            .setFlowspec(fs)
                            .build()
                    ).build()
            ).build()
        );

        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mp.setAfi(Ipv4AddressFamily.class).build()).build()).build(), buffer);
        assertArrayEquals(REACHED_NLRI_ADD_PATH, ByteArray.readAllBytes(buffer));

        assertEquals("all packets to 10.0.1.0/32 AND from 1.2.3.4/32 AND where IP protocol equals to 6 AND where port is greater than or equals to 137 and is less than or equals to 139 or equals to 8080 AND where destination port is greater than 4089 or equals to 179 AND where source port equals to 8080 ", this.FS_PARSER.stringNlri(flows));
    }

    private static PortCase createPorts() {
        final List<Ports> ports = Lists.newArrayList(new PortsBuilder().setOp(new NumericOperand(false, false, true, true, false)).setValue(137).build(),
            new PortsBuilder().setOp(new NumericOperand(true, false, true, false, true)).setValue(139).build(),
            new PortsBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue(8080).build());

        return new PortCaseBuilder().setPorts(ports).build();
    }

    @Test
    public void testParseMpUnreachNlri() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpUnreachNlriBuilder mp = new MpUnreachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();

        final FlowspecType icmpType = createIcmpType();
        builder.setFlowspecType(icmpType);
        fs.add(builder.build());

        final FlowspecType icmpCode = createIcmpCode();
        builder.setFlowspecType(icmpCode);
        fs.add(builder.build());

        final TcpFlagsCase tcp = createTcp();
        builder.setFlowspecType(tcp);
        fs.add(builder.build());

        final PacketLengthCase packet = createPackets();
        builder.setFlowspecType(packet);
        fs.add(builder.build());

        final FlowspecType dscp = createDscp();
        builder.setFlowspecType(dscp);
        fs.add(builder.build());

        final FlowspecType fragment = createFragment();
        builder.setFlowspecType(fragment);
        fs.add(builder.build());

        final FlowspecL3vpnIpv4NlriParser parser = new FlowspecL3vpnIpv4NlriParser(this.flowspecContext.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN));

        final MpUnreachNlriBuilder result = new MpUnreachNlriBuilder();
        result.setAfi(Ipv4AddressFamily.class);
        result.setSafi(FlowspecL3vpnSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.wrappedBuffer(UNREACHED_NLRI), result);

        DestinationFlowspecL3vpnIpv4 flowspecDst = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv4Case) result.getWithdrawnRoutes().getDestinationType())
            .getDestinationFlowspecL3vpnIpv4();
        final List<Flowspec> flows = flowspecDst.getFlowspec();
        checkUnreachFlows(flows, icmpType, icmpCode, tcp, packet, dscp, fragment);

        final RouteDistinguisher rd = flowspecDst.getRouteDistinguisher();

        mp.setAfi(Ipv4AddressFamily.class).setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv4CaseBuilder()
                .setDestinationFlowspecL3vpnIpv4(
                    new DestinationFlowspecL3vpnIpv4Builder()
                        .setRouteDistinguisher(rd)
                        .setFlowspec(fs)
                        .build()
                ).build()
            ).build()
        );

        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeNlri(new Object[] {rd, flows}, null, buffer);
        assertArrayEquals(UNREACHED_NLRI, ByteArray.readAllBytes(buffer));

        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class, new Attributes2Builder().setMpUnreachNlri(mp.build()).build()).build(), buffer);
        assertArrayEquals(UNREACHED_NLRI, ByteArray.readAllBytes(buffer));

        assertEquals("all packets where ICMP type is less than 2 or is less than 3 AND where ICMP code is less than 4 or 5 AND where TCP flags is not 1025 or does match 22193 AND where packet length is less than 57005 AND where DSCP is greater than 42 AND where fragment does match 'IS FIRST' 'IS LAST' 'IS A' ", this.FS_PARSER.stringNlri(flows));

    }

    private static FlowspecType createFragment() {
        final List<Fragments> fragments = Lists.newArrayList(new FragmentsBuilder().setOp(new BitmaskOperand(false, true, true, false)).setValue(new Fragment(false, true, true, true)).build());
        return new FragmentCaseBuilder().setFragments(fragments).build();
    }

    private static FlowspecType createDscp() {
        final List<Dscps> dscps = Lists.newArrayList(new DscpsBuilder().setOp(new NumericOperand(false, true, false, true, false)).setValue(new Dscp((short) 42)).build());
        return new DscpCaseBuilder().setDscps(dscps).build();
    }

    private static PacketLengthCase createPackets() {
        final List<PacketLengths> packets = Lists.newArrayList(new PacketLengthsBuilder().setOp(new NumericOperand(false, true, false, false, true))
            .setValue(57005).build());
        return new PacketLengthCaseBuilder().setPacketLengths(packets).build();
    }

    private static TcpFlagsCase createTcp() {
        final List<TcpFlags> flags = Lists.newArrayList(new TcpFlagsBuilder().setOp(new BitmaskOperand(false, false, false, true)).setValue(1025).build(),
            new TcpFlagsBuilder().setOp(new BitmaskOperand(false, true, true, false)).setValue(22193).build());
        return new TcpFlagsCaseBuilder().setTcpFlags(flags).build();
    }

    private static FlowspecType createIcmpCode() {
        final List<Codes> codes = Lists.newArrayList(new CodesBuilder().setOp(new NumericOperand(false, false, false, false, true)).setValue((short) 4).build(),
            new CodesBuilder().setOp(new NumericOperand(false, true, false, false, false)).setValue((short) 5).build());
        return new IcmpCodeCaseBuilder().setCodes(codes).build();
    }

    private static FlowspecType createIcmpType() {
        final List<Types> types = Lists.newArrayList(new TypesBuilder().setOp(new NumericOperand(false, false, false, false, true)).setValue((short) 2).build(),
            new TypesBuilder().setOp(new NumericOperand(false, true, false, false, true)).setValue((short) 3).build());
        return new IcmpTypeCaseBuilder().setTypes(types).build();
    }

    private static void checkUnreachFlows(final List<Flowspec> flows, final FlowspecType icmpType,
            final FlowspecType icmpCode, final TcpFlagsCase tcp,
            final PacketLengthCase packet, final FlowspecType dscp, final FlowspecType fragment) {
        assertEquals(6, flows.size());
        assertEquals(icmpType, flows.get(0).getFlowspecType());
        assertEquals(icmpCode, flows.get(1).getFlowspecType());
        assertEquals(tcp, flows.get(2).getFlowspecType());
        assertEquals(packet, flows.get(3).getFlowspecType());
        assertEquals(dscp, flows.get(4).getFlowspecType());
        assertEquals(fragment, flows.get(5).getFlowspecType());
    }

    @Test
    public void testParseMpUnreachNlriConstraint() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpUnreachNlriBuilder mp = new MpUnreachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();

        final FlowspecType icmpType = createIcmpType();
        builder.setFlowspecType(icmpType);
        fs.add(builder.build());

        final FlowspecType icmpCode = createIcmpCode();
        builder.setFlowspecType(icmpCode);
        fs.add(builder.build());

        final TcpFlagsCase tcp = createTcp();
        builder.setFlowspecType(tcp);
        fs.add(builder.build());

        final PacketLengthCase packet = createPackets();
        builder.setFlowspecType(packet);
        fs.add(builder.build());

        final FlowspecType dscp = createDscp();
        builder.setFlowspecType(dscp);
        fs.add(builder.build());

        final FlowspecType fragment = createFragment();
        builder.setFlowspecType(fragment);
        fs.add(builder.build());

        final FlowspecL3vpnIpv4NlriParser parser = new FlowspecL3vpnIpv4NlriParser(this.flowspecContext.getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4, SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC_VPN));

        final MpUnreachNlriBuilder result = new MpUnreachNlriBuilder();
        result.setAfi(Ipv4AddressFamily.class);
        result.setSafi(FlowspecL3vpnSubsequentAddressFamily.class);
        parser.parseNlri(Unpooled.wrappedBuffer(UNREACHED_NLRI_ADD_PATH), result, this.constraint);

        DestinationFlowspecL3vpnIpv4 flowspecDst = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv4Case) result.getWithdrawnRoutes().getDestinationType())
            .getDestinationFlowspecL3vpnIpv4();
        final List<Flowspec> flows = flowspecDst.getFlowspec();
        checkUnreachFlows(flows, icmpType, icmpCode, tcp, packet, dscp, fragment);

        final RouteDistinguisher rd = flowspecDst.getRouteDistinguisher();

        mp.setAfi(Ipv4AddressFamily.class).setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv4CaseBuilder()
                .setDestinationFlowspecL3vpnIpv4(
                    new DestinationFlowspecL3vpnIpv4Builder()
                        .setRouteDistinguisher(rd)
                        .setPathId(PATH_ID)
                        .setFlowspec(fs)
                        .build()
                ).build()
            ).build()
        );

        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeNlri(new Object[] {rd, flows}, PATH_ID, buffer);
        assertArrayEquals(UNREACHED_NLRI_ADD_PATH, ByteArray.readAllBytes(buffer));

        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class, new Attributes2Builder().setMpUnreachNlri(mp.build()).build()).build(), buffer);
        assertArrayEquals(UNREACHED_NLRI_ADD_PATH, ByteArray.readAllBytes(buffer));

        assertEquals("all packets where ICMP type is less than 2 or is less than 3 AND where ICMP code is less than 4 or 5 AND where TCP flags is not 1025 or does match 22193 AND where packet length is less than 57005 AND where DSCP is greater than 42 AND where fragment does match 'IS FIRST' 'IS LAST' 'IS A' ", this.FS_PARSER.stringNlri(flows));

    }

    @Test
    public void testExtractFlowspecDestPrefix() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.leafBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.DEST_PREFIX_NID).withValue("127.0.0.5/32").build()).build()).build()).build());

        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new DestinationPrefixCaseBuilder().setDestinationPrefix(new Ipv4Prefix("127.0.0.5/32")).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourcePrefix() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.leafBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.SOURCE_PREFIX_NID).withValue("127.0.0.6/32").build()).build()).build()).build());

        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new SourcePrefixCaseBuilder().setSourcePrefix(new Ipv4Prefix("127.0.0.6/32")).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecProtocolIps() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(PROTOCOL_IP_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(PROTOCOL_IP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.END_OF_LIST_VALUE, AbstractOperandParser.AND_BIT_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue((short) 100).build()).build())
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(PROTOCOL_IP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.AND_BIT_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue((short) 200).build()).build())
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(PROTOCOL_IP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.END_OF_LIST_VALUE, AbstractOperandParser.AND_BIT_VALUE, AbstractNumericOperandParser.EQUALS_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue((short) 240).build()).build())
                        .build()).build()).build()).build());

        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new ProtocolIpCaseBuilder().setProtocolIps(Lists.newArrayList(
            new ProtocolIpsBuilder().setValue((short) 100).setOp(new NumericOperand(true, true, false, false, false)).build(),
            new ProtocolIpsBuilder().setValue((short) 200).setOp(new NumericOperand(true, false, false, false, false)).build(),
            new ProtocolIpsBuilder().setValue((short) 240).setOp(new NumericOperand(true, true, true, false, false)).build())).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecPorts() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.PORTS_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(PROTOCOL_IP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.END_OF_LIST_VALUE, AbstractOperandParser.AND_BIT_VALUE, AbstractNumericOperandParser.LESS_THAN_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue(100).build()).build())
                        .build()).build()).build()).build());

        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new PortCaseBuilder().setPorts(Lists.newArrayList(new PortsBuilder().setValue(100).setOp(new NumericOperand(true, true, false, false, true)).build())).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecDestinationPorts() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.DEST_PORT_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.OP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.END_OF_LIST_VALUE, AbstractNumericOperandParser.EQUALS_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.VALUE_NID).withValue(1024).build()).build())
                        .build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new DestinationPortCaseBuilder().setDestinationPorts(Lists.newArrayList(new DestinationPortsBuilder().setValue(1024).setOp(new NumericOperand(false, true, true, false, false)).build())).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourcePorts() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.SOURCE_PORT_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.AND_BIT_VALUE, AbstractOperandParser.END_OF_LIST_VALUE, AbstractNumericOperandParser.EQUALS_VALUE, AbstractNumericOperandParser.GREATER_THAN_VALUE, AbstractNumericOperandParser.LESS_THAN_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue(8080).build()).build())
                        .build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new SourcePortCaseBuilder().setSourcePorts(Lists.newArrayList(new SourcePortsBuilder().setValue(8080).setOp(new NumericOperand(true, true, true, true, true)).build())).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourceTypes() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.ICMP_TYPE_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.AND_BIT_VALUE, AbstractOperandParser.END_OF_LIST_VALUE, AbstractNumericOperandParser.EQUALS_VALUE, AbstractNumericOperandParser.GREATER_THAN_VALUE, AbstractNumericOperandParser.LESS_THAN_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue((short) 22).build()).build())
                        .build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new IcmpTypeCaseBuilder().setTypes(Lists.newArrayList(new TypesBuilder().setValue((short) 22).setOp(new NumericOperand(true, true, true, true, true)).build())).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourceCodes() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.ICMP_CODE_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet()).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue((short) 23).build()).build())
                        .build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new IcmpCodeCaseBuilder().setCodes(Lists.newArrayList(new CodesBuilder().setValue((short) 23).setOp(new NumericOperand(false, false, false, false, false)).build())).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourceTcpFlags() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.TCP_FLAGS_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.AND_BIT_VALUE, AbstractOperandParser.END_OF_LIST_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue(99).build()).build())
                        .build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new TcpFlagsCaseBuilder().setTcpFlags(Lists.newArrayList(new TcpFlagsBuilder().setValue(99).setOp(new BitmaskOperand(true, true, false, false)).build())).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecPacketLengths() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.PACKET_LENGTHS_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.AND_BIT_VALUE, AbstractNumericOperandParser.GREATER_THAN_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue(101).build()).build())
                        .build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new PacketLengthCaseBuilder().setPacketLengths(Lists.newArrayList(new PacketLengthsBuilder().setValue(101).setOp(new NumericOperand(true, false, false, true, false)).build())).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecDscps() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.DSCP_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.AND_BIT_VALUE, AbstractOperandParser.END_OF_LIST_VALUE, AbstractNumericOperandParser.GREATER_THAN_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue((short) 15).build()).build())
                        .build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new DscpCaseBuilder().setDscps(Lists.newArrayList(new DscpsBuilder().setValue(new Dscp((short) 15)).setOp(new NumericOperand(true, true, false, true, false)).build())).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecFragments() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(AbstractFlowspecNlriParser.FRAGMENT_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.OP_NID).withValue(Sets.newHashSet(AbstractOperandParser.AND_BIT_VALUE, AbstractOperandParser.END_OF_LIST_VALUE, BitmaskOperandParser.MATCH_VALUE, BitmaskOperandParser.NOT_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv4NlriParser.VALUE_NID).withValue(Sets.newHashSet(AbstractFlowspecNlriParser.DO_NOT_VALUE, AbstractFlowspecNlriParser.FIRST_VALUE, AbstractFlowspecNlriParser.IS_A_VALUE, AbstractFlowspecNlriParser.LAST_VALUE)).build()).build())
                        .build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new FragmentCaseBuilder().setFragments(Lists.newArrayList(new FragmentsBuilder().setValue(new Fragment(true, true, true, true)).setOp(new BitmaskOperand(true, true, true, true)).build())).build());
        final List<Flowspec> expected = new ArrayList<>();
        expected.add(expectedFS.build());
        assertEquals(expected, this.FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecRouteDistinguisher() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(
            Builders.leafBuilder()
                .withNodeIdentifier(RD_NID)
                .withValue(
                    RouteDistinguisherBuilder.getDefaultInstance(ROUTE_DISTINGUISHER)
                ).build()
        );

        RouteDistinguisher rd = RouteDistinguisherBuilder.getDefaultInstance(ROUTE_DISTINGUISHER);
        assertEquals(rd, extractRouteDistinguisher(entry.build(), AbstractFlowspecL3vpnNlriParser.RD_NID));
    }
}

