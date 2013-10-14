/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.concepts.HandlerRegistry;
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
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspaObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NotificationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PathKeyObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcepErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ReportedRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RpObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SrpObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SvecObject;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class SimpleObjectHandlerRegistry implements ObjectHandlerRegistry {
	public static final ObjectHandlerRegistry INSTANCE;

	static {
		final ObjectHandlerRegistry reg = new SimpleObjectHandlerRegistry();

		final SubobjectHandlerRegistry subobjReg = SimpleSubobjectHandlerFactory.INSTANCE;
		final TlvHandlerRegistry tlvReg = SimpleTlvHandlerRegistry.INSTANCE;

		reg.registerObjectParser(PCEPOpenObjectParser.CLASS, PCEPOpenObjectParser.TYPE, new PCEPOpenObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPRequestParameterObjectParser.CLASS, PCEPRequestParameterObjectParser.TYPE,
				new PCEPRequestParameterObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPNoPathObjectParser.CLASS, PCEPNoPathObjectParser.TYPE, new PCEPNoPathObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPEndPointsObjectParser.CLASS, PCEPEndPointsObjectParser.TYPE, new PCEPEndPointsObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPEndPointsObjectParser.CLASS_6, PCEPEndPointsObjectParser.TYPE_6, new PCEPEndPointsObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPBandwidthObjectParser.CLASS, PCEPBandwidthObjectParser.TYPE, new PCEPBandwidthObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPBandwidthObjectParser.E_CLASS, PCEPBandwidthObjectParser.E_TYPE, new PCEPBandwidthObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPMetricObjectParser.CLASS, PCEPMetricObjectParser.TYPE, new PCEPMetricObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPExplicitRouteObjectParser.CLASS, PCEPExplicitRouteObjectParser.TYPE,
				new PCEPExplicitRouteObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPReportedRouteObjectParser.CLASS, PCEPReportedRouteObjectParser.TYPE,
				new PCEPReportedRouteObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPLspaObjectParser.CLASS, PCEPLspaObjectParser.TYPE, new PCEPLspaObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPIncludeRouteObjectParser.CLASS, PCEPIncludeRouteObjectParser.TYPE,
				new PCEPIncludeRouteObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPSvecObjectParser.CLASS, PCEPSvecObjectParser.TYPE, new PCEPSvecObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPNotificationObjectParser.CLASS, PCEPNotificationObjectParser.TYPE,
				new PCEPNotificationObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPErrorObjectParser.CLASS, PCEPErrorObjectParser.TYPE, new PCEPErrorObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPLoadBalancingObjectParser.CLASS, PCEPLoadBalancingObjectParser.TYPE,
				new PCEPLoadBalancingObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPCloseObjectParser.CLASS, PCEPCloseObjectParser.TYPE, new PCEPCloseObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPPathKeyObjectParser.CLASS, PCEPPathKeyObjectParser.TYPE, new PCEPPathKeyObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPObjectiveFunctionObjectParser.CLASS, PCEPObjectiveFunctionObjectParser.TYPE,
				new PCEPObjectiveFunctionObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPClassTypeObjectParser.CLASS, PCEPClassTypeObjectParser.TYPE, new PCEPClassTypeObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPGlobalConstraintsObjectParser.CLASS, PCEPGlobalConstraintsObjectParser.TYPE,
				new PCEPGlobalConstraintsObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPLspObjectParser.CLASS, PCEPLspObjectParser.TYPE, new PCEPLspObjectParser(subobjReg, tlvReg));
		reg.registerObjectParser(PCEPSrpObjectParser.CLASS, PCEPSrpObjectParser.TYPE, new PCEPSrpObjectParser(subobjReg, tlvReg));
		// reg.registerObjectParser(PCEPExcludeRouteObjectParser.CLASS, PCEPExcludeRouteObjectParser.TYPE, new
		// PCEPExcludeRouteObjectParser(reg));

		reg.registerObjectSerializer(OpenObject.class, new PCEPOpenObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(RpObject.class, new PCEPRequestParameterObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(NoPathObject.class, new PCEPNoPathObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(EndpointsObject.class, new PCEPEndPointsObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(BandwidthObject.class, new PCEPBandwidthObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(MetricObject.class, new PCEPMetricObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(ExplicitRouteObject.class, new PCEPExplicitRouteObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(ReportedRouteObject.class, new PCEPReportedRouteObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(LspaObject.class, new PCEPLspaObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(IncludeRouteObject.class, new PCEPIncludeRouteObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(SvecObject.class, new PCEPSvecObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(NotificationObject.class, new PCEPNotificationObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(PcepErrorObject.class, new PCEPErrorObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(LoadBalancingObject.class, new PCEPLoadBalancingObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(CloseObject.class, new PCEPCloseObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(PathKeyObject.class, new PCEPPathKeyObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(OfObject.class, new PCEPObjectiveFunctionObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(ClasstypeObject.class, new PCEPClassTypeObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(GcObject.class, new PCEPGlobalConstraintsObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(LspObject.class, new PCEPLspObjectParser(subobjReg, tlvReg));
		reg.registerObjectSerializer(SrpObject.class, new PCEPSrpObjectParser(subobjReg, tlvReg));
		// reg.registerObjectSerializer(ExcludeRouteObject.class, new PCEPExcludeRouteObjectParser(reg));

		INSTANCE = reg;
	}

	private final HandlerRegistry<DataContainer, ObjectParser, ObjectSerializer> handlers = new HandlerRegistry<>();

	private static final int createKey(final int objectClass, final int objectType) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= 255);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);
		return (objectClass << 4) | objectType;
	}

	@Override
	public AutoCloseable registerObjectParser(final int objectClass, final int objectType, final ObjectParser parser) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= 255);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);
		return handlers.registerParser(createKey(objectClass, objectType), parser);
	}

	@Override
	public AutoCloseable registerObjectSerializer(final Class<? extends Object> objClass, final ObjectSerializer serializer) {
		return handlers.registerSerializer(objClass, serializer);
	}

	@Override
	public ObjectParser getObjectParser(final int objectClass, final int objectType) {
		return handlers.getParser(createKey(objectClass, objectType));
	}

	@Override
	public ObjectSerializer getObjectSerializer(final Object object) {
		return handlers.getSerializer(object.getImplementedInterface());
	}
}
