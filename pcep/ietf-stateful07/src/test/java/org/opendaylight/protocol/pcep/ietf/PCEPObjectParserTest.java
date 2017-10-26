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
import java.math.BigInteger;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.speaker.entity.id.tlv.SpeakerEntityIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Lsp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Srp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Srp1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.error.code.tlv.LspErrorCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.srp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;

public class PCEPObjectParserTest {

    private SimplePCEPExtensionProviderContext ctx;
    private BaseParserExtensionActivator act;

    private TlvRegistry tlvRegistry;
    private VendorInformationTlvRegistry viTlvRegistry;

    private static final byte[] DB_VERSION = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    private static final byte[] SPEAKER_ID = {0x01, 0x02, 0x03, 0x04};

    @Before
    public void setUp() throws Exception {
        this.ctx = new SimplePCEPExtensionProviderContext();
        this.act = new BaseParserExtensionActivator();
        this.act.start(this.ctx);
        this.tlvRegistry = ServiceLoaderPCEPExtensionProviderContext.create().getTlvHandlerRegistry();
        this.viTlvRegistry = ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getVendorInformationTlvRegistry();
    }

    @Test
    public void testOpenObjectWithTLV() throws PCEPDeserializerException, IOException {
        try (SyncOptimizationsActivator a = new SyncOptimizationsActivator()) {
            a.start(this.ctx);

            final SyncOptimizationsOpenObjectParser parser = new SyncOptimizationsOpenObjectParser(this.ctx.getTlvHandlerRegistry(), this.ctx.getVendorInformationTlvRegistry());
            final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPOpenObject1.bin"));

            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder();
            builder.setProcessingRule(false);
            builder.setIgnore(false);
            builder.setVersion(new ProtocolVersion((short) 1));
            builder.setKeepalive((short) 30);
            builder.setDeadTimer((short) 120);
            builder.setSessionId((short) 1);

            final Stateful tlv1 = new StatefulBuilder().setLspUpdateCapability(Boolean.TRUE).addAugmentation(Stateful1.class, new Stateful1Builder().build()).build();

            final Tlvs1Builder statBuilder = new Tlvs1Builder();
            statBuilder.setStateful(tlv1);

            final Tlvs3Builder syncOptBuilder = new Tlvs3Builder();
            syncOptBuilder.setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(new BigInteger(DB_VERSION)).build());
            syncOptBuilder.setSpeakerEntityId(new SpeakerEntityIdBuilder().setSpeakerEntityIdValue(SPEAKER_ID).build());

            builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder()
                .addAugmentation(Tlvs1.class, statBuilder.build())
                .addAugmentation(Tlvs3.class, syncOptBuilder.build())
                .build());

            assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
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

            final SyncOptimizationsLspObjectParser parser = new SyncOptimizationsLspObjectParser(this.ctx.getTlvHandlerRegistry(), this.ctx.getVendorInformationTlvRegistry());
            final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPLspObject1WithTLV.bin"));

            final LspBuilder builder = new LspBuilder();
            builder.setProcessingRule(true);
            builder.setIgnore(true);
            builder.setAdministrative(true);
            builder.setDelegate(false);
            builder.setRemove(true);
            builder.setSync(false);
            builder.addAugmentation(Lsp1.class, new Lsp1Builder().setCreate(false).build());
            builder.setOperational(OperationalStatus.GoingDown);
            builder.setPlspId(new PlspId(0x12345L));

