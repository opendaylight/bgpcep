/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.subobject;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.PCEPXROSubobjectParser;
import org.opendaylight.protocol.pcep.subobject.EROExplicitExclusionRouteSubobject;

public class EROExplicitExclusionRouteSubobjectParser {
    public static EROExplicitExclusionRouteSubobject parse(byte[] cutBytes, boolean loose) throws PCEPDeserializerException {
	if (cutBytes == null || cutBytes.length == 0)
	    throw new IllegalArgumentException("Array of bytes is mandatory. Can't be null or empty.");

	return new EROExplicitExclusionRouteSubobject(PCEPXROSubobjectParser.parse(cutBytes));
    }

    public static byte[] put(EROExplicitExclusionRouteSubobject objToSerialize) {
	return PCEPXROSubobjectParser.put(objToSerialize.getXroSubobjets());
    }

}
