/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPMessageValidator;
import org.opendaylight.protocol.pcep.impl.object.UnknownObject;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.message.PCEPNotificationMessage;
import org.opendaylight.protocol.pcep.object.CompositeNotifyObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPNotificationObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

/**
 * PCEPNotificationMessage validator. Validates message integrity.
 */
public class PCEPNotificationMessageValidator extends PCEPMessageValidator {

	@Override
	public List<Message> validate(final List<PCEPObject> objects) throws PCEPDeserializerException {
		if (objects == null)
			throw new IllegalArgumentException("Passed list can't be null.");

		final List<CompositeNotifyObject> compositeNotifications = new ArrayList<CompositeNotifyObject>();

		while (!objects.isEmpty()) {
			CompositeNotifyObject comObj;
			try {
				comObj = getValidNotificationComposite(objects);
			} catch (final PCEPDocumentedException e) {
				return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(e.getError())));
			}

			if (comObj == null)
				break;

			compositeNotifications.add(comObj);
		}

		if (compositeNotifications.isEmpty())
			throw new PCEPDeserializerException("Atleast one CompositeNotifiObject is mandatory.");

		if (!objects.isEmpty())
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);

		return Arrays.asList((Message) new PCEPNotificationMessage(compositeNotifications));
	}

	private static CompositeNotifyObject getValidNotificationComposite(final List<PCEPObject> objects) throws PCEPDocumentedException {
		final List<PCEPRequestParameterObject> requestParameters = new ArrayList<PCEPRequestParameterObject>();
		final List<PCEPNotificationObject> notifications = new ArrayList<PCEPNotificationObject>();
		PCEPObject obj;

		int state = 1;
		while (!objects.isEmpty()) {
			obj = objects.get(0);

			if (obj instanceof UnknownObject)
				throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());

			switch (state) {
			case 1:
				state = 2;
				if (obj instanceof PCEPRequestParameterObject) {
					if (((PCEPRequestParameterObject) obj).isProcessed())
						throw new PCEPDocumentedException("Invalid setting of P flag.", PCEPErrors.P_FLAG_NOT_SET);
					requestParameters.add((PCEPRequestParameterObject) obj);
					state = 1;
					break;
				}
			case 2:
				if (obj instanceof PCEPNotificationObject) {
					notifications.add((PCEPNotificationObject) obj);
					state = 2;
					break;
				}
				state = 3;
			}

			if (state == 3)
				break;

			objects.remove(obj);
		}

		if (notifications.isEmpty())
			return null;

		return new CompositeNotifyObject(requestParameters, notifications);
	}
}
