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
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPMessageValidator;
import org.opendaylight.protocol.pcep.impl.object.UnknownObject;
import org.opendaylight.protocol.pcep.message.PCCreateMessage;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.object.CompositeInstantiationObject;
import org.opendaylight.protocol.pcep.object.PCEPEndPointsObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLspaObject;
import org.opendaylight.protocol.pcep.object.PCEPMetricObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject;

/**
 * PCCCreateMessage validator. Validates message integrity.
 */
public class PCCreateMessageValidator extends PCEPMessageValidator {

    @Override
    public List<PCEPMessage> validate(List<PCEPObject> objects) throws PCEPDeserializerException {
	if (objects == null)
	    throw new IllegalArgumentException("Passed list can't be null.");

	final List<CompositeInstantiationObject> insts = new ArrayList<CompositeInstantiationObject>();

	CompositeInstantiationObject inst;
	while (!objects.isEmpty()) {
	    try {
		if ((inst = this.getValidInstantiationObject(objects)) == null)
		    break;
	    } catch (final PCEPDocumentedException e) {
		return Arrays.asList((PCEPMessage) new PCEPErrorMessage(new PCEPErrorObject(e.getError())));
	    }

	    insts.add(inst);
	}

	if (insts.isEmpty())
	    throw new PCEPDeserializerException("At least one CompositeInstantiationObject is mandatory.");

	if (!objects.isEmpty())
	    throw new PCEPDeserializerException("Unprocessed objects: " + objects);

	return Arrays.asList((PCEPMessage) new PCCreateMessage(insts));
    }

    private CompositeInstantiationObject getValidInstantiationObject(List<PCEPObject> objects) throws PCEPDocumentedException {
	if (objects.get(0) instanceof UnknownObject)
	    throw new PCEPDocumentedException("Unknown object", ((UnknownObject) objects.get(0)).getError());
	if (!(objects.get(0) instanceof PCEPEndPointsObject<?>))
	    return null;

	final PCEPEndPointsObject<?> endPoints = ((PCEPEndPointsObject<?>) objects.get(0));
	objects.remove(0);

	if (objects.get(0) instanceof UnknownObject)
	    throw new PCEPDocumentedException("Unknown object", ((UnknownObject) objects.get(0)).getError());
	if (!(objects.get(0) instanceof PCEPLspaObject))
	    throw new PCEPDocumentedException("LSPA Object must be second.", PCEPErrors.LSPA_MISSING);
	final PCEPLspaObject lspa = (PCEPLspaObject) objects.get(0);
	objects.remove(0);

	PCEPExplicitRouteObject ero = null;
	PCEPRequestedPathBandwidthObject bandwidth = null;
	final List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();

	PCEPObject obj;
	int state = 1;
	while (!objects.isEmpty()) {
	    obj = objects.get(0);
	    if (obj instanceof UnknownObject) {
		throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());
	    }

	    switch (state) {
		case 1:
		    state = 2;
		    if (obj instanceof PCEPExplicitRouteObject) {
			ero = (PCEPExplicitRouteObject) obj;
			break;
		    }
		case 2:
		    state = 3;
		    if (obj instanceof PCEPRequestedPathBandwidthObject) {
			bandwidth = (PCEPRequestedPathBandwidthObject) obj;
			break;
		    }
		case 3:
		    state = 4;
		    if (obj instanceof PCEPMetricObject) {
			metrics.add((PCEPMetricObject) obj);
			state = 3;
			break;
		    }
	    }

	    if (state == 4)
		break;

	    objects.remove(0);
	}

	return new CompositeInstantiationObject(endPoints, lspa, ero, bandwidth, metrics);
    }

}
