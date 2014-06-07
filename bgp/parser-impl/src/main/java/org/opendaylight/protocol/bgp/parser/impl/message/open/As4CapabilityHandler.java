/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.As4BytesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.c.parameters.as4.bytes._case.As4BytesCapabilityBuilder;

public final class As4CapabilityHandler implements CapabilityParser, CapabilitySerializer {
    public static final int CODE = 65;

    private static final int AS4_LENGTH = 4;

    @Override
    public CParameters parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        return new As4BytesCaseBuilder().setAs4BytesCapability(
                new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(buffer.readUnsignedInt())).build()).build();
    }

    @Override
    public byte[] serializeCapability(final CParameters capability) {
        return CapabilityUtil.formatCapability(CODE, putAS4BytesParameterValue((As4BytesCase) capability));
    }

    private static byte[] putAS4BytesParameterValue(final As4BytesCase param) {
        return ByteArray.longToBytes(param.getAs4BytesCapability().getAsNumber().getValue(), AS4_LENGTH);
    }
}