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
import org.opendaylight.protocol.bgp.parser.BGPMessageFactory;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPKeepAliveMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPNotificationMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPOpenMessageParser;
import org.opendaylight.protocol.bgp.parser.impl.message.BGPUpdateMessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

/**
 * The byte array
 */
public final class BGPMessageFactoryImpl implements BGPMessageFactory {

	public static final BGPMessageFactory INSTANCE;

	static {
		final HandlerRegistry<Notification, MessageParser, MessageSerializer> reg = new HandlerRegistry<>();

		reg.registerParser(1, BGPOpenMessageParser.PARSER);
		reg.registerSerializer(Open.class, BGPOpenMessageParser.SERIALIZER);
		reg.registerParser(2, BGPUpdateMessageParser.PARSER);
		// Serialization of Update message is not supported
		reg.registerParser(3, BGPNotificationMessageParser.PARSER);
		reg.registerSerializer(Notify.class, BGPNotificationMessageParser.SERIALIZER);
		reg.registerParser(4, BGPKeepAliveMessageParser.PARSER);
		reg.registerSerializer(Keepalive.class, BGPKeepAliveMessageParser.SERIALIZER);

		INSTANCE = new BGPMessageFactoryImpl(reg);
	}

	private final static Logger logger = LoggerFactory.getLogger(BGPMessageFactoryImpl.class);

	private final static int TYPE_FIELD_LENGTH = 1; // bytes

	@VisibleForTesting
	final static int LENGTH_FIELD_LENGTH = 2; // bytes

	@VisibleForTesting
	final static int MARKER_LENGTH = 16; // bytes

	@VisibleForTesting
	final static int COMMON_HEADER_LENGTH = LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + MARKER_LENGTH;

	private final HandlerRegistry<Notification, MessageParser, MessageSerializer> handlers;

	private BGPMessageFactoryImpl(final HandlerRegistry<Notification, MessageParser, MessageSerializer> handlers) {
		this.handlers = Preconditions.checkNotNull(handlers);
	}

	private Notification parseBody(final int type, final byte[] body, final int messageLength) throws BGPDocumentedException {
		final MessageParser parser = handlers.getParser(type);
		if (parser == null) {
			return null;
		}

		return parser.parseMessage(body, messageLength);
	}

	private byte[] serializeMessage(final Notification message) {
		final MessageSerializer serializer = handlers.getSerializer(message);
		if (serializer == null) {
			return null;
		}

		final byte[] msgBody = serializer.serializeMessage(message);
		final byte[] retBytes = formatMessage(serializer.messageType(), msgBody);

		return retBytes;
	}

	/*
	 * (non-Javadoc)
	 * @see org.opendaylight.protocol.bgp.parser.BGPMessageParser#parse(byte[])
	 */
	@Override
	public List<Notification> parse(final byte[] bytes) throws DeserializerException, DocumentedException {
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
			throw BGPDocumentedException.badMessageLength("Message length field not within valid range.", messageLength);
		}
		if (msgBody.length != messageLength - COMMON_HEADER_LENGTH) {
			throw new DeserializerException("Size doesn't match size specified in header. Passed: " + msgBody.length + "; Expected: "
					+ (messageLength - COMMON_HEADER_LENGTH) + ". ");
		}

		logger.debug("Attempt to parse message from bytes: {}", ByteArray.bytesToHexString(msgBody));

		final Notification msg = parseBody(messageType, msgBody, messageLength);
		if (msg == null) {
			throw new BGPDocumentedException("Unhandled message type " + messageType, BGPError.BAD_MSG_TYPE, new byte[] { bs[LENGTH_FIELD_LENGTH] });
		}

		return Lists.newArrayList(msg);
	}

	@Override
	public byte[] put(final Notification msg) {
		if (msg == null) {
			throw new IllegalArgumentException("BGPMessage is mandatory.");
		}

		logger.trace("Serializing {}", msg);

		final byte[] ret = serializeMessage(msg);
		if (ret == null) {
			throw new IllegalArgumentException("Unknown instance of BGPMessage. Passed " + msg.getClass());
		}

		logger.trace("Serialized BGP message {}.", Arrays.toString(ret));
		return ret;
	}

	/**
	 * Serializes this BGP Message header to byte array.
	 * 
	 * @return byte array representation of this header
	 */
	private byte[] formatMessage(final int msgType, final byte[] body) {
		final byte[] retBytes = new byte[COMMON_HEADER_LENGTH + body.length];

		Arrays.fill(retBytes, 0, MARKER_LENGTH, (byte) 0xff);
		System.arraycopy(ByteArray.intToBytes(body.length + COMMON_HEADER_LENGTH),
				Integer.SIZE / Byte.SIZE - LENGTH_FIELD_LENGTH,
				retBytes, MARKER_LENGTH, LENGTH_FIELD_LENGTH);

		retBytes[MARKER_LENGTH + LENGTH_FIELD_LENGTH] = UnsignedBytes.checkedCast(msgType);
		ByteArray.copyWhole(body, retBytes, COMMON_HEADER_LENGTH);

		return retBytes;
	}
}
