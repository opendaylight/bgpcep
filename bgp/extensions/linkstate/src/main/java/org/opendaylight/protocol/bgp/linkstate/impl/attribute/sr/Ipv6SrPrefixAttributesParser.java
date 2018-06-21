/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr;


import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.Ipv6SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.prefix.state.Ipv6SrPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.Algorithm;

public final class Ipv6SrPrefixAttributesParser {
    private static final int FLAGS_SIZE = 2;

    private Ipv6SrPrefixAttributesParser() {
        throw new UnsupportedOperationException();
    }

    public static Ipv6SrPrefix parseSrIpv6Prefix(final ByteBuf buffer) {
        final Ipv6SrPrefixBuilder builder = new Ipv6SrPrefixBuilder();
        buffer.skipBytes(FLAGS_SIZE);
        builder.setAlgorithm(Algorithm.forValue(buffer.readUnsignedByte()));
        return builder.build();
    }

    public static void serializePrefixAttributes(final Algorithm algorithm, final ByteBuf buffer) {
        buffer.writeZero(FLAGS_SIZE);
        buffer.writeByte(algorithm.getIntValue());
    }

    public static void serializeIpv6SrPrefix(final Ipv6SrPrefix ipv6SrPrefix, final ByteBuf buffer) {
        serializePrefixAttributes(ipv6SrPrefix.getAlgorithm(), buffer);
    }
}
