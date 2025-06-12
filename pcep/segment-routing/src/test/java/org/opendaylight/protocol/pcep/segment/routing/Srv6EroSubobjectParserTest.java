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
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.NaiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.add.lsp.input.arguments.ero.subobject.subobject.type.Srv6EroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.subobject.SidStructureBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.subobject.srv6.nai.Ipv6AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.subobject.srv6.nai.Ipv6LocalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.subobject.srv6.nai.Ipv6NodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class Srv6EroSubobjectParserTest {

    private static final byte[] SRV6_ERO_SUBOBJECT_WITH_SRV6_SID = {
        0x28, 0x18, 0x20, 0x02,
        0x00, 0x00, 0x00, 0x0a,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x01
    };

    private static final byte[] SRV6_ERO_SUBOBJECT_WITH_SID_STRUCTURE = {
        0x28, 0x20, 0x20, 0x06,
        0x00, 0x00, 0x00, 0x0a,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x01,
        0x0a, 0x02, 0x01, 0x00,
        0x00, 0x00, 0x00, 0x00
    };

    private static final byte[] SRV6_ERO_SUBOBJECT_WITH_IPV6_NODEID = {
        0x28, 0x18, 0x20, 0x01,
        0x00, 0x00, 0x00, 0x0a,
        (byte) 0xFE, (byte) 0x80, (byte) 0xcd, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x21, 0x1E, 0x72, (byte) 0x9C
    };

    private static final byte[] SRV6_ERO_SUBOBJECT_WITH_IPV6_ADJ = {
        0x28, 0x28, 0x40, 0x01,
        0x00, 0x00, 0x00, 0x0a,
        (byte) 0xFE, (byte) 0x80, (byte) 0xcd, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x21, 0x1E, 0x72, (byte) 0x9C,
        (byte) 0xFE, (byte) 0x80, (byte) 0xcd, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x21, 0x1E, 0x72, (byte) 0x9D
    };

    private static final byte[] SRV6_ERO_SUBOBJECT_WITH_IPV6_LOCAL = {
        0x28, 0x30, 0x60, 0x01,
        0x00, 0x00, 0x00, 0x0a,
        (byte) 0xFE, (byte) 0x80, (byte) 0xcd, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x21, 0x1E, 0x72, (byte) 0x9C,
        0x00, 0x00, 0x00, 0x01,
        (byte) 0xFE, (byte) 0x80, (byte) 0xcd, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x21, 0x1E, 0x72, (byte) 0x9D,
        0x00, 0x00, 0x00, 0x02
    };

    private SimplePCEPExtensionProviderContext ctx;
    private SegmentRoutingActivator act;
    private Srv6EroSubobjectParser parser;

    @Before
    public void setUp() {
        ctx = new SimplePCEPExtensionProviderContext();
        act = new SegmentRoutingActivator();
        act.start(ctx);
        parser = new Srv6EroSubobjectParser();
    }

    @Test
    public void testSrv6EroSubobjectSrv6Sid() throws PCEPDeserializerException {
        final Srv6EroTypeBuilder builder = new Srv6EroTypeBuilder()
            .setSrv6NaiType(NaiType.Ipv6NodeId)
            .setEndpointBehavior(Uint16.TEN)
            .setVFlag(false)
            .setSrv6Sid(new Ipv6AddressNoZone("::1"));
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SRV6_ERO_SUBOBJECT_WITH_SRV6_SID, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SRV6_ERO_SUBOBJECT_WITH_SRV6_SID, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrv6EroSubobjectSrv6SidStructure() throws PCEPDeserializerException {
        final Srv6EroTypeBuilder builder = new Srv6EroTypeBuilder()
            .setSrv6NaiType(NaiType.Ipv6NodeId)
            .setEndpointBehavior(Uint16.TEN)
            .setVFlag(false)
            .setSrv6Sid(new Ipv6AddressNoZone("::1"))
            .setSidStructure(new SidStructureBuilder()
                .setLocatorBlockLength(Uint8.TEN)
                .setLocatorNodeLength(Uint8.TWO)
                .setFunctionLength(Uint8.ONE)
                .setArgumentLength(Uint8.ZERO)
                .build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SRV6_ERO_SUBOBJECT_WITH_SID_STRUCTURE, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SRV6_ERO_SUBOBJECT_WITH_SID_STRUCTURE, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrv6EroSubobjectIpv6NodeIdNAI() throws PCEPDeserializerException {
        final Srv6EroTypeBuilder builder = new Srv6EroTypeBuilder()
            .setSrv6NaiType(NaiType.Ipv6NodeId)
            .setEndpointBehavior(Uint16.TEN)
            .setVFlag(false)
            .setSrv6Nai(new Ipv6NodeIdBuilder()
                .setIpv6Address(new Ipv6AddressNoZone("fe80:cd00::211e:729c"))
                .build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SRV6_ERO_SUBOBJECT_WITH_IPV6_NODEID, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SRV6_ERO_SUBOBJECT_WITH_IPV6_NODEID, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrv6EroSubobjectIpv6AdjacencyNAI() throws PCEPDeserializerException {
        final Srv6EroTypeBuilder builder = new Srv6EroTypeBuilder()
            .setSrv6NaiType(NaiType.Ipv6Adjacency)
            .setEndpointBehavior(Uint16.TEN)
            .setVFlag(false)
            .setSrv6Nai(new Ipv6AdjacencyBuilder()
                .setIpv6LocalAddress(new Ipv6AddressNoZone("fe80:cd00::211e:729c"))
                .setIpv6RemoteAddress(new Ipv6AddressNoZone("fe80:cd00::211e:729d"))
                .build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SRV6_ERO_SUBOBJECT_WITH_IPV6_ADJ, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SRV6_ERO_SUBOBJECT_WITH_IPV6_ADJ, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrv6EroSubobjectIpv6LocalNAI() throws PCEPDeserializerException {
        final Srv6EroTypeBuilder builder = new Srv6EroTypeBuilder()
            .setSrv6NaiType(NaiType.Ipv6Local)
            .setEndpointBehavior(Uint16.TEN)
            .setVFlag(false)
            .setSrv6Nai(new Ipv6LocalBuilder()
                .setLocalIpv6(new Ipv6AddressNoZone("fe80:cd00::211e:729c"))
                .setRemoteIpv6(new Ipv6AddressNoZone("fe80:cd00::211e:729d"))
                .setLocalIdentifier(Uint32.ONE)
                .setRemoteIdentifier(Uint32.TWO)
                .build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(builder.build()).setLoose(false);

        assertEquals(subobjBuilder.build(), parser.parseSubobject(Unpooled.wrappedBuffer(
            ByteArray.cutBytes(SRV6_ERO_SUBOBJECT_WITH_IPV6_LOCAL, 2)), false));
        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeSubobject(subobjBuilder.build(), buffer);
        assertArrayEquals(SRV6_ERO_SUBOBJECT_WITH_IPV6_LOCAL, ByteArray.getAllBytes(buffer));
    }
}
