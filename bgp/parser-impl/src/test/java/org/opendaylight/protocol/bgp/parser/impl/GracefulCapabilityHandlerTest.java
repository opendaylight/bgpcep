/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.GracefulCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.GracefulRestartCapability.RestartFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.graceful.restart.capability.Tables.AfiFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.mp.capabilities.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class GracefulCapabilityHandlerTest {

    private BGPExtensionProviderContext ctx;

    @Before
    public void setUp() {
        this.ctx = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
    }

    @Test
    public void testGracefulCapabilityHandler() throws BGPDocumentedException, BGPParsingException {
        final GracefulCapabilityHandler handler = new GracefulCapabilityHandler(
            this.ctx.getAddressFamilyRegistry(),this.ctx.getSubsequentAddressFamilyRegistry());

        final byte[] capaBytes = {
            (byte) 0x40, (byte) 0x06, (byte) 0x81, (byte) 0xf4,
            (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x80
        };

        final GracefulRestartCapabilityBuilder capaBuilder = new GracefulRestartCapabilityBuilder();
        capaBuilder.setRestartFlags(new RestartFlags(true));
        capaBuilder.setRestartTime(500);
        final TablesBuilder tablesBuilder = new TablesBuilder();
        tablesBuilder.setAfiFlags(new AfiFlags(true));
        tablesBuilder.setAfi(Ipv4AddressFamily.class);
        tablesBuilder.setSafi(UnicastSubsequentAddressFamily.class);
        capaBuilder.setTables(Lists.newArrayList(tablesBuilder.build()));

        final ByteBuf buffer = Unpooled.buffer(capaBytes.length);
        handler.serializeCapability( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(), buffer);
        Assert.assertArrayEquals(capaBytes, buffer.array());

        Assert.assertEquals( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(),
            handler.parseCapability(Unpooled.wrappedBuffer(capaBytes).slice(2, capaBytes.length - 2)));

        final byte[] capaBytes2 = {
            (byte) 0x40, (byte) 0x06, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00
        };
        capaBuilder.setRestartFlags(new RestartFlags(false));
        capaBuilder.setRestartTime(0);
        tablesBuilder.setAfiFlags(new AfiFlags(false));
        capaBuilder.setTables(Lists.newArrayList(tablesBuilder.build()));
        buffer.clear();
        handler.serializeCapability( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(), buffer);
        Assert.assertArrayEquals(capaBytes2, buffer.array());
        Assert.assertEquals( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(),
            handler.parseCapability(Unpooled.wrappedBuffer(capaBytes2).slice(2, capaBytes2.length - 2)));

        capaBuilder.setRestartFlags(null);
        tablesBuilder.setAfiFlags(null);
        capaBuilder.setRestartTime(null);
        capaBuilder.setTables(Lists.newArrayList(tablesBuilder.build()));
        buffer.clear();
        handler.serializeCapability( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(), buffer);
        Assert.assertArrayEquals(capaBytes2, buffer.array());

        final byte[] capaBytes3 = {
            (byte) 0x40, (byte) 0x06, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };
        capaBuilder.setRestartFlags(new RestartFlags(false));
        capaBuilder.setRestartTime(0);
        capaBuilder.setTables(Collections.emptyList());
        Assert.assertEquals( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(),
            handler.parseCapability(Unpooled.wrappedBuffer(capaBytes3).slice(2, capaBytes3.length - 2)));

        final byte[] capaBytes4 = {
            (byte) 0x40, (byte) 0x06, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00
        };
        Assert.assertEquals( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(),
            handler.parseCapability(Unpooled.wrappedBuffer(capaBytes4).slice(2, capaBytes4.length - 2)));
        final byte[] capaBytes5 = {
            (byte) 0x40, (byte) 0x02, (byte) 0x00, (byte) 0x00
        };
        final ByteBuf b = Unpooled.buffer(capaBytes5.length);

        handler.serializeCapability( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(new GracefulRestartCapabilityBuilder().build()).build()).build(), b);
        Assert.assertArrayEquals(capaBytes5, b.array());
        Assert.assertEquals(new CParametersBuilder().addAugmentation(CParameters1.class, new CParameters1Builder()
            .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder().setRestartFlags(new RestartFlags(Boolean.FALSE))
                .setRestartTime(0).setTables(Collections.emptyList()).build()).build()).build(),
            handler.parseCapability(Unpooled.wrappedBuffer(capaBytes5).slice(2, capaBytes5.length - 2)));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnhandledAfi() {
        final GracefulCapabilityHandler handler = new GracefulCapabilityHandler(this.ctx.getAddressFamilyRegistry(),
            this.ctx.getSubsequentAddressFamilyRegistry());

        final GracefulRestartCapabilityBuilder capaBuilder = new GracefulRestartCapabilityBuilder();
        capaBuilder.setRestartFlags(new RestartFlags(true));
        capaBuilder.setRestartTime(50);
        final TablesBuilder tablesBuilder = new TablesBuilder();
        tablesBuilder.setAfiFlags(new AfiFlags(true));

        tablesBuilder.setAfi(AddressFamily.class);
        tablesBuilder.setSafi(UnicastSubsequentAddressFamily.class);
        capaBuilder.setTables(Lists.newArrayList(tablesBuilder.build()));

        final ByteBuf buffer = Unpooled.buffer();
        handler.serializeCapability( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(), buffer);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUnhandledSafi() {
        final GracefulCapabilityHandler handler = new GracefulCapabilityHandler(this.ctx.getAddressFamilyRegistry(),
            this.ctx.getSubsequentAddressFamilyRegistry());

        final GracefulRestartCapabilityBuilder capaBuilder = new GracefulRestartCapabilityBuilder();
        capaBuilder.setRestartFlags(new RestartFlags(true));
        capaBuilder.setRestartTime(50);
        final TablesBuilder tablesBuilder = new TablesBuilder();
        tablesBuilder.setAfiFlags(new AfiFlags(true));
        tablesBuilder.setAfi(Ipv4AddressFamily.class);
        tablesBuilder.setSafi(SubsequentAddressFamily.class);
        capaBuilder.setTables(Lists.newArrayList(tablesBuilder.build()));

        final ByteBuf buffer = Unpooled.buffer();
        handler.serializeCapability( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(), buffer);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRestartTimeMinValue() {
        final GracefulCapabilityHandler handler = new GracefulCapabilityHandler(this.ctx.getAddressFamilyRegistry(),
            this.ctx.getSubsequentAddressFamilyRegistry());

        final GracefulRestartCapabilityBuilder capaBuilder = new GracefulRestartCapabilityBuilder();
        capaBuilder.setRestartFlags(new RestartFlags(true));
        capaBuilder.setRestartTime(-1);
        final TablesBuilder tablesBuilder = new TablesBuilder();
        tablesBuilder.setAfiFlags(new AfiFlags(true));
        tablesBuilder.setAfi(Ipv4AddressFamily.class);
        tablesBuilder.setSafi(UnicastSubsequentAddressFamily.class);
        capaBuilder.setTables(Lists.newArrayList(tablesBuilder.build()));

        final ByteBuf buffer = Unpooled.buffer();
        handler.serializeCapability( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(), buffer);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRestartTimeMaxValue() {
        final GracefulCapabilityHandler handler = new GracefulCapabilityHandler(this.ctx.getAddressFamilyRegistry(),
            this.ctx.getSubsequentAddressFamilyRegistry());

        final GracefulRestartCapabilityBuilder capaBuilder = new GracefulRestartCapabilityBuilder();
        capaBuilder.setRestartFlags(new RestartFlags(true));
        capaBuilder.setRestartTime(50 * 1000);
        final TablesBuilder tablesBuilder = new TablesBuilder();
        tablesBuilder.setAfiFlags(new AfiFlags(true));
        tablesBuilder.setAfi(Ipv4AddressFamily.class);
        tablesBuilder.setSafi(UnicastSubsequentAddressFamily.class);
        capaBuilder.setTables(Lists.newArrayList(tablesBuilder.build()));

        final ByteBuf buffer = Unpooled.buffer();
        handler.serializeCapability( new CParametersBuilder().addAugmentation(CParameters1.class,
            new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build()).build(), buffer);
    }

}
