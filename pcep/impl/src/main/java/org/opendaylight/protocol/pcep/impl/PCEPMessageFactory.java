/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.HashMap;

import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.PCEPMessage;
import org.opendaylight.protocol.pcep.impl.message.PCCreateMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPCloseMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPErrorMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPKeepAliveMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPNotificationMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPOpenMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPRawMessage;
import org.opendaylight.protocol.pcep.impl.message.PCEPReplyMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPReportMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPRequestMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPUpdateRequestMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPXRAddTunnelMessageParser;
import org.opendaylight.protocol.pcep.impl.message.PCEPXRDeleteTunnelMessageParser;
import org.opendaylight.protocol.pcep.message.PCCreateMessage;
import org.opendaylight.protocol.pcep.message.PCEPCloseMessage;
import org.opendaylight.protocol.pcep.message.PCEPErrorMessage;
import org.opendaylight.protocol.pcep.message.PCEPKeepAliveMessage;
import org.opendaylight.protocol.pcep.message.PCEPNotificationMessage;
import org.opendaylight.protocol.pcep.message.PCEPOpenMessage;
import org.opendaylight.protocol.pcep.message.PCEPReplyMessage;
import org.opendaylight.protocol.pcep.message.PCEPReportMessage;
import org.opendaylight.protocol.pcep.message.PCEPRequestMessage;
import org.opendaylight.protocol.pcep.message.PCEPUpdateRequestMessage;
import org.opendaylight.protocol.pcep.message.PCEPXRAddTunnelMessage;
import org.opendaylight.protocol.pcep.message.PCEPXRDeleteTunnelMessage;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedBytes;

/**
 * Factory for subclasses of {@link org.opendaylight.protocol.pcep.PCEPMessage PCEPMessage}
 */
public class PCEPMessageFactory implements ProtocolMessageFactory<PCEPMessage> {

	private final static Logger logger = LoggerFactory.getLogger(PCEPMessageFactory.class);

	private final static int TYPE_SIZE = 1; // bytes

	private final static int LENGTH_SIZE = 2; // bytes

	public final static int COMMON_HEADER_LENGTH = 4; // bytes

	/**
	 * Type identifier for {@link org.opendaylight.protocol.pcep.PCEPMessage PCEPMessage}
	 */
	public enum PCEPMessageType {
		OPEN(1), NOTIFICATION(5), KEEPALIVE(2), RESPONSE(4), REQUEST(3), ERROR(6), CLOSE(7), UPDATE_REQUEST(11), STATUS_REPORT(10),
		// TODO: replace with actual values by IANA
		XR_ADD_TUNNEL(8), XR_DELETE_TUNNEL(9), PCCREATE(12);

		private final int identifier;

		PCEPMessageType(final int identifier) {
			this.identifier = identifier;
		}

		public int getIdentifier() {
			return this.identifier;
		}

		public static PCEPMessageType getFromInt(final int type) throws PCEPDeserializerException {

			for (final PCEPMessageType type_e : PCEPMessageType.values()) {
				if (type_e.getIdentifier() == type) {
					return type_e;
				}
			}

			throw new PCEPDeserializerException("Unknown PCEPMessage Class identifier. Passed: " + type + "; Known: "
					+ PCEPMessageType.values() + ".");
		}
	}

	private static class MapOfParsers extends HashMap<PCEPMessageType, PCEPMessageParser> {

		private static final long serialVersionUID = -5715193806554448822L;

		private final static MapOfParsers instance = new MapOfParsers();

		private MapOfParsers() {
			this.fillInMap();
		}

		private void fillInMap() {
			this.put(PCEPMessageType.OPEN, new PCEPOpenMessageParser());
			this.put(PCEPMessageType.KEEPALIVE, new PCEPKeepAliveMessageParser());
			this.put(PCEPMessageType.NOTIFICATION, new PCEPNotificationMessageParser());
			this.put(PCEPMessageType.ERROR, new PCEPErrorMessageParser());
			this.put(PCEPMessageType.RESPONSE, new PCEPReplyMessageParser());
			this.put(PCEPMessageType.REQUEST, new PCEPRequestMessageParser());
			this.put(PCEPMessageType.UPDATE_REQUEST, new PCEPUpdateRequestMessageParser());
			this.put(PCEPMessageType.STATUS_REPORT, new PCEPReportMessageParser());
			this.put(PCEPMessageType.CLOSE, new PCEPCloseMessageParser());
			this.put(PCEPMessageType.XR_ADD_TUNNEL, new PCEPXRAddTunnelMessageParser());
			this.put(PCEPMessageType.XR_DELETE_TUNNEL, new PCEPXRDeleteTunnelMessageParser());
			this.put(PCEPMessageType.PCCREATE, new PCCreateMessageParser());
		}

		public static MapOfParsers getInstance() {
			return instance;
		}
	}

