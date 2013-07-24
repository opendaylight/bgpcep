/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.bgp.parser.BGPParameter;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.parameter.CapabilityParameter;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for parameters in BGP Open message.
 */
public final class BGPParameterParser {

	private static final Logger logger = LoggerFactory.getLogger(BGPParameterParser.class);

	private static final int TYPE_SIZE = 1; // bytes
	private static final int LENGTH_SIZE = 1; // bytes
	private static final int CAPABILITIES_OPT_PARAM_TYPE = 2;

	private BGPParameterParser() {

	}

	/**
	 * Serializes given BGP Parameter to byte array. Currently supported only Capability parameters.
	 * 
	 * @param param BGP Parameter to be serialized
	 * @return BGP Parameter converted to byte array
	 */
	public static byte[] put(final BGPParameter param) {
		if (param == null)
			throw new IllegalArgumentException("BGP Parameter cannot be null");
		logger.trace("Started serializing BGPParameter: {}", param);

		byte[] value = null;

		if (param instanceof CapabilityParameter) {
			value = CapabilityParameterParser.put((CapabilityParameter) param);
		} else {
			logger.debug("BGP Parameter not supported.");
			return new byte[] {};
		}

		final byte[] bytes = new byte[TYPE_SIZE + LENGTH_SIZE + value.length];
		System.arraycopy(ByteArray.intToBytes(param.getType()), 3, bytes, 0, TYPE_SIZE);
		System.arraycopy(ByteArray.intToBytes(value.length), 3, bytes, TYPE_SIZE, LENGTH_SIZE);
		System.arraycopy(value, 0, bytes, TYPE_SIZE + LENGTH_SIZE, value.length);
		logger.trace("BGP Parameter serialized to: {}", Arrays.toString(bytes));
		return bytes;
	}

	/**
	 * Parses given byte array to a list of BGP Parameters. Currently supporting only Capability parameters.
	 * 
	 * @param bytes byte array representing BGP Parameters
	 * @return list of BGP Parameters
	 * @throws BGPParsingException if the parsing was unsuccessful
	 */
	public static List<BGPParameter> parse(final byte[] bytes) throws BGPParsingException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array cannot be null or empty.");
		logger.trace("Started parsing of BGP parameter: {}", Arrays.toString(bytes));
		int byteOffset = 0;
		final List<BGPParameter> params = Lists.newArrayList();
		while (byteOffset < bytes.length) {
			final int paramType = UnsignedBytes.toInt(bytes[byteOffset++]);
			final int paramLength = UnsignedBytes.toInt(bytes[byteOffset++]);
			if (paramType == CAPABILITIES_OPT_PARAM_TYPE) {
				final BGPParameter param = CapabilityParameterParser.parse(ByteArray.subByte(bytes, byteOffset, paramLength));
				if (param != null)
					params.add(param);
			} else
				logger.debug("BGP Parameter not recognized. Type: {}", paramType);
			byteOffset += paramLength;
		}
		logger.trace("Parsed BGP parameters: {}", Arrays.toString(params.toArray()));
		return params;
	}
}
