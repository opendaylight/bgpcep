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

import org.opendaylight.protocol.pcep.impl.message.PCCreateMessageParser;
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
import org.opendaylight.protocol.pcep.impl.subobject.RROPathKeySubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.Type1LabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.WavebandSwitchingLabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIpPrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROPathKeySubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROSRLGSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierTlvParser;
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
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClasstypeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.EndpointsObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExcludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.GcObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.IncludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LoadBalancingObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspaObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKeyObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcinitiateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcntfMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrepMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcreqMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrptMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcupdMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ReportedRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SvecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CClose;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.subobjects.subobject.type.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.error.code.tlv.LspErrorCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.identifiers.tlv.LspIdentifiers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure.no.path.tlvs.NoPathVector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.predundancy.group.id.tlv.PredundancyGroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rsvp.error.spec.tlv.RsvpErrorSpec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.stateful.capability.tlv.Stateful;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.symbolic.path.name.tlv.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.Srlg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.subobject.type.Unnumbered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.GeneralizedLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.Type1Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.label.type.WavebandSwitchingLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class Activator implements PCEPExtensionProviderActivator {
	private static final Logger logger = LoggerFactory.getLogger(Activator.class);
	private List<AutoCloseable> registrations;

	@Override
	public void start(final PCEPExtensionProviderContext context) {
		Preconditions.checkState(this.registrations == null);
		final List<AutoCloseable> regs = new ArrayList<>();

		final LabelHandlerRegistry labelReg = context.getLabelHandlerRegistry();
		context.registerLabelParser(Type1LabelParser.CTYPE, new Type1LabelParser());
		context.registerLabelParser(GeneralizedLabelParser.CTYPE, new GeneralizedLabelParser());
		context.registerLabelParser(WavebandSwitchingLabelParser.CTYPE, new WavebandSwitchingLabelParser());

		context.registerLabelSerializer(Type1Label.class, new Type1LabelParser());
		context.registerLabelSerializer(GeneralizedLabel.class, new GeneralizedLabelParser());
		context.registerLabelSerializer(WavebandSwitchingLabel.class, new WavebandSwitchingLabelParser());

		final EROSubobjectHandlerRegistry eroSubReg = context.getEROSubobjectHandlerRegistry();
		context.registerEROSubobjectParser(EROIpPrefixSubobjectParser.TYPE, new EROIpPrefixSubobjectParser());
		context.registerEROSubobjectParser(EROIpPrefixSubobjectParser.TYPE6, new EROIpPrefixSubobjectParser());
		context.registerEROSubobjectParser(EROAsNumberSubobjectParser.TYPE, new EROAsNumberSubobjectParser());
		context.registerEROSubobjectParser(EROLabelSubobjectParser.TYPE, new EROLabelSubobjectParser(labelReg));
		context.registerEROSubobjectParser(EROUnnumberedInterfaceSubobjectParser.TYPE, new EROUnnumberedInterfaceSubobjectParser());
		context.registerEROSubobjectParser(EROPathKeySubobjectParser.TYPE, new EROPathKeySubobjectParser());
		context.registerEROSubobjectParser(EROPathKeySubobjectParser.TYPE128, new EROPathKeySubobjectParser());

		context.registerEROSubobjectSerializer(IpPrefix.class, new EROIpPrefixSubobjectParser());
		context.registerEROSubobjectSerializer(AsNumber.class, new EROAsNumberSubobjectParser());
		context.registerEROSubobjectSerializer(Label.class, new EROLabelSubobjectParser(labelReg));
		context.registerEROSubobjectSerializer(Unnumbered.class, new EROUnnumberedInterfaceSubobjectParser());
		context.registerEROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.subobjects.subobject.type.PathKey.class,
				new EROPathKeySubobjectParser());

		final RROSubobjectHandlerRegistry rroSubReg = context.getRROSubobjectHandlerRegistry();
		context.registerRROSubobjectParser(RROIpPrefixSubobjectParser.TYPE, new RROIpPrefixSubobjectParser());
		context.registerRROSubobjectParser(RROIpPrefixSubobjectParser.TYPE6, new RROIpPrefixSubobjectParser());
		context.registerRROSubobjectParser(RROLabelSubobjectParser.TYPE, new RROLabelSubobjectParser(labelReg));
		context.registerRROSubobjectParser(RROUnnumberedInterfaceSubobjectParser.TYPE, new RROUnnumberedInterfaceSubobjectParser());
		context.registerRROSubobjectParser(RROPathKeySubobjectParser.TYPE, new RROPathKeySubobjectParser());
		context.registerRROSubobjectParser(RROPathKeySubobjectParser.TYPE128, new RROPathKeySubobjectParser());

		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.IpPrefix.class,
				new RROIpPrefixSubobjectParser());
		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.Label.class,
				new RROLabelSubobjectParser(labelReg));
		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.Unnumbered.class,
				new RROUnnumberedInterfaceSubobjectParser());
		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.subobjects.subobject.type.PathKey.class,
				new RROPathKeySubobjectParser());

		final XROSubobjectHandlerRegistry xroSubReg = context.getXROSubobjectHandlerRegistry();
		context.registerXROSubobjectParser(XROIpPrefixSubobjectParser.TYPE, new XROIpPrefixSubobjectParser());
		context.registerXROSubobjectParser(XROIpPrefixSubobjectParser.TYPE6, new XROIpPrefixSubobjectParser());
		context.registerXROSubobjectParser(XROAsNumberSubobjectParser.TYPE, new XROAsNumberSubobjectParser());
		context.registerXROSubobjectParser(XROSRLGSubobjectParser.TYPE, new XROSRLGSubobjectParser());
		context.registerXROSubobjectParser(XROUnnumberedInterfaceSubobjectParser.TYPE, new XROUnnumberedInterfaceSubobjectParser());
		context.registerXROSubobjectParser(XROPathKeySubobjectParser.TYPE, new XROPathKeySubobjectParser());
		context.registerXROSubobjectParser(XROPathKeySubobjectParser.TYPE128, new XROPathKeySubobjectParser());

		context.registerXROSubobjectSerializer(IpPrefix.class, new XROIpPrefixSubobjectParser());
		context.registerXROSubobjectSerializer(AsNumber.class, new XROAsNumberSubobjectParser());
		context.registerXROSubobjectSerializer(Srlg.class, new XROSRLGSubobjectParser());
		context.registerXROSubobjectSerializer(Unnumbered.class, new XROUnnumberedInterfaceSubobjectParser());
		context.registerXROSubobjectSerializer(PathKey.class, new XROPathKeySubobjectParser());

		final TlvHandlerRegistry tlvReg = context.getTlvHandlerRegistry();
		context.registerTlvParser(NoPathVectorTlvParser.TYPE, new NoPathVectorTlvParser());
		context.registerTlvParser(OverloadedDurationTlvParser.TYPE, new OverloadedDurationTlvParser());
		context.registerTlvParser(ReqMissingTlvParser.TYPE, new ReqMissingTlvParser());
		context.registerTlvParser(OFListTlvParser.TYPE, new OFListTlvParser());
		context.registerTlvParser(OrderTlvParser.TYPE, new OrderTlvParser());
		context.registerTlvParser(PCEStatefulCapabilityTlvParser.TYPE, new PCEStatefulCapabilityTlvParser());
		context.registerTlvParser(LspSymbolicNameTlvParser.TYPE, new LspSymbolicNameTlvParser());
		context.registerTlvParser(LSPIdentifierTlvParser.TYPE, new LSPIdentifierTlvParser());
		context.registerTlvParser(LSPIdentifierTlvParser.TYPE_6, new LSPIdentifierTlvParser());
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
		context.registerTlvSerializer(LspIdentifiers.class, new LSPIdentifierTlvParser());
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
		context.registerObjectSerializer(NoPathObject.class, new PCEPNoPathObjectParser(tlvReg));
		context.registerObjectSerializer(EndpointsObject.class, new PCEPEndPointsObjectParser(tlvReg));
		context.registerObjectSerializer(BandwidthObject.class, new PCEPBandwidthObjectParser(tlvReg));
		context.registerObjectSerializer(MetricObject.class, new PCEPMetricObjectParser(tlvReg));
		context.registerObjectSerializer(ExplicitRouteObject.class, new PCEPExplicitRouteObjectParser(eroSubReg));
		context.registerObjectSerializer(ReportedRouteObject.class, new PCEPReportedRouteObjectParser(rroSubReg));
		context.registerObjectSerializer(LspaObject.class, new PCEPLspaObjectParser(tlvReg));
		context.registerObjectSerializer(IncludeRouteObject.class, new PCEPIncludeRouteObjectParser(eroSubReg));
		context.registerObjectSerializer(SvecObject.class, new PCEPSvecObjectParser(tlvReg));
		context.registerObjectSerializer(CNotification.class, new PCEPNotificationObjectParser(tlvReg));
		context.registerObjectSerializer(ErrorObject.class, new PCEPErrorObjectParser(tlvReg));
		context.registerObjectSerializer(LoadBalancingObject.class, new PCEPLoadBalancingObjectParser(tlvReg));
		context.registerObjectSerializer(CClose.class, new PCEPCloseObjectParser(tlvReg));
		context.registerObjectSerializer(PathKeyObject.class, new PCEPPathKeyObjectParser(eroSubReg));
		context.registerObjectSerializer(OfObject.class, new PCEPObjectiveFunctionObjectParser(tlvReg));
		context.registerObjectSerializer(ClasstypeObject.class, new PCEPClassTypeObjectParser(tlvReg));
		context.registerObjectSerializer(GcObject.class, new PCEPGlobalConstraintsObjectParser(tlvReg));
		context.registerObjectSerializer(Lsp.class, new PCEPLspObjectParser(tlvReg));
		context.registerObjectSerializer(Srp.class, new PCEPSrpObjectParser(tlvReg));
		context.registerObjectSerializer(ExcludeRouteObject.class, new PCEPExcludeRouteObjectParser(xroSubReg));

		context.registerMessageParser(PCEPOpenMessageParser.TYPE, new PCEPOpenMessageParser(objReg));
		context.registerMessageParser(PCEPKeepAliveMessageParser.TYPE, new PCEPKeepAliveMessageParser(objReg));
		context.registerMessageParser(PCEPReplyMessageParser.TYPE, new PCEPReplyMessageParser(objReg));
		context.registerMessageParser(PCEPRequestMessageParser.TYPE, new PCEPRequestMessageParser(objReg));
		context.registerMessageParser(PCEPErrorMessageParser.TYPE, new PCEPErrorMessageParser(objReg));
		context.registerMessageParser(PCEPCloseMessageParser.TYPE, new PCEPCloseMessageParser(objReg));
		context.registerMessageParser(PCEPUpdateRequestMessageParser.TYPE, new PCEPUpdateRequestMessageParser(objReg));
		context.registerMessageParser(PCEPReportMessageParser.TYPE, new PCEPReportMessageParser(objReg));
		context.registerMessageParser(PCCreateMessageParser.TYPE, new PCCreateMessageParser(objReg));

		context.registerMessageSerializer(OpenMessage.class, new PCEPOpenMessageParser(objReg));
		context.registerMessageSerializer(PcntfMessage.class, new PCEPNotificationMessageParser(objReg));
		context.registerMessageSerializer(KeepaliveMessage.class, new PCEPKeepAliveMessageParser(objReg));
		context.registerMessageSerializer(PcrepMessage.class, new PCEPReplyMessageParser(objReg));
		context.registerMessageSerializer(PcreqMessage.class, new PCEPRequestMessageParser(objReg));
		context.registerMessageSerializer(PcerrMessage.class, new PCEPErrorMessageParser(objReg));
		context.registerMessageSerializer(CloseMessage.class, new PCEPCloseMessageParser(objReg));
		context.registerMessageSerializer(PcupdMessage.class, new PCEPUpdateRequestMessageParser(objReg));
		context.registerMessageSerializer(PcrptMessage.class, new PCEPReportMessageParser(objReg));
		context.registerMessageSerializer(PcinitiateMessage.class, new PCCreateMessageParser(objReg));

		this.registrations = regs;
	}

	@Override
	public void stop() {
		Preconditions.checkState(this.registrations != null);

		for (final AutoCloseable r : this.registrations) {
			try {
				r.close();
			} catch (final Exception e) {
				logger.warn("Failed to close registration", e);
			}
		}

		this.registrations = null;
	}
}
