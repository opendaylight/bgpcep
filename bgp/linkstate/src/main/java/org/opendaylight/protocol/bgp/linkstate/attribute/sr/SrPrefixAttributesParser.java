/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.PrefixSid.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SidLabel;

public final class SrPrefixAttributesParser {

    private static final int FLAGS_SIZE = 8;

    private SrPrefixAttributesParser() {
        throw new UnsupportedOperationException();
    }

    /* Flags */
    private static final int RE_ADVERTISEMENT = 0;
    private static final int NODE_SID = 1;
    private static final int NO_PHP = 2;
    private static final int EXPLICIT_NULL = 3;
    private static final int VALUE = 4;
    private static final int LOCAL = 5;

    public static SrPrefix parseSrPrefix(final ByteBuf buffer) {
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        final SrPrefixBuilder builder = new SrPrefixBuilder();
        builder.setFlags(new Flags(flags.get(EXPLICIT_NULL), flags.get(LOCAL), flags.get(NO_PHP), flags.get(NODE_SID), flags.get(RE_ADVERTISEMENT), flags.get(VALUE)));
        builder.setAlgorithm(Algorithm.forValue(buffer.readUnsignedByte()));
        builder.setSid(new SidLabel(ByteArray.readAllBytes(buffer)));
        return builder.build();
    }

    public static void serializeSrPrefix(final SrPrefix srPrefix, final ByteBuf buffer) {
        final Flags flags = srPrefix.getFlags();
        final BitArray bs = new BitArray(FLAGS_SIZE);
        bs.set(RE_ADVERTISEMENT, flags.isReadvertisement());
        bs.set(NODE_SID, flags.isNodeSid());
        bs.set(NO_PHP, flags.isNoPhp());
        bs.set(EXPLICIT_NULL, flags.isExplicitNull());
        bs.set(VALUE, flags.isValue());
        bs.set(LOCAL, flags.isLocal());
        bs.toByteBuf(buffer);
        buffer.writeByte(srPrefix.getAlgorithm().getIntValue());
        buffer.writeBytes(srPrefix.getSid().getValue());
    }
}
