/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.LlGracefulCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.uint24.rev200104.Uint24;
import org.opendaylight.yangtools.yang.common.Uint32;

public class LlGracefulCapabilityHandlerTest {
    private static final Uint24 TEN = new Uint24(Uint32.TEN);

    private LlGracefulCapabilityHandler handler;

    @Mock
    private AddressFamilyRegistry afir;
    @Mock
    private SubsequentAddressFamilyRegistry safir;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(Ipv4AddressFamily.class).when(this.afir).classForFamily(1);
        Mockito.doReturn(Ipv6AddressFamily.class).when(this.afir).classForFamily(2);
        Mockito.doReturn(null).when(this.afir).classForFamily(256);
        Mockito.doReturn(UnicastSubsequentAddressFamily.class).when(this.safir).classForFamily(1);
        Mockito.doReturn(null).when(this.safir).classForFamily(-123);

        final BGPExtensionProviderContext ctx = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
        this.handler = new LlGracefulCapabilityHandler(
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
                .setTables(Arrays.asList(new TablesBuilder()
                                .setAfi(Ipv4AddressFamily.class)
                                .setSafi(UnicastSubsequentAddressFamily.class)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                                .setLongLivedStaleTime(TEN)
                                .build(),
                        new TablesBuilder()
                                .setAfi(Ipv6AddressFamily.class)
                                .setSafi(UnicastSubsequentAddressFamily.class)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.TRUE))
                                .setLongLivedStaleTime(new Uint24(Uint32.valueOf(160)))
                                .build())
                ).build();

        final CParameters cParameters = new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setLlGracefulRestartCapability(capability).build()).build();
        final ByteBuf buffer = Unpooled.buffer(capaBytes.length);
        this.handler.serializeCapability(cParameters, buffer);

        Assert.assertArrayEquals(capaBytes, buffer.array());
        Assert.assertEquals(cParameters, this.handler.parseCapability(Unpooled.wrappedBuffer(capaBytes)
                .slice(2, capaBytes.length - 2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedAfi() {
        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(Arrays.asList(new TablesBuilder()
                                .setAfi(AddressFamily.class)
                                .setSafi(UnicastSubsequentAddressFamily.class)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                                .setLongLivedStaleTime(TEN)
                                .build())).build();

        final CParameters cParameters = new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setLlGracefulRestartCapability(capability).build()).build();
        final ByteBuf buffer = Unpooled.buffer();
        this.handler.serializeCapability(cParameters, buffer);
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
                .setTables(Arrays.asList(new TablesBuilder()
                                .setAfi(Ipv4AddressFamily.class)
                                .setSafi(UnicastSubsequentAddressFamily.class)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                                .setLongLivedStaleTime(TEN)
                                .build(),
                        new TablesBuilder()
                                .setAfi(Ipv6AddressFamily.class)
                                .setSafi(UnicastSubsequentAddressFamily.class)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                                .setLongLivedStaleTime(new Uint24(Uint32.valueOf(160)))
                                .build())).build();

        final CParameters cParameters = new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setLlGracefulRestartCapability(capability).build()).build();
        LlGracefulCapabilityHandler handler1 = new LlGracefulCapabilityHandler(
                afir, safir);
        Assert.assertEquals(cParameters, handler1.parseCapability(Unpooled.wrappedBuffer(capaBytes)
                .slice(2, capaBytes.length - 2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedSafi() {
        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(Arrays.asList(new TablesBuilder()
                        .setAfi(Ipv4AddressFamily.class)
                        .setSafi(SubsequentAddressFamily.class)
                        .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                        .setLongLivedStaleTime(TEN)
                        .build())).build();

        final CParameters cParameters = new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setLlGracefulRestartCapability(capability).build()).build();
        final ByteBuf buffer = Unpooled.buffer();
        this.handler.serializeCapability(cParameters, buffer);
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
                .setTables(Arrays.asList(new TablesBuilder()
                                .setAfi(Ipv4AddressFamily.class)
                                .setSafi(UnicastSubsequentAddressFamily.class)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.FALSE))
                                .setLongLivedStaleTime(TEN)
                                .build(),
                        new TablesBuilder()
                                .setAfi(Ipv6AddressFamily.class)
                                .setSafi(UnicastSubsequentAddressFamily.class)
                                .setAfiFlags(new Tables.AfiFlags(Boolean.TRUE))
                                .setLongLivedStaleTime(new Uint24(Uint32.valueOf(160)))
                                .build())).build();

        final CParameters cParameters = new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setLlGracefulRestartCapability(capability).build()).build();
        LlGracefulCapabilityHandler handler1 = new LlGracefulCapabilityHandler(
                afir, safir);
        Assert.assertEquals(cParameters, handler1.parseCapability(Unpooled.wrappedBuffer(capaBytes)
                .slice(2, capaBytes.length - 2)));
    }
}
