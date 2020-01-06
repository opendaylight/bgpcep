/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.util.ByteBufWriteUtil.writeIpv4Address;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link LspIdentifiers}.
 */
public final class Stateful07LSPIdentifierIpv4TlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 18;

    private static final int V4_LENGTH = 16;

    @Override
    public LspIdentifiers parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        checkArgument(buffer.readableBytes() == V4_LENGTH, "Length %s does not match LSP Identifiers Ipv4 tlv length.",
                buffer.readableBytes());
        final Ipv4Builder builder = new Ipv4Builder()
                .setIpv4TunnelSenderAddress(Ipv4Util.noZoneAddressForByteBuf(buffer));
        final LspId lspId = new LspId(Uint32.valueOf(buffer.readUnsignedShort()));
        final TunnelId tunnelId = new TunnelId(ByteBufUtils.readUint16(buffer));
        builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(Ipv4Util.noZoneAddressForByteBuf(buffer)));
        builder.setIpv4TunnelEndpointAddress(Ipv4Util.noZoneAddressForByteBuf(buffer));
        final AddressFamily afi = new Ipv4CaseBuilder().setIpv4(builder.build()).build();
        return new LspIdentifiersBuilder()
                .setAddressFamily(afi)
                .setLspId(lspId)
                .setTunnelId(tunnelId)
                .build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof LspIdentifiers, "LspIdentifiersTlv is mandatory.");
        final LspIdentifiers lsp = (LspIdentifiers) tlv;
        final AddressFamily afi = lsp.getAddressFamily();
        final ByteBuf body = Unpooled.buffer();
        if (afi.implementedInterface().equals(Ipv6Case.class)) {
            new Stateful07LSPIdentifierIpv6TlvParser().serializeTlv(tlv, buffer);
        }
        final Ipv4 ipv4 = ((Ipv4Case) afi).getIpv4();
        checkArgument(ipv4.getIpv4TunnelSenderAddress() != null, "Ipv4TunnelSenderAddress is mandatory.");
        writeIpv4Address(ipv4.getIpv4TunnelSenderAddress(), body);
        checkArgument(lsp.getLspId() != null, "LspId is mandatory.");
        body.writeShort(lsp.getLspId().getValue().shortValue());
        final TunnelId tunnelId = lsp.getTunnelId();
        checkArgument(tunnelId != null, "TunnelId is mandatory.");
        ByteBufUtils.write(body, tunnelId.getValue());
        checkArgument(ipv4.getIpv4ExtendedTunnelId() != null, "Ipv4ExtendedTunnelId is mandatory.");
        writeIpv4Address(ipv4.getIpv4ExtendedTunnelId(), body);
        checkArgument(ipv4.getIpv4TunnelEndpointAddress() != null, "Ipv4TunnelEndpointAddress is mandatory.");
        writeIpv4Address(ipv4.getIpv4TunnelEndpointAddress(), body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
