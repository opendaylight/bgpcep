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

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.parser.object.PCEPExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.NaiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.add.lsp.input.arguments.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.sr.subobject.nai.IpNodeIdBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

public class SrObjectParserTest {

    private static final byte[] SR_ERO_OBJECT_BYTES = {
        0x07,0x10,0x00,0x10,
        /* ero-subobject */
        0x24,0x0c,(byte) 0x10,0x00,
        0x00,0x01,(byte)0xe2,0x40,
        0x4A,0x7D,0x2b,0x63,
    };

    private SimplePCEPExtensionProviderContext ctx;
    private SegmentRoutingActivator act;

    @Before
    public void setUp() {
        ctx = new SimplePCEPExtensionProviderContext();
        act = new SegmentRoutingActivator();
        act.start(ctx);
    }

    @Test
    public void testSrEroObjectWithSubobjects() throws PCEPDeserializerException {
        final PCEPExplicitRouteObjectParser parser = new PCEPExplicitRouteObjectParser(
            ctx.getEROSubobjectHandlerRegistry());

        final EroBuilder builder = new EroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final List<Subobject> subobjects = new ArrayList<>();

        final SrEroTypeBuilder srEroSubBuilder = new SrEroTypeBuilder()
                .setCFlag(false)
                .setMFlag(false)
                .setNaiType(NaiType.Ipv4NodeId)
                .setSid(Uint32.valueOf(123456))
                .setNai(new IpNodeIdBuilder().setIpAddress(new IpAddressNoZone(
                    new Ipv4AddressNoZone("74.125.43.99"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(srEroSubBuilder.build())
                .setLoose(false);
        subobjects.add(subobjBuilder.build());

        builder.setSubobject(subobjects);

        final ByteBuf result = Unpooled.wrappedBuffer(SR_ERO_OBJECT_BYTES);
        assertEquals(builder.build(),
                parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeObject(builder.build(), buffer);
        assertArrayEquals(SR_ERO_OBJECT_BYTES, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrEroSerializerWithUpdateLspAugmentation() throws PCEPDeserializerException {
        final PCEPExplicitRouteObjectParser parser = new PCEPExplicitRouteObjectParser(
            ctx.getEROSubobjectHandlerRegistry());

        final EroBuilder builder = new EroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.update.lsp
            .input.arguments.ero.subobject.subobject.type.SrEroTypeBuilder srEroSubBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402
                    .update.lsp.input.arguments.ero.subobject.subobject.type.SrEroTypeBuilder()
                    .setCFlag(false)
                    .setMFlag(false)
                    .setNaiType(NaiType.Ipv4NodeId)
                    .setSid(Uint32.valueOf(123456))
                    .setNai(new IpNodeIdBuilder().setIpAddress(new IpAddressNoZone(
                        new Ipv4AddressNoZone("74.125.43.99"))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(srEroSubBuilder.build())
                .setLoose(false);
        builder.setSubobject(Lists.newArrayList(subobjBuilder.build()));

        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeObject(builder.build(), buffer);
        assertArrayEquals(SR_ERO_OBJECT_BYTES, ByteArray.getAllBytes(buffer));
    }
}
