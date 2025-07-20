/*
 * Copyright (c) 2025 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.object;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;

/**
 * Parser for {@link
 *     org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930
 *     .association.object.AssociationGroup
 * } with IPv6 address.
 * @see <a href="https://tools.ietf.org/html/rfc8697#section-6.1">Association Object Definition</a>
 */
public class PCEPAssociationIPv6ObjectParser extends AbstractAssociationGroupParser {
    private static final int IPV6_TYPE = 2;

    public PCEPAssociationIPv6ObjectParser() {
        super(IPV6_TYPE);
    }

    @Override
    public IpAddressNoZone parseAssociationSource(final ByteBuf buffer) {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() < Ipv6Util.IPV6_LENGTH) {
            return null;
        }
        return new IpAddressNoZone(Ipv6Util.addressForByteBuf(buffer));
    }

    @Override
    public boolean serializeAssociationSource(final IpAddressNoZone source, final ByteBuf buffer) {
        if (source.getIpv6AddressNoZone() == null) {
            return false;
        }
        Ipv6Util.writeIpv6Address(source.getIpv6AddressNoZone(), buffer);
        return true;
    }
}
