/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.message;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.gc.object.Gc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.load.balancing.object.LoadBalancing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcc.id.req.object.PccIdReq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pce.id.object.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.MonitoringRequest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.MonitoringRequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.SvecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.monitoring.request.PceIdList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.monitoring.request.PceIdListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.PathKeyExpansionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.SegmentComputation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.SegmentComputationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.P2p;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.P2pBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.ReportedRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.requests.segment.computation.p2p.ReportedRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.objects.VendorInformationObject;

/**
 * Parser for {@link Pcreq}
 */
public class PCEPRequestMessageParser extends AbstractMessageParser {

    public static final int TYPE = 3;

    public PCEPRequestMessageParser(final ObjectRegistry registry, final VendorInformationObjectRegistry viRegistry) {
        super(registry, viRegistry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof Pcreq, "Wrong instance of Message. Passed instance of %s. Need Pcreq.", message.getClass());
        final PcreqMessage msg = ((Pcreq) message).getPcreqMessage();
        Preconditions.checkArgument(msg.getRequests() != null && !msg.getRequests().isEmpty(), "Requests cannot be null or empty.");
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
        for (final Requests req : msg.getRequests()) {
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
            }
        }
    }

    protected void serializeSvec(final PcreqMessage msg, final ByteBuf buffer) {
        for (final Svec s : msg.getSvec()) {
            serializeObject(s.getSvec(), buffer);
            serializeObject(s.getOf(), buffer);
            serializeObject(s.getGc(), buffer);
            serializeObject(s.getXro(), buffer);
            if (s.getMetric() != null) {
                for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.Metric m : s.getMetric()) {
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
                serializeObject(rr.getBandwidth(), buffer);
            }
        }
        serializeObject(p2p.getLoadBalancing(), buffer);
        serializeObject(p2p.getLspa(), buffer);
        serializeObject(p2p.getBandwidth(), buffer);
        if (p2p.getMetrics() != null) {
            for (final Metrics m : p2p.getMetrics()) {
                serializeObject(m.getMetric(), buffer);
            }
        }
        serializeObject(p2p.getIro(), buffer);
        serializeObject(p2p.getRro(), buffer);
        serializeObject(p2p.getXro(), buffer);
        serializeObject(p2p.getOf(), buffer);
        serializeObject(p2p.getClassType(), buffer);
    }

    protected void serializeMonitoringRequest(final MonitoringRequest monReq, final ByteBuf out) {
        serializeObject(monReq.getMonitoring(), out);
        serializeObject(monReq.getPccIdReq(), out);
        if (monReq.getPceIdList() != null) {
            for (final PceIdList pceId : monReq.getPceIdList()) {
                serializeObject(pceId.getPceId(), out);
            }
        }
    }

    @Override
    protected Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        Preconditions.checkArgument(objects != null, "Passed list can't be null.");
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Pcrep message cannot be empty.");
        }
        final PcreqMessageBuilder mBuilder = new PcreqMessageBuilder();
        mBuilder.setMonitoringRequest(getMonitoring(objects));
        final List<Svec> svecs = getSvecs(objects, errors);
        if (!svecs.isEmpty()) {
            mBuilder.setSvec(svecs);
        }
        final List<Requests> requests = getRequests(objects, errors);
        if (requests != null) {
            mBuilder.setRequests(requests);
        } else {
            errors.add(createErrorMsg(PCEPErrors.RP_MISSING, Optional.<Rp>absent()));
        }
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        return new PcreqBuilder().setPcreqMessage(mBuilder.build()).build();
    }

    protected List<Svec> getSvecs(final List<Object> objects, final List<Message> errors) {
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

    protected List<Requests> getRequests(final List<Object> objects, final List<Message> errors) {
        final List<Requests> requests = new ArrayList<>();
        while (!objects.isEmpty()) {
            final RequestsBuilder rBuilder = new RequestsBuilder();
            Rp rpObj = null;
            if (!(objects.get(0) instanceof Rp)) {
                // if RP obj is missing return error only
                errors.add(createErrorMsg(PCEPErrors.RP_MISSING, Optional.<Rp>absent()));
                return null;
            }
            rpObj = (Rp) objects.get(0);
            objects.remove(0);
            if (!rpObj.isProcessingRule()) {
                errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.<Rp>absent()));
            } else {
                rBuilder.setRp(rpObj);
            }
            final List<VendorInformationObject> vendorInfo = addVendorInformationObjects(objects);
            if (!vendorInfo.isEmpty()) {
                rBuilder.setVendorInformationObject(vendorInfo);
            }
            // expansion
            if (rpObj.isPathKey() && objects.get(0) instanceof PathKey) {
                rBuilder.setPathKeyExpansion(new PathKeyExpansionBuilder().setPathKey((PathKey) objects.get(0)).build());
            }

            final P2pBuilder p2pBuilder = new P2pBuilder();

            if (!objects.isEmpty() && objects.get(0) instanceof EndpointsObj) {
                final EndpointsObj ep = (EndpointsObj) objects.get(0);
                objects.remove(0);
                if (!ep.isProcessingRule()) {
                    errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.of(rpObj)));
                } else {
                    p2pBuilder.setEndpointsObj(ep);
                }
            } else {
                errors.add(createErrorMsg(PCEPErrors.END_POINTS_MISSING, Optional.of(rpObj)));
                return null;
            }
            // p2p
            if (!rpObj.isP2mp()) {
                final SegmentComputation segm = getSegmentComputation(p2pBuilder, objects, errors, rpObj);
                if (segm != null) {
                    rBuilder.setSegmentComputation(segm);
                }
            }
            requests.add(rBuilder.build());
        }
        return requests;
    }

    protected SegmentComputation getSegmentComputation(final P2pBuilder builder, final List<Object> objects, final List<Message> errors,
            final Rp rp) {
        final List<Metrics> metrics = new ArrayList<>();
        final List<VendorInformationObject> viObjects = new ArrayList<>();

        State state = State.INIT;
        while (!objects.isEmpty() && state != State.END) {
            state = insertObject(state, objects, viObjects, builder, metrics, errors, rp);
            if (!state.equals(State.END)) {
                objects.remove(0);
            }
        }
        if (!metrics.isEmpty()) {
            builder.setMetrics(metrics);
        }
        if (!viObjects.isEmpty()) {
            builder.setVendorInformationObject(viObjects);
        }

        if (rp.isReoptimization()
                && builder.getBandwidth() != null
                && !builder.getReportedRoute().getBandwidth().getBandwidth().equals(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth(new byte[] { 0 }))
                && builder.getReportedRoute().getRro() == null) {
            errors.add(createErrorMsg(PCEPErrors.RRO_MISSING, Optional.of(rp)));
            return null;
        }
        return new SegmentComputationBuilder().setP2p(builder.build()).build();
    }

    private State insertObject(final State state, final List<Object> objects, final List<VendorInformationObject> viObjects, final P2pBuilder builder, final List<Metrics> metrics, final List<Message> errors, final Rp rp) {
        final Object obj = objects.get(0);
        switch (state) {
        case INIT:
            if (obj instanceof Rro) {
                final ReportedRouteBuilder rrBuilder = new ReportedRouteBuilder();
                rrBuilder.setRro((Rro) obj);
                objects.remove(0);
                final Object nextObj = objects.get(0);
                if (nextObj instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reoptimization.bandwidth.object.Bandwidth) {
                    rrBuilder.setBandwidth((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reoptimization.bandwidth.object.Bandwidth) nextObj);
                }
                return State.REPORTED_IN;
            }
        case REPORTED_IN:
            if (obj instanceof VendorInformationObject) {
                viObjects.add((VendorInformationObject) obj);
                return State.REPORTED_IN;
            }
        case VENDOR_INFO_LIST:
            if (obj instanceof LoadBalancing) {
                builder.setLoadBalancing((LoadBalancing) obj);
                return State.LOAD_BIN;
            }
        case LOAD_BIN:
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
            if (obj instanceof Rro) {
                builder.setRro((Rro) obj);
                return State.RRO_IN;
            }
        case RRO_IN:
            if (obj instanceof Xro) {
                builder.setXro((Xro) obj);
                return State.XRO_IN;
            }
        case XRO_IN:
            if (obj instanceof Of) {
                builder.setOf((Of) obj);
                return State.OF_IN;
            }
        case OF_IN:
            if (obj instanceof ClassType) {
                final ClassType classType = (ClassType) obj;
                if (!classType.isProcessingRule()) {
                    errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.of(rp)));
                } else {
                    builder.setClassType(classType);
                }
                return State.CT_IN;
            }
        case CT_IN:
        case END:
            return State.END;
        default:
            return state;
        }
    }

    private enum State {
        INIT, REPORTED_IN, VENDOR_INFO_LIST, LOAD_BIN, LSPA_IN, BANDWIDTH_IN, METRIC_IN, IRO_IN, RRO_IN, XRO_IN, OF_IN, CT_IN, END
    }

    private Svec getValidSvec(final SvecBuilder builder, final List<Object> objects) {
        Preconditions.checkArgument(objects != null && !objects.isEmpty(), "Passed list can't be null or empty.");

        if (objects.get(0) instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec) {
            builder.setSvec((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec) objects.get(0));
            objects.remove(0);
        } else {
            return null;
        }

        final List<Metrics> metrics = new ArrayList<>();
        final List<VendorInformationObject> viObjects = new ArrayList<>();

        Object obj = null;
        SvecState state = SvecState.INIT;
        while (!objects.isEmpty() && !state.equals(SvecState.END)) {
            obj = objects.get(0);
            state = insertObject(state, obj, builder, metrics, viObjects);
            if (!state.equals(SvecState.END)) {
                objects.remove(0);
            }
        }
        if (!viObjects.isEmpty()) {
            builder.setVendorInformationObject(viObjects);
        }
        return builder.build();
    }

    private SvecState insertObject(final SvecState state, final Object obj, final SvecBuilder builder, final List<Metrics> metrics, final List<VendorInformationObject> viObjects) {
        switch (state) {
        case INIT:
            if (obj instanceof Of) {
                builder.setOf((Of) obj);
                return SvecState.OF_IN;
            }
        case OF_IN:
            if (obj instanceof Gc) {
                builder.setGc((Gc) obj);
                return SvecState.GC_IN;
            }
        case GC_IN:
            if (obj instanceof Xro) {
                builder.setXro((Xro) obj);
                return SvecState.XRO_IN;
            }
        case XRO_IN:
            if (obj instanceof Metric) {
                metrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                return SvecState.XRO_IN;
            }
        case METRIC_IN:
            if (obj instanceof VendorInformationObject) {
                viObjects.add((VendorInformationObject) obj);
                return SvecState.METRIC_IN;
            }
        case VENDOR_INFO:
        case END:
            return SvecState.END;
        default:
            return state;
        }
    }

    private enum SvecState {
        INIT, OF_IN, GC_IN, XRO_IN, METRIC_IN, VENDOR_INFO, END
    }

    protected MonitoringRequest getMonitoring(final List<Object> objects) {
        final MonitoringRequestBuilder builder = new MonitoringRequestBuilder();
        if (!objects.isEmpty() && objects.get(0) instanceof Monitoring) {
            builder.setMonitoring((Monitoring) objects.get(0));
            objects.remove(0);
        } else {
            return null;
        }
        if (!objects.isEmpty() && objects.get(0) instanceof PccIdReq) {
            builder.setPccIdReq((PccIdReq) objects.get(0));
            objects.remove(0);
        }
        final List<PceIdList> pceIdList = new ArrayList<>();
        while(!objects.isEmpty() && objects.get(0) instanceof PceId) {
            pceIdList.add(new PceIdListBuilder().setPceId((PceId) objects.get(0)).build());
            objects.remove(0);
        }
        if (!pceIdList.isEmpty()) {
            builder.setPceIdList(pceIdList);
        }
        return builder.build();
    }
}
