/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import java.util.Arrays;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.MPReachParser;
import org.opendaylight.protocol.bgp.parser.parameter.AS4BytesCapability;
import org.opendaylight.protocol.bgp.parser.parameter.CapabilityParameter;
import org.opendaylight.protocol.bgp.parser.parameter.GracefulCapability;
import org.opendaylight.protocol.bgp.parser.parameter.MultiprotocolCapability;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.concepts.ASNumber;
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

	private CapabilityParameterParser() {

	}

	/**
	 * Serializes given BGP Capability Parameter to byte array.
	 * 
	 * @param param BGP Capability to be serialized
	 * @return BGP Capability converted to byte array
	 */
	public static byte[] put(final CapabilityParameter cap) {
		if (cap == null)
			throw new IllegalArgumentException("BGP Capability cannot be null");
		logger.trace("Started serializing BGP Capability: {}", cap);
		byte[] value = null;
		if (cap instanceof MultiprotocolCapability) {
			value = putMultiProtocolParameterValue((MultiprotocolCapability) cap);
		} else if (cap instanceof GracefulCapability) {
			value = putGracefulParameterValue((GracefulCapability) cap);
		} else if (cap instanceof AS4BytesCapability) {
			value = putAS4BytesParameterValue((AS4BytesCapability) cap);
		}
		final byte[] bytes = new byte[CODE_SIZE + LENGTH_SIZE + value.length];
		bytes[0] = ByteArray.intToBytes(cap.getCode())[Integer.SIZE / Byte.SIZE - 1];
		bytes[1] = ByteArray.intToBytes(value.length)[Integer.SIZE / Byte.SIZE - 1];
		System.arraycopy(value, 0, bytes, CODE_SIZE + LENGTH_SIZE, value.length);
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
	public static CapabilityParameter parse(final byte[] bytes) throws BGPParsingException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array cannot be null or empty.");
		logger.trace("Started parsing of BGP Capability: {}", Arrays.toString(bytes));
		int byteOffset = 0;
		final int capCode = UnsignedBytes.toInt(bytes[byteOffset++]);
		final int capLength = UnsignedBytes.toInt(bytes[byteOffset++]);
		if (capCode == MultiprotocolCapability.CODE) {
			logger.trace("Parsed BGP Capability.");
			return parseMultiProtocolParameterValue(ByteArray.subByte(bytes, byteOffset, capLength));
		} else if (capCode == AS4BytesCapability.CODE) {
			logger.trace("Parsed AS4B Capability.");
			return parseAS4BParameterValue(ByteArray.subByte(bytes, byteOffset, capLength));
		} else
			logger.debug("Only Multiprotocol Capability Parameter is supported. Received code {}", capCode);
		return null;
	}

	private static byte[] putGracefulParameterValue(final GracefulCapability param) {
		final int RESTART_FLAGS_SIZE = 4; // bits
		final int TIMER_SIZE = 12; // bits
		final int AFI_SIZE = 2; // bytes
		final int SAFI_SIZE = 1; // bytes
		final int AF_FLAGS_SIZE = 1; // bytes
		final byte[] bytes = new byte[(RESTART_FLAGS_SIZE + TIMER_SIZE + (AFI_SIZE * Byte.SIZE + SAFI_SIZE * Byte.SIZE + AF_FLAGS_SIZE
				* Byte.SIZE)
				* param.getTableTypes().size())
				/ Byte.SIZE];
		if (param.isRestartFlag())
			bytes[0] = (byte) 0x80;
		int index = (RESTART_FLAGS_SIZE + TIMER_SIZE) / Byte.SIZE;
		for (final Entry<BGPTableType, Boolean> entry : param.getTableTypes().entrySet()) {
			final byte[] a = putAfi(entry.getKey().getAddressFamily());
			final byte s = putSafi(entry.getKey().getSubsequentAddressFamily());
			final byte f = (entry.getValue()) ? (byte) 0x80 : (byte) 0x00;
			System.arraycopy(a, 0, bytes, index, AFI_SIZE);
			index += AFI_SIZE;
			bytes[index] = s;
			index += SAFI_SIZE;
			bytes[index] = f;
			index += AF_FLAGS_SIZE;
		}
		return bytes;
	}

	private static byte[] putMultiProtocolParameterValue(final MultiprotocolCapability param) {
		final byte[] a = putAfi(param.getAfi());
		final byte s = putSafi(param.getSafi());

		final byte[] bytes = new byte[AFI_SIZE + SAFI_SIZE + 1]; // 2 byte is reserved 2B AFI + 1B Reserved + 1B SAFI
		System.arraycopy(a, 0, bytes, 0, AFI_SIZE);
		bytes[AFI_SIZE + 1] = s; // +1 = reserved
		return bytes;
	}

	private static byte[] putAS4BytesParameterValue(final AS4BytesCapability param) {
		return ByteArray.subByte(ByteArray.longToBytes(param.getASNumber().getAsn()), 4, 4);
	}

	private static MultiprotocolCapability parseMultiProtocolParameterValue(final byte[] bytes) throws BGPParsingException {
		final BGPAddressFamily afi = MPReachParser.parseAfi(ByteArray.bytesToInt(ByteArray.subByte(bytes, 0, AFI_SIZE)));
		final BGPSubsequentAddressFamily safi = MPReachParser.parseSafi(ByteArray.bytesToInt(ByteArray.subByte(bytes, AFI_SIZE + 1,
				SAFI_SIZE)));
		return new MultiprotocolCapability(new BGPTableType(afi, safi));
	}

	private static AS4BytesCapability parseAS4BParameterValue(final byte[] bytes) {
		return new AS4BytesCapability(new ASNumber(ByteArray.bytesToLong(bytes)));
	}

	static byte[] putAfi(final BGPAddressFamily afi) {
		final byte[] a = ByteArray.intToBytes(serializeAfi(afi));
		return ByteArray.subByte(a, Integer.SIZE / Byte.SIZE - AFI_SIZE, AFI_SIZE);
	}

	static byte putSafi(final BGPSubsequentAddressFamily safi) {
		final byte[] a = ByteArray.intToBytes(serializeSafi(safi));
		return ByteArray.subByte(a, Integer.SIZE / Byte.SIZE - SAFI_SIZE, SAFI_SIZE)[0];
	}

	private static int serializeSafi(final BGPSubsequentAddressFamily type) {
		switch (type) {
		case Unicast:
			return 1;
		case MPLSLabeledVPN:
			return 128;
		case Linkstate:
			return MPReachParser.LS_SAFI;
		}
		return 0;
	}

	private static int serializeAfi(final BGPAddressFamily type) {
		switch (type) {
		case IPv4:
			return 1;
		case IPv6:
			return 2;
		case LinkState:
			return MPReachParser.LS_AFI;
		}
		return 0;
	}
}
