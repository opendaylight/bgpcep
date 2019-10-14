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
import org.opendaylight.protocol.pcep.parser.subobject.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROPathKey128SubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROPathKey32SubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROSRLGSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.SrlgCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.srlg._case.SrlgBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;

public class PcepXROSubobjectParserTest {

    private static final byte[] IP4_PREFIX_BYTES = {
        (byte) 0x01, (byte) 0x08, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x16, (byte) 0x00
    };
    private static final byte[] IP6_PREFIX_BYTES = {
        (byte) 0x82, (byte) 0x14, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0x16, (byte) 0x01
    };
    private static final byte[] SRLG_BYTES = {
        (byte) 0xa2, (byte) 0x08, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x00, (byte) 0x02
    };
    private static final byte[] UNNUMBERED_BYTES = {
        (byte) 0x84, (byte) 0x0c, (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    };
    private static final byte[] AS_NUMBER_BYTES = { (byte) 0xa0, (byte) 0x04, (byte) 0x00, (byte) 0x64 };
    private static final byte[] PATH_KEY32_BYTES = {
        (byte) 0xc0, (byte) 0x08, (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00
    };
    private static final byte[] PATH_KEY128_BYTES = {
        (byte) 0xc1, (byte) 0x14, (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
        (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    @Test
    public void testXROIp4PrefixSubobject() throws PCEPDeserializerException {
        final XROIpv4PrefixSubobjectParser parser = new XROIpv4PrefixSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(false);
        subs.setAttribute(Attribute.Interface);
        subs.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
            new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.255/22"))).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(
            Unpooled.wrappedBuffer(ByteArray.cutBytes(IP4_PREFIX_BYTES, 2)), false));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(IP4_PREFIX_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testXROIp6PrefixSubobject() throws PCEPDeserializerException {
        final XROIpv6PrefixSubobjectParser parser = new XROIpv6PrefixSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(true);
        subs.setAttribute(Attribute.Node);
        subs.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
            new IpPrefixBuilder().setIpPrefix(
                new IpPrefix(Ipv6Util.prefixForBytes(new byte[] {
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
                }, 22)))
            .build()).build());
        assertEquals(
            subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(IP6_PREFIX_BYTES, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(IP6_PREFIX_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Can't be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testXROSrlgSubobject() throws PCEPDeserializerException {
        final XROSRLGSubobjectParser parser = new XROSRLGSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder()
                .setMandatory(true)
                .setAttribute(Attribute.Srlg)
                .setSubobjectType(new SrlgCaseBuilder()
                    .setSrlg(new SrlgBuilder().setSrlgId(new SrlgId(Uint32.valueOf(0x12345678L))).build())
                    .build());
        assertEquals(subs.build(),
            parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(SRLG_BYTES, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(SRLG_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testXROUnnumberedSubobject() throws PCEPDeserializerException {
        final XROUnnumberedInterfaceSubobjectParser parser = new XROUnnumberedInterfaceSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder()
                .setMandatory(true)
                .setAttribute(Attribute.Node)
                .setSubobjectType(new UnnumberedCaseBuilder()
                    .setUnnumbered(new UnnumberedBuilder()
                        .setRouterId(Uint32.valueOf(0x12345000L))
                        .setInterfaceId(Uint32.valueOf(0xffffffffL))
                        .build())
                    .build());
        assertEquals(
            subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(UNNUMBERED_BYTES, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(UNNUMBERED_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testXROAsNumberSubobject() throws PCEPDeserializerException {
        final XROAsNumberSubobjectParser parser = new XROAsNumberSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder()
                .setMandatory(true)
                .setSubobjectType(new AsNumberCaseBuilder()
                    .setAsNumber(new AsNumberBuilder().setAsNumber(new AsNumber(Uint32.valueOf(0x64))).build())
                    .build());
        assertEquals(subs.build(),
            parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(AS_NUMBER_BYTES, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(AS_NUMBER_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testXROPathKey32Subobject() throws PCEPDeserializerException {
        final XROPathKey32SubobjectParser parser = new XROPathKey32SubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(true);
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 }));
        pBuilder.setPathKey(new PathKey(Uint16.valueOf(4660)));
        subs.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        assertEquals(subs.build(),
            parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(PATH_KEY32_BYTES, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(PATH_KEY32_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testXROPathKey128Subobject() throws PCEPDeserializerException {
        final XROPathKey128SubobjectParser parser = new XROPathKey128SubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(true);
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(new byte[] {
            (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0x12,
            (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        }));
        pBuilder.setPathKey(new PathKey(Uint16.valueOf(4660)));
        subs.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        assertEquals(subs.build(),
            parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(PATH_KEY128_BYTES, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        XROPathKey128SubobjectParser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(PATH_KEY128_BYTES, ByteArray.getAllBytes(buff));

        try {
            parser.parseSubobject(null, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
        try {
            parser.parseSubobject(Unpooled.EMPTY_BUFFER, true);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Array of bytes is mandatory. Cannot be null or empty.", e.getMessage());
        }
    }
}
