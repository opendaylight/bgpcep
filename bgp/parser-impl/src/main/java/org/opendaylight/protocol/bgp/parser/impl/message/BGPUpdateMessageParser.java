/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.opendaylight.protocol.bgp.concepts.BGPAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPSubsequentAddressFamily;
import org.opendaylight.protocol.bgp.concepts.BGPTableType;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BGPUpdateEvent;
import org.opendaylight.protocol.bgp.parser.BGPUpdateSynchronized;
import org.opendaylight.protocol.bgp.parser.impl.BGPMessageFactory;
import org.opendaylight.protocol.bgp.parser.impl.BGPUpdateEventBuilder;
import org.opendaylight.protocol.bgp.parser.impl.IPv6MP;
import org.opendaylight.protocol.bgp.parser.impl.PathAttribute;
import org.opendaylight.protocol.bgp.parser.impl.PathAttribute.TypeCode;
import org.opendaylight.protocol.bgp.parser.impl.message.update.PathAttributeParser;
import org.opendaylight.protocol.concepts.IPv4;
import org.opendaylight.protocol.concepts.IPv4Address;
import org.opendaylight.protocol.concepts.Prefix;
import org.opendaylight.protocol.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * LENGTH fields, that denote the length of the fields with variable length, have fixed SIZE.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc4271#section-4.3">BGP-4 Update Message Format</a>
 * 
 */
public class BGPUpdateMessageParser {

	private static Logger logger = LoggerFactory.getLogger(BGPUpdateMessageParser.class);

	/**
	 * Size of the withdrawn_routes_length field, in bytes.
	 */
	public static final int WITHDRAWN_ROUTES_LENGTH_SIZE = 2;

	/**
	 * Size of the total_path_attr_length field, in bytes.
	 */
	public static final int TOTAL_PATH_ATTR_LENGTH_SIZE = 2;

	// Constructors -------------------------------------------------------

	public BGPUpdateMessageParser() {

	}

	// Getters & setters --------------------------------------------------

	public static BGPUpdateEvent parse(final byte[] bytes, final int msgLength) throws BGPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array cannot be null or empty.");
		logger.trace("Started parsing of update message: {}", Arrays.toString(bytes));

		int byteOffset = 0;

		final int withdrawnRoutesLength = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, WITHDRAWN_ROUTES_LENGTH_SIZE));
		byteOffset += WITHDRAWN_ROUTES_LENGTH_SIZE;

		final BGPUpdateEventBuilder eventBuilder = new BGPUpdateEventBuilder();
		eventBuilder.setWithdrawnRoutesLength(withdrawnRoutesLength);

		Set<Prefix<IPv4Address>> withdrawnRoutes;
		if (withdrawnRoutesLength > 0) {
			withdrawnRoutes = IPv4.FAMILY.prefixListForBytes(ByteArray.subByte(bytes, byteOffset, withdrawnRoutesLength));
			byteOffset += withdrawnRoutesLength;
		} else {
			withdrawnRoutes = Collections.emptySet();
		}
		eventBuilder.setWithdrawnRoutes(withdrawnRoutes);

		final int totalPathAttrLength = ByteArray.bytesToInt(ByteArray.subByte(bytes, byteOffset, TOTAL_PATH_ATTR_LENGTH_SIZE));
		byteOffset += TOTAL_PATH_ATTR_LENGTH_SIZE;
		eventBuilder.setTotalPathAttrLength(totalPathAttrLength);

		if (withdrawnRoutesLength + totalPathAttrLength + BGPMessageFactory.COMMON_HEADER_LENGTH > msgLength)
			throw new BGPDocumentedException("Message length inconsistent with withdrawn router length.", BGPError.MALFORMED_ATTR_LIST);

		if (withdrawnRoutesLength == 0 && totalPathAttrLength == 0) {
			final BGPUpdateSynchronized event = new BGPUpdateSynchronized() {

				private static final long serialVersionUID = 5709361453437508337L;

				@Override
				public BGPTableType getTableType() {
					return new BGPTableType(BGPAddressFamily.IPv4, BGPSubsequentAddressFamily.Unicast);
				}
			};
			return event;
		}

		List<PathAttribute> pathAttributes;
		if (totalPathAttrLength > 0) {
			pathAttributes = parsePathAttributes(ByteArray.subByte(bytes, byteOffset, totalPathAttrLength));
			byteOffset += totalPathAttrLength;
			if (pathAttributes.get(0).getType() == TypeCode.MP_UNREACH_NLRI && totalPathAttrLength == 6) {
				if (pathAttributes.get(0).getValue() instanceof IPv6MP) {
					final BGPUpdateEvent event = new BGPUpdateSynchronized() {

						private static final long serialVersionUID = -6026212683738125407L;

						@Override
						public BGPTableType getTableType() {
							return new BGPTableType(BGPAddressFamily.IPv6, BGPSubsequentAddressFamily.Unicast);
						}

					};
					return event;
				} else if (pathAttributes.get(0).getValue() == null) {
					final BGPUpdateSynchronized event = new BGPUpdateSynchronized() {

						private static final long serialVersionUID = 5888562784007786559L;

						@Override
						public BGPTableType getTableType() {
							return new BGPTableType(BGPAddressFamily.LinkState, BGPSubsequentAddressFamily.Unicast);
						}

					};
					return event;
				}
			}
		} else {
			pathAttributes = Collections.emptyList();
		}
		eventBuilder.setPathAttributes(pathAttributes);

		final Set<Prefix<IPv4Address>> nlri = IPv4.FAMILY.prefixListForBytes(ByteArray.subByte(bytes, byteOffset, bytes.length - byteOffset));
		eventBuilder.setNlri(nlri);

		try {
			logger.trace("Update message was parsed.");
			return eventBuilder.buildEvent();
		} catch (final BGPParsingException e) {
			throw new BGPDocumentedException("Parsing unsuccessful: {}" + e.getMessage(), BGPError.MALFORMED_ATTR_LIST);
		}
	}

	/**
	 * Parse different Path Attributes from given bytes.
	 * 
	 * @param bytes byte array to be parsed
	 * @return list of Path Attributes
	 * @throws BGPParsingException
	 */
	private static List<PathAttribute> parsePathAttributes(byte[] bytes) throws BGPDocumentedException {
		if (bytes.length == 0) {
			return Collections.emptyList();
		}
		final List<PathAttribute> list = Lists.newArrayList();
		while (bytes.length != 0) {
			PathAttribute attr;
			try {
				attr = PathAttributeParser.parseAttribute(bytes);
				bytes = ByteArray.cutBytes(bytes,
						PathAttribute.ATTR_FLAGS_SIZE + PathAttribute.ATTR_TYPE_CODE_SIZE + attr.getAttrLengthSize() + attr.getLength());
				list.add(attr);
			} catch (final BGPParsingException e) {
				logger.warn("Could not parse BGP attributes: {}", e.getMessage(), e);
				throw new BGPDocumentedException("Could not parse BGP attributes.", BGPError.MALFORMED_ATTR_LIST);
			}
		}
		return list;
	}
}
