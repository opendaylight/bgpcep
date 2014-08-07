/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.pcep.impl.message.PCEPRequestMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.P2p1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.P2p1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.load.balancing.object.LoadBalancing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.SegmentComputation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.SegmentComputationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.P2p;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.P2pBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.ReportedRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.ReportedRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

/**
 * Parser for Pcreq
 */
public final class Stateful02PCRequestMessageParser extends PCEPRequestMessageParser {

    public Stateful02PCRequestMessageParser(final ObjectRegistry registry, final VendorInformationObjectRegistry viReg) {
        super(registry, viReg);
    }

    @Override
    protected void serializeP2P(final ByteBuf buffer, final P2p p2p) {
        if (p2p.getEndpointsObj() != null) {
            serializeObject(p2p.getEndpointsObj(), buffer);
        }
        if (p2p.getAugmentation(P2p1.class) != null && p2p.getAugmentation(P2p1.class).getLsp() != null) {
            serializeObject(p2p.getAugmentation(P2p1.class).getLsp(), buffer);
        }
        if (p2p.getReportedRoute() != null) {
            final ReportedRoute rr = p2p.getReportedRoute();
            if (rr.getRro() != null) {
                serializeObject(rr.getRro(), buffer);
            }
            if (rr.getBandwidth() != null) {
                serializeObject(rr.getBandwidth(), buffer);
            }
        }
        if (p2p.getLoadBalancing() != null) {
            serializeObject(p2p.getLoadBalancing(), buffer);
        }
        if (p2p.getLspa() != null) {
            serializeObject(p2p.getLspa(), buffer);
        }
        if (p2p.getBandwidth() != null) {
            serializeObject(p2p.getBandwidth(), buffer);
        }
        if (p2p.getMetrics() != null) {
            for (final Metrics m : p2p.getMetrics()) {
                serializeObject(m.getMetric(), buffer);
            }
        }
        if (p2p.getIro() != null) {
            serializeObject(p2p.getIro(), buffer);
        }
        if (p2p.getRro() != null) {
            serializeObject(p2p.getRro(), buffer);
        }
        if (p2p.getXro() != null) {
            serializeObject(p2p.getXro(), buffer);
        }
        if (p2p.getOf() != null) {
            serializeObject(p2p.getOf(), buffer);
        }
        if (p2p.getClassType() != null) {
            serializeObject(p2p.getClassType(), buffer);
        }
    }

    @Override
    protected SegmentComputation getSegmentComputation(final P2pBuilder builder, final List<Object> objects, final List<Message> errors,
            final Rp rp) {
        final List<Metrics> metrics = Lists.newArrayList();

        State state = State.Init;
        while (!objects.isEmpty() && state != State.End) {
            Object obj = objects.get(0);

            switch (state) {
            case Init:
                state = State.LspIn;
                if (obj instanceof Lsp) {
                    builder.addAugmentation(P2p1.class, new P2p1Builder().setLsp((Lsp) obj).build());
                    break;
                }
            case LspIn:
                state = State.ReportedIn;
                if (obj instanceof Rro) {
                    final ReportedRouteBuilder rrBuilder = new ReportedRouteBuilder();
                    rrBuilder.setRro((Rro) obj);
                    objects.remove(0);
                    obj = objects.get(0);
                    if (obj instanceof Bandwidth) {
                        rrBuilder.setBandwidth((Bandwidth) obj);
                    }
                    break;
                }
            case ReportedIn:
                state = State.LoadBIn;
                if (obj instanceof LoadBalancing) {
                    builder.setLoadBalancing((LoadBalancing) obj);
                    break;
                }
            case LoadBIn:
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
                    metrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
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
                state = State.RroIn;
                if (obj instanceof Rro) {
                    builder.setRro((Rro) obj);
                    break;
                }
            case RroIn:
                state = State.XroIn;
                if (obj instanceof Xro) {
                    builder.setXro((Xro) obj);
                    break;
                }
            case XroIn:
                state = State.OfIn;
                if (obj instanceof Of) {
                    builder.setOf((Of) obj);
                    break;
                }
            case OfIn:
                state = State.CtIn;
                if (obj instanceof ClassType) {
                    final ClassType classType = (ClassType) obj;
                    if (!classType.isProcessingRule()) {
                        errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.of(rp)));
                    } else {
                        builder.setClassType(classType);
                    }
                    break;
                }
            case CtIn:
                state = State.End;
                break;
            case End:
                break;
            }
            if (!state.equals(State.End)) {
                objects.remove(0);
            }
        }
        if (!metrics.isEmpty()) {
            builder.setMetrics(metrics);
        }

        if (rp.isReoptimization()
                && builder.getBandwidth() != null
                && !builder.getReportedRoute().getBandwidth().getBandwidth().equals(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth(new byte[] { 0 }))
                && builder.getReportedRoute().getRro() == null) {
            errors.add(createErrorMsg(PCEPErrors.RRO_MISSING, Optional.of(rp)));
            return null;
        }
        return new SegmentComputationBuilder().setP2p(builder.build()).build();
    }

    private enum State {
        Init, LspIn, ReportedIn, LoadBIn, LspaIn, BandwidthIn, MetricIn, IroIn, RroIn, XroIn, OfIn, CtIn, End
    }
}
