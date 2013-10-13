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

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.impl.message.update.SimpleAttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.WithdrawnRoutesBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * LENGTH fields, that denote the length of the fields with variable length, have fixed SIZE.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.3">BGP-4 Update Message Format</a>
 * 
 */
public class BGPUpdateMessageParser implements MessageParser {
	public static final int TYPE = 2;
	public static final BGPUpdateMessageParser PARSER = new BGPUpdateMessageParser(SimpleAttributeRegistry.getInstance());

	private static Logger logger = LoggerFactory.getLogger(BGPUpdateMessageParser.class);

	/**
	 * Size of the withdrawn_routes_length field, in bytes.
	 */
	public static final int WITHDRAWN_ROUTES_LENGTH_SIZE = 2;

	/**
	 * Size of the total_path_attr_length field, in bytes.
	 */
	public static final int TOTAL_PATH_ATTR_LENGTH_SIZE = 2;

	private final AttributeRegistry reg;

	// Constructors -------------------------------------------------------
	public BGPUpdateMessageParser(final AttributeRegistry reg) {
		this.reg = Preconditions.checkNotNull(reg);
	}

	// Getters & setters --------------------------------------------------

	@Override
	public Update parseMessageBody(final byte[] body, final int messageLength) throws BGPDocumentedException {
		if (body == null || body.length == 0) {
			throw new IllegalArgumentException("Byte array cannot be null or empty.");
		}
		logger.trace("Started parsing of update message: {}", Arrays.toString(body));

		int byteOffset = 0;

		final int withdrawnRoutesLength = ByteArray.bytesToInt(ByteArray.subByte(body, byteOffset, WITHDRAWN_ROUTES_LENGTH_SIZE));
		byteOffset += WITHDRAWN_ROUTES_LENGTH_SIZE;

		final UpdateBuilder eventBuilder = new UpdateBuilder();

		if (withdrawnRoutesLength > 0) {
			final List<Ipv4Prefix> withdrawnRoutes = Ipv4Util.prefixListForBytes(ByteArray.subByte(body, byteOffset, withdrawnRoutesLength));
			byteOffset += withdrawnRoutesLength;
			eventBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setWithdrawnRoutes(withdrawnRoutes).build());
		}

		final int totalPathAttrLength = ByteArray.bytesToInt(ByteArray.subByte(body, byteOffset, TOTAL_PATH_ATTR_LENGTH_SIZE));
		byteOffset += TOTAL_PATH_ATTR_LENGTH_SIZE;

		if (withdrawnRoutesLength + totalPathAttrLength > body.length) {
			throw new BGPDocumentedException("Message length inconsistent with withdrawn router length.", BGPError.MALFORMED_ATTR_LIST);
		}

		if (withdrawnRoutesLength == 0 && totalPathAttrLength == 0) {
			return eventBuilder.build();
		}

		try {
			if (totalPathAttrLength > 0) {
				final PathAttributes pathAttributes = reg.parseAttributes(ByteArray.subByte(body, byteOffset,
						totalPathAttrLength));
				byteOffset += totalPathAttrLength;
				eventBuilder.setPathAttributes(pathAttributes);
			}
		} catch (final BGPParsingException e) {
			logger.warn("Could not parse BGP attributes: {}", e.getMessage(), e);
			throw new BGPDocumentedException("Could not parse BGP attributes.", BGPError.MALFORMED_ATTR_LIST);
		}

		final List<Ipv4Prefix> nlri = Ipv4Util.prefixListForBytes(ByteArray.subByte(body, byteOffset, body.length - byteOffset));
		eventBuilder.setNlri(new NlriBuilder().setNlri(nlri).build());

		logger.trace("Update message was parsed.");
		return eventBuilder.build();
	}
}
