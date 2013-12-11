/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.impl.object.EROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.UnnumberedSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.UnnumberedCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.unnumbered._case.UnnumberedBuilder;

import com.google.common.primitives.UnsignedInts;

/**
 * Parser for {@link UnnumberedCase}
 */
public class EROUnnumberedInterfaceSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

	public static final int TYPE = 4;

	private static final int ROUTER_ID_NUMBER_LENGTH = 4;
	private static final int INTERFACE_ID_NUMBER_LENGTH = 4;

	private static final int ROUTER_ID_NUMBER_OFFSET = 2;
	private static final int INTERFACE_ID_NUMBER_OFFSET = ROUTER_ID_NUMBER_OFFSET + ROUTER_ID_NUMBER_LENGTH;

	private static final int CONTENT_LENGTH = INTERFACE_ID_NUMBER_OFFSET + INTERFACE_ID_NUMBER_LENGTH;

	@Override
	public Subobjects parseSubobject(final byte[] buffer, final boolean loose) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (buffer.length != CONTENT_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: "
					+ CONTENT_LENGTH + ".");
		}
		final SubobjectsBuilder builder = new SubobjectsBuilder();
		builder.setLoose(loose);
		final UnnumberedBuilder ubuilder = new UnnumberedBuilder();
		ubuilder.setRouterId(ByteArray.bytesToLong(ByteArray.subByte(buffer, ROUTER_ID_NUMBER_OFFSET, ROUTER_ID_NUMBER_LENGTH)));
		ubuilder.setInterfaceId(UnsignedInts.toLong(ByteArray.bytesToInt(ByteArray.subByte(buffer, INTERFACE_ID_NUMBER_OFFSET,
				INTERFACE_ID_NUMBER_LENGTH))));
		builder.setSubobjectType(new UnnumberedCaseBuilder().setUnnumbered(ubuilder.build()).build());
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		if (!(subobject.getSubobjectType() instanceof UnnumberedCase)) {
			throw new IllegalArgumentException("Unknown ExplicitRouteSubobject instance. Passed " + subobject.getSubobjectType().getClass()
					+ ". Needed UnnumberedCase.");
		}
		byte[] retBytes;
		retBytes = new byte[CONTENT_LENGTH];
		final UnnumberedSubobject specObj = ((UnnumberedCase) subobject.getSubobjectType()).getUnnumbered();
		ByteArray.copyWhole(ByteArray.longToBytes(specObj.getRouterId(), ROUTER_ID_NUMBER_LENGTH), retBytes, ROUTER_ID_NUMBER_OFFSET);
		System.arraycopy(ByteArray.longToBytes(specObj.getInterfaceId(), INTERFACE_ID_NUMBER_LENGTH), 0, retBytes,
				INTERFACE_ID_NUMBER_OFFSET, INTERFACE_ID_NUMBER_LENGTH);
		return EROSubobjectUtil.formatSubobject(TYPE, subobject.isLoose(), retBytes);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
