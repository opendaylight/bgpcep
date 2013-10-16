/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.Arrays;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.SubobjectsBuilder;

public class EROPathKeySubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

	public static final int TYPE = 64;

	public static final int TYPE128 = 65;

	public static final int PK_F_LENGTH = 2;
	public static final int PCE_ID_F_LENGTH = 4;

	public static final int PCE128_ID_F_LENGTH = 16;

	public static final int PK_F_OFFSET = 0;
	public static final int PCE_ID_F_OFFSET = PK_F_OFFSET + PK_F_LENGTH;

	public static final int CONTENT_LENGTH = PCE_ID_F_OFFSET + PCE_ID_F_LENGTH;

	@Override
	public Subobjects parseSubobject(final byte[] buffer, final boolean loose) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (buffer.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: >"
					+ CONTENT_LENGTH + ".");

		final int pathKey = ByteArray.bytesToShort(Arrays.copyOfRange(buffer, PK_F_OFFSET, PCE_ID_F_OFFSET)) & 0xFFFF;

		final byte[] pceId = Arrays.copyOfRange(buffer, PCE_ID_F_OFFSET, CONTENT_LENGTH);

		final SubobjectsBuilder builder = new SubobjectsBuilder();
		builder.setLoose(loose);
		// builder.setSubobjectType(value);
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		final byte[] retBytes = new byte[CONTENT_LENGTH];

		// System.arraycopy(ByteArray.shortToBytes((short) objToSerialize.getPathKey()), 0, retBytes, PK_F_OFFSET,
		// PK_F_LENGTH);
		//
		// if (objToSerialize.getPceId().length != PCE_ID_F_LENGTH)
		// throw new IllegalArgumentException("Wrong length of pce id. Passed: " + objToSerialize.getPceId().length +
		// ". Expected: ="
		// + PCE_ID_F_LENGTH);
		// System.arraycopy(objToSerialize.getPceId(), 0, retBytes, PCE_ID_F_OFFSET, PCE_ID_F_LENGTH);

		return retBytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}

	public int getType128() {
		return TYPE128;
	}
}
