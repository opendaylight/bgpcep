/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
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
public class LSPIdentifierIpv4TlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 18;

	private static final int IP4_F_LENGTH = 4;
	private static final int EX_TUNNEL_ID4_F_LENGTH = 4;

	private static final int LSP_ID_F_LENGTH = 2;
	private static final int TUNNEL_ID_F_LENGTH = 2;

	private static final int V4_LENGTH = 12;

	@Override
	public LspIdentifiers parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0) {
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
		}
		if (valueBytes.length != V4_LENGTH) {
			throw new IllegalArgumentException("Length " + valueBytes.length + " does not match LSP Identifiers Ipv4 tlv length.");
		}
		int position = 0;
		final Ipv4Builder builder = new Ipv4Builder();
		builder.setIpv4TunnelSenderAddress(Ipv4Util.addressForBytes(ByteArray.subByte(valueBytes, position, IP4_F_LENGTH)));
		position = IP4_F_LENGTH;
		final LspId lspId = new LspId(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, position, LSP_ID_F_LENGTH)));
		position += LSP_ID_F_LENGTH;
		final TunnelId tunnelId = new TunnelId(ByteArray.bytesToInt(ByteArray.subByte(valueBytes, position, TUNNEL_ID_F_LENGTH)));
		position += TUNNEL_ID_F_LENGTH;
		builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(Ipv4Util.addressForBytes(ByteArray.subByte(valueBytes, position,
				EX_TUNNEL_ID4_F_LENGTH))));
		final AddressFamily afi = new Ipv4CaseBuilder().setIpv4(builder.build()).build();
		position += EX_TUNNEL_ID4_F_LENGTH;
		return new LspIdentifiersBuilder().setAddressFamily(afi).setLspId(lspId).setTunnelId(tunnelId).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("LspIdentifiersTlv is mandatory.");
		}
		final LspIdentifiers lsp = (LspIdentifiers) tlv;
		final AddressFamily afi = lsp.getAddressFamily();

		if (afi.getImplementedInterface().equals(Ipv6Case.class)) {
			return new LSPIdentifierIpv6TlvParser().serializeTlv(tlv);
		}

		final byte[] bytes = new byte[V4_LENGTH];
		int offset = 0;
		final Ipv4 ipv4 = ((Ipv4Case) afi).getIpv4();
		ByteArray.copyWhole(Ipv4Util.bytesForAddress(ipv4.getIpv4TunnelSenderAddress()), bytes, offset);
		offset += IP4_F_LENGTH;
		ByteArray.copyWhole(ByteArray.longToBytes(lsp.getLspId().getValue(), LSP_ID_F_LENGTH), bytes, offset);
		offset += LSP_ID_F_LENGTH;
		ByteArray.copyWhole(ByteArray.intToBytes(lsp.getTunnelId().getValue(), TUNNEL_ID_F_LENGTH), bytes, offset);
		offset += TUNNEL_ID_F_LENGTH;
		ByteArray.copyWhole(Ipv4Util.bytesForAddress(ipv4.getIpv4ExtendedTunnelId()), bytes, offset);
		return bytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
