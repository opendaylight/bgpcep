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
import org.opendaylight.protocol.pcep.message.PCEPReportMessage;
import org.opendaylight.protocol.pcep.object.CompositeRptPathObject;
import org.opendaylight.protocol.pcep.object.CompositeStateReportObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPExistingPathBandwidthObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;
import org.opendaylight.protocol.pcep.object.PCEPLspaObject;
import org.opendaylight.protocol.pcep.object.PCEPMetricObject;
import org.opendaylight.protocol.pcep.object.PCEPReportedRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;

/**
 * PCEPReportMessage validator. Validates message integrity.
 */
public class PCEPReportMessageValidator extends PCEPMessageValidator {

	@Override
	public List<Message> validate(final List<PCEPObject> objects) throws PCEPDeserializerException {
		if (objects == null)
			throw new IllegalArgumentException("Passed list can't be null.");

		final List<CompositeStateReportObject> report = new ArrayList<CompositeStateReportObject>();

		while (!objects.isEmpty()) {
			if (objects.get(0) instanceof UnknownObject)
				return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(((UnknownObject) objects.get(0)).getError())));

			if (!(objects.get(0) instanceof PCEPLspObject))
				return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(PCEPErrors.LSP_MISSING)));

			final PCEPLspObject lsp = (PCEPLspObject) objects.get(0);
			objects.remove(0);

			final List<CompositeRptPathObject> paths = new ArrayList<CompositeRptPathObject>();

			if (!objects.isEmpty()) {
				try {
					CompositeRptPathObject path;
					path = this.getValidCompositePath(objects);
					while (path != null) {
						paths.add(path);
						if (objects.isEmpty())
							break;
						path = this.getValidCompositePath(objects);
					}
				} catch (final PCEPDocumentedException e) {
					return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(e.getError())));
				}
			}

			report.add(new CompositeStateReportObject(lsp, paths));
		}

		if (report.isEmpty())
			throw new PCEPDeserializerException("Atleast one CompositeStateReportObject is mandatory.");

		if (!objects.isEmpty()) {
			if (objects.get(0) instanceof UnknownObject)
				return Arrays.asList((Message) new PCEPErrorMessage(new PCEPErrorObject(((UnknownObject) objects.get(0)).getError())));
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
		}

		return Arrays.asList((Message) new PCEPReportMessage(report));
	}

	private CompositeRptPathObject getValidCompositePath(final List<PCEPObject> objects) throws PCEPDocumentedException {
		if (objects.get(0) instanceof UnknownObject)
			throw new PCEPDocumentedException("Unknown object", ((UnknownObject) objects.get(0)).getError());
		if (!(objects.get(0) instanceof PCEPExplicitRouteObject))
			return null;
		final PCEPExplicitRouteObject explicitRoute = (PCEPExplicitRouteObject) objects.get(0);
		objects.remove(0);

		PCEPLspaObject pathLspa = null;
		PCEPExistingPathBandwidthObject pathBandwidth = null;
		PCEPReportedRouteObject pathRro = null;
		final List<PCEPMetricObject> pathMetrics = new ArrayList<PCEPMetricObject>();

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
				if (obj instanceof PCEPLspaObject) {
					pathLspa = (PCEPLspaObject) obj;
					break;
				}
			case 2:
				state = 3;
				if (obj instanceof PCEPExistingPathBandwidthObject) {
					pathBandwidth = (PCEPExistingPathBandwidthObject) obj;
					break;
				}

			case 3:
				state = 4;
				if (obj instanceof PCEPReportedRouteObject) {
					pathRro = (PCEPReportedRouteObject) obj;
					break;
				}
			case 4:
				state = 5;
				if (obj instanceof PCEPMetricObject) {
					pathMetrics.add((PCEPMetricObject) obj);
					state = 4;
					break;
				}
			}

			if (state == 5)
				break;

			objects.remove(0);
		}

		return new CompositeRptPathObject(explicitRoute, pathLspa, pathBandwidth, pathRro, pathMetrics);
	}
}
