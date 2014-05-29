/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleAttributeRegistry implements AttributeRegistry {
    private static final class RawAttribute {
        private final AttributeParser parser;
        private final ByteBuf buffer;

        public RawAttribute(final AttributeParser parser, final ByteBuf buffer) {
            this.parser = Preconditions.checkNotNull(parser);
            this.buffer = Preconditions.checkNotNull(buffer);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SimpleAttributeRegistry.class);
    private static final int OPTIONAL_BIT = 0;
    private static final int TRANSITIVE_BIT = 1;
    private static final int PARTIAL_BIT = 2;
    private static final int EXTENDED_LENGTH_BIT = 3;
    private final HandlerRegistry<DataContainer, AttributeParser, AttributeSerializer> handlers = new HandlerRegistry<>();

    AutoCloseable registerAttributeParser(final int attributeType, final AttributeParser parser) {
        Preconditions.checkArgument(attributeType >= 0 && attributeType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(attributeType, parser);
    }

    AutoCloseable registerAttributeSerializer(final Class<? extends DataObject> paramClass, final AttributeSerializer serializer) {
        return this.handlers.registerSerializer(paramClass, serializer);
    }

    private int addAttribute(final ByteBuf buffer, final Map<Integer, RawAttribute> attributes) throws BGPDocumentedException {
        final boolean[] flags = ByteArray.parseBits(buffer.readByte());
        final Integer type = UnsignedBytes.toInt(buffer.readByte());
        final int hdrlen;
        final int len;
        if (flags[EXTENDED_LENGTH_BIT]) {
            len = UnsignedBytes.toInt(buffer.readByte()) * 256 + UnsignedBytes.toInt(buffer.readByte());
            hdrlen = 4;
        } else {
            len = UnsignedBytes.toInt(buffer.readByte());
            hdrlen = 3;
        }
        if (!attributes.containsKey(type)) {
            final AttributeParser parser = this.handlers.getParser(type);
            if (parser == null) {
                if (!flags[OPTIONAL_BIT]) {
                    throw new BGPDocumentedException("Well known attribute not recognized.", BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED);
                }
                if (flags[TRANSITIVE_BIT]) {
                    // FIXME: transitive attributes need to be preserved
                    LOG.warn("Losing unrecognized transitive attribute {}", type);
                } else {
                    LOG.debug("Ignoring unrecognized attribute type {}", type);
                }
            } else {
                attributes.put(type, new RawAttribute(parser, buffer.slice(buffer.readerIndex(), len)));
                buffer.skipBytes(len);
            }
        } else {
            LOG.debug("Ignoring duplicate attribute type {}", type);
        }
        return hdrlen + len;
    }

    @Override
    public PathAttributes parseAttributes(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final TreeMap<Integer, RawAttribute> attributes = new TreeMap<>();
        while (buffer.readableBytes() != 0) {
            addAttribute(buffer, attributes);
        }
        /*
         * TreeMap guarantees that we will be invoking the parser in the order
         * of increasing attribute type.
         */
        final PathAttributesBuilder builder = new PathAttributesBuilder();
        for (final Entry<Integer, RawAttribute> e : attributes.entrySet()) {
            LOG.debug("Parsing attribute type {}", e.getKey());

            final RawAttribute a = e.getValue();
            a.parser.parseAttribute(a.buffer, builder);
        }

        return builder.build();
    }

    @Override
    public void serializeAttribute(final DataObject attribute,final ByteBuf byteAggregator) {
        for (AttributeSerializer serializer : this.handlers.getAllSerializers()) {
            serializer.serializeAttribute(attribute, byteAggregator);
        }
    }
}
