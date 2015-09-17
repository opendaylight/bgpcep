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
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class SimpleRROSubobjectRegistryTest {

    private final int subObjectTypeOne = 1;
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[]{1, 2, 3});
    private SimpleRROSubobjectRegistry simpleRROSubobjectRegistry = new SimpleRROSubobjectRegistry();
    @Mock
    private RROSubobjectParser rroSubobjectParser;
    @Mock
    private RROSubobjectSerializer rroSubobjectSerializer;


    @Before
    public void setUp() throws RSVPParsingException {
        MockitoAnnotations.initMocks(this);
        this.simpleRROSubobjectRegistry.registerSubobjectParser(subObjectTypeOne, this.rroSubobjectParser);
        this.simpleRROSubobjectRegistry.registerSubobjectSerializer(MockRROSubObjectClass.class, this.rroSubobjectSerializer);
        Mockito.doReturn(new SubobjectContainerBuilder().build()).when(this.rroSubobjectParser).parseSubobject(this.input);
        final ArgumentCaptor<SubobjectContainer> arg = ArgumentCaptor.forClass(SubobjectContainer.class);
        final ArgumentCaptor<ByteBuf> bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        Mockito.doNothing().when(this.rroSubobjectSerializer).serializeSubobject(arg.capture(), bufArg.capture());
    }

    @Test
    public void testUnrecognizedType() throws RSVPParsingException {
        final int wrongType = 99;
        assertNull(this.simpleRROSubobjectRegistry.parseSubobject(wrongType, this.input));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        final SubobjectContainer container = new SubobjectContainerBuilder().setSubobjectType(new
            LabelCaseBuilder().build()).build();
        this.simpleRROSubobjectRegistry.serializeSubobject(container, output);
        assertEquals(0, output.readableBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongType() {
        final int wrongType = 65536;
        try {
            this.simpleRROSubobjectRegistry.parseSubobject(wrongType, this.input);
        } catch (RSVPParsingException e) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWrongType() {
        final int wrongType = 65536;
        this.simpleRROSubobjectRegistry.registerSubobjectParser(wrongType, this.rroSubobjectParser);
    }

    @Test
    public void testParserRegistration() {
        assertNotNull(this.simpleRROSubobjectRegistry.registerSubobjectParser(subObjectTypeOne, this
            .rroSubobjectParser));
    }

    @Test
    public void testSerializerRegistration() {
        assertNotNull(this.simpleRROSubobjectRegistry.registerSubobjectSerializer(MockRROSubObjectClass.class, this
            .rroSubobjectSerializer));
    }

    private final class MockRROSubObjectClass implements SubobjectType {
        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return MockRROSubObjectClass.class;
        }
    }
}