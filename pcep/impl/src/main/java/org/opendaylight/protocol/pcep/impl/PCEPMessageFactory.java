/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.framework.DeserializerException;
import org.opendaylight.protocol.framework.DocumentedException;
import org.opendaylight.protocol.framework.ProtocolMessageFactory;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

/**
 * A PCEP message parser which also does validation.
 */
public final class PCEPMessageFactory implements ProtocolMessageFactory<Message> {

	private final static Logger logger = LoggerFactory.getLogger(PCEPMessageFactory.class);

	private final static int TYPE_SIZE = 1; // bytes

	private final static int LENGTH_SIZE = 2; // bytes

	public final static int COMMON_HEADER_LENGTH = 4; // bytes

	/**
	 * Current supported version of PCEP.
	 */
	public static final int PCEP_VERSION = 1;

	private static final int VERSION_SF_LENGTH = 3;

	private static final int VER_FLAGS_MF_LENGTH = 1;
	private static final int TYPE_F_LENGTH = 1;
	private static final int LENGTH_F_LENGTH = 2;

	private static final int VER_FLAGS_MF_OFFSET = 0;
	private static final int TYPE_F_OFFSET = VER_FLAGS_MF_LENGTH + VER_FLAGS_MF_OFFSET;
	private static final int LENGTH_F_OFFSET = TYPE_F_LENGTH + TYPE_F_OFFSET;

	private final MessageHandlerRegistry registry;

	public PCEPMessageFactory(final MessageHandlerRegistry registry) {
		this.registry = Preconditions.checkNotNull(registry);
	}

	@Override
	public List<Message> parse(final byte[] bytes) throws DeserializerException, DocumentedException {
		Preconditions.checkArgument(bytes != null, "Bytes may not be null");
		Preconditions.checkArgument(bytes.length != 0, "Bytes may not be empty");

		logger.trace("Attempt to parse message from bytes: {}", ByteArray.bytesToHexString(bytes));

		final int type = UnsignedBytes.toInt(bytes[1]);

		final int msgLength = ByteArray.bytesToInt(ByteArray.subByte(bytes, TYPE_SIZE + 1, LENGTH_SIZE));

		final byte[] msgBody = ByteArray.cutBytes(bytes, TYPE_SIZE + 1 + LENGTH_SIZE);

		if (msgBody.length != msgLength - COMMON_HEADER_LENGTH) {
			throw new DeserializerException("Body size " + msgBody.length + " does not match header size "
					+ (msgLength - COMMON_HEADER_LENGTH));
		}

		Message msg = null;

		try {
			msg = this.registry.getMessageParser(type).parseMessage(msgBody);
		} catch (final PCEPDeserializerException e) {
			logger.debug("Unexpected deserializer problem", e);
			throw new DeserializerException(e.getMessage(), e);
		} catch (final PCEPDocumentedException e) {
			logger.debug("Documented deserializer problem", e);
			throw new DocumentedException(e.getMessage(), e);
		}
		logger.debug("Message was parsed. {}", msg);
		return Arrays.asList(msg);
	}

	@Override
	public byte[] put(final Message msg) {
		if (msg == null) {
			throw new IllegalArgumentException("PCEPMessage is mandatory.");
		}

		final ByteBuf buf = new UnpooledByteBufAllocator(false).buffer();

		final MessageSerializer serializer = this.registry.getMessageSerializer(msg);

		serializer.serializeMessage(msg, buf);

		final byte[] msgBody = buf.array();

		final byte[] headerBytes = new byte[COMMON_HEADER_LENGTH];

		// msgVer_Flag
		headerBytes[VER_FLAGS_MF_OFFSET] = (byte) (PCEP_VERSION << (Byte.SIZE - VERSION_SF_LENGTH));

		// msgType
		headerBytes[TYPE_F_OFFSET] = (byte) serializer.getMessageType();

		// msgLength
		System.arraycopy(ByteArray.intToBytes(msgBody.length), Integer.SIZE / Byte.SIZE - LENGTH_F_LENGTH, headerBytes, LENGTH_F_OFFSET,
				LENGTH_F_LENGTH);

		final byte[] retBytes = new byte[headerBytes.length + msgBody.length];

		ByteArray.copyWhole(headerBytes, retBytes, 0);
		ByteArray.copyWhole(msgBody, retBytes, PCEPMessageHeader.COMMON_HEADER_LENGTH);

		return retBytes;
	}
}
