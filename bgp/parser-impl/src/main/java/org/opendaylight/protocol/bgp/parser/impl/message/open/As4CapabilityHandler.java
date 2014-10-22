/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.As4BytesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.As4BytesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.as4.bytes._case.As4BytesCapabilityBuilder;

public final class As4CapabilityHandler implements CapabilityParser, CapabilitySerializer {
    public static final int CODE = 65;

    @Override
    public CParameters parseCapability(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        return new As4BytesCaseBuilder().setAs4BytesCapability(
            new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(buffer.readUnsignedInt())).build()).build();
    }

    @Override
    public void serializeCapability(final CParameters capability, final ByteBuf bytes) {
        Preconditions.checkArgument(capability instanceof As4BytesCase);
        CapabilityUtil.formatCapability(CODE, putAS4BytesParameterValue((As4BytesCase) capability),bytes);
    }

    private static ByteBuf putAS4BytesParameterValue(final As4BytesCase param) {
        return Unpooled.copyInt(param.getAs4BytesCapability().getAsNumber().getValue().intValue());
    }
}