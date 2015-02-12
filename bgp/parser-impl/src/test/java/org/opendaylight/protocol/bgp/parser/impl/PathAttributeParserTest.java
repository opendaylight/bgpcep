/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.AigpAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Aigp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.aigp.AigpTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;

/*
 * This class is aimed to test parsing and serializing path attributes.
 */
public class PathAttributeParserTest {

    @Test
    public void testOriginParser() {
        try {
            ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getAttributeRegistry().parseAttributes(
                    Unpooled.copiedBuffer(new byte[] { 0x40, 0x01, 0x01, 0x04 }));
            fail("This needs to fail.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Unknown Origin type.", e.getMessage());
            assertArrayEquals(new byte[] { 0x01, 0x01, 0x04 }, e.getData());
        } catch (final BGPParsingException e) {
            fail("This exception should not occur.");
        }
    }

    @Test
    public void testParsingAigpAttributeWithCorrectTLV() throws BGPDocumentedException, BGPParsingException {
        byte[] value = new byte[] { 1, 0, 11, 0, 0, 0, 0, 0, 0, 0, 8 };
        final ByteBuf buffer = Unpooled.buffer();

        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, AigpAttributeParser.TYPE, Unpooled.copiedBuffer(value), buffer);

        BGPExtensionProviderContext providerContext = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
        final PathAttributes pathAttributes = providerContext.getAttributeRegistry().parseAttributes(buffer);
        final Aigp aigp = pathAttributes.getAigp();
        final AigpTlv tlv = aigp.getAigpTlv();

        assertNotNull("Tlv should not be null.", tlv);
        assertEquals("Aigp tlv should have metric with value 8.", 8, tlv.getMetric().getValue().intValue());
    }

    @Test
    public void testParsingAigpAttributeWithUncorrectAndCorrectTLV() throws BGPDocumentedException, BGPParsingException {
        final byte[] value = new byte[] { 2, 0, 11, 1, 2, 3, 4, 5, 6, 7, 8, 1, 0, 11, 0, 0, 0, 0, 0, 0, 0, 7 };
        final ByteBuf buffer = Unpooled.buffer();

        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, AigpAttributeParser.TYPE, Unpooled.copiedBuffer(value), buffer);

        final BGPExtensionProviderContext providerContext = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
        final PathAttributes pathAttributes = providerContext.getAttributeRegistry().parseAttributes(buffer);
        final Aigp aigp = pathAttributes.getAigp();
        final AigpTlv tlv = aigp.getAigpTlv();

        assertNull("AIGP TLV should be null.", tlv);
    }

    @Test
    public void testSerializingAigpAttribute() throws BGPDocumentedException, BGPParsingException {
        final byte[] value = new byte[] { 1, 0, 11, 0, 0, 0, 0, 0, 0, 0, 8 };
        final ByteBuf inputData = Unpooled.buffer();
        final ByteBuf testBuffer = Unpooled.buffer();

        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL, AigpAttributeParser.TYPE, Unpooled.copiedBuffer(value), inputData);

        final BGPExtensionProviderContext providerContext = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
        final PathAttributes pathAttributes = providerContext.getAttributeRegistry().parseAttributes(inputData);
        final Aigp aigp = pathAttributes.getAigp();

        final PathAttributesBuilder pathAttributesBuilder = new PathAttributesBuilder();
        pathAttributesBuilder.setAigp(aigp);

        final AigpAttributeParser parser = new AigpAttributeParser();
        parser.serializeAttribute(pathAttributesBuilder.build(), testBuffer);

        final byte[] unparserData = inputData.copy(0, inputData.writerIndex()).array();
        final byte[] serializedData = testBuffer.copy(0, inputData.writerIndex()).array();

        assertTrue("Buffers should be the same.", Arrays.equals(unparserData, serializedData));
    }
}
