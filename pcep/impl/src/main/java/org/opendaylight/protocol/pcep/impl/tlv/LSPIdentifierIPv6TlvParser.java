/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tlv;

import org.opendaylight.protocol.concepts.IPv6Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.concepts.IPv6ExtendedTunnelIdentifier;
import org.opendaylight.protocol.pcep.concepts.LSPIdentifier;
import org.opendaylight.protocol.pcep.concepts.TunnelIdentifier;
import org.opendaylight.protocol.pcep.tlv.IPv6LSPIdentifiersTlv;
import org.opendaylight.protocol.pcep.tlv.LSPIdentifiersTlv;
import org.opendaylight.protocol.util.ByteArray;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.tlv.LSPIdentifiersTlv LSPIdentifiersTlv}
 * parameterized as IPv6Address
 */
public class LSPIdentifierIPv6TlvParser {

	private static final int IP_F_LENGTH = 16;
	private static final int LSP_ID_F_LENGTH = 2;
	private static final int TUNNEL_ID_F_LENGTH = 2;
	private static final int EX_TUNNEL_ID_F_LENGTH = 16;

	private static final int IP_F_OFFSET = 0;
	private static final int LSP_ID_F_OFFSET = IP_F_OFFSET + IP_F_LENGTH;
	private static final int TUNNLE_ID_F_OFFSET = LSP_ID_F_OFFSET + LSP_ID_F_LENGTH;
	private static final int EX_TUNNEL_ID_F_OFFSET = TUNNLE_ID_F_OFFSET + TUNNEL_ID_F_LENGTH;

	private static final int SIZE = EX_TUNNEL_ID_F_OFFSET + EX_TUNNEL_ID_F_LENGTH;

	public static LSPIdentifiersTlv<IPv6Address> parse(byte[] valueBytes) throws PCEPDeserializerException {
		if (valueBytes == null || valueBytes.length == 0)
			throw new IllegalArgumentException("Value bytes array is mandatory. Can't be null or empty.");
		if (valueBytes.length != SIZE)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + valueBytes.length + "; Expected: " + SIZE + ".");

		return new IPv6LSPIdentifiersTlv(new IPv6Address(
				ByteArray.subByte(valueBytes, IP_F_OFFSET, IP_F_LENGTH)), new LSPIdentifier(ByteArray.subByte(valueBytes, LSP_ID_F_OFFSET, LSP_ID_F_LENGTH)),
				new TunnelIdentifier(ByteArray.subByte(valueBytes, TUNNLE_ID_F_OFFSET, TUNNEL_ID_F_LENGTH)),
				new IPv6ExtendedTunnelIdentifier(new IPv6Address(ByteArray.subByte(valueBytes, EX_TUNNEL_ID_F_OFFSET, EX_TUNNEL_ID_F_LENGTH))));
	}

	public static byte[] put(IPv6LSPIdentifiersTlv objToSerialize) {
		if (objToSerialize == null)
			throw new IllegalArgumentException("IPv6LSPIdentifiersTlv is mandatory.");

		final byte[] retBytes = new byte[SIZE];

		ByteArray.copyWhole(objToSerialize.getSenderAddress().getAddress(), retBytes, IP_F_OFFSET);
		ByteArray.copyWhole(objToSerialize.getLspID().getLspId(), retBytes, LSP_ID_F_OFFSET);
		ByteArray.copyWhole(objToSerialize.getTunnelID().getBytes(), retBytes, TUNNLE_ID_F_OFFSET);
		ByteArray.copyWhole(objToSerialize.getExtendedTunnelID().getIdentifier().getAddress(), retBytes, EX_TUNNEL_ID_F_OFFSET);

		return retBytes;
	}

}
