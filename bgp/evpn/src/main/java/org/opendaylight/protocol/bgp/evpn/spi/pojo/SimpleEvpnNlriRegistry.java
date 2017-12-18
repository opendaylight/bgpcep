/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.spi.pojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnParser;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnRegistry;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.concepts.MultiRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.EvpnChoice;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class SimpleEvpnNlriRegistry implements EvpnRegistry {
    private static final SimpleEvpnNlriRegistry SINGLETON = new SimpleEvpnNlriRegistry();
    private final HandlerRegistry<DataContainer, EvpnParser, EvpnSerializer> handlers = new HandlerRegistry<>();
    private final MultiRegistry<NodeIdentifier, EvpnSerializer> modelHandlers = new MultiRegistry<>();

    private SimpleEvpnNlriRegistry() {
    }

    @FunctionalInterface
    private interface SerializerInterface {
        EvpnChoice check(EvpnSerializer serializer, ContainerNode cont);
    }

    public static SimpleEvpnNlriRegistry getInstance() {
        return SINGLETON;
    }

    public AutoCloseable registerNlriParser(final NlriType esiType, final EvpnParser parser) {
        return this.handlers.registerParser(esiType.getIntValue(), parser);
    }

    public AutoCloseable registerNlriSerializer(final Class<? extends EvpnChoice> evpnClass,
            final EvpnSerializer serializer) {
        return this.handlers.registerSerializer(evpnClass, serializer);
    }

    public AutoCloseable registerNlriModelSerializer(final QName qname, final EvpnSerializer serializer) {
        return this.modelHandlers.register(new NodeIdentifier(qname), serializer);
    }

    @Override
    public EvpnChoice parseEvpn(final NlriType type, final ByteBuf buffer) {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(),
                "Array of bytes is mandatory. Can't be null or empty.");
        final EvpnParser parser = this.handlers.getParser(type.getIntValue());
        if (parser == null) {
            return null;
        }
        return parser.parseEvpn(buffer);
    }

    @Override
    public ByteBuf serializeEvpn(final EvpnChoice evpn, final ByteBuf common) {
        final EvpnSerializer serializer = this.handlers.getSerializer(evpn.getImplementedInterface());
        if (serializer == null) {
            return common;
        }
        return serializer.serializeEvpn(evpn, common);
    }

    @Override
    public EvpnChoice serializeEvpnModel(final ChoiceNode evpnChoice) {
        return getEvpnCase(evpnChoice, EvpnSerializer::serializeEvpnModel);
    }

    @Override
    public EvpnChoice serializeEvpnRouteKey(final ChoiceNode evpnChoice) {
        return getEvpnCase(evpnChoice, EvpnSerializer::createRouteKey);
    }

    private EvpnChoice getEvpnCase(final ChoiceNode evpnChoice, final SerializerInterface serializerInterface) {
        Preconditions.checkArgument(evpnChoice != null && !evpnChoice.getValue().isEmpty(),
                "Evpn case is mandatory. Can't be null or empty.");
        final ContainerNode cont = (ContainerNode) Iterables.getOnlyElement(evpnChoice.getValue());
        final EvpnSerializer serializer = this.modelHandlers.get(cont.getIdentifier());
        if (serializer == null) {
            return null;
        }
        return serializerInterface.check(serializer, cont);
    }
}
