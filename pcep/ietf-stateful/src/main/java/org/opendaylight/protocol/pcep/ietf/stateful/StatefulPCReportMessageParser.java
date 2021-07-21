/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.PSTUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.srp.object.srp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reoptimization.bandwidth.object.ReoptimizationBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.Rro;

/**
 * Parser for {@link Pcrpt}.
 */
public class StatefulPCReportMessageParser extends AbstractMessageParser {

    public static final int TYPE = 10;

    public StatefulPCReportMessageParser(final ObjectRegistry registry) {
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

        final List<Reports> reports = new ArrayList<>();

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
        if (object instanceof Srp) {
            final Srp srp = (Srp) object;
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
        if (object instanceof Lsp) {
            final Lsp lsp = (Lsp) object;
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp
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
        if (object instanceof Ero) {
            pBuilder.setEro((Ero) object);
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

        for (Object obj = objects.peek(); obj != null; obj = objects.peek()) {
            /*
             * Object could be inserted in various order depending if PCC is conform to RFC8231 or not. Try to be
             * flexible enough to handle all cases.
             */
            if (obj instanceof Srp) {
                break;
            } else if (obj instanceof Lspa) {
                builder.setLspa((Lspa) obj);
            } else if (obj instanceof Bandwidth) {
                builder.setBandwidth((Bandwidth) obj);
            } else if (obj instanceof ReoptimizationBandwidth) {
                builder.setReoptimizationBandwidth((ReoptimizationBandwidth) obj);
            } else if (obj instanceof Metric) {
                pathMetrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
            } else if (obj instanceof Iro) {
                builder.setIro((Iro) obj);
            } else if (obj instanceof Rro) {
                builder.setRro((Rro) obj);
            }
            objects.remove();
        }
        if (!pathMetrics.isEmpty()) {
            builder.setMetrics(pathMetrics);
        }
    }
}
