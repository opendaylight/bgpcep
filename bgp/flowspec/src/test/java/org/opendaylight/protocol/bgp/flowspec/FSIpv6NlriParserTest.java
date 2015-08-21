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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.FragmentCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.fragment._case.Fragments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.fragment._case.FragmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.FlowLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.FlowLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.NextHeaderCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.NextHeaderCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.next.header._case.NextHeaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.next.header._case.NextHeadersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec.ipv6._case.DestinationFlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;

public class FSIpv6NlriParserTest {

    private static final FSIpv6NlriParser FS_PARSER = new FSIpv6NlriParser();

    private static final byte[] REACHED_NLRI = new byte[] { 0x13,
        1, 0x28, 0, 1, 2, 3, 4, 5,
        2, 0x28, 0, 1, 2, 3, 4, 6,
        03, (byte) 0x81, 06 };

    private static final byte[] UNREACHED_NLRI = new byte[] { 0x0c,
        0x0c, (byte) 0x81, 0x0e,
        0x0d, (byte) 0x21, 1, 0, 0, 6, (byte) 0x91, 1, 2 };

    @Test
    public void testParseMpReachNlri() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpReachNlriBuilder mp = new MpReachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();
        final DestinationIpv6PrefixCase destinationPrefix = new DestinationIpv6PrefixCaseBuilder().setDestinationPrefix(new Ipv6Prefix("102:304:500::/40")).build();
        builder.setFlowspecType(destinationPrefix);
        fs.add(builder.build());
        final SourceIpv6PrefixCase sourcePrefix = new SourceIpv6PrefixCaseBuilder().setSourcePrefix(new Ipv6Prefix("102:304:600::/40")).build();
        builder.setFlowspecType(sourcePrefix);
        fs.add(builder.build());

        final List<NextHeaders> nextheaders = Lists.newArrayList(new NextHeadersBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue((short) 6).build());
        final NextHeaderCase headersCase = new NextHeaderCaseBuilder().setNextHeaders(nextheaders).build();
        builder.setFlowspecType(headersCase);
        fs.add(builder.build());


