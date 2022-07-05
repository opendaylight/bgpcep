/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
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
import java.util.ServiceLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.LlGracefulCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.types.rev171204.Uint24;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class LlGracefulCapabilityHandlerTest {
    private static final Uint24 TEN = new Uint24(Uint32.TEN);

    private LlGracefulCapabilityHandler handler;

    @Mock
    private AddressFamilyRegistry afir;
    @Mock
    private SubsequentAddressFamilyRegistry safir;

    @Before
    public void setUp() {
        doReturn(Ipv4AddressFamily.class).when(afir).classForFamily(1);
        doReturn(Ipv6AddressFamily.class).when(afir).classForFamily(2);
        doReturn(null).when(afir).classForFamily(256);
        doReturn(UnicastSubsequentAddressFamily.class).when(safir).classForFamily(1);
        doReturn(null).when(safir).classForFamily(-123);

        final BGPExtensionConsumerContext ctx = ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst()
            .orElseThrow();
        handler = new LlGracefulCapabilityHandler(
                ctx.getAddressFamilyRegistry(), ctx.getSubsequentAddressFamilyRegistry());
    }

    @Test
    public void testLongLivedGracefulCapabilityHandler() throws BGPParsingException, BGPDocumentedException {

        final byte[] capaBytes = {
            //header
            (byte) 0x47, (byte) 0x0e,
            // Ipv4 Unicast, afiFlags = false, timer = 10
            (byte) 0x00, (byte) 0x01, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a,
            // Ipv6 Unicast, afiFlags = true, timer = 160
            (byte) 0x00, (byte) 0x02, (byte) 0x01,
            (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0xa0
        };

        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(BindingMap.ordered(new TablesBuilder()
                                .setAfi(Ipv4AddressFamily.VALUE)
                                .setSafi(UnicastSubsequentAddressFamily.VALUE)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                                .setLongLivedStaleTime(TEN)
                                .build(),
                        new TablesBuilder()
                                .setAfi(Ipv6AddressFamily.VALUE)
                                .setSafi(UnicastSubsequentAddressFamily.VALUE)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.TRUE))
                                .setLongLivedStaleTime(new Uint24(Uint32.valueOf(160)))
                                .build())
                ).build();

        final CParameters cParameters = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder().setLlGracefulRestartCapability(capability).build())
                .build();
        final ByteBuf buffer = Unpooled.buffer(capaBytes.length);
        handler.serializeCapability(cParameters, buffer);

        assertArrayEquals(capaBytes, buffer.array());
        assertEquals(cParameters, handler.parseCapability(Unpooled.wrappedBuffer(capaBytes)
                .slice(2, capaBytes.length - 2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedAfi() {
        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(BindingMap.of(new TablesBuilder()
                                .setAfi(AddressFamily.VALUE)
                                .setSafi(UnicastSubsequentAddressFamily.VALUE)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                                .setLongLivedStaleTime(TEN)
                                .build())).build();

        final CParameters cParameters = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder().setLlGracefulRestartCapability(capability).build())
                .build();
        final ByteBuf buffer = Unpooled.buffer();
        handler.serializeCapability(cParameters, buffer);
    }

    @Test
    public void testRecvdUnsupportedAfi() {
        final byte[] capaBytes = {
            //header
            (byte) 0x47, (byte) 0x15,
            // Ipv4 Unicast, afiFlags = false, timer = 10
            (byte) 0x00, (byte) 0x01, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a,
            // Unsupported afi, afiFlags = true, timer = 160
            (byte) 0x01, (byte) 0x00, (byte) 0x01,
            (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0xa0,
            //Ipv6  Unicast afiFlags = false, timer = 160
            (byte) 0x00, (byte) 0x02, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xa0
        };
        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(BindingMap.ordered(new TablesBuilder()
                                .setAfi(Ipv4AddressFamily.VALUE)
                                .setSafi(UnicastSubsequentAddressFamily.VALUE)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                                .setLongLivedStaleTime(TEN)
                                .build(),
                        new TablesBuilder()
                                .setAfi(Ipv6AddressFamily.VALUE)
                                .setSafi(UnicastSubsequentAddressFamily.VALUE)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                                .setLongLivedStaleTime(new Uint24(Uint32.valueOf(160)))
                                .build())).build();

        final CParameters cParameters = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder().setLlGracefulRestartCapability(capability).build())
                .build();
        LlGracefulCapabilityHandler handler1 = new LlGracefulCapabilityHandler(
                afir, safir);
        assertEquals(cParameters, handler1.parseCapability(Unpooled.wrappedBuffer(capaBytes)
                .slice(2, capaBytes.length - 2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedSafi() {
        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(BindingMap.of(new TablesBuilder()
                        .setAfi(Ipv4AddressFamily.VALUE)
                        .setSafi(SubsequentAddressFamily.VALUE)
                        .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                        .setLongLivedStaleTime(TEN)
                        .build())).build();

        final CParameters cParameters = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder().setLlGracefulRestartCapability(capability).build())
                .build();
        final ByteBuf buffer = Unpooled.buffer();
        handler.serializeCapability(cParameters, buffer);
    }

    @Test
    public void testRecvdUnsupportedSafi() {
        final byte[] capaBytes = {
            //header
            (byte) 0x47, (byte) 0x15,
            // Ipv4 Unicast, afiFlags = false, timer = 10
            (byte) 0x00, (byte) 0x01, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0a,
            // Unsupported safi, afiFlags = true, timer = 160
            (byte) 0x00, (byte) 0x01, (byte) 0x85,
            (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0xa0,
            //Ipv6  Unicast afiFlags = false, timer = 160
            (byte) 0x00, (byte) 0x02, (byte) 0x01,
            (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0xa0
        };

        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(BindingMap.ordered(new TablesBuilder()
                                .setAfi(Ipv4AddressFamily.VALUE)
                                .setSafi(UnicastSubsequentAddressFamily.VALUE)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                                .setLongLivedStaleTime(TEN)
                                .build(),
                        new TablesBuilder()
                                .setAfi(Ipv6AddressFamily.VALUE)
                                .setSafi(UnicastSubsequentAddressFamily.VALUE)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.TRUE))
                                .setLongLivedStaleTime(new Uint24(Uint32.valueOf(160)))
                                .build())).build();

        final CParameters cParameters = new CParametersBuilder()
                .addAugmentation(new CParameters1Builder().setLlGracefulRestartCapability(capability).build())
                .build();
        LlGracefulCapabilityHandler handler1 = new LlGracefulCapabilityHandler(
                afir, safir);
        assertEquals(cParameters, handler1.parseCapability(Unpooled.wrappedBuffer(capaBytes)
                .slice(2, capaBytes.length - 2)));
    }
}
