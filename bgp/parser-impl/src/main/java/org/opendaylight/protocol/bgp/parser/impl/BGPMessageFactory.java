/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPMessage;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPNotificationMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPOpenMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.message.BGPKeepAliveMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPNotificationMessage;
import org.opendaylight.protocol.bgp.parser.message.BGPOpenMessage;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

/**
 * The byte array
 */
public class BGPMessageFactory implements ProtocolMessageFactory<BGPMessage> {

	private final static Logger logger = LoggerFactory.getLogger(BGPMessageFactory.class);

	final static int LENGTH_FIELD_LENGTH = 2; // bytes

	private final static int TYPE_FIELD_LENGTH = 1; // bytes

	final static int MARKER_LENGTH = 16; // bytes

	public final static int COMMON_HEADER_LENGTH = LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + MARKER_LENGTH;

	public BGPMessageFactory() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.opendaylight.protocol.bgp.parser.BGPMessageParser#parse(byte[])
	 */
	@Override
	public List<BGPMessage> parse(final byte[] bytes) throws DeserializerException, DocumentedException {
		if (bytes == null) {
			throw new IllegalArgumentException("Array of bytes is mandatory.");
		}
		if (bytes.length < COMMON_HEADER_LENGTH) {
			throw new IllegalArgumentException("Too few bytes in passed array. Passed: " + bytes.length + ". Expected: >= "
					+ COMMON_HEADER_LENGTH + ".");
		}
		/*
		 * byte array starts with message length
		 */
		// final byte[] ones = new byte[MARKER_LENGTH];
		// Arrays.fill(ones, (byte)0xff);
		// if (Arrays.equals(bytes, ones))
		// throw new BGPDocumentedException("Marker not set to ones.", BGPError.CONNECTION_NOT_SYNC);
		final byte[] bs = ByteArray.cutBytes(bytes, MARKER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(bs, 0, LENGTH_FIELD_LENGTH));
		final int messageType = UnsignedBytes.toInt(bs[LENGTH_FIELD_LENGTH]);

		final byte[] msgBody = ByteArray.cutBytes(bs, LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH);

		if (messageLength < COMMON_HEADER_LENGTH) {
			throw new BGPDocumentedException("Message length field not within valid range.", BGPError.BAD_MSG_LENGTH, ByteArray.subByte(bs,
					0, LENGTH_FIELD_LENGTH));
		}
		if (msgBody.length != messageLength - COMMON_HEADER_LENGTH) {
			throw new DeserializerException("Size doesn't match size specified in header. Passed: " + msgBody.length + "; Expected: "
					+ (messageLength - COMMON_HEADER_LENGTH) + ". ");
		}

		logger.debug("Attempt to parse message from bytes: {}", ByteArray.bytesToHexString(msgBody));

		final BGPMessage msg;

		switch (messageType) {
		case 1:
			msg = BGPOpenMessageParser.parse(msgBody);
			logger.debug("Received and parsed Open Message: {}", msg);
			break;
		case 2:
			msg = BGPUpdateMessageParser.parse(msgBody, messageLength);
			logger.debug("Received and parsed Update Message: {}", msg);
			break;
		case 3:
			msg = BGPNotificationMessageParser.parse(msgBody);
			logger.debug("Received and parsed Notification Message: {}", msg);
			break;
		case 4:
			msg = new BGPKeepAliveMessage();
			if (messageLength != COMMON_HEADER_LENGTH) {
				throw new BGPDocumentedException("Message length field not within valid range.", BGPError.BAD_MSG_LENGTH, ByteArray.subByte(
						bs, 0, LENGTH_FIELD_LENGTH));
			}
			break;
		default:
			throw new BGPDocumentedException("Unhandled message type " + messageType, BGPError.BAD_MSG_TYPE, new byte[] { bs[LENGTH_FIELD_LENGTH] });
		}

		return Lists.newArrayList(msg);
	}

	@Override
	public byte[] put(final BGPMessage msg) {
		if (msg == null) {
			throw new IllegalArgumentException("BGPMessage is mandatory.");
		}

		logger.trace("Serializing {}", msg);

		byte[] msgBody = null;
		int msgType = 0;

		/*
		 * Update message is not supported
		 */
		if (msg instanceof BGPOpenMessage) {
			msgType = 1;
			msgBody = BGPOpenMessageParser.put((BGPOpenMessage) msg);
		} else if (msg instanceof BGPNotificationMessage) {
			msgType = 3;
			msgBody = BGPNotificationMessageParser.put((BGPNotificationMessage) msg);
		} else if (msg instanceof BGPKeepAliveMessage) {
			msgType = 4;
			msgBody = new byte[0];
		} else {
			throw new IllegalArgumentException("Unknown instance of BGPMessage. Passed " + msg.getClass());
		}

		final byte[] headerBytes = headerToBytes(msgBody.length + COMMON_HEADER_LENGTH, msgType);
		final byte[] retBytes = new byte[headerBytes.length + msgBody.length];

		ByteArray.copyWhole(headerBytes, retBytes, 0);
		ByteArray.copyWhole(msgBody, retBytes, COMMON_HEADER_LENGTH);

		logger.trace("Serialized BGP message {}.", Arrays.toString(retBytes));
		return retBytes;
	}

	/**
	 * Serializes this BGP Message header to byte array.
	 * 
	 * @return byte array representation of this header
	 */
	public byte[] headerToBytes(final int msgLength, final int msgType) {
		final byte[] retBytes = new byte[COMMON_HEADER_LENGTH];

		Arrays.fill(retBytes, 0, MARKER_LENGTH, (byte) 0xff);

		System.arraycopy(ByteArray.intToBytes(msgLength), Integer.SIZE / Byte.SIZE - LENGTH_FIELD_LENGTH, retBytes, MARKER_LENGTH,
				LENGTH_FIELD_LENGTH);

		retBytes[MARKER_LENGTH + LENGTH_FIELD_LENGTH] = (byte) msgType;

		return retBytes;
	}
}
