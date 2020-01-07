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
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.OriginatorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.OriginatorIdBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OriginatorIdAttributeParser extends AbstractAttributeParser implements AttributeSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(OriginatorIdAttributeParser.class);

    public static final int TYPE = 9;

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint)
                    throws BGPDocumentedException, BGPTreatAsWithdrawException {
        if (errorHandling == RevisedErrorHandling.EXTERNAL) {
            // RFC7606 section 7.9
            LOG.debug("Discarded ORIGINATOR_ID attribute from external peer");
            return;
        }

        final int readable = buffer.readableBytes();
        if (readable != Ipv4Util.IP4_LENGTH) {
            throw errorHandling.reportError(BGPError.ATTR_LENGTH_ERROR,
                "Length of byte array for ORIGINATOR_ID should be 4, but is %s", readable);
        }
        builder.setOriginatorId(new OriginatorIdBuilder().setOriginator(Ipv4Util.addressForByteBuf(buffer)).build());
    }

    @Override
    public void serializeAttribute(final Attributes attribute, final ByteBuf byteAggregator) {
        final OriginatorId originator = attribute.getOriginatorId();
        if (originator != null) {
            final Ipv4AddressNoZone address = originator.getOriginator();
            if (address != null) {
                AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE,
                    Unpooled.wrappedBuffer(Ipv4Util.bytesForAddress(address)), byteAggregator);
            }
        }
    }
}
