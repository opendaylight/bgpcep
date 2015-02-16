/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.AbstractRegistration;
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
    @SuppressWarnings("unused")
    private static final int PARTIAL_BIT = 2;
    private static final int EXTENDED_LENGTH_BIT = 3;
    private final HandlerRegistry<DataContainer, AttributeParser, AttributeSerializer> handlers = new HandlerRegistry<>();
    private final Map<AbstractRegistration, AttributeSerializer> serializers = new LinkedHashMap<>();
    private final AtomicReference<Iterable<AttributeSerializer>> roSerializers =
        new AtomicReference<Iterable<AttributeSerializer>>(this.serializers.values());

    AutoCloseable registerAttributeParser(final int attributeType, final AttributeParser parser) {
        Preconditions.checkArgument(attributeType >= 0 && attributeType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(attributeType, parser);
    }

    synchronized AutoCloseable registerAttributeSerializer(final Class<? extends DataObject> paramClass, final AttributeSerializer serializer) {
        final AbstractRegistration reg = this.handlers.registerSerializer(paramClass, serializer);

        this.serializers.put(reg, serializer);
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (SimpleAttributeRegistry.this) {
                    SimpleAttributeRegistry.this.serializers.remove(reg);
                    SimpleAttributeRegistry.this.roSerializers.set(SimpleAttributeRegistry.this.serializers.values());
                }
                reg.close();
            }
        };
    }

    private void addAttribute(final ByteBuf buffer, final Map<Integer, RawAttribute> attributes) throws BGPDocumentedException {
        final boolean[] flags = ByteArray.parseBits(buffer.readByte());
        final int type = buffer.readUnsignedByte();
        final int len = (flags[EXTENDED_LENGTH_BIT]) ? buffer.readUnsignedShort() : buffer.readUnsignedByte();
        if (!attributes.containsKey(type)) {
            final AttributeParser parser = this.handlers.getParser(type);
            if (parser == null) {
                if (!flags[OPTIONAL_BIT]) {
                    throw new BGPDocumentedException("Well known attribute not recognized.", BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED);
                }
                if (flags[TRANSITIVE_BIT]) {
                    // FIXME: transitive attributes need to be preserved
                    LOG.warn("Losing unrecognized transitive attribute {}. Some data might be missing from the output.", type);
                } else {
                    LOG.warn("Ignoring unrecognized attribute type {}. Some data might be missing from the output.", type);
                }
            } else {
                attributes.put(type, new RawAttribute(parser, buffer.readSlice(len)));
            }
        } else {
            LOG.debug("Ignoring duplicate attribute type {}", type);
        }
    }

    @Override
    public PathAttributes parseAttributes(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final Map<Integer, RawAttribute> attributes = new TreeMap<>();
        while (buffer.isReadable()) {
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
        for (final AttributeSerializer serializer : this.roSerializers.get()) {
            serializer.serializeAttribute(attribute, byteAggregator);
        }
    }
}
