/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.spi.pojo;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnParser;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnRegistry;
import org.opendaylight.protocol.bgp.evpn.spi.EvpnSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.evpn.Evpn;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

public class SimpleEvpnNlriRegistry implements EvpnRegistry {
    private static final SimpleEvpnNlriRegistry SINGLETON = new SimpleEvpnNlriRegistry();
    private final HandlerRegistry<DataContainer, EvpnParser, EvpnSerializer> handlers = new HandlerRegistry<>();
    private final Map<Class<? extends Evpn>, EvpnSerializer> modelSerializer = new HashMap<>();

    private SimpleEvpnNlriRegistry() {
        throw new UnsupportedOperationException();
    }

    public static SimpleEvpnNlriRegistry getInstance() {
        return SINGLETON;
    }

    public AutoCloseable registerNlriParser(final NlriType esiType, final EvpnParser parser) {
        return this.handlers.registerParser(esiType.getIntValue(), parser);
    }

    public AutoCloseable registerNlriSerializer(final Class<? extends Evpn> evpnClass, final EvpnSerializer serializer) {
        registerNlriModelSerializer(evpnClass, serializer);
        return this.handlers.registerSerializer(evpnClass, serializer);
    }

    public void registerNlriModelSerializer(final Class<? extends Evpn> evpnClass, final EvpnSerializer serializer) {
        this.modelSerializer.put(evpnClass, serializer);
    }

    @Override
    public Evpn parseEvpn(final NlriType type, final ByteBuf buffer) {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
        final EvpnParser parser = this.handlers.getParser(type.getIntValue());
        if (parser == null) {
            return null;
        }
        return parser.parseEvpn(buffer);
    }

    @Override
    public void serializeEvpn(final Evpn evpn, final ByteBuf buffer) {
        final EvpnSerializer serializer = this.handlers.getSerializer(evpn.getImplementedInterface());
        if (serializer == null) {
            return;
        }
        serializer.serializeEvpn(evpn, buffer);
    }

    public Evpn serializeEvpnModel(final Class<? extends Evpn> esRouteClass, final ChoiceNode containerNode) {
        return this.modelSerializer.get(esRouteClass).serializeEvpnModel(containerNode);
    }
}
