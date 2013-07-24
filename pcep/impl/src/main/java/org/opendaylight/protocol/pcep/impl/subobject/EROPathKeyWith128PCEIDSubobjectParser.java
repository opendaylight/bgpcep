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
import org.opendaylight.protocol.pcep.subobject.EROPathKeyWith128PCEIDSubobject;
import org.opendaylight.protocol.util.ByteArray;

public class EROPathKeyWith128PCEIDSubobjectParser {

    public static final int PK_F_LENGTH = 2;
    public static final int PCE_ID_F_LENGTH = 16;

    public static final int PK_F_OFFSET = 0;
    public static final int PCE_ID_F_OFFSET = PK_F_OFFSET + PK_F_LENGTH;

    public static final int CONTENT_LENGTH = PCE_ID_F_OFFSET + PCE_ID_F_LENGTH;

    public static EROPathKeyWith128PCEIDSubobject parse(byte[] soContentsBytes, boolean loose) throws PCEPDeserializerException {
	if (soContentsBytes == null || soContentsBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
	if (soContentsBytes.length != CONTENT_LENGTH)
	    throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + soContentsBytes.length + "; Expected: >" + CONTENT_LENGTH + ".");

	final int pathKey = ByteArray.bytesToShort(Arrays.copyOfRange(soContentsBytes, PK_F_OFFSET, PCE_ID_F_OFFSET)) & 0xFFFF;

	final byte[] pceId = Arrays.copyOfRange(soContentsBytes, PCE_ID_F_OFFSET, CONTENT_LENGTH);

	return new EROPathKeyWith128PCEIDSubobject(pathKey, pceId, loose);
    }

    public static byte[] put(EROPathKeyWith128PCEIDSubobject objToSerialize) {
	final byte[] retBytes = new byte[CONTENT_LENGTH];

	System.arraycopy(ByteArray.shortToBytes((short) objToSerialize.getPathKey()), 0, retBytes, PK_F_OFFSET, PK_F_LENGTH);

	if (objToSerialize.getPceId().length != PCE_ID_F_LENGTH)
	    throw new IllegalArgumentException("Wrong length of pce id. Passed: " + objToSerialize.getPceId().length + ". Expected: =" + PCE_ID_F_LENGTH);
	System.arraycopy(objToSerialize.getPceId(), 0, retBytes, PCE_ID_F_OFFSET, PCE_ID_F_LENGTH);

	return retBytes;
    }
}
