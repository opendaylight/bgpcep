/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.protocol.concepts.AbstractRegistration;
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
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.SubobjectParser;
import org.opendaylight.protocol.pcep.spi.SubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.MetricObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NotificationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AsNumberSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;

import com.google.common.base.Preconditions;

public class HandlerRegistryImpl implements HandlerRegistry {
	public static final HandlerRegistry INSTANCE;

	static {
		final HandlerRegistry reg = new HandlerRegistryImpl();

		// FIXME: fill this in

		// reg.registerMessageHandler(PCEPOpenMessage.class, 1, new PCEPOpenMessageParser());
		// reg.registerMessageHandler(PCEPNotificationMessage.class, 5, new PCEPNotificationMessageParser());
		// reg.registerMessageHandler(PCEPKeepAliveMessage.class, 2, new PCEPKeepAliveMessageParser());
		// reg.registerMessageHandler(PCEPReplyMessage.class, 4, new PCEPReplyMessageParser());
		// reg.registerMessageHandler(PCEPRequestMessage.class, 3, new PCEPRequestMessageParser());
		// reg.registerMessageHandler(PCEPErrorMessage.class, 6, new PCEPErrorMessageParser());
		// reg.registerMessageHandler(PCEPCloseMessage.class, 7, new PCEPCloseMessageParser());
		// reg.registerMessageHandler(PCEPUpdateRequestMessage.class, 11, new PCEPUpdateRequestMessageParser());
		// reg.registerMessageHandler(PCEPReportMessage.class, 10, new PCEPReportMessageParser());
		// reg.registerMessageHandler(PCCreateMessage.class, 12, new PCCreateMessageParser());

		reg.registerTlvParser(NoPathVectorTlvParser.TYPE, new NoPathVectorTlvParser());
		reg.registerTlvParser(OverloadedDurationTlvParser.TYPE, new OverloadedDurationTlvParser());
		reg.registerTlvParser(ReqMissingTlvParser.TYPE, new ReqMissingTlvParser());
		reg.registerTlvParser(OFListTlvParser.TYPE, new OFListTlvParser());
		reg.registerTlvParser(OrderTlvParser.TYPE, new OrderTlvParser());
		reg.registerTlvParser(PCEStatefulCapabilityTlvParser.TYPE, new PCEStatefulCapabilityTlvParser());
		reg.registerTlvParser(LspSymbolicNameTlvParser.TYPE, new LspSymbolicNameTlvParser());
		reg.registerTlvParser(LSPIdentifierIPv4TlvParser.TYPE, new LSPIdentifierIPv4TlvParser());
		reg.registerTlvParser(LSPIdentifierIPv6TlvParser.TYPE, new LSPIdentifierIPv6TlvParser());
		reg.registerTlvParser(LspUpdateErrorTlvParser.TYPE, new LspUpdateErrorTlvParser());
		reg.registerTlvParser(RSVPErrorSpecTlvParser.TYPE, new RSVPErrorSpecTlvParser());
		reg.registerTlvParser(LspDbVersionTlvParser.TYPE, new LspDbVersionTlvParser());
		reg.registerTlvParser(PredundancyGroupTlvParser.TYPE, new PredundancyGroupTlvParser());

		reg.registerTlvSerializer(NoPathVectorTlv.class, new NoPathVectorTlvParser());
		reg.registerTlvSerializer(OverloadDurationTlv.class, new OverloadedDurationTlvParser());
		reg.registerTlvSerializer(ReqMissingTlv.class, new ReqMissingTlvParser());
		reg.registerTlvSerializer(OfListTlv.class, new OFListTlvParser());
		reg.registerTlvSerializer(OrderTlv.class, new OrderTlvParser());
		reg.registerTlvSerializer(StatefulCapabilityTlv.class, new PCEStatefulCapabilityTlvParser());
		reg.registerTlvSerializer(SymbolicPathNameTlv.class, new LspSymbolicNameTlvParser());
		reg.registerTlvSerializer(LspIdentifiersTlv.class, new LSPIdentifierIPv4TlvParser());
		reg.registerTlvSerializer(LspErrorCodeTlv.class, new LspUpdateErrorTlvParser());
		reg.registerTlvSerializer(RsvpErrorSpecTlv.class, new RSVPErrorSpecTlvParser());
		reg.registerTlvSerializer(LspDbVersionTlv.class, new LspDbVersionTlvParser());
		reg.registerTlvSerializer(PredundancyGroupIdTlv.class, new PredundancyGroupTlvParser());

		reg.registerObjectParser(PCEPOpenObjectParser.CLASS, PCEPOpenObjectParser.TYPE, new PCEPOpenObjectParser(reg));
		reg.registerObjectParser(PCEPRequestParameterObjectParser.CLASS, PCEPRequestParameterObjectParser.TYPE,
				new PCEPRequestParameterObjectParser(reg));
		reg.registerObjectParser(PCEPNoPathObjectParser.CLASS, PCEPNoPathObjectParser.TYPE, new PCEPNoPathObjectParser(reg));
		reg.registerObjectParser(PCEPEndPointsObjectParser.CLASS, PCEPEndPointsObjectParser.TYPE, new PCEPEndPointsObjectParser(reg));
		reg.registerObjectParser(PCEPEndPointsObjectParser.CLASS_6, PCEPEndPointsObjectParser.TYPE_6, new PCEPEndPointsObjectParser(reg));
		reg.registerObjectParser(PCEPBandwidthObjectParser.CLASS, PCEPBandwidthObjectParser.TYPE, new PCEPBandwidthObjectParser(reg));
		reg.registerObjectParser(PCEPBandwidthObjectParser.E_CLASS, PCEPBandwidthObjectParser.E_TYPE, new PCEPBandwidthObjectParser(reg));
		reg.registerObjectParser(PCEPMetricObjectParser.CLASS, PCEPMetricObjectParser.TYPE, new PCEPMetricObjectParser(reg));
		reg.registerObjectParser(PCEPExplicitRouteObjectParser.CLASS, PCEPExplicitRouteObjectParser.TYPE,
				new PCEPExplicitRouteObjectParser(reg));
		reg.registerObjectParser(PCEPReportedRouteObjectParser.CLASS, PCEPReportedRouteObjectParser.TYPE,
				new PCEPReportedRouteObjectParser(reg));
		reg.registerObjectParser(PCEPLspaObjectParser.CLASS, PCEPLspaObjectParser.TYPE, new PCEPLspaObjectParser(reg));
		reg.registerObjectParser(PCEPIncludeRouteObjectParser.CLASS, PCEPIncludeRouteObjectParser.TYPE,
				new PCEPIncludeRouteObjectParser(reg));
		reg.registerObjectParser(PCEPSvecObjectParser.CLASS, PCEPSvecObjectParser.TYPE, new PCEPSvecObjectParser(reg));
		reg.registerObjectParser(PCEPNotificationObjectParser.CLASS, PCEPNotificationObjectParser.TYPE,
				new PCEPNotificationObjectParser(reg));
		reg.registerObjectParser(PCEPErrorObjectParser.CLASS, PCEPErrorObjectParser.TYPE, new PCEPErrorObjectParser(reg));
		reg.registerObjectParser(PCEPLoadBalancingObjectParser.CLASS, PCEPLoadBalancingObjectParser.TYPE,
				new PCEPLoadBalancingObjectParser(reg));
		reg.registerObjectParser(PCEPCloseObjectParser.CLASS, PCEPCloseObjectParser.TYPE, new PCEPCloseObjectParser(reg));
		reg.registerObjectParser(PCEPPathKeyObjectParser.CLASS, PCEPPathKeyObjectParser.TYPE, new PCEPPathKeyObjectParser(reg));
		reg.registerObjectParser(PCEPObjectiveFunctionObjectParser.CLASS, PCEPObjectiveFunctionObjectParser.TYPE,
				new PCEPObjectiveFunctionObjectParser(reg));
		reg.registerObjectParser(PCEPClassTypeObjectParser.CLASS, PCEPClassTypeObjectParser.TYPE, new PCEPClassTypeObjectParser(reg));
		reg.registerObjectParser(PCEPGlobalConstraintsObjectParser.CLASS, PCEPGlobalConstraintsObjectParser.TYPE,
				new PCEPGlobalConstraintsObjectParser(reg));
		reg.registerObjectParser(PCEPLspObjectParser.CLASS, PCEPLspObjectParser.TYPE, new PCEPLspObjectParser(reg));
		reg.registerObjectParser(PCEPSrpObjectParser.CLASS, PCEPSrpObjectParser.TYPE, new PCEPSrpObjectParser(reg));
		// reg.registerObjectParser(PCEPExcludeRouteObjectParser.CLASS, PCEPExcludeRouteObjectParser.TYPE, new
		// PCEPExcludeRouteObjectParser(reg));

		reg.registerObjectSerializer(OpenObject.class, new PCEPOpenObjectParser(reg));
		reg.registerObjectSerializer(RpObject.class, new PCEPRequestParameterObjectParser(reg));
		reg.registerObjectSerializer(NoPathObject.class, new PCEPNoPathObjectParser(reg));
		reg.registerObjectSerializer(EndpointsObject.class, new PCEPEndPointsObjectParser(reg));
		reg.registerObjectSerializer(BandwidthObject.class, new PCEPBandwidthObjectParser(reg));
		reg.registerObjectSerializer(MetricObject.class, new PCEPMetricObjectParser(reg));
		reg.registerObjectSerializer(ExplicitRouteObject.class, new PCEPExplicitRouteObjectParser(reg));
		reg.registerObjectSerializer(ReportedRouteObject.class, new PCEPReportedRouteObjectParser(reg));
		reg.registerObjectSerializer(LspaObject.class, new PCEPLspaObjectParser(reg));
		reg.registerObjectSerializer(IncludeRouteObject.class, new PCEPIncludeRouteObjectParser(reg));
		reg.registerObjectSerializer(SvecObject.class, new PCEPSvecObjectParser(reg));
		reg.registerObjectSerializer(NotificationObject.class, new PCEPNotificationObjectParser(reg));
		reg.registerObjectSerializer(PcepErrorObject.class, new PCEPErrorObjectParser(reg));
		reg.registerObjectSerializer(LoadBalancingObject.class, new PCEPLoadBalancingObjectParser(reg));
		reg.registerObjectSerializer(CloseObject.class, new PCEPCloseObjectParser(reg));
		reg.registerObjectSerializer(PathKeyObject.class, new PCEPPathKeyObjectParser(reg));
		reg.registerObjectSerializer(OfObject.class, new PCEPObjectiveFunctionObjectParser(reg));
		reg.registerObjectSerializer(ClasstypeObject.class, new PCEPClassTypeObjectParser(reg));
		reg.registerObjectSerializer(GcObject.class, new PCEPGlobalConstraintsObjectParser(reg));
		reg.registerObjectSerializer(LspObject.class, new PCEPLspObjectParser(reg));
		reg.registerObjectSerializer(SrpObject.class, new PCEPSrpObjectParser(reg));
		// reg.registerObjectSerializer(ExcludeRouteObject.class, new PCEPExcludeRouteObjectParser(reg));

		reg.registerSubobjectParser(EROAsNumberSubobjectParser.TYPE, new EROAsNumberSubobjectParser());

		reg.registerSubobjectSerializer(AsNumberSubobject.class, new EROAsNumberSubobjectParser());

		INSTANCE = reg;
	}

