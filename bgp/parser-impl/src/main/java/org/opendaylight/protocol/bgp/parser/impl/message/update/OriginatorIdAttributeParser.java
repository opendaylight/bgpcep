/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginatorIdBuilder;

public final class OriginatorIdAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 9;

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException {
        final RevisedErrorHandling revised = RevisedErrorHandling.from(constraint);
        if (revised == RevisedErrorHandling.EXTERNAL) {
            // RFC7606: attribute discard
            return;
        }

        final int readable = buffer.readableBytes();
        if (readable != Ipv4Util.IP4_LENGTH) {
            if (revised == RevisedErrorHandling.INTERNAL) {
                // FIXME: BGPCEP-359: treat-as-withdraw
            }
            throw new BGPDocumentedException("Length of byte array for ORIGINATOR_ID should be 4, but is " + readable,
                BGPError.ATTR_LENGTH_ERROR);
        }
        builder.setOriginatorId(new OriginatorIdBuilder().setOriginator(Ipv4Util.addressForByteBuf(buffer)).build());
    }

    @Override
    public void serializeAttribute(final Attributes attribute, final ByteBuf byteAggregator) {
        final OriginatorId originator = attribute.getOriginatorId();
        if (originator == null) {
            return;
        }
        final ByteBuf originatorIdBuf = Unpooled.buffer();
        originatorIdBuf.writeBytes(Ipv4Util.bytesForAddress(originator.getOriginator()));
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE, originatorIdBuf, byteAggregator);
    }
}
