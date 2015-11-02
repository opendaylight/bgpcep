/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SidLabelIndexParser.Size;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.link.state.SrLanAdjIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.AdjSidTlv;
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
    private static final int LOCAL_ISIS = 3;
    private static final int LOCAL_OSPF = 2;
    private static final int SET_ISIS = 4;
    private static final int SET_OSPF = 3;
    private static final int FLAGS_SIZE = 8;

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

    public static AdjSidTlv parseAdjacencySegmentIdentifier(final ByteBuf buffer, final ProtocolId protocolId) {
        final Flags adjFlags;
        final Weight weight;
        final SidLabelIndex sidValue;
        if (buffer.isReadable()) {
            final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
            adjFlags = parseFlags(flags, protocolId);
            weight = new Weight(buffer.readUnsignedByte());
            buffer.skipBytes(RESERVED);
            sidValue = SidLabelIndexParser.parseSidLabelIndex(Size.forValue(buffer.readableBytes()), buffer);
        } else {
            adjFlags = null;
            weight = null;
            sidValue = null;
        }
        return new AdjSidTlv() {
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return AdjSidTlv.class;
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
            public Flags getFlags() {
                return adjFlags;
            }
        };
    }

    public static SrLanAdjId parseLanAdjacencySegmentIdentifier(final ByteBuf buffer, final ProtocolId protocolId) {
        if (!buffer.isReadable()) {
            return new SrLanAdjIdBuilder().build();
        }
        final SrLanAdjIdBuilder srLanAdjIdBuilder = new SrLanAdjIdBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        srLanAdjIdBuilder.setFlags(parseFlags(flags, protocolId));
        srLanAdjIdBuilder.setWeight(new Weight(buffer.readUnsignedByte()));
        buffer.skipBytes(RESERVED);
        srLanAdjIdBuilder.setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(buffer, ISO_SYSTEM_ID_SIZE)));
        // length determines a type of next field, which is used for parsing
        srLanAdjIdBuilder.setSidLabelIndex(SidLabelIndexParser.parseSidLabelIndex(Size.forValue(buffer.readableBytes()), buffer));
        return srLanAdjIdBuilder.build();
    }

    private static Flags parseFlags(final BitArray flags, final ProtocolId protocol) {
        if (protocol.equals(ProtocolId.IsisLevel1) || protocol.equals(ProtocolId.IsisLevel2)) {
            return new IsisAdjFlagsCaseBuilder()
                .setAddressFamily(flags.get(ADDRESS_FAMILY_FLAG))
                .setBackup(flags.get(BACKUP_ISIS))
                .setSet(flags.get(SET_ISIS)).build();
        }
        if (protocol.equals(ProtocolId.Ospf)) {
            return new OspfAdjFlagsCaseBuilder()
                .setBackup(flags.get(BACKUP_OSPF))
                .setSet(flags.get(SET_OSPF)).build();
        }
        return null;
    }

    public static ByteBuf serializeAdjacencySegmentIdentifier(final AdjSidTlv adjSid) {
        final ByteBuf value = Unpooled.buffer();
        final BitArray flags = serializeAdjFlags(adjSid.getFlags(), adjSid.getSidLabelIndex());
        flags.toByteBuf(value);
        value.writeByte(adjSid.getWeight().getValue());
        value.writeZero(RESERVED);
        value.writeBytes(SidLabelIndexParser.serializeSidValue(adjSid.getSidLabelIndex()));
        return value;
    }

    public static ByteBuf serializeLanAdjacencySegmentIdentifier(final SrLanAdjId srLanAdjId) {
        final ByteBuf value = Unpooled.buffer();
        final BitArray flags = serializeAdjFlags(srLanAdjId.getFlags(), srLanAdjId.getSidLabelIndex());
        flags.toByteBuf(value);
        value.writeByte(srLanAdjId.getWeight().getValue());
        value.writeZero(RESERVED);
        value.writeBytes(srLanAdjId.getIsoSystemId().getValue());
        value.writeBytes(SidLabelIndexParser.serializeSidValue(srLanAdjId.getSidLabelIndex()));
        return value;
    }

    private static BitArray serializeAdjFlags(final Flags flags, final SidLabelIndex sidLabelIndex) {
        final BitArray bitFlags = new BitArray(FLAGS_SIZE);
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
        }
        return bitFlags;
    }

}
