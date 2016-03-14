/*
 * Copyright (c) 2016 AT&T Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleNlriTypeRegistry {


    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriTypeRegistry.class);

    private final Map<NlriType, NlriTypeCaseParser> parsers = new HashMap<>();
    private final Map<Class<? extends ObjectType>, NlriTypeCaseSerializer> serializers = new HashMap<>();
    private static final SimpleNlriTypeRegistry SINGLETON = new SimpleNlriTypeRegistry();

    private SimpleNlriTypeRegistry () {

    }

    public static SimpleNlriTypeRegistry getInstance() {
        return SINGLETON;
    }

    public void registerNlriTypeSerializer(final Class<? extends ObjectType> classKey, final NlriTypeCaseSerializer serializer) {

        final NlriTypeCaseSerializer prevser = this.serializers.get(classKey);

        if (prevser != null) {
            return;
        }

        this.serializers.put(classKey, serializer);

    }

    public void registerNlriTypeParser(final NlriType key, final NlriTypeCaseParser parser) {
        final NlriTypeCaseParser prev = this.parsers.get(key);


        if (prev != null) {
            return;
        }

        this.parsers.put(key, parser);

    }

    public NlriTypeCaseSerializer getSerializer(Class<? extends ObjectType> objtyp) {
        return this.serializers.get(objtyp);
    }

    public NlriTypeCaseParser getParser(NlriType nlrityp) {
        return this.parsers.get(nlrityp);
    }


    public ObjectType parseNlriType (final ByteBuf buffer, final NlriType type, final NodeIdentifier localdescriptor, final ByteBuf restBuffer) throws BGPParsingException {
        final NlriTypeCaseParser parser = this.parsers.get(type);
        Preconditions.checkNotNull(parser, "Parser for Nlri Type "+type+" not found");

        ObjectType nlriObjectType = parser.parseTypeNlri(buffer, type, localdescriptor, restBuffer);

        return nlriObjectType;
    }

    public NlriType serializeNlriType (CLinkstateDestination destination, ByteBuf localdesc, ByteBuf byteAggregator) {
        final NlriTypeCaseSerializer serializer = this.serializers.get(destination.getObjectType().getImplementedInterface());
        Preconditions.checkNotNull(serializer, "Serializer for "+destination.getObjectType().getImplementedInterface().getSimpleName()+" not found");


        NlriType nlriTypeVal = serializer.serializeTypeNlri(destination, localdesc, byteAggregator);

        if ( nlriTypeVal == null ) {
            LOG.warn("Unknown NLRI Type.");
        }

        return nlriTypeVal;
    }
}
