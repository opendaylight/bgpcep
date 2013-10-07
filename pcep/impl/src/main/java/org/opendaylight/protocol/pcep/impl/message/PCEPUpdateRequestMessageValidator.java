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
import org.opendaylight.protocol.pcep.impl.PCEPMessageValidator;
import org.opendaylight.protocol.pcep.impl.object.UnknownObject;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.message.PCEPUpdateRequestMessage;
import org.opendaylight.protocol.pcep.object.CompositeUpdPathObject;
import org.opendaylight.protocol.pcep.object.CompositeUpdateRequestObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;
import org.opendaylight.protocol.pcep.object.PCEPLspaObject;
import org.opendaylight.protocol.pcep.object.PCEPMetricObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PCEPUpdateRequestMessage validator. Validates message integrity.
 */
//FIXME: merge with parser
class PCEPUpdateRequestMessageValidator extends PCEPMessageValidator {

	private static final Logger logger = LoggerFactory.getLogger(PCEPUpdateRequestMessageValidator.class);

	@Override
	public List<Message> validate(final List<Object> objects) throws PCEPDeserializerException {
		if (objects == null)
			throw new IllegalArgumentException("Passed list can't be null.");

		final List<CompositeUpdateRequestObject> updateRequests = new ArrayList<CompositeUpdateRequestObject>();

		while (!objects.isEmpty()) {
			if (objects.get(0) instanceof UnknownObject) {
				logger.warn("Unknown object found (should be Lsp) - {}.", objects.get(0));
				return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(((UnknownObject) objects.get(0)).getError())));
			}
			if (!(objects.get(0) instanceof PCEPLspObject))
				return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(PCEPErrors.LSP_MISSING)));

			final PCEPLspObject lsp = (PCEPLspObject) objects.get(0);
			objects.remove(0);

			final List<CompositeUpdPathObject> paths = new ArrayList<CompositeUpdPathObject>();

			if (!objects.isEmpty()) {
				try {
					CompositeUpdPathObject path;
					path = this.getValidCompositePath(objects);
					while (path != null) {
						paths.add(path);
						if (objects.isEmpty())
							break;
						path = this.getValidCompositePath(objects);
					}
				} catch (final PCEPDocumentedException e) {
					logger.warn("Serializing failed: {}.", e);
					return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(e.getError())));
				}
			}

			updateRequests.add(new CompositeUpdateRequestObject(lsp, paths));
		}

		if (updateRequests.isEmpty())
			return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(PCEPErrors.LSP_MISSING)));

		if (!objects.isEmpty())
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);

		return Arrays.asList((Message) new PCEPUpdateRequestMessage(updateRequests));
	}

	private CompositeUpdPathObject getValidCompositePath(final List<Object> objects) throws PCEPDocumentedException {
		if (!(objects.get(0) instanceof PCEPExplicitRouteObject))
			return null;

		if (objects.get(0) instanceof UnknownObject)
			throw new PCEPDocumentedException("Unknown object", ((UnknownObject) objects.get(0)).getError());
		final PCEPExplicitRouteObject explicitRoute = (PCEPExplicitRouteObject) objects.get(0);
		objects.remove(0);

		PCEPLspaObject pathLspa = null;
		PCEPRequestedPathBandwidthObject pathBandwidth = null;
		final List<PCEPMetricObject> pathMetrics = new ArrayList<PCEPMetricObject>();

		Object obj;
		int state = 1;
		while (!objects.isEmpty()) {
			obj = objects.get(0);
			if (obj instanceof UnknownObject) {
				throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());
			}

			switch (state) {
			case 1:
				state = 2;
				if (obj instanceof PCEPLspaObject) {
					pathLspa = (PCEPLspaObject) obj;
					break;
				}
			case 2:
				state = 3;
				if (obj instanceof PCEPRequestedPathBandwidthObject) {
					pathBandwidth = (PCEPRequestedPathBandwidthObject) obj;
					break;
				}
			case 3:
				state = 4;
				if (obj instanceof PCEPMetricObject) {
					pathMetrics.add((PCEPMetricObject) obj);
					state = 3;
					break;
				}
			}

			if (state == 4)
				break;

			objects.remove(0);
		}

		return new CompositeUpdPathObject(explicitRoute, pathLspa, pathBandwidth, pathMetrics);
	}

}
