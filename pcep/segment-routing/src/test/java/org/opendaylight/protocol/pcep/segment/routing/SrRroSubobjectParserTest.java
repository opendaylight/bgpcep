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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.SidType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.add.lsp.input.arguments.rro.subobject.subobject.type.SrRroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.sr.subobject.nai.IpAdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.sr.subobject.nai.IpNodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.sr.subobject.nai.UnnumberedAdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder;

public class SrRroSubobjectParserTest {

    private static final byte[] srRroSubobjectWithIpv4NodeID  = {
        0x06,0x0c,(byte) 0x10,0x00,
        0x00,0x01,(byte) 0xe2,0x40,
        0x4A,0x7D,0x2b,0x63,
    };

    private static final byte[] srRroSubobjectWithIpv6NodeID  = {
        0x06,0x18,(byte) 0x20,0x00,
        0x00,0x01,(byte) 0xe2,0x40,
        (byte) 0xFE,(byte) 0x80,(byte) 0xCD,0x00,
        0x00,0x00,0x00,0x00,
        0x00,0x00,0x00,0x00,
        0x21,0x1E,0x72,(byte) 0x9C,
    };

    private static final byte[] srRroSubobjectWithIpv4Adjacency  = {
        0x06,0x10,(byte) 0x30,0x00,
        0x00,0x01,(byte) 0xe2,0x40,
        0x4A,0x7D,0x2b,0x63,
        0x4A,0x7D,0x2b,0x64,
    };

    private static final byte[] srRroSubobjectWithIpv6Adjacency  = {
        0x06,0x28,(byte) 0x40,0x00,
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

    private static final byte[] srRroSubobjectWithUnnumbered  = {
        0x06,0x18,(byte) 0x50,0x00,
        0x00,0x01,(byte) 0xe2,0x40,
        0x00,0x00,0x00,0x01,
        0x00,0x00,0x00,0x02,
        0x00,0x00,0x00,0x03,
        0x00,0x00,0x00,0x04
    };

    private static final byte[] srRroSubobjectWithoutNAI  = {
        0x06,0x08, (byte) 0x10,0xb,
        0x1e,0x24,(byte) (byte)-32, 0x00,
    };

    private static final byte[] srRroSubobjectWithoutSID  = {
        0x06,0x08,(byte) 0x10,0x04,
        0x4A,0x7D,0x2b,0x63,
    };

    private SimplePCEPExtensionProviderContext ctx;
    private SegmentRoutingActivator act;
    private boolean isIanaAssignedType;
    private SrRroSubobjectParser parser;

    @Before
    public void setUp() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new SegmentRoutingActivator();
        this.act.start(this.ctx);
        this.isIanaAssignedType = false;
        this.parser = new SrRroSubobjectParser(this.isIanaAssignedType);
    }

    @Test
    public void testSrRroSubobjectIpv4NodeIdNAI() throws PCEPDeserializerException {
        final SrRroTypeBuilder builder = new SrRroTypeBuilder();
        builder.setSidType(SidType.Ipv4NodeId);
        builder.setSid(123456L);
        builder.setCFlag(false);
        builder.setMFlag(false);
        builder.setNai(new IpNodeIdBuilder().setIpAddress(new IpAddress(new Ipv4Address("74.125.43.99"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build());

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(srRroSubobjectWithIpv4NodeID, 2))));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(srRroSubobjectWithIpv4NodeID, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrRroSubobjectIpv6NodeIdNAI() throws PCEPDeserializerException {
        final SrRroTypeBuilder builder = new SrRroTypeBuilder();
        builder.setSidType(SidType.Ipv6NodeId);
        builder.setCFlag(false);
        builder.setMFlag(false);
        builder.setSid(123456L);
        builder.setNai(new IpNodeIdBuilder().setIpAddress(new IpAddress(new Ipv6Address("fe80:cd00::211e:729c"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build());

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(srRroSubobjectWithIpv6NodeID, 2))));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(srRroSubobjectWithIpv6NodeID, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrRroSubobjectIpv4AdjacencyNAI() throws PCEPDeserializerException {
        final SrRroTypeBuilder builder = new SrRroTypeBuilder();
        builder.setSidType(SidType.Ipv4Adjacency);
        builder.setSid(123456L);
        builder.setCFlag(false);
        builder.setMFlag(false);
        builder.setNai(new IpAdjacencyBuilder().setLocalIpAddress(new IpAddress(new Ipv4Address("74.125.43.99")))
                .setRemoteIpAddress(new IpAddress(new Ipv4Address("74.125.43.100"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build());

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(srRroSubobjectWithIpv4Adjacency, 2))));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(srRroSubobjectWithIpv4Adjacency, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrRroSubobjectIpv6AdjacencyNAI() throws PCEPDeserializerException {
        final SrRroTypeBuilder builder = new SrRroTypeBuilder();
        builder.setSidType(SidType.Ipv6Adjacency);
        builder.setSid(123456L);
        builder.setCFlag(false);
        builder.setMFlag(false);
        builder.setNai(new IpAdjacencyBuilder().setLocalIpAddress(new IpAddress(new Ipv6Address("fe80:cd00::211e:729c")))
                .setRemoteIpAddress(new IpAddress(new Ipv6Address("fe80:cd00::211e:729d"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build());

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(srRroSubobjectWithIpv6Adjacency, 2))));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(srRroSubobjectWithIpv6Adjacency, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrRroSubobjectUnnumberedNAI() throws PCEPDeserializerException {
        final SrRroTypeBuilder builder = new SrRroTypeBuilder();
        builder.setSidType(SidType.Unnumbered);
        builder.setSid(123456L);
        builder.setCFlag(false);
        builder.setMFlag(false);
        builder.setNai(new UnnumberedAdjacencyBuilder().setLocalNodeId(1L).setLocalInterfaceId(2L).setRemoteNodeId(3L).setRemoteInterfaceId(4L).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build());

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(srRroSubobjectWithUnnumbered, 2))));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(srRroSubobjectWithUnnumbered, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrRroSubobjectWithoutNAI() throws PCEPDeserializerException {
        final SrRroTypeBuilder builder = new SrRroTypeBuilder();
        builder.setSidType(SidType.Ipv4NodeId);
        builder.setSid(123470L);
        builder.setCFlag(true);
        builder.setMFlag(true);
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build());

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(srRroSubobjectWithoutNAI, 2))));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(srRroSubobjectWithoutNAI, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrRroSubobjectWithoutBody() throws PCEPDeserializerException {
        final SrRroTypeBuilder builder = new SrRroTypeBuilder();
        builder.setSidType(SidType.Ipv4NodeId);
        builder.setCFlag(false);
        builder.setMFlag(false);
        builder.setNai(new IpNodeIdBuilder().setIpAddress(new IpAddress(new Ipv4Address("74.125.43.99"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build());

        assertEquals(subobjBuilder.build(), this.parser.parseSubobject(Unpooled.wrappedBuffer(ByteArray.cutBytes(srRroSubobjectWithoutSID, 2))));
        final ByteBuf buffer = Unpooled.buffer();
        this.parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(srRroSubobjectWithoutSID, ByteArray.getAllBytes(buffer));
    }
}
