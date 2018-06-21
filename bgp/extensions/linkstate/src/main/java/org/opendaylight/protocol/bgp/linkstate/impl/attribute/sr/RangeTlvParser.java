/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.PrefixAttributesParser.BINDING_SID;
import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv4PrefixSidParser.PREFIX_SID;
import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.Ipv6PrefixSidParser.IPV6_PREFIX_SID;
import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.SIDParser.SID_TYPE;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SidLabelIndexParser.Size;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.SrRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.SrRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.RangeSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.BindingSidTlvCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.BindingSidTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.Ipv6PrefixSidTlvCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.Ipv6PrefixSidTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.PrefixSidTlvCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.PrefixSidTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.SidLabelTlvCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.sub.tlvs.range.sub.tlv.SidLabelTlvCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.tlv.SubTlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.range.tlv.SubTlvsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RangeTlvParser {
    private static final Logger LOG = LoggerFactory.getLogger(RangeTlvParser.class);
    /* Flags */
    private static final int FLAGS_SIZE = 8;
    private static final int INNER_AREA = 0;
    private static final int RESERVED = 1;

    private RangeTlvParser() {
        throw new UnsupportedOperationException();
    }

    public static SrRange parseSrRange(final ByteBuf buffer, final ProtocolId protocolId) {
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        final SrRangeBuilder range = new SrRangeBuilder();
        if (protocolId.equals(ProtocolId.Ospf)) {
            range.setInterArea(flags.get(INNER_AREA));
        } else {
            range.setInterArea(Boolean.FALSE);
        }
        buffer.skipBytes(RESERVED);
        range.setRangeSize(buffer.readUnsignedShort());
        range.setSubTlvs(parseRangeSubTlvs(buffer, protocolId));
        return range.build();
    }

    private static List<SubTlvs> parseRangeSubTlvs(final ByteBuf buffer, final ProtocolId protocolId) {
        final List<SubTlvs> subTlvs = new ArrayList<>();
        while (buffer.isReadable()) {
            final SubTlvsBuilder subTlv = new SubTlvsBuilder();
            final RangeSubTlv subTlvCase;
            final int type = buffer.readUnsignedShort();
            final int length = buffer.readUnsignedShort();
            switch (type) {
            case PREFIX_SID:
                subTlvCase = new PrefixSidTlvCaseBuilder(SrPrefixAttributesParser.parseSrPrefix(buffer.readSlice(length), protocolId)).build();
                break;
            case IPV6_PREFIX_SID:
                subTlvCase = new Ipv6PrefixSidTlvCaseBuilder(Ipv6SrPrefixAttributesParser.parseSrIpv6Prefix(buffer.readSlice(length))).build();
                break;
            case BINDING_SID:
                subTlvCase = new BindingSidTlvCaseBuilder(BindingSidLabelParser.parseBindingSidLabel(buffer.readSlice(length), protocolId)).build();
                break;
            case SID_TYPE:
                subTlvCase = new SidLabelTlvCaseBuilder().setSidLabelIndex(SidLabelIndexParser.parseSidLabelIndex(Size.forValue(length), buffer.readSlice(length))).build();
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
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(INNER_AREA, srRange.isInterArea());
        flags.toByteBuf(aggregator);
        aggregator.writeZero(RESERVED);
        aggregator.writeShort(srRange.getRangeSize());
        serializeSubTlvs(aggregator, srRange.getSubTlvs());
    }

    private static void serializeSubTlvs(final ByteBuf aggregator, final List<SubTlvs> subTlvs) {
        for (final SubTlvs subTlv : subTlvs) {
            ByteBuf buffer = Unpooled.buffer();
            final RangeSubTlv rangeSubTlv = subTlv.getRangeSubTlv();
            if (rangeSubTlv instanceof PrefixSidTlvCase) {
                final PrefixSidTlvCase prefixSidTlv = (PrefixSidTlvCase) rangeSubTlv;
                SrPrefixAttributesParser.serializePrefixAttributes(
                    prefixSidTlv.getFlags(),
                    prefixSidTlv.getAlgorithm(),
                    prefixSidTlv.getSidLabelIndex(),
                    buffer);
                TlvUtil.writeTLV(PREFIX_SID, buffer, aggregator);
            } else if (rangeSubTlv instanceof Ipv6PrefixSidTlvCase) {
                final Ipv6PrefixSidTlvCase prefixSidTlv = (Ipv6PrefixSidTlvCase) rangeSubTlv;
                Ipv6SrPrefixAttributesParser.serializePrefixAttributes(prefixSidTlv.getAlgorithm(), buffer);
                TlvUtil.writeTLV(IPV6_PREFIX_SID, buffer, aggregator);
            } else if (rangeSubTlv instanceof BindingSidTlvCase) {
                final BindingSidTlvCase rangeTlv = (BindingSidTlvCase) rangeSubTlv;
                BindingSidLabelParser.serializeBindingSidAttributes(
                    rangeTlv.getWeight(),
                    rangeTlv.getFlags(),
                    rangeTlv.getBindingSubTlvs(),
                    buffer);
                TlvUtil.writeTLV(BINDING_SID, buffer, aggregator);
            } else if (rangeSubTlv instanceof SidLabelTlvCase) {
                buffer = SidLabelIndexParser.serializeSidValue(((SidLabelTlvCase) rangeSubTlv).getSidLabelIndex());
                TlvUtil.writeTLV(SID_TYPE, buffer, aggregator);
            }
        }
    }

}
