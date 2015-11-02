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
import org.opendaylight.protocol.bgp.linkstate.attribute.PrefixAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SidLabelIndexParser.Size;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrBindingSidLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrBindingSidLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.BindingSubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.BindingSubTlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.flags.IsisBindingFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.flags.IsisBindingFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.flags.OspfBindingFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sid.tlv.flags.OspfBindingFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.EroMetricCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.EroMetricCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv4EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv4EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv4EroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv4EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroBackupCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroBackupCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.Ipv6EroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.PrefixSidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.PrefixSidCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.SidLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.SidLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdBackupEroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdBackupEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.SidLabelIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.TeMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BindingSidLabelParser {

    private static final Logger LOG = LoggerFactory.getLogger(BindingSidLabelParser.class);

    private BindingSidLabelParser() {
        throw new UnsupportedOperationException();
    }

    /* Flags */
    private static final int FLAGS_SIZE = 8;
    private static final int AFI = 0;
    private static final int MIRROR_CONTEXT = 1;
    private static final int MIRROR_CONTEXT_OSPF = 0;
    private static final int SPREAD_TLV = 2;
    private static final int LEAKED = 3;
    private static final int ATTACHED = 4;
    private static final int LOOSE = 0;

    /* SID Label Tlv types */
    private static final int ERO_METRIC = 1162;
    private static final int ERO_IPV4 = 1163;
    private static final int ERO_IPV6 = 1164;
    private static final int UNNUMBERED_ERO = 1165;
    private static final int BACKUP_ERO_IPV4 = 1166;
    private static final int BACKUP_ERO_IPV6 = 1167;
    private static final int BACKUP_UNNUMBERED_ERO = 1168;

    private static final int RESERVED_BINDING_SID = 2;
    private static final int RESERVED_ERO = 3;

    public static SrBindingSidLabel parseBindingSidLabel(final ByteBuf buffer, final ProtocolId protocolId) {
        final SrBindingSidLabelBuilder bindingSid = new SrBindingSidLabelBuilder();
        bindingSid.setWeight(new Weight(buffer.readUnsignedByte()));
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        bindingSid.setFlags(parseBindingSidFlags(flags, protocolId));
        buffer.skipBytes(RESERVED_BINDING_SID);
        bindingSid.setBindingSubTlvs(parseBindingSubTlvs(buffer, protocolId));
        return bindingSid.build();
    }

    private static Flags parseBindingSidFlags(final BitArray flags, final ProtocolId protocol) {
        if (protocol.equals(ProtocolId.IsisLevel1) || protocol.equals(ProtocolId.IsisLevel2)) {
            return new IsisBindingFlagsCaseBuilder()
                .setAddressFamily(flags.get(AFI))
                .setMirrorContext(flags.get(MIRROR_CONTEXT))
                .setSpreadTlv(flags.get(SPREAD_TLV))
                .setLeakedFromLevel2(flags.get(LEAKED))
                .setAttachedFlag(flags.get(ATTACHED)).build();
        } else if (protocol.equals(ProtocolId.Ospf)) {
            return new OspfBindingFlagsCaseBuilder()
            .setMirroring(flags.get(MIRROR_CONTEXT_OSPF)).build();
        }
        return null;
    }

    private static List<BindingSubTlvs> parseBindingSubTlvs(final ByteBuf buffer, final ProtocolId protocolId) {
        final List<BindingSubTlvs> subTlvs = new ArrayList<BindingSubTlvs>();
        while (buffer.isReadable()) {
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            final ByteBuf slice = buffer.readSlice(length);
            final BindingSubTlvsBuilder builder = new BindingSubTlvsBuilder();
            parseSubTlv(type, slice, builder, protocolId);
            subTlvs.add(builder.build());
        }
        return subTlvs;
    }

    private static void parseSubTlv(final int type, final ByteBuf slice, final BindingSubTlvsBuilder builder, final ProtocolId protocolId) {
        switch (type) {
        case SidLabelIndexParser.SID_TYPE:
            final SidLabelIndex sid = SidLabelIndexParser.parseSidLabelIndex(Size.forValue(slice.readableBytes()), slice);
            builder.setBindingSubTlv(new SidLabelCaseBuilder()
                .setSidLabelIndex(sid).build());
            break;
        case PrefixAttributesParser.PREFIX_SID:
            final SrPrefix prefix = SrPrefixAttributesParser.parseSrPrefix(slice, protocolId);
            builder.setBindingSubTlv(new PrefixSidCaseBuilder()
                .setAlgorithm(prefix.getAlgorithm())
                .setFlags(prefix.getFlags())
                .setSidLabelIndex(prefix.getSidLabelIndex()).build());
            break;
        case ERO_METRIC:
            builder.setBindingSubTlv(new EroMetricCaseBuilder()
                .setEroMetric(new TeMetric(slice.readUnsignedInt())).build());
            break;
        case ERO_IPV4:
            final Ipv4EroCase ipv4Ero = parseIpv4EroCase(slice);
            builder.setBindingSubTlv(new Ipv4EroCaseBuilder()
                .setAddress(ipv4Ero.getAddress())
                .setLoose(ipv4Ero.isLoose()).build());
            break;
        case BACKUP_ERO_IPV4:
            final Ipv4EroCase ipv4Backup = parseIpv4EroCase(slice);
            builder.setBindingSubTlv(new Ipv4EroBackupCaseBuilder()
                .setAddress(ipv4Backup.getAddress())
                .setLoose(ipv4Backup.isLoose()).build());
            break;
        case ERO_IPV6:
            final Ipv6EroCase ipv6ero = parseIpv6EroCase(slice);
            builder.setBindingSubTlv(new Ipv6EroCaseBuilder()
                .setAddress(ipv6ero.getAddress())
                .setLoose(ipv6ero.isLoose()).build());
            break;
        case BACKUP_ERO_IPV6:
            final Ipv6EroCase ipv6backup = parseIpv6EroCase(slice);
            builder.setBindingSubTlv(new Ipv6EroBackupCaseBuilder()
                .setAddress(ipv6backup.getAddress())
                .setLoose(ipv6backup.isLoose()).build());
            break;
        case UNNUMBERED_ERO:
            final UnnumberedInterfaceIdEroCase unnumbered = parseUnnumberedEroCase(slice);
            builder.setBindingSubTlv(new UnnumberedInterfaceIdEroCaseBuilder()
                .setLoose(unnumbered.isLoose())
                .setRouterId(unnumbered.getRouterId())
                .setInterfaceId(unnumbered.getInterfaceId()).build());
            break;
        case BACKUP_UNNUMBERED_ERO:
            final UnnumberedInterfaceIdEroCase unnumberedBackup = parseUnnumberedEroCase(slice);
            builder.setBindingSubTlv(new UnnumberedInterfaceIdBackupEroCaseBuilder()
                .setLoose(unnumberedBackup.isLoose())
                .setRouterId(unnumberedBackup.getRouterId())
                .setInterfaceId(unnumberedBackup.getInterfaceId()).build());
            break;
        default:
            LOG.info("Unknown binding sub Tlv type {}", type);
            break;
        }
    }

    private static Ipv4EroCase parseIpv4EroCase(final ByteBuf buffer) {
        final Ipv4EroCaseBuilder builder = new Ipv4EroCaseBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        builder.setLoose(flags.get(LOOSE));
        buffer.skipBytes(RESERVED_ERO);
        builder.setAddress(Ipv4Util.addressForByteBuf(buffer));
        return builder.build();
    }

    private static Ipv6EroCase parseIpv6EroCase(final ByteBuf buffer) {
        final Ipv6EroCaseBuilder builder = new Ipv6EroCaseBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        builder.setLoose(flags.get(LOOSE));
        buffer.skipBytes(RESERVED_ERO);
        builder.setAddress(Ipv6Util.addressForByteBuf(buffer));
        return builder.build();
    }

    private static UnnumberedInterfaceIdEroCase parseUnnumberedEroCase(final ByteBuf buffer) {
        final UnnumberedInterfaceIdEroCaseBuilder builder = new UnnumberedInterfaceIdEroCaseBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        builder.setLoose(flags.get(LOOSE));
        buffer.skipBytes(RESERVED_ERO);
        builder.setRouterId(buffer.readUnsignedInt());
        builder.setInterfaceId(buffer.readUnsignedInt());
        return builder.build();
    }

    public static void serializeBindingSidLabel(final SrBindingSidLabel bindingSid, final ByteBuf aggregator) {
        serializeBindingSidAttributes(bindingSid.getWeight(), bindingSid.getFlags(), bindingSid.getBindingSubTlvs(), aggregator);
    }

    public static void serializeBindingSidAttributes(final Weight weight, final Flags flags, final List<BindingSubTlvs> bindingSubTlvs, final ByteBuf aggregator) {
        aggregator.writeByte(weight.getValue());
        final BitArray bitFlags = serializeBindingSidFlags(flags);
        bitFlags.toByteBuf(aggregator);
        aggregator.writeZero(RESERVED_BINDING_SID);
        serializeBindingSubTlvs(bindingSubTlvs, aggregator);
    }

    private static BitArray serializeBindingSidFlags(final Flags flags) {
        final BitArray bitFlags = new BitArray(FLAGS_SIZE);
        if (flags instanceof IsisBindingFlagsCase) {
            final IsisBindingFlagsCase isisFlags = (IsisBindingFlagsCase) flags;
            bitFlags.set(AFI, isisFlags.isAddressFamily());
            bitFlags.set(MIRROR_CONTEXT, isisFlags.isMirrorContext());
            bitFlags.set(SPREAD_TLV, isisFlags.isSpreadTlv());
            bitFlags.set(LEAKED, isisFlags.isLeakedFromLevel2());
            bitFlags.set(ATTACHED, isisFlags.isAttachedFlag());
        } else if (flags instanceof OspfBindingFlagsCase) {
            final OspfBindingFlagsCase ospfFlags = (OspfBindingFlagsCase) flags;
            bitFlags.set(MIRROR_CONTEXT_OSPF, ospfFlags.isMirroring());
        }
        return bitFlags;
    }

    private static void serializeBindingSubTlvs(final List<BindingSubTlvs> bindingSubTlvs, final ByteBuf aggregator) {
        for (final BindingSubTlvs subTlv : bindingSubTlvs) {
            ByteBuf buffer = Unpooled.buffer();
            final BindingSubTlv bindingSubTlv = subTlv.getBindingSubTlv();
            if (bindingSubTlv instanceof SidLabelCase) {
                buffer = SidLabelIndexParser.serializeSidValue(((SidLabelCase) bindingSubTlv).getSidLabelIndex());
                TlvUtil.writeTLV(SidLabelIndexParser.SID_TYPE, buffer, aggregator);
            } else if (bindingSubTlv instanceof PrefixSidCase) {
                final PrefixSidCase prefix = (PrefixSidCase) bindingSubTlv;
                SrPrefixAttributesParser.serializePrefixAttributes(prefix.getFlags(), prefix.getAlgorithm(), prefix.getSidLabelIndex(), buffer);
                TlvUtil.writeTLV(PrefixAttributesParser.PREFIX_SID, buffer, aggregator);
            } else if (bindingSubTlv instanceof EroMetricCase) {
                buffer.writeInt(((EroMetricCase) bindingSubTlv).getEroMetric().getValue().intValue());
                TlvUtil.writeTLV(ERO_METRIC, buffer, aggregator);
            } else if (bindingSubTlv instanceof Ipv4EroBackupCase) {
                final Ipv4EroBackupCase ipv4Backup = (Ipv4EroBackupCase) bindingSubTlv;
                buffer = serializeIpv4EroCase(ipv4Backup.isLoose(), ipv4Backup.getAddress());
                TlvUtil.writeTLV(BACKUP_ERO_IPV4, buffer, aggregator);
            } else if (bindingSubTlv instanceof Ipv4EroCase) {
                final Ipv4EroCase ipv4Ero = (Ipv4EroCase) bindingSubTlv;
                buffer = serializeIpv4EroCase(ipv4Ero.isLoose(), ipv4Ero.getAddress());
                TlvUtil.writeTLV(ERO_IPV4, buffer, aggregator);
            } else if (bindingSubTlv instanceof Ipv6EroBackupCase) {
                final Ipv6EroBackupCase ipv6Backup = (Ipv6EroBackupCase) bindingSubTlv;
                buffer = serializeIpv6EroCase(ipv6Backup.isLoose(), ipv6Backup.getAddress());
                TlvUtil.writeTLV(BACKUP_ERO_IPV6, buffer, aggregator);
            } else if (bindingSubTlv instanceof Ipv6EroCase) {
                final Ipv6EroCase ipv6Ero = (Ipv6EroCase) bindingSubTlv;
                buffer = serializeIpv6EroCase(ipv6Ero.isLoose(), ipv6Ero.getAddress());
                TlvUtil.writeTLV(ERO_IPV6, buffer, aggregator);
            } else if (bindingSubTlv instanceof UnnumberedInterfaceIdEroCase) {
                final UnnumberedInterfaceIdEroCase unnumberedEro = (UnnumberedInterfaceIdEroCase) bindingSubTlv;
                buffer = serializeUnnumberedIdEro(unnumberedEro.isLoose(), unnumberedEro.getRouterId(), unnumberedEro.getInterfaceId());
                TlvUtil.writeTLV(UNNUMBERED_ERO, buffer, aggregator);
            } else if (bindingSubTlv instanceof UnnumberedInterfaceIdBackupEroCase) {
                final UnnumberedInterfaceIdBackupEroCase unnumberedBackup = (UnnumberedInterfaceIdBackupEroCase) bindingSubTlv;
                buffer = serializeUnnumberedIdEro(unnumberedBackup.isLoose(), unnumberedBackup.getRouterId(), unnumberedBackup.getInterfaceId());
                TlvUtil.writeTLV(BACKUP_UNNUMBERED_ERO, buffer, aggregator);
            }
        }
    }

    private static ByteBuf serializeIpv4EroCase(final Boolean loose, final Ipv4Address address) {
        final ByteBuf buffer = Unpooled.buffer();
        serializeEroFlags(buffer, loose);
        buffer.writeBytes(Ipv4Util.byteBufForAddress(address));
        return buffer;
    }

    private static ByteBuf serializeIpv6EroCase(final Boolean loose, final Ipv6Address address) {
        final ByteBuf buffer = Unpooled.buffer();
        serializeEroFlags(buffer, loose);
        buffer.writeBytes(Ipv6Util.byteBufForAddress(address));
        return buffer;
    }

    private static ByteBuf serializeUnnumberedIdEro(final Boolean loose, final Long routerId, final Long interfaceId) {
        final ByteBuf buffer = Unpooled.buffer();
        serializeEroFlags(buffer, loose);
        buffer.writeInt(routerId.intValue());
        buffer.writeInt(interfaceId.intValue());
        return buffer;
    }

    private static void serializeEroFlags(final ByteBuf buffer, final Boolean loose) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(LOOSE, loose);
        flags.toByteBuf(buffer);
        buffer.writeZero(RESERVED_ERO);
    }
}
