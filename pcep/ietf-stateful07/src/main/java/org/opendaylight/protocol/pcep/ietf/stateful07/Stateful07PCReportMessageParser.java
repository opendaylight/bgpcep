/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful07;

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
import org.opendaylight.protocol.pcep.spi.PSTUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.srp.Tlvs;
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

/**
 * Parser for {@link Pcrpt}
 */
public class Stateful07PCReportMessageParser extends AbstractMessageParser {

    public static final int TYPE = 10;

    public Stateful07PCReportMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof Pcrpt, "Wrong instance of Message. Passed instance of %s. Need Pcrpt.", message.getClass());
        final Pcrpt msg = (Pcrpt) message;
        final List<Reports> reports = msg.getPcrptMessage().getReports();
        final ByteBuf buffer = Unpooled.buffer();
        for (final Reports report : reports) {
            serializeReport(report, buffer);
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    private void serializeReport(final Reports report, final ByteBuf buffer) {
        if (report.getSrp() != null) {
            serializeObject(report.getSrp(), buffer);
        }
        serializeObject(report.getLsp(), buffer);
        final Path p = report.getPath();
        if (p != null) {
            serializeObject(p.getEro(), buffer);
            serializeObject(p.getLspa(), buffer);
            serializeObject(p.getBandwidth(), buffer);
            serializeObject(p.getReoptimizationBandwidth(), buffer);
            if (p.getMetrics() != null) {
                for (final Metrics m : p.getMetrics()) {
                    serializeObject(m.getMetric(), buffer);
                }
            }
            serializeObject(p.getIro(), buffer);
            serializeObject(p.getRro(), buffer);
        }
    }

    @Override
    public Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        Preconditions.checkArgument(objects != null, "Passed list can't be null.");
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Pcrpt message cannot be empty.");
        }

        final List<Reports> reports = Lists.newArrayList();

        while (!objects.isEmpty()) {
            final Reports report = getValidReports(objects, errors);
            if (report != null) {
                reports.add(report);
            }
        }
        return new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(reports).build()).build();
    }

    protected Reports getValidReports(final List<Object> objects, final List<Message> errors) {
        final ReportsBuilder builder = new ReportsBuilder();

        boolean lspViaSR = false;
        Object object = objects.remove(0);
        if (object instanceof Srp) {
            final Srp srp = (Srp) object;
            final Tlvs tlvs = srp.getTlvs();
            if (tlvs != null) {
                lspViaSR = PSTUtil.isDefaultPST(tlvs.getPathSetupType());
            }
            builder.setSrp(srp);
            if (objects.isEmpty()) {
                object = null;
            } else {
                object = objects.remove(0);
            }
        }

        if (validateLsp(object, lspViaSR, errors, builder)) {
            if (!objects.isEmpty()) {
                if (!validatePath(objects, errors, builder)) {
                    return null;
                }
            }

            return builder.build();
        }
        return null;
    }

    private static boolean validateLsp(final Object object, final boolean lspViaSR, final List<Message> errors,
            final ReportsBuilder builder) {
        if (object instanceof Lsp) {
            final Lsp lsp = (Lsp) object;
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.Tlvs tlvs = lsp.getTlvs();
            if (!lspViaSR && lsp.getPlspId().getValue() != 0 && (tlvs == null || tlvs.getLspIdentifiers() == null)) {
                final Message errorMsg = createErrorMsg(PCEPErrors.LSP_IDENTIFIERS_TLV_MISSING, Optional.absent());
                errors.add(errorMsg);
                return false;
            }

            builder.setLsp(lsp);
            return true;
        }

        errors.add(createErrorMsg(PCEPErrors.LSP_MISSING, Optional.absent()));
        return false;
    }

    private static boolean validatePath(final List<Object> objects, final List<Message> errors,
            final ReportsBuilder builder) {
        final PathBuilder pBuilder = new PathBuilder();
        Object object = objects.remove(0);
        if (object instanceof Ero) {
            pBuilder.setEro((Ero) object);
        } else {
            errors.add(createErrorMsg(PCEPErrors.ERO_MISSING, Optional.absent()));
            return false;
        }
        parsePath(objects, pBuilder);
        builder.setPath(pBuilder.build());
        return true;
    }

    private static void parsePath(final List<Object> objects, final PathBuilder builder) {
        final List<Metrics> pathMetrics = Lists.newArrayList();
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

    private static State insertObject(final State state, final Object obj, final PathBuilder builder,
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
                return State.LSPA_IN;
            }
            if (obj instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reoptimization.bandwidth.object.ReoptimizationBandwidth) {
                builder.setReoptimizationBandwidth((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reoptimization.bandwidth.object.ReoptimizationBandwidth) obj);
                return State.LSPA_IN;
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
            if (obj instanceof Rro) {
                builder.setRro((Rro) obj);
                return State.RRO_IN;
            }
        case RRO_IN:
        case END:
            return State.END;
        default:
            return state;
        }
    }

    private enum State {
        INIT, LSPA_IN, BANDWIDTH_IN, METRIC_IN, IRO_IN, RRO_IN, END
    }
}
