/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.protocol.pcep.impl.tlv.LspSymbolicNameTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspUpdateErrorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PCEStatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.ReqMissingTlvParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;

import com.google.common.base.Preconditions;

public class HandlerRegistryImpl implements HandlerRegistry {
	public static final HandlerRegistry INSTANCE;

	static {
		final HandlerRegistry reg = new HandlerRegistryImpl();

		// FIXME: fill this in

		//		reg.registerMessageHandler(PCEPOpenMessage.class, 1, new PCEPOpenMessageParser());
		//		reg.registerMessageHandler(PCEPNotificationMessage.class, 5, new PCEPNotificationMessageParser());
		//		reg.registerMessageHandler(PCEPKeepAliveMessage.class, 2, new PCEPKeepAliveMessageParser());
		//		reg.registerMessageHandler(PCEPReplyMessage.class, 4, new PCEPReplyMessageParser());
		//		reg.registerMessageHandler(PCEPRequestMessage.class, 3, new PCEPRequestMessageParser());
		//		reg.registerMessageHandler(PCEPErrorMessage.class, 6, new PCEPErrorMessageParser());
		//		reg.registerMessageHandler(PCEPCloseMessage.class, 7, new PCEPCloseMessageParser());
		//		reg.registerMessageHandler(PCEPUpdateRequestMessage.class, 11, new PCEPUpdateRequestMessageParser());
		//		reg.registerMessageHandler(PCEPReportMessage.class, 10, new PCEPReportMessageParser());
		//		reg.registerMessageHandler(PCCreateMessage.class, 12, new PCCreateMessageParser());

		reg.registerTlvParser(1, new NoPathVectorTlvParser());
		reg.registerTlvParser(2, new OverloadedDurationTlvParser());
		reg.registerTlvParser(3, new ReqMissingTlvParser());
		reg.registerTlvParser(4, new OFListTlvParser());
		reg.registerTlvParser(5, new OrderTlvParser());
		reg.registerTlvParser(16, new PCEStatefulCapabilityTlvParser());
		reg.registerTlvParser(17, new LspSymbolicNameTlvParser());
		reg.registerTlvParser(18, parser);
		reg.registerTlvParser(19, parser);
		reg.registerTlvParser(20, new LspUpdateErrorTlvParser());
		INSTANCE = reg;
	}

	private final Map<Integer, MessageParser> msgParsers = new HashMap<>();
	private final Map<Class<? extends Message>, MessageSerializer> msgSerializers = new HashMap<>();

	private final Map<Integer, ObjectParser> objParsers = new HashMap<>();
	private final Map<Class<? extends Object>, ObjectSerializer> objSerializers = new HashMap<>();

	private final Map<Integer, TlvParser> tlvParsers = new HashMap<>();
	private final Map<Class<? extends Tlv>, TlvSerializer> tlvSerializers = new HashMap<>();

	private HandlerRegistryImpl() {

	}

	@Override
	public synchronized MessageParser getMessageParser(final int messageType) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return msgParsers.get(messageType);
	}

	@Override
	public synchronized MessageSerializer getMessageSerializer(final Message message) {
		return msgSerializers.get(message.getClass());
	}

	@Override
	public synchronized ObjectParser getObjectParser(final int objectClass, final int objectType) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= 255);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);

		return objParsers.get((objectClass << 4) + objectType);
	}

	@Override
	public synchronized ObjectSerializer getObjectSerializer(final Object object) {
		return objSerializers.get(object.getClass());
	}

	@Override
	public synchronized TlvParser getTlvParser(final int tlvType) {
		Preconditions.checkArgument(tlvType >= 0 && tlvType <= 65535);
		return tlvParsers.get(tlvType);
	}

	@Override
	public synchronized TlvSerializer getTlvSerializer(final Tlv tlv) {
		return tlvSerializers.get(tlv.getClass());
	}

	private synchronized void unregisterMessageParser(final Integer msgType) {
		msgParsers.remove(msgType);
	}

	@Override
	public synchronized AutoCloseable registerMessageParser(final int msgType, final MessageParser parser) {
		Preconditions.checkArgument(msgType >= 0 && msgType <= 255);
		Preconditions.checkArgument(!msgParsers.containsKey(msgType), "Message type %s already registered", msgType);
		msgParsers.put(msgType, parser);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterMessageParser(msgType);
			}
		};
	}

	private synchronized void unregisterMessageSerializer(final Class<? extends Message> msgClass) {
		msgSerializers.remove(msgClass);
	}

	@Override
	public synchronized AutoCloseable registerMessageSerializer(final Class<? extends Message> msgClass, final MessageSerializer serializer) {
		Preconditions.checkArgument(!msgSerializers.containsKey(msgClass), "Message class %s already registered", msgClass);
		msgSerializers.put(msgClass, serializer);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterMessageSerializer(msgClass);
			}
		};
	}

	private synchronized void unregisterObjectParser(final Integer key) {
		objParsers.remove(key);
	}

	@Override
	public synchronized AutoCloseable registerObjectParser(final int objectClass, final int objectType,
			final ObjectParser parser) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= 255);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);

		final Integer key = (objectClass << 4) + objectType;
		Preconditions.checkArgument(!objParsers.containsKey(key), "Object class %s type %s already registered",	objectClass, objectType);
		objParsers.put(key, parser);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterObjectParser(key);
			}
		};
	}

	private synchronized void unregisterObjectSerializer(final Class<? extends Object> objClass) {
		objSerializers.remove(objClass);
	}

	@Override
	public synchronized AutoCloseable registerObjectSerializer(final Class<? extends Object> objClass, final ObjectSerializer serializer) {
		Preconditions.checkArgument(!objSerializers.containsKey(objClass), "Object class %s already registered", objClass);
		objSerializers.put(objClass, serializer);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterObjectSerializer(objClass);
			}
		};
	}

	private synchronized void unregisterTlvParser(final int tlvType) {
		tlvParsers.remove(tlvType);
	}

	@Override
	public synchronized AutoCloseable registerTlvParser(final int tlvType, final TlvParser parser) {
		Preconditions.checkArgument(tlvType >= 0 && tlvType <= 65535);
		Preconditions.checkArgument(!tlvParsers.containsKey(tlvType), "TLV type %s already registered", tlvType);

		tlvParsers.put(tlvType, parser);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterTlvParser(tlvType);
			}
		};
	}

	private synchronized void unregisterTlvSerializer(final Class<? extends Tlv> tlvClass) {
		tlvSerializers.remove(tlvClass);
	}

	@Override
	public synchronized AutoCloseable registerTlvSerializer(final Class<? extends Tlv> tlvClass, final TlvSerializer serializer) {
		Preconditions.checkArgument(!tlvSerializers.containsKey(tlvClass), "TLV class %s already registered", tlvClass);
		tlvSerializers.put(tlvClass, serializer);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterTlvSerializer(tlvClass);
			}
		};
	}
}
