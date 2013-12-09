/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.impl.message.PCEPCloseMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPErrorMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPKeepAliveMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPNotificationMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPOpenMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPReplyMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPReportMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPRequestMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPUpdateRequestMessageParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPBandwidthObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPClassTypeObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPCloseObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPEndPointsObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPErrorObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExcludeRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPGlobalConstraintsObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPIncludeRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLoadBalancingObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLspObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPMetricObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNoPathObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNotificationObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPObjectiveFunctionObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPOpenObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPPathKeyObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPReportedRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPRequestParameterObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSrpObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSvecObjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROIpPrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROPathKeySubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.GeneralizedLabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROIpPrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROPathKey128SubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROPathKey32SubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.Type1LabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.WavebandSwitchingLabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROPathKey128SubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROPathKey32SubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROSRLGSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIpv4TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIpv6TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspDbVersionTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspSymbolicNameTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspUpdateErrorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PCEStatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PredundancyGroupTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.RSVPErrorSpecTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.ReqMissingTlvParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Close;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcntf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CClose;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobjects.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.gc.object.Gc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.load.balancing.object.LoadBalancing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.predundancy.group.id.tlv.PredundancyGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.SrlgCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.UnnumberedCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.Type1LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.WavebandSwitchingLabelCase;

