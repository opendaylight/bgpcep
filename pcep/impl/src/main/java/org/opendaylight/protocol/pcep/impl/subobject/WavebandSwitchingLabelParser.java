/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.WavebandSwitchingLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.WavebandSwitchingLabelBuilder;

public class WavebandSwitchingLabelParser implements LabelParser, LabelSerializer {

	public static final int CTYPE = 3;

	private static int WAVEB_F_LENGTH = 4;
	private static int START_F_LENGTH = 4;
	private static int END_F_LENGTH = 4;

	private static int CONTENT_LENGTH = WAVEB_F_LENGTH + START_F_LENGTH + END_F_LENGTH;

	@Override
	public LabelType parseLabel(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		if (buffer.length != CONTENT_LENGTH)
			throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + buffer.length + "; Expected: "
					+ CONTENT_LENGTH + ".");

		final WavebandSwitchingLabelBuilder builder = new WavebandSwitchingLabelBuilder();

		int byteOffset = 0;
		builder.setWavebandId(ByteArray.bytesToLong(ByteArray.subByte(buffer, byteOffset, WAVEB_F_LENGTH)));
		byteOffset += WAVEB_F_LENGTH;
		builder.setStartLabel(ByteArray.bytesToLong(ByteArray.subByte(buffer, byteOffset, START_F_LENGTH)));
		byteOffset += START_F_LENGTH;
		builder.setEndLabel(ByteArray.bytesToLong(ByteArray.subByte(buffer, byteOffset, END_F_LENGTH)));

		return builder.build();
	}

	@Override
	public byte[] serializeSubobject(final CLabel subobject) {
		if (!(subobject instanceof WavebandSwitchingLabel))
			throw new IllegalArgumentException("Unknown Label Subobject instance. Passed " + subobject.getClass()
					+ ". Needed WavebandSwitchingLabel.");
		final byte[] retBytes = new byte[CONTENT_LENGTH];

		final WavebandSwitchingLabel obj = (WavebandSwitchingLabel) subobject;

		System.arraycopy(ByteArray.intToBytes(obj.getWavebandId().intValue()), 0, retBytes, 0, WAVEB_F_LENGTH);
		System.arraycopy(ByteArray.intToBytes(obj.getStartLabel().intValue()), 0, retBytes, WAVEB_F_LENGTH, START_F_LENGTH);
		System.arraycopy(ByteArray.intToBytes(obj.getEndLabel().intValue()), 0, retBytes, WAVEB_F_LENGTH + START_F_LENGTH, END_F_LENGTH);

		return retBytes;
	}

	@Override
	public int getType() {
		return CTYPE;
	}
}
