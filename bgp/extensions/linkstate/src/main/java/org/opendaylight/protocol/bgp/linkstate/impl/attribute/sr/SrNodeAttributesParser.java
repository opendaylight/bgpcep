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
import org.opendaylight.mdsal.rfc8294.netty.RFC8294ByteBufUtils;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.NodeMsd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.NodeMsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrAlgorithmBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrLocalBlock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.state.SrLocalBlockBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.MsdType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.node.msd.tlv.Msd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.node.msd.tlv.MsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.sr.capabilities.tlv.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.sr.capabilities.tlv.SrgbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.sr.local.block.tlv.Srlb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.sr.local.block.tlv.SrlbBuilder;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class SrNodeAttributesParser {

    private static final int FLAGS_SIZE = 8;
    /* SR Capabilities flags */
    private static final int MPLS_IPV4 = 0;
    private static final int MPLS_IPV6 = 1;
    private static final int RESERVED = 1;
    private static final int SKIP_FLAG = 1;

    private SrNodeAttributesParser() {

    }

    public static SrCapabilities parseSrCapabilities(final ByteBuf buffer, final ProtocolId protocol) {
        final SrCapabilitiesBuilder builder = new SrCapabilitiesBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        setFlags(flags, protocol, builder);
        buffer.skipBytes(RESERVED);
        final List<Srgb> srgb = new ArrayList<Srgb>();
        while (buffer.isReadable()) {
            srgb.add(new SrgbBuilder()
                    .setRangeSize(RFC8294ByteBufUtils.readUint24(buffer))
                    .setSidLabelIndex(SidLabelIndexParser.parseSidSubTlv(buffer))
                    .build());
        }
        builder.setSrgb(srgb);
        return builder.build();
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
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(MPLS_IPV4, caps.getMplsIpv4());
        bs.set(MPLS_IPV6, caps.getMplsIpv6());
        bs.toByteBuf(buffer);
        buffer.writeZero(RESERVED);
        for (Srgb range: caps.getSrgb()) {
            RFC8294ByteBufUtils.writeUint24(buffer, range.getRangeSize());
            TlvUtil.writeTLV(SID_LABEL, SidLabelIndexParser.serializeSidValue(range.getSidLabelIndex()), buffer);
        }
    }

    public static SrAlgorithm parseSrAlgorithms(final ByteBuf buffer) {
        final var builder = new SrAlgorithmBuilder();
        final var algs = ImmutableSet.<Algorithm>builder();
        while (buffer.isReadable()) {
            algs.add(Algorithm.forValue(buffer.readUnsignedByte()));
        }
        builder.setAlgorithms(algs.build());
        return builder.build();
    }

    public static void serializeSrAlgorithms(final SrAlgorithm alg, final ByteBuf buffer) {
        final var algorithms = alg.getAlgorithms();
        if (algorithms != null) {
            for (var a : algorithms) {
                buffer.writeByte(a.getIntValue());
            }
        }
    }

    public static SrLocalBlock parseSrLocalBlock(final ByteBuf buffer) {
        final SrLocalBlockBuilder builder = new SrLocalBlockBuilder();
        // Skip Flags as no one has been defined for the moment. See rfc9085#section-2.1.4 and rfc8667#section-3.3
        buffer.skipBytes(SKIP_FLAG);
        buffer.skipBytes(RESERVED);
        final List<Srlb> srlb = new ArrayList<Srlb>();
        while (buffer.isReadable()) {
            srlb.add(new SrlbBuilder()
                    .setRangeSize(RFC8294ByteBufUtils.readUint24(buffer))
                    .setSidLabelIndex(SidLabelIndexParser.parseSidSubTlv(buffer))
                    .build());
        }
        builder.setSrlb(srlb);
        return builder.build();
    }

    public static void serializeSrLocalBlock(final SrLocalBlock srlb, final ByteBuf buffer) {
        // Skip Flags as no one has been defined for the moment. See rfc9085#section-2.1.4 and rfc8667#section-3.3
        buffer.writeZero(SKIP_FLAG);
        buffer.writeZero(RESERVED);
        for (Srlb range: srlb.getSrlb()) {
            RFC8294ByteBufUtils.writeUint24(buffer, range.getRangeSize());
            TlvUtil.writeTLV(SID_LABEL, SidLabelIndexParser.serializeSidValue(range.getSidLabelIndex()), buffer);
        }
    }

    public static NodeMsd parseSrNodeMsd(final ByteBuf buffer) {
        final List<Msd> msds = new ArrayList<Msd>();
        while (buffer.isReadable()) {
            msds.add(new MsdBuilder()
                    .setType(MsdType.forValue(buffer.readByte()))
                    .setValue(readUint8(buffer))
                    .build());
        }
        return new NodeMsdBuilder().setMsd(msds).build();
    }

    public static void serializeSrNodeMsd(final NodeMsd nodeMsd, final ByteBuf buffer) {
        nodeMsd.getMsd().forEach(msd -> {
            buffer.writeByte(msd.getType().getIntValue());
            ByteBufUtils.writeUint8(buffer, msd.getValue());
        });
    }
}
