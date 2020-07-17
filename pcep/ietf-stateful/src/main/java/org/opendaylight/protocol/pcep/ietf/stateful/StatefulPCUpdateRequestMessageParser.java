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
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.pcupd.message.updates.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.metric.object.Metric;

/**
 * Parser for {@link Pcupd}.
 */
public class StatefulPCUpdateRequestMessageParser extends AbstractMessageParser {

    public static final int TYPE = 11;

    public StatefulPCUpdateRequestMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof Pcupd, "Wrong instance of Message. Passed instance of %s. Need Pcupd.",
            message.getClass());
        final Pcupd msg = (Pcupd) message;
        final List<Updates> updates = msg.getPcupdMessage().getUpdates();
        final ByteBuf buffer = Unpooled.buffer();
        for (final Updates update : updates) {
            serializeUpdate(update, buffer);
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    protected void serializeUpdate(final Updates update, final ByteBuf buffer) {
        serializeObject(update.getSrp(), buffer);
        serializeObject(update.getLsp(), buffer);
        final Path p = update.getPath();
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
        }
    }

    @Override
    protected Message validate(final List<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Pcup message cannot be empty.");
        }

        final List<Updates> updateRequests = new ArrayList<>();

        while (!objects.isEmpty()) {
            final Updates upd = getValidUpdates(objects, errors);
            if (upd != null) {
                updateRequests.add(upd);
            }
        }
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        return new PcupdBuilder().setPcupdMessage(new PcupdMessageBuilder().setUpdates(updateRequests).build()).build();
    }

    protected Updates getValidUpdates(final List<Object> objects, final List<Message> errors) {
        final UpdatesBuilder builder = new UpdatesBuilder();

        Object object = objects.remove(0);
        if (object instanceof Srp) {
            builder.setSrp((Srp) object);
            if (objects.isEmpty()) {
                object = null;
            } else {
                object = objects.remove(0);
            }
        } else {
            errors.add(createErrorMsg(PCEPErrors.SRP_MISSING, Optional.empty()));
        }

        if (validateLsp(object, errors, builder)) {
            if (!objects.isEmpty()) {
                if (!validatePath(objects, errors, builder)) {
                    return null;
                }
            }

            return builder.build();
        }
        return null;
    }

    private static boolean validateLsp(final Object object, final List<Message> errors, final UpdatesBuilder builder) {
        if (object instanceof Lsp) {
            builder.setLsp((Lsp) object);
        } else {
            errors.add(createErrorMsg(PCEPErrors.LSP_MISSING, Optional.empty()));
            return false;
        }
        return true;
    }

    private static boolean validatePath(final List<Object> objects, final List<Message> errors,
            final UpdatesBuilder builder) {
        final PathBuilder pBuilder = new PathBuilder();
        Object object = objects.remove(0);
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

    private static void parsePath(final List<Object> objects, final PathBuilder pathBuilder) {
        final List<Metrics> pathMetrics = new ArrayList<>();
        Object obj;
        State state = State.INIT;
        while (!objects.isEmpty() && !state.equals(State.END)) {
            obj = objects.get(0);
            state = insertObject(state,obj, pathBuilder, pathMetrics);
            if (!state.equals(State.END)) {
                objects.remove(0);
            }
        }
        if (!pathMetrics.isEmpty()) {
            pathBuilder.setMetrics(pathMetrics);
        }
    }

    private static State insertObject(final State state, final Object obj, final PathBuilder pathBuilder,
            final List<Metrics> pathMetrics) {
        switch (state) {
            case INIT:
                if (obj instanceof Lspa) {
                    pathBuilder.setLspa((Lspa) obj);
                    return State.LSPA_IN;
                }
                // fall through
            case LSPA_IN:
                if (obj instanceof Bandwidth) {
                    pathBuilder.setBandwidth((Bandwidth) obj);
                    return State.LSPA_IN;
                }
                if (obj instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                        .reoptimization.bandwidth.object.ReoptimizationBandwidth) {
                    pathBuilder.setReoptimizationBandwidth((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                            .yang.pcep.types.rev181109.reoptimization.bandwidth.object.ReoptimizationBandwidth) obj);
                    return State.LSPA_IN;
                }
                // fall through
            case BANDWIDTH_IN:
                if (obj instanceof Metric) {
                    pathMetrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                    return State.BANDWIDTH_IN;
                }
                // fall through
            case METRIC_IN:
                if (obj instanceof Iro) {
                    pathBuilder.setIro((Iro) obj);
                    return State.IRO_IN;
                }
                // fall through
            case IRO_IN:
            case END:
                return State.END;
            default:
                return state;
        }
    }

    private enum State {
        INIT, LSPA_IN, BANDWIDTH_IN, METRIC_IN, IRO_IN, END
    }
}
