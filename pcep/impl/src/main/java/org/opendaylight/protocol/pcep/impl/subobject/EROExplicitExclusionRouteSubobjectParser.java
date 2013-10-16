/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Subobjects;

public class EROExplicitExclusionRouteSubobjectParser implements EROSubobjectParser, EROSubobjectSerializer {

	public static final int TYPE = 33;

	@Override
	public Subobjects parseSubobject(final byte[] buffer, final boolean loose) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0)
			throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

		// return new EROExplicitExclusionRouteSubobject(PCEPXROSubobjectParser.parse(cutBytes));
		return null;
	}

	@Override
	public byte[] serializeSubobject(final Subobjects subobject) {
		// return PCEPXROSubobjectParser.put(subobject.getXroSubobjets());
		return new byte[0];
	}

	@Override
	public int getType() {
		return TYPE;
	}
}
