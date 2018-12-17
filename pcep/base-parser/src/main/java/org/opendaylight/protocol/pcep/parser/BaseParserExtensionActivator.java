/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.parser;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.parser.message.PCEPCloseMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPErrorMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPKeepAliveMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPMonitoringReplyMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPMonitoringRequestMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPNotificationMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPOpenMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPReplyMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPRequestMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPStartTLSMessageParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPBandwidthObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPClassTypeObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPCloseObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPErrorObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPExcludeRouteObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPExistingBandwidthObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPGlobalConstraintsObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPIncludeRouteObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPLoadBalancingObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPMetricObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPMonitoringObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPNoPathObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPNotificationObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPObjectiveFunctionObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPOverloadObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPPathKeyObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPPccIdReqIPv4ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPPccIdReqIPv6ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPPceIdIPv4ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPPceIdIPv6ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPProcTimeObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPReportedRouteObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPRequestParameterObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPSecondaryExplicitRouteObjecParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPSecondaryRecordRouteObjectParser;
import org.opendaylight.protocol.pcep.parser.object.PCEPSvecObjectParser;
import org.opendaylight.protocol.pcep.parser.object.bnc.BranchNodeListObjectParser;
import org.opendaylight.protocol.pcep.parser.object.bnc.NonBranchNodeListObjectParser;
import org.opendaylight.protocol.pcep.parser.object.end.points.PCEPEndPointsIpv4ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.end.points.PCEPEndPointsIpv6ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.end.points.PCEPEndPointsObjectSerializer;
import org.opendaylight.protocol.pcep.parser.object.end.points.PCEPP2MPEndPointsIpv4ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.end.points.PCEPP2MPEndPointsIpv6ObjectParser;
import org.opendaylight.protocol.pcep.parser.object.unreach.PCEPIpv4UnreachDestinationParser;
import org.opendaylight.protocol.pcep.parser.object.unreach.PCEPIpv6UnreachDestinationParser;
import org.opendaylight.protocol.pcep.parser.object.unreach.PCEPUnreachDestinationSerializer;
import org.opendaylight.protocol.pcep.parser.subobject.EROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.EROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.EROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.EROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.EROPathKey128SubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.EROPathKey32SubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.EROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.GeneralizedLabelParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROPathKey128SubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROPathKey32SubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.RROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.Type1LabelParser;
import org.opendaylight.protocol.pcep.parser.subobject.WavebandSwitchingLabelParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROPathKey128SubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROPathKey32SubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROSRLGSubobjectParser;
import org.opendaylight.protocol.pcep.parser.subobject.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.parser.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.PathSetupTypeTlvParser;
import org.opendaylight.protocol.pcep.parser.tlv.ReqMissingTlvParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.LabelRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.VendorInformationTlvRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Close;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcmonrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcmonreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcntf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Starttls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.branch.node.object.BranchNodeList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.object.CClose;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.gc.object.Gc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.load.balancing.object.LoadBalancing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.overload.object.Overload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.path.setup.type.tlv.PathSetupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcc.id.req.object.PccIdReq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pce.id.object.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.failure._case.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.proc.time.object.ProcTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.secondary.explicit.route.object.Sero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.secondary.reported.route.object.Srro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.svec.object.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.unreach.destination.object.UnreachDestinationObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.SrlgCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.explicit.route.subobjects.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.GeneralizedLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.Type1LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.label.subobject.label.type.WavebandSwitchingLabelCase;
import org.opendaylight.yangtools.concepts.Registration;

