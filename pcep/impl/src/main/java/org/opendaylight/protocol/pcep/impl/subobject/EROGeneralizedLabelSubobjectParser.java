/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.subobject.EROGeneralizedLabelSubobject;
import org.opendaylight.protocol.pcep.subobject.EROLabelSubobject;

public class EROGeneralizedLabelSubobjectParser implements EROLabelParser {

    @Override
    public EROLabelSubobject parse(byte[] cutBytes, boolean upStream, boolean loose) throws PCEPDeserializerException {
	if (cutBytes == null || cutBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

	return new EROGeneralizedLabelSubobject(cutBytes, upStream, loose);
    }

    @Override
    public byte[] put(EROLabelSubobject objToSerialize) {
	if (!(objToSerialize instanceof EROGeneralizedLabelSubobject))
	    throw new IllegalArgumentException("Unknown EROLabelSubobject instance. Passed " + objToSerialize.getClass()
		    + ". Needed EROGeneralizedLabelSubobject.");
	final byte[] retBytes = ((EROGeneralizedLabelSubobject) objToSerialize).getLabel();

	return retBytes;
    }

}
