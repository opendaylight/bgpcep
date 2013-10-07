/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import org.opendaylight.protocol.concepts.Ipv6Util;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspIdentifiersTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.address.family.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.tlvs.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.Ipv6ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.TunnelId;

/**
 * Parser for {@link LspIdentifiersTlv}
 */
public class LSPIdentifierIPv6TlvParser implements TlvParser {

	private static final int IP_F_LENGTH = 16;
	private static final int LSP_ID_F_LENGTH = 2;
	private static final int TUNNEL_ID_F_LENGTH = 2;
	private static final int EX_TUNNEL_ID_F_LENGTH = 16;

	@Override
	public LspIdentifiersTlv parseTlv(final byte[] valueBytes) throws PCEPDeserializerException {
		int position = 0;
		if (valueBytes == null || valueBytes.length == 0)
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");

		final AddressFamily afi = new Ipv6Builder().setIpv6TunnelSenderAddress(
				Ipv6Util.addressForBytes(ByteArray.subByte(valueBytes, position, IP_F_LENGTH))).setIpv6ExtendedTunnelId(
				new Ipv6ExtendedTunnelId(Ipv6Util.addressForBytes(ByteArray.subByte(valueBytes, position += IP_F_LENGTH,
						EX_TUNNEL_ID_F_LENGTH)))).build();

		return new LspIdentifiersBuilder().setAddressFamily(afi).setLspId(
				new LspId(ByteArray.bytesToLong(ByteArray.subByte(valueBytes, position += EX_TUNNEL_ID_F_LENGTH, LSP_ID_F_LENGTH)))).setTunnelId(
				new TunnelId(ByteArray.bytesToInt(ByteArray.subByte(valueBytes, position += LSP_ID_F_LENGTH, TUNNEL_ID_F_LENGTH)))).build();
	}
}
