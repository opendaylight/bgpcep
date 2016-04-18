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
import org.opendaylight.protocol.bgp.linkstate.tlvs.NlriSubTlvObjectParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.NlriSubTlvObjectSerializer;
import org.opendaylight.protocol.bgp.linkstate.tlvs.NlriTlvObjectParser;
import org.opendaylight.protocol.bgp.linkstate.tlvs.NlriTlvObjectSerializer;
import org.opendaylight.protocol.bgp.linkstate.tlvs.RouterIdTlvSerializer;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleNlriTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriTypeRegistry.class);

    private final ConcurrentMap<NlriType, NlriTypeCaseParser> parsers = new ConcurrentHashMap<>();
    private final ConcurrentMap<NodeIdentifier, NlriTlvObjectParser> tlvParser = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, NlriSubTlvObjectParser> subTlvParser = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends ObjectType>, NlriTypeCaseSerializer> serializers = new ConcurrentHashMap<>();
    private final ConcurrentMap<NodeIdentifier, NlriTlvObjectSerializer> tlvSerializer = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, NlriSubTlvObjectSerializer> subTlvSerializer = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends CRouterIdentifier>, RouterIdTlvSerializer> ridserializers = new ConcurrentHashMap<>();

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

    public synchronized AutoCloseable registerNlriTlvSerializer(final NodeIdentifier qNameId, final NlriTlvObjectSerializer nlriTlvSerializer) {
        this.tlvSerializer.put(qNameId, nlriTlvSerializer);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.tlvSerializer.remove(qNameId);
                }
            }
        };
    }

    public synchronized AutoCloseable registerNlriSubTlvSerializer(final Integer tlvType, final NlriSubTlvObjectSerializer nlriSubTlvSerializer) {
        this.subTlvSerializer.put(tlvType, nlriSubTlvSerializer);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.subTlvSerializer.remove(tlvType);
                }
            }
        };
    }

    public synchronized AutoCloseable registerRouterIdSerializer(final Class<? extends CRouterIdentifier> classKey, final RouterIdTlvSerializer ridSerializer) {
        this.ridserializers.put(classKey, ridSerializer);

        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.ridserializers.remove(classKey);
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

    public synchronized AutoCloseable registerNlriTlvParser(final NodeIdentifier qNameId, final NlriTlvObjectParser tlvParser) {
        this.tlvParser.put(qNameId, tlvParser);
        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.tlvParser.remove(qNameId);
                }
            }
        };

    }

    public synchronized AutoCloseable registerNlriSubTlvParser(final Integer tlvType, final NlriSubTlvObjectParser subTlvParser) {
        this.subTlvParser.put(tlvType, subTlvParser);
        final Object lock = this;
        return new AbstractRegistration() {
            @Override
            protected void removeRegistration() {
                synchronized (lock) {
                    SimpleNlriTypeRegistry.this.subTlvParser.remove(tlvType);
                }
            }
        };

    }

    @VisibleForTesting
    public NlriTypeCaseSerializer getSerializer(Class<? extends ObjectType> objType) {
        return this.serializers.get(objType);
    }

    @VisibleForTesting
    public NlriTypeCaseParser getParser(NlriType nlriType) {
        return this.parsers.get(nlriType);
    }

    @VisibleForTesting
    public NlriTlvObjectParser getTlvParser(NodeIdentifier qNameId) {
        return this.tlvParser.get(qNameId);
    }

    public NlriSubTlvObjectParser getSubTlvParser(Integer subTlvType) {
        return this.subTlvParser.get(subTlvType);
    }

    public ObjectType parseNlriType (final ByteBuf buffer, final NlriType type, final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier localDescriptor, final ByteBuf restBuffer) throws BGPParsingException {
        final NlriTypeCaseParser parser = this.parsers.get(type);
        Preconditions.checkNotNull(parser, "Parser for Nlri type %s not found.", type);
        ObjectType nlriObjectType = parser.parseTypeNlri(buffer, type, localDescriptor, restBuffer);
        return nlriObjectType;
    }

    public Object parseTlvObject (final ByteBuf buffer, final NlriType nlriType, final NodeIdentifier qNameId) throws BGPParsingException {
        final NlriTlvObjectParser nlriTlvParser = this.tlvParser.get(qNameId);
        Preconditions.checkNotNull(nlriTlvParser, "%s TLV Parser for Nlri type: %s not found.", qNameId.getNodeType().getLocalName(), nlriType);
        final Object tlvObject = nlriTlvParser.parseNlriTlvObject(buffer, nlriType);
        LOG.trace("Finished parsing {} descriptors.", nlriType);
        return tlvObject;
    }

    public Object parseSubTlvObject (final ByteBuf buffer, final Integer tlvType, final NlriType nlriType) throws BGPParsingException {
        final NlriSubTlvObjectParser subTlvTypeParser = this.subTlvParser.get(tlvType);
        Preconditions.checkNotNull(subTlvTypeParser, "Sub TLV Parser for type %s TLV not found.", tlvType);
        final Object subTlvObject = subTlvTypeParser.parseNlriSubTlvObject(buffer, nlriType);
        return subTlvObject;

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

    public void serializeTlvObject (final ObjectType nlriTlvObject, final NodeIdentifier qNameId, final NlriType nlriType, final ByteBuf localdescs) {
        final NlriTlvObjectSerializer nlriTlvSerializer = this.tlvSerializer.get(qNameId);
        Preconditions.checkNotNull(nlriTlvSerializer, "TLV Serializer for type: %s not found.", qNameId.getNodeType().getLocalName());
        nlriTlvSerializer.serializeNlriTlvObject(nlriTlvObject, qNameId, nlriType, localdescs);
    }

    public void serializeSubTlvObject (final ObjectType nlriTlvObject, final Integer nlriSubTlvType, final NodeIdentifier qNameId, final ByteBuf localdescs) {
        final NlriSubTlvObjectSerializer nlriSubTlvSerializer = this.subTlvSerializer.get(nlriSubTlvType);
        Preconditions.checkNotNull(nlriSubTlvSerializer, "Sub TLV Serializer for type: %s not found.", nlriSubTlvType);
        nlriSubTlvSerializer.serializeNlriSubTlvObject(nlriTlvObject, qNameId, localdescs);
    }


    public void serializeRouterId (final CRouterIdentifier routerId, final ByteBuf buffer) {
        final RouterIdTlvSerializer preftlvserializer = this.ridserializers.get(routerId.getImplementedInterface());
        Preconditions.checkNotNull(preftlvserializer, "RouterId Serializer for %s not found.", routerId.getImplementedInterface().getSimpleName());
        preftlvserializer.serializeRouterId(routerId, buffer);
    }
}
