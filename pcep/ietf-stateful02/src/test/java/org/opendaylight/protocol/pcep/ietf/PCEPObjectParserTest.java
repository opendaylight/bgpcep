/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.crabbe.initiated00.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02LspObjectParser;
import org.opendaylight.protocol.pcep.ietf.stateful02.Stateful02LspaObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.lsp.cleanup.tlv.LspCleanupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.PlspId;

public class PCEPObjectParserTest {

    private TlvRegistry tlvRegistry;

    private VendorInformationTlvRegistry viTlvRegistry;

    @Before
    public void setUp() throws Exception {
        this.tlvRegistry = ServiceLoaderPCEPExtensionProviderContext.create().getTlvHandlerRegistry();
        this.viTlvRegistry = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getVendorInformationTlvRegistry();
    }

    @Test
    public void testOpenObjectWithTLV() throws PCEPDeserializerException, IOException {
        final PCEPOpenObjectParser parser = new PCEPOpenObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPOpenObject1.bin"));

        final OpenBuilder builder = new OpenBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setVersion(new ProtocolVersion((short) 1));
        builder.setKeepalive((short) 30);
        builder.setDeadTimer((short) 120);
        builder.setSessionId((short) 1);

        final Stateful tlv1 = new StatefulBuilder().setLspUpdateCapability(Boolean.TRUE).setIncludeDbVersion(Boolean.FALSE).addAugmentation(
                Stateful1.class, new Stateful1Builder().setInitiation(true).build()).build();

        final Tlvs2Builder statBuilder = new Tlvs2Builder();
        statBuilder.setStateful(tlv1);

        final Tlvs1Builder cleanupBuilder = new Tlvs1Builder();
        cleanupBuilder.setLspCleanup(new LspCleanupBuilder().setTimeout(180L).build());

        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().addAugmentation(
                Tlvs2.class, statBuilder.build()).addAugmentation(Tlvs1.class, cleanupBuilder.build()).build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
    }

    @Test
    public void testLspaObjectWithTlv() throws IOException, PCEPDeserializerException {
        final Stateful02LspaObjectParser parser = new Stateful02LspaObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPLspaObject1LowerBounds.bin"));

        final LspaBuilder builder = new LspaBuilder();
        builder.setIgnore(false);
        builder.setProcessingRule(false);
        builder.setIncludeAny(new AttributeFilter(0l));
        builder.setExcludeAny(new AttributeFilter(0l));
        builder.setIncludeAll(new AttributeFilter(0l));
        builder.setSetupPriority((short) 0);
        builder.setHoldPriority((short) 0);
        builder.setLocalProtectionDesired(false);

        final SymbolicPathName tlv = new SymbolicPathNameBuilder().setPathName(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.SymbolicPathName(new byte[] {
                    (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x20, (byte) 0x74, (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x20,
                    (byte) 0x6f, (byte) 0x66, (byte) 0x20, (byte) 0x73, (byte) 0x79, (byte) 0x6d, (byte) 0x62, (byte) 0x6f, (byte) 0x6c,
                    (byte) 0x69, (byte) 0x63, (byte) 0x20, (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65 })).build();

        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder().addAugmentation(
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2.class,
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2Builder().setSymbolicPathName(
                        tlv).build()).build());
        // Tlvs container does not contain toString
        final Object o = parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4));
        assertEquals(
                tlv,
                ((Lspa) o).getTlvs().getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Tlvs2.class).getSymbolicPathName());
        // assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), ByteArray.cutBytes(result,
        // 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
    }

    @Test
    public void testLspObjectWithTLV() throws IOException, PCEPDeserializerException {
        final Stateful02LspObjectParser parser = new Stateful02LspObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPLspObject1WithTLV.bin"));

        final LspBuilder builder = new LspBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setDelegate(false);
        builder.setSync(false);
        builder.setOperational(true);
        builder.setRemove(true);
        builder.setPlspId(new PlspId(0x12345L));

        final SymbolicPathName tlv2 = new SymbolicPathNameBuilder().setPathName(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.SymbolicPathName("Med".getBytes())).build();
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.lsp.TlvsBuilder().setSymbolicPathName(
                tlv2).build());
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
        ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
    }
}
