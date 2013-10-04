/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import java.util.Arrays;

import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.ParserUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.CAs4Bytes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.CAs4BytesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.c.parameters.c.as4.bytes.As4BytesCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.CMultiprotocolBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130918.open.bgp.parameters.c.parameters.c.multiprotocol.MultiprotocolCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for BGP Capability Parameter.
 */
public final class CapabilityParameterParser {

	private static final Logger logger = LoggerFactory.getLogger(CapabilityParameterParser.class);

	private static final int CODE_SIZE = 1; // bytes
	private static final int LENGTH_SIZE = 1; // bytes
	private static final int AFI_SIZE = 2; // bytes
	private static final int SAFI_SIZE = 1; // bytes

	private static final int As4BytesCapability_CODE = 65;
	private static final int Multiprotocol_CODE = 1;

	private CapabilityParameterParser() {

	}

	/**
	 * Serializes given BGP Capability Parameter to byte array.
	 * 
	 * @param param BGP Capability to be serialized
	 * @return BGP Capability converted to byte array
	 */
	public static byte[] put(final CParameters cap) {
		if (cap == null) {
			throw new IllegalArgumentException("BGP Capability cannot be null");
		}
		logger.trace("Started serializing BGP Capability: {}", cap);
		byte[] value = null;
		byte[] bytes = null;
		if (cap instanceof CMultiprotocol) {
			value = putMultiProtocolParameterValue((CMultiprotocol) cap);
			bytes = new byte[CODE_SIZE + LENGTH_SIZE + value.length];
			bytes[0] = ByteArray.intToBytes(Multiprotocol_CODE)[Integer.SIZE / Byte.SIZE - 1];
			// } else if (cap instanceof GracefulCapability) {
			// value = putGracefulParameterValue((GracefulCapability) cap);
		} else if (cap instanceof CAs4Bytes) {
			value = putAS4BytesParameterValue((CAs4Bytes) cap);
			bytes = new byte[CODE_SIZE + LENGTH_SIZE + value.length];
			bytes[0] = ByteArray.intToBytes(As4BytesCapability_CODE)[Integer.SIZE / Byte.SIZE - 1];
		}
		if (bytes != null) {
			bytes[1] = ByteArray.intToBytes(value.length)[Integer.SIZE / Byte.SIZE - 1];
			System.arraycopy(value, 0, bytes, CODE_SIZE + LENGTH_SIZE, value.length);
		}
		logger.trace("BGP Parameter serialized to: {}", Arrays.toString(bytes));
		return bytes;
	}

