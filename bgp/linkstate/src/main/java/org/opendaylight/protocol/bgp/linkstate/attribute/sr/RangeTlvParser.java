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
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrBindingSidLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.SidLabelIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.RangeSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.BindingSidTlvCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.BindingSidTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.PrefixSidTlvCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.PrefixSidTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.SidLabelTlvCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.SidLabelTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.tlv.SubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.tlv.SubTlvsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangeTlvParser {

    private static final Logger LOG = LoggerFactory.getLogger(RangeTlvParser.class);

    private RangeTlvParser() {
        throw new UnsupportedOperationException();
    }

    /* Flags */
    private static final int FLAGS_SIZE = 8;
    private static final int INNER_AREA = 0;

    private static final int RESERVED = 1;

    public static SrRange parseSrRange(final ByteBuf buffer) {
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        final SrRangeBuilder range = new SrRangeBuilder();
        range.setInterArea(flags.get(INNER_AREA));
        buffer.skipBytes(RESERVED);
        range.setRangeSize(buffer.readUnsignedShort());
        range.setSubTlvs(parseRangeSubTlvs(buffer));
        return null;
    }

    private static List<SubTlvs> parseRangeSubTlvs(final ByteBuf buffer) {
        final List<SubTlvs> subTlvs = new ArrayList<>();
        while (buffer.isReadable()) {
            final SubTlvsBuilder subTlv = new SubTlvsBuilder();
            RangeSubTlv subTlvCase = null;
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            switch (type) {
                case PrefixAttributesParser.PREFIX_SID:
                    subTlvCase = new PrefixSidTlvCaseBuilder(SrPrefixAttributesParser.parseSrPrefix(buffer.readSlice(length))).build();
                    break;
                case PrefixAttributesParser.BINDING_SID:
                    subTlvCase = new BindingSidTlvCaseBuilder(BindingSidLabelParser.parseBindingSidLabel(buffer.readSlice(length))).build();
                    break;
                case SidLabelIndexParser.SID_TYPE:
                    subTlvCase = new SidLabelTlvCaseBuilder((SidLabelIndex) SidLabelIndexParser.parseSidSubTlv(buffer.readSlice(length))).build();
                    break;
                default:
                    LOG.info("Unknown type of range sub-tlv: {}", type);
                    buffer.skipBytes(length);
                    continue;
            }
            subTlvs.add(subTlv.setRangeSubTlv(subTlvCase).build());
        }
        return subTlvs;
    }

    public static void serializeSrRange(final SrRange srRange, final ByteBuf aggregator) {
        final ByteBuf buffer = Unpooled.EMPTY_BUFFER;
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(INNER_AREA, srRange.isInterArea());
        flags.toByteBuf(buffer);
        buffer.writeZero(RESERVED);
        buffer.writeShort(srRange.getRangeSize());
        serializeSubTlvs(buffer, srRange.getSubTlvs());
        TlvUtil.writeTLV(PrefixAttributesParser.RANGE, buffer, aggregator);
    }

    private static void serializeSubTlvs(final ByteBuf aggregator, final List<SubTlvs> subTlvs) {
        for (final SubTlvs subTlv : subTlvs) {
            ByteBuf buffer = Unpooled.EMPTY_BUFFER;
            final RangeSubTlv rangeSubTlv = subTlv.getRangeSubTlv();
            if (rangeSubTlv instanceof PrefixSidTlvCase) {
                SrPrefixAttributesParser.serializeSrPrefix((SrPrefix) rangeSubTlv, buffer);
                TlvUtil.writeTLV(PrefixAttributesParser.PREFIX_SID, buffer, aggregator);
            } else if (rangeSubTlv instanceof BindingSidTlvCase) {
                BindingSidLabelParser.serializeBindingSidLabel((SrBindingSidLabel) rangeSubTlv, buffer);
                TlvUtil.writeTLV(PrefixAttributesParser.BINDING_SID, buffer, aggregator);
            } else if (rangeSubTlv instanceof SidLabelTlvCase) {
                buffer = SidLabelIndexParser.serializeSidValue((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.SidLabelIndex) rangeSubTlv);
                TlvUtil.writeTLV(SidLabelIndexParser.SID_TYPE, buffer, aggregator);
            }
        }
    }

}
