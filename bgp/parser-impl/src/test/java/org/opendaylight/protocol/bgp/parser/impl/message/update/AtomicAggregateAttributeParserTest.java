/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ServiceLoader;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AtomicAggregateBuilder;

public class AtomicAggregateAttributeParserTest {

    private static final byte[] ATTRIBUTE_BYTES = {(byte) 0x40, (byte) 0x06, (byte) 0x00};

    private static final Attributes RESULT = new AttributesBuilder()
            .setAtomicAggregate(new AtomicAggregateBuilder().build())
            .build();

    private final AttributeRegistry registry = ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst()
        .orElseThrow().getAttributeRegistry();

    @Test
    public void testAttributeParser() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf actual = Unpooled.buffer();
        registry.serializeAttribute(RESULT, actual);
        assertArrayEquals(ATTRIBUTE_BYTES, ByteArray.getAllBytes(actual));

        final Attributes attributeOut = registry.parseAttributes(actual, null).getAttributes();
        assertEquals(RESULT.getAtomicAggregate(), attributeOut.getAtomicAggregate());
    }
}
