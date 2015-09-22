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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.ComponentType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DestinationPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DestinationPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.DscpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.FragmentCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.IcmpCodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.IcmpCodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.IcmpTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.IcmpTypeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.PacketLengthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.PacketLengthCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.PortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.ProtocolIpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.ProtocolIpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.SourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.SourcePortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.SourcePrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.TcpFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.TcpFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.destination.port._case.DestinationPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.destination.port._case.DestinationPortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.dscp._case.Dscps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.dscp._case.DscpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.fragment._case.Fragments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.fragment._case.FragmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.code._case.Codes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.code._case.CodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.type._case.Types;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.icmp.type._case.TypesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.packet.length._case.PacketLengths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.packet.length._case.PacketLengthsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.port._case.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.port._case.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.protocol.ip._case.ProtocolIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.protocol.ip._case.ProtocolIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.source.port._case.SourcePorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.source.port._case.SourcePortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;

public class FSNlriParserTest {

    private static final byte[] nlri = new byte[] { 0x1D, 01, 0x18, 0x0a, 00, 01, 02, 0x08, (byte) 0xc0,
        03, (byte) 0x81, 06, 04, 03, (byte) 0x89, 0x45, (byte) 0x8b, (byte) 0x91, 0x1f, (byte) 0x90,
        05, 0x12, 0x0f, (byte) 0xf9, (byte) 0x81, (byte) 0xb3,
        06, (byte) 0x91, 0x1f, (byte) 0x90 };

    private static final byte[] unnlri = new byte[] { 0x1B, 07, 4, 2, (byte) 0x84, 3,
        0x08, 06, 04, (byte) 0x80, 05,
        0x09, 0x12, 04, 01, (byte) 0x91, 0x56, (byte) 0xb1,
        0x0a, (byte) 0x96, (byte) 0xde, (byte) 0xad,
        0x0b, (byte) 0x86, 0x2a,
        0x0c, (byte) 0x81, (byte) 0x0f };

    @Test
    public void testParseLength() {
        // 00-00-0000 = 1
        assertEquals(1, FSNlriParser.parseLength((byte) 0x00));
        // 00-01-0000 = 2
        assertEquals(2, FSNlriParser.parseLength((byte) 16));
        // 00-10-0000 = 4
        assertEquals(4, FSNlriParser.parseLength((byte) 32));
        // 00-11-0000 = 8
        assertEquals(8, FSNlriParser.parseLength((byte) 48));
    }

