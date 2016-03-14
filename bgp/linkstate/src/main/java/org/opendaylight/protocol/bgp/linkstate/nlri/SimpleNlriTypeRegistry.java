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
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.concepts.AbstractRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleNlriTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriTypeRegistry.class);

    private final ConcurrentMap<NlriType, NlriTypeCaseParser> parsers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends ObjectType>, NlriTypeCaseSerializer> serializers = new ConcurrentHashMap<>();
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

    @VisibleForTesting
    public NlriTypeCaseSerializer getSerializer(Class<? extends ObjectType> objtyp) {
        return this.serializers.get(objtyp);
    }

    @VisibleForTesting
    public NlriTypeCaseParser getParser(NlriType nlrityp) {
        return this.parsers.get(nlrityp);
    }


    public ObjectType parseNlriType (final ByteBuf buffer, final NlriType type, final NodeIdentifier localdescriptor, final ByteBuf restBuffer) throws BGPParsingException {
        final NlriTypeCaseParser parser = this.parsers.get(type);
        Preconditions.checkNotNull(parser, "Parser for Nlri type %s not found.", type);
        ObjectType nlriObjectType = parser.parseTypeNlri(buffer, type, localdescriptor, restBuffer);
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
}
