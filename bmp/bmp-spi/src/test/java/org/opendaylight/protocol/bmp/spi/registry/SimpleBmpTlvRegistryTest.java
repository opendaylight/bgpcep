/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.spi.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class SimpleBmpTlvRegistryTest {

    private final SimpleBmpTlvRegistry bmpTlvRegistry = new SimpleBmpTlvRegistry();
    private final byte[] bytes = new byte[]{1, 2, 3};
    private final ByteBuf input = Unpooled.wrappedBuffer(this.bytes);
    private static final int DESCRIPTION_TLV_TYPE = 1;
    private static final int OTHER_TLV_TYPE = 2;
    @Mock
    private BmpTlvParser descriptionTlvParser;
    @Mock
    private BmpTlvSerializer descriptionTlvSerializer;

    @Before
    public void setUp() throws BmpDeserializationException {
        MockitoAnnotations.initMocks(this);
        this.bmpTlvRegistry.registerBmpTlvParser(DESCRIPTION_TLV_TYPE, this.descriptionTlvParser);
        this.bmpTlvRegistry.registerBmpTlvSerializer(MockDescriptionTlv.class, this.descriptionTlvSerializer);
        Mockito.doReturn(new MockDescriptionTlv()).when(this.descriptionTlvParser).parseTlv(this.input);
        final ArgumentCaptor<Tlv> tlvArg = ArgumentCaptor.forClass(Tlv.class);
        final ArgumentCaptor<ByteBuf> bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        Mockito.doNothing().when(this.descriptionTlvSerializer).serializeTlv(tlvArg.capture(), bufArg.capture());
    }

    @Test
    public void testParserRegistration() {
        assertNotNull(this.bmpTlvRegistry.registerBmpTlvParser(DESCRIPTION_TLV_TYPE, this.descriptionTlvParser));
    }

    @Test
    public void testSerializerRegistration() {
        assertNotNull(this.bmpTlvRegistry.registerBmpTlvSerializer(MockDescriptionTlv.class,
                this.descriptionTlvSerializer));
    }

    @Test
    public void testUnrecognizedType() throws BmpDeserializationException {
        assertNull(this.bmpTlvRegistry.parseTlv(OTHER_TLV_TYPE, this.input));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        this.bmpTlvRegistry.serializeTlv(new MockTlv(), output);
        assertEquals(0, output.readableBytes());
    }

    @Test
    public void testParseTlv() throws BmpDeserializationException {
        final Tlv output = this.bmpTlvRegistry.parseTlv(DESCRIPTION_TLV_TYPE, this.input);
        assertNotNull(output);
        assertTrue(output instanceof MockDescriptionTlv);

        final ByteBuf aggregator = Unpooled.EMPTY_BUFFER;
        this.bmpTlvRegistry.serializeTlv(output, aggregator);
        Mockito.verify(this.descriptionTlvSerializer).serializeTlv(output, aggregator);
    }

    private final class MockDescriptionTlv implements Tlv {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return MockDescriptionTlv.class;
        }
    }

    private final class MockTlv implements Tlv {

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return MockTlv.class;
        }

    }
}
