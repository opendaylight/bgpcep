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
import org.opendaylight.protocol.pcep.object.PCEPReportedRouteObject;
import org.opendaylight.protocol.pcep.subobject.ReportedRouteSubobject;

/**
 * Parser for {@link org.opendaylight.protocol.pcep.object.PCEPReportedRouteObject
 * PCEPReportedRouteObject}
 */
public class PCEPReportedRouteObjectParser implements PCEPObjectParser {

	@Override
	public PCEPObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");

		final List<ReportedRouteSubobject> subobjects = PCEPRROSubobjectParser.parse(bytes);
		if (subobjects.isEmpty())
			throw new PCEPDeserializerException("Empty Reported Route Object.");

		return new PCEPReportedRouteObject(subobjects, processed);
	}

	@Override
	public byte[] put(PCEPObject obj) {

		if (!(obj instanceof PCEPReportedRouteObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPReportedRouteObject.");

		assert !(((PCEPReportedRouteObject) obj).getSubobjects().isEmpty()) : "Empty Reported Route Object.";

		return PCEPRROSubobjectParser.put(((PCEPReportedRouteObject) obj).getSubobjects());
	}

}
