/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.XROSubobjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.exclude.route.object.exclude.route.object.SubobjectContainer;

public class XROSubobjectListParserTest {

    private final XROSubobjectRegistry registry = Mockito.mock(XROSubobjectRegistry.class);
    private final SubobjectContainer subObj = Mockito.mock(SubobjectContainer.class);
    private final XroListParser parser = new XroListParser(this.registry);
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
                return XROSubobjectListParserTest.this.subObj;
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

    private class XroListParser extends XROSubobjectListParser {
        public XroListParser(final XROSubobjectRegistry subobjReg) {
            super(subobjReg);
        }
        @Override
        protected void localSerializeObject(final RsvpTeObject rsvpTeObject, final ByteBuf output) {
        }
        @Override
        protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
            return null;
        }
    }
}
