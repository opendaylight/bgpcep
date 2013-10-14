/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.impl.PCEPRROSubobjectParser;
import org.opendaylight.protocol.pcep.object.PCEPSecondaryRecordRouteObject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;

public class PCEPSecondaryRecordRouteObjectParser implements PCEPObjectParser {

    @Override
    public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException {
	if (bytes == null || bytes.length == 0)
	    throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");

	final List<ReportedRouteSubobject> subobjects = PCEPRROSubobjectParser.parse(bytes);
	if (subobjects.isEmpty())
	    throw new PCEPDeserializerException("Empty Secondary Recorded Route Object.");

	return new PCEPSecondaryRecordRouteObject(subobjects, processed, ignored);
    }

    @Override
    public byte[] put(PCEPObject obj) {

	if (!(obj instanceof PCEPSecondaryRecordRouteObject))
	    throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPSecondaryRecordRouteObject.");

	assert !(((PCEPSecondaryRecordRouteObject) obj).getSubobjects().isEmpty()) : "Empty Secondary Record Route Object.";

	return PCEPRROSubobjectParser.put(((PCEPSecondaryRecordRouteObject) obj).getSubobjects());
    }

}
