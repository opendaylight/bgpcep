/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated00;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;

/**
 * Parser for {@link Pcinitiate}
 */
public class CInitiated00PCInitiateMessageParser extends AbstractMessageParser {

    public static final int TYPE = 12;

    public CInitiated00PCInitiateMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof Pcinitiate, "Wrong instance of Message. Passed instance of %s. Need PcinitiateMessage.", message.getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.PcinitiateMessage init = ((Pcinitiate) message).getPcinitiateMessage();
        final ByteBuf buffer = Unpooled.buffer();
        for (final Requests req : init.getRequests()) {
            serializeRequest(req, buffer);
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    protected void serializeRequest(final Requests req, final ByteBuf buffer) {
        serializeObject(req.getSrp(), buffer);
        serializeObject(req.getLsp(), buffer);
        serializeObject(req.getEndpointsObj(), buffer);
        serializeObject(req.getEro(), buffer);
        serializeObject(req.getLspa(), buffer);
        serializeObject(req.getBandwidth(), buffer);
        if (req.getMetrics() != null) {
            for (final Metrics m : req.getMetrics()) {
                serializeObject(m.getMetric(), buffer);
            }
        }
        serializeObject(req.getIro(), buffer);
    }

    @Override
    protected Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        Preconditions.checkArgument(objects != null, "Passed list can't be null.");
        final PcinitiateMessageBuilder builder = new PcinitiateMessageBuilder();
        final List<Requests> reqs = Lists.newArrayList();
        while (!objects.isEmpty()) {
            reqs.add(this.getValidRequest(objects));
        }
        builder.setRequests(reqs);
        return new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build();
    }

    protected Requests getValidRequest(final List<Object> objects) {
        final RequestsBuilder builder = new RequestsBuilder();
        builder.setSrp((Srp) objects.get(0));
        objects.remove(0);

        builder.setLsp((Lsp) objects.get(0));
        objects.remove(0);

        final List<Metrics> metrics = Lists.newArrayList();

        Object obj;
        State state = State.INIT;
        while (!objects.isEmpty() && !state.equals(State.END)) {
            obj = objects.get(0);
            state = insertObject(state, obj, builder, metrics);
            if (!state.equals(State.END)) {
                objects.remove(0);
            }
        }
        builder.setMetrics(metrics);
        return builder.build();
    }

    private static State insertObject(final State state, final Object obj, final RequestsBuilder builder,
            final List<Metrics> metrics) {
        switch (state) {
        case INIT:
            if (obj instanceof EndpointsObj) {
                builder.setEndpointsObj((EndpointsObj) obj);
                return State.ENDPOINTS_IN;
            }
        case ENDPOINTS_IN:
            if (obj instanceof Ero) {
                builder.setEro((Ero) obj);
                return State.ERO_IN;
            }
        case ERO_IN:
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
                metrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
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
        INIT, ENDPOINTS_IN, ERO_IN, LSPA_IN, BANDWIDTH_IN, METRIC_IN, IRO_IN, END
    }
}
