/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RdAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for of RouteDistinguisher serialization and parsing.
 * https://tools.ietf.org/html/rfc4364#section-4.2
 */
public final class RouteDistinguisherUtil {
    private static final Logger LOG = LoggerFactory.getLogger(RouteDistinguisherUtil.class);

    private enum RD_TYPE {
        AS_2BYTE(0),
        IPV4(1),
        AS_4BYTE(2),
        INVALID(-1);

        public final int value;

        RD_TYPE(int val) {
            value = val;
        }

        public static RD_TYPE valueOf(final int value) {
            for (RD_TYPE type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return INVALID;
        }
    }

    private static final String SEPARATOR = ":";
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
            final String[] values = distinquisher.getRdAs().getValue().split(SEPARATOR);
            byteAggregator.writeShort(RD_TYPE.AS_4BYTE.value);
            final long admin = Integer.parseUnsignedInt(values[0]);
            ByteBufWriteUtil.writeUnsignedInt(admin, byteAggregator);
            ByteBufWriteUtil.writeUnsignedShort(Integer.parseInt(values[1]), byteAggregator);
        } else if (distinquisher.getRdIpv4() != null) {
            final String[] values = distinquisher.getRdIpv4().getValue().split(SEPARATOR);
            final Ipv4Address ip = new Ipv4Address(values[0]);
            byteAggregator.writeShort(RD_TYPE.IPV4.value);
            ByteBufWriteUtil.writeIpv4Address(ip, byteAggregator);
            ByteBufWriteUtil.writeUnsignedShort(Integer.parseInt(values[1]), byteAggregator);
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
        final RD_TYPE rdType = RD_TYPE.valueOf(type);
        final StringBuilder routeDistiguisher = new StringBuilder();
        switch (rdType) {
        case IPV4:
            routeDistiguisher.append(Ipv4Util.addressForByteBuf(buffer).getValue());
            routeDistiguisher.append(SEPARATOR);
            routeDistiguisher.append(buffer.readUnsignedShort());
            return new RouteDistinguisher(new RdIpv4(routeDistiguisher.toString()));
        case AS_4BYTE:
            routeDistiguisher.append(buffer.readUnsignedInt());
            routeDistiguisher.append(SEPARATOR);
            routeDistiguisher.append(buffer.readUnsignedShort());
            return new RouteDistinguisher(new RdAs(routeDistiguisher.toString()));
        default:
            // now that this RD type is not supported, we want to read the remain 6 bytes
            // in order to get the byte index correct
            for (int i = 0; i < 6; i++) {
                routeDistiguisher.append("0x").append(Integer.toHexString(buffer.readByte() & 0xFF)).append(" ");
            }
            LOG.error("Route Distinguisher type not supported: type={}, rawValue={}", type, routeDistiguisher.toString());
            break;
        }
        return null;
    }
}
