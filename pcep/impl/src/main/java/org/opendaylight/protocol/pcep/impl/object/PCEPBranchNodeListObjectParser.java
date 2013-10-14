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
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPEROSubobjectParser;
import org.opendaylight.protocol.pcep.impl.PCEPObjectParser;
import org.opendaylight.protocol.pcep.object.PCEPBranchNodeListObject;
import org.opendaylight.protocol.pcep.subobject.ExplicitRouteSubobject;

public class PCEPBranchNodeListObjectParser implements PCEPObjectParser {

	@Override
	public PCEPBranchNodeListObject parse(byte[] bytes, boolean processed, boolean ignored) throws PCEPDeserializerException, PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");

		final List<ExplicitRouteSubobject> subobjects = PCEPEROSubobjectParser.parse(bytes);
		if (subobjects.isEmpty())
			throw new PCEPDeserializerException("Empty Branch Node List Object.");

		return new PCEPBranchNodeListObject(subobjects, processed, ignored);
	}

	@Override
	public byte[] put(PCEPObject obj) {
		if (!(obj instanceof PCEPBranchNodeListObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + obj.getClass() + ". Needed PCEPBranchNodeListObject.");

		assert !(((PCEPBranchNodeListObject) obj).getSubobjects().isEmpty()) : "Empty Branch Node List Object.";

		return PCEPEROSubobjectParser.put(((PCEPBranchNodeListObject) obj).getSubobjects());
	}

}
