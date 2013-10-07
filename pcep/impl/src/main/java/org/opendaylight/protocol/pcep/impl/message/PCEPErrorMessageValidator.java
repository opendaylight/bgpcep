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
import org.opendaylight.protocol.pcep.object.CompositeErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPOpenObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

/**
 * PCEPErrorMessage validator. Validates message integrity.
 */
public class PCEPErrorMessageValidator extends PCEPMessageValidator {

	@Override
	public List<Message> validate(final List<PCEPObject> objects) throws PCEPDeserializerException {
		if (objects == null)
			throw new IllegalArgumentException("Passed list can't be null.");

		PCEPOpenObject openObj = null;
		final List<CompositeErrorObject> errors = new ArrayList<CompositeErrorObject>();
		final List<PCEPErrorObject> errorObjects = new ArrayList<PCEPErrorObject>();

		PCEPObject obj;
		int state = 1;
		while (!objects.isEmpty()) {
			obj = objects.get(0);

			if (obj instanceof UnknownObject)
				return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(((UnknownObject) obj).getError())));

			switch (state) {
			case 1:
				if (obj instanceof PCEPErrorObject) {
					errorObjects.add((PCEPErrorObject) obj);
					break;
				}
				state = 2;
			case 2:
				state = 3;
				if (obj instanceof PCEPOpenObject) {
					openObj = (PCEPOpenObject) obj;
					break;
				}
			case 3:
				while (!objects.isEmpty()) {
					CompositeErrorObject comObj;

					try {
						comObj = getValidErrorComposite(objects);
					} catch (final PCEPDocumentedException e) {
						return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(e.getError())));
					}

					if (comObj == null)
						break;

					errors.add(comObj);
				}

				state = 4;
				break;
			}

			if (state == 4) {
				break;
			}

			objects.remove(0);
		}

		if (errors.isEmpty() && errorObjects.isEmpty())
			throw new PCEPDeserializerException("At least one PCEPErrorObject is mandatory.");

		if (!objects.isEmpty())
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);

		return Arrays.asList((Message) new PCEPErrorMessage(openObj, errorObjects, errors));
	}

	private static CompositeErrorObject getValidErrorComposite(final List<PCEPObject> objects) throws PCEPDocumentedException,
			PCEPDeserializerException {
		final List<PCEPRequestParameterObject> requestParameters = new ArrayList<PCEPRequestParameterObject>();
		final List<PCEPErrorObject> errors = new ArrayList<PCEPErrorObject>();
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
				if (obj instanceof PCEPErrorObject) {
					errors.add((PCEPErrorObject) obj);
					state = 2;
					break;
				}
				state = 3;
			}

			if (state == 3)
				break;

			objects.remove(0);
		}

		if (errors.isEmpty())
			return null;

		return new CompositeErrorObject(requestParameters, errors);
	}

}