            final LspErrorCode tlv1 = new LspErrorCodeBuilder().setErrorCode(627610883L).build();
            final SymbolicPathName tlv2 = new SymbolicPathNameBuilder().setPathName(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SymbolicPathName("Med".getBytes())).build();
            final LspDbVersion lspDbVersion = new LspDbVersionBuilder().setLspDbVersionValue(new BigInteger(DB_VERSION)).build();
            builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.TlvsBuilder().setLspErrorCode(
                    tlv1).setSymbolicPathName(tlv2)
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs1Builder().setLspDbVersion(lspDbVersion).build())
                    .build());

            assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
            final ByteBuf buf = Unpooled.buffer();
            parser.serializeObject(builder.build(), buf);
            assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
        }
    }

    @Test
    public void testStateful07LspObjectWithTlv() throws IOException, PCEPDeserializerException {
        final Stateful07LspObjectParser parser = new Stateful07LspObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPLspObject2WithTLV.bin"));

        final LspBuilder builder = new LspBuilder();
        builder.setProcessingRule(true);
        builder.setIgnore(true);
        builder.setAdministrative(true);
        builder.setDelegate(false);
        builder.setRemove(true);
        builder.setSync(false);
        builder.setOperational(OperationalStatus.GoingDown);
        builder.setPlspId(new PlspId(0x12345L));

        final LspErrorCode tlv1 = new LspErrorCodeBuilder().setErrorCode(627610883L).build();
        final SymbolicPathName tlv2 = new SymbolicPathNameBuilder().setPathName(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SymbolicPathName("Med".getBytes())).build();
        final Ipv4Builder afi = new Ipv4Builder();
        afi.setIpv4TunnelSenderAddress(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 })));
        afi.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56,
            (byte) 0x78 }))));
        afi.setIpv4TunnelEndpointAddress(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 })));
        final LspIdentifiers tlv3 = new LspIdentifiersBuilder().setAddressFamily(new Ipv4CaseBuilder().setIpv4(afi.build()).build()).setLspId(
            new LspId(65535L)).setTunnelId(new TunnelId(4660)).build();
        final RsvpErrorBuilder rsvpBuilder = new RsvpErrorBuilder();
        rsvpBuilder.setNode(new IpAddress(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 }))));
        rsvpBuilder.setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ErrorSpec.Flags(false, true));
        rsvpBuilder.setCode((short) 146);
        rsvpBuilder.setValue(5634);
        final RsvpErrorSpec tlv4 = new RsvpErrorSpecBuilder().setErrorType(new RsvpCaseBuilder().setRsvpError(rsvpBuilder.build()).build()).build();
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.TlvsBuilder().setLspErrorCode(
                tlv1).setSymbolicPathName(tlv2).setLspIdentifiers(tlv3).setRsvpErrorSpec(tlv4).build());
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
    }

    @Test
    public void testLspaObject() throws IOException, PCEPDeserializerException {
        final Stateful07LspaObjectParser parser = new Stateful07LspaObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final LspaBuilder builder = new LspaBuilder();
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPLspaObject3RandVals.bin"));

        final SymbolicPathName tlv = new SymbolicPathNameBuilder().setPathName(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SymbolicPathName(new byte[] {
                    (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x20, (byte) 0x74, (byte) 0x65, (byte) 0x73, (byte) 0x74, (byte) 0x20,
                    (byte) 0x6f, (byte) 0x66, (byte) 0x20, (byte) 0x73, (byte) 0x79, (byte) 0x6d, (byte) 0x62, (byte) 0x6f, (byte) 0x6c,
                    (byte) 0x69, (byte) 0x63, (byte) 0x20, (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65 })).build();

        builder.setIgnore(false);
        builder.setProcessingRule(false);
        builder.setExcludeAny(new AttributeFilter(0x20A1FEE3L));
        builder.setIncludeAny(new AttributeFilter(0x1A025CC7L));
        builder.setIncludeAll(new AttributeFilter(0x2BB66532L));
        builder.setHoldPriority((short) 0x02);
        builder.setSetupPriority((short) 0x03);
        builder.setLocalProtectionDesired(true);
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.lspa.TlvsBuilder().addAugmentation(
                Tlvs2.class, new Tlvs2Builder().setSymbolicPathName(tlv).build()).build());

        // Tlvs container does not contain toString
        final Object o = parser.parseObject(new ObjectHeaderImpl(true, true), result.slice(4, result.readableBytes() - 4));
        assertEquals(tlv, ((Lspa) o).getTlvs().getAugmentation(Tlvs2.class).getSymbolicPathName());
        // assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true), ByteArray.cutBytes(result,
        // 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
    }

    @Test
    public void testSrpObject() throws IOException, PCEPDeserializerException {
        final CInitiated00SrpObjectParser parser = new CInitiated00SrpObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(new byte[] { (byte) 0x21, (byte) 0x10, (byte) 0x00, (byte) 0x0c, 0, 0, 0,
            (byte) 0x01, 0, 0, 0, (byte) 0x01 });

        final SrpBuilder builder = new SrpBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setOperationId(new SrpIdNumber(1L));
        builder.addAugmentation(Srp1.class, new Srp1Builder().setRemove(true).build());
        builder.setTlvs(new TlvsBuilder().build());

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
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
        final CInitiated00SrpObjectParser parser = new CInitiated00SrpObjectParser(this.tlvRegistry, this.viTlvRegistry);
        final SrpBuilder builder = new SrpBuilder();
        builder.setProcessingRule(false);
        builder.setIgnore(false);
        builder.setOperationId(new SrpIdNumber(1L));
        builder.addAugmentation(Srp1.class, new Srp1Builder().setRemove(true).build());
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.srp.TlvsBuilder().setPathSetupType(new PathSetupTypeBuilder().setPst((short) 0).build()).build());

        final ByteBuf result = Unpooled.wrappedBuffer(srpObjectWithPstTlvBytes);
        assertEquals(builder.build(),
                parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(srpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));
    }
}
