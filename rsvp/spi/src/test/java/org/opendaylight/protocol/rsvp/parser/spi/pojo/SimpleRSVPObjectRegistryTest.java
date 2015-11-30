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
import static org.junit.Assert.assertTrue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPTeObjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.RsvpTeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.object.SecondaryExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.secondary.explicit.route.object.SecondaryExplicitRouteObjectBuilder;

public class SimpleRSVPObjectRegistryTest {
    private final int subObjectTypeOne = 1;
    private final int subObjectCTypeOne = 1;
    private final ByteBuf input = Unpooled.wrappedBuffer(new byte[]{1, 2, 3});
    private final SimpleRSVPObjectRegistry simpleRSVPObjectRegistry = new SimpleRSVPObjectRegistry();
    @Mock
    private RSVPTeObjectParser rsvpTeObjectParser;
    @Mock
    private RSVPTeObjectSerializer rsvpTeObjectSerializer;

    @Before
    public void setUp() throws RSVPParsingException {
        MockitoAnnotations.initMocks(this);
        this.simpleRSVPObjectRegistry.registerRsvpObjectParser(this.subObjectTypeOne, this.subObjectCTypeOne, this.rsvpTeObjectParser);
        this.simpleRSVPObjectRegistry.registerRsvpObjectSerializer(SecondaryExplicitRouteObject.class, this.rsvpTeObjectSerializer);
        Mockito.doReturn(new SecondaryExplicitRouteObjectBuilder().build()).when(this.rsvpTeObjectParser).parseObject(this.input);
        final ArgumentCaptor<RsvpTeObject> arg = ArgumentCaptor.forClass(RsvpTeObject.class);
        final ArgumentCaptor<ByteBuf> bufArg = ArgumentCaptor.forClass(ByteBuf.class);
        Mockito.doNothing().when(this.rsvpTeObjectSerializer).serializeObject(arg.capture(), bufArg.capture());
    }

    @Test
    public void testParserRegistration() {
        this.simpleRSVPObjectRegistry.registerRsvpObjectParser(this.subObjectTypeOne, this.subObjectCTypeOne, this.rsvpTeObjectParser);
    }

    @Test
    public void testSerializerRegistration() {
        this.simpleRSVPObjectRegistry.registerRsvpObjectSerializer(SecondaryExplicitRouteObject.class, this.rsvpTeObjectSerializer);
    }

    @Test
    public void testParseWrongType() throws RSVPParsingException {
        final int wrongType = 65536;
        assertNull(this.simpleRSVPObjectRegistry.parseRSPVTe(wrongType, this.subObjectCTypeOne, this.input));
    }

    @Test
    public void testUnrecognizedType() throws RSVPParsingException {
        final int wrongType = 99;
        assertNull(this.simpleRSVPObjectRegistry.parseRSPVTe(wrongType, this.subObjectCTypeOne, this.input));
        final ByteBuf output = Unpooled.EMPTY_BUFFER;
        this.simpleRSVPObjectRegistry.serializeRSPVTe(new SecondaryExplicitRouteObjectBuilder().build(), output);
        assertEquals(0, output.readableBytes());
    }

    @Test
    public void testParseRSVP() throws RSVPParsingException {
        final RsvpTeObject output = this.simpleRSVPObjectRegistry.parseRSPVTe(this.subObjectTypeOne, this.subObjectCTypeOne, this.input);
        assertNotNull(output);
        assertTrue(output instanceof SecondaryExplicitRouteObject);

        final ByteBuf aggregator = Unpooled.EMPTY_BUFFER;
        this.simpleRSVPObjectRegistry.serializeRSPVTe(output, aggregator);
        Mockito.verify(this.rsvpTeObjectSerializer).serializeObject(output, aggregator);
    }


    @Test
    public void testRegisterWrongCType() throws RSVPParsingException {
        final int wrongCType = 65536;
        assertNull(this.simpleRSVPObjectRegistry.parseRSPVTe(this.subObjectTypeOne, wrongCType, this.input));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWrongType() {
        final int wrongType = 65536;
        this.simpleRSVPObjectRegistry.registerRsvpObjectParser(wrongType, this.subObjectCTypeOne, this.rsvpTeObjectParser);
    }

}