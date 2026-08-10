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
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.subobject.type.LabelCaseBuilder;

@ExtendWith(MockitoExtension.class)
class SimpleRROSubobjectRegistryTest {
    private final int subObjectTypeOne = 1;
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[] { 1, 2, 3 });
    private final SimpleRROSubobjectRegistry simpleRROSubobjectRegistry = new SimpleRROSubobjectRegistry();
    @Mock
    private RROSubobjectParser rroSubobjectParser;
    @Mock
    private RROSubobjectSerializer rroSubobjectSerializer;

    @BeforeEach
    void setUp() {
        simpleRROSubobjectRegistry.registerSubobjectParser(subObjectTypeOne, rroSubobjectParser);
    }

    @Test
    void testUnrecognizedType() throws RSVPParsingException {
        final int wrongType = 99;
        assertNull(simpleRROSubobjectRegistry.parseSubobject(wrongType, input));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        final SubobjectContainer container = new SubobjectContainerBuilder()
            .setSubobjectType(new LabelCaseBuilder().build()).build();
        simpleRROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(0, output.readableBytes());
    }

    @Test
    void testParseWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> simpleRROSubobjectRegistry.parseSubobject(wrongType, input));
    }

    @Test
    void testRegisterWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> simpleRROSubobjectRegistry.registerSubobjectParser(wrongType, rroSubobjectParser));
    }

    @Test
    void testParserRegistration() throws RSVPParsingException {
        doReturn(new SubobjectContainerBuilder().build()).when(rroSubobjectParser).parseSubobject(input);
        assertNotNull(simpleRROSubobjectRegistry.registerSubobjectParser(subObjectTypeOne,
            rroSubobjectParser));
        assertNotNull(simpleRROSubobjectRegistry.parseSubobject(subObjectTypeOne, input));
    }

    @Test
    void testSerializerRegistration() {
        final var arg = ArgumentCaptor.forClass(SubobjectContainer.class);
        final var bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        doAnswer(invocation -> {
            invocation.getArgument(1, ByteBuf.class).writeBoolean(true);
            return null;
        }).when(rroSubobjectSerializer).serializeSubobject(arg.capture(), bufArg.capture());
        assertNotNull(simpleRROSubobjectRegistry.registerSubobjectSerializer(LabelCase.class,
            rroSubobjectSerializer));
        final ByteBuf output = Unpooled.buffer();
        final SubobjectContainer container = new SubobjectContainerBuilder().setSubobjectType(
            new LabelCaseBuilder().build()).build();
        simpleRROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(1, output.readableBytes());
    }
}
