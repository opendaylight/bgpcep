/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrAlgorithmBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrSidLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrSidLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SidLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SidLabelBinding.SidLabelFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SrCapabilities.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.label.binding.SubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.label.binding.SubTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.SubtlvType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.EroMetricCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.EroMetricCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv4EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv4EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv4EroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv4EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv6EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv6EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv6EroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.Ipv6EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.SidLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.SidLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.UnnumberedEroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.UnnumberedEroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.UnnumberedEroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.sid.sub.tlvs.subtlv.type.UnnumberedEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SrNodeAttributesParser {

    private static final Logger LOG = LoggerFactory.getLogger(SrNodeAttributesParser.class);

    private SrNodeAttributesParser() {
        throw new UnsupportedOperationException();
    }

    private static final int TYPE = 149; // suggested value
    private static final int FLAGS_SIZE = 8;

    /* SR Capabilities flags */
    private static final int IPV4 = 0;
    private static final int IPV6 = 1;

    /* SID Label flags */
    private static final int AFI = 0;
    private static final int MIRROR = 1;

    /* SID Label Tlv types */
    private static final int SID_TLV_TYPE = 1;
    private static final int ERO_METRIC = 2;
    private static final int ERO_IPV4 = 3;
    private static final int ERO_IPV6 = 4;
    private static final int UNNUMBERED_ERO = 5;
    private static final int BACKUP_IPV4 = 6;
    private static final int BACKUP_IPV6 = 7;
    private static final int UNNUMBERED_BACKUP_ERO = 8;

    private static final int UNNUMBERED_4_SIZE = 8;
    private static final int UNNUMBERED_16_SIZE = 20;

    private static final byte LOOSE = (byte) 128;

//    subs.add(new SubTlvsBuilder().setSubtlvType(new SidLabelCaseBuilder().setSid(new SidLabel(new byte[] {1,2,3,4})).build()).build());
//    subs.add(new SubTlvsBuilder().setSubtlvType(new EroMetricCaseBuilder().setEroMetric(new TeMetric((long) 8)).build()).build());
//    subs.add(new SubTlvsBuilder().setSubtlvType(new Ipv4EroCaseBuilder().setLoose(true).setAddress(new IpAddress(new Ipv4Address("10.0.0.1"))).build()).build());
//    subs.add(new SubTlvsBuilder().setSubtlvType(new Ipv4EroBackupCaseBuilder().setLoose(true).setAddress(new IpAddress(new Ipv4Address("10.0.0.2"))).build()).build());
//    subs.add(new SubTlvsBuilder().setSubtlvType(new UnnumberedEroCaseBuilder().setLoose(true).setRouterId(new byte[] {1,2,3,4}).setInterfaceId((long)500).build()).build());

    private static List<SubTlvs> parseSidSubtlvs(final ByteBuf buffer) {
        final List<SubTlvs> subs = new ArrayList<>();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedByte();
            final int length = buffer.readUnsignedByte();
            final ByteBuf value = buffer.readSlice(length);
            SubtlvType sub = null;
            switch (type) {
            case SID_TLV_TYPE:
                sub = new SidLabelCaseBuilder().setSid(new SidLabel(ByteArray.readAllBytes(value))).build();
                break;
            case ERO_METRIC:
                sub = new EroMetricCaseBuilder().setEroMetric(new TeMetric(readNumber(length, value))).build();
                break;
            case ERO_IPV4:
                final Ipv4EroCaseBuilder ero4 = new Ipv4EroCaseBuilder().setLoose(value.readUnsignedByte() != 0);
                ero4.setAddress(new IpAddress(Ipv4Util.addressForByteBuf(value)));
                sub = ero4.build();
                break;
            case ERO_IPV6:
                final Ipv6EroCaseBuilder ero6 = new Ipv6EroCaseBuilder().setLoose(value.readUnsignedByte() != 0);
                ero6.setAddress(new IpAddress(Ipv6Util.addressForByteBuf(value)));
                sub = ero6.build();
                break;
            case UNNUMBERED_ERO:
                final UnnumberedEroCaseBuilder un = new UnnumberedEroCaseBuilder().setLoose(value.readUnsignedByte() != 0);
                un.setRouterId(readRouterId(length, value));
                un.setInterfaceId((long)value.readInt());
                sub = un.build();
                break;
            case BACKUP_IPV4:
                final Ipv4EroBackupCaseBuilder erob4 = new Ipv4EroBackupCaseBuilder().setLoose(value.readUnsignedByte() != 0);
                erob4.setAddress(new IpAddress(Ipv4Util.addressForByteBuf(value)));
                sub = erob4.build();
                break;
            case BACKUP_IPV6:
                final Ipv6EroBackupCaseBuilder erob6 = new Ipv6EroBackupCaseBuilder().setLoose(value.readUnsignedByte() != 0);
                erob6.setAddress(new IpAddress(Ipv6Util.addressForByteBuf(value)));
                sub = erob6.build();
                break;
            case UNNUMBERED_BACKUP_ERO:
                final UnnumberedEroBackupCaseBuilder unb = new UnnumberedEroBackupCaseBuilder().setLoose(value.readUnsignedByte() != 0);
                unb.setRouterId(readRouterId(length, value));
                unb.setInterfaceId((long)value.readInt());
                sub = unb.build();
                break;
            default:
                LOG.debug("Unknown SID Label Subtlv found, type {}", type);
                // we don't want to add null sub-tlv, so skip to next loop iteration
                continue;
            }
            subs.add(new SubTlvsBuilder().setSubtlvType(sub).build());
        }
        return subs;
    }

    private static byte[] readRouterId(final int length, final ByteBuf value) {
        // when length is 8(+1 byte flags+reserved): read 4 bytes
        // when length is 20(+1 byte flags+reserved): read 16 bytes
        if (length == UNNUMBERED_4_SIZE+1) {
            return ByteArray.readBytes(value, Ipv4Util.IP4_LENGTH);
        }
        return ByteArray.readBytes(value, Ipv6Util.IPV6_LENGTH);
    }

    public static SrSidLabel parseSidLabelBinding(final ByteBuf buffer) {
        final SrSidLabelBuilder builder = new SrSidLabelBuilder();
        // read TYPE and LENGTH (2B)
        buffer.readUnsignedShort();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        builder.setSidLabelFlags(new SidLabelFlags(flags.get(AFI), flags.get(MIRROR)));
        builder.setWeight(new Weight(buffer.readUnsignedByte()));
        builder.setValueRange(buffer.readUnsignedShort());
        final int bitLength = buffer.getUnsignedByte(buffer.readerIndex());
        IpPrefix prefix = null;
        if (bitLength / Byte.SIZE == Ipv4Util.IP4_LENGTH) {
            prefix = new IpPrefix(Ipv4Util.prefixForByteBuf(buffer));
        } else {
            prefix = new IpPrefix(Ipv6Util.prefixForByteBuf(buffer));
        }
        builder.setFecPrefix(prefix);
        builder.setSubTlvs(parseSidSubtlvs(buffer));
        return builder.build();
    }

    private static byte serializeLoose(final boolean loose) {
        return loose ? LOOSE : 0;
    }

    private static void serializeSidSubtlvs(final List<SubTlvs> subTlvs, final ByteBuf buffer) {
        for (final SubTlvs sub : subTlvs) {
            final SubtlvType type = sub.getSubtlvType();
            if (type instanceof SidLabelCase) {
                TlvUtil.writeSrTLV(SID_TLV_TYPE, Unpooled.wrappedBuffer(((SidLabelCase)type).getSid().getValue()), buffer);
            } else if (type instanceof EroMetricCase) {
                final ByteBuf b = Unpooled.buffer();
                writeMinimumBytes(((EroMetricCase)type).getEroMetric().getValue(), b);
                TlvUtil.writeSrTLV(ERO_METRIC, b, buffer);
            } else if (type instanceof Ipv4EroCase) {
                final ByteBuf b = Unpooled.buffer(Ipv4Util.IP4_LENGTH + FLAGS_SIZE);
                final Ipv4EroCase ero = (Ipv4EroCase)type;
                b.writeByte(serializeLoose(ero.isLoose()));
                ByteBufWriteUtil.writeIpv4Address(ero.getAddress().getIpv4Address(), b);
                TlvUtil.writeSrTLV(ERO_IPV4, b, buffer);
            } else if (type instanceof Ipv6EroCase) {
                final ByteBuf b = Unpooled.buffer(Ipv6Util.IPV6_LENGTH + FLAGS_SIZE);
                final Ipv6EroCase ero = (Ipv6EroCase)type;
                b.writeByte(serializeLoose(ero.isLoose()));
                ByteBufWriteUtil.writeIpv6Address(ero.getAddress().getIpv6Address(), b);
                TlvUtil.writeSrTLV(ERO_IPV6, b, buffer);
            } else if (type instanceof UnnumberedEroCase) {
                final UnnumberedEroCase ero = (UnnumberedEroCase)type;
                final ByteBuf b = Unpooled.buffer();
                b.writeByte(serializeLoose(ero.isLoose()));
                b.writeBytes(ero.getRouterId());
                b.writeInt(ero.getInterfaceId().intValue());
                TlvUtil.writeSrTLV(UNNUMBERED_ERO, b, buffer);
            } else if (type instanceof Ipv4EroBackupCase) {
                final ByteBuf b = Unpooled.buffer(Ipv4Util.IP4_LENGTH + FLAGS_SIZE);
                final Ipv4EroBackupCase ero = (Ipv4EroBackupCase)type;
                b.writeByte(serializeLoose(ero.isLoose()));
                ByteBufWriteUtil.writeIpv4Address(ero.getAddress().getIpv4Address(), b);
                TlvUtil.writeSrTLV(BACKUP_IPV4, b, buffer);
            } else if (type instanceof Ipv6EroBackupCase) {
                final ByteBuf b = Unpooled.buffer(Ipv6Util.IPV6_LENGTH + FLAGS_SIZE);
                final Ipv6EroBackupCase ero = (Ipv6EroBackupCase)type;
                b.writeByte(serializeLoose(ero.isLoose()));
                ByteBufWriteUtil.writeIpv6Address(ero.getAddress().getIpv6Address(), b);
                TlvUtil.writeSrTLV(BACKUP_IPV6, b, buffer);
            } else if (type instanceof UnnumberedEroBackupCase) {
                final UnnumberedEroBackupCase ero = (UnnumberedEroBackupCase)type;
                final ByteBuf b = Unpooled.buffer();
                b.writeByte(serializeLoose(ero.isLoose()));
                b.writeBytes(ero.getRouterId());
                b.writeInt(ero.getInterfaceId().intValue());
                TlvUtil.writeSrTLV(UNNUMBERED_BACKUP_ERO, b, buffer);
            }
        }
    }

    public static void serializeSidLabelBinding(final SrSidLabel binding, final ByteBuf buffer) {
        final SidLabelFlags flags = binding.getSidLabelFlags();
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(AFI, flags.isAddressFamily());
        bs.set(MIRROR, flags.isMirrorContext());
        final ByteBuf tempBuf = Unpooled.buffer();
        bs.toByteBuf(tempBuf);
        tempBuf.writeByte(binding.getWeight().getValue());
        tempBuf.writeShort(binding.getValueRange());
        final IpPrefix prefix = binding.getFecPrefix();
        if (prefix.getIpv4Prefix() != null) {
            tempBuf.writeBytes(Ipv4Util.bytesForPrefixBegin(prefix.getIpv4Prefix()));
        } else {
            tempBuf.writeBytes(Ipv6Util.bytesForPrefixBegin(prefix.getIpv6Prefix()));
        }
        if (binding.getSubTlvs() != null) {
            serializeSidSubtlvs(binding.getSubTlvs(), tempBuf);
        }
        buffer.writeByte(TYPE);
        buffer.writeByte(tempBuf.readableBytes());
        buffer.writeBytes(ByteArray.getAllBytes(tempBuf));
    }

    public static SrCapabilities parseSrCapabilities(final ByteBuf buffer) {
        final SrCapabilitiesBuilder builder = new SrCapabilitiesBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        builder.setFlags(new Flags(flags.get(IPV4), flags.get(IPV6)));
        builder.setValueRange((long)buffer.readUnsignedMedium());
        buffer.skipBytes(2);
        builder.setSid(new SidLabel(ByteArray.readAllBytes(buffer)));
        return builder.build();
    }

    public static void serializeSrCapabilities(final SrCapabilities caps, final ByteBuf buffer) {
        final Flags flags = caps.getFlags();
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(IPV4, flags.isIpv4());
        bs.set(IPV6, flags.isIpv6());
        bs.toByteBuf(buffer);
        buffer.writeMedium(caps.getValueRange().intValue());
        buffer.writeByte(SID_TLV_TYPE);
        final byte[] sid = caps.getSid().getValue();
        buffer.writeByte(sid.length);
        buffer.writeBytes(sid);
    }

    public static SrAlgorithm parseSrAlgorithms(final ByteBuf buffer) {
        final SrAlgorithmBuilder builder = new SrAlgorithmBuilder();
        final List<Algorithm> algs = new ArrayList<>();
        while (buffer.isReadable()) {
            algs.add(Algorithm.forValue(buffer.readUnsignedByte()));
        }
        builder.setAlgorithm(algs);
        return builder.build();
    }

    public static void serializeSrAlgorithms(final SrAlgorithm alg, final ByteBuf buffer) {
        if (alg.getAlgorithm() != null) {
            for (final Algorithm a : alg.getAlgorithm()) {
                buffer.writeByte(a.getIntValue());
            }
        }
    }

    // TODO move to ByteBufWriteUtil
    private static final int ONE_BYTE_MAX = 255;
    private static void writeMinimumBytes(final Long value, final ByteBuf output) {
        switch (value.intValue() / (ONE_BYTE_MAX+1)) {
        case 0:
            output.writeByte(value.intValue());
            break;
        case 1:
            ByteBufWriteUtil.writeShort(value.shortValue(), output);
            break;
        case 2:
            ByteBufWriteUtil.writeMedium(value.intValue(), output);
            break;
        case 3:
            ByteBufWriteUtil.writeInt(value.intValue(), output);
            break;
        default:
            ByteBufWriteUtil.writeUnsignedInt(value, output);
            break;
        }
    }

    // TODO move to ByteBufWriteUtil
    private static long readNumber(final int length, final ByteBuf value) {
        switch (length) {
        case 1:
            return value.readByte();
        case 2:
            return value.readShort();
        case 3:
            return value.readMedium();
        case 4:
            return value.readInt();
        default:
            return value.readUnsignedInt();
        }
    }
}
