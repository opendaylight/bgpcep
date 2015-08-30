/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.rsvp.parser.impl;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROLabelSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROPathKey128SubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROPathKey32SubobjectParser;
import org.opendaylight.protocol.rsvp.parser.impl.subobject.rro.RROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.rsvp.parser.spi.RSVPParsingException;
import org.opendaylight.protocol.rsvp.parser.spi.pojo.SimpleRSVPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.GeneralizedLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.generalized.label._case.GeneralizedLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.list.SubobjectContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.label._case.LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.record.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;

public class RROSubobjectParserTest {

    private static final byte[] ip4PrefixBytes = {(byte) 0x01, (byte) 0x08, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x16, (byte) 0x01};
    private static final byte[] ip6PrefixBytes = {(byte) 0x02, (byte) 0x14, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0x16, (byte) 0x02};
    private static final byte[] unnumberedBytes = {(byte) 0x04, (byte) 0x0c, (byte) 0x02, (byte) 0x00, (byte) 0x12, (byte) 0x34,
        (byte) 0x50, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private static final byte[] pathKey32Bytes = {(byte) 0x40, (byte) 0x08, (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34,
        (byte) 0x50, (byte) 0x00};
    private static final byte[] pathKey128Bytes = {(byte) 0x41, (byte) 0x14, (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34,
        (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    private static final byte[] labelBytes = {0x03, 0x08, (byte) 0x80, 0x02, 0x12, 0x00, 0x25, (byte) 0xFF};

    @Test
    public void testRROIp4PrefixSubobject() throws RSVPParsingException {
        final RROIpv4PrefixSubobjectParser parser = new RROIpv4PrefixSubobjectParser();
        final SubobjectContainerBuilder subs = new SubobjectContainerBuilder();
        subs.setProtectionAvailable(true);
        subs.setProtectionInUse(false);
        subs.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
            new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.255/22"))).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(ip4PrefixBytes, 2))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        Assert.assertArrayEquals(ip4PrefixBytes, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRROIp6PrefixSubobject() throws RSVPParsingException {
        final RROIpv6PrefixSubobjectParser parser = new RROIpv6PrefixSubobjectParser();
        final SubobjectContainerBuilder subs = new SubobjectContainerBuilder();
        subs.setProtectionAvailable(false);
        subs.setProtectionInUse(true);
        subs.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
            new IpPrefixBuilder().setIpPrefix(
                new IpPrefix(Ipv6Util.prefixForBytes(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, 22))).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(ip6PrefixBytes, 2))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        Assert.assertArrayEquals(ip6PrefixBytes, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRROUnnumberedSubobject() throws RSVPParsingException {
        final RROUnnumberedInterfaceSubobjectParser parser = new RROUnnumberedInterfaceSubobjectParser();
        final SubobjectContainerBuilder subs = new SubobjectContainerBuilder();
        subs.setProtectionAvailable(false);
        subs.setProtectionInUse(true);
        subs.setSubobjectType(new UnnumberedCaseBuilder().setUnnumbered(
            new UnnumberedBuilder().setRouterId(0x12345000L).setInterfaceId(0xffffffffL).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(unnumberedBytes, 2))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        Assert.assertArrayEquals(unnumberedBytes, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRROPathKey32Subobject() throws RSVPParsingException {
        final RROPathKey32SubobjectParser parser = new RROPathKey32SubobjectParser();
        final SubobjectContainerBuilder subs = new SubobjectContainerBuilder();
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(new byte[]{(byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00}));
        pBuilder.setPathKey(new PathKey(4660));
        subs.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(pathKey32Bytes, 2))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        Assert.assertArrayEquals(pathKey32Bytes, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRROPathKey128Subobject() throws RSVPParsingException {
        final RROPathKey128SubobjectParser parser = new RROPathKey128SubobjectParser();
        final SubobjectContainerBuilder subs = new SubobjectContainerBuilder();
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(new byte[]{(byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE,
            (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}));
        pBuilder.setPathKey(new PathKey(4660));
        subs.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(pathKey128Bytes, 2))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        Assert.assertArrayEquals(pathKey128Bytes, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testRROLabelSubobject() throws Exception {
        final SimpleRSVPExtensionProviderContext ctx = new SimpleRSVPExtensionProviderContext();
        try (RSVPActivator a = new RSVPActivator()) {
            a.start(ctx);
            final RROLabelSubobjectParser parser = new RROLabelSubobjectParser(ctx.getLabelHandlerRegistry());
            final SubobjectContainerBuilder subs = new SubobjectContainerBuilder();
            subs.setSubobjectType(new LabelCaseBuilder().setLabel(
                new LabelBuilder().setUniDirectional(true).setGlobal(false).setLabelType(
                    new GeneralizedLabelCaseBuilder().setGeneralizedLabel(
                        new GeneralizedLabelBuilder().setGeneralizedLabel(
                            new byte[]{(byte) 0x12, (byte) 0x00, (byte) 0x25, (byte) 0xFF}).build()).build()).build()).build());
            assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(labelBytes, 2))));
            final ByteBuf buff = Unpooled.buffer();
            parser.serializeSubobject(subs.build(), buff);
            Assert.assertArrayEquals(labelBytes, ByteArray.getAllBytes(buff));

            try {
                parser.parseSubobject(null);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
            }
            try {
                parser.parseSubobject(Unpooled.EMPTY_BUFFER);
                Assert.fail();
            } catch (final IllegalArgumentException e) {
                Assert.assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
            }
        }
    }
}
