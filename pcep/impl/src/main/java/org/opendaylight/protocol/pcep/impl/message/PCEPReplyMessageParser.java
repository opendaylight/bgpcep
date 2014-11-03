/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.Replies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.Result;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.FailureCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.FailureCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.objects.VendorInformationObject;

/**
 * Parser for {@link Pcrep}
 */
public class PCEPReplyMessageParser extends AbstractMessageParser {

    public static final int TYPE = 4;

    public PCEPReplyMessageParser(final ObjectRegistry registry, final VendorInformationObjectRegistry viRegistry) {
        super(registry, viRegistry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof Pcrep, "Wrong instance of Message. Passed instance of %s. Need Pcrep.", message.getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.PcrepMessage repMsg = ((Pcrep) message).getPcrepMessage();
        if (repMsg.getReplies() == null || repMsg.getReplies().isEmpty()) {
            throw new IllegalArgumentException("Replies cannot be null or empty.");
        }
        final ByteBuf buffer = Unpooled.buffer();
        for (final Replies reply : repMsg.getReplies()) {
            if (reply.getRp() == null) {
                throw new IllegalArgumentException("Reply must contain RP object.");
            }
            serializeReply(reply, buffer);
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    protected void serializeReply(final Replies reply, final ByteBuf buffer) {
        serializeObject(reply.getRp(), buffer);
        serializeVendorInformationObjects(reply.getVendorInformationObject(), buffer);
        if (reply.getResult() != null) {
            if (reply.getResult() instanceof FailureCase) {
                final FailureCase f = ((FailureCase) reply.getResult());
                if (f != null) {
                    serializeObject(f.getNoPath(), buffer);
                    serializeObject(f.getLspa(), buffer);
                    serializeObject(f.getBandwidth(), buffer);
                    if (f.getMetrics() != null && !f.getMetrics().isEmpty()) {
                        for (final Metrics m : f.getMetrics()) {
                            serializeObject(m.getMetric(), buffer);
                        }
                    }
                    serializeObject(f.getIro(), buffer);
                }
            } else {
                final SuccessCase s = (SuccessCase) reply.getResult();
                if (s != null && s.getSuccess() != null) {
                    for (final Paths p : s.getSuccess().getPaths()) {
                        serializeObject(p.getEro(), buffer);
                        serializeObject(p.getLspa(), buffer);
                        serializeObject(p.getOf(), buffer);
                        serializeObject(p.getBandwidth(), buffer);
                        if (p.getMetrics() != null && !p.getMetrics().isEmpty()) {
                            for (final Metrics m : p.getMetrics()) {
                                serializeObject(m.getMetric(), buffer);
                            }
                        }
                        serializeObject(p.getIro(), buffer);
                    }
                    serializeVendorInformationObjects(s.getSuccess().getVendorInformationObject(), buffer);
                }
            }
        }
    }

    @Override
    protected Pcrep validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        if (objects == null) {
            throw new IllegalArgumentException("Passed list can't be null.");
        }
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Pcrep message cannot be empty.");
        }
        final List<Replies> replies = Lists.newArrayList();
        while (!objects.isEmpty()) {
            final Replies r = this.getValidReply(objects, errors);
            if (r != null) {
                replies.add(r);
            }
        }
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        return new PcrepBuilder().setPcrepMessage(new PcrepMessageBuilder().setReplies(replies).build()).build();
    }

    protected Replies getValidReply(final List<Object> objects, final List<Message> errors) {
        if (!(objects.get(0) instanceof Rp)) {
            errors.add(createErrorMsg(PCEPErrors.RP_MISSING, Optional.<Rp>absent()));
            return null;
        }
        final Rp rp = (Rp) objects.get(0);
        objects.remove(0);
        final List<VendorInformationObject> vendorInfo = addVendorInformationObjects(objects);
        Result res = null;
        if (!objects.isEmpty()) {
            if (objects.get(0) instanceof NoPath) {
                final NoPath noPath = (NoPath) objects.get(0);
                objects.remove(0);
                final FailureCaseBuilder builder = new FailureCaseBuilder();
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
                    final List<VendorInformationObject> vendorInfoObjects = addVendorInformationObjects(objects);
                    if (!vendorInfoObjects.isEmpty()) {
                        builder.setVendorInformationObject(vendorInfoObjects);
                    }
                    this.parsePath(pBuilder, objects);
                    paths.add(pBuilder.build());
                }
                builder.setPaths(paths);
                res = new SuccessCaseBuilder().setSuccess(builder.build()).build();
            }
        }
        final RepliesBuilder builder = new RepliesBuilder();
        if (!vendorInfo.isEmpty()) {
            builder.setVendorInformationObject(vendorInfo);
        }
        return builder.setRp(rp).setResult(res).build();
    }

    protected void parseAttributes(final FailureCaseBuilder builder, final List<Object> objects) {
        final List<Metrics> pathMetrics = Lists.newArrayList();

        Object obj;
        State state = State.Init;
        while (!objects.isEmpty() && !state.equals(State.End)) {
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
                state = State.IroIn;
                if (obj instanceof Iro) {
                    builder.setIro((Iro) obj);
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
            builder.setMetrics(pathMetrics);
        }
    }

    protected void parsePath(final PathsBuilder builder, final List<Object> objects) {
        final List<Metrics> pathMetrics = Lists.newArrayList();

        Object obj;
        State state = State.Init;
        while (!objects.isEmpty() && !state.equals(State.End)) {
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
                state = State.IroIn;
                if (obj instanceof Iro) {
                    builder.setIro((Iro) obj);
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
            builder.setMetrics(pathMetrics);
        }
    }

    private enum State {
        Init, LspaIn, OfIn, BandwidthIn, MetricIn, IroIn, End
    }
}
