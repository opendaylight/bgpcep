/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RdAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;

/**
 * Utility class for of RouteDistinguisher serialization and parsing.
 * https://tools.ietf.org/html/rfc4364#section-4.2
 */
public final class RouteDistinguisherUtil {

    private static final int IPV4_TYPE = 1;
    private static final int AS_4BYTE_TYPE = 2;
    private static final char SEPARATOR = ':';
    public static final int RD_LENGTH = 8;

    private RouteDistinguisherUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Serializes route distinguisher according to type and writes into ByteBuf.
     *
     * @param distinquisher
     * @param byteAggregator
     */
    public static void serializeRouteDistinquisher(final RouteDistinguisher distinquisher, final ByteBuf byteAggregator) {
        if (distinquisher.getRdAs() != null) {
            final String[] values = distinquisher.getRdAs().getValue().split(":");
            byteAggregator.writeShort(AS_4BYTE_TYPE);
            final int admin = Integer.parseInt(values[0]);
            byteAggregator.writeInt(admin);
            final int assignedNumber = Integer.parseInt(values[1]);
            byteAggregator.writeShort(assignedNumber);
        } else if (distinquisher.getRdIpv4() != null) {
            final String[] values = distinquisher.getRdIpv4().getValue().split(":");
            final Ipv4Address ip = new Ipv4Address(values[0]);
            byteAggregator.writeShort(IPV4_TYPE);
            byteAggregator.writeBytes(Ipv4Util.byteBufForAddress(ip));
            final int assignedNumber = Integer.parseInt(values[1]);
            byteAggregator.writeShort(assignedNumber);
        }
    }

    /**
     * Parses three types of route distinguisher from given ByteBuf.
     *
     * @param buffer
     * @return RouteDistinguisher
     */
    public static RouteDistinguisher parseRouteDistinguisher(final ByteBuf buffer) {
        final int type = buffer.readUnsignedShort();
        final StringBuilder routeDistiguisher = new StringBuilder();
        switch (type) {
        case IPV4_TYPE:
            routeDistiguisher.append(Ipv4Util.addressForByteBuf(buffer).getValue());
            routeDistiguisher.append(SEPARATOR);
            routeDistiguisher.append(buffer.readUnsignedShort());
            return new RouteDistinguisher(new RdIpv4(routeDistiguisher.toString()));
        case AS_4BYTE_TYPE:
            routeDistiguisher.append(buffer.readUnsignedInt());
            routeDistiguisher.append(SEPARATOR);
            routeDistiguisher.append(buffer.readUnsignedShort());
            return new RouteDistinguisher(new RdAs(routeDistiguisher.toString()));
        default:
            break;
        }
        return null;
    }
}
