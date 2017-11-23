/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RdAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RdTwoOctetAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
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
            this.value = val;
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
     * @param distinguisher
     * @param byteAggregator
     */
    public static void serializeRouteDistinquisher(final RouteDistinguisher distinguisher, final ByteBuf byteAggregator) {
        requireNonNull(distinguisher);
        Preconditions.checkState(byteAggregator != null && byteAggregator.isWritable(RD_LENGTH), "Cannot write Route Distinguisher to provided buffer.");
        if (distinguisher.getRdTwoOctetAs() != null) {
            final String[] values = distinguisher.getRdTwoOctetAs().getValue().split(SEPARATOR);
            byteAggregator.writeShort(RD_TYPE.AS_2BYTE.value);
            ByteBufWriteUtil.writeUnsignedShort(Integer.parseInt(values[1]), byteAggregator);
            final long assignedNumber = Integer.parseUnsignedInt(values[2]);
            ByteBufWriteUtil.writeUnsignedInt(assignedNumber, byteAggregator);
        } else if (distinguisher.getRdAs() != null) {
            final String[] values = distinguisher.getRdAs().getValue().split(SEPARATOR);
            byteAggregator.writeShort(RD_TYPE.AS_4BYTE.value);
            final long admin = Integer.parseUnsignedInt(values[0]);
            ByteBufWriteUtil.writeUnsignedInt(admin, byteAggregator);
            ByteBufWriteUtil.writeUnsignedShort(Integer.parseInt(values[1]), byteAggregator);
        } else if (distinguisher.getRdIpv4() != null) {
            final String[] values = distinguisher.getRdIpv4().getValue().split(SEPARATOR);
            final Ipv4Address ip = new Ipv4Address(values[0]);
            byteAggregator.writeShort(RD_TYPE.IPV4.value);
            ByteBufWriteUtil.writeIpv4Address(ip, byteAggregator);
            ByteBufWriteUtil.writeUnsignedShort(Integer.parseInt(values[1]), byteAggregator);
        } else {
            LOG.warn("Unable to serialize Route Distinguisher. Invalid RD value found. RD={}", distinguisher);
        }
    }

    /**
     * Parses three types of route distinguisher from given ByteBuf.
     *
     * @param buffer
     * @return RouteDistinguisher
     */
    public static RouteDistinguisher parseRouteDistinguisher(final ByteBuf buffer) {
        Preconditions.checkState(buffer != null && buffer.isReadable(RD_LENGTH), "Cannot read Route Distinguisher from provided buffer.");
        final int type = buffer.readUnsignedShort();
        final RD_TYPE rdType = RD_TYPE.valueOf(type);
        final StringBuilder routeDistiguisher = new StringBuilder();
        switch (rdType) {
        case AS_2BYTE:
            routeDistiguisher.append(type);
            routeDistiguisher.append(SEPARATOR);
            routeDistiguisher.append(buffer.readUnsignedShort());
            routeDistiguisher.append(SEPARATOR);
            routeDistiguisher.append(buffer.readUnsignedInt());
            return new RouteDistinguisher(new RdTwoOctetAs(routeDistiguisher.toString()));
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
            LOG.debug("Invalid Route Distinguisher: type={}, rawRouteDistinguisherValue={}", type, routeDistiguisher.toString());
            throw new IllegalArgumentException("Invalid Route Distinguisher type " + type);
        }
    }

    public static RouteDistinguisher parseRouteDistinguisher(final String str) {
        return str == null ? null : RouteDistinguisherBuilder.getDefaultInstance(str);
    }

    public static RouteDistinguisher parseRouteDistinguisher(final Object obj) {
        if (obj instanceof String) {
            return RouteDistinguisherBuilder.getDefaultInstance((String) obj);
        } else if (obj instanceof RouteDistinguisher) {
            return (RouteDistinguisher) obj;
        } else {
            return null;
        }
    }

    public static RouteDistinguisher extractRouteDistinguisher(final DataContainerNode<?> route, final NodeIdentifier rdNid) {
        final NormalizedNode<?, ?> rdNode = NormalizedNodes.findNode(route, rdNid).orElse(null);
        if (rdNode != null) {
            return parseRouteDistinguisher(rdNode.getValue());
        }
        return null;
    }
}
