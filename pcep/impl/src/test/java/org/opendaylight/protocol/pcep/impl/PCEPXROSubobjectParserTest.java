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
import org.opendaylight.protocol.pcep.impl.subobject.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROPathKey128SubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROPathKey32SubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROSRLGSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectBuilder;
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

public class PCEPXROSubobjectParserTest {

    private static final byte[] ip4PrefixBytes = { (byte) 0x01, (byte) 0x08, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x16, (byte) 0x00 };
    private static final byte[] ip6PrefixBytes = { (byte) 0x82, (byte) 0x14, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0x16, (byte) 0x01 };
    private static final byte[] srlgBytes = { (byte) 0xa2, (byte) 0x08, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x00, (byte) 0x02 };
    private static final byte[] unnumberedBytes = { (byte) 0x84, (byte) 0x0c, (byte) 0x00, (byte) 0x01, (byte) 0x12, (byte) 0x34,
        (byte) 0x50, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
    private static final byte[] asNumberBytes = { (byte) 0xa0, (byte) 0x04, (byte) 0x00, (byte) 0x64 };
    private static final byte[] pathKey32Bytes = { (byte) 0xc0, (byte) 0x08, (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34,
        (byte) 0x50, (byte) 0x00 };
    private static final byte[] pathKey128Bytes = { (byte) 0xc1, (byte) 0x14, (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34,
        (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    @Test
    public void testXROIp4PrefixSubobject() throws PCEPDeserializerException {
        final XROIpv4PrefixSubobjectParser parser = new XROIpv4PrefixSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(false);
        subs.setAttribute(Attribute.Interface);
        subs.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
                new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.255/22"))).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(ip4PrefixBytes, 2)), false));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(ip4PrefixBytes, ByteArray.getAllBytes(buff));

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
    public void testXROIp6PrefixSubobject() throws PCEPDeserializerException {
        final XROIpv6PrefixSubobjectParser parser = new XROIpv6PrefixSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(true);
        subs.setAttribute(Attribute.Node);
        subs.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
                new IpPrefixBuilder().setIpPrefix(
                        new IpPrefix(Ipv6Util.prefixForBytes(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 22))).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(ip6PrefixBytes, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(ip6PrefixBytes, ByteArray.getAllBytes(buff));

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
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(true);
        subs.setAttribute(Attribute.Srlg);
        subs.setSubobjectType(new SrlgCaseBuilder().setSrlg(new SrlgBuilder().setSrlgId(new SrlgId(0x12345678L)).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(srlgBytes, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(srlgBytes, ByteArray.getAllBytes(buff));

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
    public void testXROUnnumberedSubobject() throws PCEPDeserializerException {
        final XROUnnumberedInterfaceSubobjectParser parser = new XROUnnumberedInterfaceSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(true);
        subs.setAttribute(Attribute.Node);
        subs.setSubobjectType(new UnnumberedCaseBuilder().setUnnumbered(
                new UnnumberedBuilder().setRouterId(0x12345000L).setInterfaceId(0xffffffffL).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(unnumberedBytes, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(unnumberedBytes, ByteArray.getAllBytes(buff));

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
    public void testXROAsNumberSubobject() throws PCEPDeserializerException {
        final XROAsNumberSubobjectParser parser = new XROAsNumberSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(true);
        subs.setSubobjectType(new AsNumberCaseBuilder().setAsNumber(new AsNumberBuilder().setAsNumber(new AsNumber(0x64L)).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(asNumberBytes, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(asNumberBytes, ByteArray.getAllBytes(buff));

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
    public void testXROPathKey32Subobject() throws PCEPDeserializerException {
        final XROPathKey32SubobjectParser parser = new XROPathKey32SubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(true);
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x50, (byte) 0x00 }));
        pBuilder.setPathKey(new PathKey(4660));
        subs.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(pathKey32Bytes, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(pathKey32Bytes, ByteArray.getAllBytes(buff));

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
    public void testXROPathKey128Subobject() throws PCEPDeserializerException {
        final XROPathKey128SubobjectParser parser = new XROPathKey128SubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setMandatory(true);
        final PathKeyBuilder pBuilder = new PathKeyBuilder();
        pBuilder.setPceId(new PceId(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE,
            (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 }));
        pBuilder.setPathKey(new PathKey(4660));
        subs.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(pathKey128Bytes, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(pathKey128Bytes, ByteArray.getAllBytes(buff));

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
}
