/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;

import com.google.common.collect.Lists;

/**
 * Parser for {@link Pcrpt}
 */
public class Stateful07PCReportMessageParser extends AbstractMessageParser {

	public static final int TYPE = 10;

	public Stateful07PCReportMessageParser(final ObjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof Pcrpt)) {
			throw new IllegalArgumentException("Wrong instance of Message. Passed instance of " + message.getClass()
					+ ". Nedded PcrptMessage.");
		}
		final Pcrpt msg = (Pcrpt) message;
		final List<Reports> reports = msg.getPcrptMessage().getReports();
		for (final Reports report : reports) {
			if (report.getSrp() != null) {
				buffer.writeBytes(serializeObject(report.getSrp()));
			}
			buffer.writeBytes(serializeObject(report.getLsp()));
			final Path p = report.getPath();
			if (p != null) {
				buffer.writeBytes(serializeObject(p.getEro()));
				if (p.getLspa() != null) {
					buffer.writeBytes(serializeObject(p.getLspa()));
				}
				if (p.getBandwidth() != null) {
					buffer.writeBytes(serializeObject(p.getBandwidth()));
				}
				if (p.getMetrics() != null && !p.getMetrics().isEmpty()) {
					for (final Metrics m : p.getMetrics()) {
						buffer.writeBytes(serializeObject(m.getMetric()));
					}
				}
				if (p.getIro() != null) {
					buffer.writeBytes(serializeObject(p.getIro()));
				}
				if (p.getRro() != null) {
					buffer.writeBytes(serializeObject(p.getRro()));
				}
			}
		}
	}

	@Override
	public Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}
		if (objects.isEmpty()) {
			throw new PCEPDeserializerException("Pcrpt message cannot be empty.");
		}

		final List<Reports> reports = Lists.newArrayList();

		while (!objects.isEmpty()) {
			final Reports report = getValidReports(objects, errors);
			if (reports != null) {
				reports.add(report);
			}
		}
		return new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(reports).build()).build();
	}

	private Reports getValidReports(final List<Object> objects, final List<Message> errors) {
		final ReportsBuilder builder = new ReportsBuilder();
		if (objects.get(0) instanceof Srp) {
			builder.setSrp((Srp) objects.get(0));
			objects.remove(0);
		}
		if (objects.get(0) instanceof Lsp) {
			builder.setLsp((Lsp) objects.get(0));
			objects.remove(0);
		} else {
			errors.add(createErrorMsg(PCEPErrors.LSP_MISSING));
			return null;
		}
		if (!objects.isEmpty()) {
			final PathBuilder pBuilder = new PathBuilder();
			parsePath(objects, pBuilder);
			builder.setPath(pBuilder.build());
		}
		return builder.build();
	}

	private void parsePath(final List<Object> objects, final PathBuilder builder) {
		final List<Metrics> pathMetrics = Lists.newArrayList();
		Object obj;
		State state = State.Init;
		while (!objects.isEmpty() && !state.equals(State.End)) {
			obj = objects.get(0);
			switch (state) {
			case Init:
				state = State.EroIn;
				if (obj instanceof Ero) {
					builder.setEro((Ero) obj);
					break;
				}
			case EroIn:
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
					pathMetrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
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
				state = State.End;
				break;
			case End:
				break;
			}
			if (!state.equals(State.End)) {
				objects.remove(0);
			}
		}
		if (!pathMetrics.isEmpty()) {
			builder.setMetrics(pathMetrics);
		}
	}

	private enum State {
		Init, EroIn, LspaIn, BandwidthIn, MetricIn, IroIn, RroIn, End
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