public final class BaseParserExtensionActivator extends AbstractPCEPExtensionProviderActivator {
    @Override
    protected List<Registration> startImpl(final PCEPExtensionProviderContext context) {
        final List<Registration> regs = new ArrayList<>();

        registerLabelParsers(regs, context);

        final LabelRegistry labelReg = context.getLabelHandlerRegistry();
        registerEROParsers(regs, context, labelReg);
        registerRROParsers(regs, context, labelReg);
        registerXROParsers(regs, context);
        registerTlvParsers(regs, context);
        registerObjectParsers(regs, context);

        final ObjectRegistry objReg = context.getObjectHandlerRegistry();
        final PCEPOpenMessageParser openParser = new PCEPOpenMessageParser(objReg);
        regs.add(context.registerMessageParser(PCEPOpenMessageParser.TYPE, openParser));
        regs.add(context.registerMessageSerializer(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Open.class,
            openParser));

        final PCEPKeepAliveMessageParser kaParser = new PCEPKeepAliveMessageParser(objReg);
        regs.add(context.registerMessageParser(PCEPKeepAliveMessageParser.TYPE, kaParser));
        regs.add(context.registerMessageSerializer(Keepalive.class, kaParser));

        final PCEPRequestMessageParser reqParser = new PCEPRequestMessageParser(objReg);
        regs.add(context.registerMessageParser(PCEPRequestMessageParser.TYPE, reqParser));
        regs.add(context.registerMessageSerializer(Pcreq.class, reqParser));

        final PCEPReplyMessageParser repParser = new PCEPReplyMessageParser(objReg);
        regs.add(context.registerMessageParser(PCEPReplyMessageParser.TYPE, repParser));
        regs.add(context.registerMessageSerializer(Pcrep.class, repParser));

        final PCEPNotificationMessageParser notParser = new PCEPNotificationMessageParser(objReg);
        regs.add(context.registerMessageParser(PCEPNotificationMessageParser.TYPE, notParser));
        regs.add(context.registerMessageSerializer(Pcntf.class, notParser));

        final PCEPErrorMessageParser errParser = new PCEPErrorMessageParser(objReg);
        regs.add(context.registerMessageParser(PCEPErrorMessageParser.TYPE, errParser));
        regs.add(context.registerMessageSerializer(Pcerr.class, errParser));

        final PCEPCloseMessageParser closeParser = new PCEPCloseMessageParser(objReg);
        regs.add(context.registerMessageParser(PCEPCloseMessageParser.TYPE, closeParser));
        regs.add(context.registerMessageSerializer(Close.class, closeParser));

        final PCEPMonitoringReplyMessageParser monRepParser = new PCEPMonitoringReplyMessageParser(objReg);
        regs.add(context.registerMessageParser(PCEPMonitoringReplyMessageParser.TYPE, monRepParser));
        regs.add(context.registerMessageSerializer(Pcmonrep.class, monRepParser));

        final PCEPMonitoringRequestMessageParser monReqParser = new PCEPMonitoringRequestMessageParser(objReg);
        regs.add(context.registerMessageParser(PCEPMonitoringRequestMessageParser.TYPE, monReqParser));
        regs.add(context.registerMessageSerializer(Pcmonreq.class, monReqParser));

        final PCEPStartTLSMessageParser startTLSParser = new PCEPStartTLSMessageParser(objReg);
        regs.add(context.registerMessageParser(PCEPStartTLSMessageParser.TYPE, startTLSParser));
        regs.add(context.registerMessageSerializer(Starttls.class, startTLSParser));

        return regs;
    }

