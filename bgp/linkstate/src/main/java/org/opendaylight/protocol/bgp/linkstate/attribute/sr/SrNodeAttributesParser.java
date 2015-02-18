/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrAlgorithmBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrCapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.state.SrSidLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SidLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SrCapabilities.Flags;

public final class SrNodeAttributesParser {

    private static final int FLAGS_SIZE = 1;

    private static final int SID_TLV_TYPE = 1;

    /* SR Capabilities flags */
    private static final int IPV4 = 7;
    private static final int IPV6 = 6;

    public static SrSidLabel parseSidLabelBinding(final ByteBuf buffer) {
        // TODO Auto-generated method stub
        return null;
    }

    public static SrCapabilities parseSrCapabilities(final ByteBuf buffer) {
        final SrCapabilitiesBuilder builder = new SrCapabilitiesBuilder();
        final BitSet flags = BitSet.valueOf(ByteArray.readBytes(buffer, FLAGS_SIZE));
        builder.setFlags(new Flags(flags.get(IPV4), flags.get(IPV6)));
        builder.setValueRange((long)buffer.readUnsignedMedium());
        buffer.skipBytes(2);
        builder.setSid(new SidLabel(ByteArray.readAllBytes(buffer)));
        return builder.build();
    }

    public static void serializeSrCapabilities(final SrCapabilities caps, final ByteBuf buffer) {
        final Flags flags = caps.getFlags();
        final BitSet bs = new BitSet(FLAGS_SIZE);
        if (flags.isIpv4() != null) {
            bs.set(IPV4, flags.isIpv4());
        }
        if (flags.isIpv6() != null) {
            bs.set(IPV6, flags.isIpv6());
        }
        buffer.writeBytes(bs.toByteArray());
        buffer.writeMedium(caps.getValueRange().intValue());
        buffer.writeByte(SID_TLV_TYPE);
        final byte[] sid = caps.getSid().getValue();
        buffer.writeByte(sid.length);
        buffer.writeBytes(sid);
    }

    public static SrAlgorithm parseSrAlgorithms(final ByteBuf buffer) {
        final SrAlgorithmBuilder builder = new SrAlgorithmBuilder();
        final List<Algorithm> algs = new ArrayList<>();
        while (buffer.isReadable()) {
            algs.add(Algorithm.forValue(buffer.readUnsignedByte()));
        }
        builder.setAlgorithm(algs);
        return builder.build();
    }

    public static void serializeSrAlgorithms(final SrAlgorithm alg, final ByteBuf buffer) {
        if (alg.getAlgorithm() != null) {
            for (final Algorithm a : alg.getAlgorithm()) {
                buffer.writeByte(a.getIntValue());
            }
        }
    }
}
