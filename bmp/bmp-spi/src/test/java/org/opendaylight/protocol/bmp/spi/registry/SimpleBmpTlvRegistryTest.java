/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.spi.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Tlv;

@ExtendWith(MockitoExtension.class)
class SimpleBmpTlvRegistryTest {
    private final SimpleBmpTlvRegistry bmpTlvRegistry = new SimpleBmpTlvRegistry();
    private final byte[] bytes = new byte[]{1, 2, 3};
    private final ByteBuf input = Unpooled.wrappedBuffer(this.bytes);
    private static final int DESCRIPTION_TLV_TYPE = 1;
    private static final int OTHER_TLV_TYPE = 2;
    @Mock
    private BmpTlvParser descriptionTlvParser;
    @Mock
    private BmpTlvSerializer descriptionTlvSerializer;

    @BeforeEach
    void setUp() {
        this.bmpTlvRegistry.registerBmpTlvParser(DESCRIPTION_TLV_TYPE, this.descriptionTlvParser);
        this.bmpTlvRegistry.registerBmpTlvSerializer(MockDescriptionTlv.class, this.descriptionTlvSerializer);
    }

    @Test
    void testParserRegistration() {
        assertNotNull(this.bmpTlvRegistry.registerBmpTlvParser(DESCRIPTION_TLV_TYPE, this.descriptionTlvParser));
    }

    @Test
    void testSerializerRegistration() {
        assertNotNull(this.bmpTlvRegistry.registerBmpTlvSerializer(MockDescriptionTlv.class,
                this.descriptionTlvSerializer));
    }

    @Test
    void testUnrecognizedType() throws BmpDeserializationException {
        assertNull(this.bmpTlvRegistry.parseTlv(OTHER_TLV_TYPE, this.input));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        this.bmpTlvRegistry.serializeTlv(new MockTlv(), output);
        assertEquals(0, output.readableBytes());
    }

    @Test
    void testParseTlv() throws BmpDeserializationException {
        doReturn(new MockDescriptionTlv()).when(this.descriptionTlvParser).parseTlv(this.input);
        final var tlvArg = ArgumentCaptor.forClass(Tlv.class);
        final var bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        doNothing().when(this.descriptionTlvSerializer).serializeTlv(tlvArg.capture(), bufArg.capture());

        final Tlv output = this.bmpTlvRegistry.parseTlv(DESCRIPTION_TLV_TYPE, this.input);
        assertNotNull(output);
        assertInstanceOf(MockDescriptionTlv.class, output);

        final ByteBuf aggregator = Unpooled.EMPTY_BUFFER;
        this.bmpTlvRegistry.serializeTlv(output, aggregator);
        verify(this.descriptionTlvSerializer).serializeTlv(output, aggregator);
    }

    private final class MockDescriptionTlv implements Tlv {
        @Override
        public Class<MockDescriptionTlv> implementedInterface() {
            return MockDescriptionTlv.class;
        }
    }

    private final class MockTlv implements Tlv {

        @Override
        public Class<MockTlv> implementedInterface() {
            return MockTlv.class;
        }

    }
}
