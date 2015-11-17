/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.spi.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleFlowspecTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleFlowspecTypeRegistry.class);

    private final HandlerRegistry<DataContainer, FlowspecTypeParser, FlowspecTypeSerializer> handlers = new HandlerRegistry<>();

    public void serializeFlowspecType(final FlowspecType fsType, final ByteBuf output) {
        final FlowspecTypeSerializer serializer = this.handlers.getSerializer(fsType.getImplementedInterface());
        if (serializer == null) {
            LOG.warn("serializer for flowspec type {} is not registered.", fsType);
        }
        serializer.serializeType(fsType, output);
    }

    public FlowspecType parseFlowspecType(ByteBuf buffer) {
        final short type = buffer.readUnsignedByte();
        final FlowspecTypeParser parser = this.handlers.getParser(type);
        if (parser == null) {
            LOG.warn("parser for flowspec type {} is not registered.", type);
            return null;
        }
        return parser.parseType(buffer);
    }

    public AutoCloseable registerFlowspecTypeParser(final int type, final FlowspecTypeParser parser) {
        return this.handlers.registerParser(type, parser);
    }

    public AutoCloseable registerFlowspecTypeSerializer(final Class<? extends FlowspecType> typeClass, final FlowspecTypeSerializer serializer) {
        return this.handlers.registerSerializer(typeClass, serializer);
    }
}
