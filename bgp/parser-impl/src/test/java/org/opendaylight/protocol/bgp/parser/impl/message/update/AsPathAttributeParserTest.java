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

import com.google.common.collect.ImmutableSet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.as.path.SegmentsBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class AsPathAttributeParserTest {
    private static final byte[] ATTRIBUTE_BYTES = {
        (byte) 0x40, (byte) 0x02, (byte) 0x14,
        (byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04
    };
    private static final byte[] EMPTY_ATTRIBUTE_BYTES = { (byte) 0x40, (byte) 0x02, (byte) 0x00 };

    private final AttributeRegistry registry = ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst()
        .orElseThrow().getAttributeRegistry();

    @Test
    public void testAttributeParser() throws BGPParsingException, BGPDocumentedException {
        final Attributes attr = new AttributesBuilder()
            .setAsPath(new AsPathBuilder()
                .setSegments(List.of(
                    new SegmentsBuilder()
                        // For testing purposes we need a predictable iteration order
                        .setAsSet(ImmutableSet.of(new AsNumber(Uint32.ONE), new AsNumber(Uint32.TWO)))
                        .build(),
                    new SegmentsBuilder()
                        .setAsSequence(List.of(new AsNumber(Uint32.valueOf(3)), new AsNumber(Uint32.valueOf(4))))
                        .build()))
                .build())
            .build();

        final ByteBuf actual = Unpooled.buffer();
        registry.serializeAttribute(attr, actual);
        assertArrayEquals(ATTRIBUTE_BYTES, ByteArray.getAllBytes(actual));

        final Attributes attributeOut = registry.parseAttributes(actual, null).getAttributes();
        assertEquals(attr.getAsPath(), attributeOut.getAsPath());
    }

    @Test
    public void testParseEmptyAttribute() {
        final ByteBuf actual = Unpooled.buffer();
        registry.serializeAttribute(new AttributesBuilder().setAsPath(new AsPathBuilder().build()).build(), actual);
        assertEquals(Unpooled.buffer().writeBytes(EMPTY_ATTRIBUTE_BYTES), actual);
    }
}
