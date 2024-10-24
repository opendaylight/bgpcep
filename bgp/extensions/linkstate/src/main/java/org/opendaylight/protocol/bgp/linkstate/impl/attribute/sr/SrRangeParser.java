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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.PrefixSidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.range.flags.IsisRangeFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.range.flags.IsisRangeFlagsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.range.flags.OspfRangeFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.range.tlv.range.flags.OspfRangeFlagsCaseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SrRangeParser {
    private static final Logger LOG = LoggerFactory.getLogger(SrRangeParser.class);

    // Flags
    private static final int FLAGS_SIZE = 8;
    private static final int INNER_AREA = 0;
    private static final int ADDRESS_FAMILY = 0;
    private static final int MIRROR_CONTEXT = 1;
    private static final int SPREAD = 2;
    private static final int LEAKED = 3;
    private static final int ATTACHED = 4;
    private static final int RESERVED = 1;

    private SrRangeParser() {
        // Hidden on purpose
    }

    public static SrRange parseSrRange(final ByteBuf buffer, final ProtocolId protocolId) {
        final var flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        final var range = new SrRangeBuilder().setRangeFlags(switch (protocolId) {
            case Ospf, OspfV3 -> new OspfRangeFlagsCaseBuilder().setInterArea(flags.get(INNER_AREA)).build();
            case IsisLevel1, IsisLevel2 -> new IsisRangeFlagsCaseBuilder()
                .setAddressFamily(flags.get(ADDRESS_FAMILY))
                .setMirrorContext(flags.get(MIRROR_CONTEXT))
                .setSpreadTlv(flags.get(SPREAD))
                .setLeakedFromLevel2(flags.get(LEAKED))
                .setAttachedFlag(flags.get(ATTACHED))
                .build();
            case null, default -> null;
        });

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
        final var flags = new BitArray(FLAGS_SIZE);
        switch (srRange.getRangeFlags()) {
            case OspfRangeFlagsCase ospf -> flags.set(INNER_AREA, ospf.getInterArea());
            case IsisRangeFlagsCase isis -> {
                flags.set(ADDRESS_FAMILY, isis.getAddressFamily());
                flags.set(MIRROR_CONTEXT, isis.getMirrorContext());
                flags.set(SPREAD, isis.getSpreadTlv());
                flags.set(LEAKED, isis.getLeakedFromLevel2());
                flags.set(ATTACHED, isis.getAttachedFlag());
            }
            case null, default -> {
                // No-op
            }
        }
        flags.toByteBuf(aggregator);

        aggregator.writeZero(RESERVED);
        writeUint16(aggregator, srRange.getRangeSize());
        final var prefixSid = srRange.getPrefixSid();
        final var buffer = Unpooled.buffer();
        SrPrefixAttributesParser.serializePrefixAttributes(
            prefixSid.getFlags(),
            prefixSid.getAlgorithm(),
            prefixSid.getSidLabelIndex(),
            buffer);
        TlvUtil.writeTLV(PREFIX_SID, buffer, aggregator);
    }
}
