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

import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPMessageValidator;
import org.opendaylight.protocol.pcep.impl.object.UnknownObject;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.message.PCEPXRAddTunnelMessage;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;

/**
 * PCEPXRAddTunnelMessage validator. Validates message integrity.
 */
public class PCEPXRAddTunnelMessageValidator extends PCEPMessageValidator {

	@Override
	public List<PCEPMessage> validate(List<PCEPObject> objects) throws PCEPDeserializerException {
		if (objects == null)
			throw new IllegalArgumentException("Passed list can't be null.");

		PCEPObject obj;
		int state = 1;

		PCEPLspObject lsp = null;
		PCEPEndPointsObject<?> ends = null;
		PCEPExplicitRouteObject ero = null;

		while (!objects.isEmpty()) {
			obj = objects.get(0);
			if (obj instanceof UnknownObject) {
				return Arrays.asList((PCEPMessage) new PCEPErrorMessage(new PCEPErrorObject(((UnknownObject) obj).getError())));
			}

			switch (state) {
				case 1:
					state = 2;
					if (obj instanceof PCEPLspObject) {
						lsp = (PCEPLspObject) obj;
						break;
					}
				case 2:
					state = 3;
					//FIXME: add support for ipv6?
					if (obj instanceof PCEPEndPointsObject<?>) {
						ends = ((PCEPEndPointsObject<?>) obj);
						break;
					}
				case 3:
					state = 4;
					if (obj instanceof PCEPExplicitRouteObject) {
						ero = (PCEPExplicitRouteObject) obj;
						break;
					}
			}

			objects.remove(0);

			if (state == 4)
				break;
		}
		if (lsp == null || ends == null || ero == null) {
			throw new PCEPDeserializerException("All objects are mandatory.");
		}

		if (!objects.isEmpty())
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);

		PCEPXRAddTunnelMessage msg = null;

		if (ends.getSourceAddress() instanceof IPv4Address && ends.getDestinationAddress() instanceof IPv4Address) {
			/*
			 * The generic type is checked above.
			 */
			@SuppressWarnings("unchecked")
			final PCEPEndPointsObject<IPv4Address> ep = (PCEPEndPointsObject<IPv4Address>) ends;
			msg = new PCEPXRAddTunnelMessage(lsp, ep, ero);
		}
		return Arrays.asList((PCEPMessage) msg);
	}
}