    private static void registerObjectParsers(final List<Registration> regs,
            final PCEPExtensionProviderContext context) {
        final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
        final VendorInformationTlvRegistry viTlvReg = context.getVendorInformationTlvRegistry();
        final PCEPOpenObjectParser openParser = new PCEPOpenObjectParser(tlvReg, viTlvReg);
        regs.add(context.registerObjectParser(openParser));
        regs.add(context.registerObjectSerializer(Open.class, openParser));

        final PCEPRequestParameterObjectParser rpParser = new PCEPRequestParameterObjectParser(tlvReg, viTlvReg);
        regs.add(context.registerObjectParser(rpParser));
        regs.add(context.registerObjectSerializer(Rp.class, rpParser));

        final PCEPNoPathObjectParser noPathParser = new PCEPNoPathObjectParser(tlvReg, viTlvReg);
        regs.add(context.registerObjectParser(noPathParser));
        regs.add(context.registerObjectSerializer(NoPath.class, noPathParser));

        final PCEPEndPointsObjectSerializer endpointsSerializer = new PCEPEndPointsObjectSerializer();
        regs.add(context.registerObjectSerializer(EndpointsObj.class, endpointsSerializer));

        final PCEPEndPointsIpv4ObjectParser endpoints4Parser = new PCEPEndPointsIpv4ObjectParser();
        regs.add(context.registerObjectParser(endpoints4Parser));

        final PCEPEndPointsIpv6ObjectParser endpoints6Parser = new PCEPEndPointsIpv6ObjectParser();
        regs.add(context.registerObjectParser(endpoints6Parser));

        final PCEPIpv4UnreachDestinationParser unreachIpv4Parser = new PCEPIpv4UnreachDestinationParser();
        final PCEPIpv6UnreachDestinationParser unreachIpv6Parser = new PCEPIpv6UnreachDestinationParser();
        regs.add(context.registerObjectParser(unreachIpv4Parser));
        regs.add(context.registerObjectParser(unreachIpv6Parser));

        final PCEPUnreachDestinationSerializer unreachSerializer = new PCEPUnreachDestinationSerializer();
        regs.add(context.registerObjectSerializer(UnreachDestinationObj.class, unreachSerializer));

        final PCEPP2MPEndPointsIpv4ObjectParser endpoints4Pp2mparser = new PCEPP2MPEndPointsIpv4ObjectParser();
        regs.add(context.registerObjectParser(endpoints4Pp2mparser));

        final PCEPP2MPEndPointsIpv6ObjectParser endpoints6Pp2mparser = new PCEPP2MPEndPointsIpv6ObjectParser();
        regs.add(context.registerObjectParser(endpoints6Pp2mparser));

        final EROSubobjectRegistry subRegistry = context.getEROSubobjectHandlerRegistry();

        final BranchNodeListObjectParser bncPArser = new BranchNodeListObjectParser(subRegistry);
        regs.add(context.registerObjectParser(bncPArser));
        regs.add(context.registerObjectSerializer(BranchNodeList.class, bncPArser));

        final NonBranchNodeListObjectParser nonBncPArser = new NonBranchNodeListObjectParser(subRegistry);
        regs.add(context.registerObjectParser(nonBncPArser));

        final PCEPBandwidthObjectParser bwParser = new PCEPBandwidthObjectParser();
        final PCEPExistingBandwidthObjectParser bwExistingParser = new PCEPExistingBandwidthObjectParser();
        regs.add(context.registerObjectParser(bwParser));
        regs.add(context.registerObjectParser(bwExistingParser));
        regs.add(context.registerObjectSerializer(Bandwidth.class, bwParser));
        regs.add(context.registerObjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .pcep.types.rev181109.reoptimization.bandwidth.object.ReoptimizationBandwidth.class, bwExistingParser));

        final PCEPMetricObjectParser metricParser = new PCEPMetricObjectParser();
        regs.add(context.registerObjectParser(metricParser));
        regs.add(context.registerObjectSerializer(Metric.class, metricParser));

        final EROSubobjectRegistry eroSubReg = context.getEROSubobjectHandlerRegistry();
        final PCEPExplicitRouteObjectParser eroParser = new PCEPExplicitRouteObjectParser(eroSubReg);
        regs.add(context.registerObjectParser(eroParser));
        regs.add(context.registerObjectSerializer(Ero.class, eroParser));

