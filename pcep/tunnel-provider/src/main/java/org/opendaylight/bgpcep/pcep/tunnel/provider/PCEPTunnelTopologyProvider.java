/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class PCEPTunnelTopologyProvider implements AutoCloseable {
    private final ListenerRegistration<NodeChangedListener> reg;
    private final TopologyReference ref;

    private PCEPTunnelTopologyProvider(final InstanceIdentifier<Topology> dst, final ListenerRegistration<NodeChangedListener> reg) {
        this.ref = new DefaultTopologyReference(dst);
        this.reg = requireNonNull(reg);
    }

    public static PCEPTunnelTopologyProvider create(final DataBroker dataProvider,
            final InstanceIdentifier<Topology> sourceTopology, final TopologyId targetTopology) {
        final InstanceIdentifier<Topology> dst = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(targetTopology)).build();
        final NodeChangedListener ncl = new NodeChangedListener(dataProvider, sourceTopology.firstKeyOf(Topology.class).getTopologyId(), dst);

        final InstanceIdentifier<Node> src = sourceTopology.child(Node.class);
        final ListenerRegistration<NodeChangedListener> reg = dataProvider.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, src), ncl);

        return new PCEPTunnelTopologyProvider(dst, reg);
    }

    @Override
    public void close() {
        this.reg.close();
    }

    public TopologyReference getTopologyReference() {
        return this.ref;
    }
}
