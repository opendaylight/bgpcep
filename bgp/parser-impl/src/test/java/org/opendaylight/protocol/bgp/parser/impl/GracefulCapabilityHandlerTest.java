/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.GracefulCapabilityHandler;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.open.message.bgp.parameters.optional.capabilities.CParametersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.CParameters1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapability.RestartFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.GracefulRestartCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.Tables.AfiFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.mp.capabilities.graceful.restart.capability.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint16;

public class GracefulCapabilityHandlerTest {
    private final BGPExtensionConsumerContext ctx = ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst()
        .orElseThrow();

    @Test
    public void testGracefulCapabilityHandler() throws BGPDocumentedException, BGPParsingException {
        final GracefulCapabilityHandler handler = new GracefulCapabilityHandler(
            ctx.getAddressFamilyRegistry(),ctx.getSubsequentAddressFamilyRegistry());

        final byte[] capaBytes = {
            (byte) 0x40, (byte) 0x06, (byte) 0x81, (byte) 0xf4,
            (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x80
        };

        final GracefulRestartCapabilityBuilder capaBuilder = new GracefulRestartCapabilityBuilder()
                .setRestartFlags(new RestartFlags(true))
                .setRestartTime(Uint16.valueOf(500));
        final TablesBuilder tablesBuilder = new TablesBuilder()
                .setAfiFlags(new AfiFlags(true))
                .setAfi(Ipv4AddressFamily.VALUE)
                .setSafi(UnicastSubsequentAddressFamily.VALUE);
        capaBuilder.setTables(BindingMap.of(tablesBuilder.build()));

        final ByteBuf buffer = Unpooled.buffer(capaBytes.length);
        handler.serializeCapability(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), buffer);
        assertArrayEquals(capaBytes, buffer.array());

        assertEquals(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), handler.parseCapability(Unpooled.wrappedBuffer(capaBytes).slice(2, capaBytes.length - 2)));

