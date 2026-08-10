/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.rsvp.parser.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.list.SubobjectContainer;
import org.opendaylight.yangtools.yang.common.Uint32;

class EROSubobjectListParserTest {
    // FIXME: @Mock
    private final EROSubobjectRegistry registry = mock(EROSubobjectRegistry.class);
    private final SubobjectContainer subObj = mock(SubobjectContainer.class);
    private final RsvpTeObject rsvpTeObj = mock(RsvpTeObject.class);
    private final EroListParser parser = new EroListParser(registry);
    private final byte[] inputList = new byte[] { 1, 3, 1, 2, 4, 1, 2 };
    private final byte[] emptyInput = new byte[] { 1, 2 };
    private final byte[] wrongInput = new byte[] { 1, 3 };
    private final List<SubobjectContainer> subobjects = List.of(subObj, subObj);

    @BeforeEach
    void setUp() throws RSVPParsingException {
        doAnswer(invocation -> invocation.getArgument(1, ByteBuf.class).readableBytes() == 0 ? null : subObj)
            .when(registry).parseSubobject(anyInt(), any(ByteBuf.class), anyBoolean());
        doReturn("lala").when(subObj).toString();
        doAnswer(invocation -> {
            invocation.getArgument(1, ByteBuf.class).writeByte(1);
            return null;
        }).when(registry).serializeSubobject(any(SubobjectContainer.class), any(ByteBuf.class));
    }

    @Test
    void testParsingException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseList(null));
    }

    @Test
    void testWrongInput() {
        assertThrows(RSVPParsingException.class,
            () -> parser.parseList(Unpooled.copiedBuffer(wrongInput)));
    }

    @Test
    void testParseList() throws RSVPParsingException {
        assertEquals(0, parser.parseList(Unpooled.copiedBuffer(emptyInput)).size());
        final ByteBuf toBeParsed = Unpooled.copiedBuffer(inputList);
        assertEquals(2, parser.parseList(toBeParsed).size());
    }

    @Test
    void testSerializeList() {
        final ByteBuf buffer = Unpooled.buffer(2);
        assertEquals(0, buffer.readableBytes());
        parser.serializeList(subobjects, buffer);
        assertEquals(2, buffer.readableBytes());
    }

    @Test
    void testAbstractRSVPObjParser() throws RSVPParsingException {
        final ByteBuf byteAggregator = Unpooled.buffer(4);
        byte[] output = new byte[] { 0, 1, 2, 3 };
        EroListParser.serializeAttributeHeader(1, (short) 2, (short) 3, byteAggregator);
        assertArrayEquals(output, byteAggregator.array());

        final ByteBuf body = Unpooled.buffer(4);
        output = new byte[] { 0, 0, 0, 1 };
        final AttributeFilter filter = new AttributeFilter(Uint32.ONE);
        EroListParser.writeAttributeFilter(filter, body);
        assertArrayEquals(output, body.array());

        final ByteBuf parseTeObj = Unpooled.buffer(1);
        assertNotNull(parser.parseObject(parseTeObj));
        assertNull(parser.parseObject(null));

        assertEquals(0, parseTeObj.readableBytes());
        parser.serializeObject(null, parseTeObj);
        assertEquals(0, parseTeObj.readableBytes());
        parser.serializeObject(rsvpTeObj, parseTeObj);
        assertEquals(1, parseTeObj.readableBytes());
        assertEquals((short) 3, parseTeObj.readUnsignedByte());
    }

    private class EroListParser extends EROSubobjectListParser {
        EroListParser(final EROSubobjectRegistry subobjReg) {
            super(subobjReg);
        }

        @Override
        protected void localSerializeObject(final RsvpTeObject rsvpTeObject, final ByteBuf output) {
            output.writeByte(3);
        }

        @Override
        protected RsvpTeObject localParseObject(final ByteBuf byteBuf) throws RSVPParsingException {
            return rsvpTeObj;
        }
    }
}