        final RROSubobjectRegistry rroSubReg = context.getRROSubobjectHandlerRegistry();
        final PCEPReportedRouteObjectParser rroParser = new PCEPReportedRouteObjectParser(rroSubReg);
        regs.add(context.registerObjectParser(rroParser));
        regs.add(context.registerObjectSerializer(Rro.class, rroParser));

        final PCEPLspaObjectParser lspaParser = new PCEPLspaObjectParser(tlvReg, viTlvReg);
        regs.add(context.registerObjectParser(lspaParser));
        regs.add(context.registerObjectSerializer(Lspa.class, lspaParser));

        final PCEPIncludeRouteObjectParser iroParser = new PCEPIncludeRouteObjectParser(eroSubReg);
        regs.add(context.registerObjectParser(iroParser));
        regs.add(context.registerObjectSerializer(Iro.class, iroParser));

        final PCEPSvecObjectParser svecParser = new PCEPSvecObjectParser();
        regs.add(context.registerObjectParser(svecParser));
        regs.add(context.registerObjectSerializer(Svec.class, svecParser));

        final PCEPNotificationObjectParser notParser = new PCEPNotificationObjectParser(tlvReg, viTlvReg);
        regs.add(context.registerObjectParser(notParser));
        regs.add(context.registerObjectSerializer(CNotification.class, notParser));

        final PCEPErrorObjectParser errParser = new PCEPErrorObjectParser(tlvReg, viTlvReg);
        regs.add(context.registerObjectParser(errParser));
        regs.add(context.registerObjectSerializer(ErrorObject.class, errParser));

        final PCEPLoadBalancingObjectParser lbParser = new PCEPLoadBalancingObjectParser();
        regs.add(context.registerObjectParser(lbParser));
        regs.add(context.registerObjectSerializer(LoadBalancing.class, lbParser));

        final PCEPCloseObjectParser closeParser = new PCEPCloseObjectParser(tlvReg, viTlvReg);
        regs.add(context.registerObjectParser(closeParser));
        regs.add(context.registerObjectSerializer(CClose.class, closeParser));

