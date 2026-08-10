/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.spi.subobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.protocol.rsvp.parser.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.list.SubobjectContainer;

class RROSubobjectListParserTest {

    private final RROSubobjectRegistry registry = mock(RROSubobjectRegistry.class);
    private final SubobjectContainer subObj = mock(SubobjectContainer.class);
    private final RroListParser parser = new RroListParser(registry);
    private final byte[] inputList = new byte[]{1, 3, 1, 2, 4, 1, 2};
    private final byte[] emptyInput = new byte[]{1, 2};
    private final byte[] wrongInput = new byte[]{1, 3};
    private final List<SubobjectContainer> subobjects = List.of(subObj, subObj);

    @BeforeEach
    void setUp() throws RSVPParsingException {
        doAnswer(invocation -> invocation.getArgument(1, ByteBuf.class).readableBytes() == 0 ? null : subObj)
            .when(registry).parseSubobject(Mockito.anyInt(), Mockito.any(ByteBuf.class));
        doReturn("lala").when(subObj).toString();
        doAnswer(invocation -> {
            invocation.getArgument(1, ByteBuf.class).writeByte(1);
            return null;
        }).when(registry).serializeSubobject(Mockito.any(SubobjectContainer.class), Mockito.any(ByteBuf.class));
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

    private static class RroListParser extends RROSubobjectListParser {
        RroListParser(final RROSubobjectRegistry subobjReg) {
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
