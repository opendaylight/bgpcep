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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for IpAddress models type(like Originator Route Ip) serialization and parsing.
 *
 * @author Claudio D. Gasparini
 */
public final class IpAddressUtil {
    private static final Logger LOG = LoggerFactory.getLogger(IpAddressUtil.class);
    
    private IpAddressUtil() {
        // Hidden on purpose
    }

    /**
     * Returns IpAddress from byte array containing ipAddress lenght + ipAddress.
     *
     * @param buffer containing ip address
     * @return IpAddressNoZone
     */
    public static @NonNull IpAddressNoZone addressForByteBuf(final ByteBuf buffer) {
        final int ipLength = buffer.readUnsignedByte();
        if (ipLength == Ipv6Util.IPV6_BITS_LENGTH) {
            return new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer));
        } else if (ipLength == Ipv4Util.IP4_BITS_LENGTH) {
            return new IpAddressNoZone(Ipv4Util.addressForByteBuf(buffer));
        }
        throw new IllegalStateException("Unexpected size");
    }

    /**
     * Returns IpAddress from byte array containing ipAddress based on ByteArray length.
     *
     * @param buffer containing ip address
     * @return IpAddressNoZone
     */
    public static @NonNull IpAddressNoZone addressForByteBufWOLength(final ByteBuf buffer) {
        final int ipLength = buffer.readableBytes();
        if (ipLength == Ipv6Util.IPV6_LENGTH) {
            return new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer));
        } else if (ipLength == Ipv4Util.IP4_LENGTH) {
            return new IpAddressNoZone(Ipv4Util.addressForByteBuf(buffer));
        }
        throw new IllegalStateException("Unexpected size");
    }

    /**
     * Returns byte array containing IpAddress length and IpAddress.
     *
     * @param address containing ipv4 or ipv6 address
     * @return byte array
     */
    public static @NonNull ByteBuf bytesFor(final IpAddressNoZone address) {
        final ByteBuf body = Unpooled.buffer();
        if (address.getIpv4AddressNoZone() != null) {
            body.writeByte(Ipv4Util.IP4_BITS_LENGTH);
            body.writeBytes(Ipv4Util.bytesForAddress(address.getIpv4AddressNoZone()));
        } else if (address.getIpv6AddressNoZone() != null) {
            body.writeByte(Ipv6Util.IPV6_BITS_LENGTH);
            body.writeBytes(Ipv6Util.bytesForAddress(address.getIpv6AddressNoZone()));
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
    public static @NonNull ByteBuf bytesWOLengthFor(final IpAddressNoZone address) {
        final ByteBuf body = Unpooled.buffer();
        if (address.getIpv4AddressNoZone() != null) {
            body.writeBytes(Ipv4Util.bytesForAddress(address.getIpv4AddressNoZone()));
        } else if (address.getIpv6AddressNoZone() != null) {
            body.writeBytes(Ipv6Util.bytesForAddress(address.getIpv6AddressNoZone()));
        } else {
            body.writeByte(0);
        }
        return body;
    }

    public static IpAddressNoZone extractIpAddress(final DataContainerNode route, final NodeIdentifier rdNid) {
        final var rdNode = NormalizedNodes.findNode(route, rdNid).orElse(null);
        if (rdNode == null) {
            return null;
        }
        final var body = rdNode.body();
        if (!(body instanceof String str)) { 
            throw new IllegalArgumentException("Impected body " + body);
        }
        try {
            return new IpAddressNoZone(new Ipv4AddressNoZone(str));
        } catch (IllegalArgumentException e) {
            LOG.debug("Failed to interpret {} as an Ipv4AddressNoZone", str);
        }
        return new IpAddressNoZone(new Ipv6AddressNoZone(str));
    }
}
