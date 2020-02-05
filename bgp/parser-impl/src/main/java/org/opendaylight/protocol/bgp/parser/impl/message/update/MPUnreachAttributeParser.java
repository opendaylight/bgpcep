/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.Attributes2Builder;

public final class MPUnreachAttributeParser extends ReachAttributeParser {
    public static final int TYPE = 15;

    private final NlriRegistry reg;

    public MPUnreachAttributeParser(final NlriRegistry reg) {
        this.reg = requireNonNull(reg);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint)
                    throws BGPDocumentedException {
        try {
            builder.addAugmentation(Attributes2.class, new Attributes2Builder()
                .setMpUnreachNlri(reg.parseMpUnreach(buffer, constraint))
                .build());
        } catch (final BGPParsingException e) {
            throw new BGPDocumentedException("Could not parse MP_UNREACH_NLRI", BGPError.OPT_ATTR_ERROR, e);
        }
    }

    @Override
    public void serializeAttribute(final Attributes attribute, final ByteBuf byteAggregator) {
        final Attributes2 pathAttributes2 = attribute.augmentation(Attributes2.class);
        if (pathAttributes2 != null) {
            final ByteBuf unreachBuffer = Unpooled.buffer();
            reg.serializeMpUnReach(pathAttributes2.getMpUnreachNlri(), unreachBuffer);
            for (final NlriSerializer nlriSerializer : this.reg.getSerializers()) {
                nlriSerializer.serializeAttribute(attribute, unreachBuffer);
            }
            AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE, unreachBuffer, byteAggregator);
        }
    }
}