        registerExtensionsObjectParsers(regs, context, tlvReg, viTlvReg, eroSubReg, rroSubReg);
    }

    private static void registerExtensionsObjectParsers(final List<Registration> regs,
            final PCEPExtensionProviderContext context, final TlvRegistry tlvReg,
            final VendorInformationTlvRegistry viTlvReg, final EROSubobjectRegistry eroSubReg,
            final RROSubobjectRegistry rroSubReg) {
        final PCEPPathKeyObjectParser pathKeyParser = new PCEPPathKeyObjectParser(eroSubReg);
        regs.add(context.registerObjectParser(pathKeyParser));
        regs.add(context.registerObjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep
            .types.rev181109.path.key.object.PathKey.class, pathKeyParser));

        final PCEPExcludeRouteObjectParser xroParser = new PCEPExcludeRouteObjectParser(
            context.getXROSubobjectHandlerRegistry());
        regs.add(context.registerObjectParser(xroParser));
        regs.add(context.registerObjectSerializer(Xro.class, xroParser));

        final PCEPObjectiveFunctionObjectParser objectiveParser = new PCEPObjectiveFunctionObjectParser(tlvReg,
            viTlvReg);
        regs.add(context.registerObjectParser(objectiveParser));
        regs.add(context.registerObjectSerializer(Of.class, objectiveParser));

        final PCEPClassTypeObjectParser ctParser = new PCEPClassTypeObjectParser();
        regs.add(context.registerObjectParser(ctParser));
        regs.add(context.registerObjectSerializer(ClassType.class, ctParser));

        final PCEPGlobalConstraintsObjectParser gcParser = new PCEPGlobalConstraintsObjectParser(tlvReg, viTlvReg);
        regs.add(context.registerObjectParser(gcParser));
        regs.add(context.registerObjectSerializer(Gc.class, gcParser));

        final PCEPSecondaryExplicitRouteObjecParser seroParser = new PCEPSecondaryExplicitRouteObjecParser(eroSubReg);
        regs.add(context.registerObjectParser(seroParser));
        regs.add(context.registerObjectSerializer(Sero.class, seroParser));

        final PCEPSecondaryRecordRouteObjectParser srroParser = new PCEPSecondaryRecordRouteObjectParser(rroSubReg);
        regs.add(context.registerObjectParser(srroParser));
        regs.add(context.registerObjectSerializer(Srro.class, srroParser));

        registerMonitoringExtensionParsers(regs, context, tlvReg, viTlvReg);
    }

    private static void registerEROParsers(final List<Registration> regs, final PCEPExtensionProviderContext context,
            final LabelRegistry labelReg) {
        final EROIpv4PrefixSubobjectParser ipv4prefixParser = new EROIpv4PrefixSubobjectParser();
        regs.add(context.registerEROSubobjectParser(EROIpv4PrefixSubobjectParser.TYPE, ipv4prefixParser));
        regs.add(context.registerEROSubobjectSerializer(IpPrefixCase.class, ipv4prefixParser));
        regs.add(context.registerEROSubobjectParser(EROIpv6PrefixSubobjectParser.TYPE,
            new EROIpv6PrefixSubobjectParser()));

        final EROAsNumberSubobjectParser asNumberParser = new EROAsNumberSubobjectParser();
        regs.add(context.registerEROSubobjectParser(EROAsNumberSubobjectParser.TYPE, asNumberParser));
        regs.add(context.registerEROSubobjectSerializer(AsNumberCase.class, asNumberParser));

        final EROUnnumberedInterfaceSubobjectParser unnumberedParser = new EROUnnumberedInterfaceSubobjectParser();
        regs.add(context.registerEROSubobjectParser(EROUnnumberedInterfaceSubobjectParser.TYPE, unnumberedParser));
        regs.add(context.registerEROSubobjectSerializer(UnnumberedCase.class, unnumberedParser));

        final EROPathKey32SubobjectParser pathKeyParser =  new EROPathKey32SubobjectParser();
        regs.add(context.registerEROSubobjectParser(EROPathKey32SubobjectParser.TYPE_32, pathKeyParser));
        regs.add(context.registerEROSubobjectParser(EROPathKey128SubobjectParser.TYPE_128,
            new EROPathKey128SubobjectParser()));
        regs.add(context.registerEROSubobjectSerializer(PathKeyCase.class, pathKeyParser));

        final EROLabelSubobjectParser labelParser = new EROLabelSubobjectParser(labelReg);
        regs.add(context.registerEROSubobjectParser(EROLabelSubobjectParser.TYPE, labelParser));
        regs.add(context.registerEROSubobjectSerializer(LabelCase.class, labelParser));
    }

    private static void registerRROParsers(final List<Registration> regs, final PCEPExtensionProviderContext context,
            final LabelRegistry labelReg) {
        final RROIpv4PrefixSubobjectParser ipv4prefixParser = new RROIpv4PrefixSubobjectParser();
        regs.add(context.registerRROSubobjectParser(RROIpv4PrefixSubobjectParser.TYPE, ipv4prefixParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .rsvp.rev150820.record.route.subobjects.subobject.type.IpPrefixCase.class, ipv4prefixParser));
        regs.add(context.registerRROSubobjectParser(RROIpv6PrefixSubobjectParser.TYPE,
            new RROIpv6PrefixSubobjectParser()));

        final RROUnnumberedInterfaceSubobjectParser unnumberedParser = new RROUnnumberedInterfaceSubobjectParser();
        regs.add(context.registerRROSubobjectParser(RROUnnumberedInterfaceSubobjectParser.TYPE, unnumberedParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .rsvp.rev150820.record.route.subobjects.subobject.type.UnnumberedCase.class, unnumberedParser));

        final RROPathKey32SubobjectParser pathKeyParser =  new RROPathKey32SubobjectParser();
        regs.add(context.registerRROSubobjectParser(RROPathKey32SubobjectParser.TYPE, pathKeyParser));
        regs.add(context.registerRROSubobjectParser(RROPathKey128SubobjectParser.TYPE,
            new RROPathKey128SubobjectParser()));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .rsvp.rev150820.record.route.subobjects.subobject.type.PathKeyCase.class, pathKeyParser));

        final RROLabelSubobjectParser labelParser = new RROLabelSubobjectParser(labelReg);
        regs.add(context.registerRROSubobjectParser(RROLabelSubobjectParser.TYPE, labelParser));
        regs.add(context.registerRROSubobjectSerializer(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .rsvp.rev150820.record.route.subobjects.subobject.type.LabelCase.class, labelParser));
    }

    private static void registerXROParsers(final List<Registration> regs, final PCEPExtensionProviderContext context) {
        final XROIpv4PrefixSubobjectParser ipv4prefixParser = new XROIpv4PrefixSubobjectParser();
        regs.add(context.registerXROSubobjectParser(XROIpv4PrefixSubobjectParser.TYPE, ipv4prefixParser));
        regs.add(context.registerXROSubobjectSerializer(IpPrefixCase.class, ipv4prefixParser));
        regs.add(context.registerXROSubobjectParser(XROIpv6PrefixSubobjectParser.TYPE,
            new XROIpv6PrefixSubobjectParser()));

        final XROAsNumberSubobjectParser asNumberParser = new XROAsNumberSubobjectParser();
        regs.add(context.registerXROSubobjectParser(XROAsNumberSubobjectParser.TYPE, asNumberParser));
        regs.add(context.registerXROSubobjectSerializer(AsNumberCase.class, asNumberParser));

        final XROSRLGSubobjectParser srlgParser = new XROSRLGSubobjectParser();
        regs.add(context.registerXROSubobjectParser(XROSRLGSubobjectParser.TYPE, srlgParser));
        regs.add(context.registerXROSubobjectSerializer(SrlgCase.class, srlgParser));

        final XROUnnumberedInterfaceSubobjectParser unnumberedParser = new XROUnnumberedInterfaceSubobjectParser();
        regs.add(context.registerXROSubobjectParser(XROUnnumberedInterfaceSubobjectParser.TYPE, unnumberedParser));
        regs.add(context.registerXROSubobjectSerializer(UnnumberedCase.class, unnumberedParser));

        final XROPathKey32SubobjectParser pathKeyParser =  new XROPathKey32SubobjectParser();
        regs.add(context.registerXROSubobjectParser(XROPathKey32SubobjectParser.TYPE, pathKeyParser));
        regs.add(context.registerXROSubobjectParser(XROPathKey128SubobjectParser.TYPE,
            new XROPathKey128SubobjectParser()));
        regs.add(context.registerXROSubobjectSerializer(PathKeyCase.class, pathKeyParser));
    }

    private static void registerLabelParsers(final List<Registration> regs,
            final PCEPExtensionProviderContext context) {
        final Type1LabelParser type1Parser = new Type1LabelParser();
        regs.add(context.registerLabelParser(Type1LabelParser.CTYPE, type1Parser));
        regs.add(context.registerLabelSerializer(Type1LabelCase.class, type1Parser));

        final GeneralizedLabelParser generalizedParser = new GeneralizedLabelParser();
        regs.add(context.registerLabelParser(GeneralizedLabelParser.CTYPE, generalizedParser));
        regs.add(context.registerLabelSerializer(GeneralizedLabelCase.class, generalizedParser));

        final WavebandSwitchingLabelParser wavebandParser = new WavebandSwitchingLabelParser();
        regs.add(context.registerLabelParser(WavebandSwitchingLabelParser.CTYPE, wavebandParser));
        regs.add(context.registerLabelSerializer(WavebandSwitchingLabelCase.class, wavebandParser));
    }

    private static void registerTlvParsers(final List<Registration> regs, final PCEPExtensionProviderContext context) {
        final NoPathVectorTlvParser noPathParser = new NoPathVectorTlvParser();
        regs.add(context.registerTlvParser(NoPathVectorTlvParser.TYPE, noPathParser));
        regs.add(context.registerTlvSerializer(NoPathVector.class, noPathParser));

        final OverloadedDurationTlvParser overloadedDurationParser = new OverloadedDurationTlvParser();
        regs.add(context.registerTlvParser(OverloadedDurationTlvParser.TYPE, overloadedDurationParser));
        regs.add(context.registerTlvSerializer(OverloadDuration.class, overloadedDurationParser));

        final ReqMissingTlvParser reqMissingParser = new ReqMissingTlvParser();
        regs.add(context.registerTlvParser(ReqMissingTlvParser.TYPE, reqMissingParser));
        regs.add(context.registerTlvSerializer(ReqMissing.class, reqMissingParser));

        final OFListTlvParser ofListParser = new OFListTlvParser();
        regs.add(context.registerTlvParser(OFListTlvParser.TYPE, ofListParser));
        regs.add(context.registerTlvSerializer(OfList.class, ofListParser));

        final OrderTlvParser orderParser = new OrderTlvParser();
        regs.add(context.registerTlvParser(OrderTlvParser.TYPE, orderParser));
        regs.add(context.registerTlvSerializer(Order.class, orderParser));

        final PathSetupTypeTlvParser pstParser = new PathSetupTypeTlvParser();
        regs.add(context.registerTlvParser(PathSetupTypeTlvParser.TYPE, pstParser));
        regs.add(context.registerTlvSerializer(PathSetupType.class, pstParser));
    }

    private static void registerMonitoringExtensionParsers(final List<Registration> regs,
            final PCEPExtensionProviderContext context, final TlvRegistry tlvReg,
            final VendorInformationTlvRegistry viTlvReg) {
        final PCEPMonitoringObjectParser monParser = new PCEPMonitoringObjectParser(tlvReg, viTlvReg);
        regs.add(context.registerObjectParser(monParser));
        regs.add(context.registerObjectSerializer(Monitoring.class, monParser));

        final PCEPPccIdReqIPv4ObjectParser pccIdIPv4Parser = new PCEPPccIdReqIPv4ObjectParser();
        regs.add(context.registerObjectParser(pccIdIPv4Parser));
        regs.add(context.registerObjectSerializer(PccIdReq.class, pccIdIPv4Parser));

        final PCEPPccIdReqIPv6ObjectParser pccIdIPv6Parser = new PCEPPccIdReqIPv6ObjectParser();
        regs.add(context.registerObjectParser(pccIdIPv6Parser));
        regs.add(context.registerObjectSerializer(PccIdReq.class, pccIdIPv6Parser));

        final PCEPPceIdIPv4ObjectParser pceIdIP4Parser = new PCEPPceIdIPv4ObjectParser();
        regs.add(context.registerObjectParser(pceIdIP4Parser));
        regs.add(context.registerObjectSerializer(PceId.class, pceIdIP4Parser));

        final PCEPPceIdIPv6ObjectParser pceIdIP6Parser = new PCEPPceIdIPv6ObjectParser();
        regs.add(context.registerObjectParser(pceIdIP6Parser));
        regs.add(context.registerObjectSerializer(PceId.class, pceIdIP6Parser));

        final PCEPProcTimeObjectParser procTimeParser = new PCEPProcTimeObjectParser();
        regs.add(context.registerObjectParser(procTimeParser));
        regs.add(context.registerObjectSerializer(ProcTime.class, procTimeParser));

        final PCEPOverloadObjectParser overloadParser = new PCEPOverloadObjectParser();
        regs.add(context.registerObjectParser(overloadParser));
        regs.add(context.registerObjectSerializer(Overload.class, overloadParser));
    }
}
