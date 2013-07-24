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
import java.util.Collections;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.impl.PCEPMessageValidator;
import org.opendaylight.protocol.pcep.impl.object.UnknownObject;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.message.PCEPReplyMessage;
import org.opendaylight.protocol.pcep.object.CompositeErrorObject;
import org.opendaylight.protocol.pcep.object.CompositePathObject;
import org.opendaylight.protocol.pcep.object.CompositeReplySvecObject;
import org.opendaylight.protocol.pcep.object.CompositeResponseObject;
import org.opendaylight.protocol.pcep.object.PCEPErrorObject;
import org.opendaylight.protocol.pcep.object.PCEPExplicitRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPIncludeRouteObject;
import org.opendaylight.protocol.pcep.object.PCEPLspObject;
import org.opendaylight.protocol.pcep.object.PCEPLspaObject;
import org.opendaylight.protocol.pcep.object.PCEPMetricObject;
import org.opendaylight.protocol.pcep.object.PCEPNoPathObject;
import org.opendaylight.protocol.pcep.object.PCEPObjectiveFunctionObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestParameterObject;
import org.opendaylight.protocol.pcep.object.PCEPRequestedPathBandwidthObject;
import org.opendaylight.protocol.pcep.object.PCEPSvecObject;

/**
 * PCEPReplyMessage validator. Validates message integrity.
 */
public class PCEPReplyMessageValidator extends PCEPMessageValidator {

	private static class SubReplyValidator {

		private boolean requestRejected = true;

		private List<PCEPMessage> msgs = new ArrayList<PCEPMessage>();

		private PCEPRequestParameterObject rpObj;

		private PCEPNoPathObject noPath;
		private PCEPLspObject lsp;
		private PCEPLspaObject lspa;
		private PCEPRequestedPathBandwidthObject bandwidth;
		private List<PCEPMetricObject> metrics;
		private PCEPIncludeRouteObject iro;
		private List<CompositePathObject> paths;

		private void init() {
			this.requestRejected = false;
			this.msgs = new ArrayList<PCEPMessage>();

			this.noPath = null;
			this.lsp = null;
			this.lspa = null;
			this.iro = null;
			this.rpObj = null;
			this.metrics = new ArrayList<PCEPMetricObject>();
			this.paths = new ArrayList<CompositePathObject>();
		}

		public List<PCEPMessage> validate(List<PCEPObject> objects, List<CompositeReplySvecObject> svecList) {
			this.init();

			if (!(objects.get(0) instanceof PCEPRequestParameterObject))
				return null;

			final PCEPRequestParameterObject rpObj = (PCEPRequestParameterObject) objects.get(0);
			objects.remove(0);

			PCEPObject obj;
			int state = 1;
			while (!objects.isEmpty()) {
				obj = objects.get(0);
				if (obj instanceof UnknownObject) {
					if (((UnknownObject) obj).isProcessed()) {
						this.msgs.add(new PCEPErrorMessage(
								new CompositeErrorObject(copyRP(rpObj, false), new PCEPErrorObject(((UnknownObject) obj).getError()))));
						this.requestRejected = true;
					}

					objects.remove(0);
					continue;
				}

				switch (state) {
					case 1:
						state = 2;
						if (obj instanceof PCEPNoPathObject) {
							this.noPath = (PCEPNoPathObject) obj;
							break;
						}
					case 2:
						state = 3;
						if (obj instanceof PCEPLspObject) {
							this.lsp = (PCEPLspObject) obj;
							break;
						}
					case 3:
						state = 4;
						if (obj instanceof PCEPLspaObject) {
							this.lspa = (PCEPLspaObject) obj;
							break;
						}
					case 4:
						state = 5;
						if (obj instanceof PCEPRequestedPathBandwidthObject) {
							this.bandwidth = (PCEPRequestedPathBandwidthObject) obj;
							break;
						}
					case 5:
						state = 6;
						if (obj instanceof PCEPMetricObject) {
							this.metrics.add((PCEPMetricObject) obj);
							state = 5;
							break;
						}
					case 6:
						state = 7;
						if (obj instanceof PCEPIncludeRouteObject) {
							this.iro = (PCEPIncludeRouteObject) obj;
							state = 8;
							break;
						}
				}

				if (state == 7)
					break;

				objects.remove(0);

				if (state == 8)
					break;
			}

			if (!objects.isEmpty()) {
				CompositePathObject path = this.getValidCompositePath(objects);
				while (path != null) {
					this.paths.add(path);
					if (objects.isEmpty())
						break;
					path = this.getValidCompositePath(objects);
				}
			}

			if (!this.requestRejected) {
				this.msgs.add(new PCEPReplyMessage(Collections.unmodifiableList(Arrays.asList(new CompositeResponseObject(rpObj, this.noPath, this.lsp,
						this.lspa, this.bandwidth, this.metrics, this.iro, this.paths))), Collections.unmodifiableList(svecList)));
			}

			return this.msgs;
		}

