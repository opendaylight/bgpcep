/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.pcep.parser.BaseParserExtensionActivator;
import org.opendaylight.protocol.pcep.parser.subobject.RROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROPathKey128SubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROPathKey32SubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.rro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.GeneralizedLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.generalized.label._case.GeneralizedLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.label._case.LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;

public class PCEPRROSubobjectParserTest {

    private static final byte[] IP4_PREFIX_BYTES = {
        (byte) 0x01, (byte) 0x08, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x16, (byte) 0x01
    };
    private static final byte[] IP6_PREFIX_BYTES = {
        (byte) 0x02, (byte) 0x14, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0x16, (byte) 0x02
    };
    private static final byte[] UNNUMBERED_BYTES = {
        (byte) 0x04, (byte) 0x0c, (byte) 0x02, (byte) 0x00, (byte) 0x12, (byte) 0x34,
        (byte) 0x50, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    };
    private static final byte[] PATH_KEY32_BYTES = {
        (byte) 0x40, (byte) 0x08, (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00
    };
    private static final byte[] PATH_KEY128_BYTES = {
        (byte) 0x41, (byte) 0x14, (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
        (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };
    private static final byte[] LABEL_BYTES = { 0x03, 0x08, (byte) 0x80, 0x02, 0x12, 0x00, 0x25, (byte) 0xFF };

    @Test
    public void testRROIp4PrefixSubobject() throws PCEPDeserializerException {
        final RROIpv4PrefixSubobjectParser parser = new RROIpv4PrefixSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setProtectionAvailable(true);
        subs.setProtectionInUse(false);
        subs.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
                new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.255/22"))).build()).build());
        assertEquals(
            subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(IP4_PREFIX_BYTES, 2))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(IP4_PREFIX_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRROIp6PrefixSubobject() throws PCEPDeserializerException {
        final RROIpv6PrefixSubobjectParser parser = new RROIpv6PrefixSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setProtectionAvailable(false);
        subs.setProtectionInUse(true);
        subs.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
            new IpPrefixBuilder().setIpPrefix(new IpPrefix(Ipv6Util.prefixForBytes(new byte[] {
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
            },
            22))).build()).build());
        assertEquals(
            subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(IP6_PREFIX_BYTES, 2))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(IP6_PREFIX_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRROUnnumberedSubobject() throws PCEPDeserializerException {
        final RROUnnumberedInterfaceSubobjectParser parser = new RROUnnumberedInterfaceSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setProtectionAvailable(false);
        subs.setProtectionInUse(true);
        subs.setSubobjectType(new UnnumberedCaseBuilder().setUnnumbered(
                new UnnumberedBuilder().setRouterId(0x12345000L).setInterfaceId(0xffffffffL).build()).build());
        assertEquals(
            subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(UNNUMBERED_BYTES, 2))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(UNNUMBERED_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRROPathKey32Subobject() throws PCEPDeserializerException {
        final RROPathKey32SubobjectParser parser = new RROPathKey32SubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 }));
        pBuilder.setPathKey(new PathKey(4660));
        subs.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        assertEquals(
            subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(PATH_KEY32_BYTES, 2))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(PATH_KEY32_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRROPathKey128Subobject() throws PCEPDeserializerException {
        final RROPathKey128SubobjectParser parser = new RROPathKey128SubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(new byte[] {
            (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0x12,
            (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        }));
        pBuilder.setPathKey(new PathKey(4660));
        subs.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        assertEquals(
            subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(PATH_KEY128_BYTES, 2))));
        final ByteBuf buff = Unpooled.buffer();
        RROPathKey128SubobjectParser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(PATH_KEY128_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRROLabelSubobject() throws Exception {
        final SimplePCEPExtensionProviderContext ctx = new SimplePCEPExtensionProviderContext();
        try (BaseParserExtensionActivator a = new BaseParserExtensionActivator()) {
            a.start(ctx);
            final RROLabelSubobjectParser parser = new RROLabelSubobjectParser(ctx.getLabelHandlerRegistry());
            final SubobjectBuilder subs = new SubobjectBuilder();
            subs.setSubobjectType(new LabelCaseBuilder().setLabel(
                new LabelBuilder().setUniDirectional(true).setGlobal(false).setLabelType(
                    new GeneralizedLabelCaseBuilder().setGeneralizedLabel(
                       new GeneralizedLabelBuilder().setGeneralizedLabel(
                            new byte[] { (byte) 0x12, (byte) 0x00, (byte) 0x25, (byte) 0xFF })
                       .build()).build()).build()).build());
            assertEquals(
                subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(LABEL_BYTES, 2))));
            final ByteBuf buff = Unpooled.buffer();
            parser.serializeSubobject(subs.build(), buff);
            assertArrayEquals(LABEL_BYTES, ByteArray.getAllBytes(buff));

            try {
                parser.parseSubobject(null);
                fail();
            } catch (final IllegalArgumentException e) {
                assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
            }
            try {
                parser.parseSubobject(Unpooled.EMPTY_BUFFER);
                fail();
            } catch (final IllegalArgumentException e) {
                assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
            }
        }
    }
}
