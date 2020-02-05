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
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;

public class OriginAttributeParserTest {

    private static final byte[] IGP_ATTRIBUTE_BYTES = {(byte) 0x40, (byte) 0x01, (byte) 0x01, (byte) 0x00};
    private static final byte[] EGP_ATTRIBUTE_BYTES = {(byte) 0x40, (byte) 0x01, (byte) 0x01, (byte) 0x01};
    private static final byte[] INCOMPLETE_ATTRIBUTE_BYTES = {(byte) 0x40, (byte) 0x01, (byte) 0x01, (byte) 0x02};

    private static final Attributes IGP_RESULT = new AttributesBuilder()
            .setOrigin(new OriginBuilder()
                    .setValue(BgpOrigin.Igp)
                    .build())
            .build();
    private static final Attributes EGP_RESULT = new AttributesBuilder()
            .setOrigin(new OriginBuilder()
                    .setValue(BgpOrigin.Egp)
                    .build())
            .build();
    private static final Attributes INCOMPLETE_RESULT = new AttributesBuilder()
            .setOrigin(new OriginBuilder()
                    .setValue(BgpOrigin.Incomplete)
                    .build())
            .build();

    @Test
    public void testIGPAttributeParser() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(IGP_RESULT, actual);
        assertArrayEquals(IGP_ATTRIBUTE_BYTES, ByteArray.getAllBytes(actual));

        final Attributes attributeOut = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
                .getAttributeRegistry().parseAttributes(actual, null).getAttributes();
        assertEquals(IGP_RESULT.getOrigin(), attributeOut.getOrigin());
    }

    @Test
    public void testEGPAttributeParser() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(EGP_RESULT, actual);
        assertArrayEquals(EGP_ATTRIBUTE_BYTES, ByteArray.getAllBytes(actual));

        final Attributes attributeOut = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
                .getAttributeRegistry().parseAttributes(actual, null).getAttributes();
        assertEquals(EGP_RESULT.getOrigin(), attributeOut.getOrigin());
    }

    @Test
    public void testIncompleeteAttributeParser() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(INCOMPLETE_RESULT, actual);
        assertArrayEquals(INCOMPLETE_ATTRIBUTE_BYTES, ByteArray.getAllBytes(actual));

        final Attributes attributeOut = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
                .getAttributeRegistry().parseAttributes(actual, null).getAttributes();
        assertEquals(INCOMPLETE_RESULT.getOrigin(), attributeOut.getOrigin());
    }

    @Test (expected = NullPointerException.class)
    public void testParseEmptyAttribute() {
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(new AttributesBuilder()
                        .setOrigin(new OriginBuilder().build())
                        .build(), Unpooled.buffer());
    }
}
