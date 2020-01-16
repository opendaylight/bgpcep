/*
 * Copyright (c) 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.server.provider;

import com.google.common.collect.Lists;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.path.descriptions.PathDescription;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.NoPathVectorTlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.FailureCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.SuccessCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.failure._case.NoPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.failure._case.no.path.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.success._case.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.success._case.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.success._case.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.P2p;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.label._case.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.label._case.LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.type1.label._case.Type1LabelBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class MessagesUtil {

    public static final byte NO_PATH = 0x0;
    public static final byte UNKNOWN_SOURCE = 0x1;
    public static final byte UNKNOWN_DESTINATION = 0x0;

    /*
     * See
     * https://www.iana.org/assignments/pcep/pcep.xhtml#metric-object-ni-field
     */
    public static final int IGP_METRIC = 1;
    public static final int TE_METRIC = 2;
    public static final int PATH_DELAY = 12;

    private MessagesUtil() {
        throw new UnsupportedOperationException();
    }

    public static Ero getEro(List<PathDescription> pathDescriptions) {
        /* Prepare ERO */
        final EroBuilder eroBuilder = new EroBuilder();
        eroBuilder.setIgnore(false);
        eroBuilder.setProcessingRule(true);
        final List<Subobject> eroSubs = new ArrayList<>();

        /* Fulfill ERO sublist */
        for (PathDescription path : pathDescriptions) {
            IpPrefix ipPref = null;

            /* Prepare IpPrefix from IPv4 or IPv6 address */
            if (path.getIpv4() != null) {
                final Ipv4Prefix ipv4Pref = new Ipv4Prefix(path.getIpv4().getValue() + "/32");
                ipPref = new IpPrefixBuilder().setIpPrefix(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix(
                                ipv4Pref))
                        .build();
            }
            if (path.getIpv6() != null) {
                final Ipv6Prefix ipv6Pref = new Ipv6Prefix(path.getIpv6().getValue() + "/128");
                ipPref = new IpPrefixBuilder().setIpPrefix(
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix(
                                ipv6Pref))
                        .build();
            }

            /* Prepare MPLS Label */
            LabelCase labelCase = null;
            if (path.getLabel() != null) {
                final Type1LabelCase labelType = new Type1LabelCaseBuilder()
                        .setType1Label(new Type1LabelBuilder().setType1Label(path.getLabel().getValue()).build())
                        .build();
                final Label label = new LabelBuilder().setLabelType(labelType).setUniDirectional(true).build();
                labelCase = new LabelCaseBuilder().setLabel(label).build();
            }

            /* Build corresponding SubObject and add it to the ERO List */
            Subobject sb = null;
            if (ipPref != null) {
                final IpPrefixCase ipPrefCase = new IpPrefixCaseBuilder().setIpPrefix(ipPref).build();
                sb = new SubobjectBuilder().setSubobjectType(ipPrefCase).setLoose(false).build();
            }
            if (labelCase != null) {
                sb = new SubobjectBuilder().setSubobjectType(labelCase).setLoose(false).build();
            }
            if (sb != null) {
                eroSubs.add(sb);
            }
        }
        /* Set ERO sublist */
        eroBuilder.setSubobject(eroSubs);

        return eroBuilder.build();
    }

    private static PathsBuilder buildPath(ConstrainedPath cpath) {
        final PathsBuilder pathBuilder = new PathsBuilder();

        /* Get ERO from Path Description */
        pathBuilder.setEro(getEro(cpath.getPathDescription()));

        /* Fulfill Computed Metrics if available */
        final ArrayList<Metrics> metrics = new ArrayList<Metrics>();
        if (cpath.getMetric() != null) {
            final MetricBuilder metricBuilder = new MetricBuilder().setComputed(true)
                    .setMetricType(Uint8.valueOf(IGP_METRIC)).setValue(new Float32(
                            ByteBuffer.allocate(4).putFloat(Float.valueOf(cpath.getMetric().floatValue())).array()));
            metrics.add(new MetricsBuilder().setMetric(metricBuilder.build()).build());
        }
        if (cpath.getTeMetric() != null) {
            final MetricBuilder metricBuilder = new MetricBuilder().setComputed(true)
                    .setMetricType(Uint8.valueOf(TE_METRIC)).setValue(new Float32(
                            ByteBuffer.allocate(4).putFloat(Float.valueOf(cpath.getTeMetric().floatValue())).array()));
            metrics.add(new MetricsBuilder().setMetric(metricBuilder.build()).build());
        }
        if (cpath.getDelay() != null) {
            final MetricBuilder metricBuilder = new MetricBuilder().setComputed(true)
                    .setMetricType(Uint8.valueOf(PATH_DELAY)).setValue(new Float32(ByteBuffer.allocate(4)
                            .putFloat(Float.valueOf(cpath.getDelay().getValue().floatValue())).array()));
            metrics.add(new MetricsBuilder().setMetric(metricBuilder.build()).build());
        }
        if (!metrics.isEmpty()) {
            pathBuilder.setMetrics(metrics);
        }
        /* Fulfill Bandwidth and ClassType if set */
        if (cpath.getBandwidth() != null) {
            final BandwidthBuilder bwBuilder = new BandwidthBuilder();
            bwBuilder.setBandwidth(new Bandwidth(new Float32(ByteBuffer.allocate(4)
                    .putFloat(Float.valueOf(cpath.getBandwidth().getValue().floatValue())).array())));
            pathBuilder.setBandwidth(bwBuilder.build());
            if (cpath.getClassType() != null) {
                pathBuilder.setClassType(new ClassTypeBuilder().setClassType(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                                .ClassType(cpath.getClassType()))
                        .build());
            }
        }
        return pathBuilder;
    }

    public static Pcrep createPcRepMessage(Rp rp, P2p p2p, ConstrainedPath cpath) {

        /* Prepare Path Object with ERO and Object from the Request */
        final ArrayList<Paths> paths = new ArrayList<Paths>();
        PathsBuilder pathBuilder = buildPath(cpath);

        if (p2p.getLspa() != null) {
            pathBuilder.setLspa(p2p.getLspa());
        }
        if (p2p.getIro() != null) {
            pathBuilder.setIro(p2p.getIro());
        }
        if (p2p.getXro() != null) {
            pathBuilder.setXro(p2p.getXro());
        }
        paths.add(pathBuilder.build());

        /* Prepare Reply with Path Object */
        final RepliesBuilder replyBuilder = new RepliesBuilder()
                .setRp(rp)
                .setResult(new SuccessCaseBuilder().setSuccess(new SuccessBuilder().setPaths(paths).build()).build());

        /* Prepare PcRep Message */
        final PcrepMessageBuilder msgBuilder = new PcrepMessageBuilder()
                .setReplies(Lists.newArrayList(replyBuilder.build()));
        return new PcrepBuilder().setPcrepMessage(msgBuilder.build()).build();
    }

    public static Pcrep createNoPathMessage(Rp rp, byte reason) {

        /* Prepare NoPath Object */
        final Flags flags = new Flags(false, false, false, false, false, false,
                (reason == UNKNOWN_DESTINATION) ? true : false, (reason == UNKNOWN_SOURCE) ? true : false);
        final NoPathVectorBuilder npvBuilder = new NoPathVectorBuilder().setFlags(flags);
        final TlvsBuilder tlvsBuilder = new TlvsBuilder().setNoPathVector(npvBuilder.build());
        final NoPathBuilder npBuilder = new NoPathBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setNatureOfIssue(Uint8.ZERO)
                .setUnsatisfiedConstraints(true)
                .setTlvs(tlvsBuilder.build());

        /* Prepare Reply */
        final RepliesBuilder replyBuilder = new RepliesBuilder()
                .setRp(rp)
                .setResult(new FailureCaseBuilder().setNoPath(npBuilder.build()).build());

        /* Prepare PcRep Message */
        final PcrepMessageBuilder msgBuilder = new PcrepMessageBuilder()
                .setReplies(Lists.newArrayList(replyBuilder.build()));
        return new PcrepBuilder().setPcrepMessage(msgBuilder.build()).build();
    }

    public static Pcerr createErrorMsg(@NonNull final PCEPErrors pcepErrors, final Uint32 reqID) {
        final PcerrMessageBuilder msgBuilder = new PcerrMessageBuilder();
        return new PcerrBuilder().setPcerrMessage(msgBuilder
                .setErrorType(
                        new RequestCaseBuilder().setRequest(new RequestBuilder()
                                .setRps(Lists
                                        .newArrayList(new RpsBuilder().setRp(new RpBuilder().setProcessingRule(false)
                                                .setIgnore(false).setRequestId(new RequestId(reqID)).build()).build()))
                                .build()).build())
                .setErrors(Collections.singletonList(new ErrorsBuilder().setErrorObject(new ErrorObjectBuilder()
                        .setType(pcepErrors.getErrorType()).setValue(pcepErrors.getErrorValue()).build()).build()))
                .build()).build();
    }

}
