/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint8;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint8;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SidLabelIndexParser.Size;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.LinkMsd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.LinkMsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrAdjIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrLanAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrLanAdjIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.EpeSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.MsdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.EpeAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.EpeAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.IsisAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.IsisAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.OspfAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.OspfAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.epe.adj.flags._case.EpeAdjFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.epe.adj.flags._case.EpeAdjFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.isis.adj.flags._case.IsisAdjFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.isis.adj.flags._case.IsisAdjFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.ospf.adj.flags._case.OspfAdjFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.ospf.adj.flags._case.OspfAdjFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.lan.adj.sid.tlv.neighbor.type.IsisNeighborCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.lan.adj.sid.tlv.neighbor.type.IsisNeighborCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.lan.adj.sid.tlv.neighbor.type.OspfNeighborCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.lan.adj.sid.tlv.neighbor.type.OspfNeighborCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.link.msd.tlv.Msd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.link.msd.tlv.MsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.SidLabelIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class SrLinkAttributesParser {

    private static final int ISO_SYSTEM_ID_SIZE = 6;
    private static final int RESERVED = 2;

    /* Adj-SID and EPE flags */
    private static final int ADDRESS_FAMILY_FLAG = 0;
    private static final int BACKUP_ISIS = 1;
    private static final int BACKUP_OSPF = 0;
    private static final int BACKUP_EPE = 2;
    private static final int VALUE_ISIS = 2;
    private static final int VALUE_OSPF = 1;
    private static final int VALUE_EPE = 0;
    private static final int LOCAL_ISIS = 3;
    private static final int LOCAL_OSPF = 2;
    private static final int LOCAL_EPE = 1;
    private static final int SET_ISIS = 4;
    private static final int SET_OSPF = 3;
    private static final int PERSISTENT_ISIS = 5;
    private static final int PERSISTENT_OSPF = 4;
    private static final int PERSISTENT_EPE = 3;
    private static final int FLAGS_BITS_SIZE = 8;

    /*
       OSPF flags
       0 1 2 3 4 5 6 7
      +-+-+-+-+-+-+-+-+
      |B|V|L|S|P|     |
      +-+-+-+-+-+-+-+-+

       ISIS flags
       0 1 2 3 4 5 6 7
      +-+-+-+-+-+-+-+-+
      |F|B|V|L|S|P|   |
      +-+-+-+-+-+-+-+-+

       EPE flags
       0 1 2 3 4 5 6 7
      +-+-+-+-+-+-+-+-+
      |V|L|B|P|       |
      +-+-+-+-+-+-+-+-+

     */

    private SrLinkAttributesParser() {

    }

    public static SrAdjIds parseAdjacencySegmentIdentifier(final ByteBuf buffer, final ProtocolId protocolId) {
        final Flags adjFlags;
        final Weight weight;
        final SidLabelIndex sidValue;
        if (buffer.isReadable()) {
            final BitArray flags = BitArray.valueOf(buffer, FLAGS_BITS_SIZE);
            adjFlags = parseFlags(flags, protocolId);
            weight = new Weight(readUint8(buffer));
            buffer.skipBytes(RESERVED);
            final boolean isValue;
            final boolean isLocal;
            switch (protocolId) {
                case IsisLevel1:
                case IsisLevel2:
                    isValue = flags.get(VALUE_ISIS);
                    isLocal = flags.get(LOCAL_ISIS);
                    break;
                case Ospf:
                case OspfV3:
                    isValue = flags.get(VALUE_OSPF);
                    isLocal = flags.get(LOCAL_OSPF);
                    break;
                default:
                    return null;
            }
            sidValue = SidLabelIndexParser.parseSidLabelIndexByFlags(Size.forValue(buffer.readableBytes()), buffer,
                    isValue, isLocal);
        } else {
            adjFlags = null;
            weight = null;
            sidValue = null;
        }
        return new SrAdjIdsBuilder().setFlags(adjFlags).setSidLabelIndex(sidValue).setWeight(weight).build();
    }

    public static EpeSidTlv parseEpeSegmentIdentifier(final ByteBuf buffer) {
        final Weight weight;
        final SidLabelIndex sidValue;
        final EpeAdjFlagsCase epeFlags;
        if (buffer.isReadable()) {
            final BitArray flags = BitArray.valueOf(buffer, FLAGS_BITS_SIZE);
            weight = new Weight(readUint8(buffer));
            buffer.skipBytes(RESERVED);
            sidValue = SidLabelIndexParser.parseSidLabelIndexByFlags(Size.forValue(buffer.readableBytes()), buffer,
                    flags.get(VALUE_EPE), flags.get(LOCAL_EPE));
            epeFlags = new EpeAdjFlagsCaseBuilder()
                    .setEpeAdjFlags(new EpeAdjFlagsBuilder()
                            .setLocal(flags.get(LOCAL_EPE))
                            .setValue(flags.get(VALUE_EPE))
                            .setBackup(flags.get(BACKUP_EPE))
                            .setPersistent(flags.get(PERSISTENT_EPE))
                            .build())
                    .build();
        } else {
            epeFlags = null;
            weight = null;
            sidValue = null;
        }
        return new EpeSidTlv() {
            @Override
            public Flags getFlags() {
                return epeFlags;
            }

            @Override
            public Weight getWeight() {
                return weight;
            }

            @Override
            public SidLabelIndex getSidLabelIndex() {
                return sidValue;
            }

            @Override
            public Class<EpeSidTlv> implementedInterface() {
                return EpeSidTlv.class;
            }
        };
    }

    public static SrLanAdjIds parseLanAdjacencySegmentIdentifier(final ByteBuf buffer, final ProtocolId protocolId) {
        if (!buffer.isReadable()) {
            return new SrLanAdjIdsBuilder().build();
        }
        final SrLanAdjIdsBuilder srLanAdjIdBuilder = new SrLanAdjIdsBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_BITS_SIZE);
        srLanAdjIdBuilder.setFlags(parseFlags(flags, protocolId));
        srLanAdjIdBuilder.setWeight(new Weight(readUint8(buffer)));
        buffer.skipBytes(RESERVED);
        final boolean isValue;
        final boolean isLocal;
        switch (protocolId) {
            case IsisLevel1:
            case IsisLevel2:
                isValue = flags.get(VALUE_ISIS);
                isLocal = flags.get(LOCAL_ISIS);
                srLanAdjIdBuilder.setNeighborType(
                    new IsisNeighborCaseBuilder()
                        .setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(buffer, ISO_SYSTEM_ID_SIZE)))
                        .build());
                break;
            case Ospf:
            case OspfV3:
                isValue = flags.get(VALUE_OSPF);
                isLocal = flags.get(LOCAL_OSPF);
                srLanAdjIdBuilder.setNeighborType(
                    new OspfNeighborCaseBuilder().setNeighborId(Ipv4Util.addressForByteBuf(buffer)).build());
                break;
            default:
                return null;
        }
        // length determines a type of next field, which is used for parsing
        srLanAdjIdBuilder.setSidLabelIndex(SidLabelIndexParser
                .parseSidLabelIndexByFlags(Size.forValue(buffer.readableBytes()), buffer, isValue, isLocal));
        return srLanAdjIdBuilder.build();
    }

    public static LinkMsd parseSrLinkMsd(final ByteBuf buffer) {
        final List<Msd> msds = new ArrayList<Msd>();
        while (buffer.isReadable()) {
            msds.add(new MsdBuilder()
                    .setType(MsdType.forValue(buffer.readByte()))
                    .setValue(readUint8(buffer))
                    .build());
        }
        return new LinkMsdBuilder().setMsd(msds).build();
    }

    private static Flags parseFlags(final BitArray flags, final ProtocolId protocol) {
        if (protocol == null) {
            return null;
        }
        return switch (protocol) {
            case IsisLevel1, IsisLevel2 -> new IsisAdjFlagsCaseBuilder()
                .setIsisAdjFlags(new IsisAdjFlagsBuilder()
                    .setAddressFamily(flags.get(ADDRESS_FAMILY_FLAG))
                    .setBackup(flags.get(BACKUP_ISIS))
                    .setValue(flags.get(VALUE_ISIS))
                    .setLocal(flags.get(LOCAL_ISIS))
                    .setSet(flags.get(SET_ISIS))
                    .setPersistent(flags.get(PERSISTENT_ISIS))
                    .build())
                .build();
            case Ospf, OspfV3 -> new OspfAdjFlagsCaseBuilder()
                .setOspfAdjFlags(new OspfAdjFlagsBuilder()
                    .setBackup(flags.get(BACKUP_OSPF))
                    .setValue(flags.get(VALUE_OSPF))
                    .setLocal(flags.get(LOCAL_OSPF))
                    .setSet(flags.get(SET_OSPF))
                    .setPersistent(flags.get(PERSISTENT_OSPF))
                    .build())
                .build();
            default -> null;
        };
    }

    public static void serializeAdjacencySegmentIdentifiers(final List<SrAdjIds> srAdjIds,
            final int tlvType, final ByteBuf byteAggregator) {
        for (final SrAdjIds id : srAdjIds) {
            TlvUtil.writeTLV(tlvType, serializeAdjacencySegmentIdentifier(id), byteAggregator);
        }
    }

    public static ByteBuf serializeAdjacencySegmentIdentifier(final SrAdjIds srAdjId) {
        final ByteBuf output = Unpooled.buffer();
        final BitArray flags = serializeAdjFlags(srAdjId.getFlags(), srAdjId.getSidLabelIndex());
        flags.toByteBuf(output);
        writeUint8(output, srAdjId.getWeight().getValue());
        output.writeZero(RESERVED);
        output.writeBytes(SidLabelIndexParser.serializeSidValue(srAdjId.getSidLabelIndex()));
        return output;
    }

    public static void serializeLanAdjacencySegmentIdentifiers(final List<SrLanAdjIds> srLanAdjIds,
            final int tlvType, final ByteBuf byteAggregator) {
        for (final SrLanAdjIds id : srLanAdjIds) {
            TlvUtil.writeTLV(tlvType, serializeLanAdjacencySegmentIdentifier(id), byteAggregator);
        }
    }

    public static ByteBuf serializeLanAdjacencySegmentIdentifier(final SrLanAdjIds srLanAdjId) {
        final ByteBuf output = Unpooled.buffer();
        final BitArray flags = serializeAdjFlags(srLanAdjId.getFlags(), srLanAdjId.getSidLabelIndex());
        flags.toByteBuf(output);
        writeUint8(output, srLanAdjId.getWeight().getValue());
        output.writeZero(RESERVED);
        if (srLanAdjId.getNeighborType() instanceof IsisNeighborCase) {
            output.writeBytes(((IsisNeighborCase) srLanAdjId.getNeighborType()).getIsoSystemId().getValue());
        } else if (srLanAdjId.getNeighborType() instanceof OspfNeighborCase) {
            output.writeBytes(
                Ipv4Util.bytesForAddress(((OspfNeighborCase) srLanAdjId.getNeighborType()).getNeighborId()));
        }
        output.writeBytes(SidLabelIndexParser.serializeSidValue(srLanAdjId.getSidLabelIndex()));
        return output;
    }

    public static void serializeEpeSegmentIdentifiers(final List<EpeSidTlv> value,
            final int type, final ByteBuf byteAggregator) {
        value.forEach(id -> TlvUtil.writeTLV(type, serializeEpeSegmentIdentifier(id), byteAggregator));
    }

    public static ByteBuf serializeEpeSegmentIdentifier(final EpeSidTlv epeSid) {
        final ByteBuf output = Unpooled.buffer();
        final BitArray flags = serializeAdjFlags(epeSid.getFlags(), epeSid.getSidLabelIndex());
        flags.toByteBuf(output);
        writeUint8(output, epeSid.getWeight().getValue());
        output.writeZero(RESERVED);
        output.writeBytes(SidLabelIndexParser.serializeSidValue(epeSid.getSidLabelIndex()));
        return output;
    }

    private static BitArray serializeAdjFlags(final Flags flags, final SidLabelIndex sidLabelIndex) {
        final BitArray bitFlags = new BitArray(FLAGS_BITS_SIZE);
        if (flags instanceof OspfAdjFlagsCase) {
            final OspfAdjFlags ospfFlags = ((OspfAdjFlagsCase) flags).getOspfAdjFlags();
            bitFlags.set(BACKUP_OSPF, ospfFlags.getBackup());
            bitFlags.set(VALUE_OSPF, ospfFlags.getValue());
            bitFlags.set(LOCAL_OSPF, ospfFlags.getLocal());
            bitFlags.set(SET_OSPF, ospfFlags.getSet());
            bitFlags.set(PERSISTENT_OSPF, ospfFlags.getPersistent());
            SidLabelIndexParser.setFlags(sidLabelIndex, bitFlags, VALUE_OSPF, LOCAL_OSPF);
        } else if (flags instanceof IsisAdjFlagsCase) {
            final IsisAdjFlags isisFlags = ((IsisAdjFlagsCase) flags).getIsisAdjFlags();
            bitFlags.set(ADDRESS_FAMILY_FLAG, isisFlags.getAddressFamily());
            bitFlags.set(BACKUP_ISIS, isisFlags.getBackup());
            bitFlags.set(VALUE_ISIS, isisFlags.getValue());
            bitFlags.set(LOCAL_ISIS, isisFlags.getLocal());
            bitFlags.set(SET_ISIS, isisFlags.getSet());
            bitFlags.set(PERSISTENT_ISIS, isisFlags.getPersistent());
            SidLabelIndexParser.setFlags(sidLabelIndex, bitFlags, VALUE_ISIS, LOCAL_ISIS);
        } else if (flags instanceof EpeAdjFlags) {
            final EpeAdjFlags epeFlags = ((EpeAdjFlags) flags);
            bitFlags.set(BACKUP_EPE, epeFlags.getBackup());
            bitFlags.set(VALUE_EPE, epeFlags.getValue());
            bitFlags.set(LOCAL_EPE, epeFlags.getLocal());
            bitFlags.set(PERSISTENT_EPE, epeFlags.getPersistent());
            SidLabelIndexParser.setFlags(sidLabelIndex, bitFlags, VALUE_EPE, LOCAL_EPE);
        }
        return bitFlags;
    }

    public static ByteBuf serializeSrLinkMsd(final LinkMsd linkMsd) {
        final ByteBuf output = Unpooled.buffer();
        for (Msd msd: linkMsd.getMsd()) {
            output.writeByte(msd.getType().getIntValue());
            ByteBufUtils.writeUint8(output, msd.getValue());
        }
        return output;
    }
}
