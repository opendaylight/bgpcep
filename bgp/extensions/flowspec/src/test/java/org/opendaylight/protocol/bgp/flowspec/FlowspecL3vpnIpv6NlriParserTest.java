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
import static org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecIpv4NlriParserTest.PATH_ID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.SAFI;
import org.opendaylight.protocol.bgp.flowspec.handlers.AbstractNumericOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.AbstractOperandParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.BitmaskOperandParser;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv6.FlowspecL3vpnIpv6NlriParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.FragmentCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.fragment._case.Fragments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.flowspec.type.fragment._case.FragmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.FlowLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.NextHeaderCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.NextHeaderCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.next.header._case.NextHeaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.group.ipv6.flowspec.flowspec.type.next.header._case.NextHeadersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.ipv6.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.destination.ipv6.DestinationFlowspecL3vpnIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.destination.ipv6.DestinationFlowspecL3vpnIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesReachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.AttributesUnreachBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.reach.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.attributes.unreach.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class FlowspecL3vpnIpv6NlriParserTest {
    private static final NodeIdentifier NEXT_HEADER_NID = new NodeIdentifier(NextHeaders.QNAME);
    private static final NodeIdentifier FLOW_LABEL_NID = new NodeIdentifier(FlowLabel.QNAME);

    private static final String ROUTE_DISTINGUISHER = "1.2.3.4:10";

    @Mock
    private PeerSpecificParserConstraint constraint;
    @Mock
    private MultiPathSupport muliPathSupport;
    private final FlowspecL3vpnIpv6NlriParser fsParser = new FlowspecL3vpnIpv6NlriParser(SAFI.FLOWSPEC);

    private static final byte[] REACHED_NLRI = new byte[]{
        0x1B,   // NLRI length: 19+8=27
        0, 1, 1, 2, 3, 4, 0, 10,    // route distinguisher: 1.2.3.4:10
        1, 0x28, 0, 1, 2, 3, 4, 5,
        2, 0x28, 0, 1, 2, 3, 4, 6,
        03, (byte) 0x81, 06};

    private static final byte[] REACHED_NLRI_ADD_PATH = new byte[]{
        0x0, 0x0, 0x0, 0x1,
        0x1B,
        0, 1, 1, 2, 3, 4, 0, 10,    // route distinguisher: 1.2.3.4:10
        1, 0x28, 0, 1, 2, 3, 4, 5,
        2, 0x28, 0, 1, 2, 3, 4, 6,
        03, (byte) 0x81, 06};

    private static final byte[] UNREACHED_NLRI = new byte[]{
        0x14,   // NLRI length: 12+8=20
        0, 1, 1, 2, 3, 4, 0, 10,    // route distinguisher: 1.2.3.4:10
        0x0c, (byte) 0x81, 0x0e,
        0x0d, (byte) 0x21, 1, 0, 0, 6, (byte) 0x91, 1, 2
    };

    private static final byte[] UNREACHED_NLRI_ADD_PATH = new byte[]{
        0x0, 0x0, 0x0, 0x1,
        0x14,   // NLRI length: 12+8=20
        0, 1, 1, 2, 3, 4, 0, 10,    // route distinguisher: 1.2.3.4:10
        0x0c, (byte) 0x81, 0x0e,
        0x0d, (byte) 0x21, 1, 0, 0, 6, (byte) 0x91, 1, 2
    };

    @Before
    public void setUp() {
        Mockito.doReturn(Optional.of(muliPathSupport)).when(constraint).getPeerConstraint(Mockito.any());
        Mockito.doReturn(true).when(muliPathSupport).isTableTypeSupported(Mockito.any());
    }

    @Test
    public void testParseMpReachNlri() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpReachNlriBuilder mp = new MpReachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();
        final DestinationIpv6PrefixCase destinationPrefix = new DestinationIpv6PrefixCaseBuilder().setDestinationPrefix(
            new Ipv6Prefix("102:304:500::/40")).build();
        builder.setFlowspecType(destinationPrefix);
        fs.add(builder.build());

        final SourceIpv6PrefixCase sourcePrefix = new SourceIpv6PrefixCaseBuilder().setSourcePrefix(
            new Ipv6Prefix("102:304:600::/40")).build();
        builder.setFlowspecType(sourcePrefix);
        fs.add(builder.build());

        final List<NextHeaders> nextheaders = List.of(new NextHeadersBuilder()
            .setOp(new NumericOperand(false, true, true, false, false)).setValue(Uint8.valueOf(6)).build());
        final NextHeaderCase headersCase = new NextHeaderCaseBuilder().setNextHeaders(nextheaders).build();
        builder.setFlowspecType(headersCase);
        fs.add(builder.build());

        final MpReachNlriBuilder result = new MpReachNlriBuilder()
            .setAfi(Ipv6AddressFamily.VALUE)
            .setSafi(FlowspecL3vpnSubsequentAddressFamily.VALUE);
        fsParser.parseNlri(Unpooled.wrappedBuffer(REACHED_NLRI), result, null);

        DestinationFlowspecL3vpnIpv6 flowspecDst = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type
                .DestinationFlowspecL3vpnIpv6Case) result.getAdvertizedRoutes().getDestinationType())
                .getDestinationFlowspecL3vpnIpv6();
        final List<Flowspec> flows = flowspecDst.getFlowspec();

        final RouteDistinguisher rd = flowspecDst.getRouteDistinguisher();

        assertEquals(3, flows.size());
        assertEquals(ROUTE_DISTINGUISHER, rd.stringValue());
        assertEquals(destinationPrefix, flows.get(0).getFlowspecType());
        assertEquals(sourcePrefix, flows.get(1).getFlowspecType());
        assertEquals(headersCase, flows.get(2).getFlowspecType());

        mp.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new org.opendaylight.yang.gen.v1.urn
            .opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes
            .destination.type.DestinationFlowspecL3vpnIpv6CaseBuilder()
                .setDestinationFlowspecL3vpnIpv6(
                    new DestinationFlowspecL3vpnIpv6Builder()
                        .setRouteDistinguisher(rd)
                        .setFlowspec(fs)
                        .build()
                )
                .build()
            ).build()
        );

        final ByteBuf buffer = Unpooled.buffer();
        fsParser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesReachBuilder()
                .setMpReachNlri(mp.setAfi(Ipv6AddressFamily.VALUE).build())
                .build())
            .build(), buffer);
        assertArrayEquals(REACHED_NLRI, ByteArray.readAllBytes(buffer));

        assertEquals("all packets to 102:304:500::/40 AND from 102:304:600::/40 AND where next header equals to 6 ",
            fsParser.stringNlri(flows));
    }

    @Test
    public void testParseMpReachNlriConstraint() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpReachNlriBuilder mp = new MpReachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();
        final DestinationIpv6PrefixCase destinationPrefix = new DestinationIpv6PrefixCaseBuilder().setDestinationPrefix(
            new Ipv6Prefix("102:304:500::/40")).build();
        builder.setFlowspecType(destinationPrefix);
        fs.add(builder.build());

        final SourceIpv6PrefixCase sourcePrefix = new SourceIpv6PrefixCaseBuilder().setSourcePrefix(
            new Ipv6Prefix("102:304:600::/40")).build();
        builder.setFlowspecType(sourcePrefix);
        fs.add(builder.build());

        final List<NextHeaders> nextheaders = List.of(new NextHeadersBuilder()
            .setOp(new NumericOperand(false, true, true, false, false)).setValue(Uint8.valueOf(6)).build());
        final NextHeaderCase headersCase = new NextHeaderCaseBuilder().setNextHeaders(nextheaders).build();
        builder.setFlowspecType(headersCase);
        fs.add(builder.build());

        final MpReachNlriBuilder result = new MpReachNlriBuilder()
            .setAfi(Ipv6AddressFamily.VALUE)
            .setSafi(FlowspecL3vpnSubsequentAddressFamily.VALUE);
        fsParser.parseNlri(Unpooled.wrappedBuffer(REACHED_NLRI_ADD_PATH), result, constraint);

        final DestinationFlowspecL3vpnIpv6 flowspecDst = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type
                .DestinationFlowspecL3vpnIpv6Case) result.getAdvertizedRoutes().getDestinationType())
                .getDestinationFlowspecL3vpnIpv6();
        final List<Flowspec> flows = flowspecDst.getFlowspec();
        final RouteDistinguisher rd = flowspecDst.getRouteDistinguisher();

        assertEquals(3, flows.size());
        assertEquals(ROUTE_DISTINGUISHER, rd.stringValue());
        assertEquals(destinationPrefix, flows.get(0).getFlowspecType());
        assertEquals(sourcePrefix, flows.get(1).getFlowspecType());
        assertEquals(headersCase, flows.get(2).getFlowspecType());

        mp.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new org.opendaylight.yang.gen.v1.urn
            .opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes
            .destination.type.DestinationFlowspecL3vpnIpv6CaseBuilder()
                .setDestinationFlowspecL3vpnIpv6(
                    new DestinationFlowspecL3vpnIpv6Builder()
                        .setPathId(PATH_ID)
                        .setRouteDistinguisher(rd)
                        .setFlowspec(fs)
                        .build()
                ).build()
            ).build()
        );

        final ByteBuf buffer = Unpooled.buffer();
        fsParser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesReachBuilder()
                .setMpReachNlri(mp.setAfi(Ipv6AddressFamily.VALUE).build())
                .build())
            .build(), buffer);
        assertArrayEquals(REACHED_NLRI_ADD_PATH, ByteArray.readAllBytes(buffer));

        assertEquals("all packets to 102:304:500::/40 AND from 102:304:600::/40 AND where next header equals to 6 ",
            fsParser.stringNlri(flows));
    }

    @Test
    public void testParseMpUnreachNlri() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpUnreachNlriBuilder mp = new MpUnreachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();

        final FragmentCase fragment = createFragment();
        builder.setFlowspecType(fragment);
        fs.add(builder.build());

        final FlowspecType label = createLabel();
        builder.setFlowspecType(label);
        fs.add(builder.build());

        final MpUnreachNlriBuilder result = new MpUnreachNlriBuilder()
            .setAfi(Ipv6AddressFamily.VALUE)
            .setSafi(FlowspecL3vpnSubsequentAddressFamily.VALUE);
        fsParser.parseNlri(Unpooled.wrappedBuffer(UNREACHED_NLRI), result, null);

        final DestinationFlowspecL3vpnIpv6 flowspecDst = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bgp.flowspec.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                .DestinationFlowspecL3vpnIpv6Case) result.getWithdrawnRoutes().getDestinationType())
                .getDestinationFlowspecL3vpnIpv6();
        final List<Flowspec> flows = flowspecDst.getFlowspec();
        final RouteDistinguisher rd = flowspecDst.getRouteDistinguisher();

        assertEquals(2, flows.size());
        assertEquals(ROUTE_DISTINGUISHER, rd.stringValue());
        assertEquals(fragment, flows.get(0).getFlowspecType());
        assertEquals(label, flows.get(1).getFlowspecType());

        mp.setAfi(Ipv6AddressFamily.VALUE).setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv6CaseBuilder()
                .setDestinationFlowspecL3vpnIpv6(
                    new DestinationFlowspecL3vpnIpv6Builder()
                        .setRouteDistinguisher(rd)
                        .setFlowspec(fs)
                        .build()
                ).build()
            ).build()
        );

        final ByteBuf buffer = Unpooled.buffer();
        fsParser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mp.build()).build())
            .build(), buffer);

        assertArrayEquals(UNREACHED_NLRI, ByteArray.readAllBytes(buffer));

        assertEquals("all packets where fragment does match 'IS FIRST' 'IS LAST' 'IS A' AND where flow label equals to "
                + "16777222 or equals to 258 ", fsParser.stringNlri(flows));
    }

    private static FragmentCase createFragment() {
        final List<Fragments> fragments = List.of(new FragmentsBuilder().setOp(
            new BitmaskOperand(false, true, true, false)).setValue(new Fragment(false, true, true, true)).build());
        return new FragmentCaseBuilder().setFragments(fragments).build();
    }

    private static FlowspecType createLabel() {
        final List<FlowLabel> labels = List.of(
            new FlowLabelBuilder().setOp(new NumericOperand(false, false, true, false, false))
                .setValue(Uint32.valueOf(16777222)).build(),
            new FlowLabelBuilder().setOp(new NumericOperand(false, true, true, false, false))
                .setValue(Uint32.valueOf(258)).build());
        return new FlowLabelCaseBuilder().setFlowLabel(labels).build();
    }

    @Test
    public void testParseMpUnreachNlriConstraint() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpUnreachNlriBuilder mp = new MpUnreachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();
        final FragmentCase fragment = createFragment();

        builder.setFlowspecType(fragment);
        fs.add(builder.build());

        final FlowspecType label = createLabel();
        builder.setFlowspecType(label);
        fs.add(builder.build());

        final MpUnreachNlriBuilder result = new MpUnreachNlriBuilder()
            .setAfi(Ipv6AddressFamily.VALUE)
            .setSafi(FlowspecL3vpnSubsequentAddressFamily.VALUE);
        fsParser.parseNlri(Unpooled.wrappedBuffer(UNREACHED_NLRI_ADD_PATH), result, constraint);

        final DestinationFlowspecL3vpnIpv6 flowspecDst = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bgp.flowspec.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                .DestinationFlowspecL3vpnIpv6Case) result.getWithdrawnRoutes().getDestinationType())
                .getDestinationFlowspecL3vpnIpv6();
        final List<Flowspec> flows = flowspecDst.nonnullFlowspec();
        assertEquals(2, flows.size());

        final RouteDistinguisher rd = flowspecDst.getRouteDistinguisher();
        assertEquals(ROUTE_DISTINGUISHER, rd.stringValue());
        assertEquals(fragment, flows.get(0).getFlowspecType());
        assertEquals(label, flows.get(1).getFlowspecType());

        mp.setAfi(Ipv6AddressFamily.VALUE).setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv6CaseBuilder()
                .setDestinationFlowspecL3vpnIpv6(
                    new DestinationFlowspecL3vpnIpv6Builder()
                        .setRouteDistinguisher(rd)
                        .setPathId(PATH_ID)
                        .setFlowspec(fs)
                        .build()
                ).build()
            ).build()
        );

        final ByteBuf buffer = Unpooled.buffer();
        fsParser.serializeAttribute(new AttributesBuilder()
            .addAugmentation(new AttributesUnreachBuilder().setMpUnreachNlri(mp.build()).build())
            .build(), buffer);

        assertArrayEquals(UNREACHED_NLRI_ADD_PATH, ByteArray.readAllBytes(buffer));

        assertEquals("all packets where fragment does match 'IS FIRST' 'IS LAST' 'IS A' AND where flow label equals to "
                + "16777222 or equals to 258 ", fsParser.stringNlri(flows));
    }

    @Test
    public void testExtractFlowspecFragments() {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry =
                Builders.mapEntryBuilder();
        entry.withNodeIdentifier(NodeIdentifierWithPredicates.of(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder()
                        .withNodeIdentifier(AbstractFlowspecNlriParser.FRAGMENT_NID)
                        .withChild(Builders.unkeyedListEntryBuilder()
                            .withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.OP_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.OP_NID)
                                .withValue(Set.of(AbstractOperandParser.AND_BIT_VALUE,
                                    AbstractOperandParser.END_OF_LIST_VALUE, BitmaskOperandParser.MATCH_VALUE,
                                    BitmaskOperandParser.NOT_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.VALUE_NID)
                                .withValue(Set.of(AbstractFlowspecNlriParser.DO_NOT_VALUE,
                                    AbstractFlowspecNlriParser.FIRST_VALUE, AbstractFlowspecNlriParser.IS_A_VALUE,
                                    AbstractFlowspecNlriParser.LAST_VALUE)).build()).build())
                        .build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new FragmentCaseBuilder().setFragments(List.of(
            new FragmentsBuilder().setValue(new Fragment(true, true, true, true)).setOp(
                new BitmaskOperand(true, true, true, true)).build())).build());
        assertEquals(List.of(expectedFS.build()), fsParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecNextHeaders() {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry =
                Builders.mapEntryBuilder();
        entry.withNodeIdentifier(NodeIdentifierWithPredicates.of(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(NEXT_HEADER_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(NEXT_HEADER_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.OP_NID)
                                .withValue(Set.of(AbstractOperandParser.END_OF_LIST_VALUE,
                                    AbstractOperandParser.AND_BIT_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.VALUE_NID)
                                .withValue(Uint8.valueOf(100)).build()).build())
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(NEXT_HEADER_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.OP_NID)
                                .withValue(Set.of(AbstractOperandParser.AND_BIT_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.VALUE_NID)
                                .withValue(Uint8.valueOf(200)).build()).build())
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(NEXT_HEADER_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.OP_NID)
                                .withValue(Set.of(AbstractOperandParser.END_OF_LIST_VALUE,
                                    AbstractOperandParser.AND_BIT_VALUE, AbstractNumericOperandParser.EQUALS_VALUE))
                                .build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.VALUE_NID)
                                .withValue(Uint8.valueOf(210)).build()).build())
                        .build()).build()).build()).build());

        final FlowspecBuilder expectedFS = new FlowspecBuilder()
                .setFlowspecType(new NextHeaderCaseBuilder()
                    .setNextHeaders(List.of(
                        new NextHeadersBuilder().setValue(Uint8.valueOf(100))
                        .setOp(new NumericOperand(true, true, false, false, false)).build(),
                        new NextHeadersBuilder().setValue(Uint8.valueOf(200))
                        .setOp(new NumericOperand(true, false, false, false, false)).build(),
                        new NextHeadersBuilder().setValue(Uint8.valueOf(210))
                        .setOp(new NumericOperand(true, true, true, false, false)).build()))
                    .build());
        assertEquals(List.of(expectedFS.build()), fsParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecFlowLabels() {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry =
                Builders.mapEntryBuilder();
        entry.withNodeIdentifier(NodeIdentifierWithPredicates.of(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FLOW_LABEL_NID)
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FLOW_LABEL_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.OP_NID)
                                .withValue(Set.of(AbstractOperandParser.END_OF_LIST_VALUE,
                                    AbstractOperandParser.AND_BIT_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.VALUE_NID)
                                .withValue(Uint32.valueOf(100)).build()).build())
                        .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FLOW_LABEL_NID)
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.OP_NID)
                                .withValue(Set.of(AbstractOperandParser.AND_BIT_VALUE)).build())
                            .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.VALUE_NID)
                                .withValue(Uint32.valueOf(200)).build()).build())
                        .build()).build()).build()).build());

        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new FlowLabelCaseBuilder().setFlowLabel(List.of(
            new FlowLabelBuilder().setValue(Uint32.valueOf(100))
                .setOp(new NumericOperand(true, true, false, false, false)).build(),
            new FlowLabelBuilder().setValue(Uint32.valueOf(200))
                .setOp(new NumericOperand(true, false, false, false, false)).build()))
            .build());
        assertEquals(List.of(expectedFS.build()), fsParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecDestPrefix() {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry =
                Builders.mapEntryBuilder();
        entry.withNodeIdentifier(NodeIdentifierWithPredicates.of(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.leafBuilder().withNodeIdentifier(FlowspecL3vpnIpv6NlriParser.DEST_PREFIX_NID)
                        .withValue("102:304:500::/40").build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new DestinationIpv6PrefixCaseBuilder().setDestinationPrefix(
            new Ipv6Prefix("102:304:500::/40")).build());
        assertEquals(List.of(expectedFS.build()), fsParser.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourcePrefix() {
        final DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry =
                Builders.mapEntryBuilder();
        entry.withNodeIdentifier(NodeIdentifierWithPredicates.of(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(Builders.unkeyedListBuilder()
            .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
            .withChild(Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_NID)
                .withChild(Builders.choiceBuilder()
                    .withNodeIdentifier(AbstractFlowspecNlriParser.FLOWSPEC_TYPE_NID)
                    .withChild(Builders.leafBuilder().withNodeIdentifier(SimpleFlowspecIpv4NlriParser.SOURCE_PREFIX_NID)
                        .withValue("102:304:600::/40").build()).build()).build()).build());
        final FlowspecBuilder expectedFS = new FlowspecBuilder();
        expectedFS.setFlowspecType(new SourceIpv6PrefixCaseBuilder().setSourcePrefix(new Ipv6Prefix("102:304:600::/40"))
            .build());
        assertEquals(List.of(expectedFS.build()), fsParser.extractFlowspec(entry.build()));
    }
}

