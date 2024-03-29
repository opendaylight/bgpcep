/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.AddPathCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.AddPathCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AddPathCapabilityHandlerTest {
    private static final Ipv6AddressFamily AFI = Ipv6AddressFamily.VALUE;
    private static final UnicastSubsequentAddressFamily SAFI = UnicastSubsequentAddressFamily.VALUE;

    @Mock
    private AddressFamilyRegistry afiRegistry;
    @Mock
    private AddressFamilyRegistry afirExpection;
    @Mock
    private SubsequentAddressFamilyRegistry safiRegistry;
    @Mock
    private SubsequentAddressFamilyRegistry safirException;
    private final ByteBuf parseCorrectBytes = Unpooled.copiedBuffer(new byte[] {1, 4, 4, 1});
    private final ByteBuf parseWrongBytes = Unpooled.copiedBuffer(new byte[] {1, 4, 4, 4});
    private final byte[] serializedBytes = new byte[] {AddPathCapabilityHandler.CODE, 4, 1, 4, 4, 1};

    @Before
    public void setUp() {
        doReturn(260).when(this.afiRegistry).numberForClass(AFI);
        doReturn(AddPathCapabilityHandlerTest.AFI).when(this.afiRegistry).classForFamily(260);

        doReturn(null).when(this.afirExpection).numberForClass(AFI);
        doReturn(null).when(this.afirExpection).classForFamily(260);

        doReturn(4).when(this.safiRegistry).numberForClass(SAFI);
        doReturn(AddPathCapabilityHandlerTest.SAFI).when(this.safiRegistry).classForFamily(4);

        doReturn(null).when(this.safirException).numberForClass(SAFI);
        doReturn(null).when(this.safirException).classForFamily(4);
    }

    @Test
    public void testCapabilityHandler() throws BGPDocumentedException, BGPParsingException {
        final List<AddressFamilies> family = new ArrayList<>();
        family.add(new AddressFamiliesBuilder().setAfi(AddPathCapabilityHandlerTest.AFI)
            .setSafi(AddPathCapabilityHandlerTest.SAFI).setSendReceive(SendReceive.forValue(1)).build());

        final CParameters capabilityToSerialize = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                    .setAddPathCapability(new AddPathCapabilityBuilder().setAddressFamilies(family).build())
                    .build())
                .build();

        final ByteBuf bytes = Unpooled.buffer(6);
        final AddPathCapabilityHandler handler = new AddPathCapabilityHandler(this.afiRegistry, this.safiRegistry);
        handler.serializeCapability(capabilityToSerialize, bytes);
        assertArrayEquals(this.serializedBytes, bytes.array());

        final CParameters newCaps = handler.parseCapability(this.parseCorrectBytes);
        assertEquals(capabilityToSerialize.hashCode(), newCaps.hashCode());
    }

    @Test(expected = BGPParsingException.class)
    public void testAfiException() throws BGPDocumentedException, BGPParsingException {
        final ByteBuf bytes = this.parseWrongBytes.copy();
        final AddPathCapabilityHandler handler = new AddPathCapabilityHandler(this.afirExpection, this.safiRegistry);
        handler.parseCapability(bytes);
    }

    @Test(expected = BGPParsingException.class)
    public void testSafiException() throws BGPDocumentedException, BGPParsingException {
        final ByteBuf bytes = this.parseWrongBytes.copy();
        final AddPathCapabilityHandler handler = new AddPathCapabilityHandler(this.afiRegistry, this.safirException);
        handler.parseCapability(bytes);
    }

    @Test
    public void testSendReceiveIgnored() throws BGPDocumentedException, BGPParsingException {
        final CParameters capabilityToSerialize = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                    .setAddPathCapability(new AddPathCapabilityBuilder().setAddressFamilies(new ArrayList<>()).build())
                    .build())
                .build();

        final ByteBuf bytes = this.parseWrongBytes.copy();
        final AddPathCapabilityHandler handler = new AddPathCapabilityHandler(this.afiRegistry, this.safiRegistry);
        final CParameters newCaps = handler.parseCapability(bytes);
        assertEquals(capabilityToSerialize.hashCode(), newCaps.hashCode());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnhandledAfi() {
        final List<AddressFamilies> family = new ArrayList<>();
        family.add(new AddressFamiliesBuilder().setAfi(AddPathCapabilityHandlerTest.AFI)
            .setSafi(AddPathCapabilityHandlerTest.SAFI).setSendReceive(SendReceive.forValue(2)).build());

        final CParameters capabilityToSerialize = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                    .setAddPathCapability(new AddPathCapabilityBuilder().setAddressFamilies(family).build())
                    .build())
                .build();

        final ByteBuf bytes = Unpooled.buffer();
        final AddPathCapabilityHandler handler = new AddPathCapabilityHandler(this.afirExpection, this.safiRegistry);
        handler.serializeCapability(capabilityToSerialize, bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnhandledSafi() {
        final List<AddressFamilies> family = new ArrayList<>();
        family.add(new AddressFamiliesBuilder().setAfi(AddPathCapabilityHandlerTest.AFI)
            .setSafi(AddPathCapabilityHandlerTest.SAFI).setSendReceive(SendReceive.forValue(3)).build());

        final CParameters capabilityToSerialize = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                    .setAddPathCapability(new AddPathCapabilityBuilder().setAddressFamilies(family).build())
                    .build())
                .build();

        final ByteBuf bytes = Unpooled.buffer();
        final AddPathCapabilityHandler handler = new AddPathCapabilityHandler(this.afiRegistry, this.safirException);
        handler.serializeCapability(capabilityToSerialize, bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnhandledSendReceive() {
        final List<AddressFamilies> family = new ArrayList<>();
        family.add(new AddressFamiliesBuilder().setAfi(AddPathCapabilityHandlerTest.AFI)
            .setSafi(AddPathCapabilityHandlerTest.SAFI).setSendReceive(SendReceive.forValue(4)).build());

        final CParameters capabilityToSerialize = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder()
                    .setAddPathCapability(new AddPathCapabilityBuilder().setAddressFamilies(family).build())
                    .build())
                .build();

        final ByteBuf bytes = Unpooled.buffer();
        final AddPathCapabilityHandler handler = new AddPathCapabilityHandler(this.afiRegistry, this.safiRegistry);
        handler.serializeCapability(capabilityToSerialize, bytes);
    }

    @Test
    public void noSerializationTest() {
        final CParameters capabilityNoAugmentation = new CParametersBuilder().removeAugmentation(CParameters1.class)
                .build();
        final CParameters capabilityNoMP = new CParametersBuilder().addAugmentation(new CParameters1Builder().build())
                .build();

        final ByteBuf bytes = Unpooled.buffer();
        final AddPathCapabilityHandler handler = new AddPathCapabilityHandler(this.afiRegistry, this.safirException);
        handler.serializeCapability(capabilityNoAugmentation, bytes);
        assertEquals(0, bytes.readableBytes());
        handler.serializeCapability(capabilityNoMP, bytes);
        assertEquals(0, bytes.readableBytes());
    }
}
