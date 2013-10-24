/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspIdentifiersTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.address.family.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.address.family.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.address.family.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.address.family.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.tlvs.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.Ipv6ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.TunnelId;

/**
 * Parser for {@link LspIdentifiersTlv}
 */
public class LSPIdentifierTlvParser implements TlvParser, TlvSerializer {

	public static final int TYPE = 18;

	public static final int TYPE_6 = 19;

	private static final int IP6_F_LENGTH = 16;
	private static final int EX_TUNNEL_ID6_F_LENGTH = 16;
	private static final int IP4_F_LENGTH = 4;
	private static final int EX_TUNNEL_ID4_F_LENGTH = 4;

	private static final int LSP_ID_F_LENGTH = 2;
	private static final int TUNNEL_ID_F_LENGTH = 2;

	private static final int V4_LENGTH = 12;
	private static final int V6_LENGTH = 36;

	@Override
	public LspIdentifiersTlv parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		int position = 0;
		if (valueBytes == null || valueBytes.length == 0) {
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
		}
		AddressFamily afi = null;
		LspId lspId = null;
		TunnelId tunnelId = null;
		if (valueBytes.length == V4_LENGTH) {
			final Ipv4Builder builder = new Ipv4Builder();
			builder.setIpv4TunnelSenderAddress(Ipv4Util.addressForBytes(ByteArray.subByte(valueBytes, position, IP4_F_LENGTH)));
			position += IP4_F_LENGTH;
			lspId = new LspId(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, position, LSP_ID_F_LENGTH)));
			position += LSP_ID_F_LENGTH;
			tunnelId = new TunnelId(ByteArray.bytesToInt(ByteArray.subByte(valueBytes, position, TUNNEL_ID_F_LENGTH)));
			position += TUNNEL_ID_F_LENGTH;
			builder.setIpv4ExtendedTunnelId(new Ipv4ExtendedTunnelId(Ipv4Util.addressForBytes(ByteArray.subByte(valueBytes, position,
					EX_TUNNEL_ID4_F_LENGTH))));
			afi = builder.build();
			position += EX_TUNNEL_ID4_F_LENGTH;
		} else if (valueBytes.length == V6_LENGTH) {
			final Ipv6Builder builder = new Ipv6Builder();
			builder.setIpv6TunnelSenderAddress(Ipv6Util.addressForBytes(ByteArray.subByte(valueBytes, position, IP6_F_LENGTH)));
			position += IP6_F_LENGTH;
			lspId = new LspId(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, position, LSP_ID_F_LENGTH)));
			position += LSP_ID_F_LENGTH;
			tunnelId = new TunnelId(ByteArray.bytesToInt(ByteArray.subByte(valueBytes, position, TUNNEL_ID_F_LENGTH)));
			position += TUNNEL_ID_F_LENGTH;
			builder.setIpv6ExtendedTunnelId(new Ipv6ExtendedTunnelId(Ipv6Util.addressForBytes(ByteArray.subByte(valueBytes, position,
					EX_TUNNEL_ID6_F_LENGTH))));
			afi = builder.build();
			position += EX_TUNNEL_ID6_F_LENGTH;
		} else {
			throw new IllegalArgumentException("Length " + valueBytes.length + " does not match LSP Identifiers tlv lengths.");
		}

		return new LspIdentifiersBuilder().setAddressFamily(afi).setLspId(lspId).setTunnelId(tunnelId).build();
	}

	@Override
	public byte[] serializeTlv(final Tlv tlv) {
		if (tlv == null) {
			throw new IllegalArgumentException("LspIdentifiersTlv is mandatory.");
		}
		final LspIdentifiersTlv lsp = (LspIdentifiersTlv) tlv;
		final AddressFamily afi = lsp.getAddressFamily();
		int offset = 0;
		if (afi.getImplementedInterface().equals(Ipv4.class)) {
			final byte[] bytes = new byte[V4_LENGTH];
			final Ipv4 ipv4 = (Ipv4) afi;
			ByteArray.copyWhole(Ipv4Util.bytesForAddress(ipv4.getIpv4TunnelSenderAddress()), bytes, offset);
			offset += IP4_F_LENGTH;
			ByteArray.copyWhole(ByteArray.subByte(ByteArray.longToBytes(lsp.getLspId().getValue()), 6, LSP_ID_F_LENGTH), bytes, offset);
			offset += LSP_ID_F_LENGTH;
			ByteArray.copyWhole(ByteArray.subByte(ByteArray.intToBytes(lsp.getTunnelId().getValue()), 2, TUNNEL_ID_F_LENGTH), bytes, offset);
			offset += TUNNEL_ID_F_LENGTH;
			ByteArray.copyWhole(Ipv4Util.bytesForAddress(ipv4.getIpv4ExtendedTunnelId()), bytes, offset);
			return bytes;
		} else {
			final byte[] bytes = new byte[V6_LENGTH];
			final Ipv6 ipv6 = (Ipv6) afi;
			ByteArray.copyWhole(Ipv6Util.bytesForAddress(ipv6.getIpv6TunnelSenderAddress()), bytes, offset);
			offset += IP6_F_LENGTH;
			ByteArray.copyWhole(ByteArray.subByte(ByteArray.longToBytes(lsp.getLspId().getValue()), 6, LSP_ID_F_LENGTH), bytes, offset);
			offset += LSP_ID_F_LENGTH;
			ByteArray.copyWhole(ByteArray.subByte(ByteArray.intToBytes(lsp.getTunnelId().getValue()), 2, TUNNEL_ID_F_LENGTH), bytes, offset);
			offset += TUNNEL_ID_F_LENGTH;
			ByteArray.copyWhole(Ipv6Util.bytesForAddress(ipv6.getIpv6ExtendedTunnelId()), bytes, offset);
			return bytes;
		}
	}

	@Override
	public int getType() {
		return TYPE;
	}

	public int getType6() {
		return TYPE_6;
	}
}
