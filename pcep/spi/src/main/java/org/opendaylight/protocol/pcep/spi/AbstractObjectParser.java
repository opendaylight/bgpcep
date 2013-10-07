/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public abstract class AbstractObjectParser<BUILDER> implements ObjectParser, ObjectSerializer {

	private static final Logger logger = LoggerFactory.getLogger(AbstractObjectParser.class);

	private static final int TLV_TYPE_F_LENGTH = 2;
	private static final int TLV_LENGTH_F_LENGTH = 2;
	private static final int TLV_HEADER_LENGTH = TLV_LENGTH_F_LENGTH + TLV_TYPE_F_LENGTH;
	
    private static final int SUB_TYPE_FLAG_F_LENGTH = 1;
    private static final int SUB_LENGTH_F_LENGTH = 1;
    private static final int SUB_HEADER_LENGTH = SUB_TYPE_FLAG_F_LENGTH + SUB_LENGTH_F_LENGTH;
   
	protected static final int PADDED_TO = 4;

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
			type = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TLV_TYPE_F_LENGTH));
			byteOffset += TLV_TYPE_F_LENGTH;
			length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TLV_LENGTH_F_LENGTH));
			byteOffset += TLV_LENGTH_F_LENGTH;

			if (TLV_HEADER_LENGTH + length > bytes.length - byteOffset)
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + (TLV_HEADER_LENGTH + length) + "; Expected: <= "
						+ (bytes.length - byteOffset) + ".");

			final byte[] tlvBytes = ByteArray.subByte(bytes, byteOffset, length);

			logger.trace("Attempt to parse tlv from bytes: {}", ByteArray.bytesToHexString(tlvBytes));
			final Tlv tlv = this.registry.getTlvParser(type).parseTlv(tlvBytes);
			logger.trace("Tlv was parsed. {}", tlv);

			addTlv(builder, tlv);

			byteOffset += length + getPadding(TLV_HEADER_LENGTH + length, PADDED_TO);
		}
	}

	protected final byte[] serializeTlv(final Tlv tlv) {

		final TlvSerializer serializer = this.registry.getTlvSerializer(tlv);

		final byte[] typeBytes = (ByteArray.cutBytes(ByteArray.intToBytes(serializer.getType()), (Integer.SIZE / 8) - TLV_TYPE_F_LENGTH));

		final byte[] valueBytes = serializer.serializeTlv(tlv);

		final byte[] lengthBytes = ByteArray.cutBytes(ByteArray.intToBytes(valueBytes.length), (Integer.SIZE / 8) - TLV_LENGTH_F_LENGTH);

		final byte[] bytes = new byte[TLV_HEADER_LENGTH + valueBytes.length + getPadding(TLV_HEADER_LENGTH + valueBytes.length, PADDED_TO)];

		int byteOffset = 0;
		System.arraycopy(typeBytes, 0, bytes, byteOffset, TLV_TYPE_F_LENGTH);
		System.arraycopy(lengthBytes, 0, bytes, byteOffset += TLV_TYPE_F_LENGTH, TLV_LENGTH_F_LENGTH);
		System.arraycopy(valueBytes, 0, bytes, byteOffset += TLV_LENGTH_F_LENGTH, valueBytes.length);
		return bytes;
	}
	
	protected final void parseSubobjects(final BUILDER builder, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null)
			throw new IllegalArgumentException("Byte array is mandatory.");
		
		boolean loose_flag = false;
		int type;
		int length;
		byte[] soContentsBytes;
		
		int byteOffset = 0;
		
		List<CSubobject> subs = Lists.newArrayList();

		while (byteOffset < bytes.length) {
		    loose_flag = ((bytes[byteOffset] & (1 << 7)) != 0) ? true : false;
		    type = (bytes[byteOffset] & 0xff) & ~(1 << 7);
		      length = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset += 1, SUB_TYPE_FLAG_F_LENGTH));

		    if (length > bytes.length - byteOffset)
			throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= " + (bytes.length - byteOffset));

		    soContentsBytes = new byte[length - (byteOffset += SUB_LENGTH_F_LENGTH)];
		    System.arraycopy(bytes, byteOffset, soContentsBytes, 0, length - byteOffset);

		    logger.trace("Attempt to parse subobject from bytes: {}", ByteArray.bytesToHexString(soContentsBytes));
		    final CSubobject subObj = this.registry.getSubobjectParser(type).parseSubobject(soContentsBytes);
		    logger.trace("Subobject was parsed. {}", subObj);

		    subs.add(subObj);
		    
		    byteOffset += length;
		}
		
		addSubobject(builder, loose_flag, subs);
	}
	
	protected final byte[] serializeSubobject(final CSubobject subobject, final boolean loose) {
		
		SubobjectSerializer serializer = this.registry.getSubobjectSerializer(subobject);
		
		final byte[] valueBytes = serializer.serializeSubobject(subobject);
		
		final byte[] bytes = new byte[SUB_HEADER_LENGTH + valueBytes.length];

		byte typeBytes = (byte) (ByteArray.cutBytes(ByteArray.intToBytes(serializer.getType()), (Integer.SIZE / 8) - 1)[0] | (loose ? 1 << 7 : 0));
		byte lengthBytes = ByteArray.cutBytes(ByteArray.intToBytes(valueBytes.length), (Integer.SIZE / 8) - 1)[0];

		bytes[0] = typeBytes;
		bytes[1] = lengthBytes;
		System.arraycopy(valueBytes, 0, bytes, SUB_HEADER_LENGTH, valueBytes.length);
		return bytes;
	}
	
	public abstract void addSubobject(final BUILDER builder, final boolean loose, final List<CSubobject> subobjects);

	public abstract void addTlv(final BUILDER builder, final Tlv tlv);

	private static int getPadding(final int length, final int padding) {
		return (padding - (length % padding)) % padding;
	}
}