	/**
	 * Parses given byte array to Capability Parameter. Only Multiprotocol capability is supported.
	 * 
	 * @param bytes byte array representing BGP Parameters
	 * @return list of BGP Parameters
	 * @throws BGPParsingException if the parsing was unsuccessful
	 */
	public static CParameters parse(final byte[] bytes) throws BGPParsingException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Byte array cannot be null or empty.");
		}
		logger.trace("Started parsing of BGP Capability: {}", Arrays.toString(bytes));
		int byteOffset = 0;
		final int capCode = UnsignedBytes.toInt(bytes[byteOffset++]);
		final int capLength = UnsignedBytes.toInt(bytes[byteOffset++]);
		if (capCode == Multiprotocol_CODE) {
			logger.trace("Parsed BGP Capability.");
			return parseMultiProtocolParameterValue(ByteArray.subByte(bytes, byteOffset, capLength));
		} else if (capCode == As4BytesCapability_CODE) {
			logger.trace("Parsed AS4B Capability.");
			return parseAS4BParameterValue(ByteArray.subByte(bytes, byteOffset, capLength));
		} else {
			logger.debug("Only Multiprotocol Capability Parameter is supported. Received code {}", capCode);
		}
		return null;
	}

	// private static byte[] putGracefulParameterValue(final GracefulCapability param) {
	// final int RESTART_FLAGS_SIZE = 4; // bits
	// final int TIMER_SIZE = 12; // bits
	// final int AFI_SIZE = 2; // bytes
	// final int SAFI_SIZE = 1; // bytes
	// final int AF_FLAGS_SIZE = 1; // bytes
	// final byte[] bytes = new byte[(RESTART_FLAGS_SIZE + TIMER_SIZE + (AFI_SIZE * Byte.SIZE + SAFI_SIZE * Byte.SIZE +
	// AF_FLAGS_SIZE
	// * Byte.SIZE)
	// * param.getTableTypes().size())
	// / Byte.SIZE];
	// if (param.isRestartFlag()) {
	// bytes[0] = (byte) 0x80;
	// }
	// int index = (RESTART_FLAGS_SIZE + TIMER_SIZE) / Byte.SIZE;
	// for (final Entry<BGPTableType, Boolean> entry : param.getTableTypes().entrySet()) {
	// final byte[] a = putAfi(entry.getKey().getAddressFamily());
	// final byte s = putSafi(entry.getKey().getSubsequentAddressFamily());
	// final byte f = (entry.getValue()) ? (byte) 0x80 : (byte) 0x00;
	// System.arraycopy(a, 0, bytes, index, AFI_SIZE);
	// index += AFI_SIZE;
	// bytes[index] = s;
	// index += SAFI_SIZE;
	// bytes[index] = f;
	// index += AF_FLAGS_SIZE;
	// }
	// return bytes;
	// }

	private static byte[] putMultiProtocolParameterValue(final CMultiprotocol param) {
		final byte[] a = putAfi(param.getMultiprotocolCapability().getAfi());
		final byte s = putSafi(param.getMultiprotocolCapability().getSafi());

		final byte[] bytes = new byte[AFI_SIZE + SAFI_SIZE + 1]; // 2 byte is reserved 2B AFI + 1B Reserved + 1B SAFI
		System.arraycopy(a, 0, bytes, 0, AFI_SIZE);
		bytes[AFI_SIZE + 1] = s; // +1 = reserved
		return bytes;
	}

	private static byte[] putAS4BytesParameterValue(final CAs4Bytes param) {
		return ByteArray.subByte(ByteArray.longToBytes(param.getAs4BytesCapability().getAsNumber().getValue()), 4, 4);
	}

	private static CMultiprotocol parseMultiProtocolParameterValue(final byte[] bytes) throws BGPParsingException {
		final Class<? extends AddressFamily> afi = ParserUtil.afiForValue(ByteArray.bytesToInt(ByteArray.subByte(bytes, 0, AFI_SIZE)));
		if (afi == null) {
			throw new BGPParsingException("Address Family Identifier: '" + ByteArray.bytesToInt(ByteArray.subByte(bytes, 0, AFI_SIZE))
					+ "' not supported.");
		}
		final Class<? extends SubsequentAddressFamily> safi = ParserUtil.safiForValue(ByteArray.bytesToInt(ByteArray.subByte(bytes,
				AFI_SIZE + 1, SAFI_SIZE)));
		if (safi == null) {
			throw new BGPParsingException("Subsequent Address Family Identifier: '"
					+ ByteArray.bytesToInt(ByteArray.subByte(bytes, AFI_SIZE + 1, SAFI_SIZE)) + "' not supported.");
		}

		return new CMultiprotocolBuilder().setMultiprotocolCapability(
				new MultiprotocolCapabilityBuilder().setAfi(afi).setSafi(safi).build()).build();
	}

	private static CAs4Bytes parseAS4BParameterValue(final byte[] bytes) {
		return new CAs4BytesBuilder().setAs4BytesCapability(
				new As4BytesCapabilityBuilder().setAsNumber(new AsNumber(ByteArray.bytesToLong(bytes))).build()).build();
	}

	private static byte[] putAfi(final Class<? extends AddressFamily> afi) {
		final byte[] a = ByteArray.intToBytes(ParserUtil.valueForAfi(afi));
		return ByteArray.subByte(a, Integer.SIZE / Byte.SIZE - AFI_SIZE, AFI_SIZE);
	}

	private static byte putSafi(final Class<? extends SubsequentAddressFamily> safi) {
		final byte[] a = ByteArray.intToBytes(ParserUtil.valueForSafi(safi));
		return ByteArray.subByte(a, Integer.SIZE / Byte.SIZE - SAFI_SIZE, SAFI_SIZE)[0];
	}
}
