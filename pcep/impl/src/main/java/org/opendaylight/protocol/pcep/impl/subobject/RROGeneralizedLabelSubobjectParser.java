/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.RROGeneralizedLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.RROLabelSubobject;

public class RROGeneralizedLabelSubobjectParser implements RROLabelParser {

    @Override
    public RROLabelSubobject parse(byte[] cutBytes, boolean upStream) throws PCEPDeserializerException {
	if (cutBytes == null || cutBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

	return new RROGeneralizedLabelSubobject(cutBytes, upStream);
    }

    @Override
    public byte[] put(RROLabelSubobject objToSerialize) {
	if (!(objToSerialize instanceof RROGeneralizedLabelSubobject))
	    throw new IllegalArgumentException("Unknown RROLabelSubobject instance. Passed " + objToSerialize.getClass()
		    + ". Needed RROGeneralizedLabelSubobject.");
	final byte[] retBytes = ((RROGeneralizedLabelSubobject) objToSerialize).getLabel();

	return retBytes;
    }

}
