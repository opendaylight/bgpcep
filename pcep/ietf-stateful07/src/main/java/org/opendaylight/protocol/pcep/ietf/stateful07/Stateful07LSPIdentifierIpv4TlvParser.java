/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Address;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeShort;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeUnsignedShort;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;

/**
 * Parser for {@link LspIdentifiers}
 */
public final class Stateful07LSPIdentifierIpv4TlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 18;

    private static final int V4_LENGTH = 16;

    @Override
    public LspIdentifiers parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        Preconditions.checkArgument(buffer.readableBytes() == V4_LENGTH, "Length %s does not match LSP Identifiers Ipv4 tlv length.", buffer.readableBytes());
        final Ipv4Builder builder = new Ipv4Builder();
        builder.setIpv4TunnelSenderAddress(Ipv4Util.addressForByteBuf(buffer));
        final LspId lspId = new LspId((long) buffer.readUnsignedShort());
        final TunnelId tunnelId = new TunnelId(buffer.readUnsignedShort());
        builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(Ipv4Util.addressForByteBuf(buffer)));
        builder.setIpv4TunnelEndpointAddress(Ipv4Util.addressForByteBuf(buffer));
        final AddressFamily afi = new Ipv4CaseBuilder().setIpv4(builder.build()).build();
        return new LspIdentifiersBuilder().setAddressFamily(afi).setLspId(lspId).setTunnelId(tunnelId).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv instanceof LspIdentifiers, "LspIdentifiersTlv is mandatory.");
        final LspIdentifiers lsp = (LspIdentifiers) tlv;
        final AddressFamily afi = lsp.getAddressFamily();
        final ByteBuf body = Unpooled.buffer();
        if (afi.getImplementedInterface().equals(Ipv6Case.class)) {
            new Stateful07LSPIdentifierIpv6TlvParser().serializeTlv(tlv, buffer);
        }
        final Ipv4 ipv4 = ((Ipv4Case) afi).getIpv4();
        Preconditions.checkArgument(ipv4.getIpv4TunnelSenderAddress() != null, "Ipv4TunnelSenderAddress is mandatory.");
        writeIpv4Address(ipv4.getIpv4TunnelSenderAddress(), body);
        Preconditions.checkArgument(lsp.getLspId() != null, "LspId is mandatory.");
        writeShort(lsp.getLspId().getValue().shortValue(), body);
        Preconditions.checkArgument(lsp.getTunnelId() != null, "TunnelId is mandatory.");
        writeUnsignedShort(lsp.getTunnelId().getValue(), body);
        Preconditions.checkArgument(ipv4.getIpv4ExtendedTunnelId() != null, "Ipv4ExtendedTunnelId is mandatory.");
        writeIpv4Address(ipv4.getIpv4ExtendedTunnelId(), body);
        Preconditions.checkArgument(ipv4.getIpv4TunnelEndpointAddress() != null, "Ipv4TunnelEndpointAddress is mandatory.");
        writeIpv4Address(ipv4.getIpv4TunnelEndpointAddress(), body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
