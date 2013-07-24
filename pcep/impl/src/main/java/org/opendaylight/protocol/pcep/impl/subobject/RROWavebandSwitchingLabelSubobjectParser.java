/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.Arrays;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.RROLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.RROWavebandSwitchingLabelSubobject;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.primitives.UnsignedInts;

public class RROWavebandSwitchingLabelSubobjectParser implements RROLabelParser {

    public static int WAVEB_F_LENGTH = 4;
    public static int START_F_LENGTH = 4;
    public static int END_F_LENGTH = 4;

    public static int WAVEB_F_OFFSET = 0;
    public static int START_F_OFFSET = WAVEB_F_OFFSET + WAVEB_F_LENGTH;
    public static int END_F_OFFSET = START_F_OFFSET + START_F_LENGTH;

    public static int CONTENT_LENGTH = END_F_OFFSET + END_F_LENGTH;

    @Override
    public RROLabelSubobject parse(byte[] cutBytes, boolean upStream) throws PCEPDeserializerException {
	if (cutBytes == null || cutBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

	if (cutBytes.length != CONTENT_LENGTH)
	    throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + cutBytes.length + "; Expected: " + CONTENT_LENGTH + ".");

	return new RROWavebandSwitchingLabelSubobject(UnsignedInts.toLong(ByteArray.bytesToInt(Arrays.copyOfRange(cutBytes, WAVEB_F_OFFSET, START_F_OFFSET))),
		UnsignedInts.toLong(ByteArray.bytesToInt(Arrays.copyOfRange(cutBytes, START_F_OFFSET, END_F_OFFSET))), UnsignedInts.toLong(ByteArray
			.bytesToInt(Arrays.copyOfRange(cutBytes, END_F_OFFSET, CONTENT_LENGTH))), upStream);
    }

    @Override
    public byte[] put(RROLabelSubobject objToSerialize) {
	if (!(objToSerialize instanceof RROWavebandSwitchingLabelSubobject))
	    throw new IllegalArgumentException("Unknown RROLabelSubobject instance. Passed " + objToSerialize.getClass()
		    + ". Needed RROWavebandSwitchingLabelSubobject.");
	final byte[] retBytes = new byte[CONTENT_LENGTH];

	final RROWavebandSwitchingLabelSubobject obj = (RROWavebandSwitchingLabelSubobject) objToSerialize;

	System.arraycopy(ByteArray.intToBytes((int) obj.getWavebandId()), 0, retBytes, WAVEB_F_OFFSET, WAVEB_F_LENGTH);
	System.arraycopy(ByteArray.intToBytes((int) obj.getStartLabel()), 0, retBytes, START_F_OFFSET, START_F_LENGTH);
	System.arraycopy(ByteArray.intToBytes((int) obj.getEndLabel()), 0, retBytes, END_F_OFFSET, END_F_LENGTH);

	return retBytes;
    }

}
