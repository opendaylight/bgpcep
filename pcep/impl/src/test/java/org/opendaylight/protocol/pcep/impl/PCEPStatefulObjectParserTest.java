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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.parser.BaseParserExtensionActivator;
import org.opendaylight.protocol.pcep.parser.object.PCEPLspObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPSrpObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.LspFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.srp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.error.code.tlv.LspErrorCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.speaker.entity.id.tlv.SpeakerEntityIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.stateful.capability.tlv.StatefulCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PCEPStatefulObjectParserTest {
    private static final Uint64 DB_VERSION = Uint64.valueOf("0102030405060708", 16);
    private static final byte[] SPEAKER_ID = {0x01, 0x02, 0x03, 0x04};

    private final SimplePCEPExtensionProviderContext ctx = new SimplePCEPExtensionProviderContext();
    private final TlvRegistry tlvRegistry = ctx.getTlvHandlerRegistry();
    private final VendorInformationTlvRegistry viTlvRegistry = ctx.getVendorInformationTlvRegistry();

    @Before
    public void setUp() {
        new BaseParserExtensionActivator().start(ctx);
    }

    @Test
    public void testOpenObjectWithTLV() throws PCEPDeserializerException, IOException {

        final PCEPOpenObjectParser parser = new PCEPOpenObjectParser(tlvRegistry, viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPOpenObject2.bin")));

        final var builder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930
            .open.object.OpenBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setVersion(new ProtocolVersion(Uint8.ONE))
                .setKeepalive(Uint8.valueOf(30))
                .setDeadTimer(Uint8.valueOf(120))
                .setSessionId(Uint8.ONE);

        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930
            .open.object.open.TlvsBuilder()
                .setStatefulCapability(new StatefulCapabilityBuilder()
                    .setLspUpdateCapability(Boolean.TRUE)
                    .setIncludeDbVersion(Boolean.FALSE)
                    .setInitiation(Boolean.FALSE)
                    .setTriggeredResync(Boolean.FALSE)
                    .setDeltaLspSyncCapability(Boolean.FALSE)
                    .setTriggeredInitialSync(Boolean.FALSE)
                    .build())
                .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(DB_VERSION).build())
                .setSpeakerEntityId(new SpeakerEntityIdBuilder().setSpeakerEntityIdValue(SPEAKER_ID).build())
                .build());

        assertEquals(builder.build(), parser.parseObject(
            new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(), ByteArray.getAllBytes(buf));
    }

    @Test
    public void testLspObjectWithTLV() throws IOException, PCEPDeserializerException {

        final PCEPLspObjectParser parser = new PCEPLspObjectParser(tlvRegistry, viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPLspObject1WithTLV.bin")));

        final LspBuilder builder = new LspBuilder()
            .setProcessingRule(true)
            .setIgnore(true)
            .setLspFlags(new LspFlagsBuilder()
                .setPceAllocation(false)
                .setCreate(false)
                .setAdministrative(true)
                .setDelegate(false)
                .setRemove(true)
                .setSync(false)
                .setOperational(OperationalStatus.GoingDown)
                .build())
            .setPlspId(new PlspId(Uint32.valueOf(0x12345)));

        final LspErrorCode tlv1 = new LspErrorCodeBuilder().setErrorCode(Uint32.valueOf(627610883)).build();
        final SymbolicPathName tlv2 = new SymbolicPathNameBuilder().setPathName(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930
                .SymbolicPathName("Med".getBytes())).build();
        final LspDbVersion lspDbVersion = new LspDbVersionBuilder().setLspDbVersionValue(DB_VERSION).build();
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object
            .rev250930.lsp.object.lsp.TlvsBuilder()
                .setLspErrorCode(tlv1)
                .setSymbolicPathName(tlv2)
                .setLspDbVersion(lspDbVersion)
                .build())
            .build();

        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
    }

    @Test
    public void testStatefulLspObjectWithTlv() throws IOException, PCEPDeserializerException {

        final PCEPLspObjectParser parser = new PCEPLspObjectParser(tlvRegistry, viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(
            Files.readAllBytes(Path.of("src/test/resources/PCEPLspObject2WithTLV.bin")));

        final LspBuilder builder = new LspBuilder()
                .setProcessingRule(true)
                .setIgnore(true)
                .setLspFlags(new LspFlagsBuilder()
                    .setPceAllocation(false)
                    .setCreate(false)
                    .setAdministrative(true)
                    .setDelegate(false)
                    .setRemove(true)
                    .setSync(false)
                    .setOperational(OperationalStatus.GoingDown)
                    .build())
                .setPlspId(new PlspId(Uint32.valueOf(0x12345)));

        final LspErrorCode tlv1 = new LspErrorCodeBuilder().setErrorCode(Uint32.valueOf(627610883)).build();
        final SymbolicPathName tlv2 = new SymbolicPathNameBuilder().setPathName(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930
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
        builder.setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object
            .rev250930.lsp.object.lsp.TlvsBuilder().setLspErrorCode(tlv1).setSymbolicPathName(tlv2)
            .setLspIdentifiers(tlv3).setRsvpErrorSpec(tlv4).build());
        assertEquals(builder.build(), parser.parseObject(new ObjectHeaderImpl(true, true),
            result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(result.array(),ByteArray.getAllBytes(buf));
    }

    @Test
    public void testSrpObject() throws PCEPDeserializerException {
        final PCEPSrpObjectParser parser = new PCEPSrpObjectParser(tlvRegistry,
            viTlvRegistry);
        final ByteBuf result = Unpooled.wrappedBuffer(new byte[] {
            (byte) 0x21, (byte) 0x10, (byte) 0x00, (byte) 0x0c, 0, 0, 0, (byte) 0x01, 0, 0, 0, (byte) 0x01
        });

        final SrpBuilder builder = new SrpBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setOperationId(new SrpIdNumber(Uint32.ONE))
                .setRemove(true)
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
        final PCEPSrpObjectParser parser = new PCEPSrpObjectParser(tlvRegistry,
                viTlvRegistry);
        final SrpBuilder builder = new SrpBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setOperationId(new SrpIdNumber(Uint32.ONE))
                .setRemove(true)
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object
                    .rev250930.srp.object.srp.TlvsBuilder()
                    .setPathSetupType(new PathSetupTypeBuilder().setPst(PsType.RsvpTe).build()).build());

        final ByteBuf result = Unpooled.wrappedBuffer(srpObjectWithPstTlvBytes);
        assertEquals(builder.build(),
                parser.parseObject(new ObjectHeaderImpl(false, false), result.slice(4, result.readableBytes() - 4)));
        final ByteBuf buf = Unpooled.buffer();
        parser.serializeObject(builder.build(), buf);
        assertArrayEquals(srpObjectWithPstTlvBytes, ByteArray.getAllBytes(buf));
    }
}
