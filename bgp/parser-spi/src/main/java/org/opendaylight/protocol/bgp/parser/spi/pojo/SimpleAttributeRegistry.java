/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.UnrecognizedAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.UnrecognizedAttributesKey;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleAttributeRegistry implements AttributeRegistry {

    private static final class RawAttribute {
        private final AttributeParser parser;
        private final ByteBuf buffer;

        public RawAttribute(final AttributeParser parser, final ByteBuf buffer) {
            this.parser = requireNonNull(parser);
            this.buffer = requireNonNull(buffer);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SimpleAttributeRegistry.class);
    private static final int OPTIONAL_BIT = 0;
    private static final int TRANSITIVE_BIT = 1;
    private static final int PARTIAL_BIT = 2;
    private static final int EXTENDED_LENGTH_BIT = 3;
    private final HandlerRegistry<DataContainer, AttributeParser, AttributeSerializer> handlers = new HandlerRegistry<>();
    private final Map<AbstractRegistration, AttributeSerializer> serializers = new LinkedHashMap<>();
    private final AtomicReference<Iterable<AttributeSerializer>> roSerializers =
        new AtomicReference<>(this.serializers.values());
    private final List<UnrecognizedAttributes> unrecognizedAttributes = new ArrayList<>();


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

    private void addAttribute(final ByteBuf buffer, final Map<Integer, RawAttribute> attributes)
            throws BGPDocumentedException {
        final BitArray flags = BitArray.valueOf(buffer.readByte());
        final int type = buffer.readUnsignedByte();
        final int len = (flags.get(EXTENDED_LENGTH_BIT)) ? buffer.readUnsignedShort() : buffer.readUnsignedByte();
        if (!attributes.containsKey(type)) {
            final AttributeParser parser = this.handlers.getParser(type);
            if (parser == null) {
                processUnrecognized(flags, type, buffer, len);
            } else {
                attributes.put(type, new RawAttribute(parser, buffer.readSlice(len)));
            }
        } else {
            LOG.debug("Ignoring duplicate attribute type {}", type);
        }
    }

    private void processUnrecognized(final BitArray flags, final int type, final ByteBuf buffer, final int len) throws BGPDocumentedException {
        if (!flags.get(OPTIONAL_BIT)) {
            throw new BGPDocumentedException("Well known attribute not recognized.", BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED);
        }
        final UnrecognizedAttributes unrecognizedAttribute = new UnrecognizedAttributesBuilder()
            .setKey(new UnrecognizedAttributesKey((short) type))
            .setPartial(flags.get(PARTIAL_BIT))
            .setTransitive(flags.get(TRANSITIVE_BIT))
            .setType((short) type)
            .setValue(ByteArray.readBytes(buffer, len)).build();
        this.unrecognizedAttributes.add(unrecognizedAttribute);
        LOG.debug("Unrecognized attribute were parsed: {}", unrecognizedAttribute);
    }

    @Override
    public Attributes parseAttributes(final ByteBuf buffer, final PeerSpecificParserConstraint constraint)
            throws BGPDocumentedException, BGPParsingException {
        final Map<Integer, RawAttribute> attributes = new TreeMap<>();
        while (buffer.isReadable()) {
            addAttribute(buffer, attributes);
        }
        /*
         * TreeMap guarantees that we will be invoking the parser in the order
         * of increasing attribute type.
         */
        final AttributesBuilder builder = new AttributesBuilder();
        for (final Entry<Integer, RawAttribute> e : attributes.entrySet()) {
            LOG.debug("Parsing attribute type {}", e.getKey());

            final RawAttribute a = e.getValue();
            a.parser.parseAttribute(a.buffer, builder, constraint);
        }
        builder.setUnrecognizedAttributes(this.unrecognizedAttributes);
        return builder.build();
    }

    @Override
    public void serializeAttribute(final DataObject attribute,final ByteBuf byteAggregator) {
        for (final AttributeSerializer serializer : this.roSerializers.get()) {
            serializer.serializeAttribute(attribute, byteAggregator);
        }
    }
}
