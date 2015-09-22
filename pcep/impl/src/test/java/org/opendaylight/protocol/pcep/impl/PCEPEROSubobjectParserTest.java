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

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.subobject.EROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROExplicitExclusionRouteSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROPathKey128SubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROPathKey32SubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.label._case.LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.ExrsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.exrs._case.ExrsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.path.key._case.PathKeyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.GeneralizedLabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.generalized.label._case.GeneralizedLabelBuilder;

public class PCEPEROSubobjectParserTest {
    private static final byte[] ip4PrefixBytes = { (byte) 0x81, (byte) 0x08, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0x16, (byte) 0x00 };
    private static final byte[] ip6PrefixBytes = { (byte) 0x02, (byte) 0x14, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
        (byte) 0xFF, (byte) 0xFF, (byte) 0x16, (byte) 0x00 };
    private static final byte[] asNumberBytes = { (byte) 0xa0, (byte) 0x04, (byte) 0x00, (byte) 0x64 };
    private static final byte[] unnumberedBytes = { (byte) 0x84, (byte) 0x0c, (byte) 0x00, (byte) 0x00, (byte) 0x12, (byte) 0x34,
        (byte) 0x50, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
    private static final byte[] pathKey32Bytes = { (byte) 0xc0, (byte) 0x08, (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34,
        (byte) 0x50, (byte) 0x00 };
    private static final byte[] pathKey128Bytes = { (byte) 0xc1, (byte) 0x14, (byte) 0x12, (byte) 0x34, (byte) 0x12, (byte) 0x34,
        (byte) 0x56, (byte) 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0x12, (byte) 0x34, (byte) 0x54, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
    private static final byte[] labelBytes = { (byte) 0x83, (byte) 0x08, (byte) 0x80, (byte) 0x02, (byte) 0x12, (byte) 0x00, (byte) 0x25,
        (byte) 0xFF };
    private static final byte[] exrsBytes = { (byte) 0xa1, (byte) 0x06, (byte) 0xa0, (byte) 0x04, (byte) 0x00, (byte) 0x64 };

    private SimplePCEPExtensionProviderContext ctx;
    private Activator act;

    @Before
    public void setUp() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new Activator();
        this.act.start(this.ctx);
    }

    @Test
    public void testEROIp4PrefixSubobject() throws PCEPDeserializerException {
        final EROIpv4PrefixSubobjectParser parser = new EROIpv4PrefixSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setLoose(true);
        subs.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
                new IpPrefixBuilder().setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.255/22"))).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(ip4PrefixBytes, 2)), true));
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
    public void testEROIp6PrefixSubobject() throws PCEPDeserializerException {
        final EROIpv6PrefixSubobjectParser parser = new EROIpv6PrefixSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setSubobjectType(new IpPrefixCaseBuilder().setIpPrefix(
                new IpPrefixBuilder().setIpPrefix(
                        new IpPrefix(Ipv6Util.prefixForBytes(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 22))).build()).build());
        subs.setLoose(false);
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(ip6PrefixBytes, 2)), false));
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
    public void testEROAsNumberSubobject() throws PCEPDeserializerException {
        final EROAsNumberSubobjectParser parser = new EROAsNumberSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setLoose(true);
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
    public void testEROUnnumberedSubobject() throws PCEPDeserializerException {
        final EROUnnumberedInterfaceSubobjectParser parser = new EROUnnumberedInterfaceSubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setLoose(true);
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
    public void testEROPathKey32Subobject() throws PCEPDeserializerException {
        final EROPathKey32SubobjectParser parser = new EROPathKey32SubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setLoose(true);
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
    public void testEROPathKey128Subobject() throws PCEPDeserializerException {
        final EROPathKey128SubobjectParser parser = new EROPathKey128SubobjectParser();
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setLoose(true);
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

    @Test
    public void testEROLabelSubobject() throws Exception {
        final EROLabelSubobjectParser parser = new EROLabelSubobjectParser(this.ctx.getLabelHandlerRegistry());

        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setLoose(true);
        subs.setSubobjectType(new LabelCaseBuilder().setLabel(
                new LabelBuilder().setUniDirectional(true).setLabelType(
                        new GeneralizedLabelCaseBuilder().setGeneralizedLabel(
                                new GeneralizedLabelBuilder().setGeneralizedLabel(
                                        new byte[] { (byte) 0x12, (byte) 0x00, (byte) 0x25, (byte) 0xFF }).build()).build()).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(labelBytes, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(labelBytes, ByteArray.getAllBytes(buff));

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
    public void testEROEXRSSubobject() throws Exception {
        final EROExplicitExclusionRouteSubobjectParser parser = new EROExplicitExclusionRouteSubobjectParser(this.ctx.getXROSubobjectHandlerRegistry());
        final SubobjectBuilder subs = new SubobjectBuilder();
        subs.setLoose(true);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.exrs._case.exrs.Exrs> list = Lists.newArrayList();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.exrs._case.exrs.ExrsBuilder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.exrs._case.exrs.ExrsBuilder();
        builder.setMandatory(true);
        builder.setSubobjectType(new AsNumberCaseBuilder().setAsNumber(new AsNumberBuilder().setAsNumber(new AsNumber(0x64L)).build()).build());
        list.add(builder.build());
        subs.setSubobjectType(new ExrsCaseBuilder().setExrs(new ExrsBuilder().setExrs(list).build()).build());
        assertEquals(subs.build(), parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(exrsBytes, 2)), true));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeSubobject(subs.build(), buff);
        assertArrayEquals(exrsBytes, ByteArray.getAllBytes(buff));

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
