/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public abstract class AbstractObjectWithTlvsParser<T> implements ObjectParser, ObjectSerializer {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractObjectWithTlvsParser.class);

	private static final int TLV_TYPE_F_LENGTH = 2;
	private static final int TLV_LENGTH_F_LENGTH = 2;
	private static final int TLV_HEADER_LENGTH = TLV_LENGTH_F_LENGTH + TLV_TYPE_F_LENGTH;

	protected static final int PADDED_TO = 4;

	private final TlvHandlerRegistry tlvReg;

	protected AbstractObjectWithTlvsParser(final TlvHandlerRegistry tlvReg) {
		this.tlvReg = Preconditions.checkNotNull(tlvReg);
	}

	protected final void parseTlvs(final T builder, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null) {
			throw new IllegalArgumentException("Byte array is mandatory.");
		}
		if (bytes.length == 0) {
			return;
		}

		int length;
		int byteOffset = 0;
		int type = 0;

		while (byteOffset < bytes.length) {
			type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TLV_TYPE_F_LENGTH));
			byteOffset += TLV_TYPE_F_LENGTH;
			length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TLV_LENGTH_F_LENGTH));
			byteOffset += TLV_LENGTH_F_LENGTH;

			if (TLV_HEADER_LENGTH + length > bytes.length) {
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + (TLV_HEADER_LENGTH + length) + "; Expected: <= "
						+ (bytes.length - byteOffset) + ".");
			}

			final byte[] tlvBytes = ByteArray.subByte(bytes, byteOffset, length);

			LOG.trace("Attempt to parse tlv from bytes: {}", ByteArray.bytesToHexString(tlvBytes));
			final TlvParser parser = this.tlvReg.getTlvParser(type);
			if (parser != null) {
				final Tlv tlv = parser.parseTlv(tlvBytes);
				LOG.trace("Tlv was parsed. {}", tlv);
				addTlv(builder, tlv);
			} else {
				LOG.warn("Unknown TLV received. Type {}. Ignoring it.", type);
			}
			byteOffset += length + getPadding(TLV_HEADER_LENGTH + length, PADDED_TO);
		}
	}

	protected final byte[] serializeTlv(final Tlv tlv) {

		final TlvSerializer serializer = this.tlvReg.getTlvSerializer(tlv);
		LOG.trace("Choosen serializer {}", serializer);

		final byte[] typeBytes = ByteArray.intToBytes(serializer.getType(), TLV_TYPE_F_LENGTH);

		final byte[] valueBytes = serializer.serializeTlv(tlv);

		final byte[] lengthBytes = ByteArray.intToBytes(valueBytes.length, TLV_LENGTH_F_LENGTH);

		final byte[] bytes = new byte[TLV_HEADER_LENGTH + valueBytes.length + getPadding(TLV_HEADER_LENGTH + valueBytes.length, PADDED_TO)];

		int byteOffset = 0;
		System.arraycopy(typeBytes, 0, bytes, byteOffset, TLV_TYPE_F_LENGTH);
		byteOffset += TLV_TYPE_F_LENGTH;
		System.arraycopy(lengthBytes, 0, bytes, byteOffset, TLV_LENGTH_F_LENGTH);
		byteOffset += TLV_LENGTH_F_LENGTH;
		System.arraycopy(valueBytes, 0, bytes, byteOffset, valueBytes.length);
		return bytes;
	}

	public abstract void addTlv(final T builder, final Tlv tlv);

	protected static int getPadding(final int length, final int padding) {
		return (padding - (length % padding)) % padding;
	}
}
