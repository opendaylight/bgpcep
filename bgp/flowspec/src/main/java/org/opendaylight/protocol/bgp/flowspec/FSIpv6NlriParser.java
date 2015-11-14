/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.base.Optional;
import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.FragmentCaseBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec.ipv6._case.DestinationFlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

public final class FSIpv6NlriParser extends AbstractFSNlriParser {

    private static final int NEXT_HEADER_VALUE = 3;
    private static final int FLOW_LABLE_VALUE = 13;

    static final NodeIdentifier NEXT_HEADER_NID = new NodeIdentifier(NextHeaders.QNAME);
    static final NodeIdentifier FLOW_LABEL_NID = new NodeIdentifier(FlowLabel.QNAME);

    @Override
    protected void serializeMpReachNlri(final Attributes1 pathAttributes, final ByteBuf byteAggregator) {
        if (pathAttributes == null) {
            return;
        }
        final AdvertizedRoutes routes = (pathAttributes.getMpReachNlri()).getAdvertizedRoutes();
        if (routes != null && routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case flowspecCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case) routes.getDestinationType();
            serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
        }
    }

    @Override
    protected void serializeMpUnreachNlri(final Attributes2 pathAttributes, final ByteBuf byteAggregator) {
        if (pathAttributes == null) {
            return;
        }
        final MpUnreachNlri mpUnreachNlri = pathAttributes.getMpUnreachNlri();
        if (mpUnreachNlri.getWithdrawnRoutes() != null && mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationFlowspecIpv6Case) {
            final DestinationFlowspecIpv6Case flowspecCase = (DestinationFlowspecIpv6Case) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
            serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
        }
    }

    @Override
    protected void serializeSpecificFSType(final FlowspecType value, final ByteBuf nlriByteBuf) {
        if (value instanceof DestinationIpv6PrefixCase) {
            nlriByteBuf.writeByte(DESTINATION_PREFIX_VALUE);
            nlriByteBuf.writeBytes(insertOffsetByte(Ipv6Util.bytesForPrefixBegin(((DestinationIpv6PrefixCase) value).getDestinationPrefix())));
        } else if (value instanceof SourceIpv6PrefixCase) {
            nlriByteBuf.writeByte(SOURCE_PREFIX_VALUE);
            nlriByteBuf.writeBytes(insertOffsetByte(Ipv6Util.bytesForPrefixBegin(((SourceIpv6PrefixCase) value).getSourcePrefix())));
        } else if (value instanceof NextHeaderCase) {
            nlriByteBuf.writeByte(NEXT_HEADER_VALUE);
            NumericOneByteOperandParser.INSTANCE.serialize(((NextHeaderCase) value).getNextHeaders(), nlriByteBuf);
        } else if (value instanceof FragmentCase) {
            nlriByteBuf.writeByte(FRAGMENT_VALUE);
            serializeFragments(((FragmentCase) value).getFragments(), nlriByteBuf);
        } else if (value instanceof FlowLabelCase) {
            nlriByteBuf.writeByte(FLOW_LABLE_VALUE);
            serializeNumericFourByteValue(((FlowLabelCase) value).getFlowLabel(), nlriByteBuf);
        }
    }

    private static void serializeNumericFourByteValue(final List<FlowLabel> list, final ByteBuf nlriByteBuf) {
        for (final FlowLabel item : list) {
            final ByteBuf protoBuf = Unpooled.buffer();
            Util.writeShortest(item.getValue().intValue(), protoBuf);
            NumericOneByteOperandParser.INSTANCE.serialize(item.getOp(), protoBuf.readableBytes(), nlriByteBuf);
            nlriByteBuf.writeBytes(protoBuf);
        }
    }

    private static byte[] insertOffsetByte(final byte[] ipPrefix) {
        // income <len, prefix>
        return Bytes.concat(new byte[] { ipPrefix[0] }, new byte[] { 0 }, ByteArray.subByte(ipPrefix, 1 , ipPrefix.length-1));
    }

    @Override
    protected byte serializeFragment(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Fragment fragment) {
        final BitArray bs = new BitArray(Byte.SIZE);
        bs.set(DONT_FRAGMENT, Boolean.FALSE);
        bs.set(FIRST_FRAGMENT, fragment.isFirst());
        bs.set(IS_A_FRAGMENT, fragment.isIsA());
        bs.set(LAST_FRAGMENT, fragment.isLast());
        return bs.toByte();
    }

    @Override
    DestinationType createWidthdrawnDestinationType(final List<Flowspec> dst) {
        return new DestinationFlowspecIpv6CaseBuilder().setDestinationFlowspec(
            new DestinationFlowspecBuilder().setFlowspec(
                dst).build()).build();
    }

    @Override
    DestinationType createAdvertizedRoutesDestinationType(final List<Flowspec> dst) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6CaseBuilder()
        .setDestinationFlowspec(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec.ipv6._case.DestinationFlowspecBuilder()
        .setFlowspec(dst).build()).build();
    }

    @Override
    protected void setSpecificFlowspecType(final FlowspecBuilder builder, final short type, final ByteBuf nlri) {
        switch (type) {
        case DESTINATION_PREFIX_VALUE:
            builder.setFlowspecType(new DestinationIpv6PrefixCaseBuilder().setDestinationPrefix(parseIpv6Prefix(nlri)).build());
            break;
        case SOURCE_PREFIX_VALUE:
            builder.setFlowspecType(new SourceIpv6PrefixCaseBuilder().setSourcePrefix(parseIpv6Prefix(nlri)).build());
            break;
        case NEXT_HEADER_VALUE:
            builder.setFlowspecType(new NextHeaderCaseBuilder().setNextHeaders(parseNextHeader(nlri)).build());
            break;
        case FRAGMENT_VALUE:
            builder.setFlowspecType(new FragmentCaseBuilder().setFragments(parseFragment(nlri)).build());
            break;
        case FLOW_LABLE_VALUE:
            builder.setFlowspecType(new FlowLabelCaseBuilder().setFlowLabel(parseFlowLabel(nlri)).build());
            break;
        default:
            break;
        }
    }

    private static Ipv6Prefix parseIpv6Prefix(final ByteBuf nlri) {
        final int bitLength = nlri.readUnsignedByte();
        // in case of ipv6 prefix we don't need to skip offset
        nlri.readUnsignedByte();
        return Ipv6Util.prefixForBytes(ByteArray.readBytes(nlri, bitLength / Byte.SIZE), bitLength);
    }

    @Override
    protected  Fragment parseFragment(final byte fragment) {
        final BitArray bs = BitArray.valueOf(fragment);
        return new Fragment(Boolean.FALSE, bs.get(FIRST_FRAGMENT), bs.get(IS_A_FRAGMENT), bs.get(LAST_FRAGMENT));
    }

    private static List<NextHeaders> parseNextHeader(final ByteBuf nlri) {
        final List<NextHeaders> headers = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final NextHeadersBuilder builder = new NextHeadersBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            builder.setValue(nlri.readUnsignedByte());
            end = op.isEndOfList();
            headers.add(builder.build());
        }
        return headers;
    }

    private static List<FlowLabel> parseFlowLabel(final ByteBuf nlri) {
        final List<FlowLabel> labels = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final FlowLabelBuilder builder = new FlowLabelBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            final short length = AbstractOperandParser.parseLength(b);
            builder.setValue(ByteArray.bytesToLong(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            labels.add(builder.build());
        }
        return labels;
    }

    @Override
    public void extractSpecificFlowspec(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        if (fsType.getChild(DEST_PREFIX_NID).isPresent()) {
            fsBuilder.setFlowspecType(new DestinationIpv6PrefixCaseBuilder()
                .setDestinationPrefix(new Ipv6Prefix((String) fsType.getChild(DEST_PREFIX_NID).get().getValue()))
                .build());
        } else if (fsType.getChild(SOURCE_PREFIX_NID).isPresent()) {
            fsBuilder.setFlowspecType(new SourceIpv6PrefixCaseBuilder()
                .setSourcePrefix(new Ipv6Prefix((String) fsType.getChild(SOURCE_PREFIX_NID).get().getValue()))
                .build());
        } else if (fsType.getChild(NEXT_HEADER_NID).isPresent()) {
            fsBuilder.setFlowspecType(new NextHeaderCaseBuilder().setNextHeaders(createNextHeaders((UnkeyedListNode) fsType.getChild(NEXT_HEADER_NID).get())).build());
        } else if (fsType.getChild(FLOW_LABEL_NID).isPresent()) {
            fsBuilder.setFlowspecType(new FlowLabelCaseBuilder().setFlowLabel(createFlowLabels((UnkeyedListNode) fsType.getChild(FLOW_LABEL_NID).get())).build());
        }
    }

    private List<NextHeaders> createNextHeaders(final UnkeyedListNode nextHeadersData) {
        final List<NextHeaders> nextHeaders = new ArrayList<>();

        for (final UnkeyedListEntryNode node : nextHeadersData.getValue()) {
            final NextHeadersBuilder nextHeadersBuilder = new NextHeadersBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                nextHeadersBuilder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                nextHeadersBuilder.setValue((Short) valueNode.get().getValue());
            }
            nextHeaders.add(nextHeadersBuilder.build());
        }

        return nextHeaders;
    }

    private List<FlowLabel> createFlowLabels(final UnkeyedListNode flowLabelsData) {
        final List<FlowLabel> flowLabels = new ArrayList<>();

        for (final UnkeyedListEntryNode node : flowLabelsData.getValue()) {
            final FlowLabelBuilder flowLabelsBuilder = new FlowLabelBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                flowLabelsBuilder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                flowLabelsBuilder.setValue((Long) valueNode.get().getValue());
            }
            flowLabels.add(flowLabelsBuilder.build());
        }

        return flowLabels;
    }

    @Override
    protected void stringSpecificFSNlriType(final FlowspecType value, final StringBuilder buffer) {
        if (value instanceof DestinationIpv6PrefixCase) {
            buffer.append("to ");
            buffer.append(((DestinationIpv6PrefixCase) value).getDestinationPrefix().getValue());
        } else if (value instanceof SourceIpv6PrefixCase) {
            buffer.append("from ");
            buffer.append(((SourceIpv6PrefixCase) value).getSourcePrefix().getValue());
        } else if (value instanceof NextHeaderCase) {
            buffer.append("where next header ");
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(((NextHeaderCase) value).getNextHeaders()));
        } else if (value instanceof FlowLabelCase) {
            buffer.append("where flow label ");
            buffer.append(stringFlowLabel(((FlowLabelCase) value).getFlowLabel()));
        }
    }

    private static String stringFlowLabel(final List<FlowLabel> list) {
        final StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (final FlowLabel item : list) {
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(item.getOp(), isFirst));
            buffer.append(item.getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

}
