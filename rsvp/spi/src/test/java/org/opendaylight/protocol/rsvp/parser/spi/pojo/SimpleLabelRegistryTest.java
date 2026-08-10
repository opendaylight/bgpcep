/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[] { 1, 2, 3 });
    @Mock
    private LabelParser labelParser;
    @Mock
    private LabelSerializer labelSerializer;

    @BeforeEach
    void setUp() {
        simpleLabelRegistry.registerLabelParser(ctype, labelParser);
        simpleLabelRegistry.registerLabelSerializer(MockLabel.class, labelSerializer);
    }

    private void stubSerializeLabel() {
        final var tlvArg = ArgumentCaptor.forClass(LabelType.class);
        final var bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        doNothing().when(labelSerializer).serializeLabel(anyBoolean(), anyBoolean(),
            tlvArg.capture(), bufArg.capture());
    }

    @Test
    void testParserRegistration() {
        assertNotNull(simpleLabelRegistry.registerLabelParser(ctype, labelParser));
    }

    @Test
    void testSerializerRegistration() {
        assertNotNull(simpleLabelRegistry.registerLabelSerializer(MockLabelClass.class, labelSerializer));
    }

    @Test
    void testUnrecognizedType() throws RSVPParsingException {
        stubSerializeLabel();
        final int wrongLabelType = 99;
        assertNull(simpleLabelRegistry.parseLabel(wrongLabelType, input));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        simpleLabelRegistry.serializeLabel(false, false, new MockLabel(), output);
        assertEquals(0, output.readableBytes());
    }

    @Test
    void testParseWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> simpleLabelRegistry.parseLabel(wrongType, input));
    }

    @Test
    void testRegisterWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> simpleLabelRegistry.registerLabelParser(wrongType, labelParser));
    }

    @Test
    void testParseLabel() throws RSVPParsingException {
        stubSerializeLabel();
        doReturn(new MockLabel()).when(labelParser).parseLabel(input);
        final LabelType output = simpleLabelRegistry.parseLabel(ctype, input);
        assertNotNull(output);
        assertInstanceOf(MockLabel.class, output);

        final ByteBuf aggregator = Unpooled.EMPTY_BUFFER;
        simpleLabelRegistry.serializeLabel(false, false, output, aggregator);
        verify(labelSerializer).serializeLabel(false, false, output, aggregator);
    }

    private static final class MockLabelClass implements LabelType {
        @Override
        public Class<? extends LabelType> implementedInterface() {
            return MockLabelClass.class;
        }
    }

    private static final class MockLabel implements LabelType {
        @Override
        public Class<? extends LabelType> implementedInterface() {
            return MockLabel.class;
        }
    }
}
