/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.segment.routing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev181109.SidType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev181109.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev181109.sr.subobject.nai.IpAdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev181109.sr.subobject.nai.IpNodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev181109.sr.subobject.nai.UnnumberedAdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class SrEroSubobjectParserTest {

    private static final byte[] SR_ERO_SUBOBJECT_WITH_IPV4_NODEID = {
        0x05,0x0c,(byte) 0x10,0x00,
        0x00,0x01,(byte) 0xe2,0x40,
        0x4A,0x7D,0x2b,0x63,
    };

    private static final byte[] SR_ERO_SUBOBJECT_WITH_IPV6_NODEID = {
        0x05,0x18,(byte) 0x20,0x00,
        0x00,0x01,(byte) 0xe2,0x40,
        (byte) 0xFE,(byte) 0x80,(byte) 0xCD,0x00,
        0x00,0x00,0x00,0x00,
        0x00,0x00,0x00,0x00,
        0x21,0x1E,0x72,(byte) 0x9C,
    };

    private static final byte[] SR_ERO_SUBOBJECT_WITH_IPV4_ADJ = {
        0x05,0x10,(byte) 0x30,0x00,
        0x00,0x01,(byte) 0xe2,0x40,
        0x4A,0x7D,0x2b,0x63,
        0x4A,0x7D,0x2b,0x64,
    };

    private static final byte[] SR_ERO_SUBOBJECT_WITH_IPV6_ADJ = {
        0x05,0x28,(byte) 0x40,0x00,
        0x00,0x01,(byte) 0xe2,0x40,
        (byte) 0xFE,(byte) 0x80,(byte) 0xCD,0x00,
        0x00,0x00,0x00,0x00,
        0x00,0x00,0x00,0x00,
        0x21,0x1E,0x72,(byte) 0x9C,
        (byte) 0xFE,(byte) 0x80,(byte) 0xCD,0x00,
        0x00,0x00,0x00,0x00,
        0x00,0x00,0x00,0x00,
        0x21,0x1E,0x72,(byte) 0x9D,
    };

    private static final byte[] SR_ERO_SUBOBJECT_WITH_UNNUMBERED = {
        0x05,0x18,(byte) 0x50,0x00,
        0x00,0x01,(byte) 0xe2,0x40,
        0x00,0x00,0x00,0x01,
        0x00,0x00,0x00,0x02,
        0x00,0x00,0x00,0x03,
        0x00,0x00,0x00,0x04
    };

    private static final byte[] SR_ERO_SUBOBJECT_WITHOUT_NAI = {
        0x05,0x08,(byte) 0x10,0x08,
        0x00,0x01,(byte) 0xe2,0x40
    };

    private static final byte[] SR_ERO_SUBOBJECT_WITHOUT_SID = {
        0x05,0x08,(byte) 0x10,0x04,
        0x4A,0x7D,0x2b,0x63,
    };

    private static final byte[] SR_ERO_SUBOBJECT_WITH_IPV4_NODEID_MFLAG = {
        0x05,0x0c,(byte) 0x10,0x01,
        0x07,0x5B,(byte) 0xCD,0x15,
        0x4A,0x7D,0x2b,0x63,
    };
    private static final byte[] SR_ERO_SUBOBJECT_WITH_IPV4_NODEID_MFLAG_AFTER = {
        0x05,0x0c,(byte) 0x10,0x01,
        0x07,0x5B,(byte) 0xC0,0x00,
        0x4A,0x7D,0x2b,0x63,
    };

    private SimplePCEPExtensionProviderContext ctx;
    private SegmentRoutingActivator act;
    private SrEroSubobjectParser parser;

    @Before
    public void setUp() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new SegmentRoutingActivator();
        this.act.start(this.ctx);
        final boolean isIanaAssignedType = false;
        this.parser = new SrEroSubobjectParser(isIanaAssignedType);
    }

    @Test
    public void testSrEroSubobjectIpv4NodeIdNAI() throws PCEPDeserializerException {
        final SrEroTypeBuilder builder = new SrEroTypeBuilder()
                .setSidType(SidType.Ipv4NodeId)
                .setSid(Uint32.valueOf(123456))
                .setCFlag(false)
                .setMFlag(false)
                .setNai(new IpNodeIdBuilder().setIpAddress(new IpAddressNoZone(new Ipv4AddressNoZone("74.125.43.99")))
                    .build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SR_ERO_SUBOBJECT_WITH_IPV4_NODEID, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SR_ERO_SUBOBJECT_WITH_IPV4_NODEID, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrEroSubobjectIpv6NodeIdNAI() throws PCEPDeserializerException {
        final SrEroTypeBuilder builder = new SrEroTypeBuilder()
                .setSidType(SidType.Ipv6NodeId)
                .setSid(Uint32.valueOf(123456))
                .setCFlag(false)
                .setMFlag(false)
                .setNai(new IpNodeIdBuilder().setIpAddress(new IpAddressNoZone(
                    new Ipv6AddressNoZone("fe80:cd00::211e:729c"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SR_ERO_SUBOBJECT_WITH_IPV6_NODEID, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SR_ERO_SUBOBJECT_WITH_IPV6_NODEID, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrEroSubobjectIpv4AdjacencyNAI() throws PCEPDeserializerException {
        final SrEroTypeBuilder builder = new SrEroTypeBuilder()
                .setSidType(SidType.Ipv4Adjacency)
                .setSid(Uint32.valueOf(123456))
                .setCFlag(false)
                .setMFlag(false)
                .setNai(new IpAdjacencyBuilder().setLocalIpAddress(new IpAddressNoZone(
                    new Ipv4AddressNoZone("74.125.43.99")))
                    .setRemoteIpAddress(new IpAddressNoZone(new Ipv4AddressNoZone("74.125.43.100"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SR_ERO_SUBOBJECT_WITH_IPV4_ADJ, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SR_ERO_SUBOBJECT_WITH_IPV4_ADJ, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrEroSubobjectIpv6AdjacencyNAI() throws PCEPDeserializerException {
        final SrEroTypeBuilder builder = new SrEroTypeBuilder()
                .setSidType(SidType.Ipv6Adjacency)
                .setSid(Uint32.valueOf(123456))
                .setCFlag(false)
                .setMFlag(false)
                .setNai(new IpAdjacencyBuilder()
                    .setLocalIpAddress(new IpAddressNoZone(new Ipv6AddressNoZone("fe80:cd00::211e:729c")))
                    .setRemoteIpAddress(new IpAddressNoZone(new Ipv6AddressNoZone("fe80:cd00::211e:729d")))
                    .build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SR_ERO_SUBOBJECT_WITH_IPV6_ADJ, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SR_ERO_SUBOBJECT_WITH_IPV6_ADJ, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrEroSubobjectUnnumberedNAI() throws PCEPDeserializerException {
        final SrEroTypeBuilder builder = new SrEroTypeBuilder()
                .setSidType(SidType.Unnumbered)
                .setSid(Uint32.valueOf(123456))
                .setCFlag(false)
                .setMFlag(false)
                .setNai(new UnnumberedAdjacencyBuilder()
                    .setLocalNodeId(Uint32.ONE).setLocalInterfaceId(Uint32.valueOf(2))
                    .setRemoteNodeId(Uint32.valueOf(3)).setRemoteInterfaceId(Uint32.valueOf(4))
                    .build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SR_ERO_SUBOBJECT_WITH_UNNUMBERED, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SR_ERO_SUBOBJECT_WITH_UNNUMBERED, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrEroSubobjectWithoutNAI() throws PCEPDeserializerException {
        final SrEroTypeBuilder builder = new SrEroTypeBuilder()
                .setSidType(SidType.Ipv4NodeId)
                .setSid(Uint32.valueOf(123456))
                .setCFlag(false)
                .setMFlag(false);
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SR_ERO_SUBOBJECT_WITHOUT_NAI, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SR_ERO_SUBOBJECT_WITHOUT_NAI, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrEroSubobjectWithoutBody() throws PCEPDeserializerException {
        final SrEroTypeBuilder builder = new SrEroTypeBuilder()
                .setSidType(SidType.Ipv4NodeId)
                .setCFlag(false)
                .setMFlag(false)
                .setNai(new IpNodeIdBuilder().setIpAddress(new IpAddressNoZone(
                    new Ipv4AddressNoZone("74.125.43.99"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SR_ERO_SUBOBJECT_WITHOUT_SID, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SR_ERO_SUBOBJECT_WITHOUT_SID, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrEroSubobjectIpv4NodeIdNAIMFlag() throws PCEPDeserializerException {
        final SrEroTypeBuilder builder = new SrEroTypeBuilder()
                .setCFlag(false)
                .setMFlag(true)
                .setSidType(SidType.Ipv4NodeId)
                .setSid(Uint32.valueOf(30140))
                .setNai(new IpNodeIdBuilder().setIpAddress(new IpAddressNoZone(new Ipv4AddressNoZone("74.125.43.99")))
                    .build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SR_ERO_SUBOBJECT_WITH_IPV4_NODEID_MFLAG, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SR_ERO_SUBOBJECT_WITH_IPV4_NODEID_MFLAG_AFTER, ByteArray.getAllBytes(buffer));
    }
}
