/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class OriginAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 1;

    private static final Origin IGP = new OriginBuilder().setValue(BgpOrigin.Igp).build();
    private static final Origin EGP = new OriginBuilder().setValue(BgpOrigin.Egp).build();
    private static final Origin INC = new OriginBuilder().setValue(BgpOrigin.Incomplete).build();

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) throws BGPDocumentedException {
        final byte rawOrigin = buffer.readByte();
        final BgpOrigin borigin = BgpOrigin.forValue(UnsignedBytes.toInt(rawOrigin));
        if (borigin == null) {
            throw new BGPDocumentedException("Unknown Origin type.", BGPError.ORIGIN_ATTR_NOT_VALID, new byte[] { (byte) 0x01, (byte) 0x01, rawOrigin} );
        }
        switch (borigin) {
        case Egp:
            builder.setOrigin(EGP);
            return;
        case Igp:
            builder.setOrigin(IGP);
            return;
        case Incomplete:
            builder.setOrigin(INC);
            return;
        default:
            return;
        }
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Origin origin = ((Attributes) attribute).getOrigin();
        if (origin == null) {
            return;
        }
        AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE, TYPE, Unpooled.wrappedBuffer(new byte[] {UnsignedBytes.checkedCast(origin.getValue().getIntValue())}), byteAggregator);
    }
}
