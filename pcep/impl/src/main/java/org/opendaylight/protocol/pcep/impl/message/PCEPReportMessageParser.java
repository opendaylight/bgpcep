/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.UnknownObject;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.Srp;

import com.google.common.collect.Lists;

/**
 * Parser for {@link Pcrpt}
 */
public class PCEPReportMessageParser extends AbstractMessageParser {

	public static final int TYPE = 10;

	public PCEPReportMessageParser(final ObjectHandlerRegistry registry) {
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
			if (p.getEro() != null) {
				buffer.writeBytes(serializeObject(p.getEro()));
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

	@Override
	public Message parseMessage(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new PCEPDeserializerException("Pcrpt message cannot be empty.");
		}
		try {
			final List<Object> objs = parseObjects(buffer);
			return validate(objs);
		} catch (final PCEPDocumentedException e) {
			return createErrorMsg(e.getError());
		}
	}

	public Pcrpt validate(final List<Object> objects) throws PCEPDeserializerException, PCEPDocumentedException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}

		final List<Reports> reports = Lists.newArrayList();

		while (!objects.isEmpty()) {
			final Reports report = getValidReports(objects);
			reports.add(report);
		}

		if (!objects.isEmpty()) {
			if (objects.get(0) instanceof UnknownObject) {
				throw new PCEPDocumentedException("Unknown object encountered", ((UnknownObject) objects.get(0)).getError());
			}
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
		}
		return new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(reports).build()).build();
	}

	private Reports getValidReports(final List<Object> objects) throws PCEPDocumentedException {
		Srp srp = null;
		Lsp lsp = null;
		Ero ero = null;
		Lspa pathLspa = null;
		Bandwidth pathBandwidth = null;
		final List<Metrics> pathMetrics = Lists.newArrayList();
		Rro pathRro = null;
		Iro iro = null;

		Object obj;
		State state = State.Init;
		while (!objects.isEmpty()) {
			obj = objects.get(0);
			if (obj instanceof UnknownObject) {
				throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());
			}
			switch (state) {
			case Init:
				state = State.SrpIn;
				if (obj instanceof Srp) {
					srp = (Srp) obj;
					break;
				}
			case SrpIn:
				state = State.LspIn;
				if (obj instanceof Lsp) {
					lsp = (Lsp) obj;
					break;
				}
			case EroIn:
				state = State.LspaIn;
				if (obj instanceof Ero) {
					ero = (Ero) obj;
					break;
				}
			case LspaIn:
				state = State.BandwidthIn;
				if (obj instanceof Lspa) {
					pathLspa = (Lspa) obj;
					break;
				}
			case BandwidthIn:
				state = State.MetricIn;
				if (obj instanceof Bandwidth) {
					pathBandwidth = (Bandwidth) obj;
					break;
				}
			case MetricIn:
				state = State.IroIn;
				if (obj instanceof Metric) {
					pathMetrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
					state = State.MetricIn;
					break;
				}
			case IroIn:
				state = State.RroIn;
				if (obj instanceof Iro) {
					iro = (Iro) obj;
					break;
				}
			case RroIn:
				state = State.End;
				if (obj instanceof Rro) {
					pathRro = (Rro) obj;
					break;
				}
			case End:
				break;
			default:
				if (obj instanceof UnknownObject) {
					throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());
				}
			}
			objects.remove(0);
		}
		if (lsp == null) {
			throw new PCEPDocumentedException("LSP object missing", PCEPErrors.LSP_MISSING);
		}

		final PathBuilder builder = new PathBuilder();
		builder.setEro(ero);
		builder.setLspa(pathLspa);
		builder.setBandwidth(pathBandwidth);
		builder.setMetrics(pathMetrics);
		builder.setIro(iro);
		builder.setRro(pathRro);
		return new ReportsBuilder().setSrp(srp).setLsp(lsp).setPath(builder.build()).build();
	}

	private enum State {
		Init, SrpIn, LspIn, EroIn, LspaIn, BandwidthIn, MetricIn, IroIn, RroIn, End
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