		private CompositePathObject getValidCompositePath(List<PCEPObject> objects) {
			if (!(objects.get(0) instanceof PCEPExplicitRouteObject))
				return null;

			final PCEPExplicitRouteObject explicitRoute = (PCEPExplicitRouteObject) objects.get(0);
			objects.remove(0);

			PCEPLspaObject pathLspa = null;
			PCEPRequestedPathBandwidthObject pathBandwidth = null;
			final List<PCEPMetricObject> pathMetrics = new ArrayList<PCEPMetricObject>();
			PCEPIncludeRouteObject pathIro = null;

			PCEPObject obj;
			int state = 1;
			while (!objects.isEmpty()) {
				obj = objects.get(0);
				if (obj instanceof UnknownObject) {
					if (((UnknownObject) obj).isProcessed()) {
						this.msgs.add(new PCEPErrorMessage(new CompositeErrorObject(copyRP(this.rpObj, false), new PCEPErrorObject(((UnknownObject) obj)
								.getError()))));
						this.requestRejected = true;
					}
					objects.remove(0);
					continue;
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
					case 4:
						state = 5;
						if (obj instanceof PCEPIncludeRouteObject) {
							pathIro = (PCEPIncludeRouteObject) obj;
							state = 6;
							break;
						}

				}

				if (state == 5)
					break;

				objects.remove(0);

				if (state == 6)
					break;
			}

			return new CompositePathObject(explicitRoute, pathLspa, pathBandwidth, pathMetrics, pathIro);
		}
	}

	@Override
	public List<PCEPMessage> validate(List<PCEPObject> objects) throws PCEPDeserializerException {
		if (objects == null)
			throw new IllegalArgumentException("Passed list can't be null.");

		final List<PCEPMessage> msgs = new ArrayList<PCEPMessage>();
		final List<CompositeReplySvecObject> svecList = new ArrayList<CompositeReplySvecObject>();

		CompositeReplySvecObject svecComp;
		while (!objects.isEmpty()) {
			try {
				if ((svecComp = this.getValidSvecComposite(objects)) == null)
					break;
			} catch (final PCEPDocumentedException e) {
				msgs.add(new PCEPErrorMessage(new PCEPErrorObject(e.getError())));
				return msgs;
			}

			svecList.add(svecComp);
		}

		List<PCEPMessage> subMessages;
		final SubReplyValidator subValidator = new SubReplyValidator();
		while (!objects.isEmpty()) {
			subMessages = subValidator.validate(objects, svecList);
			if (subMessages == null)
				break;
			msgs.addAll(subMessages);
		}

		if (msgs.isEmpty()) {
			msgs.add(new PCEPErrorMessage(new PCEPErrorObject(PCEPErrors.RP_MISSING)));
			return msgs;
		}

		if (!objects.isEmpty())
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);

		return msgs;
	}

	private CompositeReplySvecObject getValidSvecComposite(List<PCEPObject> objects) throws PCEPDocumentedException {
		if (objects == null)
			throw new IllegalArgumentException("List cannot be null.");

		if (!(objects.get(0) instanceof PCEPSvecObject))
			return null;

		final PCEPSvecObject svec = (PCEPSvecObject) objects.get(0);
		objects.remove(0);

		PCEPObjectiveFunctionObject of = null;
		final List<PCEPMetricObject> metrics = new ArrayList<PCEPMetricObject>();

		PCEPObject obj;
		int state = 1;
		while (!objects.isEmpty()) {
			obj = objects.get(0);

			if (obj instanceof UnknownObject)
				throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());

			switch (state) {
				case 1:
					state = 2;
					if (obj instanceof PCEPObjectiveFunctionObject) {
						of = (PCEPObjectiveFunctionObject) obj;
						break;
					}
				case 2:
					state = 3;
					if (obj instanceof PCEPMetricObject) {
						metrics.add((PCEPMetricObject) obj);
						state = 2;
						break;
					}
			}

			if (state == 3)
				break;

			objects.remove(0);
		}

		return new CompositeReplySvecObject(svec, of, metrics);
	}

	private static PCEPRequestParameterObject copyRP(PCEPRequestParameterObject origRp, boolean processed) {
		return new PCEPRequestParameterObject(origRp.isLoose(), origRp.isBidirectional(), origRp.isReoptimized(), origRp.isMakeBeforeBreak(),
				origRp.isReportRequestOrder(), origRp.isSuplyOFOnResponse(), origRp.isFragmentation(), origRp.isP2mp(), origRp.isEroCompression(),
				origRp.getPriority(), origRp.getRequestID(), origRp.getTlvs(), processed, origRp.isIgnored());
	}
}
