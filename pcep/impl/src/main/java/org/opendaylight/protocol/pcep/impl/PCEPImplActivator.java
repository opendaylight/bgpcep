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

		final SubobjectHandlerRegistry eroSubReg = context.getEROSubobjectHandlerRegistry();
		eroSubReg.registerSubobjectParser(EROAsNumberSubobjectParser.TYPE, new EROAsNumberSubobjectParser());
		eroSubReg.registerSubobjectSerializer(AsNumberSubobject.class, new EROAsNumberSubobjectParser());

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
		objReg.registerObjectParser(PCEPEndPointsObjectParser.CLASS_6, PCEPEndPointsObjectParser.TYPE_6, new PCEPEndPointsObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPBandwidthObjectParser.CLASS, PCEPBandwidthObjectParser.TYPE, new PCEPBandwidthObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPBandwidthObjectParser.E_CLASS, PCEPBandwidthObjectParser.E_TYPE, new PCEPBandwidthObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPMetricObjectParser.CLASS, PCEPMetricObjectParser.TYPE, new PCEPMetricObjectParser(tlvReg));
		objReg.registerObjectParser(PCEPExplicitRouteObjectParser.CLASS, PCEPExplicitRouteObjectParser.TYPE,
				new PCEPExplicitRouteObjectParser(context.getEROSubobjectHandlerRegistry()));
		objReg.registerObjectParser(PCEPReportedRouteObjectParser.CLASS, PCEPReportedRouteObjectParser.TYPE,
				new PCEPReportedRouteObjectParser(context.getRROSubobjectHandlerRegistry()));
		objReg.registerObjectParser(PCEPLspaObjectParser.CLASS, PCEPLspaObjectParser.TYPE, new PCEPLspaObjectParser( tlvReg));
		objReg.registerObjectParser(PCEPIncludeRouteObjectParser.CLASS, PCEPIncludeRouteObjectParser.TYPE,
				new PCEPIncludeRouteObjectParser(context.getEROSubobjectHandlerRegistry()));
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
		objReg.registerObjectSerializer(ExplicitRouteObject.class, new PCEPExplicitRouteObjectParser(context.getEROSubobjectHandlerRegistry()));
		objReg.registerObjectSerializer(ReportedRouteObject.class, new PCEPReportedRouteObjectParser(context.getRROSubobjectHandlerRegistry()));
		objReg.registerObjectSerializer(LspaObject.class, new PCEPLspaObjectParser(tlvReg));
		objReg.registerObjectSerializer(IncludeRouteObject.class, new PCEPIncludeRouteObjectParser(context.getEROSubobjectHandlerRegistry()));
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
