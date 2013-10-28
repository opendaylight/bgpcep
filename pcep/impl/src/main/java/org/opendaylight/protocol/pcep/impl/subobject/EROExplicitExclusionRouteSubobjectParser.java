/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.XROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.SubobjectsBuilder;

import com.google.common.collect.Lists;

public class EROExplicitExclusionRouteSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

	public static final int TYPE = 33;

	private static final int RESERVED = 2;

	private static final int SUB_TYPE_FLAG_F_LENGTH = 1;
	private static final int SUB_LENGTH_F_LENGTH = 1;
	private static final int SUB_HEADER_LENGTH = SUB_TYPE_FLAG_F_LENGTH + SUB_LENGTH_F_LENGTH;

	private static final int TYPE_FLAG_F_OFFSET = 0;
	private static final int LENGTH_F_OFFSET = TYPE_FLAG_F_OFFSET + SUB_TYPE_FLAG_F_LENGTH;
	private static final int SO_CONTENTS_OFFSET = LENGTH_F_OFFSET + SUB_LENGTH_F_LENGTH;

	private final XROSubobjectHandlerRegistry registry;

	public EROExplicitExclusionRouteSubobjectParser(final XROSubobjectHandlerRegistry registry) {
		this.registry = registry;
	}

	@Override
	public Subobjects parseSubobject(final byte[] buffer, final boolean loose) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		final SubobjectsBuilder builder = new SubobjectsBuilder();
		builder.setLoose(loose);
		// builder.setSubobjectType( )
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {

		// return PCEPXROSubobjectParser.put(subobject.getXroSubobjets());
		return new byte[0];
	}

	private List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Subobjects> parseSubobjects(
			final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null) {
			throw new IllegalArgumentException("Byte array is mandatory.");
		}

		int type;

		byte[] soContentsBytes;
		int length;
		int offset = 0;

		final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Subobjects> subs = Lists.newArrayList();

		while (offset < bytes.length) {

			length = ByteArray.bytesToInt(ByteArray.subByte(bytes, offset + LENGTH_F_OFFSET, SUB_LENGTH_F_LENGTH));

			type = bytes[offset + TYPE_FLAG_F_OFFSET];

			if (length > bytes.length - offset) {
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
						+ (bytes.length - offset));
			}

			soContentsBytes = new byte[length - SO_CONTENTS_OFFSET];
			System.arraycopy(bytes, offset + SO_CONTENTS_OFFSET, soContentsBytes, 0, length - SO_CONTENTS_OFFSET);

			final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Subobjects sub = this.registry.getSubobjectParser(
					type).parseSubobject(soContentsBytes, false);

			subs.add(sub);

			offset += length;
		}
		return subs;
	}

	private final byte[] serializeSubobject(
			final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Subobjects> subobjects) {

		final List<byte[]> result = Lists.newArrayList();

		int finalLength = 0;

		for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Subobjects subobject : subobjects) {

			final XROSubobjectSerializer serializer = this.registry.getSubobjectSerializer(subobject);

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

	@Override
	public int getType() {
		return TYPE;
	}
}
