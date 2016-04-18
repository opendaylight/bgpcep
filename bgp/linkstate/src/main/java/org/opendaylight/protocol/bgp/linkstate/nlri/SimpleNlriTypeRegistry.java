/*
 * Copyright (c) 2016 AT&T Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.protocol.bgp.linkstate.tlvs.LinkstateEpeNodeTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.LinkstateTlvParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.LinkstateTlvSerializer;
import org.opendaylight.protocol.bgp.linkstate.tlvs.RouterIdTlvSerializer;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.protocol.concepts.MultiRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleNlriTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriTypeRegistry.class);

    private final ConcurrentMap<NlriType, NlriTypeCaseParser> parsers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends ObjectType>, NlriTypeCaseSerializer> serializers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends CRouterIdentifier>, RouterIdTlvSerializer> ridSerializers = new ConcurrentHashMap<>();
    private final MultiRegistry<QName, LinkstateTlvSerializer<?>> tlvSerializers = new MultiRegistry<>();
    private final MultiRegistry<Integer, LinkstateTlvParser<?>> tlvParsers = new MultiRegistry<>();

    private static final SimpleNlriTypeRegistry SINGLETON = new SimpleNlriTypeRegistry();

    private SimpleNlriTypeRegistry () {

    }

    public static SimpleNlriTypeRegistry getInstance() {
        return SINGLETON;
    }

    public synchronized AutoCloseable registerNlriTypeSerializer(final Class<? extends ObjectType> classKey, final NlriTypeCaseSerializer serializer) {
        this.serializers.put(classKey, serializer);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.serializers.remove(classKey);
                }
            }
        };
    }

    public synchronized AutoCloseable registerRouterIdSerializer(final Class<? extends CRouterIdentifier> classKey, final RouterIdTlvSerializer ridSerializer) {
        this.ridSerializers.put(classKey, ridSerializer);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.ridSerializers.remove(classKey);
                }
            }
        };
    }

    public synchronized AutoCloseable registerNlriTypeParser(final NlriType key, final NlriTypeCaseParser parser) {
        this.parsers.put(key, parser);
        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.parsers.remove(key);
                }
            }
        };
    }

    public <T> AutoCloseable registerMessageParser(final int tlvType, final LinkstateTlvParser<T> parser) {
        return this.tlvParsers.register(tlvType, parser);
    }

    public <T> AutoCloseable registerMessageSerializer(final QName tlvQName, final LinkstateTlvSerializer<T> serializer) {
        return this.tlvSerializers.register(tlvQName, serializer);
    }


    @VisibleForTesting
    public NlriTypeCaseSerializer getSerializer(Class<? extends ObjectType> objType) {
        return this.serializers.get(objType);
    }

    @VisibleForTesting
    public NlriTypeCaseParser getParser(NlriType nlriType) {
        return this.parsers.get(nlriType);
    }

    public ObjectType parseNlriType (final ByteBuf buffer, final NlriType type, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier localDescriptor, final ByteBuf restBuffer) throws BGPParsingException {
        final NlriTypeCaseParser parser = this.parsers.get(type);
        Preconditions.checkNotNull(parser, "Parser for Nlri type %s not found.", type);
        ObjectType nlriObjectType = parser.parseTypeNlri(buffer, type, localDescriptor, restBuffer);
        return nlriObjectType;
    }

    public NlriType serializeNlriType (final CLinkstateDestination destination, final ByteBuf localdesc, final ByteBuf byteAggregator) {
        final NlriTypeCaseSerializer serializer = this.serializers.get(destination.getObjectType().getImplementedInterface());
        Preconditions.checkNotNull(serializer, "Serializer for %s not found.", destination.getObjectType().getImplementedInterface().getSimpleName());
        NlriType nlriTypeVal = serializer.serializeTypeNlri(destination, localdesc, byteAggregator);
        if (nlriTypeVal == null) {
            LOG.warn("NLRI Type value is null.");
        }
        return nlriTypeVal;
    }

    public void serializeRouterId (final CRouterIdentifier routerId, final ByteBuf buffer) {
        final RouterIdTlvSerializer preftlvserializer = this.ridSerializers.get(routerId.getImplementedInterface());
        Preconditions.checkNotNull(preftlvserializer, "RouterId Serializer for %s not found.", routerId.getImplementedInterface().getSimpleName());
        preftlvserializer.serializeRouterId(routerId, buffer);
    }

    public <T> T parseTlv(final ByteBuf buffer, final int tlvType, final NlriType nlriType) throws BGPParsingException {
        final LinkstateTlvParser<T> tlvParser = (LinkstateTlvParser<T>) this.tlvParsers.get(tlvType);
        Preconditions.checkNotNull(tlvParser, "TLV Parser for type: %s not found.", tlvType);
        final T descValue = tlvParser.parseTlvBody(buffer);
        if (nlriType.equals(NlriType.Link)) {
            return ((LinkstateEpeNodeTlvParser<T>) tlvParser).parseEpeTlvBody(tlvType, buffer, (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier)descValue);
        }
        return descValue;
    }

    public <T> T parseSubTlv(final ByteBuf tlv) throws BGPParsingException {
        final int type = tlv.readUnsignedShort();
        final int length = tlv.readUnsignedShort();
        final LinkstateTlvParser<T> tlvParser = (LinkstateTlvParser<T>) this.tlvParsers.get(type);
        Preconditions.checkNotNull(tlvParser, "sub-TLV Parser for type: %s not found.", type);
        return tlvParser.parseTlvBody(tlv.readSlice(length));
    }


    public <T> void serializeTlv(final QName tlvQName, final T tlv, final ByteBuf buffer) {
        final LinkstateTlvSerializer<T> tlvSerializer = (LinkstateTlvSerializer<T>) this.tlvSerializers.get(tlvQName);
        Preconditions.checkNotNull(tlvSerializer, "TLV/sub-TLV Serializer for type: %s not found.", tlvQName.getLocalName());
        tlvSerializer.serializeTlvBody(tlv, buffer, tlvQName);
    }
}
