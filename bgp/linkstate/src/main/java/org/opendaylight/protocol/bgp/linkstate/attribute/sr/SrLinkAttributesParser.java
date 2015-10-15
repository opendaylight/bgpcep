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
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
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

    /* Adj-SID flags */
    private static final int ADDRESS_FAMILY_FLAG = 0;
    private static final int BACKUP_FLAG = 1;
    private static final int SET_FLAG = 4;
    private static final int FLAGS_SIZE = 8;

    private SrLinkAttributesParser() {
        throw new UnsupportedOperationException();
    }

    public static AdjSidTlv parseAdjacencySegmentIdentifier(final ByteBuf buffer, final ProtocolId protocol) {
        final Flags adjFlags;
        final Weight weight;
        final SidLabelIndex sidValue;
        if (buffer.isReadable()) {
            final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
            adjFlags = parseFlags(flags, protocol);
            weight = new Weight(buffer.readUnsignedByte());
            sidValue = SidLabelIndexParser.parseSidSubTlv(buffer);
        } else {
            adjFlags = null;
            weight = null;
            sidValue = null;
        }
        return new AdjSidTlv() {
            @Override
            public Flags getFlags() {
                return adjFlags;
            }
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
        };
    }

    public static SrLanAdjId parseLanAdjacencySegmentIdentifier(final ByteBuf buffer, final ProtocolId protocol) {
        if (!buffer.isReadable()) {
            return new SrLanAdjIdBuilder().build();
        }
        final SrLanAdjIdBuilder srLanAdjIdBuilder = new SrLanAdjIdBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        srLanAdjIdBuilder.setFlags(parseFlags(flags, protocol));
        srLanAdjIdBuilder.setWeight(new Weight(buffer.readUnsignedByte()));
        srLanAdjIdBuilder.setIsoSystemId(new IsoSystemIdentifier(ByteArray.readBytes(buffer, ISO_SYSTEM_ID_SIZE)));
        srLanAdjIdBuilder.setSidLabelIndex(SidLabelIndexParser.parseSidSubTlv(buffer));
        return srLanAdjIdBuilder.build();
    }

    private static Flags parseFlags(final BitArray flags, final ProtocolId protocol) {
        if (protocol.equals(ProtocolId.IsisLevel1) || protocol.equals(ProtocolId.IsisLevel2)) {
            return new IsisAdjFlagsCaseBuilder()
                .setAddressFamily(flags.get(ADDRESS_FAMILY_FLAG))
                .setBackup(flags.get(BACKUP_FLAG))
                .setSet(flags.get(SET_FLAG)).build();
        }
        if (protocol.equals(ProtocolId.Ospf)) {
            return new OspfAdjFlagsCaseBuilder()
                .setBackup(flags.get(BACKUP_FLAG))
                .setSet(flags.get(SET_FLAG)).build();
        }
        return null;
        // TODO implement local and value elsewhere
        // srLanAdjIdBuilder.setFlags(new AdjacencyFlags(bitFlags.get(ADDRESS_FAMILY_FLAG), bitFlags.get(BACKUP_FLAG), bitFlags.get(LOCAL_FLAG), bitFlags.get(SET_FLAG), bitFlags.get(VALUE_FLAG)));
    }

    public static ByteBuf serializeAdjacencySegmentIdentifier(final AdjSidTlv adjSid) {
        final ByteBuf value = Unpooled.buffer();
        final Flags srAdjIdFlags = adjSid.getFlags();
        final BitArray flags = serializeAdjFlags(srAdjIdFlags);
        flags.toByteBuf(value);
        value.writeByte(adjSid.getWeight().getValue());
        TlvUtil.writeTLV(SidLabelIndexParser.SID_TYPE, SidLabelIndexParser.serializeSidValue(adjSid.getSidLabelIndex()), value);
        return value;
    }

    public static ByteBuf serializeLanAdjacencySegmentIdentifier(final SrLanAdjId srLanAdjId) {
        final ByteBuf value = Unpooled.buffer();
        final Flags lanAdjFlags = srLanAdjId.getFlags();
        final BitArray flags = serializeAdjFlags(lanAdjFlags);
        flags.toByteBuf(value);
        value.writeByte(srLanAdjId.getWeight().getValue());
        value.writeBytes(srLanAdjId.getIsoSystemId().getValue());
        TlvUtil.writeTLV(SidLabelIndexParser.SID_TYPE, SidLabelIndexParser.serializeSidValue(srLanAdjId.getSidLabelIndex()), value);
        return value;
    }

    private static BitArray serializeAdjFlags(final Flags flags) {
        // TODO set value and local
        final BitArray bitFlags = new BitArray(FLAGS_SIZE);
        if (flags instanceof OspfAdjFlagsCase) {
            final OspfAdjFlagsCase ospfFlags = (OspfAdjFlagsCase) flags;
            bitFlags.set(BACKUP_FLAG, ospfFlags.isBackup());
            bitFlags.set(SET_FLAG, ospfFlags.isSet());
        } else if (flags instanceof IsisAdjFlagsCase) {
            final IsisAdjFlagsCase isisFlags = (IsisAdjFlagsCase) flags;
            bitFlags.set(ADDRESS_FAMILY_FLAG, isisFlags.isAddressFamily());
            bitFlags.set(BACKUP_FLAG, isisFlags.isBackup());
            bitFlags.set(SET_FLAG, isisFlags.isSet());
        }
        return bitFlags;
    }

}
