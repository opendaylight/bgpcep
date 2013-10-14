/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import java.util.Arrays;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedBytes;

abstract class AbstractMessageRegistry implements MessageRegistry {
	private final static Logger logger = LoggerFactory.getLogger(AbstractMessageRegistry.class);

	protected abstract Notification parseBody(final int type, final byte[] body, final int messageLength) throws BGPDocumentedException;
	protected abstract byte[] serializeMessageImpl(final Notification message);

	@Override
	public final Notification parseMessage(final byte[] bytes) throws BGPDocumentedException, DeserializerException {
		if (bytes == null) {
			throw new IllegalArgumentException("Array of bytes is mandatory.");
		}
		if (bytes.length < MessageUtil.COMMON_HEADER_LENGTH) {
			throw new IllegalArgumentException("Too few bytes in passed array. Passed: " + bytes.length + ". Expected: >= "
					+ MessageUtil.COMMON_HEADER_LENGTH + ".");
		}
		/*
		 * byte array starts with message length
		 */
		// final byte[] ones = new byte[MARKER_LENGTH];
		// Arrays.fill(ones, (byte)0xff);
		// if (Arrays.equals(bytes, ones))
		// throw new BGPDocumentedException("Marker not set to ones.", BGPError.CONNECTION_NOT_SYNC);
		final byte[] bs = ByteArray.cutBytes(bytes, MessageUtil.MARKER_LENGTH);
		final int messageLength = ByteArray.bytesToInt(ByteArray.subByte(bs, 0, MessageUtil.LENGTH_FIELD_LENGTH));
		final int messageType = UnsignedBytes.toInt(bs[MessageUtil.LENGTH_FIELD_LENGTH]);

		final byte[] msgBody = ByteArray.cutBytes(bs, MessageUtil.LENGTH_FIELD_LENGTH + MessageUtil.TYPE_FIELD_LENGTH);

		if (messageLength < MessageUtil.COMMON_HEADER_LENGTH) {
			throw BGPDocumentedException.badMessageLength("Message length field not within valid range.", messageLength);
		}
		if (msgBody.length != messageLength - MessageUtil.COMMON_HEADER_LENGTH) {
			throw new DeserializerException("Size doesn't match size specified in header. Passed: " + msgBody.length + "; Expected: "
					+ (messageLength - MessageUtil.COMMON_HEADER_LENGTH) + ". ");
		}

		logger.debug("Attempt to parse message from bytes: {}", ByteArray.bytesToHexString(msgBody));

		final Notification msg = parseBody(messageType, msgBody, messageLength);
		if (msg == null) {
			throw new BGPDocumentedException("Unhandled message type " + messageType, BGPError.BAD_MSG_TYPE, new byte[] { bs[MessageUtil.LENGTH_FIELD_LENGTH] });
		}

		return msg;
	}

	@Override
	public final byte[] serializeMessage(final Notification message) {
		if (message == null) {
			throw new IllegalArgumentException("BGPMessage is mandatory.");
		}

		logger.trace("Serializing {}", message);

		final byte[] ret = serializeMessageImpl(message);
		if (ret == null) {
			throw new IllegalArgumentException("Unknown instance of BGPMessage. Passed " + message.getClass());
		}

		logger.trace("Serialized BGP message {}.", Arrays.toString(ret));
		return ret;
	}
}
