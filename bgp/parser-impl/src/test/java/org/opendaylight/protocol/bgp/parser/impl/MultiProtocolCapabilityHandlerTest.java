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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class MultiProtocolCapabilityHandlerTest {

    private static final Class<Ipv6AddressFamily> AFI = Ipv6AddressFamily.class;
    private static final Class<UnicastSubsequentAddressFamily> SAFI = UnicastSubsequentAddressFamily.class;

    @Mock
    private AddressFamilyRegistry afir;
    @Mock
    private AddressFamilyRegistry afirExpection;
    @Mock
    private SubsequentAddressFamilyRegistry safir;
    @Mock
    private SubsequentAddressFamilyRegistry safirException;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(260).when(this.afir).numberForClass(AFI);
        Mockito.doReturn(MultiProtocolCapabilityHandlerTest.AFI).when(this.afir).classForFamily(260);

        Mockito.doReturn(null).when(this.afirExpection).numberForClass(AFI);
        Mockito.doReturn(null).when(this.afirExpection).classForFamily(260);

        Mockito.doReturn(4).when(this.safir).numberForClass(SAFI);
        Mockito.doReturn(MultiProtocolCapabilityHandlerTest.SAFI).when(this.safir).classForFamily(4);

        Mockito.doReturn(null).when(this.safirException).numberForClass(SAFI);
        Mockito.doReturn(null).when(this.safirException).classForFamily(4);
    }

    @Test
    public void testCapabilityHandler() throws BGPDocumentedException, BGPParsingException {
        final CParameters capabilityToSerialize = new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(
            new MultiprotocolCapabilityBuilder().setAfi(MultiProtocolCapabilityHandlerTest.AFI).setSafi(MultiProtocolCapabilityHandlerTest.SAFI).build()).build()).build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afir, this.safir);
        handler.serializeCapability(capabilityToSerialize, bytes);
        final CParameters newCaps = handler.parseCapability(bytes);

        assertEquals(capabilityToSerialize.hashCode(), newCaps.hashCode());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnhandledAfi() {
        final CParameters capabilityToSerialize = new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(
            new MultiprotocolCapabilityBuilder().setAfi(MultiProtocolCapabilityHandlerTest.AFI).setSafi(MultiProtocolCapabilityHandlerTest.SAFI).build()).build()).build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afirExpection, this.safir);
        handler.serializeCapability(capabilityToSerialize, bytes);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnhandledSafi() {
        final CParameters capabilityToSerialize = new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder().setMultiprotocolCapability(
            new MultiprotocolCapabilityBuilder().setAfi(MultiProtocolCapabilityHandlerTest.AFI).setSafi(MultiProtocolCapabilityHandlerTest.SAFI).build()).build()).build();

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
