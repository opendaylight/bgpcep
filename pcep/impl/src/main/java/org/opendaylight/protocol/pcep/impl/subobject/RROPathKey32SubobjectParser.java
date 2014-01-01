/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.Arrays;

import org.opendaylight.protocol.pcep.impl.object.RROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.path.key._case.PathKeyBuilder;

public class RROPathKey32SubobjectParser implements RROSubobjectParser, RROSubobjectSerializer {

	public static final int TYPE = 64;

	private static final int PK_F_LENGTH = 2;
	private static final int PCE_ID_F_LENGTH = 4;

	private static final int PK_F_OFFSET = 0;
	private static final int PCE_ID_F_OFFSET = PK_F_OFFSET + PK_F_LENGTH;

	private static final int CONTENT_LENGTH = PCE_ID_F_OFFSET + PCE_ID_F_LENGTH;

	@Override
	public Subobject parseSubobject(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (buffer.length != CONTENT_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: >"
					+ CONTENT_LENGTH + ".");
		}
		final byte[] pceId = Arrays.copyOfRange(buffer, PCE_ID_F_OFFSET, CONTENT_LENGTH);
		final int pathKey = ByteArray.bytesToShort(Arrays.copyOfRange(buffer, PK_F_OFFSET, PCE_ID_F_OFFSET));
		final SubobjectBuilder builder = new SubobjectBuilder();
		final PathKeyBuilder pBuilder = new PathKeyBuilder();
		pBuilder.setPceId(new PceId(pceId));
		pBuilder.setPathKey(new PathKey(pathKey));
		builder.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobject subobject) {
		final PathKeyCase pk = (PathKeyCase) subobject.getSubobjectType();
		final int pathKey = pk.getPathKey().getPathKey().getValue();
		final byte[] pceId = pk.getPathKey().getPceId().getBinary();
		final byte[] retBytes = new byte[PK_F_LENGTH + pceId.length];
		System.arraycopy(ByteArray.shortToBytes((short) pathKey), 0, retBytes, PK_F_OFFSET, PK_F_LENGTH);
		System.arraycopy(pceId, 0, retBytes, PCE_ID_F_OFFSET, pceId.length);
		return RROSubobjectUtil.formatSubobject(TYPE, retBytes);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
