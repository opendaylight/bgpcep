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

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for parameters in BGP Open message.
 */
public final class BGPParameterParser {

	private static final Logger logger = LoggerFactory.getLogger(BGPParameterParser.class);

	private static final ParameterRegistry reg = SimpleParameterRegistry.INSTANCE;

	private BGPParameterParser() {

	}

	/**
	 * Serializes given BGP Parameter to byte array. Currently supported only Capability parameters.
	 * 
	 * @param param BGP Parameter to be serialized
	 * @return BGP Parameter converted to byte array
	 */
	public static byte[] put(final BgpParameters param) {
		if (param == null) {
			throw new IllegalArgumentException("BGP Parameter cannot be null");
		}
		logger.trace("Started serializing BGPParameter: {}", param);

		byte[] bytes = reg.serializeParameter(param);
		if (bytes == null) {
			logger.debug("BGP Parameter not supported.");
			return null;
		}

		logger.trace("BGP Parameter serialized to: {}", Arrays.toString(bytes));
		return bytes;
	}

	/**
	 * Parses given byte array to a list of BGP Parameters. Currently supporting only Capability parameters.
	 * 
	 * @param bytes byte array representing BGP Parameters
	 * @return list of BGP Parameters
	 * @throws BGPParsingException if the parsing was unsuccessful
	 * @throws BGPDocumentedException
	 */
	public static List<BgpParameters> parse(final byte[] bytes) throws BGPParsingException, BGPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Byte array cannot be null or empty.");
		}
		logger.trace("Started parsing of BGP parameter: {}", Arrays.toString(bytes));
		int byteOffset = 0;
		final List<BgpParameters> params = Lists.newArrayList();
		while (byteOffset < bytes.length) {
			// FIXME: throw a BGPParsingException here
			final int paramType = UnsignedBytes.toInt(bytes[byteOffset++]);
			final int paramLength = UnsignedBytes.toInt(bytes[byteOffset++]);
			final byte[] paramBody = ByteArray.subByte(bytes, byteOffset, paramLength);
			byteOffset += paramLength;

			final BgpParameters param = reg.parseParameter(paramType, paramBody);
			if (param != null) {
				params.add(param);
			} else {
				logger.debug("Ignoring BGP Parameter type: {}", paramType);
			}
		}

		logger.trace("Parsed BGP parameters: {}", Arrays.toString(params.toArray()));
		return params;
	}
}
