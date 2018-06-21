/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;

import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.SIDParser.SID_TYPE;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.state.SrAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.state.SrAlgorithmBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.state.SrCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Algorithm;

public final class SrNodeAttributesParser {

    private static final int FLAGS_SIZE = 8;
    /* SR Capabilities flags */
    private static final int MPLS_IPV4 = 0;
    private static final int MPLS_IPV6 = 1;
    private static final int SR_IPV6 = 2;
    private static final int RESERVERED = 1;

    private SrNodeAttributesParser() {
        throw new UnsupportedOperationException();
    }

    public static SrCapabilities parseSrCapabilities(final ByteBuf buffer, final ProtocolId protocol) {
        final SrCapabilitiesBuilder builder = new SrCapabilitiesBuilder();
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        setFlags(flags, protocol, builder);
        buffer.skipBytes(RESERVERED);
        builder.setRangeSize((long) buffer.readUnsignedMedium());
        builder.setSidLabelIndex(SidLabelIndexParser.parseSidSubTlv(buffer));
        return builder.build();
    }

    private static void setFlags(final BitArray flags, final ProtocolId protocol, final SrCapabilitiesBuilder builder) {
        if (protocol.equals(ProtocolId.IsisLevel1) || protocol.equals(ProtocolId.IsisLevel2)) {
            builder.setMplsIpv4(flags.get(MPLS_IPV4));
            builder.setMplsIpv6(flags.get(MPLS_IPV6));
            builder.setSrIpv6(flags.get(SR_IPV6));
        } else {
            builder.setMplsIpv4(Boolean.FALSE);
            builder.setMplsIpv6(Boolean.FALSE);
            builder.setSrIpv6(Boolean.FALSE);
        }
    }

    public static void serializeSrCapabilities(final SrCapabilities caps, final ByteBuf buffer) {
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(MPLS_IPV4, caps.isMplsIpv4());
        bs.set(MPLS_IPV6, caps.isMplsIpv6());
        bs.set(SR_IPV6, caps.isSrIpv6());
        bs.toByteBuf(buffer);
        buffer.writeZero(RESERVERED);
        buffer.writeMedium(caps.getRangeSize().intValue());
        TlvUtil.writeTLV(SID_TYPE, SidLabelIndexParser.serializeSidValue(caps.getSidLabelIndex()), buffer);
    }

    public static SrAlgorithm parseSrAlgorithms(final ByteBuf buffer) {
        final SrAlgorithmBuilder builder = new SrAlgorithmBuilder();
        final List<Algorithm> algs = new ArrayList<>();
        while (buffer.isReadable()) {
            algs.add(Algorithm.forValue(buffer.readUnsignedByte()));
        }
        builder.setAlgorithms(algs);
        return builder.build();
    }

    public static void serializeSrAlgorithms(final SrAlgorithm alg, final ByteBuf buffer) {
        if (alg.getAlgorithms() != null) {
            for (final Algorithm a : alg.getAlgorithms()) {
                buffer.writeByte(a.getIntValue());
            }
        }
    }
}
