/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginatorIdBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class OriginatorIdAttributeParser implements AttributeParser,AttributeSerializer {

    public static final int TYPE = 9;

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) {
        Preconditions.checkArgument(buffer.readableBytes() == Ipv4Util.IP4_LENGTH, "Length of byte array for ORIGINATOR_ID should be %s, but is %s", Ipv4Util.IP4_LENGTH, buffer.readableBytes());
        builder.setOriginatorId(new OriginatorIdBuilder().setOriginator(Ipv4Util.addressForByteBuf(buffer)).build());
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final OriginatorId originator = ((Attributes) attribute).getOriginatorId();
        if (originator == null) {
            return;
        }
        final ByteBuf originatorIdBuf = Unpooled.buffer();
        originatorIdBuf.writeBytes(Ipv4Util.bytesForAddress(originator.getOriginator()));
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE, originatorIdBuf, byteAggregator);
    }
}
