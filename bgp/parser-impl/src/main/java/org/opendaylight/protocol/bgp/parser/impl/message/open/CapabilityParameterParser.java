/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.open;

import java.util.Arrays;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for BGP Capability Parameter.
 */
public final class CapabilityParameterParser {

	private static final Logger logger = LoggerFactory.getLogger(CapabilityParameterParser.class);

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

		byte[] bytes = SimpleCapabilityRegistry.INSTANCE.serializeCapability(cap);
		if (bytes == null) {
			throw new IllegalArgumentException("Unhandled capability " + cap);
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
	 * @throws BGPDocumentedException
	 */
	public static CParameters parse(final byte[] bytes) throws BGPParsingException, BGPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Byte array cannot be null or empty.");
		}
		logger.trace("Started parsing of BGP Capability: {}", Arrays.toString(bytes));
		int byteOffset = 0;
		final int capCode = UnsignedBytes.toInt(bytes[byteOffset++]);
		final int capLength = UnsignedBytes.toInt(bytes[byteOffset++]);
		final byte[] paramBody = ByteArray.subByte(bytes, byteOffset, capLength);

		final CParameters ret = SimpleCapabilityRegistry.INSTANCE.parseCapability(capCode, paramBody);
		if (ret == null) {
			logger.debug("Ignoring unsupported capability {}", capCode);
		}
		return ret;
	}
}
