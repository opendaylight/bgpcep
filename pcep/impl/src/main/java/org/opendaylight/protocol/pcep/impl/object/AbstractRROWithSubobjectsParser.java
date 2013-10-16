/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.RROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Subobjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public abstract class AbstractRROWithSubobjectsParser implements ObjectParser, ObjectSerializer {

	private static final Logger logger = LoggerFactory.getLogger(AbstractRROWithSubobjectsParser.class);

	private static final int SUB_TYPE_FLAG_F_LENGTH = 1;
	private static final int SUB_LENGTH_F_LENGTH = 1;
	private static final int SUB_HEADER_LENGTH = SUB_TYPE_FLAG_F_LENGTH + SUB_LENGTH_F_LENGTH;

	public static final int TYPE_FLAG_F_OFFSET = 0;
	public static final int LENGTH_F_OFFSET = TYPE_FLAG_F_OFFSET + SUB_TYPE_FLAG_F_LENGTH;
	public static final int SO_CONTENTS_OFFSET = LENGTH_F_OFFSET + SUB_LENGTH_F_LENGTH;

	protected static final int PADDED_TO = 4;

	private final RROSubobjectHandlerRegistry subobjReg;

	protected AbstractRROWithSubobjectsParser(final RROSubobjectHandlerRegistry subobjReg) {
		this.subobjReg = Preconditions.checkNotNull(subobjReg);
	}

	protected List<Subobjects> parseSubobjects(final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null) {
			throw new IllegalArgumentException("Byte array is mandatory.");
		}

		int type;

		byte[] soContentsBytes;
		int length;
		int offset = 0;

		final List<Subobjects> subs = Lists.newArrayList();

		while (offset < bytes.length) {

			length = ByteArray.bytesToInt(ByteArray.subByte(bytes, offset + LENGTH_F_OFFSET, SUB_LENGTH_F_LENGTH));

			type = bytes[offset + TYPE_FLAG_F_OFFSET];

			if (length > bytes.length - offset) {
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
						+ (bytes.length - offset));
			}

			soContentsBytes = new byte[length - SO_CONTENTS_OFFSET];
			System.arraycopy(bytes, offset + SO_CONTENTS_OFFSET, soContentsBytes, 0, length - SO_CONTENTS_OFFSET);

			logger.debug("Attempt to parse subobject from bytes: {}", ByteArray.bytesToHexString(soContentsBytes));
			final Subobjects sub = this.subobjReg.getSubobjectParser(type).parseSubobject(soContentsBytes);
			logger.debug("Subobject was parsed. {}", sub);

			subs.add(sub);

			offset += length;
		}
		return subs;
	}

	protected final byte[] serializeSubobject(final List<Subobjects> subobjects) {

		final List<byte[]> result = Lists.newArrayList();

		int finalLength = 0;

		for (final Subobjects subobject : subobjects) {

			final RROSubobjectSerializer serializer = this.subobjReg.getSubobjectSerializer(subobject);

			final byte[] valueBytes = serializer.serializeSubobject(subobject);

			final byte[] bytes = new byte[SUB_HEADER_LENGTH + valueBytes.length];

			final byte typeBytes = (ByteArray.cutBytes(ByteArray.intToBytes(serializer.getType()), (Integer.SIZE / 8) - 1)[0]);
			final byte lengthBytes = ByteArray.cutBytes(ByteArray.intToBytes(valueBytes.length), (Integer.SIZE / 8) - 1)[0];

			bytes[0] = typeBytes;
			bytes[1] = lengthBytes;
			System.arraycopy(valueBytes, 0, bytes, SUB_HEADER_LENGTH, valueBytes.length);

			finalLength += bytes.length;
			result.add(bytes);
		}

		final byte[] resultBytes = new byte[finalLength];
		int byteOffset = 0;
		for (final byte[] b : result) {
			System.arraycopy(b, 0, resultBytes, byteOffset, b.length);
			byteOffset += b.length;
		}
		return resultBytes;
	}
}
