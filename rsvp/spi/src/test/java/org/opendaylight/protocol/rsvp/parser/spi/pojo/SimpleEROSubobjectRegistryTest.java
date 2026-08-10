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
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[] { 1, 2, 3 });
    private final SimpleEROSubobjectRegistry simpleEROSubobjectRegistry = new SimpleEROSubobjectRegistry();
    @Mock
    private EROSubobjectParser rroSubobjectParser;
    @Mock
    private EROSubobjectSerializer rroSubobjectSerializer;

    @BeforeEach
    void setUp() {
        simpleEROSubobjectRegistry.registerSubobjectParser(subObjectTypeOne, rroSubobjectParser);
    }

    @Test
    void testUnrecognizedType() throws RSVPParsingException {
        final int wrongType = 99;
        assertNull(simpleEROSubobjectRegistry.parseSubobject(wrongType, input, false));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        final SubobjectContainer container = new SubobjectContainerBuilder()
            .setSubobjectType(new LabelCaseBuilder().build()).build();
        simpleEROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(0, output.readableBytes());
    }

    @Test
    void testParseWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> simpleEROSubobjectRegistry.parseSubobject(wrongType, input, false));
    }

    @Test
    void testRegisterWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> simpleEROSubobjectRegistry.registerSubobjectParser(wrongType, rroSubobjectParser));
    }

    @Test
    void testParserRegistration() throws RSVPParsingException {
        doReturn(new SubobjectContainerBuilder().build()).when(rroSubobjectParser)
            .parseSubobject(input, false);
        assertNotNull(simpleEROSubobjectRegistry.registerSubobjectParser(subObjectTypeOne,
            rroSubobjectParser));
        assertNotNull(simpleEROSubobjectRegistry.parseSubobject(subObjectTypeOne, input, false));
    }

    @Test
    void testSerializerRegistration() {
        final var arg = ArgumentCaptor.forClass(SubobjectContainer.class);
        final var bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        doAnswer(invocation -> {
            invocation.getArgument(1, ByteBuf.class).writeBoolean(true);
            return null;
        }).when(rroSubobjectSerializer).serializeSubobject(arg.capture(), bufArg.capture());
        assertNotNull(simpleEROSubobjectRegistry.registerSubobjectSerializer(LabelCase.class, rroSubobjectSerializer));
        final SubobjectContainer container = new SubobjectContainerBuilder().setSubobjectType(
            new LabelCaseBuilder().build()).build();
        final ByteBuf output = Unpooled.buffer();
        simpleEROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(1, output.readableBytes());
    }

}
