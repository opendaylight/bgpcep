/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.UnnumberedSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.UnnumberedBuilder;

import com.google.common.primitives.UnsignedBytes;
import com.google.common.primitives.UnsignedInts;

/**
 * Parser for {@link UnnumberedSubobject}
 */
public class XROUnnumberedInterfaceSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

	public static final int TYPE = 4;

	private static final int ATTRIBUTE_LENGTH = 1;
	private static final int ROUTER_ID_NUMBER_LENGTH = 4;
	private static final int INTERFACE_ID_NUMBER_LENGTH = 4;

	private static final int ATTRIBUTE_OFFSET = 1;
	private static final int ROUTER_ID_NUMBER_OFFSET = ATTRIBUTE_OFFSET + ATTRIBUTE_LENGTH;
	private static final int INTERFACE_ID_NUMBER_OFFSET = ROUTER_ID_NUMBER_OFFSET + ROUTER_ID_NUMBER_LENGTH;

	private static final int CONTENT_LENGTH = INTERFACE_ID_NUMBER_OFFSET + INTERFACE_ID_NUMBER_LENGTH;

	@Override
	public Subobjects parseSubobject(final byte[] buffer, final boolean mandatory) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (buffer.length != CONTENT_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: "
					+ CONTENT_LENGTH + ".");
		}

		final SubobjectsBuilder builder = new SubobjectsBuilder();
		final UnnumberedBuilder ubuilder = new UnnumberedBuilder();
		ubuilder.setRouterId(ByteArray.bytesToLong(ByteArray.subByte(buffer, ROUTER_ID_NUMBER_OFFSET, ROUTER_ID_NUMBER_LENGTH)));
		ubuilder.setInterfaceId(UnsignedInts.toLong(ByteArray.bytesToInt(ByteArray.subByte(buffer, INTERFACE_ID_NUMBER_OFFSET,
				INTERFACE_ID_NUMBER_LENGTH))));
		builder.setSubobjectType(ubuilder.build());
		builder.setMandatory(mandatory);
		builder.setAttribute(Attribute.forValue(UnsignedBytes.toInt(buffer[ATTRIBUTE_OFFSET])));
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		if (!(subobject.getSubobjectType() instanceof UnnumberedSubobject)) {
			throw new IllegalArgumentException("Unknown PCEPXROSubobject instance. Passed " + subobject.getSubobjectType().getClass()
					+ ". Needed UnnumberedSubobject.");
		}

		final byte[] retBytes = new byte[CONTENT_LENGTH];
		final UnnumberedSubobject specObj = (UnnumberedSubobject) subobject.getSubobjectType();

		retBytes[ATTRIBUTE_OFFSET] = UnsignedBytes.checkedCast(subobject.getAttribute().getIntValue());
		ByteArray.copyWhole(ByteArray.subByte(ByteArray.longToBytes(specObj.getRouterId()), 4, ROUTER_ID_NUMBER_LENGTH), retBytes,
				ROUTER_ID_NUMBER_OFFSET);
		System.arraycopy(ByteArray.longToBytes(specObj.getInterfaceId()), Long.SIZE / Byte.SIZE - INTERFACE_ID_NUMBER_LENGTH, retBytes,
				INTERFACE_ID_NUMBER_OFFSET, INTERFACE_ID_NUMBER_LENGTH);
		return retBytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
