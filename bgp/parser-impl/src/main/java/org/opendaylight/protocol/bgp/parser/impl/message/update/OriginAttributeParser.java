/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class OriginAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 1;

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) throws BGPDocumentedException {
        byte rawOrigin = buffer.readByte();
        final BgpOrigin borigin = BgpOrigin.forValue(UnsignedBytes.toInt(rawOrigin));
        if (borigin == null) {
            throw new BGPDocumentedException("Unknown Origin type.", BGPError.ORIGIN_ATTR_NOT_VALID, new byte[] { (byte) 0x01, (byte) 0x01, rawOrigin} );
        }
        builder.setOrigin(new OriginBuilder().setValue(borigin).build());
    }

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        PathAttributes pathAttributes = (PathAttributes) attribute;
        Origin origin = pathAttributes.getOrigin();
        if (origin == null) {
            return;
        }
        byteAggregator.writeByte(origin.getValue().getIntValue());
    }
}
