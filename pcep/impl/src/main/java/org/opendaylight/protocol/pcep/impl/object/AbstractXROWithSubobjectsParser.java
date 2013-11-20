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
import org.opendaylight.protocol.pcep.spi.XROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

public abstract class AbstractXROWithSubobjectsParser implements ObjectParser, ObjectSerializer {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractXROWithSubobjectsParser.class);

	private static final int SUB_TYPE_FLAG_F_LENGTH = 1;
	private static final int SUB_LENGTH_F_LENGTH = 1;
	private static final int SUB_HEADER_LENGTH = SUB_TYPE_FLAG_F_LENGTH + SUB_LENGTH_F_LENGTH;

	private static final int TYPE_FLAG_F_OFFSET = 0;
	private static final int LENGTH_F_OFFSET = TYPE_FLAG_F_OFFSET + SUB_TYPE_FLAG_F_LENGTH;
	private static final int SO_CONTENTS_OFFSET = LENGTH_F_OFFSET + SUB_LENGTH_F_LENGTH;

	private final XROSubobjectHandlerRegistry subobjReg;

	protected AbstractXROWithSubobjectsParser(final XROSubobjectHandlerRegistry subobjReg) {
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

			final boolean mandatory = ((bytes[offset] & (1 << 7)) != 0) ? true : false;
			type = UnsignedBytes.checkedCast((bytes[offset] & 0xff) & ~(1 << 7));

			offset += SUB_TYPE_FLAG_F_LENGTH;

			length = UnsignedBytes.toInt(bytes[offset]);

			offset += SUB_LENGTH_F_LENGTH;

			if (length - SUB_HEADER_LENGTH > bytes.length - offset) {
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
						+ (bytes.length - offset));
			}
			soContentsBytes = ByteArray.subByte(bytes, offset, length - SO_CONTENTS_OFFSET);

			LOG.debug("Attempt to parse subobject from bytes: {}", ByteArray.bytesToHexString(soContentsBytes));
			final Subobjects sub = this.subobjReg.getSubobjectParser(type).parseSubobject(soContentsBytes, mandatory);
			LOG.debug("Subobject was parsed. {}", sub);

			subs.add(sub);

			offset += soContentsBytes.length;
		}
		return subs;
	}

	protected final byte[] serializeSubobject(final List<Subobjects> subobjects) {

		final List<byte[]> result = Lists.newArrayList();

		int finalLength = 0;

		for (final Subobjects subobject : subobjects) {

			final XROSubobjectSerializer serializer = this.subobjReg.getSubobjectSerializer(subobject.getSubobjectType());

			final byte typeBytes = (byte) (UnsignedBytes.checkedCast(serializer.getType()) | (subobject.isMandatory() ? 1 << 7 : 0));

			final byte[] valueBytes = serializer.serializeSubobject(subobject);

			final byte lengthBytes = UnsignedBytes.checkedCast(valueBytes.length + SUB_HEADER_LENGTH);

			final byte[] bytes = new byte[valueBytes.length + SUB_HEADER_LENGTH];

			bytes[0] = typeBytes;
			bytes[1] = lengthBytes;
			ByteArray.copyWhole(valueBytes, bytes, SUB_HEADER_LENGTH);

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
