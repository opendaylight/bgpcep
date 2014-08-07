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
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.RequestsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.SvecBuilder;
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
        if (msg.getRequests() == null || msg.getRequests().isEmpty()) {
            throw new IllegalArgumentException("Requests cannot be null or empty.");
        }
        final ByteBuf buffer = Unpooled.buffer();
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
        if (msg.getSvec() != null) {
            for (final Svec s : msg.getSvec()) {
                serializeObject(s.getSvec(), buffer);
                if (s.getOf() != null) {
                    serializeObject(s.getOf(), buffer);
                }
                if (s.getGc() != null) {
                    serializeObject(s.getGc(), buffer);
                }
                if (s.getXro() != null) {
                    serializeObject(s.getXro(), buffer);
                }
                if (s.getMetric() != null) {
                    for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.Metric m : s.getMetric()) {
                        serializeObject(m.getMetric(), buffer);
                    }
                }
                serializeVendorInformationObjects(s.getVendorInformationObject(), buffer);
            }
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    protected void serializeP2P(final ByteBuf buffer, final P2p p2p) {
        if (p2p.getEndpointsObj() != null) {
            serializeObject(p2p.getEndpointsObj(), buffer);
        }
        serializeVendorInformationObjects(p2p.getVendorInformationObject(), buffer);
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
    protected Message validate(
            final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object> objects,
            final List<Message> errors) throws PCEPDeserializerException {
        if (objects == null) {
            throw new IllegalArgumentException("Passed list can't be null.");
        }

        final List<Requests> requests = Lists.newArrayList();
        final List<Svec> svecList = Lists.newArrayList();
        while (!objects.isEmpty()) {
            final RequestsBuilder rBuilder = new RequestsBuilder();
            Rp rpObj = null;
            if (objects.get(0) instanceof Rp) {
                rpObj = (Rp) objects.get(0);
                objects.remove(0);
                if (!rpObj.isProcessingRule()) {
                    errors.add(createErrorMsg(PCEPErrors.P_FLAG_NOT_SET, Optional.<Rp>absent()));
                } else {
                    rBuilder.setRp(rpObj);
                }
            } else {
                // if RP obj is missing return error only
                errors.add(createErrorMsg(PCEPErrors.RP_MISSING, Optional.<Rp>absent()));
                return null;
            }
            final List<VendorInformationObject> vendorInfo = addVendorInformationObjects(objects);
            if (!vendorInfo.isEmpty()) {
                rBuilder.setVendorInformationObject(vendorInfo);
            }
            // expansion
            if (rpObj.isPathKey()) {
                if (objects.get(0) instanceof PathKey) {
                    rBuilder.setPathKeyExpansion(new PathKeyExpansionBuilder().setPathKey((PathKey) objects.get(0)).build());
                }
                continue;
            }

            final P2pBuilder p2pBuilder = new P2pBuilder();

            if (objects.get(0) instanceof EndpointsObj) {
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
            while (!objects.isEmpty()) {
                final SvecBuilder sBuilder = new SvecBuilder();
                final Svec svecComp = getValidSvec(sBuilder, objects);
                if (svecComp == null) {
                    break;
                }
                svecList.add(svecComp);
            }
            requests.add(rBuilder.build());
        }

        final PcreqMessageBuilder mBuilder = new PcreqMessageBuilder();
        mBuilder.setRequests(requests);
        if (!svecList.isEmpty()) {
            mBuilder.setSvec(svecList);
        }
        return new PcreqBuilder().setPcreqMessage(mBuilder.build()).build();
    }

    protected SegmentComputation getSegmentComputation(final P2pBuilder builder, final List<Object> objects, final List<Message> errors,
            final Rp rp) {
        final List<Metrics> metrics = Lists.newArrayList();
        final List<VendorInformationObject> viObjects = Lists.newArrayList();

        State state = State.Init;
        while (!objects.isEmpty() && state != State.End) {
            Object obj = objects.get(0);

            switch (state) {
            case Init:
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
                state = State.VendorInfoList;
                if (obj instanceof VendorInformationObject) {
                    viObjects.add((VendorInformationObject) obj);
                    state = State.ReportedIn;
                    break;
                }
            case VendorInfoList:
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
        if (!viObjects.isEmpty()) {
            builder.setVendorInformationObject(viObjects);
        }

        if (rp.isReoptimization()
                && builder.getBandwidth() != null
                && builder.getReportedRoute().getBandwidth().getBandwidth() !=
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth(new byte[] { 0 })
                && builder.getReportedRoute().getRro() == null) {
            errors.add(createErrorMsg(PCEPErrors.RRO_MISSING, Optional.of(rp)));
            return null;
        }
        return new SegmentComputationBuilder().setP2p(builder.build()).build();
    }

    private enum State {
        Init, ReportedIn, VendorInfoList, LoadBIn, LspaIn, BandwidthIn, MetricIn, IroIn, RroIn, XroIn, OfIn, CtIn, End
    }

    private Svec getValidSvec(final SvecBuilder builder, final List<Object> objects) {
        if (objects == null || objects.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty.");
        }

        if (objects.get(0) instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec) {
            builder.setSvec((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec) objects.get(0));
            objects.remove(0);
        } else {
            return null;
        }

        final List<Metrics> metrics = Lists.newArrayList();
        final List<VendorInformationObject> viObjects = Lists.newArrayList();

        Object obj = null;
        SvecState state = SvecState.Init;
        while (!objects.isEmpty() && !state.equals(SvecState.End)) {
            obj = objects.get(0);

            switch (state) {
            case Init:
                state = SvecState.OfIn;
                if (obj instanceof Of) {
                    builder.setOf((Of) obj);
                    break;
                }
            case OfIn:
                state = SvecState.GcIn;
                if (obj instanceof Gc) {
                    builder.setGc((Gc) obj);
                    break;
                }
            case GcIn:
                state = SvecState.XroIn;
                if (obj instanceof Xro) {
                    builder.setXro((Xro) obj);
                    break;
                }
            case XroIn:
                state = SvecState.MetricIn;
                if (obj instanceof Metric) {
                    metrics.add(new MetricsBuilder().setMetric((Metric) obj).build());
                    state = SvecState.XroIn;
                    break;
                }
            case MetricIn:
                state = SvecState.VendorInfo;
                if (obj instanceof VendorInformationObject) {
                    viObjects.add((VendorInformationObject) obj);
                    state = SvecState.MetricIn;
                    break;
                }
            case VendorInfo:
                state = SvecState.End;
                break;
            case End:
                break;
            }
            if (!state.equals(SvecState.End)) {
                objects.remove(0);
            }
        }
        if (!viObjects.isEmpty()) {
            builder.setVendorInformationObject(viObjects);
        }
        return builder.build();
    }

    private enum SvecState {
        Init, OfIn, GcIn, XroIn, MetricIn, VendorInfo, End
    }
}
