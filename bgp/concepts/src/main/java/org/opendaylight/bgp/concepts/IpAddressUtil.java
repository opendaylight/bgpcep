/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgp.concepts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;

/**
 * Utility class for IpAddress models type(like Originator Route Ip) serialization and parsing.
 *
 * @author Claudio D. Gasparini
 */
public final class IpAddressUtil {
    private IpAddressUtil() {
        // Hidden on purpose
    }

    /**
     * Returns IpAddress from byte array containing ipAddress lenght + ipAddress.
     *
     * @param buffer containing ip address
     * @return IpAddress
     */
    public static @NonNull IpAddress addressForByteBuf(final ByteBuf buffer) {
        final int ipLength = buffer.readUnsignedByte();
        if (ipLength == Ipv6Util.IPV6_BITS_LENGTH) {
            return new IpAddress(Ipv6Util.addressForByteBuf(buffer));
        } else if (ipLength == Ipv4Util.IP4_BITS_LENGTH) {
            return new IpAddress(Ipv4Util.addressForByteBuf(buffer));
        }
        throw new IllegalStateException("Unexpected size");
    }

    /**
     * Returns IpAddress from byte array containing ipAddress based on ByteArray length.
     *
     * @param buffer containing ip address
     * @return IpAddress
     */
    public static @NonNull IpAddress addressForByteBufWOLength(final ByteBuf buffer) {
        final int ipLength = buffer.readableBytes();
        if (ipLength == Ipv6Util.IPV6_LENGTH) {
            return new IpAddress(Ipv6Util.addressForByteBuf(buffer));
        } else if (ipLength == Ipv4Util.IP4_LENGTH) {
            return new IpAddress(Ipv4Util.addressForByteBuf(buffer));
        }
        throw new IllegalStateException("Unexpected size");
    }

    /**
     * Returns byte array containing IpAddress length and IpAddress.
     *
     * @param address containing ipv4 or ipv6 address
     * @return byte array
     */
    public static @NonNull ByteBuf bytesFor(final IpAddress address) {
        final ByteBuf body = Unpooled.buffer();
        if (address.getIpv4Address() != null) {
            body.writeByte(Ipv4Util.IP4_BITS_LENGTH);
            body.writeBytes(Ipv4Util.bytesForAddress(address.getIpv4Address()));
        } else if (address.getIpv6Address() != null) {
            body.writeByte(Ipv6Util.IPV6_BITS_LENGTH);
            body.writeBytes(Ipv6Util.bytesForAddress(address.getIpv6Address()));
        } else {
            body.writeByte(0);
        }
        return body;
    }

    /**
     * Returns byte array containing IpAddress.
     *
     * @param address containing ipv4 or ipv6 address
     * @return byte array
     */
    public static @NonNull ByteBuf bytesWOLengthFor(final IpAddress address) {
        final ByteBuf body = Unpooled.buffer();
        if (address.getIpv4Address() != null) {
            body.writeBytes(Ipv4Util.bytesForAddress(address.getIpv4Address()));
        } else if (address.getIpv6Address() != null) {
            body.writeBytes(Ipv6Util.bytesForAddress(address.getIpv6Address()));
        } else {
            body.writeByte(0);
        }
        return body;
    }

    public static IpAddress extractIpAddress(final DataContainerNode<?> route, final NodeIdentifier rdNid) {
        final NormalizedNode<?, ?> rdNode = NormalizedNodes.findNode(route, rdNid).orElse(null);
        if (rdNode != null) {
            return IpAddressBuilder.getDefaultInstance((String) rdNode.getValue());
        }
        return null;
    }
}
