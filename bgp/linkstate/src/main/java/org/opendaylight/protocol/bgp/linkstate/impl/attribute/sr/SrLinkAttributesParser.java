/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.LinkAttributesParser.SR_LAN_ADJ_ID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SidLabelIndexParser.Size;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.attribute.SrAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.attribute.SrAdjIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.attribute.SrLanAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.attribute.SrLanAdjIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.EpeAdjSidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Weight;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.adj.flags.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.adj.flags.flags.IsisAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.adj.flags.flags.IsisAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.adj.flags.flags.OspfAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.adj.flags.flags.OspfAdjFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.SidLabelIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class SrLinkAttributesParser {

    private static final int ISO_SYSTEM_ID_SIZE = 6;
    private static final int RESERVED = 2;

    /* Adj-SID flags */
    private static final int ADDRESS_FAMILY_FLAG = 0;
    private static final int BACKUP_ISIS = 1;
    private static final int BACKUP_OSPF = 0;
    private static final int VALUE_ISIS = 2;
    private static final int VALUE_OSPF = 1;
    private static final int VALUE_EPE = 0;
    private static final int LOCAL_ISIS = 3;
    private static final int LOCAL_OSPF = 2;
    private static final int LOCAL_EPE = 1;
    private static final int SET_ISIS = 4;
    private static final int SET_OSPF = 3;
    private static final int FLAGS_BITS_SIZE = 8;
    private static final int FLAGS_BYTE_SIZE = 1;

    /** OSPF flags
       0 1 2 3 4 5 6 7
      +-+-+-+-+-+-+-+-+
      |B|V|L|S|       |
      +-+-+-+-+-+-+-+-+

       ISIS flags
       0 1 2 3 4 5 6 7
      +-+-+-+-+-+-+-+-+
      |F|B|V|L|S|     |
      +-+-+-+-+-+-+-+-+
     */

    private SrLinkAttributesParser() {
        throw new UnsupportedOperationException();
    }

    public static SrAdjIds parseAdjacencySegmentIdentifier(final ByteBuf buffer, final ProtocolId protocolId) {
        final Flags adjFlags;
        final Weight weight;
        final SidLabelIndex sidValue;
        if (buffer.isReadable()) {
            final BitArray flags = BitArray.valueOf(buffer, FLAGS_BITS_SIZE);
            adjFlags = parseFlags(flags, protocolId);
            weight = new Weight(buffer.readUnsignedByte());
            buffer.skipBytes(RESERVED);
            sidValue = SidLabelIndexParser.parseSidLabelIndex(Size.forValue(buffer.readableBytes()), buffer);
        } else {
            adjFlags = null;
            weight = null;
            sidValue = null;
        }
        return new SrAdjIdsBuilder().setFlags(adjFlags).setSidLabelIndex(sidValue).setWeight(weight).build();
    }

    public static EpeAdjSidTlv parseEpeAdjacencySegmentIdentifier(final ByteBuf buffer) {
        final Weight weight;
        final SidLabelIndex sidValue;
        if (buffer.isReadable()) {
            buffer.skipBytes(FLAGS_BYTE_SIZE);
            weight = new Weight(buffer.readUnsignedByte());
            buffer.skipBytes(RESERVED);
            sidValue = SidLabelIndexParser.parseSidLabelIndex(Size.forValue(buffer.readableBytes()), buffer);
        } else {
            weight = null;
            sidValue = null;
        }
        return new EpeAdjSidTlv() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return EpeAdjSidTlv.class;
            }
            @Override
            public Weight getWeight() {
                return weight;
            }
            @Override
            public SidLabelIndex getSidLabelIndex() {
                return sidValue;
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
        srLanAdjIdBuilder.setWeight(new Weight(buffer.readUnsignedByte()));
        buffer.skipBytes(RESERVED);
        switch (protocolId) {
        case IsisLevel1:
        case IsisLevel2:
            srLanAdjIdBuilder.setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(buffer, ISO_SYSTEM_ID_SIZE)));
            break;
        case Ospf:
        case OspfV3:
            srLanAdjIdBuilder.setNeighborId(Ipv4Util.addressForByteBuf(buffer));
            break;
        default:
            return null;
        }
        // length determines a type of next field, which is used for parsing
        srLanAdjIdBuilder.setSidLabelIndex(SidLabelIndexParser.parseSidLabelIndex(Size.forValue(buffer.readableBytes()), buffer));
        return srLanAdjIdBuilder.build();
    }

    private static Flags parseFlags(final BitArray flags, final ProtocolId protocol) {
        if (protocol == null) {
            return null;
        }
        switch (protocol) {
        case IsisLevel1:
        case IsisLevel2:
            return new IsisAdjFlagsCaseBuilder().setAddressFamily(flags.get(ADDRESS_FAMILY_FLAG)).setBackup(flags.get(BACKUP_ISIS))
                .setSet(flags.get(SET_ISIS)).build();
        case Ospf:
        case OspfV3:
            return new OspfAdjFlagsCaseBuilder().setBackup(flags.get(BACKUP_OSPF)).setSet(flags.get(SET_OSPF)).build();
        default:
            return null;
        }
    }

    public static <T extends EpeAdjSidTlv> void serializeAdjacencySegmentIdentifiers(final List<T> adjSids, final int type, final ByteBuf byteAggregator) {
        adjSids.forEach(id -> TlvUtil.writeTLV(type, serializeAdjacencySegmentIdentifier(id), byteAggregator));
    }

    public static <T extends EpeAdjSidTlv> ByteBuf serializeAdjacencySegmentIdentifier(final T adjSid) {
        final ByteBuf value = Unpooled.buffer();
        if(adjSid instanceof SrAdjIds) {
            final BitArray flags = serializeAdjFlags(((SrAdjIds) adjSid).getFlags(), adjSid.getSidLabelIndex());
            flags.toByteBuf(value);
        }else {
            value.writeZero(FLAGS_BYTE_SIZE);
        }
        value.writeByte(adjSid.getWeight().getValue());
        value.writeZero(RESERVED);
        value.writeBytes(SidLabelIndexParser.serializeSidValue(adjSid.getSidLabelIndex()));
        return value;
    }

    public static void serializeLanAdjacencySegmentIdentifiers(final List<SrLanAdjIds> srLanAdjIds, final ByteBuf byteAggregator) {
        for (final SrLanAdjIds id : srLanAdjIds) {
            TlvUtil.writeTLV(SR_LAN_ADJ_ID, serializeLanAdjacencySegmentIdentifier(id), byteAggregator);
        }
    }

    public static ByteBuf serializeLanAdjacencySegmentIdentifier(final SrLanAdjIds srLanAdjId) {
        final ByteBuf value = Unpooled.buffer();
        final BitArray flags = serializeAdjFlags(srLanAdjId.getFlags(), srLanAdjId.getSidLabelIndex());
        flags.toByteBuf(value);
        value.writeByte(srLanAdjId.getWeight().getValue());
        value.writeZero(RESERVED);
        if (srLanAdjId.getIsoSystemId() != null) {
            value.writeBytes(srLanAdjId.getIsoSystemId().getValue());
        } else if (srLanAdjId.getNeighborId() != null) {
            value.writeBytes(Ipv4Util.byteBufForAddress(srLanAdjId.getNeighborId()));
        }
        value.writeBytes(SidLabelIndexParser.serializeSidValue(srLanAdjId.getSidLabelIndex()));
        return value;
    }

    private static BitArray serializeAdjFlags(final Flags flags, final SidLabelIndex sidLabelIndex) {
        final BitArray bitFlags = new BitArray(FLAGS_BITS_SIZE);
        if (flags instanceof OspfAdjFlagsCase) {
            final OspfAdjFlagsCase ospfFlags = (OspfAdjFlagsCase) flags;
            bitFlags.set(BACKUP_OSPF, ospfFlags.isBackup());
            bitFlags.set(SET_OSPF, ospfFlags.isSet());
            SidLabelIndexParser.setFlags(sidLabelIndex, bitFlags, VALUE_OSPF, LOCAL_OSPF);
        } else if (flags instanceof IsisAdjFlagsCase) {
            final IsisAdjFlagsCase isisFlags = (IsisAdjFlagsCase) flags;
            bitFlags.set(ADDRESS_FAMILY_FLAG, isisFlags.isAddressFamily());
            bitFlags.set(BACKUP_ISIS, isisFlags.isBackup());
            bitFlags.set(SET_ISIS, isisFlags.isSet());
            SidLabelIndexParser.setFlags(sidLabelIndex, bitFlags, VALUE_ISIS, LOCAL_ISIS);
        } else if (flags == null){
            SidLabelIndexParser.setFlags(sidLabelIndex, bitFlags, VALUE_EPE, LOCAL_EPE);
        }
        return bitFlags;
    }

}
