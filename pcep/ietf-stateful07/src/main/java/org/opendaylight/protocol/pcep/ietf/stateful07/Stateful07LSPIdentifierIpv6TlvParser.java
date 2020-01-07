/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv6._case.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv6ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

/**
 * Parser for {@link LspIdentifiers}.
 */
public final class Stateful07LSPIdentifierIpv6TlvParser implements TlvParser, TlvSerializer {
    public static final int TYPE = 19;

    private static final int V6_LENGTH = 52;

    @Override
    public LspIdentifiers parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        checkArgument(buffer.readableBytes() == V6_LENGTH, "Length %s does not match LSP Identifiers Ipv6 tlv length.",
                buffer.readableBytes());
        final Ipv6Builder builder = new Ipv6Builder();
        builder.setIpv6TunnelSenderAddress(Ipv6Util.addressForByteBuf(buffer));
        final LspId lspId = new LspId(Uint32.valueOf(buffer.readUnsignedShort()));
        final TunnelId tunnelId = new TunnelId(ByteBufUtils.readUint16(buffer));
        builder.setIpv6ExtendedTunnelId(new Ipv6ExtendedTunnelId(Ipv6Util.addressForByteBuf(buffer)));
        builder.setIpv6TunnelEndpointAddress(Ipv6Util.addressForByteBuf(buffer));
        final AddressFamily afi = new Ipv6CaseBuilder().setIpv6(builder.build()).build();
        return new LspIdentifiersBuilder().setAddressFamily(afi).setLspId(lspId).setTunnelId(tunnelId).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        checkArgument(tlv instanceof LspIdentifiers, "LspIdentifiersTlv is mandatory.");
        final LspIdentifiers lsp = (LspIdentifiers) tlv;
        final ByteBuf body = Unpooled.buffer();
        final Ipv6 ipv6 = ((Ipv6Case) lsp.getAddressFamily()).getIpv6();
        checkArgument(ipv6.getIpv6TunnelSenderAddress() != null, "Ipv6TunnelSenderAddress is mandatory.");
        Ipv6Util.writeIpv6Address(ipv6.getIpv6TunnelSenderAddress(), body);

        final LspId lspId = lsp.getLspId();
        checkArgument(lspId != null, "LspId is mandatory.");
        body.writeShort(lspId.getValue().shortValue());
        final TunnelId tunnelId = lsp.getTunnelId();
        checkArgument(tunnelId != null, "TunnelId is mandatory.");
        ByteBufUtils.write(body, tunnelId.getValue());
        checkArgument(ipv6.getIpv6ExtendedTunnelId() != null, "Ipv6ExtendedTunnelId is mandatory.");
        Ipv6Util.writeIpv6Address(ipv6.getIpv6ExtendedTunnelId(), body);
        checkArgument(ipv6.getIpv6TunnelEndpointAddress() != null, "Ipv6TunnelEndpointAddress is mandatory.");
        Ipv6Util.writeIpv6Address(ipv6.getIpv6TunnelEndpointAddress(), body);
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
