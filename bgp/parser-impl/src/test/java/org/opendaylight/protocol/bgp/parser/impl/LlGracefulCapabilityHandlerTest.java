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
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.LlGracefulCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.LlGracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.ll.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;

public class LlGracefulCapabilityHandlerTest {

    private LlGracefulCapabilityHandler handler;

    @Before
    public void setUp() {
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
                                .setAfiFlags(new Tables.AfiFlags(false))
                                .setLongLiveStaleTime(10L)
                                .build(),
                        new TablesBuilder()
                                .setAfi(Ipv6AddressFamily.class)
                                .setSafi(UnicastSubsequentAddressFamily.class)
                                .setAfiFlags(new Tables.AfiFlags(true))
                                .setLongLiveStaleTime(160L)
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

    @Test(expected=IllegalArgumentException.class)
    public void testUnsupportedAfi() {
        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(Arrays.asList(new TablesBuilder()
                                .setAfi(AddressFamily.class)
                                .setSafi(UnicastSubsequentAddressFamily.class)
                                .setAfiFlags(new Tables.AfiFlags(false))
                                .setLongLiveStaleTime(10L)
                                .build())).build();

        final CParameters cParameters = new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setLlGracefulRestartCapability(capability).build()).build();
        final ByteBuf buffer = Unpooled.buffer();
        this.handler.serializeCapability(cParameters, buffer);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnsupportedSafi() {
        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(Arrays.asList(new TablesBuilder()
                        .setAfi(Ipv4AddressFamily.class)
                        .setSafi(SubsequentAddressFamily.class)
                        .setAfiFlags(new Tables.AfiFlags(false))
                        .setLongLiveStaleTime(10L)
                        .build())).build();

        final CParameters cParameters = new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setLlGracefulRestartCapability(capability).build()).build();
        final ByteBuf buffer = Unpooled.buffer();
        this.handler.serializeCapability(cParameters, buffer);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMinStaleTime() {
        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(Arrays.asList(new TablesBuilder()
                                .setAfi(Ipv4AddressFamily.class)
                                .setSafi(UnicastSubsequentAddressFamily.class)
                                .setAfiFlags(new Tables.AfiFlags(false))
                                .setLongLiveStaleTime(-1L)
                                .build())).build();

        final CParameters cParameters = new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setLlGracefulRestartCapability(capability).build()).build();
        final ByteBuf buffer = Unpooled.buffer();
        this.handler.serializeCapability(cParameters, buffer);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMaxStaleTime() {

        final LlGracefulRestartCapability capability = new LlGracefulRestartCapabilityBuilder()
                .setTables(Arrays.asList(new TablesBuilder()
                        .setAfi(Ipv4AddressFamily.class)
                        .setSafi(UnicastSubsequentAddressFamily.class)
                        .setAfiFlags(new Tables.AfiFlags(false))
                        .setLongLiveStaleTime(16777216L)
                        .build())).build();

        final CParameters cParameters = new CParametersBuilder().addAugmentation(CParameters1.class,
                new CParameters1Builder().setLlGracefulRestartCapability(capability).build()).build();
        final ByteBuf buffer = Unpooled.buffer();
        this.handler.serializeCapability(cParameters, buffer);
    }
}
