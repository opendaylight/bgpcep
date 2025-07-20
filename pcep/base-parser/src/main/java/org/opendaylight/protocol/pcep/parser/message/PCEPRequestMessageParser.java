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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.PcreqMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.PcreqMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.MonitoringRequest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.MonitoringRequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.SvecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.monitoring.request.PceIdList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.monitoring.request.PceIdListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.PathKeyExpansionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.SegmentComputation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.SegmentComputationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.segment.computation.P2mp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.segment.computation.P2mpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.segment.computation.P2p;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcreq.message.pcreq.message.requests.segment.computation.P2pBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bnc.object.BranchNodeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bnc.object.branch.node.type.BranchNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bnc.object.branch.node.type.BranchNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bnc.object.branch.node.type.NonBranchNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bnc.object.branch.node.type.NonBranchNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.branch.node.object.BranchNodeList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.gc.object.Gc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.load.balancing.object.LoadBalancing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.non.branch.node.object.NonBranchNodeList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.EndpointRroPair;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.EndpointRroPairBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.endpoint.rro.pair.Rros;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.endpoint.rro.pair.RrosBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.endpoint.rro.pair.rros.route.object.ReportedRouteObjectCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.endpoint.rro.pair.rros.route.object.ReportedRouteObjectCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.endpoint.rro.pair.rros.route.object.SecondaryReportedRouteObjectCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.endpoint.rro.pair.rros.route.object.SecondaryReportedRouteObjectCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.iro.bnc.choice.BncCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.iro.bnc.choice.BncCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.iro.bnc.choice.IroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.iro.bnc.choice.IroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2p.ReportedRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.path.key.object.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcc.id.req.object.PccIdReq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pce.id.object.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reoptimization.bandwidth.object.ReoptimizationBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.secondary.reported.route.object.Srro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.vendor.information.objects.VendorInformationObject;

/**
 * Parser for {@link Pcreq}.
 */
public class PCEPRequestMessageParser extends AbstractMessageParser {

    public static final int TYPE = 3;

    public PCEPRequestMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        checkArgument(message instanceof Pcreq,
                "Wrong instance of Message. Passed instance of %s. Need Pcreq.", message.getClass());
        final PcreqMessage msg = ((Pcreq) message).getPcreqMessage();
        checkArgument(!msg.nonnullRequests().isEmpty(), "Requests cannot be null or empty.");
        final ByteBuf buffer = Unpooled.buffer();
        if (msg.getMonitoringRequest() != null) {
            serializeMonitoringRequest(msg.getMonitoringRequest(), buffer);
        }
        if (msg.getSvec() != null) {
            serializeSvec(msg, buffer);
        }
        serializeRequest(msg, buffer);
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    protected void serializeRequest(final PcreqMessage msg, final ByteBuf buffer) {
        for (final Requests req : msg.nonnullRequests()) {
            serializeObject(req.getRp(), buffer);
            serializeVendorInformationObjects(req.getVendorInformationObject(), buffer);
            if (req.getPathKeyExpansion() != null) {
                serializeObject(req.getPathKeyExpansion().getPathKey(), buffer);
            }
            if (req.getSegmentComputation() != null) {
                final SegmentComputation sc = req.getSegmentComputation();
                if (sc.getP2p() != null) {
                    serializeP2P(buffer, sc.getP2p());
                }
                if (sc.getP2mp() != null) {
                    serializeP2MP(buffer, sc.getP2mp());
                }
            }
        }
    }

