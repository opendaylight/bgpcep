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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.updates.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.updates.PathBuilder;
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

import com.google.common.collect.Lists;

/**
 * Parser for {@link Pcupd}
 */
public class Stateful07PCUpdateRequestMessageParser extends AbstractMessageParser {

	public static final int TYPE = 11;

	public Stateful07PCUpdateRequestMessageParser(final ObjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof Pcupd)) {
			throw new IllegalArgumentException("Wrong instance of PCEPMessage. Passed instance of " + message.getClass()
					+ ". Nedded PcupdMessage.");
		}
		final Pcupd msg = (Pcupd) message;
		final List<Updates> updates = msg.getPcupdMessage().getUpdates();
		for (final Updates update : updates) {
			buffer.writeBytes(serializeObject(update.getSrp()));
			buffer.writeBytes(serializeObject(update.getLsp()));
			final Path p = update.getPath();
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
			}
		}
	}

	@Override
	protected Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}
		if (objects.isEmpty()) {
			throw new PCEPDeserializerException("Pcup message cannot be empty.");
		}

		final List<Updates> updateRequests = Lists.newArrayList();

		while (!objects.isEmpty()) {
			final Updates update = getValidUpdates(objects, errors);
			if (update != null) {
				updateRequests.add(update);
			}
		}
		if (!objects.isEmpty()) {
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
		}
		return new PcupdBuilder().setPcupdMessage(new PcupdMessageBuilder().setUpdates(updateRequests).build()).build();
	}

	private Updates getValidUpdates(final List<Object> objects, final List<Message> errors) {
		final UpdatesBuilder builder = new UpdatesBuilder();
		if (objects.get(0) instanceof Srp) {
			builder.setSrp((Srp) objects.get(0));
			objects.remove(0);
		} else {
			errors.add(createErrorMsg(PCEPErrors.SRP_MISSING));
			return null;
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
			if (objects.get(0) instanceof Ero) {
				pBuilder.setEro((Ero) objects.get(0));
				objects.remove(0);
			} else {
				errors.add(createErrorMsg(PCEPErrors.ERO_MISSING));
				return null;
			}
			parsePath(objects, pBuilder);
			builder.setPath(pBuilder.build());
		}
		return builder.build();
	}

	private void parsePath(final List<Object> objects, final PathBuilder pBuilder) {
		final List<Metrics> pathMetrics = Lists.newArrayList();
		Object obj;
		State state = State.Init;
		while (!objects.isEmpty() && !state.equals(State.End)) {
			obj = objects.get(0);
			switch (state) {
			case Init:
				state = State.LspaIn;
				if (obj instanceof Lspa) {
					pBuilder.setLspa((Lspa) obj);
					break;
				}
			case LspaIn:
				state = State.BandwidthIn;
				if (obj instanceof Bandwidth) {
					pBuilder.setBandwidth((Bandwidth) obj);
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
					pBuilder.setIro((Iro) obj);
					break;
				}
			case IroIn:
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
			pBuilder.setMetrics(pathMetrics);
		}
	}

	private enum State {
		Init, LspaIn, BandwidthIn, MetricIn, IroIn, End
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
