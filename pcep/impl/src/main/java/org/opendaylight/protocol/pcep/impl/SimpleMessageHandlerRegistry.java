/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.concepts.HandlerRegistry;
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
import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.KeepaliveMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcerrMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcinitiateMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcntfMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrepMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcreqMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcrptMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PcupdMessage;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

public final class SimpleMessageHandlerFactory implements MessageHandlerRegistry {
	public static final MessageHandlerRegistry INSTANCE;

	static {
		final MessageHandlerRegistry reg = new SimpleMessageHandlerFactory();

		final ObjectHandlerRegistry objReg = new SimpleObjectHandlerRegistry();

		reg.registerMessageParser(PCEPOpenMessageParser.TYPE, new PCEPOpenMessageParser(objReg));
		reg.registerMessageParser(PCEPNotificationMessageParser.TYPE, new PCEPNotificationMessageParser(objReg));
		reg.registerMessageParser(PCEPKeepAliveMessageParser.TYPE, new PCEPKeepAliveMessageParser(objReg));
		reg.registerMessageParser(PCEPReplyMessageParser.TYPE, new PCEPReplyMessageParser(objReg));
		reg.registerMessageParser(PCEPRequestMessageParser.TYPE, new PCEPRequestMessageParser(objReg));
		reg.registerMessageParser(PCEPErrorMessageParser.TYPE, new PCEPErrorMessageParser(objReg));
		reg.registerMessageParser(PCEPCloseMessageParser.TYPE, new PCEPCloseMessageParser(objReg));
		reg.registerMessageParser(PCEPUpdateRequestMessageParser.TYPE, new PCEPUpdateRequestMessageParser(objReg));
		reg.registerMessageParser(PCEPReportMessageParser.TYPE, new PCEPReportMessageParser(objReg));
		reg.registerMessageParser(PCCreateMessageParser.TYPE, new PCCreateMessageParser(objReg));

		reg.registerMessageSerializer(OpenMessage.class, new PCEPOpenMessageParser(objReg));
		reg.registerMessageSerializer(PcntfMessage.class, new PCEPNotificationMessageParser(objReg));
		reg.registerMessageSerializer(KeepaliveMessage.class, new PCEPKeepAliveMessageParser(objReg));
		reg.registerMessageSerializer(PcrepMessage.class, new PCEPReplyMessageParser(objReg));
		reg.registerMessageSerializer(PcreqMessage.class, new PCEPRequestMessageParser(objReg));
		reg.registerMessageSerializer(PcerrMessage.class, new PCEPErrorMessageParser(objReg));
		reg.registerMessageSerializer(CloseMessage.class, new PCEPCloseMessageParser(objReg));
		reg.registerMessageSerializer(PcupdMessage.class, new PCEPUpdateRequestMessageParser(objReg));
		reg.registerMessageSerializer(PcrptMessage.class, new PCEPReportMessageParser(objReg));
		reg.registerMessageSerializer(PcinitiateMessage.class, new PCCreateMessageParser(objReg));

		INSTANCE = reg;
	}

	private final HandlerRegistry<DataContainer, MessageParser, MessageSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return this.handlers.registerParser(messageType, parser);
	}

	@Override
	public AutoCloseable registerMessageSerializer(final Class<? extends Message> msgClass, final MessageSerializer serializer) {
		return this.handlers.registerSerializer(msgClass, serializer);
	}

	@Override
	public MessageParser getMessageParser(final int messageType) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return this.handlers.getParser(messageType);
	}

	@Override
	public MessageSerializer getMessageSerializer(final Message message) {
		return this.handlers.getSerializer(message.getImplementedInterface());
	}
}
