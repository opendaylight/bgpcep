/*
 * Copyright (c) 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.opendaylight.protocol.pcep.parser.message.PCEPRequestMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.SegmentComputation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.SegmentComputationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.segment.computation.P2p;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.segment.computation.P2pBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.load.balancing.object.LoadBalancing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2p.ReportedRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reoptimization.bandwidth.object.ReoptimizationBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.vendor.information.objects.VendorInformationObject;


/**
 * Parser for {@link Pcreq}.
 */
public class StatefulPCRequestMessageParser extends PCEPRequestMessageParser {

    public static final int TYPE = 3;

    public StatefulPCRequestMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    protected void serializeP2P(final ByteBuf buffer, final P2p p2p) {
        serializeObject(p2p.getEndpointsObj(), buffer);
        serializeVendorInformationObjects(p2p.getVendorInformationObject(), buffer);
        if (p2p.getReportedRoute() != null) {
            final ReportedRoute rr = p2p.getReportedRoute();
            if (rr != null) {
                serializeObject(rr.getRro(), buffer);
                serializeObject(rr.getReoptimizationBandwidth(), buffer);
            }
        }
        serializeObject(p2p.getLoadBalancing(), buffer);
        serializeObject(p2p.getLsp(), buffer);
        serializeObject(p2p.getLspa(), buffer);
        serializeObject(p2p.getBandwidth(), buffer);
        for (final Metrics m : p2p.nonnullMetrics()) {
            serializeObject(m.getMetric(), buffer);
        }
        serializeObject(p2p.getIro(), buffer);
        serializeObject(p2p.getRro(), buffer);
        serializeObject(p2p.getXro(), buffer);
        serializeObject(p2p.getOf(), buffer);
        serializeObject(p2p.getClassType(), buffer);
    }


    @Override
    protected SegmentComputation getP2PSegmentComputation(final P2pBuilder builder,
                                                          final Queue<Object> objects,
                                                          final List<Message> errors,
                                                          final Rp rp) {
        final List<Metrics> metrics = new ArrayList<>();
        final List<VendorInformationObject> viObjects = new ArrayList<>();

        P2PState p2PState = P2PState.INIT;
        while (!objects.isEmpty() && p2PState != P2PState.END) {
            p2PState = insertP2PObject(p2PState, objects, viObjects, builder, metrics, errors, rp);
            if (!p2PState.equals(P2PState.END)) {
                objects.remove();
            }
        }
        if (!metrics.isEmpty()) {
            builder.setMetrics(metrics);
        }
        if (!viObjects.isEmpty()) {
            builder.setVendorInformationObject(viObjects);
        }

        if (rp.getReoptimization() && builder.getBandwidth() != null
                && !builder.getReportedRoute().getReoptimizationBandwidth().getBandwidth().equals(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network
                            .concepts.rev131125.Bandwidth(new byte[] { 0 }))
                && builder.getReportedRoute().getRro() == null) {
            errors.add(createErrorMsg(PCEPErrors.RRO_MISSING, Optional.of(rp)));
            return null;
        }
        return new SegmentComputationBuilder().setP2p(builder.build()).build();
    }

    // Note: objects is expected to be non-empty and caller will remove the first object if non-empty
    @SuppressWarnings("fallthrough")
    private static P2PState insertP2PObject(final P2PState p2PState,
                                            final Queue<Object> objects,
                                            final List<VendorInformationObject> viObjects,
                                            final P2pBuilder builder,
                                            final List<Metrics> metrics,
                                            final List<Message> errors,
                                            final Rp rp) {
        final Object obj = objects.element();
        switch (p2PState) {
            case INIT:
                if (obj instanceof Rro) {
                    builder.setRro((Rro) obj);
                    objects.remove();

                    // FIXME: should we guard against empty objects?
                    final Object nextObj = objects.element();
                    if (nextObj instanceof ReoptimizationBandwidth) {
                        builder.setReoptimizationBandwidth((ReoptimizationBandwidth) nextObj);
                    }
                    return P2PState.REPORTED_IN;
                }
                // fallthrough
            case REPORTED_IN:
                if (obj instanceof VendorInformationObject) {
                    viObjects.add((VendorInformationObject) obj);
                    return P2PState.REPORTED_IN;
                }
                // fallthrough
            case VENDOR_INFO_LIST:
                if (obj instanceof LoadBalancing) {
                    builder.setLoadBalancing((LoadBalancing) obj);
                    return P2PState.LOAD_BIN;
                }
                // fallthrough
            case LOAD_BIN:
                if (obj instanceof Lsp) {
                    builder.setLsp((Lsp) obj);
                    return P2PState.LSP_IN;
                }
                // fallthrough
            case LSP_IN:
                if (obj instanceof Lspa) {
                    builder.setLspa((Lspa) obj);
                    return P2PState.LSPA_IN;
                }
                // fallthrough
            case LSPA_IN:
                if (obj instanceof Bandwidth) {
                    builder.setBandwidth((Bandwidth) obj);
                    return P2PState.BANDWIDTH_IN;
                }
                // fallthrough
            case BANDWIDTH_IN:
                if (obj instanceof Metric) {
                    metrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                    return P2PState.BANDWIDTH_IN;
                }
                // fallthrough
            case METRIC_IN:
                if (obj instanceof Iro) {
                    builder.setIro((Iro) obj);
                    return P2PState.IRO_IN;
                }
                // fallthrough
            case IRO_IN:
                if (obj instanceof Rro) {
                    builder.setRro((Rro) obj);
                    return P2PState.RRO_IN;
                }
                // fallthrough
            case RRO_IN:
                if (obj instanceof Xro) {
                    builder.setXro((Xro) obj);
                    return P2PState.XRO_IN;
                }
                // fallthrough
            case XRO_IN:
                if (obj instanceof Of) {
                    builder.setOf((Of) obj);
                    return P2PState.OF_IN;
                }
                // fallthrough
            case OF_IN:
                if (obj instanceof ClassType) {
                    final ClassType classType = (ClassType) obj;
                    if (!classType.getProcessingRule()) {
                        errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.of(rp)));
                    } else {
                        builder.setClassType(classType);
                    }
                    return P2PState.CT_IN;
                }
                // fallthrough
            case CT_IN:
            case END:
                return P2PState.END;
            default:
                return p2PState;
        }
    }

    private enum P2PState {
        INIT,
        REPORTED_IN,
        VENDOR_INFO_LIST,
        LOAD_BIN,
        LSP_IN,
        LSPA_IN,
        BANDWIDTH_IN,
        METRIC_IN,
        IRO_IN,
        RRO_IN,
        XRO_IN,
        OF_IN,
        CT_IN,
        END
    }
}
