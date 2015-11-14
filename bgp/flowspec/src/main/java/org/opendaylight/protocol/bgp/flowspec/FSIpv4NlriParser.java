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
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.FragmentCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.DestinationPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.ProtocolIpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.ProtocolIpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.SourcePrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.protocol.ip._case.ProtocolIps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.protocol.ip._case.ProtocolIpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder;
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

public final class FSIpv4NlriParser extends AbstractFSNlriParser {

    private static final int IP_PROTOCOL_VALUE = 3;

    @VisibleForTesting
    static final NodeIdentifier PROTOCOL_IP_NID = new NodeIdentifier(ProtocolIps.QNAME);

    @Override
    protected void serializeMpReachNlri(final Attributes1 pathAttributes, final ByteBuf byteAggregator) {
        if (pathAttributes == null) {
            return;
        }
        final AdvertizedRoutes routes = (pathAttributes.getMpReachNlri()).getAdvertizedRoutes();
        if (routes != null && routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase flowspecCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) routes.getDestinationType();
            serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
        }
    }

    @Override
    protected void serializeMpUnreachNlri(final Attributes2 pathAttributes, final ByteBuf byteAggregator) {
        if (pathAttributes == null) {
            return;
        }
        final MpUnreachNlri mpUnreachNlri = pathAttributes.getMpUnreachNlri();
        if (mpUnreachNlri.getWithdrawnRoutes() != null && mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationFlowspecCase) {
            final DestinationFlowspecCase flowspecCase = (DestinationFlowspecCase) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
            serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
        }
    }

    @Override
    protected void serializeSpecificFSType(final FlowspecType value, final ByteBuf nlriByteBuf) {
        if (value instanceof DestinationPrefixCase) {
            nlriByteBuf.writeByte(DESTINATION_PREFIX_VALUE);
            nlriByteBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(((DestinationPrefixCase) value).getDestinationPrefix()));
        } else if (value instanceof SourcePrefixCase) {
            nlriByteBuf.writeByte(SOURCE_PREFIX_VALUE);
            nlriByteBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(((SourcePrefixCase) value).getSourcePrefix()));
        } else if (value instanceof ProtocolIpCase) {
            nlriByteBuf.writeByte(IP_PROTOCOL_VALUE);
            NumericOneByteOperandParser.INSTANCE.serialize(((ProtocolIpCase) value).getProtocolIps(), nlriByteBuf);
        } else if (value instanceof FragmentCase) {
            nlriByteBuf.writeByte(FRAGMENT_VALUE);
            serializeFragments(((FragmentCase) value).getFragments(), nlriByteBuf);
        }
    }

    @Override
    DestinationType createWidthdrawnDestinationType(final List<Flowspec> dst) {
        return new DestinationFlowspecCaseBuilder().setDestinationFlowspec(
            new DestinationFlowspecBuilder().setFlowspec(
                dst).build()).build();
    }

    @Override
    DestinationType createAdvertizedRoutesDestinationType(final List<Flowspec> dst) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder()
            .setDestinationFlowspec(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder()
                .setFlowspec(dst).build()).build();
    }

    @Override
    protected void setSpecificFlowspecType(final FlowspecBuilder builder, final short type, final ByteBuf nlri) {
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

    private static List<ProtocolIps> parseProtocolIp(final ByteBuf nlri) {
        final List<ProtocolIps> ips = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final ProtocolIpsBuilder builder = new ProtocolIpsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            builder.setValue(nlri.readUnsignedByte());
            end = op.isEndOfList();
            ips.add(builder.build());
        }
        return ips;
    }

    @Override
    protected Fragment parseFragment(final byte fragment) {
        final BitArray bs = BitArray.valueOf(fragment);
        return new Fragment(bs.get(DONT_FRAGMENT), bs.get(FIRST_FRAGMENT), bs.get(IS_A_FRAGMENT), bs.get(LAST_FRAGMENT));
    }

    @Override
    protected byte serializeFragment(final Fragment fragment) {
        final BitArray bs = new BitArray(Byte.SIZE);
        bs.set(DONT_FRAGMENT, fragment.isDoNot());
        bs.set(FIRST_FRAGMENT, fragment.isFirst());
        bs.set(IS_A_FRAGMENT, fragment.isIsA());
        bs.set(LAST_FRAGMENT, fragment.isLast());
        return bs.toByte();
    }

    @Override
    public void extractSpecificFlowspec(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        if (fsType.getChild(DEST_PREFIX_NID).isPresent()) {
            fsBuilder.setFlowspecType(new DestinationPrefixCaseBuilder().setDestinationPrefix(
                new Ipv4Prefix((String) fsType.getChild(DEST_PREFIX_NID).get().getValue())).build());
        } else if (fsType.getChild(SOURCE_PREFIX_NID).isPresent()) {
            fsBuilder.setFlowspecType(new SourcePrefixCaseBuilder().setSourcePrefix(new Ipv4Prefix((String) fsType.getChild(SOURCE_PREFIX_NID).get().getValue()))
                .build());
        } else if (fsType.getChild(PROTOCOL_IP_NID).isPresent()) {
            fsBuilder.setFlowspecType(new ProtocolIpCaseBuilder().setProtocolIps(createProtocolsIps((UnkeyedListNode) fsType.getChild(PROTOCOL_IP_NID).get())).build());
        }
    }

    private static List<ProtocolIps> createProtocolsIps(final UnkeyedListNode protocolIpsData) {
        final List<ProtocolIps> protocolIps = new ArrayList<>();

        for (final UnkeyedListEntryNode node : protocolIpsData.getValue()) {
            final ProtocolIpsBuilder ipsBuilder = new ProtocolIpsBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                ipsBuilder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                ipsBuilder.setValue((Short) valueNode.get().getValue());
            }
            protocolIps.add(ipsBuilder.build());
        }

        return protocolIps;
    }

    @Override
    protected void stringSpecificFSNlriType(final FlowspecType value, final StringBuilder buffer) {
        if (value instanceof DestinationPrefixCase) {
            buffer.append("to ");
            buffer.append(((DestinationPrefixCase) value).getDestinationPrefix().getValue());
        } else if (value instanceof SourcePrefixCase) {
            buffer.append("from ");
            buffer.append(((SourcePrefixCase) value).getSourcePrefix().getValue());
        } else if (value instanceof ProtocolIpCase) {
            buffer.append("where IP protocol ");
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(((ProtocolIpCase) value).getProtocolIps()));
        }
    }

}
