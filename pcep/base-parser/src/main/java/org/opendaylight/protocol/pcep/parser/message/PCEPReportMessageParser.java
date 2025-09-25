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
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.PSTUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.srp.Tlvs;

/**
 * Parser for {@link Pcrpt}.
 */
public class PCEPReportMessageParser extends AbstractMessageParser {

    public static final int TYPE = 10;

    public PCEPReportMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof Pcrpt, "Wrong instance of Message. Passed instance of %s. Need Pcrpt.",
            message.getClass());
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
            for (final Metrics m : p.nonnullMetrics()) {
                serializeObject(m.getMetric(), buffer);
            }
            serializeObject(p.getIro(), buffer);
            serializeObject(p.getRro(), buffer);
        }
    }

    @Override
    public Message validate(final Queue<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Pcrpt message cannot be empty.");
        }

        final var reports = new ArrayList<Reports>();
        while (!objects.isEmpty()) {
            final Reports report = getValidReports(objects, errors);
            if (report != null) {
                reports.add(report);
            }
        }
        return new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(reports).build()).build();
    }

    protected Reports getValidReports(final Queue<Object> objects, final List<Message> errors) {
        final ReportsBuilder builder = new ReportsBuilder();

        boolean lspViaSR = false;
        Object object = objects.remove();
        if (object instanceof Srp srp) {
            final Tlvs tlvs = srp.getTlvs();
            if (tlvs != null) {
                lspViaSR = PSTUtil.isDefaultPST(tlvs.getPathSetupType());
            }
            builder.setSrp(srp);
            object = objects.poll();
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
        if (object instanceof Lsp lsp) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp
                .object.lsp.Tlvs tlvs = lsp.getTlvs();
            if (!lspViaSR && lsp.getPlspId().getValue().toJava() != 0
                    && (tlvs == null || tlvs.getLspIdentifiers() == null)) {
                final Message errorMsg = createErrorMsg(PCEPErrors.LSP_IDENTIFIERS_TLV_MISSING, Optional.empty());
                errors.add(errorMsg);
                return false;
            }

            builder.setLsp(lsp);
            return true;
        }

        errors.add(createErrorMsg(PCEPErrors.LSP_MISSING, Optional.empty()));
        return false;
    }

    private static boolean validatePath(final Queue<Object> objects, final List<Message> errors,
            final ReportsBuilder builder) {
        final PathBuilder pBuilder = new PathBuilder();
        Object object = objects.remove();
        if (object instanceof Ero ero) {
            pBuilder.setEro(ero);
        } else {
            errors.add(createErrorMsg(PCEPErrors.ERO_MISSING, Optional.empty()));
            return false;
        }
        parsePath(objects, pBuilder);
        builder.setPath(pBuilder.build());
        return true;
    }

    private static void parsePath(final Queue<Object> objects, final PathBuilder builder) {
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

    /**
     * Determine the type of Object and insert it in the PathBuilder.
     *
     * <p>This method uses a state machine to check that Objects are seen only once and to speed up the browsing.
     * However, the order of Object in the PcReport has changed between old draft version and final RFC8231.
     * Indeed, as per RFC8231, the PcReport is composed of: ["SRP"], "LSP", "path"
     * where "path" = "intended-path", ["actual-attribute-list", "actual-path"], "intended-attribute-list"
     * and where "intended-path" = ERO, "actual-attribute-list" = BANDWIDTH, METRICS, "actual-path" = RRO
     * and "intended-attribute-list" = LSPA, BANDWIDTH, METRICS, IRO
     * In old draft version, "intended-attribute-list" was placed just right after the "intended-path".
     * Thus, the state machine should be flexible enough to accommodate to PCCs that continue to use old draft and
     * PCCs that are compliant to the RFC8231.</p>
     *
     * @param state         Current State of the state machine
     * @param obj           Object to be identify and added to Path Builder
     * @param builder       Path Builder to be fill with the Object
     * @param pathMetrics   List of Metrics to be fill with Object when it is a Metrics
     *
     * @return              New State of the state machine
     */
    private static State insertObject(final State state, final Object obj, final PathBuilder builder,
            final List<Metrics> pathMetrics) {
        switch (state) {
            case INIT:
                if (obj instanceof Lspa lspa) {
                    builder.setLspa(lspa);
                    return State.LSPA_IN;
                }
                // fall through
            case LSPA_IN:
                // Check presence for <intended-attribute-list> i.e LSPA, Bandwidth, Metrics, IRO ... as per old draft
                if (obj instanceof Bandwidth bandwidth) {
                    builder.setBandwidth(bandwidth);
                    return State.LSPA_IN;
                }
                if (obj
                        instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object
                            .rev250930.reoptimization.bandwidth.object.ReoptimizationBandwidth reoptBandwidth) {
                    builder.setReoptimizationBandwidth(reoptBandwidth);
                    return State.LSPA_IN;
                }
                // fall through
            case BANDWIDTH_IN:
                if (obj instanceof Metric metric) {
                    pathMetrics.add(new MetricsBuilder().setMetric(metric).build());
                    return State.BANDWIDTH_IN;
                }
                // fall through
            case METRIC_IN:
                if (obj instanceof Iro iro) {
                    builder.setIro(iro);
                    return State.IRO_IN;
                }
                // fall through
            case IRO_IN:
                if (obj instanceof Rro rro) {
                    builder.setRro(rro);
                    return State.RRO_IN;
                }
                // fall through
            case RRO_IN:
                // Check presence for <intended-attribute-list> i.e LSPA, Bandwidth, Metrics, IRO ... as per RFC8231
                if (obj instanceof Lspa lspa) {
                    builder.setLspa(lspa);
                    return State.LSPA_IN;
                }
                // fall through
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
