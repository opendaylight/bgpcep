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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RdAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RdIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RdTwoOctetAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.RouteDistinguisherBuilder;
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
    public static final int RD_LENGTH = 8;
    private static final Logger LOG = LoggerFactory.getLogger(RouteDistinguisherUtil.class);
    private static final String SEPARATOR = ":";

    private RouteDistinguisherUtil() {
        // Hidden on purpose
    }

    /**
     * Serializes route distinguisher according to type and writes into ByteBuf.
     */
    public static void serializeRouteDistinquisher(final RouteDistinguisher distinguisher,
            final ByteBuf byteAggregator) {
        requireNonNull(distinguisher);
        Preconditions.checkState(byteAggregator != null && byteAggregator.isWritable(RD_LENGTH),
                "Cannot write Route Distinguisher to provided buffer.");
        if (distinguisher.getRdTwoOctetAs() != null) {
            byteAggregator.writeShort(RDType.AS_2BYTE.value);
            final String[] values = distinguisher.getRdTwoOctetAs().getValue().split(SEPARATOR);
            ByteBufWriteUtil.writeUnsignedShort(Integer.parseInt(values[1]), byteAggregator);
            final long assignedNumber = Integer.parseUnsignedInt(values[2]);
            ByteBufWriteUtil.writeUnsignedInt(assignedNumber, byteAggregator);
        } else if (distinguisher.getRdAs() != null) {
            byteAggregator.writeShort(RDType.AS_4BYTE.value);
            final String[] values = distinguisher.getRdAs().getValue().split(SEPARATOR);
            final long admin = Integer.parseUnsignedInt(values[0]);
            ByteBufWriteUtil.writeUnsignedInt(admin, byteAggregator);
            ByteBufWriteUtil.writeUnsignedShort(Integer.parseInt(values[1]), byteAggregator);
        } else if (distinguisher.getRdIpv4() != null) {
            final String[] values = distinguisher.getRdIpv4().getValue().split(SEPARATOR);
            final Ipv4Address ip = new Ipv4Address(values[0]);
            byteAggregator.writeShort(RDType.IPV4.value);
            ByteBufWriteUtil.writeIpv4Address(ip, byteAggregator);
            ByteBufWriteUtil.writeUnsignedShort(Integer.parseInt(values[1]), byteAggregator);
        } else {
            LOG.warn("Unable to serialize Route Distinguisher. Invalid RD value found. RD={}", distinguisher);
        }
    }

    /**
     * Parses three types of route distinguisher from given ByteBuf.
     */
    public static RouteDistinguisher parseRouteDistinguisher(final ByteBuf buffer) {
        Preconditions.checkState(buffer != null && buffer.isReadable(RD_LENGTH),
                "Cannot read Route Distinguisher from provided buffer.");
        final int type = buffer.readUnsignedShort();
        final RDType rdType = RDType.valueOf(type);
        switch (rdType) {
            case AS_2BYTE:
                return new RouteDistinguisher(new RdTwoOctetAs(new StringBuilder()
                    .append(type)
                    .append(SEPARATOR)
                    .append(buffer.readUnsignedShort())
                    .append(SEPARATOR)
                    .append(buffer.readUnsignedInt())
                    .toString()));
            case IPV4:

                return new RouteDistinguisher(new RdIpv4(new StringBuilder()
                    .append(Ipv4Util.addressForByteBuf(buffer).getValue())
                    .append(SEPARATOR)
                    .append(buffer.readUnsignedShort())
                    .toString()));
            case AS_4BYTE:
                return new RouteDistinguisher(new RdAs(new StringBuilder()
                    .append(buffer.readUnsignedInt())
                    .append(SEPARATOR)
                    .append(buffer.readUnsignedShort())
                    .toString()));
            default:
                // now that this RD type is not supported, we want to read the remain 6 bytes
                // in order to get the byte index correct
                final StringBuilder routeDistiguisher = new StringBuilder();
                for (int i = 0; i < 6; i++) {
                    routeDistiguisher.append("0x").append(Integer.toHexString(buffer.readByte() & 0xFF)).append(' ');
                }
                LOG.debug("Invalid Route Distinguisher: type={}, rawRouteDistinguisherValue={}", type,
                    routeDistiguisher);
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

    public static RouteDistinguisher extractRouteDistinguisher(final DataContainerNode<?> route,
            final NodeIdentifier rdNid) {
        final NormalizedNode<?, ?> rdNode = NormalizedNodes.findNode(route, rdNid).orElse(null);
        if (rdNode != null) {
            return parseRouteDistinguisher(rdNode.getValue());
        }
        return null;
    }

    private enum RDType {
        AS_2BYTE(0),
        IPV4(1),
        AS_4BYTE(2),
        INVALID(-1);

        public final int value;

        RDType(int val) {
            this.value = val;
        }

        static RDType valueOf(final int value) {
            for (RDType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return INVALID;
        }
    }
}
