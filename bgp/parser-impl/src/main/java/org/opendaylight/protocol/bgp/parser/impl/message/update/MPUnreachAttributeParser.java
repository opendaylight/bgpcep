/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class MPUnreachAttributeParser implements AttributeParser, AttributeSerializer {
    public static final int TYPE = 15;

    private final NlriRegistry reg;

    public MPUnreachAttributeParser(final NlriRegistry reg) {
        this.reg = requireNonNull(reg);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) throws BGPDocumentedException {
        parseAttribute(buffer, builder, null);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder, final PeerSpecificParserConstraint constraint)
            throws BGPDocumentedException {
        try {
            final MpUnreachNlri mpUnreachNlri = this.reg.parseMpUnreach(buffer, constraint);
            final Attributes2 a = new Attributes2Builder().setMpUnreachNlri(mpUnreachNlri).build();
            builder.addAugmentation(Attributes2.class, a);
        } catch (final BGPParsingException e) {
            throw new BGPDocumentedException("Could not parse MP_UNREACH_NLRI", BGPError.OPT_ATTR_ERROR, e);
        }
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes2 pathAttributes2 = pathAttributes.getAugmentation(Attributes2.class);
        if (pathAttributes2 == null) {
            return;
        }
        final MpUnreachNlri mpUnreachNlri = pathAttributes2.getMpUnreachNlri();
        final ByteBuf unreachBuffer = Unpooled.buffer();
        this.reg.serializeMpUnReach(mpUnreachNlri, unreachBuffer);
        for (final NlriSerializer nlriSerializer : this.reg.getSerializers()){
            nlriSerializer.serializeAttribute(attribute,unreachBuffer);
        }
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE, unreachBuffer, byteAggregator);
    }
}
