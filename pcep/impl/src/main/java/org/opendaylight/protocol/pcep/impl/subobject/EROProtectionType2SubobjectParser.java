/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import java.util.Arrays;
import java.util.BitSet;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.EROProtectionSubobject;
import org.opendaylight.protocol.pcep.subobject.EROProtectionType2Subobject;
import org.opendaylight.protocol.util.ByteArray;

public class EROProtectionType2SubobjectParser implements EROProtectionParser {

    public static final int RES_F_LENGTH = 8;

    public static final int RES_F_OFFSET = 0;

    public static final int CONTENT_LENGTH = RES_F_OFFSET + RES_F_LENGTH;

    /*
     * offsets of flags inside reserved field
     */
    public static final int S_FLAG_OFFSET = 0;
    public static final int P_FLAG_OFFSET = 1;
    public static final int N_FLAG_OFFSET = 2;
    public static final int O_FLAG_OFFSET = 3;

    public static final int I_FLAG_OFFSET = 32;
    public static final int R_FLAG_OFFSET = 33;

    @Override
    public EROProtectionSubobject parse(byte[] cutBytes, boolean loose) throws PCEPDeserializerException {
	if (cutBytes == null || cutBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
	if (cutBytes.length < CONTENT_LENGTH)
	    throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + cutBytes.length + "; Expected: >" + CONTENT_LENGTH + ".");

	final BitSet reserved = ByteArray.bytesToBitSet(Arrays.copyOfRange(cutBytes, RES_F_OFFSET, RES_F_LENGTH));

	final byte linkFlags = (byte) (cutBytes[3] & 0x3F);
	final byte lspFlags = (byte) (cutBytes[1] & 0x3F);
	final byte segFlags = (byte) (cutBytes[5] & 0x3F);

	return new EROProtectionType2Subobject(reserved.get(S_FLAG_OFFSET), reserved.get(P_FLAG_OFFSET), reserved.get(N_FLAG_OFFSET),
		reserved.get(N_FLAG_OFFSET), lspFlags, linkFlags, reserved.get(I_FLAG_OFFSET), reserved.get(R_FLAG_OFFSET), segFlags, loose);
    }

    @Override
    public byte[] put(EROProtectionSubobject objToSerialize) {
	if (!(objToSerialize instanceof EROProtectionType2Subobject))
	    throw new IllegalArgumentException("Unknown EROProtectionSubobject instance. Passed " + objToSerialize.getClass()
		    + ". Needed EROProtectionType2Subobject.");

	final byte[] retBytes = new byte[CONTENT_LENGTH];

	final BitSet reserved = new BitSet();
	reserved.set(S_FLAG_OFFSET, ((EROProtectionType2Subobject) objToSerialize).isSecondaryLsp());
	reserved.set(P_FLAG_OFFSET, ((EROProtectionType2Subobject) objToSerialize).isProtectionLsp());
	reserved.set(N_FLAG_OFFSET, ((EROProtectionType2Subobject) objToSerialize).isNotification());
	reserved.set(O_FLAG_OFFSET, ((EROProtectionType2Subobject) objToSerialize).isOperational());
	reserved.set(I_FLAG_OFFSET, ((EROProtectionType2Subobject) objToSerialize).isInPlace());
	reserved.set(R_FLAG_OFFSET, ((EROProtectionType2Subobject) objToSerialize).isRequired());
	System.arraycopy(ByteArray.bitSetToBytes(reserved, RES_F_LENGTH), 0, retBytes, RES_F_OFFSET, RES_F_LENGTH);

	retBytes[3] |= ((EROProtectionType2Subobject) objToSerialize).getLinkFlags() & 0x3F;
	retBytes[1] |= ((EROProtectionType2Subobject) objToSerialize).getLspFlags() & 0x3F;
	retBytes[5] |= ((EROProtectionType2Subobject) objToSerialize).getSegFlags() & 0x3F;

	return retBytes;
    }

}
