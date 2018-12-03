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
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Values;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.UnrecognizedAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.UnrecognizedAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.attributes.UnrecognizedAttributesKey;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleAttributeRegistry implements AttributeRegistry {

    private static final class RawAttribute {
        private final AttributeParser parser;
        private final ByteBuf buffer;

        RawAttribute(final AttributeParser parser, final ByteBuf buffer) {
            this.parser = requireNonNull(parser);
            this.buffer = requireNonNull(buffer);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SimpleAttributeRegistry.class);
    private static final int OPTIONAL_BIT = 0;
    private static final int TRANSITIVE_BIT = 1;
    private static final int PARTIAL_BIT = 2;
    private static final int EXTENDED_LENGTH_BIT = 3;

    private final HandlerRegistry<DataContainer, AttributeParser, AttributeSerializer> handlers =
            new HandlerRegistry<>();
    private final Map<AbstractRegistration, AttributeSerializer> serializers = new LinkedHashMap<>();
    private final AtomicReference<Iterable<AttributeSerializer>> roSerializers =
        new AtomicReference<>(this.serializers.values());
    private final List<UnrecognizedAttributes> unrecognizedAttributes = new ArrayList<>();


    AutoCloseable registerAttributeParser(final int attributeType, final AttributeParser parser) {
        Preconditions.checkArgument(attributeType >= 0 && attributeType <= Values.UNSIGNED_BYTE_MAX_VALUE);
        return this.handlers.registerParser(attributeType, parser);
    }

    synchronized AutoCloseable registerAttributeSerializer(final Class<? extends DataObject> paramClass,
            final AttributeSerializer serializer) {
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

    private void addAttribute(final ByteBuf buffer, final RevisedErrorHandling errorHandling,
            final Map<Integer, RawAttribute> attributes) throws BGPDocumentedException {
        final BitArray flags = BitArray.valueOf(buffer.readByte());
        final int type = buffer.readUnsignedByte();
        final int len = flags.get(EXTENDED_LENGTH_BIT) ? buffer.readUnsignedShort() : buffer.readUnsignedByte();
        final AttributeParser parser = this.handlers.getParser(type);
        if (attributes.containsKey(type)) {
            if (parser != null && !parser.ignoreDuplicates(errorHandling)) {
                throw new BGPDocumentedException("Duplicate attribute " + type, BGPError.MALFORMED_ATTR_LIST);
            }
            LOG.debug("Ignoring duplicate attribute type {}", type);
            return;
        }

        if (parser == null) {
            processUnrecognized(flags, type, buffer, len);
        } else {
            attributes.put(type, new RawAttribute(parser, buffer.readSlice(len)));
        }
    }

    private void processUnrecognized(final BitArray flags, final int type, final ByteBuf buffer, final int len)
            throws BGPDocumentedException {
        if (!flags.get(OPTIONAL_BIT)) {
            throw new BGPDocumentedException("Well known attribute not recognized.",
                BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED);
        }
        final UnrecognizedAttributes unrecognizedAttribute = new UnrecognizedAttributesBuilder()
            .withKey(new UnrecognizedAttributesKey((short) type))
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
        final RevisedErrorHandling errorHandling = RevisedErrorHandling.from(constraint);
        final Map<Integer, RawAttribute> attributes = new TreeMap<>();
        while (buffer.isReadable()) {
            addAttribute(buffer, errorHandling, attributes);
        }
        /*
         * TreeMap guarantees that we will be invoking the parser in the order
         * of increasing attribute type.
         */
        // We may have multiple attribute errors, each specifying a withdraw. We need to finish parsing the message
        // all attributes before we can decide whether we can discard attributes, or whether we need to terminate
        // the session.
        final AttributesBuilder builder = new AttributesBuilder();
        BGPTreatAsWithdrawException withdrawCause = null;
        for (final Entry<Integer, RawAttribute> entry : attributes.entrySet()) {
            LOG.debug("Parsing attribute type {}", entry.getKey());

            final RawAttribute a = entry.getValue();
            try {
                a.parser.parseAttribute(a.buffer, builder, errorHandling, constraint);
            } catch (BGPTreatAsWithdrawException e) {
                LOG.info("Attribute {} indicated treat-as-withdraw", entry.getKey(), e);
                if (withdrawCause == null) {
                    withdrawCause = e;
                } else {
                    withdrawCause.addSuppressed(e);
                }
            }
        }
        builder.setUnrecognizedAttributes(this.unrecognizedAttributes);

        // FIXME: BGPCEP-359 report withdrawCause upstream, so it can be handled properly
        if (withdrawCause != null) {
            throw withdrawCause.toDocumentedException();
        }

        return builder.build();
    }

    @Override
    public void serializeAttribute(final Attributes attribute,final ByteBuf byteAggregator) {
        for (final AttributeSerializer serializer : this.roSerializers.get()) {
            serializer.serializeAttribute(attribute, byteAggregator);
        }
    }
}
