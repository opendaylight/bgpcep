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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.as.path.SegmentsBuilder;

public class AsPathAttributeParserTest {

    private static final byte[] ATTRIBUTE_BYTES = {(byte) 0x40, (byte) 0x02, (byte) 0x14,
            (byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x02,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04};
    private static final byte[] EMPTY_ATTRIBUTE_BYTES = {(byte) 0x40, (byte) 0x02, (byte) 0x00};

    @Test
    public void testAttributeParser() throws BGPParsingException, BGPDocumentedException {
        final List<Segments> segments = new ArrayList<>();
        segments.add(new SegmentsBuilder()
                .setAsSet(Arrays.asList(new AsNumber(1L), new AsNumber(2L)))
                .build());
        segments.add(new SegmentsBuilder()
                .setAsSequence(Arrays.asList(new AsNumber(3L), new AsNumber(4L)))
                .build());
        final Attributes attr = new AttributesBuilder().setAsPath(new AsPathBuilder()
                    .setSegments(segments)
                    .build())
                .build();

        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(attr, actual);
        assertArrayEquals(ATTRIBUTE_BYTES, ByteArray.getAllBytes(actual));

        final Attributes attributeOut = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
                .getAttributeRegistry().parseAttributes(actual, null).getAttributes();
        assertEquals(attr.getAsPath(), attributeOut.getAsPath());
    }

    @Test
    public void testParseEmptyAttribute() {
        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(new AttributesBuilder()
                        .setAsPath(new AsPathBuilder()
                                .build())
                        .build(), actual);
        assertEquals(Unpooled.buffer().writeBytes(EMPTY_ATTRIBUTE_BYTES), actual);
    }
}
