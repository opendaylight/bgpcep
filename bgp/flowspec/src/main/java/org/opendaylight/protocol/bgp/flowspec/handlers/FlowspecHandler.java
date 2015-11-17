/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.spi.handlers;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.flowspec.impl.registry.FlowspecTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.FlowspecBuilder;


public abstract class FlowspecHandler implements FlowspecParser, FlowspecSerializer {

    private final FlowspecTypeRegistry fsTypeRegistry;

    public FlowspecHandler(final FlowspecTypeRegistry registry) {
        this.fsTypeRegistry = registry;
    }

    @Override
    public final void serializeFlowspec(Flowspec spec, ByteBuf output) {
        this.fsTypeRegistry.serializeFlowspecType(spec.getFlowspecType(), output);
    }

    @Override
    public final Flowspec parseFlowspec(ByteBuf buffer) {
        return new FlowspecBuilder().setFlowspecType(this.fsTypeRegistry.parseFlowspecType(buffer)).build();
    }
}
