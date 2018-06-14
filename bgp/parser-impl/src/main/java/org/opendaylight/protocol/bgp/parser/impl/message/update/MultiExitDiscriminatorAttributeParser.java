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
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.MultiExitDiscBuilder;

public final class MultiExitDiscriminatorAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 4;

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) {
        builder.setMultiExitDisc(new MultiExitDiscBuilder().setMed(buffer.readUnsignedInt()).build());
    }

    @Override
    public void serializeAttribute(final Attributes attribute, final ByteBuf byteAggregator) {
        final MultiExitDisc multiExitDisc = attribute.getMultiExitDisc();
        if (multiExitDisc == null) {
            return;
        }
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, TYPE,
                Unpooled.copyInt(multiExitDisc.getMed().intValue()), byteAggregator);
    }
}
