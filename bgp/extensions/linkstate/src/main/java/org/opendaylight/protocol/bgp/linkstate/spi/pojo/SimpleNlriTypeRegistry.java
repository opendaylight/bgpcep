/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.spi.pojo;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.bgp.linkstate.spi.NlriTypeCaseParser;
import org.opendaylight.protocol.bgp.linkstate.spi.NlriTypeCaseSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.concepts.MultiRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleNlriTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriTypeRegistry.class);
    private static final SimpleNlriTypeRegistry SINGLETON = new SimpleNlriTypeRegistry();
    private final HandlerRegistry<ObjectType, NlriTypeCaseParser, NlriTypeCaseSerializer> nlriRegistry = new HandlerRegistry<>();
    private final MultiRegistry<QName, LinkstateTlvParser.LinkstateTlvSerializer<?>> tlvSerializers = new MultiRegistry<>();
    private final MultiRegistry<Integer, LinkstateTlvParser<?>> tlvParsers = new MultiRegistry<>();

    private SimpleNlriTypeRegistry() {
    }

    public static SimpleNlriTypeRegistry getInstance() {
        return SINGLETON;
    }

    public AutoCloseable registerNlriParser(final int type, final NlriTypeCaseParser parser) {
        return this.nlriRegistry.registerParser(type, parser);
    }

    public AutoCloseable registerNlriSerializer(final Class<? extends ObjectType> clazzType, final NlriTypeCaseSerializer serializer) {
        return this.nlriRegistry.registerSerializer(clazzType, serializer);
    }

    public <T> AutoCloseable registerTlvParser(final int tlvType, final LinkstateTlvParser<T> parser) {
        return this.tlvParsers.register(tlvType, parser);
    }

    public <T> AutoCloseable registerTlvSerializer(final QName tlvQName, final LinkstateTlvParser.LinkstateTlvSerializer<T> serializer) {
        return this.tlvSerializers.register(tlvQName, serializer);
    }

    public CLinkstateDestination parseNlriType(final ByteBuf buffer) {
        final int type = buffer.readUnsignedShort();
        final int length = buffer.readUnsignedShort();
        final NlriTypeCaseParser parser = this.nlriRegistry.getParser(type);
        if (parser == null) {
            LOG.warn("Linkstate NLRI parser for Type: {} was not found.", type);
            return null;
        }
        return parser.parseTypeNlri(buffer.readSlice(length));
    }

    public void serializeNlriType(final CLinkstateDestination nlri, final ByteBuf byteAggregator) {
        if (nlri == null) {
            return;
        }
        requireNonNull(byteAggregator);
        final ObjectType objectType = nlri.getObjectType();
        final NlriTypeCaseSerializer serializer = this.nlriRegistry.getSerializer((Class<? extends ObjectType>) objectType.getImplementedInterface());
        if (serializer == null) {
            LOG.warn("Linkstate NLRI serializer for Type: {} was not found.", objectType.getImplementedInterface());
        }
        final ByteBuf nlriType = Unpooled.buffer();
        serializer.serializeTypeNlri(nlri, nlriType);
        TlvUtil.writeTLV(serializer.getNlriType(), nlriType, byteAggregator);
    }

    public <T> T parseTlv(final ByteBuf buffer) {
        return parseTlv(buffer, getParser(buffer));
    }

    public Map<QName, Object> parseSubTlvs(final ByteBuf buffer) {
        final Map<QName, Object> tlvs = new HashMap<>();
        while (buffer.isReadable()) {
            final LinkstateTlvParser<?> tlvParser = getParser(buffer);
            final Object tlvBody = parseTlv(buffer, tlvParser);
            if (tlvBody != null) {
                tlvs.put(tlvParser.getTlvQName(), tlvBody);
            }
        }
        return tlvs;
    }

    private <T> LinkstateTlvParser<T> getParser(final ByteBuf buffer) {
        Preconditions.checkArgument(buffer != null && buffer.isReadable());
        final int type = buffer.readUnsignedShort();
        final LinkstateTlvParser<T> parser = (LinkstateTlvParser<T>) this.tlvParsers.get(type);
        if (parser == null) {
            LOG.warn("Linkstate TLV parser for Type: {} was not found.", type);
        }
        return parser;
    }

    private static <T> T parseTlv(final ByteBuf buffer, final LinkstateTlvParser<T> parser) {
        if (parser == null) {
            return null;
        }
        Preconditions.checkArgument(buffer != null && buffer.isReadable());
        final int length = buffer.readUnsignedShort();
        return parser.parseTlvBody(buffer.readSlice(length));
    }


    public <T> void serializeTlv(final QName tlvQName, final T tlv, final ByteBuf buffer) {
        if (tlv == null) {
            return;
        }
        requireNonNull(tlvQName);
        requireNonNull(buffer);
        final LinkstateTlvParser.LinkstateTlvSerializer<T> tlvSerializer = (LinkstateTlvParser.LinkstateTlvSerializer<T>) this.tlvSerializers.get(tlvQName);
        if (tlvSerializer == null) {
            LOG.warn("Linkstate TLV serializer for QName: {} was not found.", tlvQName);
            return;
        }
        final ByteBuf body = Unpooled.buffer();
        tlvSerializer.serializeTlvBody(tlv, body);
        TlvUtil.writeTLV(tlvSerializer.getType(), body, buffer);
    }
}
