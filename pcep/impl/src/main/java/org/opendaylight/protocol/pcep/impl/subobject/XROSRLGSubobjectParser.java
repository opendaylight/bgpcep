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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.ExcludeRouteSubobjects.Attribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.SrlgBuilder;

/**
 * Parser for {@link SrlgSubobject}
 */
public class XROSRLGSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

	public static final int TYPE = 34;

	public static final int SRLG_ID_NUMBER_LENGTH = 4;
	public static final int ATTRIBUTE_LENGTH = 1;

	public static final int SRLG_ID_NUMBER_OFFSET = 0;
	public static final int ATTRIBUTE_OFFSET = SRLG_ID_NUMBER_OFFSET + SRLG_ID_NUMBER_LENGTH + 1; // added reserved
																									// field of size 1

	public static final int CONTENT_LENGTH = ATTRIBUTE_OFFSET + ATTRIBUTE_LENGTH;

	@Override
	public Subobjects parseSubobject(final byte[] buffer, final boolean mandatory) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (buffer.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: "
					+ CONTENT_LENGTH + ".");

		final SubobjectsBuilder builder = new SubobjectsBuilder();
		builder.setMandatory(mandatory);
		builder.setAttribute(Attribute.Srlg);
		builder.setSubobjectType(new SrlgBuilder().setSrlgId(
				new SrlgId(ByteArray.bytesToLong(ByteArray.subByte(buffer, SRLG_ID_NUMBER_OFFSET, SRLG_ID_NUMBER_LENGTH)))).build());
		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		if (!(subobject.getSubobjectType() instanceof SrlgSubobject))
			throw new IllegalArgumentException("Unknown PCEPXROSubobject instance. Passed " + subobject.getSubobjectType().getClass()
					+ ". Needed SrlgSubobject.");

		byte[] retBytes;
		retBytes = new byte[CONTENT_LENGTH];
		final SrlgSubobject specObj = (SrlgSubobject) subobject.getSubobjectType();

		ByteArray.copyWhole(ByteArray.longToBytes(specObj.getSrlgId().getValue()), retBytes, SRLG_ID_NUMBER_OFFSET);
		retBytes[ATTRIBUTE_OFFSET] = (byte) subobject.getAttribute().getIntValue();

		return retBytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
