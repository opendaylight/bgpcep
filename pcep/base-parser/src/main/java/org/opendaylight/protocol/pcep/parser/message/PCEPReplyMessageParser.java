/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser.message;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.parser.util.Util;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.monitoring.metrics.MetricPce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcc.id.req.object.PccIdReq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pce.id.object.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.PcrepMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.Replies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.Result;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.FailureCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.FailureCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.SuccessCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.SuccessCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.failure._case.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.success._case.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.success._case.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcrep.message.pcrep.message.replies.result.success._case.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.vendor.information.objects.VendorInformationObject;

/**
 * Parser for {@link Pcrep}.
 */
public class PCEPReplyMessageParser extends AbstractMessageParser {

    public static final int TYPE = 4;

    public PCEPReplyMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof Pcrep,
                "Wrong instance of Message. Passed instance of %s. Need Pcrep.", message.getClass());
        final PcrepMessage repMsg = ((Pcrep) message).getPcrepMessage();
        final List<Replies> replies = repMsg.nonnullReplies();

        checkArgument(!replies.isEmpty(), "Replies cannot be null or empty.");
        final ByteBuf buffer = Unpooled.buffer();
        for (final Replies reply : replies) {
            checkArgument(reply.getRp() != null, "Reply must contain RP object.");
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

    private void serializeFailure(final FailureCase failure, final ByteBuf buffer) {
        if (failure == null) {
            return;
        }
        serializeObject(failure.getNoPath(), buffer);
        serializeObject(failure.getLspa(), buffer);
        serializeObject(failure.getBandwidth(), buffer);
        for (final Metrics m : failure.nonnullMetrics()) {
            serializeObject(m.getMetric(), buffer);
        }
        serializeObject(failure.getIro(), buffer);
    }

    private void serializeSuccess(final SuccessCase success, final ByteBuf buffer) {
        if (success == null || success.getSuccess() == null) {
            return;
        }
        for (final Paths p : success.getSuccess().nonnullPaths()) {
            serializeObject(p.getEro(), buffer);
            serializeObject(p.getLspa(), buffer);
            serializeObject(p.getOf(), buffer);
            serializeObject(p.getBandwidth(), buffer);
            for (final Metrics m : p.nonnullMetrics()) {
                serializeObject(m.getMetric(), buffer);
            }
            serializeObject(p.getIro(), buffer);
        }
        serializeVendorInformationObjects(success.getSuccess().getVendorInformationObject(), buffer);
    }

    private void serializeMonitoring(final Replies reply, final ByteBuf buffer) {
        serializeObject(reply.getMonitoring(), buffer);
        serializeObject(reply.getPccIdReq(), buffer);
    }

    private void serializeMonitoringMetrics(final Replies reply, final ByteBuf buffer) {
        for (final MetricPce metricPce : reply.nonnullMetricPce()) {
            serializeMetricPce(metricPce, buffer);
        }
    }

    protected void serializeMetricPce(final MetricPce metricPce, final ByteBuf buffer) {
        checkArgument(metricPce.getPceId() != null, "PCE-ID must be present.");
        serializeObject(metricPce.getPceId(), buffer);
        serializeObject(metricPce.getProcTime(), buffer);
        serializeObject(metricPce.getOverload(), buffer);
    }

    @Override
    protected Pcrep validate(final Queue<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Pcrep message cannot be empty.");
        }
        final List<Replies> replies = new ArrayList<>();
        while (!objects.isEmpty()) {
            final Replies r = getValidReply(objects, errors);
            if (r != null) {
                replies.add(r);
            }
        }
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        return new PcrepBuilder().setPcrepMessage(new PcrepMessageBuilder().setReplies(replies).build()).build();
    }

    protected Replies getValidReply(final Queue<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        Object obj = objects.remove();
        if (!(obj instanceof Rp)) {
            errors.add(createErrorMsg(PCEPErrors.RP_MISSING, Optional.empty()));
            return null;
        }

        final RepliesBuilder repliesBuilder = new RepliesBuilder().setRp((Rp) obj);
        obj = objects.peek();
        if (obj instanceof Monitoring) {
            repliesBuilder.setMonitoring((Monitoring) obj);
            objects.remove();
            obj = objects.peek();
        }
        if (obj instanceof PccIdReq) {
            repliesBuilder.setPccIdReq((PccIdReq) obj);
            objects.remove();
            // last option, no need to peek
        }

        // note: this may modify 'objects'
        final List<VendorInformationObject> vendorInfo = addVendorInformationObjects(objects);
        if (!vendorInfo.isEmpty()) {
            repliesBuilder.setVendorInformationObject(vendorInfo);
        }

        final Result res;
        obj = objects.peek();
        if (obj instanceof NoPath) {
            objects.remove();
            res = handleNoPath((NoPath) obj, objects);
        } else if (obj instanceof Ero) {
            res = handleEros(objects);
        } else {
            res = null;
        }

        if (objects.peek() instanceof PceId) {
            final List<MetricPce> metricPceList = new ArrayList<>();
            while (!objects.isEmpty()) {
                metricPceList.add(Util.validateMonitoringMetrics(objects));
            }
            if (!metricPceList.isEmpty()) {
                repliesBuilder.setMetricPce(metricPceList);
            }
        }

        return repliesBuilder.setResult(res).build();
    }

    private Result handleNoPath(final NoPath noPath, final Queue<Object> objects) {
        final FailureCaseBuilder builder = new FailureCaseBuilder().setNoPath(noPath);
        for (Object obj = objects.peek(); obj != null && !(obj instanceof PceId); obj = objects.peek()) {
            parseAttributes(builder, objects);
        }
        return builder.build();
    }

    private Result handleEros(final Queue<Object> objects) {
        final SuccessBuilder builder = new SuccessBuilder();
        final List<Paths> paths = new ArrayList<>();

        for (Object obj = objects.peek(); obj != null && !(obj instanceof PceId); obj = objects.peek()) {
            final PathsBuilder pBuilder = new PathsBuilder();
            if (obj instanceof Ero) {
                pBuilder.setEro((Ero) obj);
                objects.remove();
            }
            final List<VendorInformationObject> vendorInfoObjects = addVendorInformationObjects(objects);
            if (!vendorInfoObjects.isEmpty()) {
                builder.setVendorInformationObject(vendorInfoObjects);
            }
            parsePath(pBuilder, objects);
            paths.add(pBuilder.build());
        }
        builder.setPaths(paths);
        return new SuccessCaseBuilder().setSuccess(builder.build()).build();
    }

    protected void parseAttributes(final FailureCaseBuilder builder, final Queue<Object> objects) {
        final List<Metrics> pathMetrics = new ArrayList<>();

        State state = State.INIT;
        for (Object obj = objects.peek(); obj != null; obj = objects.peek()) {
            state = insertObject(state, obj, builder, pathMetrics);
            if (state == State.END) {
                break;
            }

            objects.remove();
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
                // fallthrough
            case LSPA_IN:
                if (obj instanceof Bandwidth) {
                    builder.setBandwidth((Bandwidth) obj);
                    return State.BANDWIDTH_IN;
                }
                // fallthrough
            case BANDWIDTH_IN:
                if (obj instanceof Metric) {
                    pathMetrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                    return State.BANDWIDTH_IN;
                }
                // fallthrough
            case METRIC_IN:
                if (obj instanceof Iro) {
                    builder.setIro((Iro) obj);
                    return State.IRO_IN;
                }
                // fallthrough
            case IRO_IN:
            case END:
                return State.END;
            default:
                return state;
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
                // fallthrough
            case LSPA_IN:
                if (obj instanceof Of) {
                    builder.setOf((Of) obj);
                    return State.OF_IN;
                }
                // fallthrough
            case OF_IN:
                if (obj instanceof Bandwidth) {
                    builder.setBandwidth((Bandwidth) obj);
                    return State.BANDWIDTH_IN;
                }
                // fallthrough
            case BANDWIDTH_IN:
                if (obj instanceof Metric) {
                    pathMetrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                    return State.BANDWIDTH_IN;
                }
                // fallthrough
            case METRIC_IN:
                if (obj instanceof Iro) {
                    builder.setIro((Iro) obj);
                    return State.IRO_IN;
                }
                // fallthrough
            case IRO_IN:
            case END:
                return State.END;
            default:
                return state;
        }
    }

    protected void parsePath(final PathsBuilder builder, final Queue<Object> objects) {
        final List<Metrics> pathMetrics = new ArrayList<>();

        State state = State.INIT;
        for (Object obj = objects.peek(); obj != null; obj = objects.peek()) {
            state = insertObject(state, obj, builder, pathMetrics);
            if (state == State.END) {
                break;
            }

            objects.remove();
        }

        if (!pathMetrics.isEmpty()) {
            builder.setMetrics(pathMetrics);
        }
    }

    private enum State {
        INIT, LSPA_IN, OF_IN, BANDWIDTH_IN, METRIC_IN, IRO_IN, END
    }
}
