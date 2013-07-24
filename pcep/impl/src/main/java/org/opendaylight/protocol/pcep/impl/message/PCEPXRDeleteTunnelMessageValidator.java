/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPMessageValidator;
import org.opendaylight.protocol.pcep.message.PCEPXRDeleteTunnelMessage;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;

/**
 * PCEPXRDeleteTunnelMessage validator. Validates message integrity.
 */
public class PCEPXRDeleteTunnelMessageValidator extends PCEPMessageValidator {

	@Override
	public List<PCEPMessage> validate(List<PCEPObject> objects) throws PCEPDeserializerException {
		if (objects == null || objects.isEmpty())
			throw new IllegalArgumentException("Passed list can't be null or empty.");

		PCEPObject obj;

		//PCEPRequestParameterObject rp = null;
		PCEPLspObject lsp = null;
		//	PCEPNoPathObject noPath = null;

		obj = objects.get(0);

		if (obj instanceof PCEPLspObject) {
			lsp = (PCEPLspObject) obj;
		}

		if (lsp == null) {
			throw new PCEPDeserializerException("All objects are mandatory.");
		}

		return Arrays.asList((PCEPMessage) new PCEPXRDeleteTunnelMessage(lsp));
	}

}
