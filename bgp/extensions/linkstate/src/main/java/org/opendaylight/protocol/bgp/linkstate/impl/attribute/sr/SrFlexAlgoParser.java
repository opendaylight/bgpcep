/*
 * Copyright (c) 2024 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint8;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint16;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint32;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint8;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Set;
import org.opendaylight.protocol.bgp.linkstate.impl.attribute.LinkstateAttributeParser;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.FlexAlgoDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.FlexAlgoDefinitionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.FlexAlgoPrefixMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.prefix.state.FlexAlgoPrefixMetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.ExtendedAdminGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.FlexAlgo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.FlexAlgoDefinitionFlag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.FlexAlgoPrefixFlag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.FlexMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.definitions.FlexAlgoDefinitionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.definitions.FlexAlgoDefinitionTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.definitions.flex.algo.definition.tlv.FlexAlgoSubtlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.definitions.flex.algo.definition.tlv.FlexAlgoSubtlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.subtlv.UnsupportedTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.subtlv.UnsupportedTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class SrFlexAlgoParser {
    private static final int DEFINITION_FLAGS_SIZE = 32;
    private static final int PREFIX_FLAGS_SIZE = 8;
    private static final int SKIP_FLAG = 1;
    private static final int M_FLAG = 0;
    private static final int E_FLAG = 0;
    private static final int RESERVED = 2;

    private static final int FLEX_ALGO_EXCLUDE_ANY = 1040;
    private static final int FLEX_ALGO_INCLUDE_ANY = 1041;
    private static final int FLEX_ALGO_INCLUDE_ALL = 1042;
    private static final int FLEX_ALGO_DEFINITION_FLAGS = 1043;
    private static final int FLEX_ALGO_EXCLUDE_SRLG = 1045;
    private static final int FLEX_ALGO_UNSUPPORTED = 1046;

    private SrFlexAlgoParser() {
        // Hidden on purpose
    }

    public static FlexAlgoDefinition parseSrFlexAlgoDefinition(final ByteBuf buffer) {
        final var fadTlvs = new ArrayList<FlexAlgoDefinitionTlv>();
        while (buffer.isReadable()) {
            fadTlvs.add(new FlexAlgoDefinitionTlvBuilder()
                .setFlexAlgo(new FlexAlgo(readUint8(buffer)))
                .setMetricType(FlexMetric.forValue(buffer.readByte()))
                .setCalcType(readUint8(buffer))
                .setPriority(readUint8(buffer))
                .setFlexAlgoSubtlvs(parseSrFlexAlgoSubTLVs(buffer))
                .build());
        }
        return new FlexAlgoDefinitionBuilder().setFlexAlgoDefinitionTlv(fadTlvs).build();
    }

    private static FlexAlgoSubtlvs parseSrFlexAlgoSubTLVs(final ByteBuf buffer) {
        final var fasBuilder = new FlexAlgoSubtlvsBuilder();
        for (var entry : LinkstateAttributeParser.getAttributesMap(buffer).entries()) {
            final int type = entry.getKey();
            final ByteBuf value = entry.getValue();
            switch (type) {
                case FLEX_ALGO_EXCLUDE_ANY -> fasBuilder.setExcludeAny(parseExtendedAdminGroup(value));
                case FLEX_ALGO_INCLUDE_ANY -> fasBuilder.setIncludeAny(parseExtendedAdminGroup(value));
                case FLEX_ALGO_INCLUDE_ALL -> fasBuilder.setIncludeAll(parseExtendedAdminGroup(value));
                case FLEX_ALGO_DEFINITION_FLAGS -> {
                    final var flags = BitArray.valueOf(value, DEFINITION_FLAGS_SIZE);
                    fasBuilder.setFlags(new FlexAlgoDefinitionFlag(flags.get(M_FLAG)));
                }
                case FLEX_ALGO_EXCLUDE_SRLG -> fasBuilder.setExcludeSrlg(parseExcludeSrlg(value));
                case FLEX_ALGO_UNSUPPORTED -> fasBuilder.setUnsupportedTlv(parseUnsupportedTlv(value));
                default -> {
                    // no-op
                }
            }
        }
        return fasBuilder.build();
    }

    public static Set<ExtendedAdminGroup> parseExtendedAdminGroup(final ByteBuf buffer) {
        // Check that the Extended Admin Group length is a multiple of 4
        if (buffer.readableBytes() % 4 != 0) {
            return null;
        }

        final var extendedAdminGroup = ImmutableSet.<ExtendedAdminGroup>builder();
        while (buffer.isReadable()) {
            extendedAdminGroup.add(new ExtendedAdminGroup(readUint32(buffer)));
        }
        return extendedAdminGroup.build();
    }

    private static Set<SrlgId> parseExcludeSrlg(final ByteBuf buffer) {
        // Check that the SRLG length is a multiple of 4
        if (buffer.readableBytes() % 4 != 0) {
            return null;
        }

        final var excludeSrlg = ImmutableSet.<SrlgId>builder();
        while (buffer.isReadable()) {
            excludeSrlg.add(new SrlgId(readUint32(buffer)));
        }
        return excludeSrlg.build();
    }

    private static UnsupportedTlv parseUnsupportedTlv(final ByteBuf buffer) {
        final int protocolId = buffer.readByte();
        final var types = ImmutableSet.<Uint16>builder();
        final var utBuilder = new UnsupportedTlvBuilder().setProtocolId(Uint8.valueOf(protocolId));

        while (buffer.isReadable()) {
            if (protocolId == ProtocolId.IsisLevel1.getIntValue()
                    || protocolId == ProtocolId.IsisLevel2.getIntValue()) {
                types.add(readUint8(buffer).toUint16());
            } else if (protocolId == ProtocolId.Ospf.getIntValue() || protocolId == ProtocolId.OspfV3.getIntValue()) {
                types.add(readUint16(buffer));
            }
        }

        return utBuilder.setProtocolType(types.build()).build();
    }

    public static void serializeSrFlexAlgoDefinition(final FlexAlgoDefinition fad, final ByteBuf fadBuffer) {
        fad.nonnullFlexAlgoDefinitionTlv().forEach(fadTlv -> {
            // Serialize Flex Algo Definition
            writeUint8(fadBuffer, fadTlv.getFlexAlgo().getValue());
            fadBuffer.writeByte(fadTlv.getMetricType().getIntValue());
            writeUint8(fadBuffer, fadTlv.getCalcType());
            writeUint8(fadBuffer, fadTlv.getPriority());

            // Then, serialize SubTlvs if present
            final var fas = fadTlv.getFlexAlgoSubtlvs();
            if (fas != null) {
                final var ea = fas.getExcludeAny();
                if (ea != null) {
                    final var buffer = Unpooled.buffer();
                    ea.forEach(id -> writeUint32(buffer, id.getValue()));
                    TlvUtil.writeTLV(FLEX_ALGO_EXCLUDE_ANY, buffer, fadBuffer);
                }
                final var ia = fas.getIncludeAny();
                if (ia != null) {
                    final var buffer = Unpooled.buffer();
                    ia.forEach(id -> writeUint32(buffer, id.getValue()));
                    TlvUtil.writeTLV(FLEX_ALGO_INCLUDE_ANY, buffer, fadBuffer);
                }
                final var all = fas.getIncludeAll();
                if (all != null) {
                    final var buffer = Unpooled.buffer();
                    all.forEach(id -> writeUint32(buffer, id.getValue()));
                    TlvUtil.writeTLV(FLEX_ALGO_INCLUDE_ALL, buffer, fadBuffer);
                }
                final var flags = fas.getFlags();
                if (flags != null) {
                    final var buffer = Unpooled.buffer();
                    final var bs = new BitArray(DEFINITION_FLAGS_SIZE);
                    bs.set(M_FLAG, flags.getInterArea());
                    bs.toByteBuf(buffer);
                    TlvUtil.writeTLV(FLEX_ALGO_DEFINITION_FLAGS, buffer, fadBuffer);
                }
                final var es = fas.getExcludeSrlg();
                if (es != null) {
                    final var buffer = Unpooled.buffer();
                    es.forEach(id -> writeUint32(buffer, id.getValue()));
                    TlvUtil.writeTLV(FLEX_ALGO_EXCLUDE_SRLG, buffer, fadBuffer);
                }
                final var ut = fas.getUnsupportedTlv();
                if (ut != null) {
                    final ByteBuf buffer = Unpooled.buffer();
                    serializeUnsupportedTlv(ut, buffer);
                    TlvUtil.writeTLV(FLEX_ALGO_UNSUPPORTED, buffer, fadBuffer);
                }
            }
        });
    }

    private static void serializeUnsupportedTlv(final UnsupportedTlv ust, final ByteBuf buffer) {
        writeUint8(buffer, ust.getProtocolId());
        final int protocolId = ust.getProtocolId().intValue();
        ust.getProtocolType().forEach(type -> {
            if (protocolId == ProtocolId.IsisLevel1.getIntValue()
                    || protocolId == ProtocolId.IsisLevel2.getIntValue()) {
                buffer.writeByte(type.shortValue());
            } else if (protocolId == ProtocolId.Ospf.getIntValue() || protocolId == ProtocolId.OspfV3.getIntValue()) {
                writeUint16(buffer, type);
            }
        });
    }

    public static FlexAlgoPrefixMetric parseFlexAlgoPrefixMetric(final ByteBuf buffer, final ProtocolId protocolId) {
        final var fapmBuilder = new FlexAlgoPrefixMetricBuilder().setFlexAlgo(new FlexAlgo(readUint8(buffer)));

        if (protocolId == ProtocolId.Ospf || protocolId == ProtocolId.OspfV3) {
            final BitArray flags = BitArray.valueOf(buffer, PREFIX_FLAGS_SIZE);
            fapmBuilder.setFlags(new FlexAlgoPrefixFlag(flags.get(E_FLAG)));
        } else {
            buffer.skipBytes(SKIP_FLAG);
        }
        buffer.skipBytes(RESERVED);

        return fapmBuilder.setMetric(readUint32(buffer)).build();
    }

    public static void serializeFlexAlgoPrefixMetric(final FlexAlgoPrefixMetric fapm, final ByteBuf buffer) {
        writeUint8(buffer, fapm.getFlexAlgo().getValue());
        final var flags = fapm.getFlags();
        if (flags != null) {
            final BitArray bs = new BitArray(PREFIX_FLAGS_SIZE);
            bs.set(E_FLAG, flags.getExternalMetric());
            bs.toByteBuf(buffer);
        } else {
            buffer.writeZero(SKIP_FLAG);
        }
        buffer.writeZero(RESERVED);
        writeUint32(buffer, fapm.getMetric());
    }
}
