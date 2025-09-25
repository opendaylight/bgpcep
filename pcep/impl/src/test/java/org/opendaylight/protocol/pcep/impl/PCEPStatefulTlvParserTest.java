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
import static org.junit.Assert.fail;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.parser.tlv.LSPIdentifierIpv4TlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.LSPIdentifierIpv6TlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.LspSymbolicNameTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.LspUpdateErrorTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.PathBindingTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.RSVPErrorSpecTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.StatefulCapabilityTlvParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.BindingType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.error.code.tlv.LspErrorCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.PathBinding;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.PathBindingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.FlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.MplsLabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.MplsLabelEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.Srv6BehaviorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.binding.tlv.path.binding.binding.value.Srv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.RsvpErrorSpecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.rsvp.error.spec.error.type.RsvpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.rsvp.error.spec.error.type.UserCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.rsvp.error.spec.error.type.rsvp._case.RsvpErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.rsvp.error.spec.tlv.rsvp.error.spec.error.type.user._case.UserErrorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.stateful.capability.tlv.StatefulCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.stateful.capability.tlv.StatefulCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv6ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PCEPStatefulTlvParserTest {

    private static final byte[] STATEFUL_BYTES = { 0x00, 0x10, 0x00, 0x04, 0x00, 0x00, 0x00, 0x01 };
    private static final byte[] STATEFUL_SYNC_OPT_BYTES = new byte[]{ 0x00, 0x10, 0x00, 0x04, 0x00, 0x00, 0x00, 0x33 };
    private static final byte[] SYMBOLIC_NAME_BYTES = {
        0x00, 0x11, 0x00, 0x1C, 0x4d, 0x65, 0x64, 0x20, 0x74, 0x65, 0x73, 0x74, 0x20, 0x6f,
        0x66, 0x20, 0x73, 0x79, 0x6d, 0x62, 0x6f, 0x6c, 0x69, 0x63, 0x20, 0x6e, 0x61, 0x6d, 0x65, 0x65, 0x65, 0x65
    };
    private static final byte[] LSP_UPDATE_ERROR_BYTES = { 0x00, 0x14, 0x00, 0x04, 0x25, 0x68, (byte) 0x95, 0x03 };
    private static final byte[] LSP_IDENTIFIERS4_BYTES = {
        0x00, 0x12, 0x00, 0x10, 0x12, 0x34, 0x56, 0x78, (byte) 0xFF, (byte) 0xFF, 0x12,
        0x34, 0x12, 0x34, 0x56, 0x78, 0x12, 0x34, 0x56, 0x78
    };
    private static final byte[] LSP_IDENTIFIERS6_BYTES = {
        0x00, 0x13, 0x00, 0x34, 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC,
        (byte) 0xDE, (byte) 0xF0, 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0, 0x12,
        0x34, (byte) 0xFF, (byte) 0xFF, 0x12, 0x34, 0x56, 0x78, 0x12, 0x34, 0x56, 0x78, 0x01, 0x23, 0x45, 0x67, 0x01,
        0x23, 0x45, 0x67, 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0, 0x12, 0x34, 0x56,
        0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0
    };
    private static final byte[] RSVP_ERROR_BYTES = {
        0x00, 0x15, 0x00, 0x0c, 0, 0x0c, 0x06, 0x01, 0x12, 0x34, 0x56, 0x78, 0x02, (byte) 0x92, 0x16, 0x02
    };
    private static final byte[] RSVP_ERROR6_BYTES = {
        0x00, 0x15, 0x00, 0x18, 0, 0x18, 0x06, 0x02, 0x12, 0x34, 0x56, 0x78, (byte) 0x9a, (byte) 0xbc, (byte) 0xde,
        (byte) 0xf0, 0x12, 0x34, 0x56, 0x78, (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0, 0x02, (byte) 0xd5,
        (byte) 0xc5, (byte) 0xd9
    };
    private static final byte[] USER_ERROR_BYTES = {
        0x00, 0x15, 0x00, 0x18, 0, 0x18, (byte) 0xc2, 0x01, 0x00, 0x00, 0x30, 0x39, 0x05, 0x09, 0x00,
        0x26, 0x75, 0x73, 0x65, 0x72, 0x20, 0x64, 0x65, 0x73, 0x63, 0, 0, 0
    };

    @Test
    public void testStatefulTlv() throws PCEPDeserializerException {
        final StatefulCapabilityTlvParser parser = new StatefulCapabilityTlvParser();
        final StatefulCapability tlv = new StatefulCapabilityBuilder()
            .setLspUpdateCapability(Boolean.TRUE)
            .setIncludeDbVersion(Boolean.FALSE)
            .setInitiation(Boolean.FALSE)
            .setTriggeredResync(Boolean.FALSE)
            .setDeltaLspSyncCapability(Boolean.FALSE)
            .setTriggeredInitialSync(Boolean.FALSE)
            .build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(STATEFUL_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(STATEFUL_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testStatefulTlvSyncOptimizationExtension() throws PCEPDeserializerException {
        final StatefulCapabilityTlvParser parser = new StatefulCapabilityTlvParser();
        final StatefulCapability tlv = new StatefulCapabilityBuilder()
            .setLspUpdateCapability(Boolean.TRUE)
            .setInitiation(Boolean.FALSE)
            .setIncludeDbVersion(Boolean.TRUE)
            .setTriggeredResync(Boolean.FALSE)
            .setDeltaLspSyncCapability(Boolean.TRUE)
            .setTriggeredInitialSync(Boolean.TRUE)
            .build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(STATEFUL_SYNC_OPT_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(STATEFUL_SYNC_OPT_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testSymbolicNameTlv() throws PCEPDeserializerException {
        final LspSymbolicNameTlvParser parser = new LspSymbolicNameTlvParser();
        final SymbolicPathName tlv = new SymbolicPathNameBuilder().setPathName(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930
                .SymbolicPathName("Med test of symbolic nameeee".getBytes())).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(SYMBOLIC_NAME_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(SYMBOLIC_NAME_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testLspErrorCodeTlv() throws PCEPDeserializerException {
        final LspUpdateErrorTlvParser parser = new LspUpdateErrorTlvParser();
        final LspErrorCode tlv = new LspErrorCodeBuilder().setErrorCode(Uint32.valueOf(627610883)).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(LSP_UPDATE_ERROR_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(LSP_UPDATE_ERROR_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testLspIdentifiers4Tlv() throws PCEPDeserializerException {
        final LSPIdentifierIpv4TlvParser parser = new LSPIdentifierIpv4TlvParser();
        final Ipv4Builder afi = new Ipv4Builder();
        afi.setIpv4TunnelSenderAddress(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(
            new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 })));
        afi.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(Ipv4Util.addressForByteBuf(
            Unpooled.wrappedBuffer(new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 }))));
        afi.setIpv4TunnelEndpointAddress(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(
            new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 })));
        final LspIdentifiers tlv = new LspIdentifiersBuilder().setAddressFamily(
            new Ipv4CaseBuilder().setIpv4(afi.build()).build()).setLspId(new LspId(Uint32.valueOf(65535)))
                .setTunnelId(new TunnelId(Uint16.valueOf(4660))).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(LSP_IDENTIFIERS4_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(LSP_IDENTIFIERS4_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testLspIdentifiers6Tlv() throws PCEPDeserializerException {
        final LSPIdentifierIpv6TlvParser parser = new LSPIdentifierIpv6TlvParser();
        final Ipv6Builder afi = new Ipv6Builder();
        afi.setIpv6TunnelSenderAddress(Ipv6Util.addressForByteBuf(Unpooled.wrappedBuffer(
            new byte[] {
                (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0
            })));
        afi.setIpv6ExtendedTunnelId(new Ipv6ExtendedTunnelId(Ipv6Util.addressForByteBuf(Unpooled.wrappedBuffer(
            new byte[] {
                (byte) 0x12, (byte) 0x34, (byte) 0x56,
                (byte) 0x78, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78, (byte) 0x01, (byte) 0x23, (byte) 0x45,
                (byte) 0x67, (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67
            }))));
        afi.setIpv6TunnelEndpointAddress(Ipv6Util.addressForByteBuf(Unpooled.wrappedBuffer(
            new byte[] {
                (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0, (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0
            })));
        final LspIdentifiers tlv = new LspIdentifiersBuilder()
                .setAddressFamily(new Ipv6CaseBuilder().setIpv6(afi.build()).build())
                .setLspId(new LspId(Uint32.valueOf(4660))).setTunnelId(new TunnelId(Uint16.MAX_VALUE)).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(LSP_IDENTIFIERS6_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(LSP_IDENTIFIERS6_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testRSVPError4SpecTlv() throws PCEPDeserializerException {
        final RSVPErrorSpecTlvParser parser = new RSVPErrorSpecTlvParser();
        final RsvpErrorBuilder builder = new RsvpErrorBuilder()
                .setNode(new IpAddressNoZone(Ipv4Util.addressForByteBuf(Unpooled.wrappedBuffer(
                    new byte[] { (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78 }))))
                .setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ErrorSpec
                    .Flags(false, true))
                .setCode(Uint8.valueOf(146))
                .setValue(Uint16.valueOf(5634));
        final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder()
                .setErrorType(new RsvpCaseBuilder().setRsvpError(builder.build()).build()).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(RSVP_ERROR_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(RSVP_ERROR_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testRSVPError6SpecTlv() throws PCEPDeserializerException {
        final RSVPErrorSpecTlvParser parser = new RSVPErrorSpecTlvParser();
        final RsvpErrorBuilder builder = new RsvpErrorBuilder()
                .setNode(new IpAddressNoZone(Ipv6Util.addressForByteBuf(Unpooled.wrappedBuffer(new byte[] {
                    (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78,
                    (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0, (byte) 0x12, (byte) 0x34, (byte) 0x56,
                    (byte) 0x78, (byte) 0x9a, (byte) 0xbc, (byte) 0xde, (byte) 0xf0
                }))))
                .setFlags(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.ErrorSpec
                    .Flags(false, true))
                .setCode(Uint8.valueOf(213))
                .setValue(Uint16.valueOf(50649));
        final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder()
                .setErrorType(new RsvpCaseBuilder().setRsvpError(builder.build()).build()).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(RSVP_ERROR6_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(RSVP_ERROR6_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testUserErrorSpecTlv() throws PCEPDeserializerException {
        final RSVPErrorSpecTlvParser parser = new RSVPErrorSpecTlvParser();
        final UserErrorBuilder builder = new UserErrorBuilder()
                .setEnterprise(new EnterpriseNumber(Uint32.valueOf(12345)))
                .setSubOrg(Uint8.valueOf(5))
                .setValue(Uint16.valueOf(38))
                .setDescription("user desc");
        final RsvpErrorSpec tlv = new RsvpErrorSpecBuilder()
                .setErrorType(new UserCaseBuilder().setUserError(builder.build()).build()).build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(USER_ERROR_BYTES, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(USER_ERROR_BYTES, ByteArray.getAllBytes(buff));
    }

    @Test
    public void testPathBindingTlvMplsLabel() throws PCEPDeserializerException {
        final byte[] pathBindingBytes = {
            0x00, 0x37, 0x00, 0x07, 0x00, 0x00, 0x00, 0x00, (byte) 0xA8, 0x0F, (byte) 0x60, 0x00
        };
        final PathBindingTlvParser parser = new PathBindingTlvParser();
        final PathBinding tlv = new PathBindingBuilder()
            .setBindingType(BindingType.MplsLabel)
            .setFlags(new FlagsBuilder().setRemoval(false).setSpecified(false).build())
            .setBindingValue(new MplsLabelBuilder().setMplsLabel(new MplsLabel(Uint32.valueOf(688374))).build())
            .build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(pathBindingBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(pathBindingBytes, ByteArray.readAllBytes(buff));

        try {
            final byte[] wrong = {0x00, 0x37, 0x00, 0x07, (byte)0xff, 0x00, 0x00, 0x00, 3, 2, 1, 0};
            parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(wrong, 4)));
            fail();
        } catch (final PCEPDeserializerException e) {
            assertEquals("Unsupported Path Binding Type", e.getMessage());
        }
    }

    @Test
    public void testPathBindingTlvMplsLabelEntry() throws PCEPDeserializerException {
        final byte[] pathBindingBytes = {
            0x00, 0x37, 0x00, 0x08, 0x01, (byte) 0x80, 0x00, 0x00, (byte) 0xA8, (byte) 0x0F, (byte) 0x6D, (byte) 0xAD
        };
        final PathBindingTlvParser parser = new PathBindingTlvParser();
        final PathBinding tlv = new PathBindingBuilder()
            .setBindingType(BindingType.MplsLabelEntry)
            .setFlags(new FlagsBuilder().setRemoval(true).setSpecified(false).build())
            .setBindingValue(new MplsLabelEntryBuilder()
                .setTrafficClass(Uint8.valueOf(6))
                .setTimeToLive(Uint8.valueOf(173))
                .setBottomOfStack(true)
                .setLabel(new MplsLabel(Uint32.valueOf(688374))).build())
            .build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(pathBindingBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(pathBindingBytes, ByteArray.readAllBytes(buff));
    }

    @Test
    public void testPathBindingTlvSrv6Entry() throws PCEPDeserializerException {
        final byte[] pathBindingBytes = {
            0x00, 0x37, 0x00, 0x14,
            0x02, 0x40, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01
        };
        final PathBindingTlvParser parser = new PathBindingTlvParser();
        final PathBinding tlv = new PathBindingBuilder()
            .setBindingType(BindingType.Srv6)
            .setFlags(new FlagsBuilder().setRemoval(false).setSpecified(true).build())
            .setBindingValue(new Srv6Builder().setSrv6Address(new Ipv6AddressNoZone("::1")).build())
            .build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(pathBindingBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(pathBindingBytes, ByteArray.readAllBytes(buff));
    }

    @Test
    public void testPathBindingTlvSrv6BehaviorEntry() throws PCEPDeserializerException {
        final byte[] pathBindingBytes = {
            0x00, 0x37, 0x00, 0x1c,
            0x03, 0x40, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x0a,
            0x0a, 0x01, 0x02, 0x00
        };
        final PathBindingTlvParser parser = new PathBindingTlvParser();
        final PathBinding tlv = new PathBindingBuilder()
            .setBindingType(BindingType.Srv6Behavior)
            .setFlags(new FlagsBuilder().setRemoval(false).setSpecified(true).build())
            .setBindingValue(new Srv6BehaviorBuilder()
                .setSrv6Sid(new Ipv6AddressNoZone("::1"))
                .setEndpointBehavior(Uint16.TEN)
                .setLocatorBlockLength(Uint8.TEN)
                .setLocatorNodeLength(Uint8.ONE)
                .setFunctionLength(Uint8.TWO)
                .setArgumentLength(Uint8.ZERO)
                .build())
            .build();
        assertEquals(tlv, parser.parseTlv(Unpooled.wrappedBuffer(ByteArray.cutBytes(pathBindingBytes, 4))));
        final ByteBuf buff = Unpooled.buffer();
        parser.serializeTlv(tlv, buff);
        assertArrayEquals(pathBindingBytes, ByteArray.readAllBytes(buff));
    }
}
