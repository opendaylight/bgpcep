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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PcupdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.PcupdMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.updates.PathBuilder;
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
 * Parser for {@link Pcupd}
 */
public final class Stateful02PCUpdateRequestMessageParser extends AbstractMessageParser {

    public static final int TYPE = 11;

    public Stateful02PCUpdateRequestMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof Pcupd, "Wrong instance of Message. Passed instance of %s. Need Pcupd.", message.getClass());
        final Pcupd msg = (Pcupd) message;
        final List<Updates> updates = msg.getPcupdMessage().getUpdates();
        final ByteBuf buffer = Unpooled.buffer();
        for (final Updates update : updates) {
            serializeObject(update.getLsp(), buffer);
            final Path p = update.getPath();
            if (p != null) {
                serializeObject(p.getEro(), buffer);
                serializeObject(p.getLspa(), buffer);
                serializeObject(p.getBandwidth(), buffer);
                if (p.getMetrics() != null && !p.getMetrics().isEmpty()) {
                    for (final Metrics m : p.getMetrics()) {
                        serializeObject(m.getMetric(), buffer);
                    }
                }
                serializeObject(p.getIro(), buffer);
            }
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    @Override
    protected Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        if (objects == null) {
            throw new IllegalArgumentException("Passed list can't be null.");
        }
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Pcup message cannot be empty.");
        }

        final List<Updates> updateRequests = Lists.newArrayList();

        while (!objects.isEmpty()) {
            final Updates update = getValidUpdates(objects, errors);
            if (update != null) {
                updateRequests.add(update);
            }
        }
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        return new PcupdBuilder().setPcupdMessage(new PcupdMessageBuilder().setUpdates(updateRequests).build()).build();
    }

    private Updates getValidUpdates(final List<Object> objects, final List<Message> errors) {
        final UpdatesBuilder builder = new UpdatesBuilder();
        if (objects.get(0) instanceof Lsp) {
            builder.setLsp((Lsp) objects.get(0));
            objects.remove(0);
        } else {
            errors.add(createErrorMsg(PCEPErrors.LSP_MISSING, Optional.<Rp>absent()));
            return null;
        }
        if (!objects.isEmpty()) {
            final PathBuilder pBuilder = new PathBuilder();
            if (objects.get(0) instanceof Ero) {
                pBuilder.setEro((Ero) objects.get(0));
                objects.remove(0);
            } else {
                errors.add(createErrorMsg(PCEPErrors.ERO_MISSING, Optional.<Rp>absent()));
                return null;
            }
            parsePath(objects, pBuilder);
            builder.setPath(pBuilder.build());
        }
        return builder.build();
    }

    private void parsePath(final List<Object> objects, final PathBuilder pBuilder) {
        final List<Metrics> pathMetrics = Lists.newArrayList();
        Object obj;
        State state = State.Init;
        while (!objects.isEmpty() && !state.equals(State.End)) {
            obj = objects.get(0);
            switch (state) {
            case Init:
                state = State.LspaIn;
                if (obj instanceof Lspa) {
                    pBuilder.setLspa((Lspa) obj);
                    break;
                }
            case LspaIn:
                state = State.BandwidthIn;
                if (obj instanceof Bandwidth) {
                    pBuilder.setBandwidth((Bandwidth) obj);
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
                    pBuilder.setIro((Iro) obj);
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
            pBuilder.setMetrics(pathMetrics);
        }
    }

    private enum State {
        Init, LspaIn, BandwidthIn, MetricIn, IroIn, End
    }
}
