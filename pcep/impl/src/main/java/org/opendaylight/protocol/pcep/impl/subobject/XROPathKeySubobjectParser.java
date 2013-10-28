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
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKeySubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.subobjects.subobject.type.PathKeyBuilder;

/**
 * Parser for {@link PathKeySubobject}
 */
public class XROPathKeySubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

	public static final int TYPE = 64;

	public static final int TYPE128 = 65;

	private static final int PK_F_LENGTH = 2;
	private static final int PCE_ID_F_LENGTH = 4;

	private static final int PCE128_ID_F_LENGTH = 16;

	private static final int PK_F_OFFSET = 0;
	private static final int PCE_ID_F_OFFSET = PK_F_OFFSET + PK_F_LENGTH;

	private static final int CONTENT_LENGTH = PCE_ID_F_OFFSET + PCE_ID_F_LENGTH;
	private static final int CONTENT128_LENGTH = PCE_ID_F_OFFSET + PCE128_ID_F_LENGTH;

	@Override
	public Subobjects parseSubobject(final byte[] buffer, final boolean mandatory) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		byte[] pceId = null;
		if (buffer.length == CONTENT_LENGTH) {
			pceId = Arrays.copyOfRange(buffer, PCE_ID_F_OFFSET, CONTENT_LENGTH);
		} else if (buffer.length == CONTENT128_LENGTH) {
			pceId = Arrays.copyOfRange(buffer, PCE_ID_F_OFFSET, CONTENT128_LENGTH);
		} else {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: >"
					+ CONTENT_LENGTH + ".");
		}
		final int pathKey = ByteArray.bytesToShort(Arrays.copyOfRange(buffer, PK_F_OFFSET, PCE_ID_F_OFFSET));
		final SubobjectsBuilder builder = new SubobjectsBuilder();
		builder.setMandatory(mandatory);
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.subobjects.subobject.type.path.key.PathKeyBuilder pBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.subobjects.subobject.type.path.key.PathKeyBuilder();
		pBuilder.setPceId(new PceId(pceId));
		pBuilder.setPathKey(new PathKey(pathKey));
		builder.setSubobjectType(new PathKeyBuilder().setPathKey(pBuilder.build()).build());
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.subobjects.subobject.type.PathKey pk = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.subobjects.subobject.type.PathKey) subobject.getSubobjectType();
		final int pathKey = pk.getPathKey().getPathKey().getValue();
		final byte[] pceId = pk.getPathKey().getPceId().getBinary();
		final byte[] retBytes = new byte[PK_F_LENGTH + pceId.length];
		System.arraycopy(ByteArray.shortToBytes((short) pathKey), 0, retBytes, PK_F_OFFSET, PK_F_LENGTH);
		System.arraycopy(pceId, 0, retBytes, PCE_ID_F_OFFSET, pceId.length);
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
