/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.MultiProtocolCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.open.bgp.parameters.optional.capabilities.c.parameters.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class MultiProtocolCapabilityHandlerTest {

    private final static Class afi = Ipv6AddressFamily.class;
    @Mock private AddressFamilyRegistry afir;
    @Mock private AddressFamilyRegistry afirExpection;
    private final static Class safi = UnicastSubsequentAddressFamily.class;
    @Mock private SubsequentAddressFamilyRegistry safir;
    @Mock private SubsequentAddressFamilyRegistry safirException;
    private final ByteBuf serializedBytes = Unpooled.copiedBuffer(new byte[] {1, 4, 1, 4, 0, 4});

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(260).when(this.afir).numberForClass(afi);
        Mockito.doReturn(this.afi).when(this.afir).classForFamily(260);

        Mockito.doReturn(null).when(this.afirExpection).numberForClass(afi);
        Mockito.doReturn(null).when(this.afirExpection).classForFamily(260);

        Mockito.doReturn(4).when(this.safir).numberForClass(safi);
        Mockito.doReturn(this.safi).when(this.safir).classForFamily(4);

        Mockito.doReturn(null).when(this.safirException).numberForClass(safi);
        Mockito.doReturn(null).when(this.safirException).classForFamily(4);
    }

    @Test
    public void testCapabilityHandler() throws BGPDocumentedException, BGPParsingException {
        final CParameters capabilityToSerialize = new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(
            new MultiprotocolCapabilityBuilder().setAfi(this.afi).setSafi(this.safi).build()).build()).build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afir, this.safir);
        handler.serializeCapability(capabilityToSerialize, bytes);
        final CParameters newCaps = handler.parseCapability(bytes);

        assertEquals(capabilityToSerialize.hashCode(), newCaps.hashCode());
    }

    @Test(expected=BGPParsingException.class)
    public void testAfiException() throws BGPDocumentedException, BGPParsingException {
        final ByteBuf bytes = this.serializedBytes.copy();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afirExpection, this.safir);
        handler.parseCapability(bytes);
    }

    @Test(expected=BGPParsingException.class)
    public void testSafiException() throws BGPDocumentedException, BGPParsingException {
        final ByteBuf bytes = this.serializedBytes.copy();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afir, this.safirException);
        handler.parseCapability(bytes);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnhandledAfi() {
        final CParameters capabilityToSerialize = new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(
            new MultiprotocolCapabilityBuilder().setAfi(this.afi).setSafi(this.safi).build()).build()).build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afirExpection, this.safir);
        handler.serializeCapability(capabilityToSerialize, bytes);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnhandledSafi() {
        final CParameters capabilityToSerialize = new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(
            new MultiprotocolCapabilityBuilder().setAfi(this.afi).setSafi(this.safi).build()).build()).build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afir, this.safirException);
        handler.serializeCapability(capabilityToSerialize, bytes);
    }

    @Test
    public void noSerializationTest() {
        final CParameters capabilityNoAugmentation = new CParametersBuilder().addAugmentation(CParameters1.class, null).build();
        final CParameters capabilityNoMP = new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder().build()).build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afir, this.safirException);
        handler.serializeCapability(capabilityNoAugmentation, bytes);
        assertEquals(0, bytes.readableBytes());
        handler.serializeCapability(capabilityNoMP, bytes);
        assertEquals(0, bytes.readableBytes());
    }
}
