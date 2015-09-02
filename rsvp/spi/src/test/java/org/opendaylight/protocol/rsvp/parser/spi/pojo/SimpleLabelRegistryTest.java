/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.pojo;

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
import org.opendaylight.protocol.rsvp.parser.spi.LabelParser;
import org.opendaylight.protocol.rsvp.parser.spi.LabelSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.LabelType;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class SimpleLabelRegistryTest {
    final short cTypeOne = 1;
    private final SimpleLabelRegistry simpleLabelRegistry = new SimpleLabelRegistry();
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[]{1, 2, 3});
    @Mock
    private LabelParser labelParser;
    @Mock
    private LabelSerializer labelSerializer;

    @Before
    public void setUp() throws RSVPParsingException {
        MockitoAnnotations.initMocks(this);
        this.simpleLabelRegistry.registerLabelParser(cTypeOne, this.labelParser);
        this.simpleLabelRegistry.registerLabelSerializer(MockLabel.class, this.labelSerializer);
        Mockito.doReturn(new MockLabel()).when(this.labelParser).parseLabel(this.input);
        final ArgumentCaptor<LabelType> tlvArg = ArgumentCaptor.forClass(LabelType.class);
        final ArgumentCaptor<ByteBuf> bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        Mockito.doNothing().when(this.labelSerializer).serializeLabel(Mockito.anyBoolean(), Mockito.anyBoolean(), tlvArg.capture(), bufArg.capture());
    }

    @Test
    public void testParserRegistration() {
        assertNotNull(this.simpleLabelRegistry.registerLabelParser(cTypeOne, this.labelParser));
    }

    @Test
    public void testSerializerRegistration() {
        assertNotNull(this.simpleLabelRegistry.registerLabelSerializer(MockLabelClass.class, this.labelSerializer));
    }

    @Test
    public void testUnrecognizedType() throws RSVPParsingException {
        final int wrongLabelType = 99;
        assertNull(this.simpleLabelRegistry.parseLabel(wrongLabelType, this.input));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        this.simpleLabelRegistry.serializeLabel(false, false, new MockLabel(), output);
        assertEquals(0, output.readableBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongType() {
        final int wrongType = 65536;
        try {
            this.simpleLabelRegistry.parseLabel(wrongType, this.input);
        } catch (RSVPParsingException e) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWrongType() {
        final int wrongType = 65536;
        this.simpleLabelRegistry.registerLabelParser(wrongType, this.labelParser);
    }

    @Test
    public void testParseLabel() throws RSVPParsingException {
        final LabelType output = this.simpleLabelRegistry.parseLabel(cTypeOne, this.input);
        assertNotNull(output);
        assertTrue(output instanceof MockLabel);

        final ByteBuf aggregator = Unpooled.EMPTY_BUFFER;
        this.simpleLabelRegistry.serializeLabel(false, false, output, aggregator);
        Mockito.verify(this.labelSerializer).serializeLabel(false, false, output, aggregator);
    }

    private final class MockLabelClass implements LabelType {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return MockLabelClass.class;
        }
    }

    private final class MockLabel implements LabelType {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return MockLabel.class;
        }
    }
}