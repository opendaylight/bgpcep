/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import io.netty.buffer.ByteBuf;
import java.util.BitSet;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.PrefixSid.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SidLabel;

public class SrPrefixAttributesParser {

    private static final int FLAGS_SIZE = 1;

    /* Flags */
    private static final int RE_ADVERTISEMENT = 7;
    private static final int NODE_SID = 6;
    private static final int NO_PHP = 5;
    private static final int EXPLICIT_NULL = 4;
    private static final int VALUE = 3;
    private static final int LOCAL = 2;

    public static SrPrefix parseSrPrefix(final ByteBuf buffer) {
        final BitSet flags = BitSet.valueOf(ByteArray.readBytes(buffer, FLAGS_SIZE));
        final SrPrefixBuilder builder = new SrPrefixBuilder();
        builder.setFlags(new Flags(flags.get(EXPLICIT_NULL), flags.get(LOCAL), flags.get(NO_PHP), flags.get(NODE_SID), flags.get(RE_ADVERTISEMENT), flags.get(VALUE)));
        builder.setAlgorithm(Algorithm.forValue(buffer.readUnsignedByte()));
        builder.setSid(new SidLabel(ByteArray.readAllBytes(buffer)));
        return builder.build();
    }

    public static void serializeSrPrefix(final SrPrefix srPrefix, final ByteBuf buffer) {
        final Flags flags = srPrefix.getFlags();
        final BitSet bs = new BitSet(FLAGS_SIZE);
        if (flags.isReadvertisement() != null) {
            bs.set(RE_ADVERTISEMENT, flags.isReadvertisement());
        }
        if (flags.isNodeSid() != null) {
            bs.set(NODE_SID, flags.isNodeSid());
        }
        if (flags.isNoPhp() != null) {
            bs.set(NO_PHP, flags.isNoPhp());
        }
        if (flags.isExplicitNull() != null) {
            bs.set(EXPLICIT_NULL, flags.isExplicitNull());
        }
        if (flags.isValue() != null) {
            bs.set(VALUE, flags.isValue());
        }
        if (flags.isLocal() != null) {
            bs.set(LOCAL, flags.isLocal());
        }
        buffer.writeBytes(bs.toByteArray());
        buffer.writeByte(srPrefix.getAlgorithm().getIntValue());
        buffer.writeBytes(srPrefix.getSid().getValue());
    }
}