        mp.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(new DestinationFlowspecIpv6CaseBuilder().setDestinationFlowspec(new DestinationFlowspecBuilder().setFlowspec(fs).build()).build()).build());

        final MpReachNlriBuilder result = new MpReachNlriBuilder();
        result.setAfi(Ipv6AddressFamily.class);
        FS_PARSER.parseNlri(Unpooled.wrappedBuffer(REACHED_NLRI), result);

        final List<Flowspec> flows = ((DestinationFlowspecIpv6Case) (result.getAdvertizedRoutes().getDestinationType())).getDestinationFlowspec().getFlowspec();
        assertEquals(3, flows.size());
        assertEquals(destinationPrefix, flows.get(0).getFlowspecType());
        assertEquals(sourcePrefix, flows.get(1).getFlowspecType());
        assertEquals(headersCase, flows.get(2).getFlowspecType());

        final ByteBuf buffer = Unpooled.buffer();
        FS_PARSER.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes1.class, new Attributes1Builder().setMpReachNlri(mp.setAfi(Ipv6AddressFamily.class).build()).build()).build(), buffer);
        assertArrayEquals(REACHED_NLRI, ByteArray.readAllBytes(buffer));

        assertEquals("all packets to 102:304:500::/40", FS_PARSER.stringNlri(flows.get(0)));
        assertEquals("all packets from 102:304:600::/40", FS_PARSER.stringNlri(flows.get(1)));
        assertEquals("all packets where next header equals to 6 ", FS_PARSER.stringNlri(flows.get(2)));
    }

    @Test
    public void testParseMpUnreachNlri() throws BGPParsingException {
        final List<Flowspec> fs = new ArrayList<>();
        final MpUnreachNlriBuilder mp = new MpUnreachNlriBuilder();

        final FlowspecBuilder builder = new FlowspecBuilder();

        final List<Fragments> fragments = Lists.newArrayList(new FragmentsBuilder().setOp(new BitmaskOperand(false, true, true, false)).setValue(new Fragment(false, true, true, true)).build());
        final FragmentCase fragment = new FragmentCaseBuilder().setFragments(fragments).build();
        builder.setFlowspecType(fragment);
        fs.add(builder.build());

        final List<FlowLabel> labels = Lists.newArrayList();
        labels.add(new FlowLabelBuilder().setOp(new NumericOperand(false, false, true, false, false)).setValue(new Long(16777222L)).build());
        labels.add(new FlowLabelBuilder().setOp(new NumericOperand(false, true, true, false, false)).setValue(new Long(258L)).build());
        final FlowLabelCase label = new FlowLabelCaseBuilder().setFlowLabel(labels).build();
        builder.setFlowspecType(label);
        fs.add(builder.build());

        mp.setAfi(Ipv6AddressFamily.class).setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6CaseBuilder().setDestinationFlowspec(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec.ipv6._case.DestinationFlowspecBuilder().setFlowspec(fs).build()).build()).build());

        final MpUnreachNlriBuilder result = new MpUnreachNlriBuilder();
        result.setAfi(Ipv6AddressFamily.class);
        FS_PARSER.parseNlri(Unpooled.wrappedBuffer(UNREACHED_NLRI), result);

        final List<Flowspec> flows = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6Case) (result.getWithdrawnRoutes().getDestinationType())).getDestinationFlowspec().getFlowspec();
        assertEquals(2, flows.size());
        assertEquals(fragment, flows.get(0).getFlowspecType());
        assertEquals(label, flows.get(1).getFlowspecType());

        final ByteBuf buffer = Unpooled.buffer();
        FS_PARSER.serializeAttribute(new AttributesBuilder().addAugmentation(Attributes2.class, new Attributes2Builder().setMpUnreachNlri(mp.build()).build()).build(), buffer);

        assertArrayEquals(UNREACHED_NLRI, ByteArray.readAllBytes(buffer));

        assertEquals("all packets where fragment does match 'IS FIRST' 'IS LAST' 'IS A' ", FS_PARSER.stringNlri(flows.get(0)));
        assertEquals("all packets where flow label equals to 16777222 or equals to 258 ", FS_PARSER.stringNlri(flows.get(1)));
    }

    @Test
    public void testExtractFlowspecFragments() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSIpv6NlriParser.FRAGMENT_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSIpv6NlriParser.OP_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.OP_NID).withValue(Sets.<String>newHashSet(AbstractOperandParser.AND_BIT_VALUE, AbstractOperandParser.END_OF_LIST_VALUE, BitmaskOperandParser.MATCH_VALUE, BitmaskOperandParser.NOT_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.VALUE_NID).withValue(Sets.newHashSet(FSIpv6NlriParser.DO_NOT_VALUE, FSIpv6NlriParser.FIRST_VALUE, FSIpv6NlriParser.IS_A_VALUE, FSIpv6NlriParser.LAST_VALUE)).build()).build())
                .build()).build()).build();
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new FragmentCaseBuilder().setFragments(Lists.<Fragments>newArrayList(new FragmentsBuilder().setValue(new Fragment(true, true, true, true)).setOp(new BitmaskOperand(true, true, true, true)).build())).build());
        assertEquals(expected.build(), FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecNextHeaders() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSIpv6NlriParser.NEXT_HEADER_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSIpv6NlriParser.NEXT_HEADER_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.OP_NID).withValue(Sets.<String>newHashSet(AbstractOperandParser.END_OF_LIST_VALUE, AbstractOperandParser.AND_BIT_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.VALUE_NID).withValue((short) 100).build()).build())
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSIpv6NlriParser.NEXT_HEADER_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.OP_NID).withValue(Sets.<String>newHashSet(AbstractOperandParser.AND_BIT_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.VALUE_NID).withValue((short) 200).build()).build())
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSIpv6NlriParser.NEXT_HEADER_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.OP_NID).withValue(Sets.<String>newHashSet(AbstractOperandParser.END_OF_LIST_VALUE, AbstractOperandParser.AND_BIT_VALUE, AbstractNumericOperandParser.EQUALS_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.VALUE_NID).withValue((short) 210).build()).build())
                .build()).build());

        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new NextHeaderCaseBuilder().setNextHeaders(Lists.<NextHeaders>newArrayList(
                new NextHeadersBuilder().setValue((short) 100).setOp(new NumericOperand(true, true, false, false, false)).build(),
                new NextHeadersBuilder().setValue((short) 200).setOp(new NumericOperand(true, false, false, false, false)).build(),
                new NextHeadersBuilder().setValue((short) 210).setOp(new NumericOperand(true, true, true, false, false)).build())).build());
        assertEquals(expected.build(), FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecFlowLabels() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(FSIpv6NlriParser.FLOW_LABEL_NID)
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSIpv6NlriParser.FLOW_LABEL_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.OP_NID).withValue(Sets.<String>newHashSet(AbstractOperandParser.END_OF_LIST_VALUE, AbstractOperandParser.AND_BIT_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.VALUE_NID).withValue(100L).build()).build())
                .withChild(Builders.unkeyedListEntryBuilder().withNodeIdentifier(FSIpv6NlriParser.FLOW_LABEL_NID)
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.OP_NID).withValue(Sets.<String>newHashSet(AbstractOperandParser.AND_BIT_VALUE)).build())
                        .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.VALUE_NID).withValue(200L).build()).build())
                .build()).build());

        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new FlowLabelCaseBuilder().setFlowLabel(Lists.<FlowLabel>newArrayList(
                new FlowLabelBuilder().setValue(100L).setOp(new NumericOperand(true, true, false, false, false)).build(),
                new FlowLabelBuilder().setValue(200L).setOp(new NumericOperand(true, false, false, false, false)).build())).build());
        assertEquals(expected.build(), FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecDestPrefix() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv6NlriParser.DEST_PREFIX_NID).withValue("102:304:500::/40").build()).build());
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new DestinationIpv6PrefixCaseBuilder().setDestinationPrefix(new Ipv6Prefix("102:304:500::/40")).build());
        assertEquals(expected.build(), FS_PARSER.extractFlowspec(entry.build()));
    }

    @Test
    public void testExtractFlowspecSourcePrefix() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(Flowspec.QNAME, Flowspec.QNAME, entry));
        entry.withChild(Builders.choiceBuilder().withNodeIdentifier(new NodeIdentifier(FlowspecType.QNAME))
            .withChild(Builders.leafBuilder().withNodeIdentifier(FSIpv4NlriParser.SOURCE_PREFIX_NID).withValue("102:304:600::/40").build()).build());
        final FlowspecBuilder expected = new FlowspecBuilder();
        expected.setFlowspecType(new SourceIpv6PrefixCaseBuilder().setSourcePrefix(new Ipv6Prefix("102:304:600::/40")).build());
        assertEquals(expected.build(), FS_PARSER.extractFlowspec(entry.build()));
    }
}
