/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractObjectParser<BUILDER> implements ObjectParser {

	private static final Logger logger = LoggerFactory.getLogger(AbstractObjectParser.class);

	private static final int TYPE_F_LENGTH = 2;
	private static final int LENGTH_F_LENGTH = 2;
	private static final int HEADER_LENGTH = LENGTH_F_LENGTH + TYPE_F_LENGTH;

	private static final int PADDED_TO = 4;

	private final HandlerRegistry registry;

	protected AbstractObjectParser(final HandlerRegistry registry) {
		this.registry = registry;
	}

	protected final void parseTlvs(final BUILDER builder, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");

		int length;
		int byteOffset = 0;
		int type = 0;

		while (byteOffset < bytes.length) {
			type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TYPE_F_LENGTH));
			byteOffset += TYPE_F_LENGTH;
			length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, LENGTH_F_LENGTH));
			byteOffset += LENGTH_F_LENGTH;

			if (HEADER_LENGTH + length > bytes.length - byteOffset)
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + (HEADER_LENGTH + length) + "; Expected: <= "
						+ (bytes.length - byteOffset) + ".");

			final byte[] tlvBytes = ByteArray.subByte(bytes, byteOffset, length);

			logger.trace("Attempt to parse tlv from bytes: {}", ByteArray.bytesToHexString(tlvBytes));
			final Tlv tlv = this.registry.getTlvParser(type).parseTlv(tlvBytes);
			logger.trace("Tlv was parsed. {}", tlv);

			addTlv(builder, tlv);

			byteOffset += length + getPadding(HEADER_LENGTH + length, PADDED_TO);
		}
	}

	public abstract void addTlv(final BUILDER builder, final Tlv tlv);

	private static int getPadding(final int length, final int padding) {
		return (padding - (length % padding)) % padding;
	}
}
