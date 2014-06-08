/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import com.google.common.base.Preconditions;

import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class PCEPTunnelTopologyProvider implements AutoCloseable {
    private final ListenerRegistration<DataChangeListener> reg;
    private final TopologyReference ref;

    private PCEPTunnelTopologyProvider(final InstanceIdentifier<Topology> dst, final ListenerRegistration<DataChangeListener> reg) {
        this.ref = new DefaultTopologyReference(dst);
        this.reg = Preconditions.checkNotNull(reg);
    }

    public static PCEPTunnelTopologyProvider create(final DataProviderService dataProvider,
            final InstanceIdentifier<Topology> sourceTopology, final TopologyId targetTopology) {
        final InstanceIdentifier<Topology> dst = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(targetTopology)).toInstance();
        final NodeChangedListener ncl = new NodeChangedListener(dataProvider, dst);

        final InstanceIdentifier<Node> src = sourceTopology.child(Node.class);
        final ListenerRegistration<DataChangeListener> reg = dataProvider.registerDataChangeListener(src, ncl);

        return new PCEPTunnelTopologyProvider(dst, reg);
    }

    @Override
    public void close() {
        reg.close();
    }

    public TopologyReference getTopologyReference() {
        return ref;
    }
}
