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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainerBuilder;

@ExtendWith(MockitoExtension.class)
class SimpleEROSubobjectRegistryTest {
    private final int subObjectTypeOne = 1;
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[]{1, 2, 3});
    private final SimpleEROSubobjectRegistry simpleEROSubobjectRegistry = new SimpleEROSubobjectRegistry();
    @Mock
    private EROSubobjectParser rroSubobjectParser;
    @Mock
    private EROSubobjectSerializer rroSubobjectSerializer;


    @BeforeEach
    void setUp() {
        this.simpleEROSubobjectRegistry.registerSubobjectParser(this.subObjectTypeOne, this.rroSubobjectParser);
    }

    @Test
    void testUnrecognizedType() throws RSVPParsingException {
        final int wrongType = 99;
        assertNull(this.simpleEROSubobjectRegistry.parseSubobject(wrongType, this.input, false));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        final SubobjectContainer container = new SubobjectContainerBuilder()
            .setSubobjectType(new LabelCaseBuilder().build()).build();
        this.simpleEROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(0, output.readableBytes());
    }

    @Test
    void testParseWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> this.simpleEROSubobjectRegistry.parseSubobject(wrongType, this.input, false));
    }

    @Test
    void testRegisterWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> this.simpleEROSubobjectRegistry.registerSubobjectParser(wrongType, this.rroSubobjectParser));
    }

    @Test
    void testParserRegistration() throws RSVPParsingException {
        Mockito.doReturn(new SubobjectContainerBuilder().build()).when(this.rroSubobjectParser)
            .parseSubobject(this.input, false);
        assertNotNull(this.simpleEROSubobjectRegistry.registerSubobjectParser(this.subObjectTypeOne,
            this.rroSubobjectParser));
        assertNotNull(this.simpleEROSubobjectRegistry.parseSubobject(this.subObjectTypeOne, this.input, false));
    }

    @Test
    void testSerializerRegistration() {
        final ArgumentCaptor<SubobjectContainer> arg = ArgumentCaptor.forClass(SubobjectContainer.class);
        final ArgumentCaptor<ByteBuf> bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        Mockito.doAnswer(invocation -> {
            final Object[] args = invocation.getArguments();
            ((ByteBuf) args[1]).writeBoolean(Boolean.TRUE);
            return null;
        }).when(this.rroSubobjectSerializer).serializeSubobject(arg.capture(), bufArg.capture());
        assertNotNull(this.simpleEROSubobjectRegistry.registerSubobjectSerializer(LabelCase.class, this
            .rroSubobjectSerializer));
        final SubobjectContainer container = new SubobjectContainerBuilder().setSubobjectType(
            new LabelCaseBuilder().build()).build();
        final ByteBuf output = Unpooled.buffer();
        this.simpleEROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(1, output.readableBytes());
    }

}
