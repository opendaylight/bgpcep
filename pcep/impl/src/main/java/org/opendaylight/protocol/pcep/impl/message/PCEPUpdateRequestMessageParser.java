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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcupdMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.updates.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.Srp;

import com.google.common.collect.Lists;

/**
 * Parser for {@link PcupdMessage}
 */
public class PCEPUpdateRequestMessageParser extends AbstractMessageParser {

	public static final int TYPE = 11;

	public PCEPUpdateRequestMessageParser(final ObjectHandlerRegistry registry) {
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
	public Message parseMessage(final byte[] buffer) throws PCEPDeserializerException {
		if (buffer == null || buffer.length == 0) {
			throw new PCEPDeserializerException("Pcup message cannot be empty.");
		}
		try {
			final List<Object> objs = parseObjects(buffer);
			return validate(objs);
		} catch (final PCEPDocumentedException e) {
			return createErrorMsg(e.getError());
		}
	}

	public Message validate(final List<Object> objects) throws PCEPDeserializerException, PCEPDocumentedException {
		if (objects == null) {
			throw new IllegalArgumentException("Passed list can't be null.");
		}
		final List<Updates> updateRequests = Lists.newArrayList();

		while (!objects.isEmpty()) {
			final Updates update = getValidUpdates(objects);
			if (update != null) {
				updateRequests.add(update);
			}
		}
		if (!objects.isEmpty()) {
			if (objects.get(0) instanceof UnknownObject) {
				throw new PCEPDocumentedException("Unknown object encountered", ((UnknownObject) objects.get(0)).getError());
			}
			throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
		}
		return new PcupdBuilder().setPcupdMessage(new PcupdMessageBuilder().setUpdates(updateRequests).build()).build();
	}

	private Updates getValidUpdates(final List<Object> objects) throws PCEPDocumentedException {
		final UpdatesBuilder builder = new UpdatesBuilder();
		if (objects.get(0) instanceof Srp) {
			builder.setSrp((Srp) objects.get(0));
			objects.remove(0);
		} else {
			throw new PCEPDocumentedException("Srp object missing.", PCEPErrors.SRP_MISSING);
		}
		if (objects.get(0) instanceof Lsp) {
			builder.setLsp((Lsp) objects.get(0));
			objects.remove(0);
		} else {
			throw new PCEPDocumentedException("Lsp object missing.", PCEPErrors.LSP_MISSING);
		}
		if (!objects.isEmpty()) {
			final PathBuilder pBuilder = new PathBuilder();
			if (objects.get(0) instanceof Ero) {
				pBuilder.setEro((Ero) objects.get(0));
				objects.remove(0);
			} else {
				throw new PCEPDocumentedException("Ero object missing.", PCEPErrors.ERO_MISSING);
			}
			parsePath(objects, pBuilder);
			builder.setPath(pBuilder.build());
		}
		return builder.build();
	}

	private void parsePath(final List<Object> objects, final PathBuilder pBuilder) throws PCEPDocumentedException {
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
			default:
				if (obj instanceof UnknownObject) {
					throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());
				}
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
