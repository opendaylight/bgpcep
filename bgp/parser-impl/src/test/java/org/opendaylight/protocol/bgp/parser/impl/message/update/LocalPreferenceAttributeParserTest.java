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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.LocalPrefBuilder;

public class LocalPreferenceAttributeParserTest {
    private static final byte[] ATTRIBUTE_BYTES = {
        (byte) 0x40, (byte) 0x05, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01
    };

    private static final Attributes RESULT = new AttributesBuilder()
            .setLocalPref(new LocalPrefBuilder()
                    .setPref(1L)
                    .build())
            .build();

    @Test
    public void testAttributeParser() throws BGPParsingException, BGPDocumentedException {
        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(RESULT, actual);
        assertArrayEquals(ATTRIBUTE_BYTES, ByteArray.getAllBytes(actual));

        final Attributes attributeOut = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance()
                .getAttributeRegistry().parseAttributes(actual, null).getAttributes();
        assertEquals(RESULT.getLocalPref(), attributeOut.getLocalPref());
    }

    @Test
    public void testParseEmptyAttribute() {
        final ByteBuf actual = Unpooled.buffer();
        ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry()
                .serializeAttribute(new AttributesBuilder()
                        .setLocalPref(new LocalPrefBuilder().build())
                        .build(), actual);
        assertEquals(Unpooled.buffer(), actual);
    }
}
