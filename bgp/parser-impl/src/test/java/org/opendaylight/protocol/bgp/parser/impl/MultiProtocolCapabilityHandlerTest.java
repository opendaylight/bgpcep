/*
 * Copyright (c) 2015 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.MultiProtocolCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

@ExtendWith(MockitoExtension.class)
class MultiProtocolCapabilityHandlerTest {
    private static final Ipv6AddressFamily AFI = Ipv6AddressFamily.VALUE;
    private static final UnicastSubsequentAddressFamily SAFI = UnicastSubsequentAddressFamily.VALUE;

    @Mock
    private AddressFamilyRegistry afir;
    @Mock
    private AddressFamilyRegistry afirExpection;
    @Mock
    private SubsequentAddressFamilyRegistry safir;
    @Mock
    private SubsequentAddressFamilyRegistry safirException;

    @Test
    void testCapabilityHandler() throws BGPDocumentedException, BGPParsingException {
        doReturn(260).when(this.afir).numberForClass(AFI);
        doReturn(MultiProtocolCapabilityHandlerTest.AFI).when(this.afir).classForFamily(260);
        doReturn(4).when(this.safir).numberForClass(SAFI);
        doReturn(MultiProtocolCapabilityHandlerTest.SAFI).when(this.safir).classForFamily(4);

        final CParameters capabilityToSerialize = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                    .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                        .setAfi(MultiProtocolCapabilityHandlerTest.AFI)
                        .setSafi(MultiProtocolCapabilityHandlerTest.SAFI)
                        .build())
                    .build())
                .build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afir, this.safir);
        handler.serializeCapability(capabilityToSerialize, bytes);
        final CParameters newCaps = handler.parseCapability(bytes);

        assertEquals(capabilityToSerialize.hashCode(), newCaps.hashCode());
    }

    @Test
    void testUnhandledAfi() {
        doReturn(null).when(this.afirExpection).numberForClass(AFI);

        final CParameters capabilityToSerialize = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                    .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                        .setAfi(MultiProtocolCapabilityHandlerTest.AFI)
                        .setSafi(MultiProtocolCapabilityHandlerTest.SAFI).build())
                    .build())
                .build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afirExpection,
            this.safir);
        assertThrows(IllegalArgumentException.class, () -> handler.serializeCapability(capabilityToSerialize, bytes));
    }

    @Test
    void testUnhandledSafi() {
        doReturn(260).when(this.afir).numberForClass(AFI);
        doReturn(null).when(this.safirException).numberForClass(SAFI);

        final CParameters capabilityToSerialize = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                    .setMultiprotocolCapability(new MultiprotocolCapabilityBuilder()
                        .setAfi(MultiProtocolCapabilityHandlerTest.AFI)
                        .setSafi(MultiProtocolCapabilityHandlerTest.SAFI)
                        .build())
                    .build())
                .build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afir,
            this.safirException);
        assertThrows(IllegalArgumentException.class, () -> handler.serializeCapability(capabilityToSerialize, bytes));
    }

    @Test
    void noSerializationTest() {
        final CParameters capabilityNoAugmentation = new CParametersBuilder().removeAugmentation(CParameters1.class)
                .build();
        final CParameters capabilityNoMP = new CParametersBuilder().addAugmentation(new CParameters1Builder().build())
                .build();

        final ByteBuf bytes = Unpooled.buffer();
        final MultiProtocolCapabilityHandler handler = new MultiProtocolCapabilityHandler(this.afir,
            this.safirException);
        handler.serializeCapability(capabilityNoAugmentation, bytes);
        assertEquals(0, bytes.readableBytes());
        handler.serializeCapability(capabilityNoMP, bytes);
        assertEquals(0, bytes.readableBytes());
    }
}
