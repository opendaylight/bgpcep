/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

public final class SimpleAttributeRegistry implements AttributeRegistry {
	private final HandlerRegistry<DataContainer, AttributeParser, AttributeSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerAttributeParser(final int attributeType, final AttributeParser parser) {
		Preconditions.checkArgument(attributeType >= 0 && attributeType <= 255);
		return handlers.registerParser(attributeType, parser);
	}

	@Override
	public AutoCloseable registerAttributeSerializer(final Class<? extends DataObject> paramClass, final AttributeSerializer serializer) {
		return handlers.registerSerializer(paramClass, serializer);
	}

	private int parseAttribute( final byte[] bytes, final int offset, final PathAttributesBuilder builder)
			throws BGPDocumentedException, BGPParsingException {
		// FIXME: validate minimum length
		final boolean[] flags = ByteArray.parseBits(bytes[offset]);
		final int type = UnsignedBytes.toInt(bytes[offset + 1]);
		final int hdrlen;
		final int len;
		if (flags[3]) {
			len = UnsignedBytes.toInt(bytes[offset + 2]) * 256 + UnsignedBytes.toInt(bytes[offset + 3]);
			hdrlen = 4;
		} else {
			len = UnsignedBytes.toInt(bytes[offset + 2]);
			hdrlen = 3;
		}

		final AttributeParser parser = handlers.getParser(type);
		if (parser == null) {
			if (!flags[0]) {
				throw new BGPDocumentedException("Well known attribute not recognized.", BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED);
			}
		} else {
			parser.parseAttribute(ByteArray.subByte(bytes, offset + hdrlen, len), builder);
		}

		return hdrlen + len;
	}

	@Override
	public PathAttributes parseAttributes(final byte[] bytes) throws BGPDocumentedException, BGPParsingException {
		int byteOffset = 0;
		final PathAttributesBuilder builder = new PathAttributesBuilder();
		while (byteOffset < bytes.length) {
			byteOffset += parseAttribute(bytes, byteOffset, builder);
		}
		return builder.build();
	}

	@Override
	public byte[] serializeAttribute(final DataObject attribute) {
		final AttributeSerializer serializer = handlers.getSerializer(attribute.getImplementedInterface());
		if (serializer == null) {
			return null;
		}

		return serializer.serializeAttribute(attribute);
	}
}
