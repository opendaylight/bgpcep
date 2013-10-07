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
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.tlv.LSPUpdateErrorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspDbVersionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspIdentifiersTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfListTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OrderTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OverloadDurationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PredundancyGroupIdTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ReqMissingTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RsvpErrorSpecTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.StatefulCapabilityTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SymbolicPathNameTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

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

		reg.registerTlvParser(1, new NoPathVectorTlvParser());
		reg.registerTlvParser(2, new OverloadedDurationTlvParser());
		reg.registerTlvParser(3, new ReqMissingTlvParser());
		reg.registerTlvParser(4, new OFListTlvParser());
		reg.registerTlvParser(5, new OrderTlvParser());
		reg.registerTlvParser(16, new PCEStatefulCapabilityTlvParser());
		reg.registerTlvParser(17, new LspSymbolicNameTlvParser());
		reg.registerTlvParser(18, new LSPIdentifierIPv4TlvParser());
		reg.registerTlvParser(19, new LSPIdentifierIPv6TlvParser());
		reg.registerTlvParser(20, new LspUpdateErrorTlvParser());
		reg.registerTlvParser(21, new RSVPErrorSpecTlvParser());
		reg.registerTlvParser(23, new LspDbVersionTlvParser());
		reg.registerTlvParser(24, new PredundancyGroupTlvParser());

		reg.registerTlvSerializer(NoPathVectorTlv.class, new NoPathVectorTlvParser());
		reg.registerTlvSerializer(OverloadDurationTlv.class, new OverloadedDurationTlvParser());
		reg.registerTlvSerializer(ReqMissingTlv.class, new ReqMissingTlvParser());
		reg.registerTlvSerializer(OfListTlv.class, new OFListTlvParser());
		reg.registerTlvSerializer(OrderTlv.class, new OrderTlvParser());
		reg.registerTlvSerializer(StatefulCapabilityTlv.class, new PCEStatefulCapabilityTlvParser());
		reg.registerTlvSerializer(SymbolicPathNameTlv.class, new LspSymbolicNameTlvParser());
		reg.registerTlvSerializer(LspIdentifiersTlv.class, new LSPIdentifierIPv4TlvParser());
		reg.registerTlvSerializer(LSPUpdateErrorTlv.class, new LspUpdateErrorTlvParser());
		reg.registerTlvSerializer(RsvpErrorSpecTlv.class, new RSVPErrorSpecTlvParser());
		reg.registerTlvSerializer(LspDbVersionTlv.class, new LspDbVersionTlvParser());
		reg.registerTlvSerializer(PredundancyGroupIdTlv.class, new PredundancyGroupTlvParser());
		INSTANCE = reg;
	}

	private final Map<Integer, MessageParser> msgParsers = new ConcurrentHashMap<>();
	private final Map<Class<? extends Message>, MessageSerializer> msgSerializers = new ConcurrentHashMap<>();

	private final Map<Integer, ObjectParser> objParsers = new ConcurrentHashMap<>();
	private final Map<Class<? extends Object>, ObjectSerializer> objSerializers = new ConcurrentHashMap<>();

	private final Map<Integer, TlvParser> tlvParsers = new ConcurrentHashMap<>();
	private final Map<Class<? extends Tlv>, TlvSerializer> tlvSerializers = new ConcurrentHashMap<>();

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
		for (Map.Entry<Class<? extends Message>, MessageSerializer> s : msgSerializers.entrySet()) {
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
		for (Map.Entry<Class<? extends Object>, ObjectSerializer> s : objSerializers.entrySet()) {
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
		for (Map.Entry<Class<? extends Tlv>, TlvSerializer> s : tlvSerializers.entrySet()) {
			if (s.getKey().isAssignableFrom(c)) {
				return s.getValue();
			}
		}

		return null;
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
}