    @Test
    public void testParseMpReachNlri() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpReachNlriBuilder mp = new MpReachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();
        builder.setComponentType(ComponentType.DestinationPrefix);
        final DestinationPrefixCase destinationPrefix = new DestinationPrefixCaseBuilder().setDestinationPrefix(new Ipv4Prefix("10.0.1.0/24")).build();
        builder.setFlowspecType(destinationPrefix);
        fs.add(builder.build());
        builder.setComponentType(ComponentType.SourcePrefix);
        final SourcePrefixCase sourcePrefix = new SourcePrefixCaseBuilder().setSourcePrefix(new Ipv4Prefix("192.0.0.0/8")).build();
        builder.setFlowspecType(sourcePrefix);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.ProtocolIp);
        final List<ProtocolIps> protocols = Lists.newArrayList(new ProtocolIpsBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue(6).build());
        final ProtocolIpCase prots = new ProtocolIpCaseBuilder().setProtocolIps(protocols).build();
        builder.setFlowspecType(prots);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.Port);
        final List<Ports> ports = Lists.newArrayList(new PortsBuilder().setOp(new NumericOperand(false, false, true, true, false)).setValue(137).build(),
            new PortsBuilder().setOp(new NumericOperand(true, false, true, false, true)).setValue(139).build(),
            new PortsBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue(8080).build());
        final PortCase ps = new PortCaseBuilder().setPorts(ports).build();
        builder.setFlowspecType(ps);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.DestinationPort);
        final List<DestinationPorts> destports = Lists.newArrayList(new DestinationPortsBuilder().setOp(new NumericOperand(false, false, false, true, false)).setValue(4089).build(),
            new DestinationPortsBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue(179).build());
        final DestinationPortCase dps = new DestinationPortCaseBuilder().setDestinationPorts(destports).build();
        builder.setFlowspecType(dps);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.SourcePort);
        final List<SourcePorts> sports = Lists.newArrayList(new SourcePortsBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue(8080).build());
        final SourcePortCase sps = new SourcePortCaseBuilder().setSourcePorts(sports).build();
        builder.setFlowspecType(sps);
        fs.add(builder.build());

        mp.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationFlowspecCaseBuilder().setDestinationFlowspec(new DestinationFlowspecBuilder().setFlowspec(fs).build()).build()).build());

        final FSNlriParser parser = new FSNlriParser();

        final MpReachNlriBuilder result = new MpReachNlriBuilder();
        parser.parseNlri(Unpooled.wrappedBuffer(nlri), result);

        final List<Flowspec> flows = ((DestinationFlowspecCase) (result.getAdvertizedRoutes().getDestinationType())).getDestinationFlowspec().getFlowspec();
        assertEquals(6, flows.size());
        assertEquals(destinationPrefix, flows.get(0).getFlowspecType());
        assertEquals(sourcePrefix, flows.get(1).getFlowspecType());
        assertEquals(prots, flows.get(2).getFlowspecType());
        assertEquals(ps, flows.get(3).getFlowspecType());
        assertEquals(dps, flows.get(4).getFlowspecType());
        assertEquals(sps, flows.get(5).getFlowspecType());

        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mp.build()).build()).build(), buffer);
        assertArrayEquals(nlri, ByteArray.readAllBytes(buffer));
        assertEquals("all packets to 10.0.1.0/24 AND from 192.0.0.0/8 AND where protocol equals to 6 AND where port is greater than or equal to 137 and is less than or equal to 139 or equals to 8080 AND where destination port is greater than 4089 or equals to 179 AND where source port equals to 8080 ", FSNlriParser.stringNlri(flows));
    }

    @Test
    public void testParseMpUnreachNlri() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpUnreachNlriBuilder mp = new MpUnreachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();

        builder.setComponentType(ComponentType.IcmpType);
        final List<Types> types = Lists.newArrayList(new TypesBuilder().setOp(new NumericOperand(false, false, false, false, true)).setValue((short) 2).build(),
            new TypesBuilder().setOp(new NumericOperand(false, true, false, false, true)).setValue((short) 3).build());
        final IcmpTypeCase icmpType = new IcmpTypeCaseBuilder().setTypes(types).build();
        builder.setFlowspecType(icmpType);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.IcmpCode);
        final List<Codes> codes = Lists.newArrayList(new CodesBuilder().setOp(new NumericOperand(false, false, false, true, true)).setValue((short) 4).build(),
            new CodesBuilder().setOp(new NumericOperand(false, true, false, false, false)).setValue((short) 5).build());
        final IcmpCodeCase icmpCode = new IcmpCodeCaseBuilder().setCodes(codes).build();
        builder.setFlowspecType(icmpCode);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.TcpFlags);
        final List<TcpFlags> flags = Lists.newArrayList(new TcpFlagsBuilder().setOp(new BitmaskOperand(false, false, false, true)).setValue(1025).build(),
            new TcpFlagsBuilder().setOp(new BitmaskOperand(false, true, true, false)).setValue(22193).build());
        final TcpFlagsCase tcp = new TcpFlagsCaseBuilder().setTcpFlags(flags).build();
        builder.setFlowspecType(tcp);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.PacketLength);
        final List<PacketLengths> packets = Lists.newArrayList(new PacketLengthsBuilder().setOp(new NumericOperand(false, true, false, true, true)).setValue(57005).build());
        final PacketLengthCase packet = new PacketLengthCaseBuilder().setPacketLengths(packets).build();
        builder.setFlowspecType(packet);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.Dscp);
        final List<Dscps> dscps = Lists.newArrayList(new DscpsBuilder().setOp(new NumericOperand(false, true, false, true, true)).setValue(new Dscp((short) 42)).build());
        final DscpCase dscp = new DscpCaseBuilder().setDscps(dscps).build();
        builder.setFlowspecType(dscp);
        fs.add(builder.build());

        builder.setComponentType(ComponentType.Fragment);
        final List<Fragments> fragments = Lists.newArrayList(new FragmentsBuilder().setOp(new BitmaskOperand(false, true, true, false)).setValue(new Fragment(true, true, true, true)).build());
        final FragmentCase fragment = new FragmentCaseBuilder().setFragments(fragments).build();
        builder.setFlowspecType(fragment);
        fs.add(builder.build());

        mp.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCaseBuilder().setDestinationFlowspec(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder().setFlowspec(fs).build()).build()).build());

        final FSNlriParser parser = new FSNlriParser();

        final MpUnreachNlriBuilder result = new MpUnreachNlriBuilder();
        parser.parseNlri(Unpooled.wrappedBuffer(unnlri), result);

        final List<Flowspec> flows = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCase) (result.getWithdrawnRoutes().getDestinationType())).getDestinationFlowspec().getFlowspec();
        assertEquals(6, flows.size());
        assertEquals(icmpType, flows.get(0).getFlowspecType());
        assertEquals(icmpCode, flows.get(1).getFlowspecType());
        assertEquals(tcp, flows.get(2).getFlowspecType());
        assertEquals(packet, flows.get(3).getFlowspecType());
        assertEquals(dscp, flows.get(4).getFlowspecType());
        assertEquals(fragment, flows.get(5).getFlowspecType());

        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class, new Attributes2Builder().setMpUnreachNlri(mp.build()).build()).build(), buffer);
        assertArrayEquals(unnlri, ByteArray.readAllBytes(buffer));
        assertEquals("all packets where ICMP type is less than 2 or is less than 3 AND where ICMP code is less than is greater than 4 or 5 AND where TCP flags is not 1025 or does match 22193 AND where packet length is less than is greater than 57005 AND where DSCP is less than is greater than 42 AND where fragment does match 'DO NOT' 'IS FIRST' 'IS LAST' 'IS A' ", FSNlriParser.stringNlri(flows));
    }

    @Test
    public void testExtractFlowspecDestPrefix() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("destination-prefix").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.DEST_PREFIX_NID).withValue("127.0.0.5/32").build()).build());
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setComponentType(ComponentType.DestinationPrefix);
        expected.setFlowspecType(new DestinationPrefixCaseBuilder().setDestinationPrefix(new Ipv4Prefix("127.0.0.5/32")).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourcePrefix() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("source-prefix").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.SOURCE_PREFIX_NID).withValue("127.0.0.6/32").build()).build());
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setComponentType(ComponentType.SourcePrefix);
        expected.setFlowspecType(new SourcePrefixCaseBuilder().setSourcePrefix(new Ipv4Prefix("127.0.0.6/32")).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecProtocolIps() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("protocol-ips").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSNlriParser.PROTOCOL_IP_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.PROTOCOL_IP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.END_OF_LIST_VALUE, FSNlriParser.AND_BIT_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue(100).build()).build())
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.PROTOCOL_IP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.AND_BIT_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue(200).build()).build())
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.PROTOCOL_IP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.END_OF_LIST_VALUE, FSNlriParser.AND_BIT_VALUE, FSNlriParser.EQUALS_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue(300).build()).build())
                .build()).build());

        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new ProtocolIpCaseBuilder().setProtocolIps(Lists.<ProtocolIps>newArrayList(
                new ProtocolIpsBuilder().setValue(100).setOp(new NumericOperand(true, true, false, false, false)).build(),
                new ProtocolIpsBuilder().setValue(200).setOp(new NumericOperand(true, false, false, false, false)).build(),
                new ProtocolIpsBuilder().setValue(300).setOp(new NumericOperand(true, true, true, false, false)).build())).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecPorts() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("ports").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSNlriParser.PORTS_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.PROTOCOL_IP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.END_OF_LIST_VALUE, FSNlriParser.AND_BIT_VALUE, FSNlriParser.LESS_THAN_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue(100).build()).build())
                .build()).build()).build();
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new PortCaseBuilder().setPorts(Lists.<Ports>newArrayList(new PortsBuilder().setValue(100).setOp(new NumericOperand(true, true, false, false, true)).build())).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecDestinationPorts() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("destination-ports").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSNlriParser.DEST_PORT_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.OP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.END_OF_LIST_VALUE, FSNlriParser.EQUALS_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue(1024).build()).build())
                .build()).build()).build();
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new DestinationPortCaseBuilder().setDestinationPorts(Lists.<DestinationPorts>newArrayList(new DestinationPortsBuilder().setValue(1024).setOp(new NumericOperand(false, true, true, false, false)).build())).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourcePorts() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("source-ports").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSNlriParser.SOURCE_PORT_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.OP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.AND_BIT_VALUE, FSNlriParser.END_OF_LIST_VALUE, FSNlriParser.EQUALS_VALUE, FSNlriParser.GREATER_THAN_VALUE, FSNlriParser.LESS_THAN_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue(8080).build()).build())
                .build()).build()).build();
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new SourcePortCaseBuilder().setSourcePorts(Lists.<SourcePorts>newArrayList(new SourcePortsBuilder().setValue(8080).setOp(new NumericOperand(true, true, true, true, true)).build())).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourceTypes() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("types").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSNlriParser.ICMP_TYPE_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.OP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.AND_BIT_VALUE, FSNlriParser.END_OF_LIST_VALUE, FSNlriParser.EQUALS_VALUE, FSNlriParser.GREATER_THAN_VALUE, FSNlriParser.LESS_THAN_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue((short) 22).build()).build())
                .build()).build()).build();
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new IcmpTypeCaseBuilder().setTypes(Lists.<Types>newArrayList(new TypesBuilder().setValue((short) 22).setOp(new NumericOperand(true, true, true, true, true)).build())).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourceCodes() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("codes").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSNlriParser.ICMP_CODE_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.OP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet()).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue((short) 23).build()).build())
                .build()).build()).build();
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new IcmpCodeCaseBuilder().setCodes(Lists.<Codes>newArrayList(new CodesBuilder().setValue((short) 23).setOp(new NumericOperand(false, false, false, false, false)).build())).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourceTcpFlags() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("tcp-flags").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSNlriParser.TCP_FLAGS_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.OP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.AND_BIT_VALUE, FSNlriParser.END_OF_LIST_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue(99).build()).build())
                .build()).build()).build();
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setComponentType(ComponentType.TcpFlags).setFlowspecType(new TcpFlagsCaseBuilder().setTcpFlags(Lists.<TcpFlags>newArrayList(new TcpFlagsBuilder().setValue(99).setOp(new BitmaskOperand(true, true, false, false)).build())).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecPacketLengths() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("packet-lengths").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSNlriParser.PACKET_LENGTHS_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.OP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.AND_BIT_VALUE, FSNlriParser.GREATER_THAN_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue(101).build()).build())
               .build()).build()).build();
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new PacketLengthCaseBuilder().setPacketLengths(Lists.<PacketLengths>newArrayList(new PacketLengthsBuilder().setValue(101).setOp(new NumericOperand(true, false, false, true, false)).build())).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecDscps() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("packet-lengths").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSNlriParser.DSCP_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.OP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.AND_BIT_VALUE, FSNlriParser.END_OF_LIST_VALUE, FSNlriParser.GREATER_THAN_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue((short)15).build()).build())
               .build()).build()).build();
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new DscpCaseBuilder().setDscps(Lists.<Dscps>newArrayList(new DscpsBuilder().setValue(new Dscp((short)15)).setOp(new NumericOperand(true, true, false, true, false)).build())).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecFragments() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.COMPONENT_TYPE_NID).withValue("packet-lengths").build());
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSNlriParser.FRAGMENT_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSNlriParser.OP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.OP_NID).withValue(Sets.<String>newHashSet(FSNlriParser.AND_BIT_VALUE, FSNlriParser.END_OF_LIST_VALUE, FSNlriParser.MATCH_VALUE, FSNlriParser.NOT_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSNlriParser.VALUE_NID).withValue(Sets.newHashSet(FSNlriParser.DO_NOT_VALUE, FSNlriParser.FIRST_VALUE, FSNlriParser.IS_A_VALUE, FSNlriParser.LAST_VALUE)).build()).build())
                .build()).build()).build();
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new FragmentCaseBuilder().setFragments(Lists.<Fragments>newArrayList(new FragmentsBuilder().setValue(new Fragment(true, true, true, true)).setOp(new BitmaskOperand(true, true, true, true)).build())).build());
        assertEquals(expected.build(), FSNlriParser.extractFlowspec(entry.build()));
    }
}
