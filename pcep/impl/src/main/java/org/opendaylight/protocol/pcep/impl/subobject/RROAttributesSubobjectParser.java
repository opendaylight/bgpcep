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
import org.opendaylight.protocol.pcep.subobject.RROAttributesSubobject;

public class RROAttributesSubobjectParser {

    public static final int RES_F_LENGTH = 2;

    public static final int RES_F_OFFSET = 0;
    public static final int ATTRS_F_OFFSET = RES_F_OFFSET + RES_F_LENGTH;

    public static RROAttributesSubobject parse(byte[] soContentsBytes) throws PCEPDeserializerException {
	if (soContentsBytes == null || soContentsBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
	if (soContentsBytes.length <= RES_F_LENGTH)
	    throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + soContentsBytes.length + "; Expected: >" + RES_F_LENGTH + ".");

	final byte[] attributes = Arrays.copyOfRange(soContentsBytes, ATTRS_F_OFFSET, soContentsBytes.length);

	return new RROAttributesSubobject(attributes);
    }

    public static byte[] put(RROAttributesSubobject objToSerialize) {
	final byte[] retBytes = new byte[RES_F_LENGTH + objToSerialize.getAttributes().length];

	final byte[] attrs = objToSerialize.getAttributes();

	System.arraycopy(attrs, 0, retBytes, ATTRS_F_OFFSET, attrs.length);

	return retBytes;
    }
}
