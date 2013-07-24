/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessage;
import org.opendaylight.protocol.framework.ProtocolMessageHeader;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.BGPMessageHeader;
import org.opendaylight.protocol.bgp.parser.BGPMessageParser;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPNotificationMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPOpenMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.message.BGPKeepAliveMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPNotificationMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.util.ByteArray;

/**
 *
 */
public class BGPMessageFactory implements BGPMessageParser {

	private final static Logger logger = LoggerFactory.getLogger(BGPMessageFactory.class);

	public BGPMessageFactory() {
	}

	@Override
	public BGPMessage parse(final byte[] bytes, final ProtocolMessageHeader msgH) throws DeserializerException, DocumentedException {
		final BGPMessageHeader msgHeader = (BGPMessageHeader) msgH;
		if (bytes == null)
			throw new IllegalArgumentException("Array of bytes is mandatory.");
		if (msgHeader == null)
			throw new IllegalArgumentException("BGPMessageHeader is mandatory.");
		if (msgHeader.getLength() < BGPMessageHeader.COMMON_HEADER_LENGTH)
			throw new BGPDocumentedException("Message length field not within valid range.", BGPError.BAD_MSG_LENGTH, ByteArray.intToBytes(msgHeader.getLength()));
		if (bytes.length != (msgHeader.getLength() - BGPMessageHeader.COMMON_HEADER_LENGTH))
			throw new BGPParsingException("Size doesn't match size specified in header. Passed: " + bytes.length + "; Expected: "
					+ (msgHeader.getLength() - BGPMessageHeader.COMMON_HEADER_LENGTH) + ". " + msgHeader.getLength());

		logger.debug("Attempt to parse message from bytes: {}", ByteArray.bytesToHexString(bytes));

		BGPMessage msg = null;

		switch (msgHeader.getType()) {
		case 1:
			msg = BGPOpenMessageParser.parse(bytes);
			logger.debug("Received and parsed Open Message: {}", msg);
			break;
		case 2:
			msg = BGPUpdateMessageParser.parse(bytes, msgHeader.getLength());
			logger.debug("Received and parsed Update Message: {}", msg);
			break;
		case 3:
			msg = BGPNotificationMessageParser.parse(bytes);
			logger.debug("Received and parsed Notification Message: {}", msg);
			break;
		case 4:
			msg = new BGPKeepAliveMessage();
			if (msgHeader.getLength() != BGPMessageHeader.COMMON_HEADER_LENGTH)
				throw new BGPDocumentedException("Message length field not within valid range.", BGPError.BAD_MSG_LENGTH, ByteArray.intToBytes(msgHeader.getLength()));
			break;
		default:
			throw new BGPDocumentedException("Unhandled message type " + msgHeader.getType(), BGPError.BAD_MSG_TYPE, ByteArray.intToBytes(msgHeader.getType()));
		}
		return msg;
	}

	@Override
	public byte[] put(final ProtocolMessage msg) {
		final BGPMessage bgpMsg = (BGPMessage) msg;
		if (bgpMsg == null)
			throw new IllegalArgumentException("BGPMessage is mandatory.");

		logger.trace("Serializing {}", bgpMsg);

		byte[] msgBody = null;
		int msgType = 0;

		/*
		 * Update message is not supported
		 */
		if (bgpMsg instanceof BGPOpenMessage) {
			msgType = 1;
			msgBody = BGPOpenMessageParser.put((BGPOpenMessage) bgpMsg);
		} else if (bgpMsg instanceof BGPNotificationMessage) {
			msgType = 3;
			msgBody = BGPNotificationMessageParser.put((BGPNotificationMessage) bgpMsg);
		} else if (bgpMsg instanceof BGPKeepAliveMessage) {
			msgType = 4;
			msgBody = new byte[0];
		} else {
			throw new IllegalArgumentException("Unknown instance of BGPMessage. Passed " + bgpMsg.getClass());
		}

		final BGPMessageHeader msgHeader = new BGPMessageHeader(msgType, msgBody.length + BGPMessageHeader.COMMON_HEADER_LENGTH);

		final byte[] headerBytes = msgHeader.toBytes();
		final byte[] retBytes = new byte[headerBytes.length + msgBody.length];

		ByteArray.copyWhole(headerBytes, retBytes, 0);
		ByteArray.copyWhole(msgBody, retBytes, BGPMessageHeader.COMMON_HEADER_LENGTH);

		logger.trace("Serialized BGP message {}.", Arrays.toString(retBytes));
		return retBytes;
	}

	@Override
	public void close() throws IOException {
		// nothing to close
	}
}
