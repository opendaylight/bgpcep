/*
 * Copyright (c) 2016 AT&T Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.destination.CLinkstateDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleNlriTypeRegistry {


    private static final Logger LOG = LoggerFactory.getLogger(SimpleNlriTypeRegistry.class);

    private final Map<NlriType, NlriTypeCaseParser> parsers = new HashMap<>();
    private final Map<Class<? extends DataObject>, NlriTypeCaseSerializer> serializers = new HashMap<>();
    private static final SimpleNlriTypeRegistry SINGLETON = new SimpleNlriTypeRegistry();

    private SimpleNlriTypeRegistry () {

    }

    static SimpleNlriTypeRegistry getInstance() {
        return SINGLETON;
    }

    public void registerNlriTypeSerializer(final Class<? extends DataObject> classKey, final NlriTypeCaseSerializer serializer) {
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

    public NodeCase parseNlriNode(final ByteBuf buffer, final NlriType type, final NodeIdentifier localdescriptor) throws BGPParsingException {
        final NlriTypeCaseParser parser = this.parsers.get(type);
        ObjectType nodecase = parser.parseTypeNlri(buffer, type, localdescriptor);
        return (NodeCase)nodecase;
    }

    public LinkCase parseNlriLink(final ByteBuf buffer, final NlriType type, final NodeIdentifier localdescriptor) throws BGPParsingException {
        final NlriTypeCaseParser parser = this.parsers.get(type);
        ObjectType linkcase = parser.parseTypeNlri(buffer, type, localdescriptor);
        return (LinkCase)linkcase;
    }

    public PrefixCase parseNlriPrefix(final ByteBuf buffer, final NlriType type, final NodeIdentifier localdescriptor) throws BGPParsingException {
        final NlriTypeCaseParser parser = this.parsers.get(type);
        ObjectType prefixcase = parser.parseTypeNlri(buffer, type, localdescriptor);
        return (PrefixCase)prefixcase;
    }

    public NlriType serializeNlriPrefix(CLinkstateDestination destination, ByteBuf localdesc, ByteBuf byteAggregator) {
        final NlriTypeCaseSerializer serializer = this.serializers.get(PrefixCase.class);
        NlriType nlriType = serializer.serializeTypeNlri(destination, localdesc, byteAggregator);
        return nlriType;
    }

    public NlriType serializeNlriLink(CLinkstateDestination destination, ByteBuf localdesc, ByteBuf byteAggregator) {
        final NlriTypeCaseSerializer serializer = this.serializers.get(LinkCase.class);
        NlriType nlriType = serializer.serializeTypeNlri(destination, localdesc, byteAggregator);
        return nlriType;
    }

    public NlriType serializeNlriNode(CLinkstateDestination destination, ByteBuf localdesc, ByteBuf byteAggregator) {
        final NlriTypeCaseSerializer serializer = this.serializers.get(NodeCase.class);
        NlriType nlriType = serializer.serializeTypeNlri(destination, localdesc, byteAggregator);
        return nlriType;
    }
}
