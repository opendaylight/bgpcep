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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainerBuilder;

public class SimpleXROSubobjectRegistryTest {
    private final int subObjectTypeOne = 1;
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[]{1, 2, 3});
    private final SimpleXROSubobjectRegistry simpleXROSubobjectRegistry = new SimpleXROSubobjectRegistry();
    @Mock
    private XROSubobjectParser rroSubobjectParser;
    @Mock
    private XROSubobjectSerializer rroSubobjectSerializer;


    @Before
    public void setUp() throws RSVPParsingException {
        MockitoAnnotations.initMocks(this);
        this.simpleXROSubobjectRegistry.registerSubobjectParser(this.subObjectTypeOne, this.rroSubobjectParser);
        Mockito.doReturn(new SubobjectContainerBuilder().build()).when(this.rroSubobjectParser).parseSubobject(this
            .input, false);
        final ArgumentCaptor<SubobjectContainer> arg = ArgumentCaptor.forClass(SubobjectContainer.class);
        final ArgumentCaptor<ByteBuf> bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                ((ByteBuf) args[1]).writeBoolean(Boolean.TRUE);
                return null;
            }
        }).when(this.rroSubobjectSerializer).serializeSubobject(arg.capture(), bufArg.capture());
    }

    @Test
    public void testUnrecognizedType() throws RSVPParsingException {
        final int wrongType = 99;
        assertNull(this.simpleXROSubobjectRegistry.parseSubobject(wrongType, this.input, false));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        final SubobjectContainer container = new SubobjectContainerBuilder().setSubobjectType(new
            LabelCaseBuilder().build()).build();
        this.simpleXROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(0, output.readableBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongType() {
        final int wrongType = 65536;
        try {
            this.simpleXROSubobjectRegistry.parseSubobject(wrongType, this.input, false);
        } catch (final RSVPParsingException e) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWrongType() {
        final int wrongType = 65536;
        this.simpleXROSubobjectRegistry.registerSubobjectParser(wrongType, this.rroSubobjectParser);
    }

    @Test
    public void testParserRegistration() throws RSVPParsingException {
        assertNotNull(this.simpleXROSubobjectRegistry.registerSubobjectParser(this.subObjectTypeOne, this
            .rroSubobjectParser));
        assertNotNull(this.simpleXROSubobjectRegistry.parseSubobject(this.subObjectTypeOne, this.input, false));
    }

    @Test
    public void testSerializerRegistration() {
        assertNotNull(this.simpleXROSubobjectRegistry.registerSubobjectSerializer(LabelCase.class, this
            .rroSubobjectSerializer));
        final SubobjectContainer container = new SubobjectContainerBuilder().setSubobjectType(new
            LabelCaseBuilder().build()).build();
        final ByteBuf output = Unpooled.buffer();
        this.simpleXROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(1, output.readableBytes());
    }

}