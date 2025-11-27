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
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;

/**
 * Parser for {@link
 *     org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930
 *     .association.object.AssociationGroup
 * } with IPv4 address.
 * @see <a href="https://tools.ietf.org/html/rfc8697#section-6.1">Association Object Definition</a>
 */
public class PCEPAssociationIPv4ObjectParser extends AbstractAssociationGroupParser {
    private static final int IPV4_TYPE = 1;

    public PCEPAssociationIPv4ObjectParser() {
        super(IPV4_TYPE);
    }

    @Override
    public IpAddressNoZone parseAssociationSource(final ByteBuf buffer) {
        checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        if (buffer.readableBytes() < Ipv4Util.IP4_LENGTH) {
            return null;
        }
        return new IpAddressNoZone(Ipv4Util.addressForByteBuf(buffer));
    }

    @Override
    public boolean serializeAssociationSource(final IpAddressNoZone source, final ByteBuf buffer) {
        if (source.getIpv4AddressNoZone() == null) {
            return false;
        }
        Ipv4Util.writeIpv4Address(source.getIpv4AddressNoZone(), buffer);
        return true;
    }
}