    protected void serializeSvec(final PcreqMessage msg, final ByteBuf buffer) {
        for (final Svec s : msg.nonnullSvec()) {
            serializeObject(s.getSvec(), buffer);
            serializeObject(s.getOf(), buffer);
            serializeObject(s.getGc(), buffer);
            serializeObject(s.getXro(), buffer);
            if (s.getMetric() != null) {
                for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.svec
                        .Metric m : s.nonnullMetric()) {
                    serializeObject(m.getMetric(), buffer);
                }
            }
            serializeVendorInformationObjects(s.getVendorInformationObject(), buffer);
        }
    }

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

    protected void serializeP2MP(final ByteBuf buffer, final P2mp p2mp) {
        final List<EndpointRroPair> endpointRroPairList = p2mp.getEndpointRroPair();
        if (endpointRroPairList == null || endpointRroPairList.isEmpty()) {
            throw new IllegalStateException("At least one instance of Endpoint must be present in P2MP Request!");
        }
        endpointRroPairList.forEach(pair -> {
            serializeObject(pair.getEndpointsObj(), buffer);
            pair.nonnullRros().forEach(rro -> {
                if (rro.getRouteObject() instanceof ReportedRouteObjectCase) {
                    serializeObject(((ReportedRouteObjectCase) rro.getRouteObject()).getRro(), buffer);
                } else if (rro.getRouteObject() instanceof SecondaryReportedRouteObjectCase) {
                    serializeObject(((SecondaryReportedRouteObjectCase) rro.getRouteObject()).getSrro(), buffer);
                }
            });
            serializeObject(pair.getReoptimizationBandwidth(), buffer);
        });
        serializeObject(p2mp.getOf(), buffer);
        serializeObject(p2mp.getLspa(), buffer);
        serializeObject(p2mp.getBandwidth(), buffer);
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.Metrics
                m : p2mp.nonnullMetrics()) {
            serializeObject(m.getMetric(), buffer);
        }
        if (p2mp.getIroBncChoice() instanceof IroCase) {
            serializeObject(((IroCase) p2mp.getIroBncChoice()).getIro(), buffer);
        } else if (p2mp.getIroBncChoice() instanceof BncCase) {
            final BranchNodeType branchNodeType = ((BncCase) p2mp.getIroBncChoice()).getBranchNodeType();
            if (branchNodeType instanceof BranchNodeCase) {
                serializeObject(((BranchNodeCase) branchNodeType).getBranchNodeList(), buffer);
            } else if (branchNodeType instanceof NonBranchNodeCase) {
                serializeObject(((NonBranchNodeCase) branchNodeType).getNonBranchNodeList(), buffer);
            }
        }
        serializeObject(p2mp.getLoadBalancing(), buffer);
    }

    protected void serializeMonitoringRequest(final MonitoringRequest monReq, final ByteBuf out) {
        serializeObject(monReq.getMonitoring(), out);
        serializeObject(monReq.getPccIdReq(), out);
        for (final PceIdList pceId : monReq.nonnullPceIdList()) {
            serializeObject(pceId.getPceId(), out);
        }
    }

    @Override
    protected Message validate(final Queue<Object> objects, final List<Message> errors)
            throws PCEPDeserializerException {
        checkArgument(objects != null, "Passed list can't be null.");
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Pcrep message cannot be empty.");
        }
        final PcreqMessageBuilder mBuilder = new PcreqMessageBuilder().setMonitoringRequest(getMonitoring(objects));
        final List<Svec> svecs = getSvecs(objects);
        if (!svecs.isEmpty()) {
            mBuilder.setSvec(svecs);
        }
        final List<Requests> requests = getRequests(objects, errors);
        if (requests != null) {
            mBuilder.setRequests(requests);
        } else {
            errors.add(createErrorMsg(PCEPErrors.RP_MISSING, Optional.empty()));
        }
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        return new PcreqBuilder().setPcreqMessage(mBuilder.build()).build();
    }

    protected List<Svec> getSvecs(final Queue<Object> objects) {
        final List<Svec> svecList = new ArrayList<>();
        while (!objects.isEmpty()) {
            final SvecBuilder sBuilder = new SvecBuilder();
            final Svec svecComp = getValidSvec(sBuilder, objects);
            if (svecComp == null) {
                break;
            }
            svecList.add(svecComp);
        }
        return svecList;
    }

    protected List<Requests> getRequests(final Queue<Object> objects, final List<Message> errors) {
        final List<Requests> requests = new ArrayList<>();

        for (Object obj = objects.peek(); obj != null; obj = objects.peek()) {
            if (!(obj instanceof Rp)) {
                // if RP obj is missing return error only
                errors.add(createErrorMsg(PCEPErrors.RP_MISSING, Optional.empty()));
                return null;
            }

            final RequestsBuilder rBuilder = new RequestsBuilder();
            final Rp rpObj = (Rp) obj;
            objects.remove();

            if (rpObj.getProcessingRule()) {
                rBuilder.setRp(rpObj);
            } else {
                errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.empty()));
            }

            final List<VendorInformationObject> vendorInfo = addVendorInformationObjects(objects);
            if (!vendorInfo.isEmpty()) {
                rBuilder.setVendorInformationObject(vendorInfo);
            }

            // expansion
            if (rpObj.getPathKey()) {
                // FIXME: this can fail on malformed messages (i.e. objects.isEmpty()), add an explicit check/error
                obj = objects.element();
                if (obj instanceof PathKey) {
                    // FIXME: shouldn't we be also removing the object?
                    rBuilder.setPathKeyExpansion(new PathKeyExpansionBuilder().setPathKey((PathKey) obj).build());
                }
            }

            obj = objects.peek();
            if (!(obj instanceof EndpointsObj)) {
                errors.add(createErrorMsg(PCEPErrors.END_POINTS_MISSING, Optional.of(rpObj)));
                return null;
            }

            if (!rpObj.getP2mp()) {
                // p2p
                // FIXME: explicit check for empty/type?
                final EndpointsObj ep = (EndpointsObj) objects.remove();

                final P2pBuilder p2pBuilder = new P2pBuilder();
                if (!ep.getProcessingRule()) {
                    errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.of(rpObj)));
                } else {
                    p2pBuilder.setEndpointsObj(ep);
                }

                final SegmentComputation segm = getP2PSegmentComputation(p2pBuilder, objects, errors, rpObj);
                if (segm != null) {
                    rBuilder.setSegmentComputation(segm);
                }
            } else {
                // p2mp
                final SegmentComputation segm = getP2MPSegmentComputation(objects, errors, rpObj);
                if (segm != null) {
                    rBuilder.setSegmentComputation(segm);
                }
            }
            requests.add(rBuilder.build());
        }
        return requests;
    }

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
                if (obj instanceof ClassType classType) {
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

    private static SvecState insertP2PObject(final SvecState state, final Object obj, final SvecBuilder builder,
            final List<Metrics> metrics, final List<VendorInformationObject> viObjects) {
        switch (state) {
            case INIT:
                if (obj instanceof Of) {
                    builder.setOf((Of) obj);
                    return SvecState.OF_IN;
                }
                // fallthrough
            case OF_IN:
                if (obj instanceof Gc) {
                    builder.setGc((Gc) obj);
                    return SvecState.GC_IN;
                }
                // fallthrough
            case GC_IN:
                if (obj instanceof Xro) {
                    builder.setXro((Xro) obj);
                    return SvecState.XRO_IN;
                }
                // fallthrough
            case XRO_IN:
                if (obj instanceof Metric) {
                    metrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                    return SvecState.XRO_IN;
                }
                // fallthrough
            case METRIC_IN:
                if (obj instanceof VendorInformationObject) {
                    viObjects.add((VendorInformationObject) obj);
                    return SvecState.METRIC_IN;
                }
                // fallthrough
            case VENDOR_INFO:
            case END:
                return SvecState.END;
            default:
                return state;
        }
    }

    private enum P2PState {
        INIT,
        REPORTED_IN,
        VENDOR_INFO_LIST,
        LOAD_BIN, LSPA_IN,
        BANDWIDTH_IN,
        METRIC_IN,
        IRO_IN,
        RRO_IN,
        XRO_IN,
        OF_IN,
        CT_IN,
        END
    }

    protected SegmentComputation getP2MPSegmentComputation(final Queue<Object> objects, final List<Message> errors,
            final Rp rp) {
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp.Metrics>
            metrics = new ArrayList<>();
        final P2mpBuilder builder = new P2mpBuilder();
        final List<EndpointRroPair> epRros = new ArrayList<>();

        P2MPState state = P2MPState.RP;
        while (!objects.isEmpty() && state != P2MPState.END) {
            state = insertP2MPObject(state, objects, builder, epRros, metrics, errors, rp);
            if (!state.equals(P2MPState.END)) {
                objects.remove();
            }
        }
        if (!epRros.isEmpty()) {
            builder.setEndpointRroPair(epRros);
        }
        if (!metrics.isEmpty()) {
            builder.setMetrics(metrics);
        }

        if (rp.getReoptimization() && builder.getBandwidth() != null) {
            if (!isValidReoptimizationRro(epRros) || !isValidReoptimizationBandwidth(epRros)) {
                errors.add(createErrorMsg(PCEPErrors.RRO_MISSING, Optional.of(rp)));
            }
        }

        return new SegmentComputationBuilder().setP2mp(builder.build()).build();
    }

    private static boolean isValidReoptimizationRro(final List<EndpointRroPair> epRros) {
        for (EndpointRroPair epRro : epRros) {
            if (epRro.nonnullRros().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidReoptimizationBandwidth(final List<EndpointRroPair> epRros) {
        for (EndpointRroPair epRro : epRros) {
            if (epRro.getReoptimizationBandwidth() == null) {
                return false;
            }
        }
        return true;
    }

    private static P2MPState insertP2MPObject(final P2MPState p2MPState, final Queue<Object> objects,
            final P2mpBuilder builder, final List<EndpointRroPair> epRros,
            final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.p2mp
                .Metrics> metrics,
            final List<Message> errors, final Rp rp) {
        final Object obj = objects.element();
        switch (p2MPState) {
            case RP:
                if (obj instanceof EndpointsObj) {
                    final EndpointRroPairBuilder rroPairBuilder = new EndpointRroPairBuilder();
                    if (obj.getProcessingRule()) {
                        rroPairBuilder.setEndpointsObj((EndpointsObj) obj);
                    } else {
                        errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.of(rp)));
                    }
                    epRros.add(rroPairBuilder.setRros(new ArrayList<>()).build());
                    return P2MPState.ENDPOINT;
                }
                // fallthrough
            case ENDPOINT:
                if (obj instanceof Rro || obj instanceof Srro) {
                    if (obj.getProcessingRule()) {
                        final int lastIndex = epRros.size() - 1;
                        final EndpointRroPair endpointRroPair = epRros.get(lastIndex);
                        List<Rros> rros = endpointRroPair.getRros();
                        if (rros == null) {
                            rros = new ArrayList<>();
                        }
                        if (obj instanceof Rro) {
                            rros.add(new RrosBuilder()
                                    .setRouteObject(new ReportedRouteObjectCaseBuilder().setRro((Rro) obj)
                                            .build()).build());
                        } else {
                            rros.add(new RrosBuilder()
                                    .setRouteObject(new SecondaryReportedRouteObjectCaseBuilder().setSrro((Srro) obj)
                                            .build()).build());
                        }
                        epRros.remove(lastIndex);
                        epRros.add(lastIndex, new EndpointRroPairBuilder(endpointRroPair).setRros(rros).build());
                    }
                    return P2MPState.ENDPOINT;
                }
                // fallthrough
            case RRO_SRRO:
                if (obj instanceof ReoptimizationBandwidth) {
                    final int lastIndex = epRros.size() - 1;
                    final EndpointRroPair endpointRroPair = epRros.get(lastIndex);
                    epRros.remove(lastIndex);
                    epRros.add(lastIndex, new EndpointRroPairBuilder(endpointRroPair)
                            .setReoptimizationBandwidth((ReoptimizationBandwidth) obj)
                            .build());
                    return P2MPState.BANDWIDTH;
                }
                // fallthrough
            case BANDWIDTH:
                if (obj instanceof EndpointsObj) {
                    return P2MPState.RP;
                }
                if (obj instanceof Of) {
                    builder.setOf((Of) obj);
                    return P2MPState.OF_IN;
                }
                // fallthrough
            case OF_IN:
                if (obj instanceof Lspa) {
                    builder.setLspa((Lspa) obj);
                    return P2MPState.LSPA_IN;
                }
                // fallthrough
            case LSPA_IN:
                if (obj instanceof Bandwidth) {
                    builder.setBandwidth((Bandwidth) obj);
                    return P2MPState.BANDWIDTH_IN;
                }
                // fallthrough
            case BANDWIDTH_IN:
                if (obj instanceof Metric) {
                    metrics.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object
                        .rev250930.p2mp.MetricsBuilder().setMetric((Metric) obj).build());
                    return P2MPState.BANDWIDTH_IN;
                }
                // fallthrough
            case METRIC_IN:
                if (obj instanceof Iro) {
                    builder.setIroBncChoice(new IroCaseBuilder().setIro((Iro) obj).build());
                    return P2MPState.IRO_BNC_IN;
                }
                if (obj instanceof BranchNodeList) {
                    builder.setIroBncChoice(new BncCaseBuilder().setBranchNodeType(new BranchNodeCaseBuilder()
                            .setBranchNodeList((BranchNodeList) obj).build()).build());
                    return P2MPState.IRO_BNC_IN;
                }
                if (obj instanceof NonBranchNodeList) {
                    builder.setIroBncChoice(new BncCaseBuilder().setBranchNodeType(new NonBranchNodeCaseBuilder()
                            .setNonBranchNodeList((NonBranchNodeList) obj).build()).build());
                    return P2MPState.IRO_BNC_IN;
                }
                // fallthrough
            case IRO_BNC_IN:
                if (obj instanceof LoadBalancing) {
                    builder.setLoadBalancing((LoadBalancing) obj);
                    return P2MPState.LOAD_BIN;
                }
                // fallthrough
            case LOAD_BIN:
            case END:
            default:
                return P2MPState.END;
        }
    }

    private enum P2MPState {
        RP, ENDPOINT, RRO_SRRO, BANDWIDTH, OF_IN, LSPA_IN, BANDWIDTH_IN, METRIC_IN, IRO_BNC_IN, LOAD_BIN, END
    }

    // Note: objects is expected to be non-empty
    private static Svec getValidSvec(final SvecBuilder builder, final Queue<Object> objects) {
        final Object svec = objects.element();
        if (svec instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.svec
                .object.Svec) {
            builder.setSvec((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.svec
                .object.Svec) svec);
            objects.remove();
        } else {
            return null;
        }

        // FIXME: this list is not retained anywhere
        final List<Metrics> metrics = new ArrayList<>();
        final List<VendorInformationObject> viObjects = new ArrayList<>();

        SvecState state = SvecState.INIT;
        for (Object obj = objects.peek(); obj != null; obj = objects.peek()) {
            state = insertP2PObject(state, obj, builder, metrics, viObjects);
            if (state == SvecState.END) {
                break;
            }

            objects.remove();
        }

        if (!viObjects.isEmpty()) {
            builder.setVendorInformationObject(viObjects);
        }
        return builder.build();
    }

    private enum SvecState {
        INIT, OF_IN, GC_IN, XRO_IN, METRIC_IN, VENDOR_INFO, END
    }

    protected MonitoringRequest getMonitoring(final Queue<Object> objects) {
        final MonitoringRequestBuilder builder = new MonitoringRequestBuilder();

        Object obj = objects.peek();
        if (obj instanceof Monitoring) {
            builder.setMonitoring((Monitoring) obj);
            objects.remove();
        } else {
            return null;
        }

        obj = objects.peek();
        if (obj instanceof PccIdReq) {
            builder.setPccIdReq((PccIdReq) obj);
            objects.remove();
            obj = objects.peek();
        }

        final List<PceIdList> pceIdList = new ArrayList<>();
        while (obj instanceof PceId) {
            pceIdList.add(new PceIdListBuilder().setPceId((PceId) obj).build());
            objects.remove();
            obj = objects.peek();
        }

        if (!pceIdList.isEmpty()) {
            builder.setPceIdList(pceIdList);
        }
        return builder.build();
    }
}
