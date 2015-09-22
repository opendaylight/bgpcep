/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInts;
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
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.ComponentType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.NumericOneByteValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.NumericTwoByteValue;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder;
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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSNlriParser implements NlriParser, NlriSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(FSNlriParser.class);

    @VisibleForTesting
    static final NodeIdentifier FLOWSPEC_NID = new NodeIdentifier(Flowspec.QNAME);
    @VisibleForTesting
    static final NodeIdentifier FLOWSPEC_TYPE_NID = new NodeIdentifier(FlowspecType.QNAME);
    @VisibleForTesting
    static final NodeIdentifier COMPONENT_TYPE_NID = new NodeIdentifier(QName.cachedReference(QName.create(Flowspec.QNAME, "component-type")));
    @VisibleForTesting
    static final NodeIdentifier DEST_PREFIX_NID = new NodeIdentifier(QName.cachedReference(QName.create(DestinationPrefixCase.QNAME, "destination-prefix")));
    @VisibleForTesting
    static final NodeIdentifier SOURCE_PREFIX_NID = new NodeIdentifier(QName.cachedReference(QName.create(SourcePrefixCase.QNAME, "source-prefix")));
    @VisibleForTesting
    static final NodeIdentifier PROTOCOL_IP_NID = new NodeIdentifier(ProtocolIps.QNAME);
    @VisibleForTesting
    static final NodeIdentifier PORTS_NID = new NodeIdentifier(Ports.QNAME);
    @VisibleForTesting
    static final NodeIdentifier DEST_PORT_NID = new NodeIdentifier(DestinationPorts.QNAME);
    @VisibleForTesting
    static final NodeIdentifier SOURCE_PORT_NID = new NodeIdentifier(SourcePorts.QNAME);
    @VisibleForTesting
    static final NodeIdentifier ICMP_TYPE_NID = new NodeIdentifier(Types.QNAME);
    @VisibleForTesting
    static final NodeIdentifier ICMP_CODE_NID = new NodeIdentifier(Codes.QNAME);
    @VisibleForTesting
    static final NodeIdentifier TCP_FLAGS_NID = new NodeIdentifier(TcpFlags.QNAME);
    @VisibleForTesting
    static final NodeIdentifier PACKET_LENGTHS_NID = new NodeIdentifier(PacketLengths.QNAME);
    @VisibleForTesting
    static final NodeIdentifier DSCP_NID = new NodeIdentifier(Dscps.QNAME);
    @VisibleForTesting
    static final NodeIdentifier FRAGMENT_NID = new NodeIdentifier(Fragments.QNAME);
    @VisibleForTesting
    static final NodeIdentifier OP_NID = new NodeIdentifier(QName.create("urn:opendaylight:params:xml:ns:yang:bgp-flowspec","2015-01-14","op"));
    @VisibleForTesting
    static final NodeIdentifier VALUE_NID = new NodeIdentifier(QName.create("urn:opendaylight:params:xml:ns:yang:bgp-flowspec","2015-01-14","value"));

    private static final int NLRI_LENGTH = 1;
    private static final int NLRI_LENGTH_EXTENDED = 2;

    @VisibleForTesting
    static final String AND_BIT_VALUE = "and-bit";
    @VisibleForTesting
    static final String END_OF_LIST_VALUE = "end-of-list";
    @VisibleForTesting
    static final String EQUALS_VALUE = "equals";
    @VisibleForTesting
    static final String GREATER_THAN_VALUE = "greater-than";
    @VisibleForTesting
    static final String LESS_THAN_VALUE = "less-than";
    @VisibleForTesting
    static final String MATCH_VALUE = "match";
    @VisibleForTesting
    static final String NOT_VALUE = "not";
    @VisibleForTesting
    static final String DO_NOT_VALUE = "do-not";
    @VisibleForTesting
    static final String FIRST_VALUE = "first";
    @VisibleForTesting
    static final String LAST_VALUE = "last";
    @VisibleForTesting
    static final String IS_A_VALUE = "is-a";

    private static final String FLOW_SEPARATOR = " AND ";

    /**
     * Add this constant to length value to achieve all ones in the leftmost nibble.
     */
    private static final int LENGTH_MAGIC = 61440;

    private static final int OPERAND_LENGTH = 8;

    private static final int END_OF_LIST = 0;
    private static final int AND_BIT = 1;
    private static final int LENGTH_BITMASK = 48;
    private static final int LENGTH_SHIFT = 4;
    private static final int LESS_THAN = 5;
    private static final int GREATER_THAN = 6;
    private static final int EQUAL = 7;

    private static final int NOT = 6;
    private static final int MATCH = 7;

    private static final int LAST_FRAGMENT = 4;
    private static final int FIRST_FRAGMENT = 5;
    private static final int IS_A_FRAGMENT = 6;
    private static final int DONT_FRAGMENT = 7;

    private static final int MAX_NLRI_LENGTH = 4095;
    private static final int MAX_NLRI_LENGTH_ONE_BYTE = 240;

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null && routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase flowspecCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) routes.getDestinationType();
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

    /**
     * Serializes Flowspec component type that has maximum of 2B sized value field and numeric operand.
     *
     * @param list of items to be serialized
     * @param nlriByteBuf where the items will be serialized
     */
    private static <T extends NumericTwoByteValue> void serializeNumericTwoByteValue(final List<T> list, final ByteBuf nlriByteBuf) {
        for (final T item : list) {
            final ByteBuf protoBuf = Unpooled.buffer();
            writeShortest(item.getValue(), protoBuf);
            serializeNumericOperand(item.getOp(), protoBuf.readableBytes(), nlriByteBuf);
            nlriByteBuf.writeBytes(protoBuf);
        }
    }

    /**
     * Serializes Flowspec component type that has maximum of 1B sized value field and numeric operand.
     *
     * @param list of items to be serialized
     * @param nlriByteBuf where the items will be serialized
     */
    private static <T extends NumericOneByteValue> void serializeNumericOneByteValue(final List<T> list, final ByteBuf nlriByteBuf) {
        for (final T type : list) {
            serializeNumericOperand(type.getOp(), 1, nlriByteBuf);
            writeShortest(type.getValue(), nlriByteBuf);
        }
    }

    /**
     * Serializes Flowspec NLRI to ByteBuf.
     *
     * @param flows flowspec NLRI to be serialized
     * @param buffer where flowspec NLRI will be serialized
     */
    public static void serializeNlri(final List<Flowspec> flows, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final Flowspec flow : flows) {
            nlriByteBuf.writeByte(flow.getComponentType().getIntValue());
            final FlowspecType value = flow.getFlowspecType();
            switch (flow.getComponentType()) {
            case DestinationPrefix:
                nlriByteBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(((DestinationPrefixCase) value).getDestinationPrefix()));
                break;
            case SourcePrefix:
                nlriByteBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(((SourcePrefixCase) value).getSourcePrefix()));
                break;
            case ProtocolIp:
                serializeNumericTwoByteValue(((ProtocolIpCase) value).getProtocolIps(), nlriByteBuf);
                break;
            case Port:
                serializeNumericTwoByteValue(((PortCase) value).getPorts(), nlriByteBuf);
                break;
            case DestinationPort:
                serializeNumericTwoByteValue(((DestinationPortCase) value).getDestinationPorts(), nlriByteBuf);
                break;
            case SourcePort:
                serializeNumericTwoByteValue(((SourcePortCase) value).getSourcePorts(), nlriByteBuf);
                break;
            case IcmpType:
                serializeNumericOneByteValue(((IcmpTypeCase) value).getTypes(), nlriByteBuf);
                break;
            case IcmpCode:
                serializeNumericOneByteValue(((IcmpCodeCase) value).getCodes(), nlriByteBuf);
                break;
            case TcpFlags:
                final List<TcpFlags> flags = ((TcpFlagsCase) value).getTcpFlags();
                for (final TcpFlags flag : flags) {
                    final ByteBuf flagsBuf = Unpooled.buffer();
                    writeShortest(flag.getValue(), flagsBuf);
                    serializeBitmaskOperand(flag.getOp(), flagsBuf.readableBytes(), nlriByteBuf);
                    nlriByteBuf.writeBytes(flagsBuf);
                }
                break;
            case PacketLength:
                serializeNumericTwoByteValue(((PacketLengthCase) value).getPacketLengths(), nlriByteBuf);
                break;
            case Dscp:
                final List<Dscps> dscps = ((DscpCase) value).getDscps();
                for (final Dscps dscp : dscps) {
                    serializeNumericOperand(dscp.getOp(), 1, nlriByteBuf);
                    writeShortest(dscp.getValue().getValue(), nlriByteBuf);
                }
                break;
            case Fragment:
                final List<Fragments> fragments = ((FragmentCase) value).getFragments();
                for (final Fragments fragment : fragments) {
                    serializeBitmaskOperand(fragment.getOp(), 1, nlriByteBuf);
                    nlriByteBuf.writeByte(serializeFragment(fragment.getValue()));
                }
                break;
            default:
                LOG.warn("Unknown Component Type.");
                break;
            }
        }
        Preconditions.checkState(nlriByteBuf.readableBytes() <= MAX_NLRI_LENGTH, "Maximum length of Flowspec NLRI reached.");
        if (nlriByteBuf.readableBytes() <= MAX_NLRI_LENGTH_ONE_BYTE) {
            buffer.writeByte(nlriByteBuf.readableBytes());
        } else {
            buffer.writeShort(nlriByteBuf.readableBytes() + LENGTH_MAGIC);
        }
        buffer.writeBytes(nlriByteBuf);
    }

    /**
     * Given the integer values, this method instead of writing the value
     * in 4B field, compresses the value to lowest required byte field
     * depending on the value.
     *
     * @param value integer to be written
     * @param buffer ByteBuf where the value will be written
     */
    private static void writeShortest(final int value, final ByteBuf buffer) {
        if (value <= Values.UNSIGNED_BYTE_MAX_VALUE) {
            buffer.writeByte(UnsignedBytes.checkedCast(value));
        } else if (value <= Values.UNSIGNED_SHORT_MAX_VALUE) {
            ByteBufWriteUtil.writeUnsignedShort(value, buffer);
        } else if (value <= Values.UNSIGNED_INT_MAX_VALUE) {
            ByteBufWriteUtil.writeUnsignedInt(UnsignedInts.toLong(value), buffer);
        } else {
            buffer.writeLong(value);
        }
    }

    private static void serializeNumericOperand(final NumericOperand op, final int length, final ByteBuf buffer) {
        final BitArray bs = new BitArray(OPERAND_LENGTH);
        bs.set(END_OF_LIST, op.isEndOfList());
        bs.set(AND_BIT, op.isAndBit());
        bs.set(LESS_THAN, op.isLessThan());
        bs.set(GREATER_THAN, op.isGreaterThan());
        bs.set(EQUAL, op.isEquals());
        final byte len = (byte) (Integer.numberOfTrailingZeros(length) << LENGTH_SHIFT);
        buffer.writeByte(bs.toByte() | len);
    }

    private static void serializeBitmaskOperand(final BitmaskOperand op, final int length, final ByteBuf buffer) {
        final BitArray bs = new BitArray(OPERAND_LENGTH);
        bs.set(END_OF_LIST, op.isEndOfList());
        bs.set(AND_BIT, op.isAndBit());
        bs.set(MATCH, op.isMatch());
        bs.set(NOT, op.isNot());
        final byte len = (byte) (Integer.numberOfTrailingZeros(length) << LENGTH_SHIFT);
        buffer.writeByte(bs.toByte() | len);
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
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder()
                .setDestinationFlowspec(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder()
                    .setFlowspec(dst).build()).build()).build());
    }

    /**
     * Parses Flowspec NLRI into list of Flowspec.
     *
     * @param nlri byte representation of NLRI which will be parsed
     * @return list of Flowspec
     */
    public static List<Flowspec> parseNlri(final ByteBuf nlri) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<Flowspec> fss = new ArrayList<>();

        // length field can be one or two bytes (if needed)
        // check the length of nlri to see how many bytes we can skip
        final int length = nlri.readableBytes();
        nlri.skipBytes(length > MAX_NLRI_LENGTH_ONE_BYTE ? NLRI_LENGTH_EXTENDED : NLRI_LENGTH);

        while (nlri.isReadable()) {
            final FlowspecBuilder builder = new FlowspecBuilder();
            // read type
            final ComponentType type = ComponentType.forValue(nlri.readUnsignedByte());
            builder.setComponentType(type);
            switch (type) {
            case DestinationPrefix:
                builder.setFlowspecType(new DestinationPrefixCaseBuilder().setDestinationPrefix(Ipv4Util.prefixForByteBuf(nlri)).build());
                break;
            case SourcePrefix:
                builder.setFlowspecType(new SourcePrefixCaseBuilder().setSourcePrefix(Ipv4Util.prefixForByteBuf(nlri)).build());
                break;
            case ProtocolIp:
                builder.setFlowspecType(new ProtocolIpCaseBuilder().setProtocolIps(parseProtocolIp(nlri)).build());
                break;
            case Port:
                builder.setFlowspecType(new PortCaseBuilder().setPorts(parsePort(nlri)).build());
                break;
            case DestinationPort:
                builder.setFlowspecType(new DestinationPortCaseBuilder().setDestinationPorts(parseDestinationPort(nlri)).build());
                break;
            case SourcePort:
                builder.setFlowspecType(new SourcePortCaseBuilder().setSourcePorts(parseSourcePort(nlri)).build());
                break;
            case IcmpType:
                builder.setFlowspecType(new IcmpTypeCaseBuilder().setTypes(parseIcmpType(nlri)).build());
                break;
            case IcmpCode:
                builder.setFlowspecType(new IcmpCodeCaseBuilder().setCodes(parseIcmpCode(nlri)).build());
                break;
            case TcpFlags:
                builder.setFlowspecType(new TcpFlagsCaseBuilder().setTcpFlags(parseTcpFlags(nlri)).build());
                break;
            case PacketLength:
                builder.setFlowspecType(new PacketLengthCaseBuilder().setPacketLengths(parsePacketLength(nlri)).build());
                break;
            case Dscp:
                builder.setFlowspecType(new DscpCaseBuilder().setDscps(parseDscp(nlri)).build());
                break;
            case Fragment:
                builder.setFlowspecType(new FragmentCaseBuilder().setFragments(parseFragment(nlri)).build());
                break;
            default:
                break;
            }
            fss.add(builder.build());
        }
        return fss;
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

    private static List<Ports> parsePort(final ByteBuf nlri) {
        final List<Ports> ports = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final PortsBuilder builder = new PortsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            ports.add(builder.build());
        }
        return ports;
    }

    private static List<DestinationPorts> parseDestinationPort(final ByteBuf nlri) {
        final List<DestinationPorts> ports = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final DestinationPortsBuilder builder = new DestinationPortsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            ports.add(builder.build());
        }
        return ports;
    }

    private static List<SourcePorts> parseSourcePort(final ByteBuf nlri) {
        final List<SourcePorts> ports = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final SourcePortsBuilder builder = new SourcePortsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            ports.add(builder.build());
        }
        return ports;
    }

    private static List<Types> parseIcmpType(final ByteBuf nlri) {
        final List<Types> types = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final TypesBuilder builder = new TypesBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            builder.setValue(nlri.readUnsignedByte());
            end = op.isEndOfList();
            types.add(builder.build());
        }
        return types;
    }

    private static List<Codes> parseIcmpCode(final ByteBuf nlri) {
        final List<Codes> codes = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final CodesBuilder builder = new CodesBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            builder.setValue(nlri.readUnsignedByte());
            end = op.isEndOfList();
            codes.add(builder.build());
        }
        return codes;
    }

    private static List<TcpFlags> parseTcpFlags(final ByteBuf nlri) {
        final List<TcpFlags> flags = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final TcpFlagsBuilder builder = new TcpFlagsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final BitmaskOperand op = parseBitmask(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            flags.add(builder.build());
        }
        return flags;
    }

    private static List<PacketLengths> parsePacketLength(final ByteBuf nlri) {
        final List<PacketLengths> plengths = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final PacketLengthsBuilder builder = new PacketLengthsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            // RFC does not specify which operand to use
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            final short length = parseLength(b);
            builder.setValue(ByteArray.bytesToInt(ByteArray.readBytes(nlri, length)));
            end = op.isEndOfList();
            plengths.add(builder.build());
        }
        return plengths;
    }

    private static List<Dscps> parseDscp(final ByteBuf nlri) {
        final List<Dscps> dscps = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final DscpsBuilder builder = new DscpsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            // RFC does not specify operator
            final NumericOperand op = parseNumeric(b);
            builder.setOp(op);
            builder.setValue(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Dscp(nlri.readUnsignedByte()));
            end = op.isEndOfList();
            dscps.add(builder.build());
        }
        return dscps;
    }

    private static List<Fragments> parseFragment(final ByteBuf nlri) {
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

    private static NumericOperand parseNumeric(final byte op) {
        final BitArray bs = BitArray.valueOf(op);
        return new NumericOperand(bs.get(AND_BIT), bs.get(END_OF_LIST), bs.get(EQUAL), bs.get(GREATER_THAN), bs.get(LESS_THAN));
    }

    private static BitmaskOperand parseBitmask(final byte op) {
        final BitArray bs = BitArray.valueOf(op);
        return new BitmaskOperand(bs.get(AND_BIT), bs.get(END_OF_LIST), bs.get(MATCH), bs.get(NOT));
    }

    @VisibleForTesting
    public static short parseLength(final byte op) {
        return (short) (1 << ((op & LENGTH_BITMASK) >> LENGTH_SHIFT));
    }

    private static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Fragment parseFragment(final byte fragment) {
        final BitArray bs = BitArray.valueOf(fragment);
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Fragment(bs.get(DONT_FRAGMENT), bs.get(FIRST_FRAGMENT), bs.get(IS_A_FRAGMENT), bs.get(LAST_FRAGMENT));
    }

    private static byte serializeFragment(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Fragment fragment) {
        final BitArray bs = new BitArray(Byte.SIZE);
        bs.set(DONT_FRAGMENT, fragment.isDoNot());
        bs.set(FIRST_FRAGMENT, fragment.isFirst());
        bs.set(IS_A_FRAGMENT, fragment.isIsA());
        bs.set(LAST_FRAGMENT, fragment.isLast());
        return bs.toByte();
    }

    public static String stringNlri(final DataContainerNode<?> route) {
        return stringNlri(extractFlowspec(route));
    }

    public static List<Flowspec> extractFlowspec(final DataContainerNode<?> route) {
        final List<Flowspec> fsList = new ArrayList<>();
        final Optional<DataContainerChild<? extends PathArgument, ?>> flowspecs = route.getChild(FLOWSPEC_NID);
        if (flowspecs.isPresent()) {
            for (final UnkeyedListEntryNode flowspec : ((UnkeyedListNode)flowspecs.get()).getValue()) {
                final FlowspecBuilder fs = new FlowspecBuilder();
                final Optional<DataContainerChild<? extends PathArgument, ?>> compType = flowspec.getChild(COMPONENT_TYPE_NID);
                if (compType.isPresent()) {
                    fs.setComponentType(componentTypeValue((String) compType.get().getValue()));
                }
                final ChoiceNode fsType = (ChoiceNode) flowspec.getChild(FLOWSPEC_TYPE_NID).get();
                if (fsType.getChild(DEST_PREFIX_NID).isPresent()) {
                    fs.setFlowspecType(new DestinationPrefixCaseBuilder().setDestinationPrefix(
                            new Ipv4Prefix((String) fsType.getChild(DEST_PREFIX_NID).get().getValue())).build());
                } else if (fsType.getChild(SOURCE_PREFIX_NID).isPresent()) {
                    fs.setFlowspecType(new SourcePrefixCaseBuilder().setSourcePrefix(new Ipv4Prefix((String) fsType.getChild(SOURCE_PREFIX_NID).get().getValue()))
                            .build());
                } else if (fsType.getChild(PROTOCOL_IP_NID).isPresent()) {
                    fs.setFlowspecType(new ProtocolIpCaseBuilder().setProtocolIps(createProtocolsIps((UnkeyedListNode) fsType.getChild(PROTOCOL_IP_NID).get())).build());
                } else if (fsType.getChild(PORTS_NID).isPresent()) {
                    fs.setFlowspecType(new PortCaseBuilder().setPorts(createPorts((UnkeyedListNode) fsType.getChild(PORTS_NID).get())).build());
                } else if (fsType.getChild(DEST_PORT_NID).isPresent()) {
                    fs.setFlowspecType(new DestinationPortCaseBuilder().setDestinationPorts(createDestinationPorts((UnkeyedListNode) fsType.getChild(DEST_PORT_NID).get())).build());
                } else if (fsType.getChild(SOURCE_PORT_NID).isPresent()) {
                    fs.setFlowspecType(new SourcePortCaseBuilder().setSourcePorts(createSourcePorts((UnkeyedListNode) fsType.getChild(SOURCE_PORT_NID).get())).build());
                } else if (fsType.getChild(ICMP_TYPE_NID).isPresent()) {
                    fs.setFlowspecType(new IcmpTypeCaseBuilder().setTypes(createTypes((UnkeyedListNode) fsType.getChild(ICMP_TYPE_NID).get())).build());
                } else if (fsType.getChild(ICMP_CODE_NID).isPresent()) {
                    fs.setFlowspecType(new IcmpCodeCaseBuilder().setCodes(createCodes((UnkeyedListNode) fsType.getChild(ICMP_CODE_NID).get())).build());
                } else if (fsType.getChild(TCP_FLAGS_NID).isPresent()) {
                    fs.setFlowspecType(new TcpFlagsCaseBuilder().setTcpFlags(createTcpFlags((UnkeyedListNode) fsType.getChild(TCP_FLAGS_NID).get())).build());
                } else if (fsType.getChild(PACKET_LENGTHS_NID).isPresent()) {
                    fs.setFlowspecType(new PacketLengthCaseBuilder().setPacketLengths(createPacketLengths((UnkeyedListNode) fsType.getChild(PACKET_LENGTHS_NID).get())).build());
                } else if (fsType.getChild(DSCP_NID).isPresent()) {
                    fs.setFlowspecType(new DscpCaseBuilder().setDscps(createDscpsLengths((UnkeyedListNode) fsType.getChild(DSCP_NID).get())).build());
                } else if (fsType.getChild(FRAGMENT_NID).isPresent()) {
                    fs.setFlowspecType(new FragmentCaseBuilder().setFragments(createFragments((UnkeyedListNode) fsType.getChild(FRAGMENT_NID).get())).build());
                }
                fsList.add(fs.build());
            }
        }
        return fsList;
    }

    private static NumericOperand createNumericOperand(final Set<String> opValues) {
        return new NumericOperand(opValues.contains(AND_BIT_VALUE), opValues.contains(END_OF_LIST_VALUE), opValues.contains(EQUALS_VALUE), opValues.contains(GREATER_THAN_VALUE), opValues.contains(LESS_THAN_VALUE));
    }

    private static BitmaskOperand createBitmaskOperand(final Set<String> opValues) {
        return new BitmaskOperand(opValues.contains(AND_BIT_VALUE), opValues.contains(END_OF_LIST_VALUE), opValues.contains(MATCH_VALUE), opValues.contains(NOT_VALUE));
    }

    private static Fragment createFragment(final Set<String> data) {
        return new Fragment(data.contains(DO_NOT_VALUE), data.contains(FIRST_VALUE), data.contains(IS_A_VALUE), data.contains(LAST_VALUE));
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

    private static List<Ports> createPorts(final UnkeyedListNode portsData) {
        final List<Ports> ports = new ArrayList<>();

        for (final UnkeyedListEntryNode node : portsData.getValue()) {
            final PortsBuilder portsBuilder = new PortsBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                portsBuilder.setOp(createNumericOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                portsBuilder.setValue((Integer) valueNode.get().getValue());
            }
            ports.add(portsBuilder.build());
        }

        return ports;
    }

    private static List<DestinationPorts> createDestinationPorts(final UnkeyedListNode destinationPortsData) {
        final List<DestinationPorts> destinationPorts = new ArrayList<>();

        for (final UnkeyedListEntryNode node : destinationPortsData.getValue()) {
            final DestinationPortsBuilder destPortsBuilder = new DestinationPortsBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                destPortsBuilder.setOp(createNumericOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                destPortsBuilder.setValue((Integer) valueNode.get().getValue());
            }
            destinationPorts.add(destPortsBuilder.build());
        }

        return destinationPorts;
    }

    private static List<SourcePorts> createSourcePorts(final UnkeyedListNode sourcePortsData) {
        final List<SourcePorts> sourcePorts = new ArrayList<>();

        for (final UnkeyedListEntryNode node : sourcePortsData.getValue()) {
            final SourcePortsBuilder sourcePortsBuilder = new SourcePortsBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                sourcePortsBuilder.setOp(createNumericOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                sourcePortsBuilder.setValue((Integer) valueNode.get().getValue());
            }
            sourcePorts.add(sourcePortsBuilder.build());
        }

        return sourcePorts;
    }

    private static List<Types> createTypes(final UnkeyedListNode typesData) {
        final List<Types> types = new ArrayList<>();

        for (final UnkeyedListEntryNode node : typesData.getValue()) {
            final TypesBuilder typesBuilder = new TypesBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                typesBuilder.setOp(createNumericOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                typesBuilder.setValue((Short) valueNode.get().getValue());
            }
            types.add(typesBuilder.build());
        }

        return types;
    }

    private static List<Codes> createCodes(final UnkeyedListNode codesData) {
        final List<Codes> codes = new ArrayList<>();

        for (final UnkeyedListEntryNode node : codesData.getValue()) {
            final CodesBuilder codesBuilder = new CodesBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                codesBuilder.setOp(createNumericOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                codesBuilder.setValue((Short) valueNode.get().getValue());
            }
            codes.add(codesBuilder.build());
        }

        return codes;
    }

    private static List<TcpFlags> createTcpFlags(final UnkeyedListNode tcpFlagsData) {
        final List<TcpFlags> tcpFlags = new ArrayList<>();

        for (final UnkeyedListEntryNode node : tcpFlagsData.getValue()) {
            final TcpFlagsBuilder tcpFlagsBuilder = new TcpFlagsBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                tcpFlagsBuilder.setOp(createBitmaskOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                tcpFlagsBuilder.setValue((Integer) valueNode.get().getValue());
            }
            tcpFlags.add(tcpFlagsBuilder.build());
        }

        return tcpFlags;
    }

    private static List<PacketLengths> createPacketLengths(final UnkeyedListNode packetLengthsData) {
        final List<PacketLengths> packetLengths = new ArrayList<>();

        for (final UnkeyedListEntryNode node : packetLengthsData.getValue()) {
            final PacketLengthsBuilder packetLengthsBuilder = new PacketLengthsBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                packetLengthsBuilder.setOp(createNumericOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                packetLengthsBuilder.setValue((Integer) valueNode.get().getValue());
            }
            packetLengths.add(packetLengthsBuilder.build());
        }

        return packetLengths;
    }

    private static List<Dscps> createDscpsLengths(final UnkeyedListNode dscpLengthsData) {
        final List<Dscps> dscpsLengths = new ArrayList<>();

        for (final UnkeyedListEntryNode node : dscpLengthsData.getValue()) {
            final DscpsBuilder dscpsLengthsBuilder = new DscpsBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                dscpsLengthsBuilder.setOp(createNumericOperand((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                dscpsLengthsBuilder.setValue(new Dscp((Short) valueNode.get().getValue()));
            }
            dscpsLengths.add(dscpsLengthsBuilder.build());
        }

        return dscpsLengths;
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

    // FIXME: use codec
    private static ComponentType componentTypeValue(final String compType) {
        switch (compType) {
        case "destination-prefix":
            return ComponentType.DestinationPrefix;
        case "source-prefix":
            return ComponentType.SourcePrefix;
        case "protocol-ip":
            return ComponentType.ProtocolIp;
        case "port":
            return ComponentType.Port;
        case "destination-port":
            return ComponentType.DestinationPort;
        case "source-port":
            return ComponentType.SourcePort;
        case "icmp-type":
            return ComponentType.IcmpType;
        case "icmp-code":
            return ComponentType.IcmpCode;
        case "tcp-flags":
            return ComponentType.TcpFlags;
        case "packet-length":
            return ComponentType.PacketLength;
        case "dscp":
            return ComponentType.Dscp;
        case "fragment":
            return ComponentType.Fragment;
        default:
            return null;
        }
    }

    @VisibleForTesting
    static final String stringNlri(final List<Flowspec> flows) {
        final StringBuilder buffer = new StringBuilder("all packets ");
        final Joiner joiner = Joiner.on(FLOW_SEPARATOR);
        joiner.appendTo(buffer, Iterables.transform(flows, new Function<Flowspec, String>() {
            @Override
            public String apply(final Flowspec input) {
                return encodeFlow(input);
            }
        }));
        return buffer.toString().replace("  ", " ");
    }

    private static String encodeFlow(final Flowspec flow) {
        final StringBuilder buffer = new StringBuilder();
        final FlowspecType value = flow.getFlowspecType();
        switch (flow.getComponentType()) {
        case DestinationPrefix:
            buffer.append("to ");
            buffer.append(((DestinationPrefixCase) value).getDestinationPrefix().getValue());
            break;
        case SourcePrefix:
            buffer.append("from ");
            buffer.append(((SourcePrefixCase) value).getSourcePrefix().getValue());
            break;
        case ProtocolIp:
            buffer.append("where protocol ");
            buffer.append(stringNumericTwo(((ProtocolIpCase) value).getProtocolIps()));
            break;
        case Port:
            buffer.append("where port ");
            buffer.append(stringNumericTwo(((PortCase) value).getPorts()));
            break;
        case DestinationPort:
            buffer.append("where destination port ");
            buffer.append(stringNumericTwo(((DestinationPortCase) value).getDestinationPorts()));
            break;
        case SourcePort:
            buffer.append("where source port ");
            buffer.append(stringNumericTwo(((SourcePortCase) value).getSourcePorts()));
            break;
        case IcmpType:
            buffer.append("where ICMP type ");
            buffer.append(stringNumericOne(((IcmpTypeCase) value).getTypes()));
            break;
        case IcmpCode:
            buffer.append("where ICMP code ");
            buffer.append(stringNumericOne(((IcmpCodeCase) value).getCodes()));
            break;
        case TcpFlags:
            buffer.append(stringTcpFlags(((TcpFlagsCase) value).getTcpFlags()));
            break;
        case PacketLength:
            buffer.append("where packet length ");
            buffer.append(stringNumericTwo(((PacketLengthCase) value).getPacketLengths()));
            break;
        case Dscp:
            buffer.append(stringDscp(((DscpCase) value).getDscps()));
            break;
        case Fragment:
            buffer.append(stringFragment(((FragmentCase) value).getFragments()));
            break;
        default:
            LOG.warn("Skipping unhandled component type {}" , flow.getComponentType());
            break;
        }
        return buffer.toString();
    }

    private static <T extends NumericTwoByteValue> String stringNumericTwo(final List<T> list) {
        final StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (final T item : list) {
            buffer.append(stringNumericOperand(item.getOp(), isFirst));
            buffer.append(item.getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

    private static <T extends NumericOneByteValue> String stringNumericOne(final List<T> list) {
        final StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (final T item : list) {
            buffer.append(stringNumericOperand(item.getOp(), isFirst));
            buffer.append(item.getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

    private static String stringNumericOperand(final NumericOperand op, final boolean isFirst) {
        final StringBuilder buffer = new StringBuilder();
        if (op == null) {
            return buffer.toString();
        }
        if (!op.isAndBit() && !isFirst) {
            buffer.append("or ");
        }
        if (op.isAndBit()) {
            buffer.append("and ");
        }
        if (op.isLessThan() && op.isEquals()) {
            buffer.append("is less than or equal to ");
            return buffer.toString();
        } else if (op.isGreaterThan() && op.isEquals()) {
            buffer.append("is greater than or equal to ");
            return buffer.toString();
        }
        if (op.isEquals()) {
            buffer.append("equals to ");
        }
        if (op.isLessThan()) {
            buffer.append("is less than ");
        }
        if (op.isGreaterThan()) {
            buffer.append("is greater than ");
        }
        return buffer.toString();
    }

    private static String stringTcpFlags(final List<TcpFlags> flags) {
        final StringBuilder buffer = new StringBuilder("where TCP flags ");
        boolean isFirst = true;
        for (final TcpFlags item : flags) {
            buffer.append(stringBitmaskOperand(item.getOp(), isFirst));
            buffer.append(item.getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

    private static String stringBitmaskOperand(final BitmaskOperand op, final boolean isFirst) {
        final StringBuilder buffer = new StringBuilder();
        if (!op.isAndBit() && !isFirst) {
            buffer.append("or ");
        }
        if (op.isAndBit()) {
            buffer.append("and ");
        }
        if (op.isMatch()) {
            buffer.append("does ");
            if (op.isNot()) {
                buffer.append("not ");
            }
            buffer.append("match ");
        } else if (op.isNot()) {
            buffer.append("is not ");
        }
        return buffer.toString();
    }

    private static String stringDscp(final List<Dscps> dscps) {
        final StringBuilder buffer = new StringBuilder("where DSCP ");
        boolean isFirst = true;
        for (final Dscps item : dscps) {
            buffer.append(stringNumericOperand(item.getOp(), isFirst));
            buffer.append(item.getValue().getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
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

    private static String stringFragment(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.Fragment fragment) {
        final StringBuilder buffer = new StringBuilder();
        if (fragment.isDoNot()) {
            buffer.append("'DO NOT' ");
        }
        if (fragment.isFirst()) {
            buffer.append("'IS FIRST' ");
        }
        if (fragment.isLast()) {
            buffer.append("'IS LAST' ");
        }
        if (fragment.isIsA()) {
            buffer.append("'IS A' ");
        }
        return buffer.toString();
    }
}
