/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPMessageValidator;
import org.opendaylight.protocol.pcep.message.PCEPOpenMessage;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;

/**
 * PCEPOpenMessage validator. Validates message integrity.
 */
public class PCEPOpenMessageValidator extends PCEPMessageValidator {

	@Override
	public List<PCEPMessage> validate(List<PCEPObject> objects) throws PCEPDeserializerException {
		if (objects == null)
			throw new IllegalArgumentException("Passed list can't be null.");

		if (objects.isEmpty() || !(objects.get(0) instanceof PCEPOpenObject))
			throw new PCEPDeserializerException("Open message doesn't contain OPEN object.");

		final PCEPOpenMessage msg = new PCEPOpenMessage((PCEPOpenObject) objects.get(0));
		objects.remove(0);

		if (!objects.isEmpty())
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);

		return new ArrayList<PCEPMessage>() {
			private static final long serialVersionUID = 1L;
			{
				this.add(msg);
			}
		};
	}
}
