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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.BitmaskOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Fragment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.NumericOperand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.DestinationPortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.DestinationPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.DscpCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.DscpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.FragmentCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.FragmentCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.IcmpCodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.IcmpCodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.IcmpTypeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.IcmpTypeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.PacketLengthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.PacketLengthCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.PortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.PortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.SourcePortCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.SourcePortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.TcpFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.TcpFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.destination.port._case.DestinationPorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.destination.port._case.DestinationPortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.dscp._case.Dscps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.dscp._case.DscpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.fragment._case.Fragments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.fragment._case.FragmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.icmp.code._case.Codes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.icmp.code._case.CodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.icmp.type._case.Types;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.icmp.type._case.TypesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.packet.length._case.PacketLengths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.packet.length._case.PacketLengthsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.port._case.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.port._case.PortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.source.port._case.SourcePorts;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.source.port._case.SourcePortsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.flowspec.type.tcp.flags._case.TcpFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.DestinationPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv4.flowspec.flowspec.type.SourcePrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
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

public abstract class AbstractFSNlriParser implements NlriParser, NlriSerializer {

    // component types values
    protected static final int DESTINATION_PREFIX_VALUE = 1;
    protected static final int SOURCE_PREFIX_VALUE = 2;
    protected static final int PORT_VALUE = 4;
    protected static final int DESTINATION_PORT_VALUE = 5;
    protected static final int SOURCE_PORT_VALUE = 6;
    protected static final int ICMP_TYPE_VALUE = 7;
    protected static final int ICMP_CODE_VALUE = 8;
    protected static final int TCP_FLAGS_VALUE = 9;
    protected static final int PACKET_LENGTH_VALUE = 10;
    protected static final int DSCP_VALUE = 11;
    protected static final int FRAGMENT_VALUE = 12;

    @VisibleForTesting
    static final NodeIdentifier FLOWSPEC_NID = new NodeIdentifier(Flowspec.QNAME);
    @VisibleForTesting
    protected static final NodeIdentifier FLOWSPEC_TYPE_NID = new NodeIdentifier(FlowspecType.QNAME);
    @VisibleForTesting
    static final NodeIdentifier DEST_PREFIX_NID = new NodeIdentifier(QName.create(DestinationPrefixCase.QNAME, "destination-prefix").intern());
    @VisibleForTesting
    static final NodeIdentifier SOURCE_PREFIX_NID = new NodeIdentifier(QName.create(SourcePrefixCase.QNAME, "source-prefix").intern());
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
    static final NodeIdentifier OP_NID = new NodeIdentifier(QName.create("urn:opendaylight:params:xml:ns:yang:bgp-flowspec","2015-08-07","op"));
    @VisibleForTesting
    static final NodeIdentifier VALUE_NID = new NodeIdentifier(QName.create("urn:opendaylight:params:xml:ns:yang:bgp-flowspec","2015-08-07","value"));

    protected static final int NLRI_LENGTH = 1;
    protected static final int NLRI_LENGTH_EXTENDED = 2;

    /**
     * Add this constant to length value to achieve all ones in the leftmost nibble.
     */
    private static final int LENGTH_MAGIC = 61440;
    private static final int MAX_NLRI_LENGTH = 4095;
    private static final int MAX_NLRI_LENGTH_ONE_BYTE = 240;

    @VisibleForTesting
    static final String DO_NOT_VALUE = "do-not";
    @VisibleForTesting
    static final String FIRST_VALUE = "first";
    @VisibleForTesting
    static final String LAST_VALUE = "last";
    @VisibleForTesting
    static final String IS_A_VALUE = "is-a";

    protected static final int LAST_FRAGMENT = 4;
    protected static final int FIRST_FRAGMENT = 5;
    protected static final int IS_A_FRAGMENT = 6;
    protected static final int DONT_FRAGMENT = 7;

    private static final String FLOW_SEPARATOR = " AND ";

    protected abstract void serializeMpReachNlri(final Attributes1 pathAttributes, final ByteBuf byteAggregator);

    protected abstract void serializeMpUnreachNlri(final Attributes2 pathAttributes, final ByteBuf byteAggregator);

    protected abstract void serializeSpecificFSType(final FlowspecType fsType, final ByteBuf nlriByteBuf);

    protected abstract byte serializeFragment(final Fragment fragment);

    protected abstract Fragment parseFragment(final byte fragment);

