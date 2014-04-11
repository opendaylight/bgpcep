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
import org.opendaylight.protocol.pcep.spi.LabelUtil;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.Type1LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.Type1LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.type1.label._case.Type1LabelBuilder;

import com.google.common.primitives.UnsignedInts;

/**
 * Parser for {@link Type1LabelCase}
 */
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
		return new Type1LabelCaseBuilder().setType1Label(
				new Type1LabelBuilder().setType1Label(UnsignedInts.toLong(ByteArray.bytesToInt(buffer))).build()).build();
	}

	@Override
	public byte[] serializeLabel(final boolean unidirectional, final boolean global, final LabelType subobject) {
		if (!(subobject instanceof Type1LabelCase)) {
			throw new IllegalArgumentException("Unknown Label Subobject instance. Passed " + subobject.getClass()
					+ ". Needed Type1LabelCase.");
		}
		return LabelUtil.formatLabel(CTYPE, unidirectional, global, ByteArray.longToBytes(((Type1LabelCase) subobject).getType1Label().getType1Label().longValue(), LABEL_LENGTH));
	}

	@Override
	public int getType() {
		return CTYPE;
	}
}
