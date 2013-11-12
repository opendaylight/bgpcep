/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.impl.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrepMessage;

/**
 * Parser for {@link PcrepMessage}
 */
// FIXME: finish
public class PCEPReplyMessageParser extends AbstractMessageParser {

	public static final int TYPE = 4;

	public PCEPReplyMessageParser(final ObjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof PcrepMessage)) {
			throw new IllegalArgumentException("Wrong instance of Message. Passed instance of " + message.getClass()
					+ ". Nedded PcrepMessage.");
		}

	}

	@Override
	public PcrepMessage parseMessage(final byte[] buffer) throws PCEPDeserializerException {
		return null;
	}

	// private static class SubReplyValidator {
	//
	// private boolean requestRejected = true;
	//
	// private List<Message> msgs = Lists.newArrayList();
	//
	// private PCEPRequestParameterObject rpObj;
	//
	// private PCEPNoPathObject noPath;
	// private PCEPLspObject lsp;
	// private PCEPLspaObject lspa;
	// private PCEPRequestedPathBandwidthObject bandwidth;
	// private List<PCEPMetricObject> metrics;
	// private PCEPIncludeRouteObject iro;
	// private List<CompositePathObject> paths;
	//
	// private void init() {
	// this.requestRejected = false;
	// this.msgs = Lists.newArrayList();
	//
	// this.noPath = null;
	// this.lsp = null;
	// this.lspa = null;
	// this.iro = null;
	// this.rpObj = null;
	// this.metrics = new ArrayList<PCEPMetricObject>();
	// this.paths = new ArrayList<CompositePathObject>();
	// }
	//
	// public List<Message> validate(final List<Object> objects, final List<CompositeReplySvecObject> svecList) {
	// this.init();
	//
	// if (!(objects.get(0) instanceof PCEPRequestParameterObject))
	// return null;
	//
	// final PCEPRequestParameterObject rpObj = (PCEPRequestParameterObject) objects.get(0);
	// objects.remove(0);
	//
	// Object obj;
	// int state = 1;
	// while (!objects.isEmpty()) {
	// obj = objects.get(0);
	// if (obj instanceof UnknownObject) {
	// if (((UnknownObject) obj).isProcessingRule()) {
	// this.msgs.add(new PCEPErrorMessage(new CompositeErrorObject(copyRP(rpObj, false), new
	// PCEPErrorObject(((UnknownObject) obj).getError()))));
	// this.requestRejected = true;
	// }
	//
	// objects.remove(0);
	// continue;
	// }
	//
	// switch (state) {
	// case 1:
	// state = 2;
	// if (obj instanceof PCEPNoPathObject) {
	// this.noPath = (PCEPNoPathObject) obj;
	// break;
	// }
	// case 2:
	// state = 3;
	// if (obj instanceof PCEPLspObject) {
	// this.lsp = (PCEPLspObject) obj;
	// break;
	// }
	// case 3:
	// state = 4;
	// if (obj instanceof PCEPLspaObject) {
	// this.lspa = (PCEPLspaObject) obj;
	// break;
	// }
	// case 4:
	// state = 5;
	// if (obj instanceof PCEPRequestedPathBandwidthObject) {
	// this.bandwidth = (PCEPRequestedPathBandwidthObject) obj;
	// break;
	// }
	// case 5:
	// state = 6;
	// if (obj instanceof PCEPMetricObject) {
	// this.metrics.add((PCEPMetricObject) obj);
	// state = 5;
	// break;
	// }
	// case 6:
	// state = 7;
	// if (obj instanceof PCEPIncludeRouteObject) {
	// this.iro = (PCEPIncludeRouteObject) obj;
	// state = 8;
	// break;
	// }
	// }
	//
	// if (state == 7)
	// break;
	//
	// objects.remove(0);
	//
	// if (state == 8)
	// break;
	// }
	//
	// if (!objects.isEmpty()) {
	// CompositePathObject path = this.getValidCompositePath(objects);
	// while (path != null) {
	// this.paths.add(path);
	// if (objects.isEmpty())
	// break;
	// path = this.getValidCompositePath(objects);
	// }
	// }
	//
	// if (!this.requestRejected) {
	// this.msgs.add(new PCEPReplyMessage(Collections.unmodifiableList(Arrays.asList(new CompositeResponseObject(rpObj,
	// this.noPath, this.lsp, this.lspa, this.bandwidth, this.metrics, this.iro, this.paths))),
	// Collections.unmodifiableList(svecList)));
	// }
	//
	// return this.msgs;
	// }
	//

	// private Replies getValidCompositePath(final List<Object> objects) {
	// if (!(objects.get(0) instanceof Rp)) {
	// throw new PCEPDocumentedException("Pcrep message must contain at least one RP object.", PCEPErrors.RP_MISSING);
	// }
	// final Rp rp = (Rp) objects.get(0);
	// objects.remove(0);
	//
	// NoPath nopath = null;
	// final Ero ero = null;
	// final Lspa lspa = null;
	// final Bandwidth bandwidth = null;
	// final List<Metrics> pathMetrics = Lists.newArrayList();
	// Iro pathIro = null;
	//
	// Object obj;
	// State state = State.Init;
	// while (!objects.isEmpty()) {
	// obj = objects.get(0);
	//
	// switch (state) {
	// case Init:
	// state = State.EroIn;
	// if (obj instanceof NoPath) {
	// nopath = (NoPath) obj;
	// state = State.End;
	// break;
	// }
	// case EroIn:
	// state = State.LspaIn;
	// if (obj instanceof Lspa) {
	// lspa = (Lspa) obj;
	// break;
	// }
	// case LspaIn:
	// state = State.BandwidthIn;
	// if (obj instanceof Bandwidth) {
	// bandwidth = (Bandwidth) obj;
	// break;
	// }
	// case MetricIn:
	// state = State.IroIn;
	// if (obj instanceof Metric) {
	// pathMetrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
	// state = State.MetricIn;
	// break;
	// }
	// case IroIn:
	// state = State.End;
	// if (obj instanceof Iro) {
	// pathIro = (Iro) obj;
	// break;
	// }
	// case End:
	// break;
	// default:
	// if (obj instanceof UnknownObject) {
	// if (((UnknownObject) obj).isProcessingRule()) {
	// this.msgs.add(new PCEPErrorMessage(new CompositeErrorObject(copyRP(this.rpObj, false), new
	// PCEPErrorObject(((UnknownObject) obj).getError()))));
	// this.requestRejected = true;
	// }
	// objects.remove(0);
	// continue;
	// }
	// }
	// objects.remove(0);
	// }
	//
	// final PathBuilder builder = new PathBuilder();
	// builder.setEro(ero);
	// builder.setLspa(lspa);
	// builder.setBandwidth(bandwidth);
	// builder.setMetrics(pathMetrics);
	// builder.setIro(pathIro);
	// return new RepliesBuilder().setRp(rp).setResult(value).build();
	// }
	//
	// private Result parseResult(List<Object> objs) {
	//
	// }

	private enum State {
		Init, NoPathIn, EroIn, LspaIn, BandwidthIn, MetricIn, IroIn, End
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
