/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.stateful02;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.pcep.impl.message.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.P2p1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.P2p1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.gc.object.Gc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.load.balancing.object.LoadBalancing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.SvecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.PathKeyExpansionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.SegmentComputation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.SegmentComputationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.P2p;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.P2pBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.ReportedRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.ReportedRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

import com.google.common.collect.Lists;

/**
 * Parser for {@link Pcreq}
 */
public class PCEPRequestMessageParser extends AbstractMessageParser {

	public static final int TYPE = 3;

	public PCEPRequestMessageParser(final ObjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof Pcreq)) {
			throw new IllegalArgumentException("Wrong instance of PCEPMessage. Passed instance of " + message.getClass()
					+ ". Needed PcrepMessage.");
		}
		final PcreqMessage msg = ((Pcreq) message).getPcreqMessage();
		if (msg.getRequests() == null || msg.getRequests().isEmpty()) {
			throw new IllegalArgumentException("Requests cannot be null or empty.");
		}
		for (final Requests req : msg.getRequests()) {
			buffer.writeBytes(serializeObject(req.getRp()));
			if (req.getPathKeyExpansion() != null) {
				buffer.writeBytes(serializeObject(req.getPathKeyExpansion().getPathKey()));
			}
			if (req.getSegmentComputation() != null) {
				final SegmentComputation sc = req.getSegmentComputation();
				if (sc.getP2p() != null) {
					serializeP2P(buffer, sc.getP2p());
				}
			}
		}
		if (msg.getSvec() != null) {
			for (final Svec s : msg.getSvec()) {
				buffer.writeBytes(serializeObject(s.getSvec()));
				if (s.getOf() != null) {
					buffer.writeBytes(serializeObject(s.getOf()));
				}
				if (s.getGc() != null) {
					buffer.writeBytes(serializeObject(s.getGc()));
				}
				if (s.getXro() != null) {
					buffer.writeBytes(serializeObject(s.getXro()));
				}
				if (s.getMetric() != null) {
					for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.Metric m : s.getMetric()) {
						buffer.writeBytes(serializeObject(m.getMetric()));
					}
				}
			}
		}
	}

	private void serializeP2P(final ByteBuf buffer, final P2p p2p) {
		if (p2p.getEndpointsObj() != null) {
			buffer.writeBytes(serializeObject(p2p.getEndpointsObj()));
		}
		if (p2p.getAugmentation(P2p1.class) != null && p2p.getAugmentation(P2p1.class).getLsp() != null) {
			buffer.writeBytes(serializeObject(p2p.getAugmentation(P2p1.class).getLsp()));
		}
		if (p2p.getReportedRoute() != null) {
			final ReportedRoute rr = p2p.getReportedRoute();
			if (rr.getRro() != null) {
				buffer.writeBytes(serializeObject(rr.getRro()));
			}
			if (rr.getBandwidth() != null) {
				buffer.writeBytes(serializeObject(rr.getBandwidth()));
			}
		}
		if (p2p.getLoadBalancing() != null) {
			buffer.writeBytes(serializeObject(p2p.getLoadBalancing()));
		}
		if (p2p.getLspa() != null) {
			buffer.writeBytes(serializeObject(p2p.getLspa()));
		}
		if (p2p.getBandwidth() != null) {
			buffer.writeBytes(serializeObject(p2p.getBandwidth()));
		}
		if (p2p.getMetrics() != null) {
			for (final Metrics m : p2p.getMetrics()) {
				buffer.writeBytes(serializeObject(m.getMetric()));
			}
		}
		if (p2p.getIro() != null) {
			buffer.writeBytes(serializeObject(p2p.getIro()));
		}
		if (p2p.getRro() != null) {
			buffer.writeBytes(serializeObject(p2p.getRro()));
		}
		if (p2p.getXro() != null) {
			buffer.writeBytes(serializeObject(p2p.getXro()));
		}
		if (p2p.getOf() != null) {
			buffer.writeBytes(serializeObject(p2p.getOf()));
		}
		if (p2p.getClassType() != null) {
			buffer.writeBytes(serializeObject(p2p.getClassType()));
		}
	}

	@Override
	protected Message validate(
			final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object> objects,
			final List<Message> errors) throws PCEPDeserializerException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}

		final List<Requests> requests = Lists.newArrayList();
		final List<Svec> svecList = Lists.newArrayList();
		while (!objects.isEmpty()) {
			final RequestsBuilder rBuilder = new RequestsBuilder();
			Rp rpObj = null;
			if (objects.get(0) instanceof Rp) {
				rpObj = (Rp) objects.get(0);
				objects.remove(0);
				if (!rpObj.isProcessingRule()) {
					errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET));
				} else {
					rBuilder.setRp(rpObj);
				}
			} else {
				// if RP obj is missing return error only
				errors.add(createErrorMsg(PCEPErrors.RP_MISSING));
				return null;
			}

			// expansion
			if (rpObj.isPathKey()) {
				if (objects.get(0) instanceof PathKey) {
					rBuilder.setPathKeyExpansion(new PathKeyExpansionBuilder().setPathKey((PathKey) objects.get(0)).build());
				}
				continue;
			}

			final P2pBuilder p2pBuilder = new P2pBuilder();

			if (objects.get(0) instanceof EndpointsObj) {
				final EndpointsObj ep = (EndpointsObj) objects.get(0);
				objects.remove(0);
				if (!ep.isProcessingRule()) {
					errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, rpObj));
				} else {
					p2pBuilder.setEndpointsObj(ep);
				}
			} else {
				errors.add(createErrorMsg(PCEPErrors.END_POINTS_MISSING, rpObj));
				return null;
			}
			// p2p
			if (!rpObj.isP2mp()) {
				final SegmentComputation segm = getSegmentComputation(p2pBuilder, objects, errors, rpObj);
				if (segm != null) {
					rBuilder.setSegmentComputation(segm);
				}
			}
			while (!objects.isEmpty()) {
				final SvecBuilder sBuilder = new SvecBuilder();
				final Svec svecComp = getValidSvec(sBuilder, objects);
				if (svecComp == null) {
					break;
				}
				svecList.add(svecComp);
			}
			requests.add(rBuilder.build());
		}

		final PcreqMessageBuilder mBuilder = new PcreqMessageBuilder();
		mBuilder.setRequests(requests);
		if (!svecList.isEmpty()) {
			mBuilder.setSvec(svecList);
		}
		return new PcreqBuilder().setPcreqMessage(mBuilder.build()).build();
	}

	private SegmentComputation getSegmentComputation(final P2pBuilder builder, final List<Object> objects, final List<Message> errors,
			final Rp rp) {
		final List<Metrics> metrics = Lists.newArrayList();

		State state = State.Init;
		while (!objects.isEmpty() && state != State.End) {
			Object obj = objects.get(0);

			switch (state) {
			case Init:
				state = State.LspIn;
				if (obj instanceof Lsp) {
					builder.addAugmentation(P2p1.class, new P2p1Builder().setLsp((Lsp) obj).build());
					break;
				}
			case LspIn:
				state = State.ReportedIn;
				if (obj instanceof Rro) {
					final ReportedRouteBuilder rrBuilder = new ReportedRouteBuilder();
					rrBuilder.setRro((Rro) obj);
					objects.remove(0);
					obj = objects.get(0);
					if (obj instanceof Bandwidth) {
						rrBuilder.setBandwidth((Bandwidth) obj);
					}
					break;
				}
			case ReportedIn:
				state = State.LoadBIn;
				if (obj instanceof LoadBalancing) {
					builder.setLoadBalancing((LoadBalancing) obj);
					break;
				}
			case LoadBIn:
				state = State.LspaIn;
				if (obj instanceof Lspa) {
					builder.setLspa((Lspa) obj);
					break;
				}
			case LspaIn:
				state = State.BandwidthIn;
				if (obj instanceof Bandwidth) {
					builder.setBandwidth((Bandwidth) obj);
					break;
				}
			case BandwidthIn:
				state = State.MetricIn;
				if (obj instanceof Metric) {
					metrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
					state = State.BandwidthIn;
					break;
				}
			case MetricIn:
				state = State.IroIn;
				if (obj instanceof Iro) {
					builder.setIro((Iro) obj);
					break;
				}
			case IroIn:
				state = State.RroIn;
				if (obj instanceof Rro) {
					builder.setRro((Rro) obj);
					break;
				}
			case RroIn:
				state = State.XroIn;
				if (obj instanceof Xro) {
					builder.setXro((Xro) obj);
					break;
				}
			case XroIn:
				state = State.OfIn;
				if (obj instanceof Of) {
					builder.setOf((Of) obj);
					break;
				}
			case OfIn:
				state = State.CtIn;
				if (obj instanceof ClassType) {
					final ClassType classType = (ClassType) obj;
					if (!classType.isProcessingRule()) {
						errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, rp));
					} else {
						builder.setClassType(classType);
					}
					break;
				}
			case CtIn:
				state = State.End;
				break;
			case End:
				break;
			}
			if (!state.equals(State.End)) {
				objects.remove(0);
			}
		}
		if (!metrics.isEmpty()) {
			builder.setMetrics(metrics);
		}

		if (rp.isReoptimization()
				&& builder.getBandwidth() != null
				&& builder.getReportedRoute().getBandwidth().getBandwidth() != new BandwidthBuilder().setBandwidth(
						new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth(new byte[] { 0 })).build()
				&& builder.getReportedRoute().getRro() == null) {
			errors.add(createErrorMsg(PCEPErrors.RRO_MISSING, rp));
			return null;
		}
		return new SegmentComputationBuilder().setP2p(builder.build()).build();
	}

	private enum State {
		Init, LspIn, ReportedIn, LoadBIn, LspaIn, BandwidthIn, MetricIn, IroIn, RroIn, XroIn, OfIn, CtIn, End
	}

	private Svec getValidSvec(final SvecBuilder builder, final List<Object> objects) {
		if (objects == null || objects.isEmpty()) {
			throw new IllegalArgumentException("List cannot be null or empty.");
		}

		if (objects.get(0) instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec) {
			builder.setSvec((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec) objects.get(0));
			objects.remove(0);
		} else {
			return null;
		}

		final List<Metrics> metrics = Lists.newArrayList();

		Object obj = null;
		SvecState state = SvecState.Init;
		while (!objects.isEmpty() && !state.equals(SvecState.End)) {
			obj = objects.get(0);

			switch (state) {
			case Init:
				state = SvecState.OfIn;
				if (obj instanceof Of) {
					builder.setOf((Of) obj);
					break;
				}
			case OfIn:
				state = SvecState.GcIn;
				if (obj instanceof Gc) {
					builder.setGc((Gc) obj);
					break;
				}
			case GcIn:
				state = SvecState.XroIn;
				if (obj instanceof Xro) {
					builder.setXro((Xro) obj);
					break;
				}
			case XroIn:
				state = SvecState.MetricIn;
				if (obj instanceof Metric) {
					metrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
					state = SvecState.XroIn;
					break;
				}
			case MetricIn:
				state = SvecState.End;
				break;
			case End:
				break;
			}
			if (!state.equals(SvecState.End)) {
				objects.remove(0);
			}
		}
		return builder.build();
	}

	private enum SvecState {
		Init, OfIn, GcIn, XroIn, MetricIn, End
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