    protected abstract void setSpecificFlowspecType(final FlowspecBuilder builder, final short type, final ByteBuf nlri);

    public abstract void extractSpecificFlowspec(final ChoiceNode fsType, final FlowspecBuilder fsBuilder);

    abstract DestinationType createWidthdrawnDestinationType(final List<Flowspec> dst);

    abstract DestinationType createAdvertizedRoutesDestinationType(final List<Flowspec> dst);

    protected abstract void stringSpecificFSNlriType(final FlowspecType value, final StringBuilder buffer);

    @Override
    public final void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        serializeMpReachNlri(pathAttributes1, byteAggregator);
        serializeMpUnreachNlri(pathAttributes2, byteAggregator);
    }

    @Override
    public final void parseNlri(final ByteBuf nlri, final MpUnreachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<Flowspec> dst = parseNlri(nlri);

        builder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setDestinationType(createWidthdrawnDestinationType(dst)).build());
    }

    @Override
    public final void parseNlri(final ByteBuf nlri, final MpReachNlriBuilder builder) throws BGPParsingException {
        if (!nlri.isReadable()) {
            return;
        }
        final List<Flowspec> dst = parseNlri(nlri);
        builder.setAdvertizedRoutes(new AdvertizedRoutesBuilder().setDestinationType(createAdvertizedRoutesDestinationType(dst)).build());
    }

    /**
     * Serializes Flowspec NLRI to ByteBuf.
     *
     * @param flows flowspec NLRI to be serialized
     * @param buffer where flowspec NLRI will be serialized
     */
    public final void serializeNlri(final List<Flowspec> flows, final ByteBuf buffer) {
        final ByteBuf nlriByteBuf = Unpooled.buffer();
        for (final Flowspec flow : flows) {
            serializeFlowspec(flow, nlriByteBuf);
        }
        Preconditions.checkState(nlriByteBuf.readableBytes() <= MAX_NLRI_LENGTH, "Maximum length of Flowspec NLRI reached.");
        if (nlriByteBuf.readableBytes() <= MAX_NLRI_LENGTH_ONE_BYTE) {
            buffer.writeByte(nlriByteBuf.readableBytes());
        } else {
            buffer.writeShort(nlriByteBuf.readableBytes() + LENGTH_MAGIC);
        }
        buffer.writeBytes(nlriByteBuf);
    }

    protected static final void serializeTcpFlags(final List<TcpFlags> flags, final ByteBuf nlriByteBuf) {
        for (final TcpFlags flag : flags) {
            final ByteBuf flagsBuf = Unpooled.buffer();
            Util.writeShortest(flag.getValue(), flagsBuf);
            BitmaskOperandParser.INSTANCE.serialize(flag.getOp(), flagsBuf.readableBytes(), nlriByteBuf);
            nlriByteBuf.writeBytes(flagsBuf);
        }
    }

    protected static final void serializeDscps(final List<Dscps> dscps, final ByteBuf nlriByteBuf) {
        for (final Dscps dscp : dscps) {
            NumericOneByteOperandParser.INSTANCE.serialize(dscp.getOp(), 1, nlriByteBuf);
            Util.writeShortest(dscp.getValue().getValue(), nlriByteBuf);
        }
    }

    protected final void serializeFragments(final List<Fragments> fragments, final ByteBuf nlriByteBuf) {
        for (final Fragments fragment : fragments) {
            BitmaskOperandParser.INSTANCE.serialize(fragment.getOp(), 1, nlriByteBuf);
            nlriByteBuf.writeByte(serializeFragment(fragment.getValue()));
        }
    }

    protected final void serializeFlowspec(final Flowspec flow, final ByteBuf nlriByteBuf) {
        final FlowspecType fsType = flow.getFlowspecType();
        if (fsType instanceof PortCase) {
            nlriByteBuf.writeByte(PORT_VALUE);
            NumericTwoByteOperandParser.INSTANCE.serialize(((PortCase) fsType).getPorts(), nlriByteBuf);
        } else if (fsType instanceof DestinationPortCase) {
            nlriByteBuf.writeByte(DESTINATION_PORT_VALUE);
            NumericTwoByteOperandParser.INSTANCE.serialize(((DestinationPortCase) fsType).getDestinationPorts(), nlriByteBuf);
        } else if (fsType instanceof SourcePortCase) {
            nlriByteBuf.writeByte(SOURCE_PORT_VALUE);
            NumericTwoByteOperandParser.INSTANCE.serialize(((SourcePortCase) fsType).getSourcePorts(), nlriByteBuf);
        } else if (fsType instanceof IcmpTypeCase) {
            nlriByteBuf.writeByte(ICMP_TYPE_VALUE);
            NumericOneByteOperandParser.INSTANCE.serialize(((IcmpTypeCase) fsType).getTypes(), nlriByteBuf);
        } else if (fsType instanceof IcmpCodeCase) {
            nlriByteBuf.writeByte(ICMP_CODE_VALUE);
            NumericOneByteOperandParser.INSTANCE.serialize(((IcmpCodeCase) fsType).getCodes(), nlriByteBuf);
        } else if (fsType instanceof TcpFlagsCase) {
            nlriByteBuf.writeByte(TCP_FLAGS_VALUE);
            serializeTcpFlags(((TcpFlagsCase) fsType).getTcpFlags(), nlriByteBuf);
        } else if (fsType instanceof PacketLengthCase) {
            nlriByteBuf.writeByte(PACKET_LENGTH_VALUE);
            NumericTwoByteOperandParser.INSTANCE.serialize(((PacketLengthCase) fsType).getPacketLengths(), nlriByteBuf);
        } else if (fsType instanceof DscpCase) {
            nlriByteBuf.writeByte(DSCP_VALUE);
            serializeDscps(((DscpCase) fsType).getDscps(), nlriByteBuf);
        } else {
            serializeSpecificFSType(fsType, nlriByteBuf);
        }
    }

    /**
     * Parses Flowspec NLRI into list of Flowspec.
     *
     * @param nlri byte representation of NLRI which will be parsed
     * @return list of Flowspec
     */
    public final List<Flowspec> parseNlri(final ByteBuf nlri) throws BGPParsingException {
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
            final short type = nlri.readUnsignedByte();
            setFlowspecType(builder, type, nlri);
            fss.add(builder.build());
        }
        return fss;
    }

    protected final void setFlowspecType(final FlowspecBuilder builder, final short type, final ByteBuf nlri) {
        switch (type) {
        case PORT_VALUE:
            builder.setFlowspecType(new PortCaseBuilder().setPorts(parsePort(nlri)).build());
            break;
        case DESTINATION_PORT_VALUE:
            builder.setFlowspecType(new DestinationPortCaseBuilder().setDestinationPorts(parseDestinationPort(nlri)).build());
            break;
        case SOURCE_PORT_VALUE:
            builder.setFlowspecType(new SourcePortCaseBuilder().setSourcePorts(parseSourcePort(nlri)).build());
            break;
        case ICMP_TYPE_VALUE:
            builder.setFlowspecType(new IcmpTypeCaseBuilder().setTypes(parseIcmpType(nlri)).build());
            break;
        case ICMP_CODE_VALUE:
            builder.setFlowspecType(new IcmpCodeCaseBuilder().setCodes(parseIcmpCode(nlri)).build());
            break;
        case TCP_FLAGS_VALUE:
            builder.setFlowspecType(new TcpFlagsCaseBuilder().setTcpFlags(parseTcpFlags(nlri)).build());
            break;
        case PACKET_LENGTH_VALUE:
            builder.setFlowspecType(new PacketLengthCaseBuilder().setPacketLengths(parsePacketLength(nlri)).build());
            break;
        case DSCP_VALUE:
            builder.setFlowspecType(new DscpCaseBuilder().setDscps(parseDscp(nlri)).build());
            break;
        default:
            setSpecificFlowspecType(builder, type, nlri);
            break;
        }
    }

    private static List<Ports> parsePort(final ByteBuf nlri) {
        final List<Ports> ports = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final PortsBuilder builder = new PortsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            final short length = AbstractOperandParser.parseLength(b);
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
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            final short length = AbstractOperandParser.parseLength(b);
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
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            final short length = AbstractOperandParser.parseLength(b);
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
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
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
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
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
            final BitmaskOperand op = BitmaskOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            final short length = AbstractOperandParser.parseLength(b);
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
            final NumericOperand op = NumericTwoByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            final short length = AbstractOperandParser.parseLength(b);
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
            final NumericOperand op = NumericOneByteOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            builder.setValue(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Dscp(nlri.readUnsignedByte()));
            end = op.isEndOfList();
            dscps.add(builder.build());
        }
        return dscps;
    }

    protected final List<Fragments> parseFragment(final ByteBuf nlri) {
        final List<Fragments> fragments = new ArrayList<>();
        boolean end = false;
        // we can do this as all fields will be rewritten in the cycle
        final FragmentsBuilder builder = new FragmentsBuilder();
        while (!end) {
            final byte b = nlri.readByte();
            final BitmaskOperand op = BitmaskOperandParser.INSTANCE.parse(b);
            builder.setOp(op);
            builder.setValue(parseFragment(nlri.readByte()));
            end = op.isEndOfList();
            fragments.add(builder.build());
        }
        return fragments;
    }

    public final String stringNlri(final DataContainerNode<?> route) {
        return stringNlri(extractFlowspec(route));
    }

    public final List<Flowspec> extractFlowspec(final DataContainerNode<?> route) {
        final List<Flowspec> fsList = new ArrayList<>();
        final Optional<DataContainerChild<? extends PathArgument, ?>> flowspecs = route.getChild(FLOWSPEC_NID);
        if (flowspecs.isPresent()) {
            for (final UnkeyedListEntryNode flowspec : ((UnkeyedListNode)flowspecs.get()).getValue()) {
                final FlowspecBuilder fsBuilder = new FlowspecBuilder();
                final Optional<DataContainerChild<?, ?>> flowspecType = flowspec.getChild(FLOWSPEC_TYPE_NID);
                if (flowspecType.isPresent()) {
                    final ChoiceNode fsType = (ChoiceNode) flowspecType.get();
                    processFlowspecType(fsType, fsBuilder);
                }
                fsList.add(fsBuilder.build());
            }
        }
        return fsList;
    }

    private void processFlowspecType(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        if (fsType.getChild(PORTS_NID).isPresent()) {
            fsBuilder.setFlowspecType(new PortCaseBuilder().setPorts(createPorts((UnkeyedListNode) fsType.getChild(PORTS_NID).get())).build());
        } else if (fsType.getChild(DEST_PORT_NID).isPresent()) {
            fsBuilder.setFlowspecType(new DestinationPortCaseBuilder().setDestinationPorts(createDestinationPorts((UnkeyedListNode) fsType.getChild(DEST_PORT_NID).get())).build());
        } else if (fsType.getChild(SOURCE_PORT_NID).isPresent()) {
            fsBuilder.setFlowspecType(new SourcePortCaseBuilder().setSourcePorts(createSourcePorts((UnkeyedListNode) fsType.getChild(SOURCE_PORT_NID).get())).build());
        } else if (fsType.getChild(ICMP_TYPE_NID).isPresent()) {
            fsBuilder.setFlowspecType(new IcmpTypeCaseBuilder().setTypes(createTypes((UnkeyedListNode) fsType.getChild(ICMP_TYPE_NID).get())).build());
        } else if (fsType.getChild(ICMP_CODE_NID).isPresent()) {
            fsBuilder.setFlowspecType(new IcmpCodeCaseBuilder().setCodes(createCodes((UnkeyedListNode) fsType.getChild(ICMP_CODE_NID).get())).build());
        } else if (fsType.getChild(TCP_FLAGS_NID).isPresent()) {
            fsBuilder.setFlowspecType(new TcpFlagsCaseBuilder().setTcpFlags(createTcpFlags((UnkeyedListNode) fsType.getChild(TCP_FLAGS_NID).get())).build());
        } else if (fsType.getChild(PACKET_LENGTHS_NID).isPresent()) {
            fsBuilder.setFlowspecType(new PacketLengthCaseBuilder().setPacketLengths(createPacketLengths((UnkeyedListNode) fsType.getChild(PACKET_LENGTHS_NID).get())).build());
        } else if (fsType.getChild(DSCP_NID).isPresent()) {
            fsBuilder.setFlowspecType(new DscpCaseBuilder().setDscps(createDscpsLengths((UnkeyedListNode) fsType.getChild(DSCP_NID).get())).build());
        } else if (fsType.getChild(FRAGMENT_NID).isPresent()) {
            fsBuilder.setFlowspecType(new FragmentCaseBuilder().setFragments(createFragments((UnkeyedListNode) fsType.getChild(FRAGMENT_NID).get())).build());
        } else {
            extractSpecificFlowspec(fsType, fsBuilder);
        }
    }

    private static List<Ports> createPorts(final UnkeyedListNode portsData) {
        final List<Ports> ports = new ArrayList<>();

        for (final UnkeyedListEntryNode node : portsData.getValue()) {
            final PortsBuilder portsBuilder = new PortsBuilder();
            final Optional<DataContainerChild<? extends PathArgument, ?>> opValue = node.getChild(OP_NID);
            if (opValue.isPresent()) {
                portsBuilder.setOp(NumericTwoByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
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
                destPortsBuilder.setOp(NumericTwoByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
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
                sourcePortsBuilder.setOp(NumericTwoByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
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
                typesBuilder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
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
                codesBuilder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
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
                tcpFlagsBuilder.setOp(BitmaskOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
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
                packetLengthsBuilder.setOp(NumericTwoByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
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
                dscpsLengthsBuilder.setOp(NumericOneByteOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
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
                fragmentsBuilder.setOp(BitmaskOperandParser.INSTANCE.create((Set<String>) opValue.get().getValue()));
            }
            final Optional<DataContainerChild<? extends PathArgument, ?>> valueNode = node.getChild(VALUE_NID);
            if (valueNode.isPresent()) {
                fragmentsBuilder.setValue(createFragment((Set<String>) valueNode.get().getValue()));
            }
            fragments.add(fragmentsBuilder.build());
        }

        return fragments;
    }

    private static Fragment createFragment(final Set<String> data) {
        return new Fragment(data.contains(DO_NOT_VALUE), data.contains(FIRST_VALUE), data.contains(IS_A_VALUE), data.contains(LAST_VALUE));
    }

    final String stringNlri(final List<Flowspec> flows) {
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

    @VisibleForTesting
    private String encodeFlow(final Flowspec flow) {
        final StringBuilder buffer = new StringBuilder();
        final FlowspecType value = flow.getFlowspecType();
        if (value instanceof PortCase) {
            buffer.append("where port ");
            buffer.append(NumericTwoByteOperandParser.INSTANCE.toString(((PortCase) value).getPorts()));
        } else if (value instanceof DestinationPortCase) {
            buffer.append("where destination port ");
            buffer.append(NumericTwoByteOperandParser.INSTANCE.toString(((DestinationPortCase) value).getDestinationPorts()));
        } else if (value instanceof SourcePortCase) {
            buffer.append("where source port ");
            buffer.append(NumericTwoByteOperandParser.INSTANCE.toString(((SourcePortCase) value).getSourcePorts()));
        } else if (value instanceof IcmpTypeCase) {
            buffer.append("where ICMP type ");
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(((IcmpTypeCase) value).getTypes()));
        } else if (value instanceof IcmpCodeCase) {
            buffer.append("where ICMP code ");
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(((IcmpCodeCase) value).getCodes()));
        } else if (value instanceof TcpFlagsCase) {
            buffer.append(stringTcpFlags(((TcpFlagsCase) value).getTcpFlags()));
        } else if (value instanceof PacketLengthCase) {
            buffer.append("where packet length ");
            buffer.append(NumericTwoByteOperandParser.INSTANCE.toString(((PacketLengthCase) value).getPacketLengths()));
        } else if (value instanceof DscpCase) {
            buffer.append(stringDscp(((DscpCase) value).getDscps()));
        } else if (value instanceof FragmentCase) {
            buffer.append(stringFragment(((FragmentCase) value).getFragments()));
        } else {
            stringSpecificFSNlriType(value, buffer);
        }
        return buffer.toString();
    }

    private static String stringTcpFlags(final List<TcpFlags> flags) {
        final StringBuilder buffer = new StringBuilder("where TCP flags ");
        boolean isFirst = true;
        for (final TcpFlags item : flags) {
            buffer.append(BitmaskOperandParser.INSTANCE.toString(item.getOp(), isFirst));
            buffer.append(item.getValue());
            buffer.append(' ');
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

    private static String stringDscp(final List<Dscps> dscps) {
        final StringBuilder buffer = new StringBuilder("where DSCP ");
        boolean isFirst = true;
        for (final Dscps item : dscps) {
            buffer.append(NumericOneByteOperandParser.INSTANCE.toString(item.getOp(), isFirst));
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
            buffer.append(BitmaskOperandParser.INSTANCE.toString(item.getOp(), isFirst));
            buffer.append(stringFragment(item.getValue()));
            if (isFirst) {
                isFirst = false;
            }
        }
        return buffer.toString();
    }

    private static String stringFragment(final Fragment fragment) {
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