public final class Activator extends AbstractPCEPExtensionProviderActivator {
	@Override
	protected List<AutoCloseable> startImpl(final PCEPExtensionProviderContext context) {
		final List<AutoCloseable> regs = new ArrayList<>();

		final LabelHandlerRegistry labelReg = context.getLabelHandlerRegistry();
		context.registerLabelParser(Type1LabelParser.CTYPE, new Type1LabelParser());
		context.registerLabelParser(GeneralizedLabelParser.CTYPE, new GeneralizedLabelParser());
		context.registerLabelParser(WavebandSwitchingLabelParser.CTYPE, new WavebandSwitchingLabelParser());

		context.registerLabelSerializer(Type1LabelCase.class, new Type1LabelParser());
		context.registerLabelSerializer(GeneralizedLabelCase.class, new GeneralizedLabelParser());
		context.registerLabelSerializer(WavebandSwitchingLabelCase.class, new WavebandSwitchingLabelParser());

		final EROSubobjectHandlerRegistry eroSubReg = context.getEROSubobjectHandlerRegistry();
		context.registerEROSubobjectParser(EROIpPrefixSubobjectParser.TYPE, new EROIpPrefixSubobjectParser());
		context.registerEROSubobjectParser(EROIpPrefixSubobjectParser.TYPE6, new EROIpPrefixSubobjectParser());
		context.registerEROSubobjectParser(EROAsNumberSubobjectParser.TYPE, new EROAsNumberSubobjectParser());
		context.registerEROSubobjectParser(EROLabelSubobjectParser.TYPE, new EROLabelSubobjectParser(labelReg));
		context.registerEROSubobjectParser(EROUnnumberedInterfaceSubobjectParser.TYPE, new EROUnnumberedInterfaceSubobjectParser());
		context.registerEROSubobjectParser(EROPathKeySubobjectParser.TYPE, new EROPathKeySubobjectParser());
		context.registerEROSubobjectParser(EROPathKeySubobjectParser.TYPE128, new EROPathKeySubobjectParser());

		context.registerEROSubobjectSerializer(IpPrefixCase.class, new EROIpPrefixSubobjectParser());
		context.registerEROSubobjectSerializer(AsNumberCase.class, new EROAsNumberSubobjectParser());
		context.registerEROSubobjectSerializer(LabelCase.class, new EROLabelSubobjectParser(labelReg));
		context.registerEROSubobjectSerializer(UnnumberedCase.class, new EROUnnumberedInterfaceSubobjectParser());
		context.registerEROSubobjectSerializer(PathKeyCase.class, new EROPathKeySubobjectParser());

		final RROSubobjectHandlerRegistry rroSubReg = context.getRROSubobjectHandlerRegistry();
		context.registerRROSubobjectParser(RROIpPrefixSubobjectParser.TYPE, new RROIpPrefixSubobjectParser());
		context.registerRROSubobjectParser(RROIpPrefixSubobjectParser.TYPE6, new RROIpPrefixSubobjectParser());
		context.registerRROSubobjectParser(RROLabelSubobjectParser.TYPE, new RROLabelSubobjectParser(labelReg));
		context.registerRROSubobjectParser(RROUnnumberedInterfaceSubobjectParser.TYPE, new RROUnnumberedInterfaceSubobjectParser());
		context.registerRROSubobjectParser(RROPathKey32SubobjectParser.TYPE, new RROPathKey32SubobjectParser());
		context.registerRROSubobjectParser(RROPathKey128SubobjectParser.TYPE, new RROPathKey128SubobjectParser());

		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.IpPrefixCase.class,
				new RROIpPrefixSubobjectParser());
		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.LabelCase.class,
				new RROLabelSubobjectParser(labelReg));
		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.UnnumberedCase.class,
				new RROUnnumberedInterfaceSubobjectParser());
		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobjects.subobject.type.PathKeyCase.class,
				new RROPathKey32SubobjectParser());

		final XROSubobjectHandlerRegistry xroSubReg = context.getXROSubobjectHandlerRegistry();
		context.registerXROSubobjectParser(XROIpv4PrefixSubobjectParser.TYPE, new XROIpv4PrefixSubobjectParser());
		context.registerXROSubobjectParser(XROIpv6PrefixSubobjectParser.TYPE, new XROIpv6PrefixSubobjectParser());
		context.registerXROSubobjectParser(XROAsNumberSubobjectParser.TYPE, new XROAsNumberSubobjectParser());
		context.registerXROSubobjectParser(XROSRLGSubobjectParser.TYPE, new XROSRLGSubobjectParser());
		context.registerXROSubobjectParser(XROUnnumberedInterfaceSubobjectParser.TYPE, new XROUnnumberedInterfaceSubobjectParser());
		context.registerXROSubobjectParser(XROPathKey32SubobjectParser.TYPE, new XROPathKey32SubobjectParser());
		context.registerXROSubobjectParser(XROPathKey128SubobjectParser.TYPE, new XROPathKey128SubobjectParser());

		context.registerXROSubobjectSerializer(IpPrefixCase.class, new XROIpv4PrefixSubobjectParser());
		context.registerXROSubobjectSerializer(AsNumberCase.class, new XROAsNumberSubobjectParser());
		context.registerXROSubobjectSerializer(SrlgCase.class, new XROSRLGSubobjectParser());
		context.registerXROSubobjectSerializer(UnnumberedCase.class, new XROUnnumberedInterfaceSubobjectParser());
		context.registerXROSubobjectSerializer(PathKeyCase.class, new XROPathKey32SubobjectParser());

		final TlvHandlerRegistry tlvReg = context.getTlvHandlerRegistry();
		context.registerTlvParser(NoPathVectorTlvParser.TYPE, new NoPathVectorTlvParser());
		context.registerTlvParser(OverloadedDurationTlvParser.TYPE, new OverloadedDurationTlvParser());
		context.registerTlvParser(ReqMissingTlvParser.TYPE, new ReqMissingTlvParser());
		context.registerTlvParser(OFListTlvParser.TYPE, new OFListTlvParser());
		context.registerTlvParser(OrderTlvParser.TYPE, new OrderTlvParser());
		context.registerTlvParser(PCEStatefulCapabilityTlvParser.TYPE, new PCEStatefulCapabilityTlvParser());
		context.registerTlvParser(LspSymbolicNameTlvParser.TYPE, new LspSymbolicNameTlvParser());
		context.registerTlvParser(LSPIdentifierIpv4TlvParser.TYPE, new LSPIdentifierIpv4TlvParser());
		context.registerTlvParser(LSPIdentifierIpv6TlvParser.TYPE, new LSPIdentifierIpv6TlvParser());
		context.registerTlvParser(LspUpdateErrorTlvParser.TYPE, new LspUpdateErrorTlvParser());
		context.registerTlvParser(RSVPErrorSpecTlvParser.TYPE, new RSVPErrorSpecTlvParser());
		context.registerTlvParser(LspDbVersionTlvParser.TYPE, new LspDbVersionTlvParser());
		context.registerTlvParser(PredundancyGroupTlvParser.TYPE, new PredundancyGroupTlvParser());

		context.registerTlvSerializer(NoPathVector.class, new NoPathVectorTlvParser());
		context.registerTlvSerializer(OverloadDuration.class, new OverloadedDurationTlvParser());
		context.registerTlvSerializer(ReqMissing.class, new ReqMissingTlvParser());
		context.registerTlvSerializer(OfList.class, new OFListTlvParser());
		context.registerTlvSerializer(Order.class, new OrderTlvParser());
		context.registerTlvSerializer(Stateful.class, new PCEStatefulCapabilityTlvParser());
		context.registerTlvSerializer(SymbolicPathName.class, new LspSymbolicNameTlvParser());
		context.registerTlvSerializer(LspIdentifiers.class, new LSPIdentifierIpv4TlvParser());
		context.registerTlvSerializer(LspErrorCode.class, new LspUpdateErrorTlvParser());
		context.registerTlvSerializer(RsvpErrorSpec.class, new RSVPErrorSpecTlvParser());
		context.registerTlvSerializer(LspDbVersion.class, new LspDbVersionTlvParser());
		context.registerTlvSerializer(PredundancyGroupId.class, new PredundancyGroupTlvParser());

		final ObjectHandlerRegistry objReg = context.getObjectHandlerRegistry();
		context.registerObjectParser(PCEPOpenObjectParser.CLASS, PCEPOpenObjectParser.TYPE, new PCEPOpenObjectParser(tlvReg));
		context.registerObjectParser(PCEPRequestParameterObjectParser.CLASS, PCEPRequestParameterObjectParser.TYPE,
				new PCEPRequestParameterObjectParser(tlvReg));
		context.registerObjectParser(PCEPNoPathObjectParser.CLASS, PCEPNoPathObjectParser.TYPE, new PCEPNoPathObjectParser(tlvReg));
		context.registerObjectParser(PCEPEndPointsObjectParser.CLASS, PCEPEndPointsObjectParser.TYPE, new PCEPEndPointsObjectParser(tlvReg));
		context.registerObjectParser(PCEPEndPointsObjectParser.CLASS_6, PCEPEndPointsObjectParser.TYPE_6,
				new PCEPEndPointsObjectParser(tlvReg));
		context.registerObjectParser(PCEPBandwidthObjectParser.CLASS, PCEPBandwidthObjectParser.TYPE, new PCEPBandwidthObjectParser(tlvReg));
		context.registerObjectParser(PCEPBandwidthObjectParser.E_CLASS, PCEPBandwidthObjectParser.E_TYPE,
				new PCEPBandwidthObjectParser(tlvReg));
		context.registerObjectParser(PCEPMetricObjectParser.CLASS, PCEPMetricObjectParser.TYPE, new PCEPMetricObjectParser(tlvReg));

		context.registerObjectParser(PCEPExplicitRouteObjectParser.CLASS, PCEPExplicitRouteObjectParser.TYPE,
				new PCEPExplicitRouteObjectParser(eroSubReg));
		context.registerObjectParser(PCEPReportedRouteObjectParser.CLASS, PCEPReportedRouteObjectParser.TYPE,
				new PCEPReportedRouteObjectParser(rroSubReg));
		context.registerObjectParser(PCEPLspaObjectParser.CLASS, PCEPLspaObjectParser.TYPE, new PCEPLspaObjectParser(tlvReg));
		context.registerObjectParser(PCEPIncludeRouteObjectParser.CLASS, PCEPIncludeRouteObjectParser.TYPE,
				new PCEPIncludeRouteObjectParser(eroSubReg));
		context.registerObjectParser(PCEPSvecObjectParser.CLASS, PCEPSvecObjectParser.TYPE, new PCEPSvecObjectParser(tlvReg));
		context.registerObjectParser(PCEPNotificationObjectParser.CLASS, PCEPNotificationObjectParser.TYPE,
				new PCEPNotificationObjectParser(tlvReg));
		context.registerObjectParser(PCEPErrorObjectParser.CLASS, PCEPErrorObjectParser.TYPE, new PCEPErrorObjectParser(tlvReg));
		context.registerObjectParser(PCEPLoadBalancingObjectParser.CLASS, PCEPLoadBalancingObjectParser.TYPE,
				new PCEPLoadBalancingObjectParser(tlvReg));
		context.registerObjectParser(PCEPCloseObjectParser.CLASS, PCEPCloseObjectParser.TYPE, new PCEPCloseObjectParser(tlvReg));
		context.registerObjectParser(PCEPPathKeyObjectParser.CLASS, PCEPPathKeyObjectParser.TYPE, new PCEPPathKeyObjectParser(eroSubReg));
		context.registerObjectParser(PCEPObjectiveFunctionObjectParser.CLASS, PCEPObjectiveFunctionObjectParser.TYPE,
				new PCEPObjectiveFunctionObjectParser(tlvReg));
		context.registerObjectParser(PCEPClassTypeObjectParser.CLASS, PCEPClassTypeObjectParser.TYPE, new PCEPClassTypeObjectParser(tlvReg));

		context.registerObjectParser(PCEPGlobalConstraintsObjectParser.CLASS, PCEPGlobalConstraintsObjectParser.TYPE,
				new PCEPGlobalConstraintsObjectParser(tlvReg));
		context.registerObjectParser(PCEPLspObjectParser.CLASS, PCEPLspObjectParser.TYPE, new PCEPLspObjectParser(tlvReg));
		context.registerObjectParser(PCEPSrpObjectParser.CLASS, PCEPSrpObjectParser.TYPE, new PCEPSrpObjectParser(tlvReg));
		context.registerObjectParser(PCEPExcludeRouteObjectParser.CLASS, PCEPExcludeRouteObjectParser.TYPE,
				new PCEPExcludeRouteObjectParser(xroSubReg));

		context.registerObjectSerializer(Open.class, new PCEPOpenObjectParser(tlvReg));
		context.registerObjectSerializer(Rp.class, new PCEPRequestParameterObjectParser(tlvReg));
		context.registerObjectSerializer(NoPath.class, new PCEPNoPathObjectParser(tlvReg));
		context.registerObjectSerializer(EndpointsObj.class, new PCEPEndPointsObjectParser(tlvReg));
		context.registerObjectSerializer(Bandwidth.class, new PCEPBandwidthObjectParser(tlvReg));
		context.registerObjectSerializer(Metric.class, new PCEPMetricObjectParser(tlvReg));
		context.registerObjectSerializer(Ero.class, new PCEPExplicitRouteObjectParser(eroSubReg));
		context.registerObjectSerializer(Rro.class, new PCEPReportedRouteObjectParser(rroSubReg));
		context.registerObjectSerializer(Lspa.class, new PCEPLspaObjectParser(tlvReg));
		context.registerObjectSerializer(Iro.class, new PCEPIncludeRouteObjectParser(eroSubReg));
		context.registerObjectSerializer(Svec.class, new PCEPSvecObjectParser(tlvReg));
		context.registerObjectSerializer(CNotification.class, new PCEPNotificationObjectParser(tlvReg));
		context.registerObjectSerializer(ErrorObject.class, new PCEPErrorObjectParser(tlvReg));
		context.registerObjectSerializer(LoadBalancing.class, new PCEPLoadBalancingObjectParser(tlvReg));
		context.registerObjectSerializer(CClose.class, new PCEPCloseObjectParser(tlvReg));
		context.registerObjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.key.object.PathKey.class,
				new PCEPPathKeyObjectParser(eroSubReg));
		context.registerObjectSerializer(Of.class, new PCEPObjectiveFunctionObjectParser(tlvReg));
		context.registerObjectSerializer(ClassType.class, new PCEPClassTypeObjectParser(tlvReg));
		context.registerObjectSerializer(Gc.class, new PCEPGlobalConstraintsObjectParser(tlvReg));
		context.registerObjectSerializer(Lsp.class, new PCEPLspObjectParser(tlvReg));
		context.registerObjectSerializer(Srp.class, new PCEPSrpObjectParser(tlvReg));
		context.registerObjectSerializer(Xro.class, new PCEPExcludeRouteObjectParser(xroSubReg));

		context.registerMessageParser(PCEPOpenMessageParser.TYPE, new PCEPOpenMessageParser(objReg));
		context.registerMessageParser(PCEPKeepAliveMessageParser.TYPE, new PCEPKeepAliveMessageParser(objReg));
		context.registerMessageParser(PCEPReplyMessageParser.TYPE, new PCEPReplyMessageParser(objReg));
		context.registerMessageParser(PCEPRequestMessageParser.TYPE, new PCEPRequestMessageParser(objReg));
		context.registerMessageParser(PCEPErrorMessageParser.TYPE, new PCEPErrorMessageParser(objReg));
		context.registerMessageParser(PCEPCloseMessageParser.TYPE, new PCEPCloseMessageParser(objReg));
		context.registerMessageParser(PCEPUpdateRequestMessageParser.TYPE, new PCEPUpdateRequestMessageParser(objReg));
		context.registerMessageParser(PCEPReportMessageParser.TYPE, new PCEPReportMessageParser(objReg));

		context.registerMessageSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Open.class,
				new PCEPOpenMessageParser(objReg));
		context.registerMessageSerializer(Pcntf.class, new PCEPNotificationMessageParser(objReg));
		context.registerMessageSerializer(Keepalive.class, new PCEPKeepAliveMessageParser(objReg));
		context.registerMessageSerializer(Pcrep.class, new PCEPReplyMessageParser(objReg));
		context.registerMessageSerializer(Pcreq.class, new PCEPRequestMessageParser(objReg));
		context.registerMessageSerializer(Pcerr.class, new PCEPErrorMessageParser(objReg));
		context.registerMessageSerializer(Close.class, new PCEPCloseMessageParser(objReg));
		context.registerMessageSerializer(Pcupd.class, new PCEPUpdateRequestMessageParser(objReg));
		context.registerMessageSerializer(Pcrpt.class, new PCEPReportMessageParser(objReg));

		return regs;
	}
}
