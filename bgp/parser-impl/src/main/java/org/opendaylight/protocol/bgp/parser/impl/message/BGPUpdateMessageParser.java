/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.WithdrawnRoutesBuilder;
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

	private static final Logger LOG = LoggerFactory.getLogger(BGPUpdateMessageParser.class);

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
	public Update parseMessageBody(final ByteBuf buffer, final int messageLength) throws BGPDocumentedException {
		Preconditions.checkArgument(buffer != null && buffer.readableBytes() != 0, "Byte array cannot be null or empty.");
		LOG.trace("Started parsing of update message: {}", Arrays.toString(ByteArray.getAllBytes(buffer)));

		final int withdrawnRoutesLength = buffer.readUnsignedShort();
		final UpdateBuilder eventBuilder = new UpdateBuilder();

		if (withdrawnRoutesLength > 0) {
			final List<Ipv4Prefix> withdrawnRoutes = Ipv4Util.prefixListForBytes(ByteArray.readBytes(buffer, withdrawnRoutesLength));
			buffer.skipBytes(withdrawnRoutesLength);
			eventBuilder.setWithdrawnRoutes(new WithdrawnRoutesBuilder().setWithdrawnRoutes(withdrawnRoutes).build());
		}
		final int totalPathAttrLength = buffer.readUnsignedShort();

		if (withdrawnRoutesLength + totalPathAttrLength > buffer.readableBytes()) {
			throw new BGPDocumentedException("Message length inconsistent with withdrawn router length.", BGPError.MALFORMED_ATTR_LIST);
		}

		if (withdrawnRoutesLength == 0 && totalPathAttrLength == 0) {
			return eventBuilder.build();
		}
		if (totalPathAttrLength > 0) {
			try {
				final PathAttributes pathAttributes = this.reg.parseAttributes(buffer.slice(buffer.readerIndex(), totalPathAttrLength));
				buffer.skipBytes(totalPathAttrLength);
				eventBuilder.setPathAttributes(pathAttributes);
			} catch (final BGPDocumentedException e) {
				// Rethrow BGPDocumentedExceptions
				throw e;
			} catch (final Exception e) {
				// Catch everything else and turn it into a BGPDocumentedException
				LOG.warn("Could not parse BGP attributes", e);
				throw new BGPDocumentedException("Could not parse BGP attributes.", BGPError.MALFORMED_ATTR_LIST, e);
			}
		}
		final List<Ipv4Prefix> nlri = Ipv4Util.prefixListForBytes(ByteArray.readAllBytes(buffer));
		if (nlri != null && !nlri.isEmpty()) {
			eventBuilder.setNlri(new NlriBuilder().setNlri(nlri).build());
		}
		LOG.trace("Update message was parsed.");
		return eventBuilder.build();
	}
}
