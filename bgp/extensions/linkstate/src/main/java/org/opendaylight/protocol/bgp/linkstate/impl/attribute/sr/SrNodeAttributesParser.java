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

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.ietf.rfc8294.netty.RFC8294ByteBufUtils;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.SrCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.sr.capabilities.NodeMsd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.sr.capabilities.NodeMsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.MsdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.capabilities.tlv.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.capabilities.tlv.SrgbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.local.block.tlv.Srlb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sr.local.block.tlv.SrlbBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class SrNodeAttributesParser {
    private static final int FLAGS_SIZE = 8;

    // SR Capabilities flags
    private static final int MPLS_IPV4 = 0;
    private static final int MPLS_IPV6 = 1;
    private static final int RESERVED = 1;
    private static final int SKIP_FLAG = 1;

    private SrNodeAttributesParser() {
        // Hidden on purpose
    }

    public static void parseSrCapabilities(final SrCapabilitiesBuilder builder, final ByteBuf buffer,
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

    private static void setFlags(final BitArray flags, final ProtocolId protocol, final SrCapabilitiesBuilder builder) {
        if (protocol.equals(ProtocolId.IsisLevel1) || protocol.equals(ProtocolId.IsisLevel2)) {
            builder.setMplsIpv4(flags.get(MPLS_IPV4));
            builder.setMplsIpv6(flags.get(MPLS_IPV6));
        } else {
            builder.setMplsIpv4(Boolean.FALSE);
            builder.setMplsIpv6(Boolean.FALSE);
        }
    }

    public static void serializeSrCapabilities(final SrCapabilities caps, final ByteBuf buffer) {
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
            algs.add(Algorithm.forValue(buffer.readUnsignedByte()));
        }
        return algs.build();
    }

    public static void serializeSrAlgorithms(final Set<Algorithm> algs, final ByteBuf buffer) {
        algs.forEach(alg -> buffer.writeByte(alg.getIntValue()));
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
            msds.add(new NodeMsdBuilder()
                .setType(MsdType.forValue(buffer.readByte()))
                .setValue(readUint8(buffer))
                .build());
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
