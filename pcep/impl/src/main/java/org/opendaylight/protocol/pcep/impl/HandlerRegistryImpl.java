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
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageHandler;
import org.opendaylight.protocol.pcep.spi.ObjectHandler;
import org.opendaylight.protocol.pcep.spi.TlvHandler;
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

		INSTANCE = reg;
	}

	private final Map<Integer, MessageHandler> msgHandlerInts = new HashMap<>();
	private final Map<Class<? extends Message>, MessageHandler> msgHandlerClasses = new HashMap<>();

	private final Map<Integer, ObjectHandler> objHandlerInts = new HashMap<>();
	private final Map<Class<? extends Object>, ObjectHandler> objHandlerClasses = new HashMap<>();

	private final Map<Integer, TlvHandler> tlvHandlerInts = new HashMap<>();
	private final Map<Class<? extends Tlv>, TlvHandler> tlvHandlerClasses = new HashMap<>();

	private HandlerRegistryImpl() {

	}

	@Override
	public synchronized MessageHandler getMessageHandler(final int messageType) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return msgHandlerInts.get(messageType);
	}

	@Override
	public synchronized MessageHandler getMessageHandler(final Message message) {
		return msgHandlerClasses.get(message.getClass());
	}

	@Override
	public synchronized ObjectHandler getObjectHandler(final int objectClass, final int objectType) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= 255);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);

		return objHandlerInts.get((objectClass << 4) + objectType);
	}

	@Override
	public synchronized ObjectHandler getObjectHandler(final Object object) {
		return objHandlerClasses.get(object.getClass());
	}

	@Override
	public synchronized TlvHandler getTlvHandler(final int tlvType) {
		Preconditions.checkArgument(tlvType >= 0 && tlvType <= 65535);
		return tlvHandlerInts.get(tlvType);
	}

	@Override
	public synchronized TlvHandler getTlvHandler(final Tlv tlv) {
		return tlvHandlerClasses.get(tlv.getClass());
	}

	private synchronized void unregisterMessageHandler(final Integer msgType, final Class<? extends Message> msgClass) {
		msgHandlerInts.remove(msgType);
		msgHandlerClasses.remove(msgClass);
	}

	@Override
	public synchronized AutoCloseable registerMessageHandler(
			final Class<? extends Message> msgClass, final int msgType,
			final MessageHandler handler) {
		Preconditions.checkArgument(msgType >= 0 && msgType <= 255);

		Preconditions.checkArgument(!msgHandlerInts.containsKey(msgType), "Message type %s already registered", msgType);
		Preconditions.checkArgument(!msgHandlerClasses.containsKey(msgClass), "Message class %s already registered", msgClass);

		msgHandlerInts.put(msgType, handler);
		msgHandlerClasses.put(msgClass, handler);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterMessageHandler(msgType, msgClass);
			}
		};
	}

	private synchronized void unregisterObjectHandler(final Integer key, final Class<? extends Object> objClass) {
		objHandlerInts.remove(key);
		objHandlerClasses.remove(objClass);
	}

	@Override
	public synchronized AutoCloseable registerObjectHandler(
			final Class<? extends Object> objClass, final int objectClass, final int objectType,
			final ObjectHandler handler) {
		Preconditions.checkArgument(objectClass >= 0 && objectClass <= 255);
		Preconditions.checkArgument(objectType >= 0 && objectType <= 15);

		final Integer key = (objectClass << 4) + objectType;
		Preconditions.checkArgument(!objHandlerInts.containsKey(key), "Object class %s type %s already registered",
				objectClass, objectType);
		Preconditions.checkArgument(!objHandlerClasses.containsKey(objectClass), "TLV class %s already registered", objectClass);

		objHandlerInts.put(key, handler);
		objHandlerClasses.put(objClass, handler);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterObjectHandler(key, objClass);
			}
		};
	}

	private synchronized void unregisterTlvHandler(final int tlvType, final Class<? extends Tlv> tlvClass) {
		tlvHandlerInts.remove(tlvType);
		tlvHandlerClasses.remove(tlvClass);
	}

	@Override
	public synchronized AutoCloseable registerTlvHandler(final Class<? extends Tlv> tlvClass,
			final int tlvType, final TlvHandler handler) {
		Preconditions.checkArgument(tlvType >= 0 && tlvType <= 65535);

		Preconditions.checkArgument(!tlvHandlerInts.containsKey(tlvType), "TLV type %s already registered", tlvType);
		Preconditions.checkArgument(!tlvHandlerClasses.containsKey(tlvClass), "TLV class %s already registered", tlvClass);

		tlvHandlerInts.put(tlvType, handler);
		tlvHandlerClasses.put(tlvClass, handler);

		return new AbstractRegistration() {
			@Override
			protected void removeRegistration() {
				unregisterTlvHandler(tlvType, tlvClass);
			}
		};
	}
}
