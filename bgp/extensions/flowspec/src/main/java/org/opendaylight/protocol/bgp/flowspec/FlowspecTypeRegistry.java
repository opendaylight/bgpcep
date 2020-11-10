/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2020 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class FlowspecTypeRegistry {
    private final HandlerRegistry<DataContainer, FlowspecTypeParser, FlowspecTypeSerializer> handlers;

    FlowspecTypeRegistry(final HandlerRegistry<DataContainer, FlowspecTypeParser, FlowspecTypeSerializer> handlers) {
        this.handlers = requireNonNull(handlers);
    }

    public FlowspecTypeParser getFlowspecTypeParser(final short type) {
        return handlers.getParser(type);
    }

    public FlowspecTypeSerializer getFlowspecTypeSerializer(final FlowspecType fsType) {
        return handlers.getSerializer(fsType.implementedInterface());
    }

    public void serializeFlowspecType(final FlowspecType fsType, final ByteBuf output) {
        final FlowspecTypeSerializer serializer = getFlowspecTypeSerializer(fsType);
        requireNonNull(serializer, "serializer for flowspec type " + fsType + " is not registered.");
        serializer.serializeType(fsType, output);
    }

    public FlowspecType parseFlowspecType(final ByteBuf buffer) {
        final short type = buffer.readUnsignedByte();
        final FlowspecTypeParser parser = getFlowspecTypeParser(type);
        requireNonNull(parser, "parser for flowspec type " + type + " is not registered");
        return parser.parseType(buffer);
    }
}
