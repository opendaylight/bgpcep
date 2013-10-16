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
import org.opendaylight.protocol.pcep.impl.subobject.EROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.GeneralizedLabelChannelSetParser;
import org.opendaylight.protocol.pcep.impl.subobject.GeneralizedLabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROIpPrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.Type1LabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.WavebandSwitchingLabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROIpPrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROSRLGSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.XROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIPv4TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIPv6TlvParser;
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
import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClasstypeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.EndpointsObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.GcObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.IncludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LoadBalancingObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspDbVersionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspErrorCodeTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspIdentifiersTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspaObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NotificationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfListTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OrderTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OverloadDurationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKeyObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcepErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcinitiateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcntfMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrepMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcreqMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrptMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcupdMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PredundancyGroupIdTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ReportedRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ReqMissingTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RpObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RsvpErrorSpecTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SrpObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.StatefulCapabilityTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SvecObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SymbolicPathNameTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AsNumberSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.GeneralizedChannelSetLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.GeneralizedLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LabelSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.SrlgSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.Type1Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.UnnumberedSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.WavebandSwitchingLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class PCEPImplActivator implements PCEPProviderActivator {
	private static final Logger logger = LoggerFactory.getLogger(PCEPImplActivator.class);
	private List<AutoCloseable> registrations;

	@Override
	public void start(final PCEPProviderContext context) {
		Preconditions.checkState(this.registrations == null);
		final List<AutoCloseable> regs = new ArrayList<>();

		final LabelHandlerRegistry labelReg = context.getLabelHandlerRegistry();
		labelReg.registerLabelParser(Type1LabelParser.CTYPE, new Type1LabelParser());
		labelReg.registerLabelParser(GeneralizedLabelParser.CTYPE, new GeneralizedLabelParser());
		labelReg.registerLabelParser(WavebandSwitchingLabelParser.CTYPE, new WavebandSwitchingLabelParser());
		labelReg.registerLabelParser(GeneralizedLabelChannelSetParser.CTYPE, new GeneralizedLabelChannelSetParser());

		labelReg.registerLabelSerializer(Type1Label.class, new Type1LabelParser());
		labelReg.registerLabelSerializer(GeneralizedLabel.class, new GeneralizedLabelParser());
		labelReg.registerLabelSerializer(WavebandSwitchingLabel.class, new WavebandSwitchingLabelParser());
		labelReg.registerLabelSerializer(GeneralizedChannelSetLabel.class, new GeneralizedLabelChannelSetParser());

		final EROSubobjectHandlerRegistry eroSubReg = context.getEROSubobjectHandlerRegistry();
		eroSubReg.registerSubobjectParser(EROIpPrefixSubobjectParser.TYPE, new EROIpPrefixSubobjectParser());
		eroSubReg.registerSubobjectParser(EROIpPrefixSubobjectParser.TYPE6, new EROIpPrefixSubobjectParser());
		eroSubReg.registerSubobjectParser(EROAsNumberSubobjectParser.TYPE, new EROAsNumberSubobjectParser());
		eroSubReg.registerSubobjectParser(EROLabelSubobjectParser.TYPE, new EROLabelSubobjectParser(labelReg));
		eroSubReg.registerSubobjectParser(EROUnnumberedInterfaceSubobjectParser.TYPE, new EROUnnumberedInterfaceSubobjectParser());

		eroSubReg.registerSubobjectSerializer(IpPrefixSubobject.class, new EROIpPrefixSubobjectParser());
		eroSubReg.registerSubobjectSerializer(AsNumberSubobject.class, new EROAsNumberSubobjectParser());
		eroSubReg.registerSubobjectSerializer(LabelSubobject.class, new EROLabelSubobjectParser(labelReg));
		eroSubReg.registerSubobjectSerializer(UnnumberedSubobject.class, new EROUnnumberedInterfaceSubobjectParser());

		final RROSubobjectHandlerRegistry rroSubReg = context.getRROSubobjectHandlerRegistry();
		rroSubReg.registerSubobjectParser(RROIpPrefixSubobjectParser.TYPE, new RROIpPrefixSubobjectParser());
		rroSubReg.registerSubobjectParser(RROIpPrefixSubobjectParser.TYPE6, new RROIpPrefixSubobjectParser());
		rroSubReg.registerSubobjectParser(RROLabelSubobjectParser.TYPE, new RROLabelSubobjectParser(labelReg));
		rroSubReg.registerSubobjectParser(RROAsNumberSubobjectParser.TYPE, new RROAsNumberSubobjectParser());
		rroSubReg.registerSubobjectParser(RROUnnumberedInterfaceSubobjectParser.TYPE, new RROUnnumberedInterfaceSubobjectParser());

		rroSubReg.registerSubobjectSerializer(IpPrefixSubobject.class, new RROIpPrefixSubobjectParser());
		rroSubReg.registerSubobjectSerializer(LabelSubobject.class, new RROLabelSubobjectParser(labelReg));
		rroSubReg.registerSubobjectSerializer(AsNumberSubobject.class, new RROAsNumberSubobjectParser());
		rroSubReg.registerSubobjectSerializer(UnnumberedSubobject.class, new RROUnnumberedInterfaceSubobjectParser());

		final XROSubobjectHandlerRegistry xroSubReg = context.getXROSubobjectHandlerRegistry();
		xroSubReg.registerSubobjectParser(XROIpPrefixSubobjectParser.TYPE, new XROIpPrefixSubobjectParser());
		xroSubReg.registerSubobjectParser(XROIpPrefixSubobjectParser.TYPE6, new XROIpPrefixSubobjectParser());
		xroSubReg.registerSubobjectParser(XROAsNumberSubobjectParser.TYPE, new XROAsNumberSubobjectParser());
		xroSubReg.registerSubobjectParser(XROSRLGSubobjectParser.TYPE, new XROSRLGSubobjectParser());
		xroSubReg.registerSubobjectParser(XROUnnumberedInterfaceSubobjectParser.TYPE, new XROUnnumberedInterfaceSubobjectParser());

		xroSubReg.registerSubobjectSerializer(IpPrefixSubobject.class, new XROIpPrefixSubobjectParser());
		xroSubReg.registerSubobjectSerializer(AsNumberSubobject.class, new XROAsNumberSubobjectParser());
		xroSubReg.registerSubobjectSerializer(SrlgSubobject.class, new XROSRLGSubobjectParser());
		xroSubReg.registerSubobjectSerializer(UnnumberedSubobject.class, new XROUnnumberedInterfaceSubobjectParser());

		final TlvHandlerRegistry tlvReg = context.getTlvHandlerRegistry();
		tlvReg.registerTlvParser(NoPathVectorTlvParser.TYPE, new NoPathVectorTlvParser());
		tlvReg.registerTlvParser(OverloadedDurationTlvParser.TYPE, new OverloadedDurationTlvParser());
		tlvReg.registerTlvParser(ReqMissingTlvParser.TYPE, new ReqMissingTlvParser());
		tlvReg.registerTlvParser(OFListTlvParser.TYPE, new OFListTlvParser());
		tlvReg.registerTlvParser(OrderTlvParser.TYPE, new OrderTlvParser());
		tlvReg.registerTlvParser(PCEStatefulCapabilityTlvParser.TYPE, new PCEStatefulCapabilityTlvParser());
		tlvReg.registerTlvParser(LspSymbolicNameTlvParser.TYPE, new LspSymbolicNameTlvParser());
		tlvReg.registerTlvParser(LSPIdentifierIPv4TlvParser.TYPE, new LSPIdentifierIPv4TlvParser());
		tlvReg.registerTlvParser(LSPIdentifierIPv6TlvParser.TYPE, new LSPIdentifierIPv6TlvParser());
		tlvReg.registerTlvParser(LspUpdateErrorTlvParser.TYPE, new LspUpdateErrorTlvParser());
		tlvReg.registerTlvParser(RSVPErrorSpecTlvParser.TYPE, new RSVPErrorSpecTlvParser());
		tlvReg.registerTlvParser(LspDbVersionTlvParser.TYPE, new LspDbVersionTlvParser());
		tlvReg.registerTlvParser(PredundancyGroupTlvParser.TYPE, new PredundancyGroupTlvParser());

		tlvReg.registerTlvSerializer(NoPathVectorTlv.class, new NoPathVectorTlvParser());
		tlvReg.registerTlvSerializer(OverloadDurationTlv.class, new OverloadedDurationTlvParser());
		tlvReg.registerTlvSerializer(ReqMissingTlv.class, new ReqMissingTlvParser());
		tlvReg.registerTlvSerializer(OfListTlv.class, new OFListTlvParser());
		tlvReg.registerTlvSerializer(OrderTlv.class, new OrderTlvParser());
		tlvReg.registerTlvSerializer(StatefulCapabilityTlv.class, new PCEStatefulCapabilityTlvParser());
		tlvReg.registerTlvSerializer(SymbolicPathNameTlv.class, new LspSymbolicNameTlvParser());
		tlvReg.registerTlvSerializer(LspIdentifiersTlv.class, new LSPIdentifierIPv4TlvParser());
		tlvReg.registerTlvSerializer(LspErrorCodeTlv.class, new LspUpdateErrorTlvParser());
		tlvReg.registerTlvSerializer(RsvpErrorSpecTlv.class, new RSVPErrorSpecTlvParser());
		tlvReg.registerTlvSerializer(LspDbVersionTlv.class, new LspDbVersionTlvParser());
		tlvReg.registerTlvSerializer(PredundancyGroupIdTlv.class, new PredundancyGroupTlvParser());

		final ObjectHandlerRegistry objReg = context.getObjectHandlerRegistry();
		objReg.registerObjectParser(PCEPOpenObjectParser.CLASS, PCEPOpenObjectParser.TYPE, new PCEPOpenObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPRequestParameterObjectParser.CLASS, PCEPRequestParameterObjectParser.TYPE,
				new PCEPRequestParameterObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPNoPathObjectParser.CLASS, PCEPNoPathObjectParser.TYPE, new PCEPNoPathObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPEndPointsObjectParser.CLASS, PCEPEndPointsObjectParser.TYPE, new PCEPEndPointsObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPEndPointsObjectParser.CLASS_6, PCEPEndPointsObjectParser.TYPE_6,
				new PCEPEndPointsObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPBandwidthObjectParser.CLASS, PCEPBandwidthObjectParser.TYPE, new PCEPBandwidthObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPBandwidthObjectParser.E_CLASS, PCEPBandwidthObjectParser.E_TYPE,
				new PCEPBandwidthObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPMetricObjectParser.CLASS, PCEPMetricObjectParser.TYPE, new PCEPMetricObjectParser(tlvReg));

		objReg.registerObjectParser(PCEPExplicitRouteObjectParser.CLASS, PCEPExplicitRouteObjectParser.TYPE,
				new PCEPExplicitRouteObjectParser(eroSubReg));
		objReg.registerObjectParser(PCEPReportedRouteObjectParser.CLASS, PCEPReportedRouteObjectParser.TYPE,
				new PCEPReportedRouteObjectParser(rroSubReg));
		objReg.registerObjectParser(PCEPLspaObjectParser.CLASS, PCEPLspaObjectParser.TYPE, new PCEPLspaObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPIncludeRouteObjectParser.CLASS, PCEPIncludeRouteObjectParser.TYPE,
				new PCEPIncludeRouteObjectParser(eroSubReg));
		objReg.registerObjectParser(PCEPSvecObjectParser.CLASS, PCEPSvecObjectParser.TYPE, new PCEPSvecObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPNotificationObjectParser.CLASS, PCEPNotificationObjectParser.TYPE,
				new PCEPNotificationObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPErrorObjectParser.CLASS, PCEPErrorObjectParser.TYPE, new PCEPErrorObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPLoadBalancingObjectParser.CLASS, PCEPLoadBalancingObjectParser.TYPE,
				new PCEPLoadBalancingObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPCloseObjectParser.CLASS, PCEPCloseObjectParser.TYPE, new PCEPCloseObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPPathKeyObjectParser.CLASS, PCEPPathKeyObjectParser.TYPE, new PCEPPathKeyObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPObjectiveFunctionObjectParser.CLASS, PCEPObjectiveFunctionObjectParser.TYPE,
				new PCEPObjectiveFunctionObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPClassTypeObjectParser.CLASS, PCEPClassTypeObjectParser.TYPE, new PCEPClassTypeObjectParser(tlvReg));

		objReg.registerObjectParser(PCEPGlobalConstraintsObjectParser.CLASS, PCEPGlobalConstraintsObjectParser.TYPE,
				new PCEPGlobalConstraintsObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPLspObjectParser.CLASS, PCEPLspObjectParser.TYPE, new PCEPLspObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPSrpObjectParser.CLASS, PCEPSrpObjectParser.TYPE, new PCEPSrpObjectParser(tlvReg));
		// objReg.registerObjectParser(PCEPExcludeRouteObjectParser.CLASS, PCEPExcludeRouteObjectParser.TYPE, new
		// PCEPExcludeRouteObjectParser(context.getXROSubobjectHandlerRegistry()));

		objReg.registerObjectSerializer(OpenObject.class, new PCEPOpenObjectParser(tlvReg));
		objReg.registerObjectSerializer(RpObject.class, new PCEPRequestParameterObjectParser(tlvReg));
		objReg.registerObjectSerializer(NoPathObject.class, new PCEPNoPathObjectParser(tlvReg));
		objReg.registerObjectSerializer(EndpointsObject.class, new PCEPEndPointsObjectParser(tlvReg));
		objReg.registerObjectSerializer(BandwidthObject.class, new PCEPBandwidthObjectParser(tlvReg));
		objReg.registerObjectSerializer(MetricObject.class, new PCEPMetricObjectParser(tlvReg));
		objReg.registerObjectSerializer(ExplicitRouteObject.class, new PCEPExplicitRouteObjectParser(eroSubReg));
		objReg.registerObjectSerializer(ReportedRouteObject.class, new PCEPReportedRouteObjectParser(rroSubReg));
		objReg.registerObjectSerializer(LspaObject.class, new PCEPLspaObjectParser(tlvReg));
		objReg.registerObjectSerializer(IncludeRouteObject.class, new PCEPIncludeRouteObjectParser(eroSubReg));
		objReg.registerObjectSerializer(SvecObject.class, new PCEPSvecObjectParser(tlvReg));
		objReg.registerObjectSerializer(NotificationObject.class, new PCEPNotificationObjectParser(tlvReg));
		objReg.registerObjectSerializer(PcepErrorObject.class, new PCEPErrorObjectParser(tlvReg));
		objReg.registerObjectSerializer(LoadBalancingObject.class, new PCEPLoadBalancingObjectParser(tlvReg));
		objReg.registerObjectSerializer(CloseObject.class, new PCEPCloseObjectParser(tlvReg));
		objReg.registerObjectSerializer(PathKeyObject.class, new PCEPPathKeyObjectParser(tlvReg));
		objReg.registerObjectSerializer(OfObject.class, new PCEPObjectiveFunctionObjectParser(tlvReg));
		objReg.registerObjectSerializer(ClasstypeObject.class, new PCEPClassTypeObjectParser(tlvReg));
		objReg.registerObjectSerializer(GcObject.class, new PCEPGlobalConstraintsObjectParser(tlvReg));
		objReg.registerObjectSerializer(LspObject.class, new PCEPLspObjectParser(tlvReg));
		objReg.registerObjectSerializer(SrpObject.class, new PCEPSrpObjectParser(tlvReg));
		// reg.registerObjectSerializer(ExcludeRouteObject.class, new PCEPExcludeRouteObjectParser(xroSubReg));

		final MessageHandlerRegistry msgReg = context.getMessageHandlerRegistry();
		msgReg.registerMessageParser(PCEPOpenMessageParser.TYPE, new PCEPOpenMessageParser(objReg));
		msgReg.registerMessageParser(PCEPKeepAliveMessageParser.TYPE, new PCEPKeepAliveMessageParser(objReg));
		msgReg.registerMessageParser(PCEPReplyMessageParser.TYPE, new PCEPReplyMessageParser(objReg));
		msgReg.registerMessageParser(PCEPRequestMessageParser.TYPE, new PCEPRequestMessageParser(objReg));
		msgReg.registerMessageParser(PCEPErrorMessageParser.TYPE, new PCEPErrorMessageParser(objReg));
		msgReg.registerMessageParser(PCEPCloseMessageParser.TYPE, new PCEPCloseMessageParser(objReg));
		msgReg.registerMessageParser(PCEPUpdateRequestMessageParser.TYPE, new PCEPUpdateRequestMessageParser(objReg));
		msgReg.registerMessageParser(PCEPReportMessageParser.TYPE, new PCEPReportMessageParser(objReg));
		msgReg.registerMessageParser(PCCreateMessageParser.TYPE, new PCCreateMessageParser(objReg));

		msgReg.registerMessageSerializer(OpenMessage.class, new PCEPOpenMessageParser(objReg));
		msgReg.registerMessageSerializer(PcntfMessage.class, new PCEPNotificationMessageParser(objReg));
		msgReg.registerMessageSerializer(KeepaliveMessage.class, new PCEPKeepAliveMessageParser(objReg));
		msgReg.registerMessageSerializer(PcrepMessage.class, new PCEPReplyMessageParser(objReg));
		msgReg.registerMessageSerializer(PcreqMessage.class, new PCEPRequestMessageParser(objReg));
		msgReg.registerMessageSerializer(PcerrMessage.class, new PCEPErrorMessageParser(objReg));
		msgReg.registerMessageSerializer(CloseMessage.class, new PCEPCloseMessageParser(objReg));
		msgReg.registerMessageSerializer(PcupdMessage.class, new PCEPUpdateRequestMessageParser(objReg));
		msgReg.registerMessageSerializer(PcrptMessage.class, new PCEPReportMessageParser(objReg));
		msgReg.registerMessageSerializer(PcinitiateMessage.class, new PCCreateMessageParser(objReg));

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
