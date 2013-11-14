/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrorMapping;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.UnknownObject;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrepMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.Replies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.Result;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.Failure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.FailureBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.Success;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

import com.google.common.collect.Lists;

/**
 * Parser for {@link PcrepMessage}
 */
public class PCEPReplyMessageParser extends AbstractMessageParser {

	public static final int TYPE = 4;

	public PCEPReplyMessageParser(final ObjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public void serializeMessage(final Message message, final ByteBuf buffer) {
		if (!(message instanceof Pcrep)) {
			throw new IllegalArgumentException("Wrong instance of Message. Passed instance of " + message.getClass()
					+ ". Nedded PcrepMessage.");
		}
		final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.PcrepMessage repMsg = ((Pcrep) message).getPcrepMessage();
		if (repMsg.getReplies() == null || repMsg.getReplies().isEmpty()) {
			throw new IllegalArgumentException("Replies cannot be null or empty.");
		}
		for (final Replies reply : repMsg.getReplies()) {
			if (reply.getRp() == null) {
				throw new IllegalArgumentException("Reply must contain RP object.");
			}
			buffer.writeBytes(serializeObject(reply.getRp()));
			if (reply.getResult() != null) {
				if (reply.getResult() instanceof Failure) {
					final Failure f = (Failure) reply.getResult();
					buffer.writeBytes(serializeObject(f.getNoPath()));
					if (f.getLspa() != null) {
						buffer.writeBytes(serializeObject(f.getLspa()));
					}
					if (f.getBandwidth() != null) {
						buffer.writeBytes(serializeObject(f.getBandwidth()));
					}
					if (f.getMetrics() != null && !f.getMetrics().isEmpty()) {
						for (final Metrics m : f.getMetrics()) {
							buffer.writeBytes(serializeObject(m.getMetric()));
						}
					}
					if (f.getIro() != null) {
						buffer.writeBytes(serializeObject(f.getIro()));
					}
				} else {
					final Success s = (Success) reply.getResult();
					for (final Paths p : s.getPaths()) {
						buffer.writeBytes(serializeObject(p.getEro()));
						if (p.getLspa() != null) {
							buffer.writeBytes(serializeObject(p.getLspa()));
						}
						if (p.getOf() != null) {
							buffer.writeBytes(serializeObject(p.getOf()));
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
		}
	}

	@Override
	public Message parseMessage(final byte[] buffer) throws PCEPDeserializerException, PCEPDocumentedException {
		if (buffer == null || buffer.length == 0) {
			throw new PCEPDeserializerException("Error message is empty.");
		}
		final List<Object> objs = parseObjects(buffer);
		PcrepMessage m = null;
		try {
			m = validate(objs);
		} catch (final PCEPDocumentedException e) {
			final PCEPErrorMapping maping = PCEPErrorMapping.getInstance();
			return new PcerrBuilder().setPcerrMessage(
					new PcerrMessageBuilder().setErrors(
							Arrays.asList(new ErrorsBuilder().setErrorObject(
									new ErrorObjectBuilder().setType(maping.getFromErrorsEnum(e.getError()).type).setValue(
											maping.getFromErrorsEnum(e.getError()).value).build()).build())).build()).build();
		}
		return m;
	}

	private Pcrep validate(final List<Object> objects) throws PCEPDocumentedException {
		final List<Replies> replies = Lists.newArrayList();
		while (!objects.isEmpty()) {
			replies.add(this.getValidReply(objects));
		}
		return new PcrepBuilder().setPcrepMessage(new PcrepMessageBuilder().setReplies(replies).build()).build();
	}

	private Replies getValidReply(final List<Object> objects) throws PCEPDocumentedException {
		if (!(objects.get(0) instanceof Rp)) {
			throw new PCEPDocumentedException("Pcrep message must contain at least one RP object.", PCEPErrors.RP_MISSING);
		}
		final Rp rp = (Rp) objects.get(0);
		objects.remove(0);
		Result res = null;
		if (!objects.isEmpty()) {
			if (objects.get(0) instanceof NoPath) {
				final NoPath noPath = (NoPath) objects.get(0);
				objects.remove(0);
				final FailureBuilder builder = new FailureBuilder();
				builder.setNoPath(noPath);
				while (!objects.isEmpty()) {
					this.parseAttributes(builder, objects);
				}
				res = builder.build();
			} else if (objects.get(0) instanceof Ero) {
				final Ero ero = (Ero) objects.get(0);
				objects.remove(0);
				final SuccessBuilder builder = new SuccessBuilder();
				final List<Paths> paths = Lists.newArrayList();
				final PathsBuilder pBuilder = new PathsBuilder();
				pBuilder.setEro(ero);
				while (!objects.isEmpty()) {
					this.parsePath(pBuilder, objects);
					paths.add(pBuilder.build());
				}
				builder.setPaths(paths);
				res = builder.build();
			}
		}
		return new RepliesBuilder().setRp(rp).setResult(res).build();
	}

	private void parseAttributes(final FailureBuilder builder, final List<Object> objects) throws PCEPDocumentedException {
		final List<Metrics> pathMetrics = Lists.newArrayList();

		Object obj;
		State state = State.Init;
		while (!objects.isEmpty()) {
			obj = objects.get(0);

			switch (state) {
			case Init:
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
					state = State.MetricIn;
					break;
				}
			case MetricIn:
				state = State.End;
				if (obj instanceof Iro) {
					builder.setIro((Iro) obj);
					break;
				}
			case End:
				break;
			default:
				throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());
			}
			objects.remove(0);
		}
		builder.setMetrics(pathMetrics);
	}

	private void parsePath(final PathsBuilder builder, final List<Object> objects) throws PCEPDocumentedException {
		final List<Metrics> pathMetrics = Lists.newArrayList();

		Object obj;
		State state = State.Init;
		while (!objects.isEmpty()) {
			obj = objects.get(0);

			switch (state) {
			case Init:
				state = State.LspaIn;
				if (obj instanceof Lspa) {
					builder.setLspa((Lspa) obj);
					break;
				}
			case LspaIn:
				state = State.OfIn;
				if (obj instanceof Of) {
					builder.setOf((Of) obj);
					break;
				}
			case OfIn:
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
				state = State.End;
				if (obj instanceof Iro) {
					builder.setIro((Iro) obj);
					break;
				}
			case End:
				break;
			default:
				throw new PCEPDocumentedException("Unknown object", ((UnknownObject) obj).getError());
			}
			objects.remove(0);
		}
		builder.setMetrics(pathMetrics);
	}

	private enum State {
		Init, LspaIn, OfIn, BandwidthIn, MetricIn, End
	}

	@Override
	public int getMessageType() {
		return TYPE;
	}
}
