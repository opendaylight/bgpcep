/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInts;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.ComponentType;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.reach.nlri.AdvertizedRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.mp.unreach.nlri.WithdrawnRoutesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSNlriParser implements NlriParser, NlriSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(FSNlriParser.class);

    private static final int NLRI_LENGTH = 1;
    private static final int NLRI_LENGTH_EXTENDED = 2;
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
        Preconditions.checkArgument(attribute instanceof PathAttributes, "Attribute parameter is not a PathAttribute object.");
        final PathAttributes pathAttributes = (PathAttributes) attribute;
        final PathAttributes1 pathAttributes1 = pathAttributes.getAugmentation(PathAttributes1.class);
        final PathAttributes2 pathAttributes2 = pathAttributes.getAugmentation(PathAttributes2.class);
        if (pathAttributes1 != null) {
            final AdvertizedRoutes routes = (pathAttributes1.getMpReachNlri()).getAdvertizedRoutes();
            if (routes != null &&
                routes.getDestinationType()
                instanceof
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) {
                final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase
                flowspecCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) routes.getDestinationType();
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
     * @param flow flowspec NLRI to be serialized
     */
    public static void serializeNlri(final List<Flowspec> flows, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final Flowspec flow : flows) {
            nlriByteBuf.writeByte(flow.getComponentType().getIntValue());
            final FlowspecType value = flow.getFlowspecType();
            switch (flow.getComponentType()) {
            case DestinationPrefix:
                nlriByteBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(((DestinationPrefixCase)value).getDestinationPrefix()));
                break;
            case SourcePrefix:
                nlriByteBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(((SourcePrefixCase)value).getSourcePrefix()));
                break;
            case ProtocolIp:
                serializeNumericTwoByteValue(((ProtocolIpCase)value).getProtocolIps(), nlriByteBuf);
                break;
            case Port:
                serializeNumericTwoByteValue(((PortCase)value).getPorts(), nlriByteBuf);
                break;
            case DestinationPort:
                serializeNumericTwoByteValue(((DestinationPortCase)value).getDestinationPorts(), nlriByteBuf);
                break;
            case SourcePort:
                serializeNumericTwoByteValue(((SourcePortCase)value).getSourcePorts(), nlriByteBuf);
                break;
            case IcmpType:
                serializeNumericOneByteValue(((IcmpTypeCase)value).getTypes(), nlriByteBuf);
                break;
            case IcmpCode:
                serializeNumericOneByteValue(((IcmpCodeCase)value).getCodes(), nlriByteBuf);
                break;
            case TcpFlags:
                final List<TcpFlags> flags = ((TcpFlagsCase)value).getTcpFlags();
                for (final TcpFlags flag : flags) {
                    final ByteBuf flagsBuf = Unpooled.buffer();
                    writeShortest(flag.getValue(), flagsBuf);
                    serializeBitmaskOperand(flag.getOp(), flagsBuf.readableBytes(), nlriByteBuf);
                    nlriByteBuf.writeBytes(flagsBuf);
                }
                break;
            case PacketLength:
                serializeNumericTwoByteValue(((PacketLengthCase)value).getPacketLengths(), nlriByteBuf);
                break;
            case Dscp:
                final List<Dscps> dscps = ((DscpCase)value).getDscps();
                for (final Dscps dscp : dscps) {
                    serializeNumericOperand(dscp.getOp(), 1, nlriByteBuf);
                    writeShortest(dscp.getValue().getValue(), nlriByteBuf);
                }
                break;
            case Fragment:
                final List<Fragments> fragments = ((FragmentCase)value).getFragments();
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
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder()
                .setDestinationFlowspec(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150114.update.path.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder().setFlowspec(dst).build()).build()).build());
    }

    public static List<Flowspec> parseNlri(final ByteBuf nlri) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return null;
        }
        final List<Flowspec> fss = new ArrayList<>();

        // length field can be one or two bytes (if needed)
        // check the length of nlri to see how many bytes we can skip
        final int length = nlri.readableBytes();
        nlri.skipBytes(length > MAX_NLRI_LENGTH_ONE_BYTE ? NLRI_LENGTH_EXTENDED : NLRI_LENGTH);

        while(nlri.isReadable()) {
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
}
