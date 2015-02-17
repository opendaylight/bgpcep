/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.attribute.sr;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.prefix.state.SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.PrefixSid.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev150206.SidLabel;

public class SrPrefixAttributesParser {

    /* Prefix-SID flags */
    private static int READVERTISEMENT_FLAG = 7;
    private static int NODE_SID_FLAG = 6;
    private static int NO_PHP_FLAG = 5;
    private static int EXPLICIT_NULL_FLAG = 4;
    private static int VALUE_FLAG = 3;
    private static int LOCAL_FLAG = 2;

    private static final int FLAGS_SIZE = 1;

    public static SrPrefix parseSrPrefix(final ByteBuf buffer) {
        final SrPrefixBuilder srPrefixBuilder = new SrPrefixBuilder();
        if (!buffer.isReadable()) {
            return srPrefixBuilder.build();
        }
        final BitSet flags = BitSet.valueOf(ByteArray.readBytes(buffer, FLAGS_SIZE));
        srPrefixBuilder.setFlags(new Flags(flags.get(EXPLICIT_NULL_FLAG), flags.get(LOCAL_FLAG),
                flags.get(NO_PHP_FLAG), flags.get(NODE_SID_FLAG), flags.get(READVERTISEMENT_FLAG), flags.get(VALUE_FLAG)));
        final int algorithm = buffer.readByte();
        srPrefixBuilder.setAlgorithm(Algorithm.forValue(algorithm));
        srPrefixBuilder.setSid(new SidLabel(ByteArray.readAllBytes(buffer)));
        return srPrefixBuilder.build();
    }

    public static ByteBuf serializeSrPrefix(final SrPrefix srPrefix) {
        final ByteBuf value = Unpooled.buffer();
        final Flags srFlags = srPrefix.getFlags();
        final BitSet flags = new BitSet(FLAGS_SIZE);
        if (srFlags.isReadvertisement() != null) {
            flags.set(READVERTISEMENT_FLAG, srFlags.isReadvertisement());
        }
        if (srFlags.isNodeSid() != null) {
            flags.set(NODE_SID_FLAG, srFlags.isNodeSid());
        }
        if (srFlags.isNoPhp() != null) {
            flags.set(NO_PHP_FLAG, srFlags.isNoPhp());
        }
        if (srFlags.isExplicitNull() != null) {
            flags.set(EXPLICIT_NULL_FLAG, srFlags.isExplicitNull());
        }
        if (srFlags.isValue() != null) {
            flags.set(VALUE_FLAG, srFlags.isValue());
        }
        if (srFlags.isLocal() != null) {
            flags.set(LOCAL_FLAG, srFlags.isLocal());
        }
        value.writeBytes(flags.toByteArray());
        value.writeByte(srPrefix.getAlgorithm().getIntValue());
        value.writeBytes(srPrefix.getSid().getValue());
        return value;
    }
}
