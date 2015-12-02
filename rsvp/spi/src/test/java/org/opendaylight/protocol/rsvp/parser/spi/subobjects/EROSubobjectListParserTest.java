/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer;

public class EROSubobjectListParserTest {

    private final EROSubobjectRegistry registry = Mockito.mock(EROSubobjectRegistry.class);
    private final SubobjectContainer subObj = Mockito.mock(SubobjectContainer.class);
    private final RsvpTeObject rsvpTeObj = Mockito.mock(RsvpTeObject.class);
    private final EroListParser parser = new EroListParser(this.registry);
    private final byte[] inputList = new byte[] {1, 3, 1, 2, 4, 1, 2};
    private final byte[] emptyInput = new byte[] {1, 2};
    private final byte[] wrongInput = new byte[] {1, 3};
    private final List<SubobjectContainer> subobjects = Arrays.asList(this.subObj, this.subObj);

    @Before
    public void setUp() throws RSVPParsingException {
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                if (((ByteBuf) invocation.getArguments()[1]).readableBytes() == 0) {
                    return null;
                }
                return EROSubobjectListParserTest.this.subObj;
            }
        }).when(this.registry).parseSubobject(Mockito.anyInt(), Mockito.any(ByteBuf.class), Mockito.anyBoolean());
        Mockito.doReturn("lala").when(this.subObj).toString();
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                ((ByteBuf) invocation.getArguments()[1]).writeByte(1);
                return null;
            }
        }).when(this.registry).serializeSubobject(Mockito.any(SubobjectContainer.class), Mockito.any(ByteBuf.class));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParsingException() throws RSVPParsingException {
        this.parser.parseList(null);
    }

    @Test(expected=RSVPParsingException.class)
    public void testWrongInput() throws RSVPParsingException {
        this.parser.parseList(Unpooled.copiedBuffer(this.wrongInput));
    }

    @Test
    public void testParseList() throws RSVPParsingException {
        assertEquals(0, this.parser.parseList(Unpooled.copiedBuffer(this.emptyInput)).size());
        final ByteBuf toBeParsed = Unpooled.copiedBuffer(this.inputList);
        assertEquals(2, this.parser.parseList(toBeParsed).size());
    }

    @Test
    public void testSerializeList() {
        final ByteBuf buffer = Unpooled.buffer(2);
        assertEquals(0, buffer.readableBytes());
        this.parser.serializeList(this.subobjects, buffer);
        assertEquals(2, buffer.readableBytes());
    }

    @Test
    public void testAbstractRSVPObjParser() throws RSVPParsingException {
        final ByteBuf byteAggregator = Unpooled.buffer(4);
        byte[] output = new byte[] {0, 1, 2, 3};
        this.parser.serializeAttributeHeader(1, (short) 2, (short) 3, byteAggregator);
        assertArrayEquals(output, byteAggregator.array());

        final ByteBuf body = Unpooled.buffer(4);
        output = new byte[] {0, 0, 0, 1};
        final AttributeFilter filter = new AttributeFilter(1L);
        this.parser.writeAttributeFilter(filter, body);
        assertArrayEquals(output, body.array());

        final ByteBuf parseTeObj = Unpooled.buffer(1);
        assertNotNull(this.parser.parseObject(parseTeObj));
        assertNull(this.parser.parseObject(null));

        assertEquals(0, parseTeObj.readableBytes());
        this.parser.serializeObject(null, parseTeObj);
        assertEquals(0, parseTeObj.readableBytes());
        this.parser.serializeObject(this.rsvpTeObj, parseTeObj);
        assertEquals(1, parseTeObj.readableBytes());
        assertEquals((short) 3, parseTeObj.readUnsignedByte());
    }

    private class EroListParser extends EROSubobjectListParser {
        public EroListParser(final EROSubobjectRegistry subobjReg) {
            super(subobjReg);
        }
        @Override
        protected void localSerializeObject(final RsvpTeObject rsvpTeObject, final ByteBuf output) {
            output.writeByte(3);
        }
        @Override
        protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
            return EROSubobjectListParserTest.this.rsvpTeObj;
        }
    }
}
