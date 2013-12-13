/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import java.util.Arrays;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.NotifyBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for BGPNotification message.
 */
public final class BGPNotificationMessageParser implements MessageParser, MessageSerializer {
	public static final int TYPE = 3;

	private static final Logger LOG = LoggerFactory.getLogger(BGPNotificationMessageParser.class);

	private static final int ERROR_SIZE = 2;

	/**
	 * Serializes BGP Notification message.
	 * 
	 * @param msg to be serialized
	 * @return BGP Notification message converted to byte array
	 */
	@Override
	public byte[] serializeMessage(final Notification msg) {
		if (msg == null) {
			throw new IllegalArgumentException("BGP Notification message cannot be null");
		}

		final Notify ntf = (Notify) msg;
		LOG.trace("Started serializing Notification message: {}", ntf);

		final byte[] msgBody = (ntf.getData() == null) ? new byte[ERROR_SIZE] : new byte[ERROR_SIZE + ntf.getData().length];

		msgBody[0] = UnsignedBytes.checkedCast(ntf.getErrorCode());

		msgBody[1] = UnsignedBytes.checkedCast(ntf.getErrorSubcode());

		if (ntf.getData() != null) {
			System.arraycopy(ntf.getData(), 0, msgBody, ERROR_SIZE, ntf.getData().length);
		}

		final byte[] ret = MessageUtil.formatMessage(TYPE, msgBody);
		LOG.trace("Notification message serialized to: {}", Arrays.toString(ret));
		return ret;
	}

	/**
	 * Parses BGP Notification message to bytes.
	 * 
	 * @param body byte array to be parsed
	 * @return BGPNotification message
	 * @throws BGPDocumentedException
	 */
	@Override
	public Notify parseMessageBody(final byte[] body, final int messageLength) throws BGPDocumentedException {
		if (body == null) {
			throw new IllegalArgumentException("Byte array cannot be null.");
		}
		LOG.trace("Started parsing of notification message: {}", Arrays.toString(body));

		if (body.length < ERROR_SIZE) {
			throw BGPDocumentedException.badMessageLength("Notification message too small.", messageLength);
		}
		final int errorCode = UnsignedBytes.toInt(body[0]);
		final int errorSubcode = UnsignedBytes.toInt(body[1]);

		byte[] data = null;
		if (body.length > ERROR_SIZE) {
			data = ByteArray.subByte(body, ERROR_SIZE, body.length - ERROR_SIZE);
		}
		LOG.trace("Notification message was parsed: err = {}, data = {}.", BGPError.forValue(errorCode, errorSubcode),
				Arrays.toString(data));
		final NotifyBuilder builder = new NotifyBuilder().setErrorCode((short) errorCode).setErrorSubcode((short) errorSubcode);
		if (data != null) {
			builder.setData(data);
		}
		return builder.build();
	}
}
