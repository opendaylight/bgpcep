/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.message;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.protocol.pcep.parser.util.Util;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.metrics.MetricPce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcc.id.req.object.PccIdReq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pce.id.object.PceId;
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

    public PCEPReplyMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof Pcrep, "Wrong instance of Message. Passed instance of %s. Need Pcrep.", message.getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.PcrepMessage repMsg = ((Pcrep) message).getPcrepMessage();
        Preconditions.checkArgument(repMsg.getReplies() != null && !repMsg.getReplies().isEmpty(), "Replies cannot be null or empty.");
        final ByteBuf buffer = Unpooled.buffer();
        for (final Replies reply : repMsg.getReplies()) {
            Preconditions.checkArgument(reply.getRp() != null, "Reply must contain RP object.");
            serializeReply(reply, buffer);
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    protected void serializeReply(final Replies reply, final ByteBuf buffer) {
        serializeObject(reply.getRp(), buffer);
        serializeMonitoring(reply, buffer);
        serializeVendorInformationObjects(reply.getVendorInformationObject(), buffer);
        if (reply.getResult() == null) {
            return;
        }
        if (reply.getResult() instanceof FailureCase) {
            final FailureCase f = (FailureCase) reply.getResult();
            serializeFailure(f, buffer);
            return;
        }
        final SuccessCase s = (SuccessCase) reply.getResult();
        serializeSuccess(s, buffer);
        serializeMonitoringMetrics(reply, buffer);
    }

    private void serializeFailure(final FailureCase f, final ByteBuf buffer) {
        if (f == null) {
            return;
        }
        serializeObject(f.getNoPath(), buffer);
        serializeObject(f.getLspa(), buffer);
        serializeObject(f.getBandwidth(), buffer);
        if (f.getMetrics() != null) {
            for (final Metrics m : f.getMetrics()) {
                serializeObject(m.getMetric(), buffer);
            }
        }
        serializeObject(f.getIro(), buffer);
    }

    private void serializeSuccess(final SuccessCase s, final ByteBuf buffer) {
        if (s == null || s.getSuccess() == null) {
            return;
        }
        for (final Paths p : s.getSuccess().getPaths()) {
            serializeObject(p.getEro(), buffer);
            serializeObject(p.getLspa(), buffer);
            serializeObject(p.getOf(), buffer);
            serializeObject(p.getBandwidth(), buffer);
            if (p.getMetrics() != null) {
                for (final Metrics m : p.getMetrics()) {
                    serializeObject(m.getMetric(), buffer);
                }
            }
            serializeObject(p.getIro(), buffer);
        }
        serializeVendorInformationObjects(s.getSuccess().getVendorInformationObject(), buffer);
    }

    private void serializeMonitoring(final Replies reply, final ByteBuf buffer) {
        serializeObject(reply.getMonitoring(), buffer);
        serializeObject(reply.getPccIdReq(), buffer);
    }

    private void serializeMonitoringMetrics(final Replies reply, final ByteBuf buffer) {
        if (reply.getMetricPce() != null) {
            for (final MetricPce metricPce : reply.getMetricPce()) {
                serializeMetricPce(metricPce, buffer);
            }
        }
    }

    protected void serializeMetricPce(final MetricPce metricPce, final ByteBuf buffer) {
        Preconditions.checkArgument(metricPce.getPceId() != null, "PCE-ID must be present.");
        serializeObject(metricPce.getPceId(), buffer);
        serializeObject(metricPce.getProcTime(), buffer);
        serializeObject(metricPce.getOverload(), buffer);
    }

    @Override
    protected Pcrep validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        Preconditions.checkArgument(objects != null, "Passed list can't be null.");
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Pcrep message cannot be empty.");
        }
        final List<Replies> replies = new ArrayList<>();
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

    protected Replies getValidReply(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        Object object = objects.remove(0);
        if (!(object instanceof Rp)) {
            errors.add(createErrorMsg(PCEPErrors.RP_MISSING, Optional.empty()));
            return null;
        }
        final Rp rp = (Rp) object;
        final RepliesBuilder repliesBuilder = new RepliesBuilder();
        if (!objects.isEmpty() && objects.get(0) instanceof Monitoring) {
            repliesBuilder.setMonitoring((Monitoring) objects.get(0));
            objects.remove(0);
        }
        if (!objects.isEmpty() && objects.get(0) instanceof PccIdReq) {
            repliesBuilder.setPccIdReq((PccIdReq) objects.get(0));
            objects.remove(0);
        }
        final List<VendorInformationObject> vendorInfo = addVendorInformationObjects(objects);
        Result res = null;
        if (!objects.isEmpty()) {
            if (objects.get(0) instanceof NoPath) {
                res = handleNoPath((NoPath) objects.get(0), objects);
            } else if (objects.get(0) instanceof Ero) {
                res = handleEro((Ero) objects.get(0), objects);
            }
        }
        final List<MetricPce> metricPceList = new ArrayList<>();
        if (!objects.isEmpty() && objects.get(0) instanceof PceId) {
            while (!objects.isEmpty()) {
                metricPceList.add(Util.validateMonitoringMetrics(objects));
            }
        }
        if (!vendorInfo.isEmpty()) {
            repliesBuilder.setVendorInformationObject(vendorInfo);
        }
        if (!metricPceList.isEmpty()) {
            repliesBuilder.setMetricPce(metricPceList);
        }
        return repliesBuilder.setRp(rp).setResult(res).build();
    }

    private Result handleNoPath(final NoPath noPath, final List<Object> objects) {
        objects.remove(0);
        final FailureCaseBuilder builder = new FailureCaseBuilder();
        builder.setNoPath(noPath);
        while (!objects.isEmpty() && !(objects.get(0) instanceof PceId)) {
            this.parseAttributes(builder, objects);
        }
        return builder.build();
    }

    private Result handleEro(final Ero ero, final List<Object> objects) {
        objects.remove(0);
        final SuccessBuilder builder = new SuccessBuilder();
        final List<Paths> paths = new ArrayList<>();
        final PathsBuilder pBuilder = new PathsBuilder();
        pBuilder.setEro(ero);
        while (!objects.isEmpty() && !(objects.get(0) instanceof PceId)) {
            final List<VendorInformationObject> vendorInfoObjects = addVendorInformationObjects(objects);
            if (!vendorInfoObjects.isEmpty()) {
                builder.setVendorInformationObject(vendorInfoObjects);
            }
            this.parsePath(pBuilder, objects);
            paths.add(pBuilder.build());
        }
        builder.setPaths(paths);
        return new SuccessCaseBuilder().setSuccess(builder.build()).build();
    }

    protected void parseAttributes(final FailureCaseBuilder builder, final List<Object> objects) {
        final List<Metrics> pathMetrics = new ArrayList<>();

        Object obj;
        State state = State.INIT;
        while (!objects.isEmpty() && !state.equals(State.END)) {
            obj = objects.get(0);
            state = insertObject(state, obj, builder, pathMetrics);
            if (!state.equals(State.END)) {
                objects.remove(0);
            }
        }
        if (!pathMetrics.isEmpty()) {
            builder.setMetrics(pathMetrics);
        }
    }

    private static State insertObject(final State state, final Object obj, final FailureCaseBuilder builder,
            final List<Metrics> pathMetrics) {
        switch (state) {
        case INIT:
            if (obj instanceof Lspa) {
                builder.setLspa((Lspa) obj);
                return State.LSPA_IN;
            }
        case LSPA_IN:
            if (obj instanceof Bandwidth) {
                builder.setBandwidth((Bandwidth) obj);
                return State.BANDWIDTH_IN;
            }
        case BANDWIDTH_IN:
            if (obj instanceof Metric) {
                pathMetrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                return State.BANDWIDTH_IN;
            }
        case METRIC_IN:
            if (obj instanceof Iro) {
                builder.setIro((Iro) obj);
                return State.IRO_IN;
            }
        case IRO_IN:
        case END:
            return State.END;
        default:
            return state;
        }
    }

    protected void parsePath(final PathsBuilder builder, final List<Object> objects) {
        final List<Metrics> pathMetrics = new ArrayList<>();

        Object obj;
        State state = State.INIT;
        while (!objects.isEmpty() && !state.equals(State.END)) {
            obj = objects.get(0);
            state = insertObject(state, obj, builder, pathMetrics);
            if (!state.equals(State.END)) {
                objects.remove(0);
            }
        }
        if (!pathMetrics.isEmpty()) {
            builder.setMetrics(pathMetrics);
        }
    }

    private static State insertObject(final State state, final Object obj, final PathsBuilder builder,
            final List<Metrics> pathMetrics) {
        switch (state) {
        case INIT:
            if (obj instanceof Lspa) {
                builder.setLspa((Lspa) obj);
                return State.LSPA_IN;
            }
        case LSPA_IN:
            if (obj instanceof Of) {
                builder.setOf((Of) obj);
                return State.OF_IN;
            }
        case OF_IN:
            if (obj instanceof Bandwidth) {
                builder.setBandwidth((Bandwidth) obj);
                return State.BANDWIDTH_IN;
            }
        case BANDWIDTH_IN:
            if (obj instanceof Metric) {
                pathMetrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                return State.BANDWIDTH_IN;
            }
        case METRIC_IN:
            if (obj instanceof Iro) {
                builder.setIro((Iro) obj);
                return State.IRO_IN;
            }
        case IRO_IN:
        case END:
            return State.END;
        default:
            return state;
        }
    }

    private enum State {
        INIT, LSPA_IN, OF_IN, BANDWIDTH_IN, METRIC_IN, IRO_IN, END
    }
}
