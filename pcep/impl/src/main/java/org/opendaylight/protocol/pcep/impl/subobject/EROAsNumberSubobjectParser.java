/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.SubobjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AsNumberSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.route.subobjects.subobject.type.AsNumberBuilder;

/**
 * Parser for {@link AsNumberSubobject}
 */

public class EROAsNumberSubobjectParser implements SubobjectParser, SubobjectSerializer {
	
	public static final int TYPE = 32;
	
	public static final int AS_NUMBER_LENGTH = 4;

	public static final int AS_NUMBER_OFFSET = 0;

	public static final int CONTENT_LENGTH = AS_NUMBER_LENGTH + AS_NUMBER_OFFSET;

	public AsNumberSubobject parseSubobject(byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		if (buffer.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: "
					+ CONTENT_LENGTH + ".");

		return new AsNumberBuilder().setAsNumber(new AsNumber(ByteArray.bytesToLong(buffer))).build();
	}

	public byte[] serializeSubobject(CSubobject subobject) {
		if (!(subobject instanceof AsNumberSubobject))
			throw new IllegalArgumentException("Unknown subobject instance. Passed " + subobject.getClass()
					+ ". Needed AsNumberSubobject.");

		final byte[] retBytes = new byte[CONTENT_LENGTH];

		System.arraycopy(ByteArray.longToBytes(((AsNumberSubobject) subobject).getAsNumber().getValue()), Long.SIZE / Byte.SIZE
				- AS_NUMBER_LENGTH, retBytes, AS_NUMBER_OFFSET, AS_NUMBER_LENGTH);

		return retBytes;
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
