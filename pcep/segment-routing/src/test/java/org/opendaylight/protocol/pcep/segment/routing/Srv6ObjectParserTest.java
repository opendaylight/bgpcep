/*
 * Copyright (c) 2025 Orange.  All rights reserved.
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
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.parser.object.PCEPExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.NaiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.add.lsp.input.arguments.ero.subobject.subobject.type.Srv6EroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.Srv6PceCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.pce.capability.tlv.srv6.pce.capability.MsdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.srv6.subobject.SidStructureBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250328.open.object.open.TlvsBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

public class Srv6ObjectParserTest {

    private static final byte[] OPEN_OBJECT_BYTES = {
        0x01, 0x10, 0x00, 0x14,
        0x20, 0x1e, 0x78, 0x01,
        /* srv6-capability-tlv */
        0x00, 0x1b, 0x00, 0x06,
        0x00, 0x00, 0x00, 0x02,
        0x01, 0x0a, 0x00, 0x00
    };

    private static final byte[] SRV6_ERO_OBJECT_BYTES = {
        0x07, 0x10, 0x00, 0x24,
        /* ero-subobject */
        0x28, 0x20, 0x20, 0x06,
        0x00, 0x00, 0x00, 0x0a,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x01,
        0x0a, 0x02, 0x01, 0x00,
        0x00, 0x00, 0x00, 0x00
    };

    private TlvRegistry tlvRegistry;
    private VendorInformationTlvRegistry viTlvRegistry;

    private SimplePCEPExtensionProviderContext ctx;
    private SegmentRoutingActivator act;

    @Before
    public void setUp() {
        ctx = new SimplePCEPExtensionProviderContext();
        act = new SegmentRoutingActivator();
        act.start(ctx);
        tlvRegistry = ctx.getTlvHandlerRegistry();
        viTlvRegistry = ctx.getVendorInformationTlvRegistry();
    }

    @Test
    public void testOpenObjectWithSv6pcTlv() throws PCEPDeserializerException {
        final PcepOpenObjectWithSpcTlvParser parser = new PcepOpenObjectWithSpcTlvParser(tlvRegistry,
            viTlvRegistry);

        final OpenBuilder builder = new OpenBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setVersion(new ProtocolVersion(Uint8.ONE))
                .setKeepalive(Uint8.valueOf(30))
                .setDeadTimer(Uint8.valueOf(120))
                .setSessionId(Uint8.ONE);

        builder.setTlvs(new TlvsBuilder()
                .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
                    .rev250328.Tlvs1Builder().build())
                .addAugmentation(new Tlvs1Builder()
                    .setSrv6PceCapability(new Srv6PceCapabilityBuilder().setNFlag(Boolean.TRUE)
                        .setMsds(List.of(new MsdsBuilder().setMsdType(Uint8.ONE).setMsdValue(Uint8.TEN).build()))
                        .build())
                    .build())
                .addAugmentation(new Tlvs3Builder().build()).build());

        final ByteBuf result = Unpooled.wrappedBuffer(OPEN_OBJECT_BYTES);
        assertEquals(builder.build(),
                parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeObject(builder.build(), buffer);
        assertArrayEquals(OPEN_OBJECT_BYTES, ByteArray.getAllBytes(buffer));
    }

    @Test
    public void testSrv6EroObjectWithSubobjects() throws PCEPDeserializerException {
        final PCEPExplicitRouteObjectParser parser = new PCEPExplicitRouteObjectParser(
            ctx.getEROSubobjectHandlerRegistry());

        final EroBuilder builder = new EroBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        final List<Subobject> subobjects = new ArrayList<>();

        final Srv6EroTypeBuilder srv6EroSubBuilder = new Srv6EroTypeBuilder()
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
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(srv6EroSubBuilder.build())
                .setLoose(false);
        subobjects.add(subobjBuilder.build());

        builder.setSubobject(subobjects);

        final ByteBuf result = Unpooled.wrappedBuffer(SRV6_ERO_OBJECT_BYTES);
        assertEquals(builder.build(),
                parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buffer = Unpooled.buffer();
        parser.serializeObject(builder.build(), buffer);
        assertArrayEquals(SRV6_ERO_OBJECT_BYTES, ByteArray.getAllBytes(buffer));
    }
}
