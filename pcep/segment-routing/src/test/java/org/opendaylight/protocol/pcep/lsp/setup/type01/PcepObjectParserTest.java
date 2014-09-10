/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.lsp.setup.type01;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.segment.routing02.SegmentRoutingActivator;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs5Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs7;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs7Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs8;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs8Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.rp.TlvsBuilder;

public class PcepObjectParserTest {

    private static final byte[] rpObjectWithPstTlvBytes = { 0x2, 0x10, 0x0, 0x14, 0x0, 0x0, 0x4, 0x2d, (byte) 0xde,
        (byte) 0xad, (byte) 0xbe, (byte) 0xef,
        /* pst-tlv */
        0x0, 0x1b, 0x0, 0x4, 0x0, 0x0, 0x0, 0x1 };

    private static final byte[] srpObjectWithPstTlvBytes = { 0x21, 0x10, 0x00, 0x14, 0x0, 0x0, 0x0, 0x01, 0x0, 0x0,
        0x0, 0x01,
        /* pst-tlv */
        0x0, 0x1b, 0x0, 0x4, 0x0, 0x0, 0x0, 0x1 };

    private TlvRegistry tlvRegistry;
    private VendorInformationTlvRegistry viTlvRegistry;

    private SimplePCEPExtensionProviderContext ctx;
    private SegmentRoutingActivator act;

    @Before
    public void setUp() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new SegmentRoutingActivator();
        this.act.start(this.ctx);
        this.tlvRegistry = this.ctx.getTlvHandlerRegistry();
        this.viTlvRegistry = this.ctx.getVendorInformationTlvRegistry();
    }

    @Test
    public void testRpObjectWithPstTlvParser() throws PCEPDeserializerException {
        final PcepRpObjectWithPstTlvParser parser = new PcepRpObjectWithPstTlvParser(this.tlvRegistry, this.viTlvRegistry);
        final RpBuilder builder = new RpBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setReoptimization(true);
        builder.setBiDirectional(false);
        builder.setLoose(true);
        builder.setMakeBeforeBreak(true);
        builder.setOrder(false);
        builder.setPathKey(false);
        builder.setSupplyOf(false);
        builder.setFragmentation(false);
        builder.setP2mp(false);
        builder.setEroCompression(false);
        builder.setPriority((short) 5);
        builder.setRequestId(new RequestId(0xdeadbeefL));
        builder.setTlvs(new TlvsBuilder().addAugmentation(Tlvs1.class,
                new Tlvs1Builder().setPathSetupType(new PathSetupTypeBuilder().setPst(true).build()).build()).build());

        final ByteBuf result = Unpooled.wrappedBuffer(rpObjectWithPstTlvBytes);
        assertEquals(builder.build(),
                parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(rpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));

        buf.clear();
        builder.setTlvs(new TlvsBuilder().addAugmentation(Tlvs2.class,
                new Tlvs2Builder().setPathSetupType(new PathSetupTypeBuilder().setPst(true).build()).build()).build());
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(rpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));

        buf.clear();
        builder.setTlvs(new TlvsBuilder().addAugmentation(Tlvs3.class,
                new Tlvs3Builder().setPathSetupType(new PathSetupTypeBuilder().setPst(true).build()).build()).build());
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(rpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));

        buf.clear();
        builder.setTlvs(new TlvsBuilder().addAugmentation(Tlvs4.class,
                new Tlvs4Builder().setPathSetupType(new PathSetupTypeBuilder().setPst(true).build()).build()).build());
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(rpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));
    }

    @Test
    public void testSrpObjectWithPstTlvParser() throws PCEPDeserializerException {
        final CInitiated00SrpObjectWithPstTlvParser parser = new CInitiated00SrpObjectWithPstTlvParser(this.tlvRegistry, this.viTlvRegistry);
        SrpBuilder builder = new SrpBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setOperationId(new SrpIdNumber(1L));
        builder.addAugmentation(Srp1.class, new Srp1Builder().setRemove(true).build());
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.TlvsBuilder().addAugmentation(Tlvs7.class,
                new Tlvs7Builder().setPathSetupType(new PathSetupTypeBuilder().setPst(true).build()).build())
                .build());

        final ByteBuf result = Unpooled.wrappedBuffer(srpObjectWithPstTlvBytes);
        assertEquals(builder.build(),
                parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(srpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));

        buf.clear();
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.TlvsBuilder().addAugmentation(Tlvs8.class,
                new Tlvs8Builder().setPathSetupType(new PathSetupTypeBuilder().setPst(true).build()).build())
                .build());
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(srpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));

        buf.clear();
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.TlvsBuilder().addAugmentation(Tlvs6.class,
                new Tlvs6Builder().setPathSetupType(new PathSetupTypeBuilder().setPst(true).build()).build())
                .build());
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(srpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));

        buf.clear();
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.TlvsBuilder().addAugmentation(Tlvs5.class,
                new Tlvs5Builder().setPathSetupType(new PathSetupTypeBuilder().setPst(true).build()).build())
                .build());
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(srpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));
    }
}