	private final Map<Integer, MessageParser> msgParsers = new ConcurrentHashMap<>();
	private final Map<Class<? extends Message>, MessageSerializer> msgSerializers = new ConcurrentHashMap<>();

	private final Map<Integer, ObjectParser> objParsers = new ConcurrentHashMap<>();
	private final Map<Class<? extends Object>, ObjectSerializer> objSerializers = new ConcurrentHashMap<>();

	private final Map<Integer, TlvParser> tlvParsers = new ConcurrentHashMap<>();
	private final Map<Class<? extends Tlv>, TlvSerializer> tlvSerializers = new ConcurrentHashMap<>();

	private final Map<Integer, SubobjectParser> subobjectParsers = new ConcurrentHashMap<>();
	private final Map<Class<? extends CSubobject>, SubobjectSerializer> subobjectSerializers = new ConcurrentHashMap<>();

	private HandlerRegistryImpl() {

	}

	@Override
	public MessageParser getMessageParser(final int messageType) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return this.msgParsers.get(messageType);
	}

	@Override
	public MessageSerializer getMessageSerializer(final Message message) {
		final Class<? extends Message> c = message.getClass();
		for (final Map.Entry<Class<? extends Message>, MessageSerializer> s : this.msgSerializers.entrySet()) {
			if (s.getKey().isAssignableFrom(c)) {
				return s.getValue();
			}
		}

		return null;
	}

	@Override
	public synchronized ObjectParser getObjectParser(final int objectClass, final int objectType) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= 255);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);

		return this.objParsers.get((objectClass << 4) + objectType);
	}

	@Override
	public ObjectSerializer getObjectSerializer(final Object object) {
		final Class<? extends Object> c = object.getClass();
		for (final Map.Entry<Class<? extends Object>, ObjectSerializer> s : this.objSerializers.entrySet()) {
			if (s.getKey().isAssignableFrom(c)) {
				return s.getValue();
			}
		}

		return null;
	}

	@Override
	public TlvParser getTlvParser(final int tlvType) {
		Preconditions.checkArgument(tlvType >= 0 && tlvType <= 65535);
		return this.tlvParsers.get(tlvType);
	}

	@Override
	public TlvSerializer getTlvSerializer(final Tlv tlv) {
		final Class<? extends Tlv> c = tlv.getClass();
		for (final Map.Entry<Class<? extends Tlv>, TlvSerializer> s : this.tlvSerializers.entrySet()) {
			if (s.getKey().isAssignableFrom(c)) {
				return s.getValue();
			}
		}

		return null;
	}

	@Override
	public SubobjectSerializer getSubobjectSerializer(final CSubobject subobject) {
		final Class<? extends CSubobject> c = subobject.getClass();
		for (final Map.Entry<Class<? extends CSubobject>, SubobjectSerializer> s : this.subobjectSerializers.entrySet()) {
			if (s.getKey().isAssignableFrom(c)) {
				return s.getValue();
			}
		}
		return null;
	}

	@Override
	public SubobjectParser getSubobjectParser(final int subobjectType) {
		Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= 65535);
		return this.subobjectParsers.get(subobjectType);
	}

	private synchronized void unregisterMessageParser(final Integer msgType) {
		this.msgParsers.remove(msgType);
	}

	@Override
	public synchronized AutoCloseable registerMessageParser(final int msgType, final MessageParser parser) {
		Preconditions.checkArgument(msgType >= 0 && msgType <= 255);
		Preconditions.checkArgument(!this.msgParsers.containsKey(msgType), "Message type %s already registered", msgType);
		this.msgParsers.put(msgType, parser);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterMessageParser(msgType);
			}
		};
	}

	private synchronized void unregisterMessageSerializer(final Class<? extends Message> msgClass) {
		this.msgSerializers.remove(msgClass);
	}

	@Override
	public synchronized AutoCloseable registerMessageSerializer(final Class<? extends Message> msgClass, final MessageSerializer serializer) {
		Preconditions.checkArgument(!this.msgSerializers.containsKey(msgClass), "Message class %s already registered", msgClass);
		this.msgSerializers.put(msgClass, serializer);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterMessageSerializer(msgClass);
			}
		};
	}

	private synchronized void unregisterObjectParser(final Integer key) {
		this.objParsers.remove(key);
	}

	@Override
	public synchronized AutoCloseable registerObjectParser(final int objectClass, final int objectType, final ObjectParser parser) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= 255);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);

		final Integer key = (objectClass << 4) + objectType;
		Preconditions.checkArgument(!this.objParsers.containsKey(key), "Object class %s type %s already registered", objectClass,
				objectType);
		this.objParsers.put(key, parser);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterObjectParser(key);
			}
		};
	}

	private synchronized void unregisterObjectSerializer(final Class<? extends Object> objClass) {
		this.objSerializers.remove(objClass);
	}

	@Override
	public synchronized AutoCloseable registerObjectSerializer(final Class<? extends Object> objClass, final ObjectSerializer serializer) {
		Preconditions.checkArgument(!this.objSerializers.containsKey(objClass), "Object class %s already registered", objClass);
		this.objSerializers.put(objClass, serializer);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterObjectSerializer(objClass);
			}
		};
	}

	private synchronized void unregisterTlvParser(final int tlvType) {
		this.tlvParsers.remove(tlvType);
	}

	@Override
	public synchronized AutoCloseable registerTlvParser(final int tlvType, final TlvParser parser) {
		Preconditions.checkArgument(tlvType >= 0 && tlvType <= 65535);
		Preconditions.checkArgument(!this.tlvParsers.containsKey(tlvType), "TLV type %s already registered", tlvType);

		this.tlvParsers.put(tlvType, parser);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterTlvParser(tlvType);
			}
		};
	}

	private synchronized void unregisterTlvSerializer(final Class<? extends Tlv> tlvClass) {
		this.tlvSerializers.remove(tlvClass);
	}

	@Override
	public synchronized AutoCloseable registerTlvSerializer(final Class<? extends Tlv> tlvClass, final TlvSerializer serializer) {
		Preconditions.checkArgument(!this.tlvSerializers.containsKey(tlvClass), "TLV class %s already registered", tlvClass);
		this.tlvSerializers.put(tlvClass, serializer);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterTlvSerializer(tlvClass);
			}
		};
	}

	@Override
	public AutoCloseable registerSubobjectSerializer(final Class<? extends CSubobject> subobjectClass, final SubobjectSerializer serializer) {
		Preconditions.checkArgument(!this.tlvSerializers.containsKey(subobjectClass), "Subobject class %s already registered",
				subobjectClass);
		this.subobjectSerializers.put(subobjectClass, serializer);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterSubobjectSerializer(subobjectClass);
			}
		};
	}

	private synchronized void unregisterSubobjectSerializer(final Class<? extends CSubobject> subobjectClass) {
		this.subobjectSerializers.remove(subobjectClass);
	}

	@Override
	public AutoCloseable registerSubobjectParser(final int subobjectType, final SubobjectParser parser) {
		Preconditions.checkArgument(subobjectType >= 0 && subobjectType <= 65535);
		Preconditions.checkArgument(!this.subobjectParsers.containsKey(subobjectType), "Subobject type %s already registered",
				subobjectType);

		this.subobjectParsers.put(subobjectType, parser);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterSubobjectParser(subobjectType);
			}
		};
	}

	private synchronized void unregisterSubobjectParser(final int subobjectType) {
		this.subobjectParsers.remove(subobjectType);
	}
}
