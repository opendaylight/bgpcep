/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.Type1Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.Type1LabelBuilder;

import com.google.common.primitives.UnsignedInts;

public class Type1LabelParser implements LabelParser, LabelSerializer {

	public static final int CTYPE = 1;

	public static final int LABEL_LENGTH = 4;

	@Override
	public LabelType parseLabel(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
		}
		if (buffer.length != LABEL_LENGTH) {
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: " + LABEL_LENGTH
					+ ".");
		}
		return new Type1LabelBuilder().setType1Label(UnsignedInts.toLong(ByteArray.bytesToInt(buffer))).build();
	}

	@Override
	public byte[] serializeLabel(final LabelType subobject) {
		if (!(subobject instanceof Type1Label)) {
			throw new IllegalArgumentException("Unknown Label Subobject instance. Passed " + subobject.getClass() + ". Needed Type1Label.");
		}
		return ByteArray.longToBytes(((Type1Label) subobject).getType1Label().longValue(), LABEL_LENGTH);
	}

	@Override
	public int getType() {
		return CTYPE;
	}
}
