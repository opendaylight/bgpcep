/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.crabbe.initiated00;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.PcinitiateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.PcinitiateMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.pcinitiate.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;

/**
 * Parser for {@link Pcinitiate}
 */
public class PcinitiateMessageParser extends AbstractMessageParser {
    public static final int TYPE = 12;

    public PcinitiateMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof Pcinitiate, "Wrong instance of Message. Passed instance of %s. Need Pcinitiate.", message.getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.pcinitiate.message.PcinitiateMessage init = ((Pcinitiate) message).getPcinitiateMessage();
        final ByteBuf buffer = Unpooled.buffer();
        for (final Requests req : init.getRequests()) {
            serializeObject(req.getEndpointsObj(), buffer);
            serializeObject(req.getLspa(), buffer);
            serializeObject(req.getEro(), buffer);
            serializeObject(req.getBandwidth(), buffer);
            if (req.getMetrics() != null && !req.getMetrics().isEmpty()) {
                for (final Metrics m : req.getMetrics()) {
                    serializeObject(m.getMetric(), buffer);
                }
            }
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    @Override
    protected Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        if (objects == null) {
            throw new IllegalArgumentException("Passed list can't be null.");
        }
        final PcinitiateMessageBuilder builder = new PcinitiateMessageBuilder();
        final List<Requests> reqs = Lists.newArrayList();
        while (!objects.isEmpty()) {
            reqs.add(this.getValidRequest(objects));
        }
        builder.setRequests(reqs);
        return new PcinitiateBuilder().setPcinitiateMessage(builder.build()).build();
    }

    private Requests getValidRequest(final List<Object> objects) {
        final RequestsBuilder builder = new RequestsBuilder();
        builder.setEndpointsObj((EndpointsObj) objects.get(0));
        objects.remove(0);

        builder.setLspa((Lspa) objects.get(0));
        objects.remove(0);
        final List<Metrics> metrics = Lists.newArrayList();

        Object obj;
        State state = State.INIT;
        while (!objects.isEmpty() && !state.equals(State.END)) {
            obj = objects.get(0);

            switch (state) {
            case INIT:
                state = State.ERO_IN;
                if (obj instanceof Ero) {
                    builder.setEro((Ero) obj);
                    break;
                }
            case ERO_IN:
                state = State.BANDWIDTH_IN;
                if (obj instanceof Bandwidth) {
                    builder.setBandwidth((Bandwidth) obj);
                    break;
                }
            case BANDWIDTH_IN:
                state = State.METRIC_IN;
                if (obj instanceof Metric) {
                    metrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                    state = State.BANDWIDTH_IN;
                    break;
                }
            case METRIC_IN:
                state = State.END;
                break;
            case END:
                break;
            default:
                break;
            }
            if (!state.equals(State.END)) {
                objects.remove(0);
            }
        }
        builder.setMetrics(metrics);
        return builder.build();
    }

    private enum State {
        INIT, ERO_IN, BANDWIDTH_IN, METRIC_IN, END
    }
}
