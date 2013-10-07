/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.concepts.Ipv4Util;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.tlvs.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.TunnelId;

/**
 * Parser for {@link LspIdentifiersTlv}
 */
public class LSPIdentifierIPv4TlvParser implements TlvParser, TlvSerializer {

	private static final int IP_F_LENGTH = 4;
	private static final int LSP_ID_F_LENGTH = 2;
	private static final int TUNNEL_ID_F_LENGTH = 2;
	private static final int EX_TUNNEL_ID_F_LENGTH = 4;

	@Override
	public LspIdentifiersTlv parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		int position = 0;
		if (valueBytes == null || valueBytes.length == 0)
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");

		final AddressFamily afi = new Ipv4Builder().setIpv4TunnelSenderAddress(
				Ipv4Util.addressForBytes(ByteArray.subByte(valueBytes, position, IP_F_LENGTH))).setIpv4ExtendedTunnelId(
				new Ipv4ExtendedTunnelId(Ipv4Util.addressForBytes(ByteArray.subByte(valueBytes, position += IP_F_LENGTH,
						EX_TUNNEL_ID_F_LENGTH)))).build();

		return new LspIdentifiersBuilder().setAddressFamily(afi).setLspId(
				new LspId(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, position += EX_TUNNEL_ID_F_LENGTH, LSP_ID_F_LENGTH)))).setTunnelId(
				new TunnelId(ByteArray.bytesToInt(ByteArray.subByte(valueBytes, position += LSP_ID_F_LENGTH, TUNNEL_ID_F_LENGTH)))).build();
	}

	@Override
	public void serializeTlv(final Tlv tlv, final ByteBuf buffer) {
		if (tlv == null)
			throw new IllegalArgumentException("LspIdentifiersTlv is mandatory.");
		final LspIdentifiersTlv lsp = (LspIdentifiersTlv) tlv;
		final AddressFamily afi = lsp.getAddressFamily();
		if (afi.getClass().equals(Ipv4.class)) {
			final Ipv4 ipv4 = (Ipv4) afi;
			buffer.writeBytes(ipv4.getIpv4TunnelSenderAddress().getValue().getBytes());
			buffer.writeBytes(ByteArray.subByte(ByteArray.longToBytes(lsp.getLspId().getValue()), 6, LSP_ID_F_LENGTH));
			buffer.writeBytes(ByteArray.subByte(ByteArray.intToBytes(lsp.getTunnelId().getValue()), 2, TUNNEL_ID_F_LENGTH));
			buffer.writeBytes(ipv4.getIpv4TunnelSenderAddress().getValue().getBytes());
		} else {
			final Ipv6 ipv6 = (Ipv6) afi;
			buffer.writeBytes(ipv6.getIpv6TunnelSenderAddress().getValue().getBytes());
			buffer.writeBytes(ByteArray.subByte(ByteArray.longToBytes(lsp.getLspId().getValue()), 6, LSP_ID_F_LENGTH));
			buffer.writeBytes(ByteArray.subByte(ByteArray.intToBytes(lsp.getTunnelId().getValue()), 2, TUNNEL_ID_F_LENGTH));
			buffer.writeBytes(ipv6.getIpv6TunnelSenderAddress().getValue().getBytes());
		}
	}
}
