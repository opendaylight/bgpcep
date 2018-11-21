/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static org.opendaylight.protocol.util.Ipv4Util.IP4_LENGTH;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.bgp.concepts.NextHopUtil;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.next.hop.CNextHop;

public final class NextHopAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 3;

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException {
        final int readable = buffer.readableBytes();
        if (readable != IP4_LENGTH) {
            if (RevisedErrorHandling.from(constraint) != RevisedErrorHandling.NONE) {
                // FIXME: BGPCEP-359: treat-as-withdraw
            }
            throw new BGPDocumentedException("NEXT_HOP attribute is expected to have length " + IP4_LENGTH + " but has "
                + readable, BGPError.ATTR_LENGTH_ERROR);
        }
        builder.setCNextHop(NextHopUtil.parseNextHop(buffer));
    }

    @Override
    public void serializeAttribute(final Attributes attribute, final ByteBuf byteAggregator) {
        final CNextHop cNextHop = attribute.getCNextHop();
        if (cNextHop == null) {
            return;
        }
        final ByteBuf nextHopBuffer = Unpooled.buffer();
        NextHopUtil.serializeNextHop(cNextHop, nextHopBuffer);
        AttributeUtil.formatAttribute(AttributeUtil.TRANSITIVE, TYPE, nextHopBuffer, byteAggregator);
    }
}
