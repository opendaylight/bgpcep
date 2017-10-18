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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yangtools.yang.binding.DataObject;


public final class MPReachAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 14;

    private final NlriRegistry reg;

    public MPReachAttributeParser(final NlriRegistry reg) {
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
            final MpReachNlri mpReachNlri = this.reg.parseMpReach(buffer, constraint);
            final Attributes1 a = new Attributes1Builder().setMpReachNlri(mpReachNlri).build();
            builder.addAugmentation(Attributes1.class, a);
        } catch (final BGPParsingException e) {
            throw new BGPDocumentedException("Could not parse MP_REACH_NLRI", BGPError.OPT_ATTR_ERROR, e);
        }
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final Attributes pathAttributes = (Attributes) attribute;
        final Attributes1 pathAttributes1 = pathAttributes.getAugmentation(Attributes1.class);
        if (pathAttributes1 == null) {
            return;
        }
        final MpReachNlri mpReachNlri = pathAttributes1.getMpReachNlri();
        final ByteBuf reachBuffer = Unpooled.buffer();
        this.reg.serializeMpReach(mpReachNlri, reachBuffer);

        for (final NlriSerializer nlriSerializer : this.reg.getSerializers()) {
            nlriSerializer.serializeAttribute(attribute, reachBuffer);
        }
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE, reachBuffer, byteAggregator);
    }
}
