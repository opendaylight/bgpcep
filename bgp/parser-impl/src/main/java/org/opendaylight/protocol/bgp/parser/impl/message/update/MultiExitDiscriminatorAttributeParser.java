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

import org.opendaylight.protocol.bgp.parser.AttributeFlags;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class MultiExitDiscriminatorAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 4;
    public static final int ATTR_LENGTH = 4;

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) {
        builder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(buffer.readUnsignedInt()).build());
    }

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        PathAttributes pathAttributes = (PathAttributes) attribute;
        MultiExitDisc multiExitDisc = pathAttributes.getMultiExitDisc();
        if (multiExitDisc == null) {
            return;
        }
        byteAggregator.writeByte(UnsignedBytes.checkedCast(AttributeFlags.OPTIONAL));
        byteAggregator.writeByte(UnsignedBytes.checkedCast(TYPE));
        byteAggregator.writeByte(UnsignedBytes.checkedCast(ATTR_LENGTH));
        byteAggregator.writeInt(multiExitDisc.getMed().intValue());
    }
}
