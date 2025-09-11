/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.initiated;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcinitiate.message.PcinitiateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.Srp;

/**
 * Parser for {@link Pcinitiate}.
 */
public class InitiatedPCInitiateMessageParser extends AbstractMessageParser {

    public static final int TYPE = 12;

    public InitiatedPCInitiateMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof Pcinitiate,
            "Wrong instance of Message. Passed instance of %s. Need PcinitiateMessage.", message.getClass());
        final PcinitiateMessage init = ((Pcinitiate) message).getPcinitiateMessage();
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
        for (final Metrics m : req.nonnullMetrics()) {
            serializeObject(m.getMetric(), buffer);
        }
        serializeObject(req.getIro(), buffer);
    }

    @Override
    protected Message validate(final Queue<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");
        final PcinitiateMessageBuilder builder = new PcinitiateMessageBuilder();
        final var reqs = new ArrayList<Requests>();
        while (!objects.isEmpty()) {
            reqs.add(getValidRequest(objects));
        }
        builder.setRequests(reqs);
        return new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build();
    }

    protected Requests getValidRequest(final Queue<Object> objects) throws PCEPDeserializerException {
        final RequestsBuilder builder = new RequestsBuilder()
            .setSrp(consumeObject(objects, Srp.class))
            .setLsp(consumeObject(objects, Lsp.class));

        final var metrics = new ArrayList<Metrics>();
        State state = State.INIT;
        for (Object obj = objects.peek(); obj != null; obj = objects.peek()) {
            state = insertObject(state, obj, builder, metrics);
            if (state == State.END) {
                break;
            }

            objects.remove();
        }

        builder.setMetrics(metrics);
        return builder.build();
    }

    private static <T extends Object> T consumeObject(final Queue<Object> objects, final Class<T> expectedClass)
            throws PCEPDeserializerException {
        final Object obj;
        try {
            obj = objects.remove();
        } catch (NoSuchElementException e) {
            throw new PCEPDeserializerException("No objects left when expecting " + expectedClass.getSimpleName(), e);
        }
        try {
            return expectedClass.cast(obj);
        } catch (ClassCastException e) {
            throw new PCEPDeserializerException("Cannot interpret " + obj, e);
        }
    }

    private static State insertObject(final State state, final Object obj, final RequestsBuilder builder,
            final List<Metrics> metrics) {
        switch (state) {
            case INIT:
                if (obj instanceof EndpointsObj) {
                    builder.setEndpointsObj((EndpointsObj) obj);
                    return State.ENDPOINTS_IN;
                }
                // fall through
            case ENDPOINTS_IN:
                if (obj instanceof Ero) {
                    builder.setEro((Ero) obj);
                    return State.ERO_IN;
                }
                // fall through
            case ERO_IN:
                if (obj instanceof Lspa) {
                    builder.setLspa((Lspa) obj);
                    return State.LSPA_IN;
                }
                // fall through
            case LSPA_IN:
                if (obj instanceof Bandwidth) {
                    builder.setBandwidth((Bandwidth) obj);
                    return State.BANDWIDTH_IN;
                }
                // fall through
            case BANDWIDTH_IN:
                if (obj instanceof Metric) {
                    metrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                    return State.BANDWIDTH_IN;
                }
                // fall through
            case METRIC_IN:
                if (obj instanceof Iro) {
                    builder.setIro((Iro) obj);
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
        INIT, ENDPOINTS_IN, ERO_IN, LSPA_IN, BANDWIDTH_IN, METRIC_IN, IRO_IN, END
    }
}
