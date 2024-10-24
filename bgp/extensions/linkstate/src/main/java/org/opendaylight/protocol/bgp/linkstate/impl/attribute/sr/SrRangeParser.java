/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.PrefixAttributesParser.PREFIX_SID;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint16;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.SrRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.SrRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.PrefixSid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.PrefixSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.range.flags.IsisRangeFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.range.flags.IsisRangeFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.range.flags.OspfRangeFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.range.flags.OspfRangeFlagsCaseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SrRangeParser {
    private static final Logger LOG = LoggerFactory.getLogger(SrRangeParser.class);
    /* Flags */
    private static final int FLAGS_SIZE = 8;
    private static final int INNER_AREA = 0;
    private static final int ADDRESS_FAMILY = 0;
    private static final int MIRROR_CONTEXT = 1;
    private static final int SPREAD = 2;
    private static final int LEAKED = 3;
    private static final int ATTACHED = 4;
    private static final int RESERVED = 1;

    private SrRangeParser() {

    }

    public static SrRange parseSrRange(final ByteBuf buffer, final ProtocolId protocolId) {
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        final SrRangeBuilder range = new SrRangeBuilder();
        switch (protocolId) {
            case Ospf:
            case OspfV3:
                range.setRangeFlags(new OspfRangeFlagsCaseBuilder().setInterArea(flags.get(INNER_AREA)).build());
                break;
            case IsisLevel1:
            case IsisLevel2:
                range.setRangeFlags(new IsisRangeFlagsCaseBuilder()
                        .setAddressFamily(flags.get(ADDRESS_FAMILY))
                        .setMirrorContext(flags.get(MIRROR_CONTEXT))
                        .setSpreadTlv(flags.get(SPREAD))
                        .setLeakedFromLevel2(flags.get(LEAKED))
                        .setAttachedFlag(flags.get(ATTACHED))
                        .build());
                break;
            default:
                range.setRangeFlags(null);
        }
        buffer.skipBytes(RESERVED);
        range.setRangeSize(readUint16(buffer));
        final int type = buffer.readUnsignedShort();
        final int length = buffer.readUnsignedShort();
        if (type != PREFIX_SID || length > 8 || length < 7) {
            LOG.warn("Wrong Range SubTLV: type = {}, lenght = {}", type, length);
            return null;
        }
        range.setPrefixSid(new PrefixSidBuilder(SrPrefixAttributesParser.parseSrPrefix(buffer, protocolId)).build());
        LOG.debug("Parse Range TLV: {}", range.build());
        return range.build();
    }

    public static void serializeSrRange(final SrRange srRange, final ByteBuf aggregator) {
        final BitArray flags = new BitArray(FLAGS_SIZE);
        if (srRange.getRangeFlags() instanceof OspfRangeFlagsCase) {
            flags.set(INNER_AREA, ((OspfRangeFlagsCase) srRange.getRangeFlags()).getInterArea());
        } else if (srRange.getRangeFlags() instanceof IsisRangeFlagsCase) {
            IsisRangeFlagsCase isisRangeFlags = ((IsisRangeFlagsCase) srRange.getRangeFlags());
            flags.set(ADDRESS_FAMILY, isisRangeFlags.getAddressFamily());
            flags.set(MIRROR_CONTEXT, isisRangeFlags.getMirrorContext());
            flags.set(SPREAD, isisRangeFlags.getSpreadTlv());
            flags.set(LEAKED, isisRangeFlags.getLeakedFromLevel2());
            flags.set(ATTACHED, isisRangeFlags.getAttachedFlag());
        }
        flags.toByteBuf(aggregator);
        aggregator.writeZero(RESERVED);
        writeUint16(aggregator, srRange.getRangeSize());
        final PrefixSid prefixSid = srRange.getPrefixSid();
        final ByteBuf buffer = Unpooled.buffer();
        SrPrefixAttributesParser.serializePrefixAttributes(
            prefixSid.getFlags(),
            prefixSid.getAlgorithm(),
            prefixSid.getSidLabelIndex(),
            buffer);
        TlvUtil.writeTLV(PREFIX_SID, buffer, aggregator);
    }
}
