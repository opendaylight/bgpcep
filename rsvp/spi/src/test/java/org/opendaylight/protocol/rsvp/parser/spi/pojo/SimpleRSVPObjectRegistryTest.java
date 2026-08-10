/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.rsvp.parser.spi.pojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.object.SecondaryExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.object.SecondaryExplicitRouteObjectBuilder;

@ExtendWith(MockitoExtension.class)
class SimpleRSVPObjectRegistryTest {
    private final int subObjectTypeOne = 1;
    private final int subObjectCTypeOne = 1;
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[]{1, 2, 3});
    private final SimpleRSVPObjectRegistry simpleRSVPObjectRegistry = new SimpleRSVPObjectRegistry();
    @Mock
    private RSVPTeObjectParser rsvpTeObjectParser;
    @Mock
    private RSVPTeObjectSerializer rsvpTeObjectSerializer;

    @BeforeEach
    void setUp() {
        this.simpleRSVPObjectRegistry.registerRsvpObjectParser(this.subObjectTypeOne, this.subObjectCTypeOne,
            this.rsvpTeObjectParser);
        this.simpleRSVPObjectRegistry.registerRsvpObjectSerializer(SecondaryExplicitRouteObject.class,
            this.rsvpTeObjectSerializer);
    }

    private void stubSerializeObject() {
        final var arg = ArgumentCaptor.forClass(RsvpTeObject.class);
        final var bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        doNothing().when(this.rsvpTeObjectSerializer).serializeObject(arg.capture(), bufArg.capture());
    }

    @Test
    void testParserRegistration() {
        this.simpleRSVPObjectRegistry.registerRsvpObjectParser(this.subObjectTypeOne, this.subObjectCTypeOne,
            this.rsvpTeObjectParser);
    }

    @Test
    void testSerializerRegistration() {
        this.simpleRSVPObjectRegistry.registerRsvpObjectSerializer(SecondaryExplicitRouteObject.class,
            this.rsvpTeObjectSerializer);
    }

    @Test
    void testParseWrongType() throws RSVPParsingException {
        final int wrongType = 65536;
        assertNull(this.simpleRSVPObjectRegistry.parseRSPVTe(wrongType, this.subObjectCTypeOne, this.input));
    }

    @Test
    void testUnrecognizedType() throws RSVPParsingException {
        stubSerializeObject();
        final int wrongType = 99;
        assertNull(this.simpleRSVPObjectRegistry.parseRSPVTe(wrongType, this.subObjectCTypeOne, this.input));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        this.simpleRSVPObjectRegistry.serializeRSPVTe(new SecondaryExplicitRouteObjectBuilder().build(), output);
        assertEquals(0, output.readableBytes());
    }

    @Test
    void testParseRSVP() throws RSVPParsingException {
        stubSerializeObject();
        doReturn(new SecondaryExplicitRouteObjectBuilder().build()).when(this.rsvpTeObjectParser)
            .parseObject(this.input);
        final RsvpTeObject output = this.simpleRSVPObjectRegistry.parseRSPVTe(this.subObjectTypeOne,
            this.subObjectCTypeOne, this.input);
        assertNotNull(output);
        assertInstanceOf(SecondaryExplicitRouteObject.class, output);

        final ByteBuf aggregator = Unpooled.EMPTY_BUFFER;
        this.simpleRSVPObjectRegistry.serializeRSPVTe(output, aggregator);
        verify(this.rsvpTeObjectSerializer).serializeObject(output, aggregator);
    }


    @Test
    void testRegisterWrongCType() throws RSVPParsingException {
        final int wrongCType = 65536;
        assertNull(this.simpleRSVPObjectRegistry.parseRSPVTe(this.subObjectTypeOne, wrongCType, this.input));
    }

    @Test
    void testRegisterWrongType() {
        final int wrongType = 65536;
        assertThrows(IllegalArgumentException.class,
            () -> this.simpleRSVPObjectRegistry.registerRsvpObjectParser(wrongType, this.subObjectCTypeOne,
                this.rsvpTeObjectParser));
    }

}
