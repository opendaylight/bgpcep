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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainerBuilder;

@ExtendWith(MockitoExtension.class)
class SimpleXROSubobjectRegistryTest {
    private final int subObjectTypeOne = 1;
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[]{1, 2, 3});
    private final SimpleXROSubobjectRegistry simpleXROSubobjectRegistry = new SimpleXROSubobjectRegistry();
    @Mock
    private XROSubobjectParser rroSubobjectParser;
    @Mock
    private XROSubobjectSerializer rroSubobjectSerializer;

    @BeforeEach
    void setUp() {
        this.simpleXROSubobjectRegistry.registerSubobjectParser(this.subObjectTypeOne, this.rroSubobjectParser);
    }

    @Test
    void testUnrecognizedType() throws RSVPParsingException {
        final int wrongType = 99;
        assertNull(this.simpleXROSubobjectRegistry.parseSubobject(wrongType, this.input, false));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        final SubobjectContainer container = new SubobjectContainerBuilder().setSubobjectType(new
            LabelCaseBuilder().build()).build();
        this.simpleXROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(0, output.readableBytes());
    }

    @Test
    void testParseWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> this.simpleXROSubobjectRegistry.parseSubobject(wrongType, this.input, false));
    }

    @Test
    void testRegisterWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> this.simpleXROSubobjectRegistry.registerSubobjectParser(wrongType, this.rroSubobjectParser));
    }

    @Test
    void testParserRegistration() throws RSVPParsingException {
        doReturn(new SubobjectContainerBuilder().build()).when(this.rroSubobjectParser).parseSubobject(this.input,
            false);
        assertNotNull(this.simpleXROSubobjectRegistry.registerSubobjectParser(this.subObjectTypeOne, this
            .rroSubobjectParser));
        assertNotNull(this.simpleXROSubobjectRegistry.parseSubobject(this.subObjectTypeOne, this.input, false));
    }

    @Test
    void testSerializerRegistration() {
        final ArgumentCaptor<SubobjectContainer> arg = ArgumentCaptor.forClass(SubobjectContainer.class);
        final ArgumentCaptor<ByteBuf> bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        doAnswer(invocation -> invocation.<ByteBuf>getArgument(1).writeBoolean(Boolean.TRUE))
            .when(this.rroSubobjectSerializer).serializeSubobject(arg.capture(), bufArg.capture());
        assertNotNull(this.simpleXROSubobjectRegistry.registerSubobjectSerializer(LabelCase.class, this
            .rroSubobjectSerializer));
        final SubobjectContainer container = new SubobjectContainerBuilder().setSubobjectType(new
            LabelCaseBuilder().build()).build();
        final ByteBuf output = Unpooled.buffer();
        this.simpleXROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(1, output.readableBytes());
    }

}
