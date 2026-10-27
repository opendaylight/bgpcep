/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.SidLabelIndexParser.SID_LABEL;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint8;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint8;

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.ietf.rfc8294.netty.RFC8294ByteBufUtils;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.segment.routing.NodeMsd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.segment.routing.NodeMsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.segment.routing.SrMplsCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.segment.routing.SrMplsCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.AlgorithmStd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.MsdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.capabilities.tlv.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.capabilities.tlv.SrgbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.local.block.tlv.Srlb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.local.block.tlv.SrlbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.yang.igp.algo.types.rev261110.OdlIgpAlgoUtil;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class SrNodeAttributesParser {
    private static final int FLAGS_SIZE = 8;

    // SR Capabilities flags
    private static final int MPLS_IPV4 = 0;
    private static final int MPLS_IPV6 = 1;
    private static final int RESERVED = 1;
    private static final int SKIP_FLAG = 1;

    // IGP algorithms
    private static final Algorithm ALGO_SPF = new Algorithm(AlgorithmStd.ShortestPathFirst);
    private static final Algorithm ALGO_SPF_STRICT = new Algorithm(AlgorithmStd.StrictShortestPathFirst);

    private SrNodeAttributesParser() {
        // Hidden on purpose
    }

    public static void parseSrCapabilities(final SrMplsCapabilitiesBuilder builder, final ByteBuf buffer,
            final ProtocolId protocol) {
        final var flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        setFlags(flags, protocol, builder);
        buffer.skipBytes(RESERVED);
        final var srgb = new ArrayList<Srgb>();
        while (buffer.isReadable()) {
            srgb.add(new SrgbBuilder()
                    .setRangeSize(RFC8294ByteBufUtils.readUint24(buffer))
                    .setSidLabelIndex(SidLabelIndexParser.parseSidSubTlv(buffer))
                    .build());
        }
        builder.setSrgb(srgb);
    }

    private static void setFlags(final BitArray flags, final ProtocolId protocol,
        final SrMplsCapabilitiesBuilder builder) {
        if (protocol.equals(ProtocolId.IsisLevel1) || protocol.equals(ProtocolId.IsisLevel2)) {
            builder.setMplsIpv4(flags.get(MPLS_IPV4));
            builder.setMplsIpv6(flags.get(MPLS_IPV6));
        } else {
            builder.setMplsIpv4(Boolean.FALSE);
            builder.setMplsIpv6(Boolean.FALSE);
        }
    }

    public static void serializeSrCapabilities(final SrMplsCapabilities caps, final ByteBuf buffer) {
        final var bs = new BitArray(FLAGS_SIZE);
        bs.set(MPLS_IPV4, caps.getMplsIpv4());
        bs.set(MPLS_IPV6, caps.getMplsIpv6());
        bs.toByteBuf(buffer);
        buffer.writeZero(RESERVED);
        for (var range: caps.nonnullSrgb()) {
            RFC8294ByteBufUtils.writeUint24(buffer, range.getRangeSize());
            TlvUtil.writeTLV(SID_LABEL, SidLabelIndexParser.serializeSidValue(range.getSidLabelIndex()), buffer);
        }
    }

    public static Set<Algorithm> parseSrAlgorithms(final ByteBuf buffer) {
        final var algs = ImmutableSet.<Algorithm>builder();
        while (buffer.isReadable()) {
            final var algo = readAlgorithm(buffer);
            if (algo != null) {
                algs.add(algo);
            }
        }
        return algs.build();
    }

    static @Nullable Algorithm readAlgorithm(final ByteBuf buffer) {
        // read and check that Algorithm is valid:
        // - 0..1 for standard algorithms
        // - 128..255 for FlexAlgo
        final var algo = readUint8(buffer);
        return switch (algo.byteValue()) {
            case 0 -> ALGO_SPF;
            case 1 -> ALGO_SPF_STRICT;
            default -> {
                final var flex = OdlIgpAlgoUtil.algoFlexFor(algo);
                yield flex == null ? null : new Algorithm(flex);
            }
        };
    }

    static void writeAlgorithm(final ByteBuf buffer, final Algorithm alg) {
        final var std = alg.getAlgorithmStd();
        if (std != null) {
            buffer.writeByte(std.getIntValue());
        } else {
            writeUint8(buffer, alg.getAlgoFlex().getValue());
        }
    }

    public static void serializeSrAlgorithms(final Set<Algorithm> algs, final ByteBuf buffer) {
        for (var alg : algs) {
            writeAlgorithm(buffer, alg);
        }
    }

    public static List<Srlb> parseSrLocalBlock(final ByteBuf buffer) {
        // Skip Flags as no one has been defined for the moment. See rfc9085#section-2.1.4 and rfc8667#section-3.3
        buffer.skipBytes(SKIP_FLAG);
        buffer.skipBytes(RESERVED);
        final var srlbs = new ArrayList<Srlb>();
        while (buffer.isReadable()) {
            srlbs.add(new SrlbBuilder()
                .setRangeSize(RFC8294ByteBufUtils.readUint24(buffer))
                .setSidLabelIndex(SidLabelIndexParser.parseSidSubTlv(buffer))
                .build());
        }
        return srlbs;
    }

    public static void serializeSrLocalBlock(final List<Srlb> srlbs, final ByteBuf buffer) {
        // Skip Flags as no one has been defined for the moment. See rfc9085#section-2.1.4 and rfc8667#section-3.3
        buffer.writeZero(SKIP_FLAG);
        buffer.writeZero(RESERVED);
        for (var srlb : srlbs) {
            RFC8294ByteBufUtils.writeUint24(buffer, srlb.getRangeSize());
            TlvUtil.writeTLV(SID_LABEL, SidLabelIndexParser.serializeSidValue(srlb.getSidLabelIndex()), buffer);
        }
    }

    public static List<NodeMsd> parseSrNodeMsd(final ByteBuf buffer) {
        final var msds = new ArrayList<NodeMsd>();
        while (buffer.isReadable()) {
            final var msdType = MsdType.forValue(buffer.readByte());
            final var value = readUint8(buffer);
            // Skip Unknown MSD Type
            if (msdType != null) {
                msds.add(new NodeMsdBuilder().setType(msdType).setValue(value).build());
            }
        }
        return msds;
    }

    public static void serializeSrNodeMsd(final List<NodeMsd> msds, final ByteBuf buffer) {
        msds.forEach(msd -> {
            buffer.writeByte(msd.getType().getIntValue());
            ByteBufUtils.writeUint8(buffer, msd.getValue());
        });
    }
}
