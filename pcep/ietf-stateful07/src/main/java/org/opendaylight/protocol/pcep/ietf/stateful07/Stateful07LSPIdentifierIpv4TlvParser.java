/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.TlvUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.TunnelId;

/**
 * Parser for {@link LspIdentifiers}
 */
public final class Stateful07LSPIdentifierIpv4TlvParser implements TlvParser, TlvSerializer {

    public static final int TYPE = 18;

    private static final int EX_TUNNEL_ID4_F_LENGTH = 4;

    private static final int V4_LENGTH = 16;

    @Override
    public LspIdentifiers parseTlv(final ByteBuf buffer) throws PCEPDeserializerException {
        if (buffer == null) {
            return null;
        }
        if (buffer.readableBytes() != V4_LENGTH) {
            throw new IllegalArgumentException("Length " + buffer.readableBytes() + " does not match LSP Identifiers Ipv4 tlv length.");
        }
        final Ipv4Builder builder = new Ipv4Builder();
        builder.setIpv4TunnelSenderAddress(Ipv4Util.addressForBytes(ByteArray.readBytes(buffer, Ipv4Util.IP4_LENGTH)));
        final LspId lspId = new LspId((long) buffer.readUnsignedShort());
        final TunnelId tunnelId = new TunnelId(buffer.readUnsignedShort());
        builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(Ipv4Util.addressForBytes(ByteArray.readBytes(buffer,
                EX_TUNNEL_ID4_F_LENGTH))));
        builder.setIpv4TunnelEndpointAddress(Ipv4Util.addressForBytes(ByteArray.readBytes(buffer, Ipv4Util.IP4_LENGTH)));
        final AddressFamily afi = new Ipv4CaseBuilder().setIpv4(builder.build()).build();
        return new LspIdentifiersBuilder().setAddressFamily(afi).setLspId(lspId).setTunnelId(tunnelId).build();
    }

    @Override
    public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
        Preconditions.checkArgument(tlv != null, "LspIdentifiersTlv is mandatory.");
        final LspIdentifiers lsp = (LspIdentifiers) tlv;
        final AddressFamily afi = lsp.getAddressFamily();
        final ByteBuf body = Unpooled.buffer();
        if (afi.getImplementedInterface().equals(Ipv6Case.class)) {
            new Stateful07LSPIdentifierIpv6TlvParser().serializeTlv(tlv, buffer);
        }
        final Ipv4 ipv4 = ((Ipv4Case) afi).getIpv4();
        body.writeBytes(Ipv4Util.bytesForAddress(ipv4.getIpv4TunnelSenderAddress()));
        body.writeShort(lsp.getLspId().getValue().shortValue());
        body.writeShort(lsp.getTunnelId().getValue().shortValue());
        body.writeBytes(Ipv4Util.bytesForAddress(ipv4.getIpv4ExtendedTunnelId()));
        body.writeBytes(Ipv4Util.bytesForAddress(ipv4.getIpv4TunnelEndpointAddress()));
        TlvUtil.formatTlv(TYPE, body, buffer);
    }
}