	/**
	 * 
	 * @param bytes assume array of bytes without common header
	 * @param msgHeader
	 * @return Parsed specific PCEPMessage
	 * @throws PCEPDeserializerException
	 * @throws PCEPDocumentedException
	 */

	@Override
	public PCEPMessage parse(final byte[] bytes) throws DeserializerException, DocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Array of bytes is mandatory.");
		}

		logger.trace("Attempt to parse message from bytes: {}", ByteArray.bytesToHexString(bytes));

		final int type = UnsignedBytes.toInt(bytes[1]);

		final int msgLength = ByteArray.bytesToInt(ByteArray.subByte(bytes, TYPE_SIZE + 1, LENGTH_SIZE));

		final byte[] msgBody = ByteArray.cutBytes(bytes, TYPE_SIZE + 1 + LENGTH_SIZE);

		if (msgBody.length != msgLength - COMMON_HEADER_LENGTH) {
			throw new DeserializerException("Size don't match size specified in header. Passed: " + msgBody.length + "; Expected: "
					+ (msgLength - COMMON_HEADER_LENGTH) + ". " + msgLength);
		}

		/*
		 * if PCEPObjectIdentifier.getObjectClassFromInt() dont't throws
		 * exception and if returned null we know the error type
		 */
		PCEPMessageType msgType;
		try {
			msgType = PCEPMessageType.getFromInt(type);
		} catch (final PCEPDeserializerException e) {
			throw new DeserializerException(e.getMessage(), e);
		}
		if (msgType == null) {
			throw new DocumentedException("Unhandled message type " + type, new PCEPDocumentedException("Unhandled message type " + type, PCEPErrors.CAPABILITY_NOT_SUPPORTED));
		}

		PCEPMessage msg;
		try {
			msg = new PCEPRawMessage(PCEPObjectFactory.parseObjects(msgBody), msgType);
		} catch (final PCEPDeserializerException e) {
			throw new DeserializerException(e.getMessage(), e);
		} catch (final PCEPDocumentedException e) {
			throw new DocumentedException(e.getMessage(), e);
		}
		logger.debug("Message was parsed. {}", msg);
		return msg;
	}

	@Override
	public byte[] put(final PCEPMessage msg) {
		if (msg == null) {
			throw new IllegalArgumentException("PCEPMessage is mandatory.");
		}

		final PCEPMessageType msgType;

		if (msg instanceof PCEPOpenMessage) {
			msgType = PCEPMessageType.OPEN;
		} else if (msg instanceof PCEPKeepAliveMessage) {
			msgType = PCEPMessageType.KEEPALIVE;
		} else if (msg instanceof PCEPCloseMessage) {
			msgType = PCEPMessageType.CLOSE;
		} else if (msg instanceof PCEPReplyMessage) {
			msgType = PCEPMessageType.RESPONSE;
		} else if (msg instanceof PCEPRequestMessage) {
			msgType = PCEPMessageType.REQUEST;
		} else if (msg instanceof PCEPNotificationMessage) {
			msgType = PCEPMessageType.NOTIFICATION;
		} else if (msg instanceof PCEPErrorMessage) {
			msgType = PCEPMessageType.ERROR;
		} else if (msg instanceof PCEPReportMessage) {
			msgType = PCEPMessageType.STATUS_REPORT;
		} else if (msg instanceof PCEPUpdateRequestMessage) {
			msgType = PCEPMessageType.UPDATE_REQUEST;
		} else if (msg instanceof PCEPXRAddTunnelMessage) {
			msgType = PCEPMessageType.XR_ADD_TUNNEL;
		} else if (msg instanceof PCEPXRDeleteTunnelMessage) {
			msgType = PCEPMessageType.XR_DELETE_TUNNEL;
		} else if (msg instanceof PCCreateMessage) {
			msgType = PCEPMessageType.PCCREATE;
		} else {
			logger.error("Unknown instance of PCEPMessage. Message class: {}", msg.getClass());
			throw new IllegalArgumentException("Unknown instance of PCEPMessage. Passed " + msg.getClass());
		}

		logger.trace("Serializing {}", msgType);

		final byte[] msgBody = MapOfParsers.getInstance().get(msgType).put(msg);

		final PCEPMessageHeader msgHeader = new PCEPMessageHeader(msgType.getIdentifier(), msgBody.length
				+ PCEPMessageHeader.COMMON_HEADER_LENGTH, PCEPMessage.PCEP_VERSION);

		final byte[] headerBytes = msgHeader.toBytes();
		final byte[] retBytes = new byte[headerBytes.length + msgBody.length];

		ByteArray.copyWhole(headerBytes, retBytes, 0);
		ByteArray.copyWhole(msgBody, retBytes, PCEPMessageHeader.COMMON_HEADER_LENGTH);

		return retBytes;
	}
}
