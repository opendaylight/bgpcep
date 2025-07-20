/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcmonrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcmonrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcmonrep.message.PcmonrepMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcmonrep.message.PcmonrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.metrics.MetricPce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.response.monitoring.metrics.list.GeneralMetricsList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.response.monitoring.metrics.list.GeneralMetricsListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.response.monitoring.metrics.list.SpecificMetricsList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.response.monitoring.metrics.list.SpecificMetricsListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.response.monitoring.metrics.list.specific.metrics.list.SpecificMetrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.response.monitoring.metrics.list.specific.metrics.list.SpecificMetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcc.id.req.object.PccIdReq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.rp.object.Rp;


/**
 * Parser for {@link Pcmonrep}.
 * @see <a href="https://tools.ietf.org/html/rfc5886#section-3.2">Path Monitoring Replay Message</a>
 */
public class PCEPMonitoringReplyMessageParser extends AbstractMessageParser {

    public static final int TYPE = 9;

    public PCEPMonitoringReplyMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf buffer) {
        checkArgument(message instanceof Pcmonrep,
                "Wrong instance of Message. Passed instance of %s. Need Pcmonrep.", message.getClass());
        final PcmonrepMessage monRepMsg = ((Pcmonrep) message).getPcmonrepMessage();
        checkArgument(monRepMsg.getMonitoring() != null, "MONITORING object is mandatory.");
        final ByteBuf body = Unpooled.buffer();
        serializeObject(monRepMsg.getMonitoring(), body);
        serializeObject(monRepMsg.getPccIdReq(), body);
        if (monRepMsg.getMonitoringMetricsList() instanceof GeneralMetricsList) {
            final GeneralMetricsList gml = (GeneralMetricsList) monRepMsg.getMonitoringMetricsList();
            for (final MetricPce metricPce : gml.nonnullMetricPce()) {
                serializeMetricPce(metricPce, body);
            }
        } else if (monRepMsg.getMonitoringMetricsList() instanceof SpecificMetricsList) {
            final SpecificMetricsList sml = (SpecificMetricsList) monRepMsg.getMonitoringMetricsList();
            for (final SpecificMetrics specificMetrics : sml.nonnullSpecificMetrics()) {
                serializeObject(specificMetrics.getRp(), body);
                for (final MetricPce metricPce : specificMetrics.nonnullMetricPce()) {
                    serializeMetricPce(metricPce, body);
                }
            }
        }

        MessageUtil.formatMessage(TYPE, body, buffer);
    }

    private void serializeMetricPce(final MetricPce metricPce, final ByteBuf buffer) {
        checkArgument(metricPce.getPceId() != null, "PCE-ID must be present.");
        serializeObject(metricPce.getPceId(), buffer);
        serializeObject(metricPce.getProcTime(), buffer);
        serializeObject(metricPce.getOverload(), buffer);
    }

    @Override
    protected Message validate(final Queue<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");

        final Object monitoring = objects.poll();
        if (monitoring == null) {
            throw new PCEPDeserializerException("Pcmonrep message cannot be empty.");
        }
        if (!(monitoring instanceof Monitoring)) {
            errors.add(createErrorMsg(PCEPErrors.MONITORING_OBJECT_MISSING, Optional.empty()));
            return null;
        }

        final PcmonrepMessageBuilder builder = new PcmonrepMessageBuilder().setMonitoring((Monitoring) monitoring);

        final Object obj = objects.peek();
        if (obj instanceof PccIdReq) {
            builder.setPccIdReq((PccIdReq)obj);
            objects.remove();
        }

        validateSpecificMetrics(objects, builder);
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        return new PcmonrepBuilder().setPcmonrepMessage(builder.build()).build();
    }

    private static void validateSpecificMetrics(final Queue<Object> objects, final PcmonrepMessageBuilder builder)
            throws PCEPDeserializerException {
        final List<SpecificMetrics> specificMetrics = new ArrayList<>();

        for (Object obj = objects.peek(); obj != null; obj = objects.peek()) {
            final SpecificMetricsBuilder smb = new SpecificMetricsBuilder();
            if (obj instanceof Rp) {
                smb.setRp((Rp) obj);
                objects.remove();
            }

            final List<MetricPce> metricPceList = new ArrayList<>();
            for (obj = objects.peek(); obj != null && !(obj instanceof Rp); obj = objects.peek()) {
                metricPceList.add(Util.validateMonitoringMetrics(objects));
            }

            if (smb.getRp() != null) {
                specificMetrics.add(smb.setMetricPce(metricPceList).build());
            } else if (!metricPceList.isEmpty()) {
                builder.setMonitoringMetricsList(new GeneralMetricsListBuilder().setMetricPce(metricPceList).build());
            }
        }

        if (!specificMetrics.isEmpty()) {
            builder.setMonitoringMetricsList(
                    new SpecificMetricsListBuilder().setSpecificMetrics(specificMetrics).build());
        }
    }
}
