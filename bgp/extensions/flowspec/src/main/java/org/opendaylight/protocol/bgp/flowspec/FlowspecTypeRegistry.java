/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;

public abstract class FlowspecTypeRegistry {
    FlowspecTypeRegistry() {
        // Hidden on purpose
    }

    abstract FlowspecTypeParser getFlowspecTypeParser(short type);

    abstract FlowspecTypeSerializer getFlowspecTypeSerializer(FlowspecType fsType);

    public final void serializeFlowspecType(final FlowspecType fsType, final ByteBuf output) {
        final FlowspecTypeSerializer serializer = getFlowspecTypeSerializer(fsType);
        requireNonNull(serializer, "serializer for flowspec type " + fsType + " is not registered.");
        serializer.serializeType(fsType, output);
    }

    public final FlowspecType parseFlowspecType(final ByteBuf buffer) {
        final short type = buffer.readUnsignedByte();
        final FlowspecTypeParser parser = getFlowspecTypeParser(type);
        requireNonNull(parser, "parser for flowspec type " + type + " is not registered");
        return parser.parseType(buffer);
    }
}
