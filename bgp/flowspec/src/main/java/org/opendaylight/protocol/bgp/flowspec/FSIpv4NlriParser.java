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
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.DestinationPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.FragmentCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.ProtocolIpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.ProtocolIpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.SourcePrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.fragment._case.Fragments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.fragment._case.FragmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.protocol.ip._case.ProtocolIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.protocol.ip._case.ProtocolIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder;
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

public final class FSIpv4NlriParser extends AbstractFSNlriParser implements NlriParser, NlriSerializer {

    private static final int IP_PROTOCOL_VALUE = 3;

    @VisibleForTesting
    static final NodeIdentifier PROTOCOL_IP_NID = new NodeIdentifier(ProtocolIps.QNAME);

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null && routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase flowspecCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) routes.getDestinationType();
                serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
            }
        } else if (pathAttributes2 != null) {
            final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
            if (mpUnreachNlri.getWithdrawnRoutes() != null && mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationFlowspecCase) {
                final DestinationFlowspecCase flowspecCase = (DestinationFlowspecCase) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
                serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
            }
        }
    }

    @Override
    protected void serializeFlowspec(final Flowspec flow, final ByteBuf nlriByteBuf) {
        super.serializeFlowspec(flow, nlriByteBuf);
        final FlowspecType value = flow.getFlowspecType();
        if (value instanceof DestinationPrefixCase) {
            nlriByteBuf.writeByte(DESTINATION_PREFIX_VALUE);
            nlriByteBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(((DestinationPrefixCase) value).getDestinationPrefix()));
        } else if (value instanceof SourcePrefixCase) {
            nlriByteBuf.writeByte(SOURCE_PREFIX_VALUE);
            nlriByteBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(((SourcePrefixCase) value).getSourcePrefix()));
        } else if (value instanceof ProtocolIpCase) {
            nlriByteBuf.writeByte(IP_PROTOCOL_VALUE);
            serializeNumericTwoByteValue(((ProtocolIpCase) value).getProtocolIps(), nlriByteBuf);
        } else if (value instanceof FragmentCase) {
            nlriByteBuf.writeByte(FRAGMENT_VALUE);
            serializeFragments(((FragmentCase) value).getFragments(), nlriByteBuf);
        }
    }

    private void serializeFragments(final List<Fragments> fragments, final ByteBuf nlriByteBuf) {
        for (final Fragments fragment : fragments) {
            serializeBitmaskOperand(fragment.getOp(), 1, nlriByteBuf);
            nlriByteBuf.writeByte(serializeFragment(fragment.getValue()));
        }
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<Flowspec> dst = parseNlri(nlri);

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(
            new DestinationFlowspecCaseBuilder().setDestinationFlowspec(
                new DestinationFlowspecBuilder().setFlowspec(
                    dst).build()).build()).build());
    }

    @Override
    public void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<Flowspec> dst = parseNlri(nlri);

        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder()
                .setDestinationFlowspec(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder()
                    .setFlowspec(dst).build()).build()).build());
    }

    @Override
    protected void setFlowspecType(final FlowspecBuilder builder, final short type, final ByteBuf nlri) {
        super.setFlowspecType(builder, type, nlri);
        switch (type) {
        case DESTINATION_PREFIX_VALUE:
            builder.setFlowspecType(new DestinationPrefixCaseBuilder().setDestinationPrefix(Ipv4Util.prefixForByteBuf(nlri)).build());
            break;
        case SOURCE_PREFIX_VALUE:
            builder.setFlowspecType(new SourcePrefixCaseBuilder().setSourcePrefix(Ipv4Util.prefixForByteBuf(nlri)).build());
            break;
        case IP_PROTOCOL_VALUE:
            builder.setFlowspecType(new ProtocolIpCaseBuilder().setProtocolIps(parseProtocolIp(nlri)).build());
            break;
        case FRAGMENT_VALUE:
            builder.setFlowspecType(new FragmentCaseBuilder().setFragments(parseFragment(nlri)).build());
            break;
        default:
            break;
        }
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

    private static List<ProtocolIps> parseProtocolIp(final ByteBuf nlri) {
        final List<ProtocolIps> ips = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final ProtocolIpsBuilder builder = new ProtocolIpsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            ips.add(builder.build());
        }
        return ips;
    }

    @Override
    protected org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Fragment parseFragment(final byte fragment) {
        final BitArray bs = BitArray.valueOf(fragment);
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Fragment(bs.get(DONT_FRAGMENT), bs.get(FIRST_FRAGMENT), bs.get(IS_A_FRAGMENT), bs.get(LAST_FRAGMENT));
    }

    @Override
    protected byte serializeFragment(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Fragment fragment) {
        final BitArray bs = new BitArray(Byte.SIZE);
        bs.set(DONT_FRAGMENT, fragment.isDoNot());
        bs.set(FIRST_FRAGMENT, fragment.isFirst());
        bs.set(IS_A_FRAGMENT, fragment.isIsA());
        bs.set(LAST_FRAGMENT, fragment.isLast());
        return bs.toByte();
    }

    @Override
    public Flowspec extractFlowspec(final MapEntryNode route) {
        final FlowspecBuilder fs = new FlowspecBuilder();
        final Optional<DataContainerChild<?, ?>> flowspecType = route.getChild(FLOWSPEC_TYPE_NID);
        if (flowspecType.isPresent()) {
            final ChoiceNode fsType = (ChoiceNode) flowspecType.get();
            extractFlowspec(fsType, fs);
            if (fsType.getChild(DEST_PREFIX_NID).isPresent()) {
                fs.setFlowspecType(new DestinationPrefixCaseBuilder().setDestinationPrefix(
                        new Ipv4Prefix((String) fsType.getChild(DEST_PREFIX_NID).get().getValue())).build());
            } else if (fsType.getChild(SOURCE_PREFIX_NID).isPresent()) {
                fs.setFlowspecType(new SourcePrefixCaseBuilder().setSourcePrefix(new Ipv4Prefix((String) fsType.getChild(SOURCE_PREFIX_NID).get().getValue()))
                        .build());
            } else if (fsType.getChild(PROTOCOL_IP_NID).isPresent()) {
                fs.setFlowspecType(new ProtocolIpCaseBuilder().setProtocolIps(createProtocolsIps((UnkeyedListNode) fsType.getChild(PROTOCOL_IP_NID).get())).build());
            } else if (fsType.getChild(FRAGMENT_NID).isPresent()) {
                fs.setFlowspecType(new FragmentCaseBuilder().setFragments(createFragments((UnkeyedListNode) fsType.getChild(FRAGMENT_NID).get())).build());
            }
        }
        return fs.build();
    }

    private static List<ProtocolIps> createProtocolsIps(final UnkeyedListNode protocolIpsData) {
        final List<ProtocolIps> protocolIps = new ArrayList<>();

        for (final UnkeyedListEntryNode node : protocolIpsData.getValue()) {
            final ProtocolIpsBuilder ipsBuilder = new ProtocolIpsBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                ipsBuilder.setOp(createNumericOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                ipsBuilder.setValue((Integer) valueNode.get().getValue());
            }
            protocolIps.add(ipsBuilder.build());
        }

        return protocolIps;
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

    @Override
    @VisibleForTesting
    String stringNlri(final Flowspec flow) {
        final StringBuilder buffer = new StringBuilder("all packets ");
        final FlowspecType value = flow.getFlowspecType();

        stringNlri(value, buffer);

        if (value instanceof DestinationPrefixCase) {
            buffer.append("to ");
            buffer.append(((DestinationPrefixCase) value).getDestinationPrefix().getValue());
        } else if (value instanceof SourcePrefixCase) {
            buffer.append("from ");
            buffer.append(((SourcePrefixCase) value).getSourcePrefix().getValue());
        } else if (value instanceof ProtocolIpCase) {
            buffer.append("where IP protocol ");
            buffer.append(stringNumericTwo(((ProtocolIpCase) value).getProtocolIps()));
        } else if (value instanceof FragmentCase) {
            buffer.append(stringFragment(((FragmentCase) value).getFragments()));
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


}
