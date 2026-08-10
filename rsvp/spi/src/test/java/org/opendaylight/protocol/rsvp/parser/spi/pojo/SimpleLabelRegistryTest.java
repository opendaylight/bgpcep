/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import org.opendaylight.protocol.rsvp.parser.spi.LabelParser;
import org.opendaylight.protocol.rsvp.parser.spi.LabelSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;

@ExtendWith(MockitoExtension.class)
class SimpleLabelRegistryTest {
    private final short ctype = 1;
    private final SimpleLabelRegistry simpleLabelRegistry = new SimpleLabelRegistry();
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[]{1, 2, 3});
    @Mock
    private LabelParser labelParser;
    @Mock
    private LabelSerializer labelSerializer;

    @BeforeEach
    void setUp() {
        this.simpleLabelRegistry.registerLabelParser(this.ctype, this.labelParser);
        this.simpleLabelRegistry.registerLabelSerializer(MockLabel.class, this.labelSerializer);
    }

    private void stubSerializeLabel() {
        final ArgumentCaptor<LabelType> tlvArg = ArgumentCaptor.forClass(LabelType.class);
        final ArgumentCaptor<ByteBuf> bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        doNothing().when(this.labelSerializer).serializeLabel(anyBoolean(), anyBoolean(),
            tlvArg.capture(), bufArg.capture());
    }

    @Test
    void testParserRegistration() {
        assertNotNull(this.simpleLabelRegistry.registerLabelParser(this.ctype, this.labelParser));
    }

    @Test
    void testSerializerRegistration() {
        assertNotNull(this.simpleLabelRegistry.registerLabelSerializer(MockLabelClass.class, this.labelSerializer));
    }

    @Test
    void testUnrecognizedType() throws RSVPParsingException {
        stubSerializeLabel();
        final int wrongLabelType = 99;
        assertNull(this.simpleLabelRegistry.parseLabel(wrongLabelType, this.input));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        this.simpleLabelRegistry.serializeLabel(false, false, new MockLabel(), output);
        assertEquals(0, output.readableBytes());
    }

    @Test
    void testParseWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> this.simpleLabelRegistry.parseLabel(wrongType, this.input));
    }

    @Test
    void testRegisterWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> this.simpleLabelRegistry.registerLabelParser(wrongType, this.labelParser));
    }

    @Test
    void testParseLabel() throws RSVPParsingException {
        stubSerializeLabel();
        doReturn(new MockLabel()).when(this.labelParser).parseLabel(this.input);
        final LabelType output = this.simpleLabelRegistry.parseLabel(this.ctype, this.input);
        assertNotNull(output);
        assertTrue(output instanceof MockLabel);

        final ByteBuf aggregator = Unpooled.EMPTY_BUFFER;
        this.simpleLabelRegistry.serializeLabel(false, false, output, aggregator);
        verify(this.labelSerializer).serializeLabel(false, false, output, aggregator);
    }

    private final class MockLabelClass implements LabelType {
        @Override
        public Class<? extends LabelType> implementedInterface() {
            return MockLabelClass.class;
        }
    }

    private final class MockLabel implements LabelType {
        @Override
        public Class<? extends LabelType> implementedInterface() {
            return MockLabel.class;
        }
    }
}
