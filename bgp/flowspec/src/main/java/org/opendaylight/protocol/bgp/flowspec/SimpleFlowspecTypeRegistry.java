/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleFlowspecTypeRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleFlowspecTypeRegistry.class);
    private final HandlerRegistry<DataContainer, FlowspecTypeParser, FlowspecTypeSerializer> handlers = new HandlerRegistry<>();

    public FlowspecTypeParser getFlowspecTypeParser(final short type) {
        return this.handlers.getParser(type);
    }

    public FlowspecTypeSerializer getFlowspecTypeSerializer(final FlowspecType fsType) {
        return this.handlers.getSerializer(fsType.getImplementedInterface());
    }

    public void serializeFlowspecType(final FlowspecType fsType, final ByteBuf output) {
        final FlowspecTypeSerializer serializer = getFlowspecTypeSerializer(fsType);
        Preconditions.checkNotNull(serializer, "serializer for flowspec type %s is not registered.", fsType);
        serializer.serializeType(fsType, output);
    }

    public FlowspecType parseFlowspecType(final @Nonnull ByteBuf buffer) {
        final short type = buffer.readUnsignedByte();
        LOG.trace("Flowspec type is {}", type);
        final FlowspecTypeParser parser = getFlowspecTypeParser(type);
        Preconditions.checkNotNull(parser, "parser for flowspec type %s is not registered", type);
        return parser.parseType(buffer);
    }

    public AutoCloseable registerFlowspecTypeParser(final int type, final FlowspecTypeParser parser) {
        return this.handlers.registerParser(type, parser);
    }

    public AutoCloseable registerFlowspecTypeSerializer(final Class<? extends FlowspecType> typeClass, final FlowspecTypeSerializer serializer) {
        return this.handlers.registerSerializer(typeClass, serializer);
    }
}
