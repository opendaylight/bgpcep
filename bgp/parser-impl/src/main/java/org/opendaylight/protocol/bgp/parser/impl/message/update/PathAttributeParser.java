/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;

import com.google.common.primitives.UnsignedBytes;

/**
 * 
 * Parser for different Path Attributes. Each attributes has its own method for parsing.
 * 
 */
public class PathAttributeParser {

	private static final int FLAGS_LENGTH = 1;

	private static final int TYPE_LENGTH = 1;

	private static AttributeRegistry reg = SimpleAttributeRegistry.INSTANCE;

	private PathAttributeParser() {

	}

	/**
	 * Parse path attribute header (the same for all path attributes) and set type, length and value fields.
	 * 
	 * @param bytes byte array to be parsed.
	 * @return generic Path Attribute
	 * @throws BGPParsingException
	 * @throws BGPDocumentedException
	 */
	public static PathAttributes parseAttribute(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		if (bytes == null || bytes.length == 0) {
			throw new BGPParsingException("Insufficient length of byte array: " + bytes.length);
		}
		int byteOffset = 0;
		final PathAttributesBuilder builder = new PathAttributesBuilder();
		while (byteOffset < bytes.length) {
			final boolean[] bits = ByteArray.parseBits(bytes[0]);
			final boolean optional = bits[0];
			final int attrLength = (bits[3]) ? ByteArray.bytesToInt(ByteArray.subByte(bytes, 2, 2)) : UnsignedBytes.toInt(bytes[2]);
			final int hdrLength = FLAGS_LENGTH + TYPE_LENGTH + ((bits[3]) ? 2 : 1);

			final byte[] attrBody = ByteArray.subByte(bytes, hdrLength, attrLength);

			boolean found = reg.parseAttribute(UnsignedBytes.toInt(bytes[1]), attrBody, builder);
			if (!optional && !found) {
				throw new BGPDocumentedException("Well known attribute not recognized.", BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED);
			}

			byteOffset += hdrLength + attrLength;
		}
		return builder.build();
	}

}
