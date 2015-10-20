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
import org.opendaylight.protocol.bgp.linkstate.attribute.PrefixAttributesParser;
import org.opendaylight.protocol.bgp.linkstate.attribute.sr.SidLabelIndexParser.Size;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.sid.label.index.SidLabelIndex;

public final class SrPrefixAttributesParser {

    private SrPrefixAttributesParser() {
        throw new UnsupportedOperationException();
    }

    private static final int RESERVED_PREFIX = 2;

    public static SrPrefix parseSrPrefix(final ByteBuf buffer) {
        final SrPrefixBuilder builder = new SrPrefixBuilder();
        builder.setFlags(new byte[] { (byte) buffer.readUnsignedByte() });
        builder.setAlgorithm(Algorithm.forValue(buffer.readUnsignedByte()));
        buffer.skipBytes(RESERVED_PREFIX);
        builder.setSidLabelIndex(SidLabelIndexParser.parseSidLabelIndex(Size.forValue(buffer.readableBytes()), buffer));
        return builder.build();
    }

    public static void serializeSrPrefix(final SrPrefix srPrefix, final ByteBuf aggregator) {
        final ByteBuf buffer = serializePrefixAttributes(srPrefix.getFlags(), srPrefix.getAlgorithm(), srPrefix.getSidLabelIndex());
        TlvUtil.writeTLV(PrefixAttributesParser.PREFIX_SID, buffer, aggregator);
    }

    public static ByteBuf serializePrefixAttributes(final byte[] flags, final Algorithm algorithm, final SidLabelIndex sidLabelIndex) {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(flags[0]);
        buffer.writeByte(algorithm.getIntValue());
        buffer.writeZero(RESERVED_PREFIX);
        buffer.writeBytes(SidLabelIndexParser.serializeSidValue(sidLabelIndex));
        return buffer;
    }

}
