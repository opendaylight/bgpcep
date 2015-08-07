/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.DestinationIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.FlowLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.FlowLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.FragmentIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.FragmentIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.NextHeaderCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.NextHeaderCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.SourceIpv6PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.flow.label._case.FlowLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.fragment.ipv6._case.Fragments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.fragment.ipv6._case.FragmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.next.header._case.NextHeaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.flowspec.flowspec.type.next.header._case.NextHeadersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec.ipv6._case.DestinationFlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

public final class FSIpv6NlriParser extends AbstractFSNlriParser implements NlriParser, NlriSerializer {

    private static final int NEXT_HEADER_VALUE = 3;
    private static final int FLOW_LABLE_VALUE = 13;

    static final NodeIdentifier NEXT_HEADER_NID = new NodeIdentifier(NextHeaders.QNAME);
    static final NodeIdentifier FLOW_LABEL_NID = new NodeIdentifier(FlowLabel.QNAME);

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null && routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case flowspecCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case) routes.getDestinationType();
                serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            if (mpUnreachNlri.getWithdrawnRoutes() != null && mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationFlowspecIpv6Case) {
                final DestinationFlowspecIpv6Case flowspecCase = (DestinationFlowspecIpv6Case) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
            }
        }
    }

    @Override
    protected void serializeFlowspec(final Flowspec flow, final ByteBuf nlriByteBuf) {
        super.serializeFlowspec(flow, nlriByteBuf);
        final FlowspecType value = flow.getFlowspecType();
        if (value instanceof DestinationIpv6PrefixCase) {
            nlriByteBuf.writeByte(DESTINATION_PREFIX_VALUE);
            nlriByteBuf.writeBytes(insertOffsetByte(Ipv6Util.bytesForPrefixBegin(((DestinationIpv6PrefixCase) value).getDestinationPrefix())));
        } else if (value instanceof SourceIpv6PrefixCase) {
            nlriByteBuf.writeByte(SOURCE_PREFIX_VALUE);
            nlriByteBuf.writeBytes(insertOffsetByte(Ipv6Util.bytesForPrefixBegin(((SourceIpv6PrefixCase) value).getSourcePrefix())));
        } else if (value instanceof NextHeaderCase) {
            nlriByteBuf.writeByte(NEXT_HEADER_VALUE);
            serializeNumericOneByteValue(((NextHeaderCase) value).getNextHeaders(), nlriByteBuf);
        } else if (value instanceof FragmentIpv6Case) {
            nlriByteBuf.writeByte(FRAGMENT_VALUE);
            serializeFragments(((FragmentIpv6Case) value).getFragments(), nlriByteBuf);
        } else if (value instanceof FlowLabelCase) {
            nlriByteBuf.writeByte(FLOW_LABLE_VALUE);
            serializeNumericFourByteValue(((FlowLabelCase) value).getFlowLabel(), nlriByteBuf);
        }
    }

    private void serializeFragments(final List<Fragments> fragments, final ByteBuf nlriByteBuf) {
        for (final Fragments fragment : fragments) {
            serializeBitmaskOperand(fragment.getOp(), 1, nlriByteBuf);
            nlriByteBuf.writeByte(serializeFragment(fragment.getValue()));
        }
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

    private static void serializeNumericFourByteValue(final List<FlowLabel> list, final ByteBuf nlriByteBuf) {
        for (final FlowLabel item : list) {
            final ByteBuf protoBuf = Unpooled.buffer();
            writeShortest(item.getValue().intValue(), protoBuf);
            serializeNumericOperand(item.getOp(), protoBuf.readableBytes(), nlriByteBuf);
            nlriByteBuf.writeBytes(protoBuf);
        }
    }

    private static byte[] insertOffsetByte(final byte[] ipPrefix) {
        // income <len, prefix>
        return Bytes.concat(new byte[] { ipPrefix[0] }, new byte[] { 0 }, ByteArray.subByte(ipPrefix, 1 , ipPrefix.length-1));
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<Flowspec> dst = super.parseNlri(nlri);

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new DestinationFlowspecIpv6CaseBuilder().setDestinationFlowspec(
                new DestinationFlowspecBuilder().setFlowspec(
                    dst).build()).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<Flowspec> dst = super.parseNlri(nlri);

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6CaseBuilder()
                .setDestinationFlowspec(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec.ipv6._case.DestinationFlowspecBuilder()
                    .setFlowspec(dst).build()).build()).build());
    }

    @Override
    protected void setFlowspecType(final FlowspecBuilder builder, final short type, final ByteBuf nlri) {
        super.setFlowspecType(builder, type, nlri);
        switch (type) {
        case DESTINATION_PREFIX_VALUE:
            builder.setFlowspecType(new DestinationIpv6PrefixCaseBuilder().setDestinationPrefix(parseIpPrefix(nlri)).build());
            break;
        case SOURCE_PREFIX_VALUE:
            builder.setFlowspecType(new SourceIpv6PrefixCaseBuilder().setSourcePrefix(parseIpPrefix(nlri)).build());
            break;
        case NEXT_HEADER_VALUE:
            builder.setFlowspecType(new NextHeaderCaseBuilder().setNextHeaders(parseNextHeader(nlri)).build());
            break;
        case FRAGMENT_VALUE:
            builder.setFlowspecType(new FragmentIpv6CaseBuilder().setFragments(parseFragment(nlri)).build());
            break;
        case FLOW_LABLE_VALUE:
            builder.setFlowspecType(new FlowLabelCaseBuilder().setFlowLabel(parseFlowLabel(nlri)).build());
            break;
        default:
            break;
        }
    }

    private static Ipv6Prefix parseIpPrefix(final ByteBuf nlri) {
        final int bitLength = nlri.readByte();
        final int offset = nlri.readByte();
        nlri.readBytes(offset);
        return Ipv6Util.prefixForBytes(ByteArray.readBytes(nlri, bitLength / Byte.SIZE), bitLength);
    }

    private static List<NextHeaders> parseNextHeader(final ByteBuf nlri) {
        final List<NextHeaders> headers = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final NextHeadersBuilder builder = new NextHeadersBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            builder.setValue(nlri.readUnsignedByte());
            end = op.isEndOfList();
            headers.add(builder.build());
        }
        return headers;
    }

    private List<Fragments> parseFragment(final ByteBuf nlri) {
        final List<Fragments> fragments = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final FragmentsBuilder builder = new FragmentsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final BitmaskOperand op = parseBitmask(b);
            builder.setOp(op);
            builder.setValue(parseFragment(nlri.readByte()));
            end = op.isEndOfList();
            fragments.add(builder.build());
        }
        return fragments;
    }

    private static List<FlowLabel> parseFlowLabel(final ByteBuf nlri) {
        final List<FlowLabel> labels = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final FlowLabelBuilder builder = new FlowLabelBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToLong(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            labels.add(builder.build());
        }
        return labels;
    }

    @Override
    protected  org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Fragment parseFragment(final byte fragment) {
        final BitArray bs = BitArray.valueOf(fragment);
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Fragment(Boolean.FALSE, bs.get(FIRST_FRAGMENT), bs.get(IS_A_FRAGMENT), bs.get(LAST_FRAGMENT));
    }

    @Override
    public Flowspec extractFlowspec(final MapEntryNode route) {
        final FlowspecBuilder fs = new FlowspecBuilder();
        final Optional<DataContainerChild<?, ?>> flowspecType = route.getChild(FLOWSPEC_TYPE_NID);
        if (flowspecType.isPresent()) {
            final ChoiceNode fsType = (ChoiceNode) route.getChild(FLOWSPEC_TYPE_NID).get();
            extractFlowspec(fsType, fs);
            if (fsType.getChild(DEST_PREFIX_NID).isPresent()) {
                fs.setFlowspecType(new DestinationIpv6PrefixCaseBuilder()
                    .setDestinationPrefix(new Ipv6Prefix((String) fsType.getChild(DEST_PREFIX_NID).get().getValue()))
                    .build());
            } else if (fsType.getChild(SOURCE_PREFIX_NID).isPresent()) {
                fs.setFlowspecType(new SourceIpv6PrefixCaseBuilder()
                    .setSourcePrefix(new Ipv6Prefix((String) fsType.getChild(SOURCE_PREFIX_NID).get().getValue()))
                    .build());
            } else if (fsType.getChild(NEXT_HEADER_NID).isPresent()) {
                fs.setFlowspecType(new NextHeaderCaseBuilder().setNextHeaders(createNextHeaders((UnkeyedListNode) fsType.getChild(NEXT_HEADER_NID).get())).build());
            } else if (fsType.getChild(FRAGMENT_NID).isPresent()) {
                fs.setFlowspecType(new FragmentIpv6CaseBuilder().setFragments(createFragments((UnkeyedListNode) fsType.getChild(FRAGMENT_NID).get())).build());
            } else if (fsType.getChild(FLOW_LABEL_NID).isPresent()) {
                fs.setFlowspecType(new FlowLabelCaseBuilder().setFlowLabel(createFlowLabels((UnkeyedListNode) fsType.getChild(FLOW_LABEL_NID).get())).build());
            }
        }
        return fs.build();
    }

    private List<NextHeaders> createNextHeaders(final UnkeyedListNode nextHeadersData) {
        final List<NextHeaders> nextHeaders = new ArrayList<>();

        for (final UnkeyedListEntryNode node : nextHeadersData.getValue()) {
            final NextHeadersBuilder nextHeadersBuilder = new NextHeadersBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                nextHeadersBuilder.setOp(createNumericOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                nextHeadersBuilder.setValue((Short) valueNode.get().getValue());
            }
            nextHeaders.add(nextHeadersBuilder.build());
        }

        return nextHeaders;
    }

    private static List<Fragments> createFragments(final UnkeyedListNode fragmentsData) {
        final List<Fragments> fragments = new ArrayList<>();

        for (final UnkeyedListEntryNode node : fragmentsData.getValue()) {
            final FragmentsBuilder fragmentsBuilder = new FragmentsBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                fragmentsBuilder.setOp(createBitmaskOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                fragmentsBuilder.setValue(createFragment((Set<String>) valueNode.get().getValue()));
            }
            fragments.add(fragmentsBuilder.build());
        }

        return fragments;
    }

    private List<FlowLabel> createFlowLabels(final UnkeyedListNode flowLabelsData) {
        final List<FlowLabel> flowLabels = new ArrayList<>();

        for (final UnkeyedListEntryNode node : flowLabelsData.getValue()) {
            final FlowLabelBuilder flowLabelsBuilder = new FlowLabelBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                flowLabelsBuilder.setOp(createNumericOperand((Set<String>) opValue.get().getValue()));
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
    @VisibleForTesting
    String stringNlri(final Flowspec flow) {
        final StringBuilder buffer = new StringBuilder("all packets ");
        final FlowspecType value = flow.getFlowspecType();

        stringNlri(value, buffer);

        if (value instanceof DestinationIpv6PrefixCase) {
            buffer.append("to ");
            buffer.append(((DestinationIpv6PrefixCase) value).getDestinationPrefix().getValue());
        } else if (value instanceof SourceIpv6PrefixCase) {
            buffer.append("from ");
            buffer.append(((SourceIpv6PrefixCase) value).getSourcePrefix().getValue());
        } else if (value instanceof NextHeaderCase) {
            buffer.append("where next header ");
            buffer.append(stringNumericOne(((NextHeaderCase) value).getNextHeaders()));
        } else if (value instanceof FragmentIpv6Case) {
            buffer.append(stringFragment(((FragmentIpv6Case) value).getFragments()));
        } else if (value instanceof FlowLabelCase) {
            buffer.append("where flow label ");
            buffer.append(stringFlowLabel(((FlowLabelCase) value).getFlowLabel()));
        }
        return buffer.toString();
    }

    private static String stringFragment(final List<Fragments> fragments) {
        final StringBuilder buffer = new StringBuilder("where fragment ");
        boolean isFirst = true;
        for (final Fragments item : fragments) {
            buffer.append(stringBitmaskOperand(item.getOp(), isFirst));
            buffer.append(stringFragment(item.getValue()));
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

    private static String stringFlowLabel(final List<FlowLabel> list) {
        final StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (final FlowLabel item : list) {
            buffer.append(stringNumericOperand(item.getOp(), isFirst));
            buffer.append(item.getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }
}
