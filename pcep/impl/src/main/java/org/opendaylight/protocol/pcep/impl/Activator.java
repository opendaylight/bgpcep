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
import org.opendaylight.protocol.pcep.impl.object.PCEPBandwidthObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPClassTypeObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPCloseObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPEndPointsIpv4ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPEndPointsIpv6ObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPErrorObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExcludeRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExistingBandwidthObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPExplicitRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPGlobalConstraintsObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPIncludeRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLoadBalancingObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPLspaObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPMetricObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNoPathObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPNotificationObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPObjectiveFunctionObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPPathKeyObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPReportedRouteObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPRequestParameterObjectParser;
import org.opendaylight.protocol.pcep.impl.object.PCEPSvecObjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROAsNumberSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROIpv6PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROLabelSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROPathKey128SubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROPathKey32SubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.EROUnnumberedInterfaceSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.GeneralizedLabelParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROIpv4PrefixSubobjectParser;
import org.opendaylight.protocol.pcep.impl.subobject.RROIpv6PrefixSubobjectParser;
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
import org.opendaylight.protocol.pcep.impl.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.ReqMissingTlvParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.TlvRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.AbstractPCEPExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Close;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcntf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.close.object.CClose;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.exclude.route.object.Xro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.subobject.subobject.type.PathKeyCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.gc.object.Gc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.load.balancing.object.LoadBalancing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.metric.object.Metric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.list.tlv.OfList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.order.tlv.Order;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.overload.duration.tlv.OverloadDuration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.no.path.tlvs.NoPathVector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.req.missing.tlv.ReqMissing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.svec.object.Svec;
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
		context.registerEROSubobjectParser(EROIpv4PrefixSubobjectParser.TYPE, new EROIpv4PrefixSubobjectParser());
		context.registerEROSubobjectParser(EROIpv6PrefixSubobjectParser.TYPE, new EROIpv6PrefixSubobjectParser());
		context.registerEROSubobjectParser(EROAsNumberSubobjectParser.TYPE, new EROAsNumberSubobjectParser());
		context.registerEROSubobjectParser(EROLabelSubobjectParser.TYPE, new EROLabelSubobjectParser(labelReg));
		context.registerEROSubobjectParser(EROUnnumberedInterfaceSubobjectParser.TYPE, new EROUnnumberedInterfaceSubobjectParser());
		context.registerEROSubobjectParser(EROPathKey32SubobjectParser.TYPE, new EROPathKey32SubobjectParser());
		context.registerEROSubobjectParser(EROPathKey128SubobjectParser.TYPE, new EROPathKey128SubobjectParser());

		context.registerEROSubobjectSerializer(IpPrefixCase.class, new EROIpv4PrefixSubobjectParser());
		context.registerEROSubobjectSerializer(AsNumberCase.class, new EROAsNumberSubobjectParser());
		context.registerEROSubobjectSerializer(LabelCase.class, new EROLabelSubobjectParser(labelReg));
		context.registerEROSubobjectSerializer(UnnumberedCase.class, new EROUnnumberedInterfaceSubobjectParser());
		context.registerEROSubobjectSerializer(PathKeyCase.class, new EROPathKey32SubobjectParser());

		final RROSubobjectRegistry rroSubReg = context.getRROSubobjectHandlerRegistry();
		context.registerRROSubobjectParser(RROIpv4PrefixSubobjectParser.TYPE, new RROIpv4PrefixSubobjectParser());
		context.registerRROSubobjectParser(RROIpv6PrefixSubobjectParser.TYPE, new RROIpv6PrefixSubobjectParser());
		context.registerRROSubobjectParser(RROLabelSubobjectParser.TYPE, new RROLabelSubobjectParser(labelReg));
		context.registerRROSubobjectParser(RROUnnumberedInterfaceSubobjectParser.TYPE, new RROUnnumberedInterfaceSubobjectParser());
		context.registerRROSubobjectParser(RROPathKey32SubobjectParser.TYPE, new RROPathKey32SubobjectParser());
		context.registerRROSubobjectParser(RROPathKey128SubobjectParser.TYPE, new RROPathKey128SubobjectParser());

		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.IpPrefixCase.class,
				new RROIpv4PrefixSubobjectParser());
		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.LabelCase.class,
				new RROLabelSubobjectParser(labelReg));
		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.subobject.type.UnnumberedCase.class,
				new RROUnnumberedInterfaceSubobjectParser());
		context.registerRROSubobjectSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.rro.subobject.subobject.type.PathKeyCase.class,
				new RROPathKey32SubobjectParser());

		final XROSubobjectRegistry xroSubReg = context.getXROSubobjectHandlerRegistry();
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

		final TlvRegistry tlvReg = context.getTlvHandlerRegistry();
		context.registerTlvParser(NoPathVectorTlvParser.TYPE, new NoPathVectorTlvParser());
		context.registerTlvParser(OverloadedDurationTlvParser.TYPE, new OverloadedDurationTlvParser());
		context.registerTlvParser(ReqMissingTlvParser.TYPE, new ReqMissingTlvParser());
		context.registerTlvParser(OFListTlvParser.TYPE, new OFListTlvParser());
		context.registerTlvParser(OrderTlvParser.TYPE, new OrderTlvParser());

		context.registerTlvSerializer(NoPathVector.class, new NoPathVectorTlvParser());
		context.registerTlvSerializer(OverloadDuration.class, new OverloadedDurationTlvParser());
		context.registerTlvSerializer(ReqMissing.class, new ReqMissingTlvParser());
		context.registerTlvSerializer(OfList.class, new OFListTlvParser());
		context.registerTlvSerializer(Order.class, new OrderTlvParser());

		final ObjectHandlerRegistry objReg = context.getObjectHandlerRegistry();
		context.registerObjectParser(PCEPRequestParameterObjectParser.CLASS, PCEPRequestParameterObjectParser.TYPE,
				new PCEPRequestParameterObjectParser(tlvReg));
		context.registerObjectParser(PCEPNoPathObjectParser.CLASS, PCEPNoPathObjectParser.TYPE, new PCEPNoPathObjectParser(tlvReg));
		context.registerObjectParser(PCEPEndPointsIpv4ObjectParser.CLASS, PCEPEndPointsIpv4ObjectParser.TYPE,
				new PCEPEndPointsIpv4ObjectParser(tlvReg));
		context.registerObjectParser(PCEPEndPointsIpv6ObjectParser.CLASS, PCEPEndPointsIpv6ObjectParser.TYPE,
				new PCEPEndPointsIpv4ObjectParser(tlvReg));
		context.registerObjectParser(PCEPBandwidthObjectParser.CLASS, PCEPBandwidthObjectParser.TYPE, new PCEPBandwidthObjectParser(tlvReg));
		context.registerObjectParser(PCEPExistingBandwidthObjectParser.CLASS, PCEPExistingBandwidthObjectParser.TYPE,
				new PCEPExistingBandwidthObjectParser(tlvReg));
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
		context.registerObjectParser(PCEPExcludeRouteObjectParser.CLASS, PCEPExcludeRouteObjectParser.TYPE,
				new PCEPExcludeRouteObjectParser(xroSubReg));

		context.registerObjectSerializer(Rp.class, new PCEPRequestParameterObjectParser(tlvReg));
		context.registerObjectSerializer(NoPath.class, new PCEPNoPathObjectParser(tlvReg));
		context.registerObjectSerializer(EndpointsObj.class, new PCEPEndPointsIpv4ObjectParser(tlvReg));
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
		context.registerObjectSerializer(Xro.class, new PCEPExcludeRouteObjectParser(xroSubReg));

		context.registerMessageParser(PCEPOpenMessageParser.TYPE, new PCEPOpenMessageParser(objReg));
		context.registerMessageParser(PCEPKeepAliveMessageParser.TYPE, new PCEPKeepAliveMessageParser(objReg));
		context.registerMessageParser(PCEPErrorMessageParser.TYPE, new PCEPErrorMessageParser(objReg));
		context.registerMessageParser(PCEPCloseMessageParser.TYPE, new PCEPCloseMessageParser(objReg));

		context.registerMessageSerializer(
				org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Open.class,
				new PCEPOpenMessageParser(objReg));
		context.registerMessageSerializer(Pcntf.class, new PCEPNotificationMessageParser(objReg));
		context.registerMessageSerializer(Keepalive.class, new PCEPKeepAliveMessageParser(objReg));
		context.registerMessageSerializer(Pcerr.class, new PCEPErrorMessageParser(objReg));
		context.registerMessageSerializer(Close.class, new PCEPCloseMessageParser(objReg));

		return regs;
	}
}
