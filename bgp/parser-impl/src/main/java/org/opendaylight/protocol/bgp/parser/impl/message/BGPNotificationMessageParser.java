/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.message.BGPNotificationMessage;
import org.opendaylight.protocol.util.ByteArray;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for BGPNotification message.
 */
public final class BGPNotificationMessageParser {

	private static final Logger logger = LoggerFactory.getLogger(BGPNotificationMessageParser.class);

	private static final int ERROR_SIZE = 2; // bytes

	private static class BGPErr {
		private final int errorCode;
		private final int errorSubcode;

		public BGPErr(final int errorCode, final int errorSubcode) {
			this.errorCode = errorCode;
			this.errorSubcode = errorSubcode;
		}

		public int getErrorCode() {
			return this.errorCode;
		}

		public int getErrorSubcode() {
			return this.errorSubcode;
		}
	}

	/**
	 * Serializes BGP Notification message.
	 * 
	 * @param msg to be serialized
	 * @return BGP Notification message converted to byte array
	 */
	public static byte[] put(final BGPNotificationMessage msg) {
		if (msg == null)
			throw new IllegalArgumentException("BGP Notification message cannot be null");
		logger.trace("Started serializing Notification message: {}", msg);

		final byte[] msgBody = (msg.getData() == null) ? new byte[ERROR_SIZE] : new byte[ERROR_SIZE + msg.getData().length];

		final BGPErr numError = parseBGPError(msg.getError());

		if (numError == null)
			throw new IllegalArgumentException("Cannot parse BGP Error: " + msg.getError());

		msgBody[0] = ByteArray.intToBytes(numError.getErrorCode())[Integer.SIZE / Byte.SIZE - 1];

		msgBody[1] = ByteArray.intToBytes(numError.getErrorSubcode())[Integer.SIZE / Byte.SIZE - 1];

		if (msg.getData() != null)
			System.arraycopy(msg.getData(), 0, msgBody, ERROR_SIZE, msg.getData().length);
		logger.trace("Notification message serialized to: {}", Arrays.toString(msgBody));
		return msgBody;
	}

	/**
	 * Parses BGP Notification message to bytes.
	 * 
	 * @param bytes byte array to be parsed
	 * @return BGPNotification message
	 * @throws BGPDocumentedException
	 */
	public static BGPNotificationMessage parse(final byte[] bytes) throws BGPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array cannot be null or empty.");
		logger.trace("Started parsing of notification message: {}", Arrays.toString(bytes));

		if (bytes.length < ERROR_SIZE)
			throw new BGPDocumentedException("Notification message too small.", BGPError.BAD_MSG_LENGTH, ByteArray.intToBytes(bytes.length));
		final int errorCode = UnsignedBytes.toInt(bytes[0]);
		final int errorSubcode = UnsignedBytes.toInt(bytes[1]);

		final BGPError err = putBGPError(new BGPErr(errorCode, errorSubcode));
		byte[] data = null;
		if (bytes.length > ERROR_SIZE) {
			data = ByteArray.subByte(bytes, ERROR_SIZE, bytes.length - ERROR_SIZE);
		}
		logger.trace("Notification message was parsed: err = {}, data = {}.", err, Arrays.toString(data));
		return (data == null) ? new BGPNotificationMessage(err) : new BGPNotificationMessage(err, data);
	}

	private static BGPErr parseBGPError(final BGPError err) {
		switch (err) {
		case CONNECTION_NOT_SYNC:
			return new BGPErr(1, 1);
		case BAD_MSG_LENGTH:
			return new BGPErr(1, 2);
		case BAD_MSG_TYPE:
			return new BGPErr(1, 3);
		case UNSPECIFIC_OPEN_ERROR:
			return new BGPErr(2, 0);
		case VERSION_NOT_SUPPORTED:
			return new BGPErr(2, 1);
		case BAD_PEER_AS:
			return new BGPErr(2, 2);
		case BAD_BGP_ID:
			return new BGPErr(2, 3);
		case OPT_PARAM_NOT_SUPPORTED:
			return new BGPErr(2, 4);
		case HOLD_TIME_NOT_ACC:
			return new BGPErr(2, 6);
		case MALFORMED_ATTR_LIST:
			return new BGPErr(3, 1);
		case WELL_KNOWN_ATTR_NOT_RECOGNIZED:
			return new BGPErr(3, 2);
		case WELL_KNOWN_ATTR_MISSING:
			return new BGPErr(3, 3);
		case ATTR_FLAGS_MISSING:
			return new BGPErr(3, 4);
		case ATTR_LENGTH_ERROR:
			return new BGPErr(3, 5);
		case ORIGIN_ATTR_NOT_VALID:
			return new BGPErr(3, 6);
		case NEXT_HOP_NOT_VALID:
			return new BGPErr(3, 8);
		case OPT_ATTR_ERROR:
			return new BGPErr(3, 9);
		case NETWORK_NOT_VALID:
			return new BGPErr(3, 10);
		case AS_PATH_MALFORMED:
			return new BGPErr(3, 11);
		case HOLD_TIMER_EXPIRED:
			return new BGPErr(4, 0);
		case FSM_ERROR:
			return new BGPErr(5, 0);
		case CEASE:
			return new BGPErr(6, 0);
		default:
			return null;
		}
	}

	private static BGPError putBGPError(final BGPErr err) {
		final int e = err.getErrorCode();
		final int s = err.getErrorSubcode();
		if (e == 1) {
			if (s == 1)
				return BGPError.CONNECTION_NOT_SYNC;
			if (s == 2)
				return BGPError.BAD_MSG_LENGTH;
			if (s == 3)
				return BGPError.BAD_MSG_TYPE;
		} else if (e == 2) {
			if (s == 0)
				return BGPError.UNSPECIFIC_OPEN_ERROR;
			if (s == 1)
				return BGPError.VERSION_NOT_SUPPORTED;
			if (s == 2)
				return BGPError.BAD_PEER_AS;
			if (s == 3)
				return BGPError.BAD_BGP_ID;
			if (s == 4)
				return BGPError.OPT_PARAM_NOT_SUPPORTED;
			if (s == 6)
				return BGPError.HOLD_TIME_NOT_ACC;
		} else if (e == 3) {
			if (s == 1)
				return BGPError.MALFORMED_ATTR_LIST;
			if (s == 2)
				return BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED;
			if (s == 3)
				return BGPError.WELL_KNOWN_ATTR_MISSING;
			if (s == 4)
				return BGPError.ATTR_FLAGS_MISSING;
			if (s == 5)
				return BGPError.ATTR_LENGTH_ERROR;
			if (s == 6)
				return BGPError.ORIGIN_ATTR_NOT_VALID;
			if (s == 8)
				return BGPError.NEXT_HOP_NOT_VALID;
			if (s == 9)
				return BGPError.OPT_ATTR_ERROR;
			if (s == 10)
				return BGPError.NETWORK_NOT_VALID;
			if (s == 11)
				return BGPError.AS_PATH_MALFORMED;
		} else if (e == 4)
			return BGPError.HOLD_TIMER_EXPIRED;
		else if (e == 5)
			return BGPError.FSM_ERROR;
		else if (e == 6)
			return BGPError.CEASE;
		throw new IllegalArgumentException("BGP Error code " + e + " and subcode " + s + " not recognized.");
	}
}
