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
import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPProviderContext;
import org.opendaylight.protocol.pcep.spi.SubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.BandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ClasstypeObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.EndpointsObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.GcObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.IncludeRouteObject;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OrderTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OverloadDurationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKeyObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcepErrorObject;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class PCEPImplActivator implements PCEPProviderActivator {
	private static final Logger logger = LoggerFactory.getLogger(PCEPImplActivator.class);
	private List<AutoCloseable> registrations;

	@Override
	public void start(final PCEPProviderContext context) {
		Preconditions.checkState(registrations == null);
		final List<AutoCloseable> regs = new ArrayList<>();

		final SubobjectHandlerRegistry subReg = context.getSubobjectHandlerRegistry();
		subReg.registerSubobjectParser(EROAsNumberSubobjectParser.TYPE, new EROAsNumberSubobjectParser());
		subReg.registerSubobjectSerializer(AsNumberSubobject.class, new EROAsNumberSubobjectParser());

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
		objReg.registerObjectParser(PCEPOpenObjectParser.CLASS, PCEPOpenObjectParser.TYPE, new PCEPOpenObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPRequestParameterObjectParser.CLASS, PCEPRequestParameterObjectParser.TYPE,
				new PCEPRequestParameterObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPNoPathObjectParser.CLASS, PCEPNoPathObjectParser.TYPE, new PCEPNoPathObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPEndPointsObjectParser.CLASS, PCEPEndPointsObjectParser.TYPE, new PCEPEndPointsObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPEndPointsObjectParser.CLASS_6, PCEPEndPointsObjectParser.TYPE_6, new PCEPEndPointsObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPBandwidthObjectParser.CLASS, PCEPBandwidthObjectParser.TYPE, new PCEPBandwidthObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPBandwidthObjectParser.E_CLASS, PCEPBandwidthObjectParser.E_TYPE, new PCEPBandwidthObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPMetricObjectParser.CLASS, PCEPMetricObjectParser.TYPE, new PCEPMetricObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPExplicitRouteObjectParser.CLASS, PCEPExplicitRouteObjectParser.TYPE,
				new PCEPExplicitRouteObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPReportedRouteObjectParser.CLASS, PCEPReportedRouteObjectParser.TYPE,
				new PCEPReportedRouteObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPLspaObjectParser.CLASS, PCEPLspaObjectParser.TYPE, new PCEPLspaObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPIncludeRouteObjectParser.CLASS, PCEPIncludeRouteObjectParser.TYPE,
				new PCEPIncludeRouteObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPSvecObjectParser.CLASS, PCEPSvecObjectParser.TYPE, new PCEPSvecObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPNotificationObjectParser.CLASS, PCEPNotificationObjectParser.TYPE,
				new PCEPNotificationObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPErrorObjectParser.CLASS, PCEPErrorObjectParser.TYPE, new PCEPErrorObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPLoadBalancingObjectParser.CLASS, PCEPLoadBalancingObjectParser.TYPE,
				new PCEPLoadBalancingObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPCloseObjectParser.CLASS, PCEPCloseObjectParser.TYPE, new PCEPCloseObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPPathKeyObjectParser.CLASS, PCEPPathKeyObjectParser.TYPE, new PCEPPathKeyObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPObjectiveFunctionObjectParser.CLASS, PCEPObjectiveFunctionObjectParser.TYPE,
				new PCEPObjectiveFunctionObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPClassTypeObjectParser.CLASS, PCEPClassTypeObjectParser.TYPE, new PCEPClassTypeObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPGlobalConstraintsObjectParser.CLASS, PCEPGlobalConstraintsObjectParser.TYPE,
				new PCEPGlobalConstraintsObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPLspObjectParser.CLASS, PCEPLspObjectParser.TYPE, new PCEPLspObjectParser(subReg, tlvReg));
		objReg.registerObjectParser(PCEPSrpObjectParser.CLASS, PCEPSrpObjectParser.TYPE, new PCEPSrpObjectParser(subReg, tlvReg));
		// objReg.registerObjectParser(PCEPExcludeRouteObjectParser.CLASS, PCEPExcludeRouteObjectParser.TYPE, new
		// PCEPExcludeRouteObjectParser(reg));

		objReg.registerObjectSerializer(OpenObject.class, new PCEPOpenObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(RpObject.class, new PCEPRequestParameterObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(NoPathObject.class, new PCEPNoPathObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(EndpointsObject.class, new PCEPEndPointsObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(BandwidthObject.class, new PCEPBandwidthObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(MetricObject.class, new PCEPMetricObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(ExplicitRouteObject.class, new PCEPExplicitRouteObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(ReportedRouteObject.class, new PCEPReportedRouteObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(LspaObject.class, new PCEPLspaObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(IncludeRouteObject.class, new PCEPIncludeRouteObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(SvecObject.class, new PCEPSvecObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(NotificationObject.class, new PCEPNotificationObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(PcepErrorObject.class, new PCEPErrorObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(LoadBalancingObject.class, new PCEPLoadBalancingObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(CloseObject.class, new PCEPCloseObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(PathKeyObject.class, new PCEPPathKeyObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(OfObject.class, new PCEPObjectiveFunctionObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(ClasstypeObject.class, new PCEPClassTypeObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(GcObject.class, new PCEPGlobalConstraintsObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(LspObject.class, new PCEPLspObjectParser(subReg, tlvReg));
		objReg.registerObjectSerializer(SrpObject.class, new PCEPSrpObjectParser(subReg, tlvReg));
		// reg.registerObjectSerializer(ExcludeRouteObject.class, new PCEPExcludeRouteObjectParser(reg));

		final MessageHandlerRegistry msgReg = context.getMessageHandlerRegistry();
		// FIXME: finish this
		// msgReg.registerMessageHandler(PCEPOpenMessage.class, 1, new PCEPOpenMessageParser());
		// msgReg.registerMessageHandler(PCEPNotificationMessage.class, 5, new PCEPNotificationMessageParser());
		// msgReg.registerMessageHandler(PCEPKeepAliveMessage.class, 2, new PCEPKeepAliveMessageParser());
		// msgReg.registerMessageHandler(PCEPReplyMessage.class, 4, new PCEPReplyMessageParser());
		// msgReg.registerMessageHandler(PCEPRequestMessage.class, 3, new PCEPRequestMessageParser());
		// msgReg.registerMessageHandler(PCEPErrorMessage.class, 6, new PCEPErrorMessageParser());
		// msgReg.registerMessageHandler(PCEPCloseMessage.class, 7, new PCEPCloseMessageParser());
		// msgReg.registerMessageHandler(PCEPUpdateRequestMessage.class, 11, new PCEPUpdateRequestMessageParser());
		// msgReg.registerMessageHandler(PCEPReportMessage.class, 10, new PCEPReportMessageParser());
		// msgReg.registerMessageHandler(PCCreateMessage.class, 12, new PCCreateMessageParser());

		registrations = regs;
	}

	@Override
	public void stop() {
		Preconditions.checkState(registrations != null);

		for (AutoCloseable r : registrations) {
			try {
				r.close();
			} catch (Exception e) {
				logger.warn("Failed to close registration", e);
			}
		}

		registrations = null;
	}
}
