/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.RROLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.RROType1LabelSubobject;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.primitives.UnsignedInts;

public class RROType1LabelSubobjectParser implements RROLabelParser {

    public static final int LABEL_LENGTH = 4;

    @Override
    public RROLabelSubobject parse(byte[] cutBytes, boolean upStream) throws PCEPDeserializerException {
	if (cutBytes == null || cutBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");
	if (cutBytes.length != LABEL_LENGTH)
	    throw new PCEPDeserializerException("Wrong length of array of bytes. Passed: " + cutBytes.length + "; Expected: " + LABEL_LENGTH + ".");

	return new RROType1LabelSubobject(UnsignedInts.toLong(ByteArray.bytesToInt(cutBytes)), upStream);
    }

    @Override
    public byte[] put(RROLabelSubobject objToSerialize) {
	if (!(objToSerialize instanceof RROType1LabelSubobject))
	    throw new IllegalArgumentException("Unknown RROLabelSubobject instance. Passed " + objToSerialize.getClass() + ". Needed RROType1LabelSubobject.");

	return ByteArray.intToBytes((int) ((RROType1LabelSubobject) objToSerialize).getLabel());
    }

}
