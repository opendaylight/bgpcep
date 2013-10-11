/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.open.SimpleParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.UnsignedBytes;

/**
 * Parser for BGP Open message.
 */
public final class BGPOpenMessageParser implements MessageParser, MessageSerializer {
	public static final MessageParser PARSER;
	public static final MessageSerializer SERIALIZER;

	static {
		final BGPOpenMessageParser p = new BGPOpenMessageParser();
		PARSER = p;
		SERIALIZER = p;
	}

	private static final Logger logger = LoggerFactory.getLogger(BGPOpenMessageParser.class);

	private static final int VERSION_SIZE = 1;
	private static final int AS_SIZE = 2;
	private static final int HOLD_TIME_SIZE = 2;
	private static final int BGP_ID_SIZE = 4;
	private static final int OPT_PARAM_LENGTH_SIZE = 1;

	private static final int MIN_MSG_LENGTH = VERSION_SIZE + AS_SIZE + HOLD_TIME_SIZE + BGP_ID_SIZE + OPT_PARAM_LENGTH_SIZE;

	private static final int BGP_VERSION = 4;

	@Deprecated
	private static final ParameterRegistry reg = SimpleParameterRegistry.INSTANCE;

	private BGPOpenMessageParser() {

	}

	/**
	 * Serializes given BGP Open message to byte array, without the header.
	 * 
	 * @param msg BGP Open message to be serialized.
	 * @return BGP Open message converted to byte array
	 */
	@Override
	public byte[] serializeMessage(final Notification msg) {
		if (msg == null) {
			throw new IllegalArgumentException("BGPOpen message cannot be null");
		}
		logger.trace("Started serializing open message: {}", msg);
		final Open open = (Open) msg;

		final Map<byte[], Integer> optParams = Maps.newHashMap();

		int optParamsLength = 0;

		if (open.getBgpParameters() != null) {
			for (final BgpParameters param : open.getBgpParameters()) {
				final byte[] p = reg.serializeParameter(param);
				if (p != null) {
					optParams.put(p, p.length);
					optParamsLength += p.length;
				}
			}
		}

		final byte[] msgBody = new byte[MIN_MSG_LENGTH + optParamsLength];

		int offset = 0;

		msgBody[offset] = ByteArray.intToBytes(BGP_VERSION)[(Integer.SIZE / Byte.SIZE) - 1];
		offset += VERSION_SIZE;

		// When our AS number does not fit into two bytes, we report it as AS_TRANS
		int openAS = open.getMyAsNumber();
		if (openAS > 65535) {
			openAS = 2345;
		}

		System.arraycopy(ByteArray.longToBytes(openAS), 6, msgBody, offset, AS_SIZE);
		offset += AS_SIZE;

		System.arraycopy(ByteArray.intToBytes(open.getHoldTimer()), 2, msgBody, offset, HOLD_TIME_SIZE);
		offset += HOLD_TIME_SIZE;

		System.arraycopy(Ipv4Util.bytesForAddress(open.getBgpIdentifier()), 0, msgBody, offset, BGP_ID_SIZE);
		offset += BGP_ID_SIZE;

		msgBody[offset] = ByteArray.intToBytes(optParamsLength)[Integer.SIZE / Byte.SIZE - 1];

		int index = MIN_MSG_LENGTH;
		if (optParams != null) {
			for (final Entry<byte[], Integer> entry : optParams.entrySet()) {
				System.arraycopy(entry.getKey(), 0, msgBody, index, entry.getValue());
				index += entry.getValue();
			}
		}

		final byte[] ret = MessageUtil.formatMessage(1, msgBody);
		logger.trace("Open message serialized to: {}", Arrays.toString(ret));
		return ret;
	}

