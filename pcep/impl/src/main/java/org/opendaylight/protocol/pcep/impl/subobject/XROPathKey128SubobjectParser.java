/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.impl.object.XROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKeyBuilder;

import com.google.common.base.Preconditions;

/**
 * Parser for {@link PathKey}
 */
public class XROPathKey128SubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

	public static final int TYPE = 65;

	private static final int PK_F_LENGTH = 2;

	private static final int PCE128_ID_F_LENGTH = 16;

	private static final int PK_F_OFFSET = 0;
	private static final int PCE_ID_F_OFFSET = PK_F_OFFSET + PK_F_LENGTH;

	private static final int CONTENT128_LENGTH = PCE_ID_F_OFFSET + PCE128_ID_F_LENGTH;

	@Override
	public Subobject parseSubobject(final ByteBuf buffer, final boolean mandatory) throws PCEPDeserializerException {
		Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
		if (buffer.readableBytes() != CONTENT128_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.readableBytes() + "; Expected: >"
					+ CONTENT128_LENGTH + ".");
		}
		final int pathKey = buffer.readUnsignedShort();
		final byte[] pceId = ByteArray.readBytes(buffer, PCE128_ID_F_LENGTH);
		final SubobjectBuilder builder = new SubobjectBuilder();
		final PathKeyBuilder pBuilder = new PathKeyBuilder();
		pBuilder.setPceId(new PceId(pceId));
		pBuilder.setPathKey(new PathKey(pathKey));
		builder.setMandatory(mandatory);
		builder.setSubobjectType(new PathKeyCaseBuilder().setPathKey(pBuilder.build()).build());
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobject subobject) {
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.path.key._case.PathKey pk = ((PathKeyCase) subobject.getSubobjectType()).getPathKey();
		final int pathKey = pk.getPathKey().getValue();
		final byte[] pceId = pk.getPceId().getBinary();
		final byte[] retBytes = new byte[PK_F_LENGTH + pceId.length];
		System.arraycopy(ByteArray.shortToBytes((short) pathKey), 0, retBytes, PK_F_OFFSET, PK_F_LENGTH);
		System.arraycopy(pceId, 0, retBytes, PCE_ID_F_OFFSET, pceId.length);
		return XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), retBytes);
	}
}
