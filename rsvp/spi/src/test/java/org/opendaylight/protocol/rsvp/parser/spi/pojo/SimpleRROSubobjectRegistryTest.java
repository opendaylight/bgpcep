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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCaseBuilder;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SimpleRROSubobjectRegistryTest {
    private final int subObjectTypeOne = 1;
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[]{1, 2, 3});
    private final SimpleRROSubobjectRegistry simpleRROSubobjectRegistry = new SimpleRROSubobjectRegistry();
    @Mock
    private RROSubobjectParser rroSubobjectParser;
    @Mock
    private RROSubobjectSerializer rroSubobjectSerializer;

    @Before
    public void setUp() throws RSVPParsingException {
        this.simpleRROSubobjectRegistry.registerSubobjectParser(this.subObjectTypeOne, this.rroSubobjectParser);
        doReturn(new SubobjectContainerBuilder().build()).when(this.rroSubobjectParser).parseSubobject(this.input);
        final ArgumentCaptor<SubobjectContainer> arg = ArgumentCaptor.forClass(SubobjectContainer.class);
        final ArgumentCaptor<ByteBuf> bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        doAnswer(invocation -> {
            invocation.<ByteBuf>getArgument(1).writeBoolean(Boolean.TRUE);
            return null;
        }).when(this.rroSubobjectSerializer).serializeSubobject(arg.capture(), bufArg.capture());
    }

    @Test
    public void testUnrecognizedType() throws RSVPParsingException {
        final int wrongType = 99;
        assertNull(this.simpleRROSubobjectRegistry.parseSubobject(wrongType, this.input));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        final SubobjectContainer container = new SubobjectContainerBuilder()
            .setSubobjectType(new LabelCaseBuilder().build()).build();
        this.simpleRROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(0, output.readableBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongType() throws RSVPParsingException {
        final int wrongType = 65536;
        this.simpleRROSubobjectRegistry.parseSubobject(wrongType, this.input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWrongType() {
        final int wrongType = 65536;
        this.simpleRROSubobjectRegistry.registerSubobjectParser(wrongType, this.rroSubobjectParser);
    }

    @Test
    public void testParserRegistration() throws RSVPParsingException {
        assertNotNull(this.simpleRROSubobjectRegistry.registerSubobjectParser(this.subObjectTypeOne,
            this.rroSubobjectParser));
        assertNotNull(this.simpleRROSubobjectRegistry.parseSubobject(this.subObjectTypeOne, this.input));
    }

    @Test
    public void testSerializerRegistration() {
        assertNotNull(this.simpleRROSubobjectRegistry.registerSubobjectSerializer(LabelCase.class,
            this.rroSubobjectSerializer));
        final ByteBuf output = Unpooled.buffer();
        final SubobjectContainer container = new SubobjectContainerBuilder().setSubobjectType(
            new LabelCaseBuilder().build()).build();
        this.simpleRROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(1, output.readableBytes());
    }

}