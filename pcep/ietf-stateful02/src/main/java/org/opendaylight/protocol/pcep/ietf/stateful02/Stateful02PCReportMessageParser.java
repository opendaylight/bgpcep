/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

/**
 * Parser for {@link Pcrpt}
 */
public final class Stateful02PCReportMessageParser extends AbstractMessageParser {

    public static final int TYPE = 10;

    public Stateful02PCReportMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof Pcrpt, "Wrong instance of Message. Passed instance of %s. Need Pcrpt.", message.getClass());
        final Pcrpt msg = (Pcrpt) message;
        final List<Reports> reports = msg.getPcrptMessage().getReports();
        ByteBuf buffer = Unpooled.buffer();
        for (final Reports report : reports) {
            serializeObject(report.getLsp(), buffer);
            final Path p = report.getPath();
            if (p != null) {
                if (p.getEro() != null) {
                    serializeObject(p.getEro(), buffer);
                }
                if (p.getLspa() != null) {
                    serializeObject(p.getLspa(), buffer);
                }
                if (p.getBandwidth() != null) {
                    serializeObject(p.getBandwidth(), buffer);
                }
                if (p.getMetrics() != null && !p.getMetrics().isEmpty()) {
                    for (final Metrics m : p.getMetrics()) {
                        serializeObject(m.getMetric(), buffer);
                    }
                }
                if (p.getIro() != null) {
                    serializeObject(p.getRro(), buffer);
                }
            }
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
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
        if (objects.get(0) instanceof Lsp) {
            builder.setLsp((Lsp) objects.get(0));
            objects.remove(0);
        } else {
            errors.add(createErrorMsg(PCEPErrors.LSP_MISSING, Optional.<Rp>absent()));
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
        Init, EroIn, LspaIn, BandwidthIn, MetricIn, IroIn, End
    }
}
