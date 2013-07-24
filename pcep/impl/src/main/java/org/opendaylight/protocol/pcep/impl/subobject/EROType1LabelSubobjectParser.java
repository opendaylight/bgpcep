/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.EROLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.EROType1LabelSubobject;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.primitives.UnsignedInts;

public class EROType1LabelSubobjectParser implements EROLabelParser {

    public static final int LABEL_LENGTH = 4;

    @Override
    public EROLabelSubobject parse(byte[] cutBytes, boolean upStream, boolean loose) throws PCEPDeserializerException {
	if (cutBytes == null || cutBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
	if (cutBytes.length != LABEL_LENGTH)
	    throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + cutBytes.length + "; Expected: " + LABEL_LENGTH + ".");

	return new EROType1LabelSubobject(UnsignedInts.toLong(ByteArray.bytesToInt(cutBytes)), upStream, loose);
    }

    @Override
    public byte[] put(EROLabelSubobject objToSerialize) {
	if (!(objToSerialize instanceof EROType1LabelSubobject))
	    throw new IllegalArgumentException("Unknown EROLabelSubobject instance. Passed " + objToSerialize.getClass() + ". Needed EROType1LabelSubobject.");

	return ByteArray.intToBytes((int) ((EROType1LabelSubobject) objToSerialize).getLabel());
    }

}
