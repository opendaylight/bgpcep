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

import org.opendaylight.protocol.bgp.parser.AttributeFlags;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.PathAttributes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.path.attributes.MpReachNlri;
import org.opendaylight.yangtools.yang.binding.DataObject;


public final class MPReachAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 14;
    public static final int ATTR_LENGTH = 2;
    public static final int MAX_ATTR_LENGTH_FOR_SINGLE_BYTE = 127;
    private final NlriRegistry reg;

    public MPReachAttributeParser(final NlriRegistry reg) {
        this.reg = Preconditions.checkNotNull(reg);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) throws BGPDocumentedException {
        try {
            final PathAttributes1 a = new PathAttributes1Builder().setMpReachNlri(this.reg.parseMpReach(buffer)).build();
            builder.addAugmentation(PathAttributes1.class, a);
        } catch (final BGPParsingException e) {
            throw new BGPDocumentedException("Could not parse MP_REACH_NLRI", BGPError.OPT_ATTR_ERROR, e);
        }
    }

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        PathAttributes pathAttributes = (PathAttributes) attribute;
        PathAttributes1 pathAttributes1 = pathAttributes.getAugmentation(PathAttributes1.class);
        if (pathAttributes1 == null) {
            return;
        }
        MpReachNlri mpReachNlri = pathAttributes1.getMpReachNlri();
        ByteBuf reachBuffer = Unpooled.buffer();
        this.reg.serializeMpReach(mpReachNlri, reachBuffer);

        for (NlriSerializer nlriSerializer : this.reg.getSerializers()) {
            nlriSerializer.serializeAttribute(attribute, reachBuffer);
        }

        if (reachBuffer.writerIndex() > MAX_ATTR_LENGTH_FOR_SINGLE_BYTE) {
            byteAggregator.writeByte(AttributeFlags.OPTIONAL | AttributeFlags.EXTENDED);
            byteAggregator.writeByte(TYPE);
            byteAggregator.writeShort(reachBuffer.writerIndex());
        } else {
            byteAggregator.writeByte(AttributeFlags.OPTIONAL);
            byteAggregator.writeByte(TYPE);
            byteAggregator.writeByte(reachBuffer.writerIndex());
        }
        byteAggregator.writeBytes(reachBuffer);
    }
}
