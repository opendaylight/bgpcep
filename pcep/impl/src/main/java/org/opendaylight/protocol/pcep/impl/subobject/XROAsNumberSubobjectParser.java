/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.impl.object.XROSubobjectUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.xro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AsNumberSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;

/**
 * Parser for {@link AsNumberCase}
 */
public class XROAsNumberSubobjectParser implements XROSubobjectParser, XROSubobjectSerializer {

	public static final int TYPE = 32;

	private static final int AS_NUMBER_LENGTH = 2;

	private static final int AS_NUMBER_OFFSET = 0;

	private static final int CONTENT_LENGTH = AS_NUMBER_LENGTH + AS_NUMBER_OFFSET;

	@Override
	public Subobject parseSubobject(final byte[] buffer, final boolean mandatory) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (buffer.length != CONTENT_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: "
					+ CONTENT_LENGTH + ".");
		}
		return new SubobjectBuilder().setMandatory(mandatory).setSubobjectType(
				new AsNumberCaseBuilder().setAsNumber(
						new AsNumberBuilder().setAsNumber(new AsNumber(ByteArray.bytesToLong(buffer))).build()).build()).build();
	}

	@Override
	public byte[] serializeSubobject(final Subobject subobject) {
		if (!(subobject.getSubobjectType() instanceof AsNumberCase)) {
			throw new IllegalArgumentException("Unknown PCEPXROSubobject instance. Passed " + subobject.getSubobjectType().getClass()
					+ ". Needed AsNumberCase.");
		}
		final byte[] retBytes = new byte[CONTENT_LENGTH];
		final AsNumberSubobject obj = ((AsNumberCase) subobject.getSubobjectType()).getAsNumber();
		System.arraycopy(ByteArray.longToBytes(obj.getAsNumber().getValue(), AS_NUMBER_LENGTH), 0, retBytes, AS_NUMBER_OFFSET,
				AS_NUMBER_LENGTH);
		return XROSubobjectUtil.formatSubobject(TYPE, subobject.isMandatory(), retBytes);
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
