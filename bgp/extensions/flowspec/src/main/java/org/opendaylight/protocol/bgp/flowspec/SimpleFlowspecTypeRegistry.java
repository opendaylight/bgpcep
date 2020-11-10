/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public class SimpleFlowspecTypeRegistry extends FlowspecTypeRegistry {
    private final HandlerRegistry<DataContainer, FlowspecTypeParser, FlowspecTypeSerializer> handlers =
            new HandlerRegistry<>();

    @Override
    public FlowspecTypeParser getFlowspecTypeParser(final short type) {
        return handlers.getParser(type);
    }

    @Override
    public FlowspecTypeSerializer getFlowspecTypeSerializer(final FlowspecType fsType) {
        return handlers.getSerializer(fsType.implementedInterface());
    }

    Registration registerFlowspecTypeParser(final int type, final FlowspecTypeParser parser) {
        return handlers.registerParser(type, parser);
    }

    Registration registerFlowspecTypeSerializer(final Class<? extends FlowspecType> typeClass,
            final FlowspecTypeSerializer serializer) {
        return handlers.registerSerializer(typeClass, serializer);
    }
}