	/**
	 * Parses given byte array to BGP Open message
	 * 
	 * @param body byte array representing BGP Open message, without header
	 * @return BGP Open Message
	 * @throws BGPDocumentedException if the parsing was unsuccessful
	 */
	@Override
	public Open parseMessageBody(final byte[] body, final int messageLength) throws BGPDocumentedException {
		if (body == null) {
			throw new IllegalArgumentException("Byte array cannot be null.");
		}
		logger.trace("Started parsing of open message: {}", Arrays.toString(body));

		if (body.length < MIN_MSG_LENGTH) {
			throw BGPDocumentedException.badMessageLength("Open message too small.", messageLength);
		}
		if (UnsignedBytes.toInt(body[0]) != BGP_VERSION) {
			throw new BGPDocumentedException("BGP Protocol version " + UnsignedBytes.toInt(body[0]) + " not supported.", BGPError.VERSION_NOT_SUPPORTED, ByteArray.subByte(
					ByteArray.intToBytes(BGP_VERSION), 2, 2));
		}

		int offset = VERSION_SIZE;
		final AsNumber as = new AsNumber(ByteArray.bytesToLong(ByteArray.subByte(body, offset, AS_SIZE)));
		offset += AS_SIZE;

		// TODO: BAD_PEER_AS Error: when is an AS unacceptable?

		final short holdTime = ByteArray.bytesToShort(ByteArray.subByte(body, offset, HOLD_TIME_SIZE));
		offset += HOLD_TIME_SIZE;
		if (holdTime == 1 || holdTime == 2) {
			throw new BGPDocumentedException("Hold time value not acceptable.", BGPError.HOLD_TIME_NOT_ACC);
		}

		Ipv4Address bgpId = null;
		try {
			bgpId = Ipv4Util.addressForBytes(ByteArray.subByte(body, offset, BGP_ID_SIZE));
		} catch (final IllegalArgumentException e) {
			throw new BGPDocumentedException("BGP Identifier is not a valid IPv4 Address", BGPError.BAD_BGP_ID);
		}
		offset += BGP_ID_SIZE;

		final int optLength = UnsignedBytes.toInt(body[offset]);

		List<BgpParameters> optParams = Lists.newArrayList();
		if (optLength > 0) {
			fillParams(ByteArray.subByte(body, MIN_MSG_LENGTH, optLength), optParams);
		}
		logger.trace("Open message was parsed: AS = {}, holdTimer = {}, bgpId = {}, optParams = {}", as, holdTime, bgpId, optParams);
		return new OpenBuilder().setMyAsNumber(as.getValue().intValue()).setHoldTimer((int) holdTime).setBgpIdentifier(bgpId).setBgpParameters(
				optParams).build();
	}

	private void fillParams(final byte[] bytes, final List<BgpParameters> params) throws BGPDocumentedException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Byte array cannot be null or empty.");
		}

		logger.trace("Started parsing of BGP parameter: {}", Arrays.toString(bytes));
		int byteOffset = 0;
		while (byteOffset < bytes.length) {
			if (byteOffset + 2 >= bytes.length) {
				// FIXME: throw a BGPDocumentedException here?
				throw new IllegalArgumentException("Malformed parameter encountered (" + (bytes.length - byteOffset) + " bytes left)");
			}

			final int paramType = UnsignedBytes.toInt(bytes[byteOffset++]);
			final int paramLength = UnsignedBytes.toInt(bytes[byteOffset++]);
			final byte[] paramBody = ByteArray.subByte(bytes, byteOffset, paramLength);
			byteOffset += paramLength;

			final BgpParameters param;
			try {
				param = reg.parseParameter(paramType, paramBody);
			} catch (final BGPParsingException e) {
				throw new BGPDocumentedException("Optional parameter not parsed: ." + e.getMessage(), BGPError.UNSPECIFIC_OPEN_ERROR);
			}

			if (param != null) {
				params.add(param);
			} else {
				logger.debug("Ignoring BGP Parameter type: {}", paramType);
			}
		}

		logger.trace("Parsed BGP parameters: {}", Arrays.toString(params.toArray()));
	}
}
