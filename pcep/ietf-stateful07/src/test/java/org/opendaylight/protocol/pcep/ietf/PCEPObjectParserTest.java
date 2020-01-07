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
import org.opendaylight.protocol.pcep.ietf.initiated00.CInitiated00SrpObjectParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07LspObjectParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07LspaObjectParser;
import org.opendaylight.protocol.pcep.ietf.stateful07.StatefulActivator;
import org.opendaylight.protocol.pcep.parser.BaseParserExtensionActivator;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.sync.optimizations.SyncOptimizationsActivator;
import org.opendaylight.protocol.pcep.sync.optimizations.SyncOptimizationsLspObjectParser;
import org.opendaylight.protocol.pcep.sync.optimizations.SyncOptimizationsOpenObjectParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.speaker.entity.id.tlv.SpeakerEntityIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.error.code.tlv.LspErrorCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.srp.object.srp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PCEPObjectParserTest {

    private SimplePCEPExtensionProviderContext ctx;
    private BaseParserExtensionActivator act;

    private TlvRegistry tlvRegistry;
    private VendorInformationTlvRegistry viTlvRegistry;

    private static final Uint64 DB_VERSION = Uint64.valueOf("0102030405060708", 16);
    private static final byte[] SPEAKER_ID = {0x01, 0x02, 0x03, 0x04};

    @Before
    public void setUp() {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new BaseParserExtensionActivator();
        this.act.start(this.ctx);
        this.tlvRegistry = ServiceLoaderPCEPExtensionProviderContext.create().getTlvHandlerRegistry();
        this.viTlvRegistry = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance()
                .getVendorInformationTlvRegistry();
    }

    @Test
    public void testOpenObjectWithTLV() throws PCEPDeserializerException, IOException {
        try (SyncOptimizationsActivator a = new SyncOptimizationsActivator()) {
            a.start(this.ctx);

            final SyncOptimizationsOpenObjectParser parser = new SyncOptimizationsOpenObjectParser(
                this.ctx.getTlvHandlerRegistry(), this.ctx.getVendorInformationTlvRegistry());
            final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes(
                "src/test/resources/PCEPOpenObject1.bin"));

            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object
                .OpenBuilder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types
                    .rev181109.open.object.OpenBuilder()
                    .setProcessingRule(false)
                    .setIgnore(false)
                    .setVersion(new ProtocolVersion(Uint8.ONE))
                    .setKeepalive(Uint8.valueOf(30))
                    .setDeadTimer(Uint8.valueOf(120))
                    .setSessionId(Uint8.ONE);

            final Stateful tlv1 = new StatefulBuilder().setLspUpdateCapability(Boolean.TRUE)
                    .addAugmentation(Stateful1.class, new Stateful1Builder().build()).build();

            final Tlvs1Builder statBuilder = new Tlvs1Builder();
            statBuilder.setStateful(tlv1);

            final Tlvs3Builder syncOptBuilder = new Tlvs3Builder()
                    .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(DB_VERSION).build())
                    .setSpeakerEntityId(new SpeakerEntityIdBuilder().setSpeakerEntityIdValue(SPEAKER_ID).build());

            builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                .open.object.open.TlvsBuilder()
                .addAugmentation(Tlvs1.class, statBuilder.build())
                .addAugmentation(Tlvs3.class, syncOptBuilder.build())
                .build());

            assertEquals(builder.build(), parser.parseObject(
                new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
            final ByteBuf buf = Unpooled.buffer();
            parser.serializeObject(builder.build(), buf);
            assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));
        }
    }

    @Test
    public void testLspObjectWithTLV() throws IOException, PCEPDeserializerException {
        try (StatefulActivator a = new StatefulActivator();
            SyncOptimizationsActivator a2 = new SyncOptimizationsActivator()) {
            a.start(this.ctx);
            a2.start(this.ctx);

            final SyncOptimizationsLspObjectParser parser = new SyncOptimizationsLspObjectParser(
                this.ctx.getTlvHandlerRegistry(), this.ctx.getVendorInformationTlvRegistry());
            final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes(
                "src/test/resources/PCEPLspObject1WithTLV.bin"));

            final LspBuilder builder = new LspBuilder()
                    .setProcessingRule(true)
                    .setIgnore(true)
                    .setAdministrative(true)
                    .setDelegate(false)
                    .setRemove(true)
                    .setSync(false)
                    .addAugmentation(Lsp1.class, new Lsp1Builder().setCreate(false).build())
                    .setOperational(OperationalStatus.GoingDown)
                    .setPlspId(new PlspId(Uint32.valueOf(0x12345)));

            final LspErrorCode tlv1 = new LspErrorCodeBuilder().setErrorCode(Uint32.valueOf(627610883)).build();
            final SymbolicPathName tlv2 = new SymbolicPathNameBuilder().setPathName(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109
                        .SymbolicPathName("Med".getBytes())).build();
            final LspDbVersion lspDbVersion = new LspDbVersionBuilder().setLspDbVersionValue(DB_VERSION).build();
            builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
                .rev181109.lsp.object.lsp.TlvsBuilder().setLspErrorCode(tlv1).setSymbolicPathName(tlv2)
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep
                        .sync.optimizations.rev181109.Tlvs1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight
                            .params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Tlvs1Builder()
                            .setLspDbVersion(lspDbVersion).build())
                    .build());

            assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
                result.slice(4, result.readableBytes() - 4)));
            final ByteBuf buf = Unpooled.buffer();
            parser.serializeObject(builder.build(), buf);
            assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
        }
    }

    @Test
    public void testStateful07LspObjectWithTlv() throws IOException, PCEPDeserializerException {
        final Stateful07LspObjectParser parser = new Stateful07LspObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes(
            "src/test/resources/PCEPLspObject2WithTLV.bin"));

        final LspBuilder builder = new LspBuilder()
                .setProcessingRule(true)
                .setIgnore(true)
                .setAdministrative(true)
                .setDelegate(false)
                .setRemove(true)
                .setSync(false)
                .setOperational(OperationalStatus.GoingDown)
                .setPlspId(new PlspId(Uint32.valueOf(0x12345)));

        final LspErrorCode tlv1 = new LspErrorCodeBuilder().setErrorCode(Uint32.valueOf(627610883)).build();
        final SymbolicPathName tlv2 = new SymbolicPathNameBuilder().setPathName(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109
                    .SymbolicPathName("Med".getBytes())).build();
        final LspIdentifiers tlv3 = new LspIdentifiersBuilder()
                .setAddressFamily(new Ipv4CaseBuilder()
                    .setIpv4(new Ipv4Builder()
                        .setIpv4TunnelSenderAddress(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(
                            new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 })))
                        .setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(Ipv4Util.addressForByteBuf(
                            Unpooled.wrappedBuffer(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 }))))
                        .setIpv4TunnelEndpointAddress(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(
                            new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 })))
                        .build())
                    .build())
                .setLspId(new LspId(Uint32.valueOf(65535))).setTunnelId(new TunnelId(Uint16.valueOf(4660)))
                .build();
        final RsvpErrorSpec tlv4 = new RsvpErrorSpecBuilder()
                .setErrorType(new RsvpCaseBuilder()
                    .setRsvpError(new RsvpErrorBuilder()
                        .setNode(new IpAddressNoZone(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(
                            new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 }))))
                        .setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820
                            .ErrorSpec.Flags(false, true))
                        .setCode(Uint8.valueOf(146))
                        .setValue(Uint16.valueOf(5634))
                        .build())
                    .build())
                .build();
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
            .rev181109.lsp.object.lsp.TlvsBuilder().setLspErrorCode(tlv1).setSymbolicPathName(tlv2)
            .setLspIdentifiers(tlv3).setRsvpErrorSpec(tlv4).build());
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
    }

    @Test
    public void testLspaObject() throws IOException, PCEPDeserializerException {
        final Stateful07LspaObjectParser parser = new Stateful07LspaObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final LspaBuilder builder = new LspaBuilder();
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes(
            "src/test/resources/PCEPLspaObject3RandVals.bin"));

        final SymbolicPathName tlv = new SymbolicPathNameBuilder().setPathName(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109
                    .SymbolicPathName(new byte[] {
                        (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x20, (byte) 0x74, (byte) 0x65, (byte) 0x73,
                        (byte) 0x74, (byte) 0x20, (byte) 0x6f, (byte) 0x66, (byte) 0x20, (byte) 0x73, (byte) 0x79,
                        (byte) 0x6d, (byte) 0x62, (byte) 0x6f, (byte) 0x6c, (byte) 0x69, (byte) 0x63, (byte) 0x20,
                        (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65 })).build();

        builder.setIgnore(false)
            .setProcessingRule(false)
            .setExcludeAny(new AttributeFilter(Uint32.valueOf(0x20A1FEE3L)))
            .setIncludeAny(new AttributeFilter(Uint32.valueOf(0x1A025CC7L)))
            .setIncludeAll(new AttributeFilter(Uint32.valueOf(0x2BB66532L)))
            .setHoldPriority(Uint8.valueOf(0x02))
            .setSetupPriority(Uint8.valueOf(0x03))
            .setLocalProtectionDesired(true)
            .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa
                .object.lspa.TlvsBuilder().addAugmentation(Tlvs2.class,
                    new Tlvs2Builder().setSymbolicPathName(tlv).build()).build());

        // Tlvs container does not contain toString
        final Object o = parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4));
        assertEquals(tlv, ((Lspa) o).getTlvs().augmentation(Tlvs2.class).getSymbolicPathName());
        // assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), ByteArray.cutBytes(result,
        // 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
    }

    @Test
    public void testSrpObject() throws PCEPDeserializerException {
        final CInitiated00SrpObjectParser parser = new CInitiated00SrpObjectParser(this.tlvRegistry,
            this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(new byte[] {
            (byte) 0x21, (byte) 0x10, (byte) 0x00, (byte) 0x0c, 0, 0, 0, (byte) 0x01, 0, 0, 0, (byte) 0x01
        });

        final SrpBuilder builder = new SrpBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setOperationId(new SrpIdNumber(Uint32.ONE))
                .addAugmentation(Srp1.class, new Srp1Builder().setRemove(true).build())
                .setTlvs(new TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
    }

    @Test
    public void testSRPObjectWithPSTTlv() throws PCEPDeserializerException {
        final byte[] srpObjectWithPstTlvBytes = { 0x21, 0x10, 0x00, 0x14, 0x0, 0x0, 0x0, 0x01, 0x0, 0x0,
            0x0, 0x01,
            /* pst-tlv */
            0x0, 0x1C, 0x0, 0x4, 0x0, 0x0, 0x0, 0x0 };
        final CInitiated00SrpObjectParser parser = new CInitiated00SrpObjectParser(this.tlvRegistry,
            this.viTlvRegistry);
        final SrpBuilder builder = new SrpBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setOperationId(new SrpIdNumber(Uint32.ONE))
                .addAugmentation(Srp1.class, new Srp1Builder().setRemove(true).build())
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful
                    .rev181109.srp.object.srp.TlvsBuilder()
                    .setPathSetupType(new PathSetupTypeBuilder().setPst(Uint8.ZERO).build()).build());

        final ByteBuf result = Unpooled.wrappedBuffer(srpObjectWithPstTlvBytes);
        assertEquals(builder.build(),
                parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(srpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));
    }
}