        final byte[] capaBytes2 = {
            (byte) 0x40, (byte) 0x06, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00
        };
        capaBuilder.setRestartFlags(new RestartFlags(false));
        capaBuilder.setRestartTime(Uint16.ZERO);
        tablesBuilder.setAfiFlags(new AfiFlags(false));
        capaBuilder.setTables(BindingMap.of(tablesBuilder.build()));
        buffer.clear();
        handler.serializeCapability(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), buffer);
        assertArrayEquals(capaBytes2, buffer.array());
        assertEquals(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), handler.parseCapability(Unpooled.wrappedBuffer(capaBytes2).slice(2, capaBytes2.length - 2)));

        capaBuilder.setRestartFlags(null);
        tablesBuilder.setAfiFlags(null);
        capaBuilder.setRestartTime((Uint16) null);
        capaBuilder.setTables(BindingMap.of(tablesBuilder.build()));
        buffer.clear();
        handler.serializeCapability(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), buffer);
        assertArrayEquals(capaBytes2, buffer.array());

        final byte[] capaBytes3 = {
            (byte) 0x40, (byte) 0x06, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };
        capaBuilder.setRestartFlags(new RestartFlags(false));
        capaBuilder.setRestartTime(Uint16.ZERO);
        capaBuilder.setTables(Map.of());
        assertEquals(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), handler.parseCapability(Unpooled.wrappedBuffer(capaBytes3).slice(2, capaBytes3.length - 2)));

        final byte[] capaBytes4 = {
            (byte) 0x40, (byte) 0x06, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00
        };
        assertEquals(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), handler.parseCapability(Unpooled.wrappedBuffer(capaBytes4).slice(2, capaBytes4.length - 2)));
        final byte[] capaBytes5 = {
            (byte) 0x40, (byte) 0x02, (byte) 0x00, (byte) 0x00
        };
        final ByteBuf b = Unpooled.buffer(capaBytes5.length);

        handler.serializeCapability(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder()
                .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder().build())
                .build())
            .build(), b);
        assertArrayEquals(capaBytes5, b.array());
        assertEquals(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder()
                .setGracefulRestartCapability(new GracefulRestartCapabilityBuilder()
                    .setRestartFlags(new RestartFlags(Boolean.FALSE))
                    .setRestartTime(Uint16.ZERO)
                    .build())
                .build())
            .build(), handler.parseCapability(Unpooled.wrappedBuffer(capaBytes5).slice(2, capaBytes5.length - 2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnhandledAfi() {
        final GracefulCapabilityHandler handler = new GracefulCapabilityHandler(ctx.getAddressFamilyRegistry(),
            ctx.getSubsequentAddressFamilyRegistry());

        final GracefulRestartCapabilityBuilder capaBuilder = new GracefulRestartCapabilityBuilder();
        capaBuilder.setRestartFlags(new RestartFlags(true));
        capaBuilder.setRestartTime(Uint16.valueOf(50));
        capaBuilder.setTables(BindingMap.of(new TablesBuilder()
            .setAfiFlags(new AfiFlags(true))
            .setAfi(AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE)
            .build()));

        final ByteBuf buffer = Unpooled.buffer();
        handler.serializeCapability(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), buffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnhandledSafi() {
        final GracefulCapabilityHandler handler = new GracefulCapabilityHandler(ctx.getAddressFamilyRegistry(),
            ctx.getSubsequentAddressFamilyRegistry());

        final GracefulRestartCapabilityBuilder capaBuilder = new GracefulRestartCapabilityBuilder();
        capaBuilder.setRestartFlags(new RestartFlags(true));
        capaBuilder.setRestartTime(Uint16.valueOf(50));
        capaBuilder.setTables(BindingMap.of(new TablesBuilder()
            .setAfiFlags(new AfiFlags(true))
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(SubsequentAddressFamily.VALUE)
            .build()));

        final ByteBuf buffer = Unpooled.buffer();
        handler.serializeCapability(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), buffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRestartTimeMinValue() {
        final GracefulCapabilityHandler handler = new GracefulCapabilityHandler(ctx.getAddressFamilyRegistry(),
            ctx.getSubsequentAddressFamilyRegistry());

        final GracefulRestartCapabilityBuilder capaBuilder = new GracefulRestartCapabilityBuilder()
                .setRestartFlags(new RestartFlags(true))
                // FIXME: this is throwing IAE, why is the rest of the test even here?
                .setRestartTime(Uint16.MAX_VALUE)
                .setTables(BindingMap.of(new TablesBuilder()
                    .setAfiFlags(new AfiFlags(true))
                    .setAfi(Ipv4AddressFamily.VALUE)
                    .setSafi(UnicastSubsequentAddressFamily.VALUE)
                    .build()));

        final ByteBuf buffer = Unpooled.buffer();
        handler.serializeCapability(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), buffer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRestartTimeMaxValue() {
        final GracefulCapabilityHandler handler = new GracefulCapabilityHandler(ctx.getAddressFamilyRegistry(),
            ctx.getSubsequentAddressFamilyRegistry());

        final GracefulRestartCapabilityBuilder capaBuilder = new GracefulRestartCapabilityBuilder()
                .setRestartFlags(new RestartFlags(true))
                .setRestartTime(Uint16.valueOf(50000));

        capaBuilder.setTables(BindingMap.of(new TablesBuilder()
            .setAfiFlags(new AfiFlags(true))
            .setAfi(Ipv4AddressFamily.VALUE)
            .setSafi(UnicastSubsequentAddressFamily.VALUE)
            .build()));

        final ByteBuf buffer = Unpooled.buffer();
        handler.serializeCapability(new CParametersBuilder()
            .addAugmentation(new CParameters1Builder().setGracefulRestartCapability(capaBuilder.build()).build())
            .build(), buffer);
    }
}
