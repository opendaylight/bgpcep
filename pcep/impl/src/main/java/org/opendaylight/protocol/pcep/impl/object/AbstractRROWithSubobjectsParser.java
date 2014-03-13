/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.List;

import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public abstract class AbstractRROWithSubobjectsParser implements ObjectParser, ObjectSerializer {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractRROWithSubobjectsParser.class);

	private static final int SUB_TYPE_FLAG_F_LENGTH = 1;
	private static final int SUB_LENGTH_F_LENGTH = 1;

	private static final int TYPE_FLAG_F_OFFSET = 0;
	private static final int LENGTH_F_OFFSET = TYPE_FLAG_F_OFFSET + SUB_TYPE_FLAG_F_LENGTH;
	private static final int SO_CONTENTS_OFFSET = LENGTH_F_OFFSET + SUB_LENGTH_F_LENGTH;

	private final RROSubobjectRegistry subobjReg;

	protected AbstractRROWithSubobjectsParser(final RROSubobjectRegistry subobjReg) {
		this.subobjReg = Preconditions.checkNotNull(subobjReg);
	}

	protected List<Subobject> parseSubobjects(final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null) {
			throw new IllegalArgumentException("Byte array is mandatory.");
		}

		int type;

		byte[] soContentsBytes;
		int length;
		int offset = 0;

		final List<Subobject> subs = Lists.newArrayList();

		while (offset < bytes.length) {

			length = ByteArray.bytesToInt(ByteArray.subByte(bytes, offset + LENGTH_F_OFFSET, SUB_LENGTH_F_LENGTH));

			type = bytes[offset + TYPE_FLAG_F_OFFSET];

			if (length > bytes.length - offset) {
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
						+ (bytes.length - offset));
			}

			soContentsBytes = new byte[length - SO_CONTENTS_OFFSET];
			System.arraycopy(bytes, offset + SO_CONTENTS_OFFSET, soContentsBytes, 0, length - SO_CONTENTS_OFFSET);

			LOG.debug("Attempt to parse subobject from bytes: {}", ByteArray.bytesToHexString(soContentsBytes));
			final Subobject sub = this.subobjReg.parseSubobject(type, soContentsBytes);
			if (sub == null) {
				LOG.warn("Unknown subobject type: {}. Ignoring subobject.", type);
			} else {
				LOG.debug("Subobject was parsed. {}", sub);
				subs.add(sub);
			}
			offset += length;
		}
		return subs;
	}

	protected final byte[] serializeSubobject(final List<Subobject> subobjects) {
		final List<byte[]> result = Lists.newArrayList();
		int finalLength = 0;
		for (final Subobject subobject : subobjects) {
			final byte[] bytes = this.subobjReg.serializeSubobject(subobject);
			if (bytes == null) {
				LOG.warn("Could not find serializer for subobject type: {}. Skipping subobject.", subobject.getSubobjectType());
			} else  {
				finalLength += bytes.length;
				result.add(bytes);
			}
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
