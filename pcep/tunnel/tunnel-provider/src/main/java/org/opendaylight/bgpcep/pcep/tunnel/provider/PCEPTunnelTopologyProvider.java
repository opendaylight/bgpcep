/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

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
    private final NodeChangedListener ncl;
    private final InstanceIdentifier<Node> src;
    private ListenerRegistration<NodeChangedListener> reg;

    public PCEPTunnelTopologyProvider(
            final DataBroker dataBroker,
            final InstanceIdentifier<Topology> pcepTopology,
            final TopologyId pcepTopologyId, final InstanceIdentifier<Topology> tunneltopology
    ) {

        this.ncl = new NodeChangedListener(dataBroker, pcepTopologyId, tunneltopology);
        this.src = pcepTopology.child(Node.class);
    }

    void init() {
        this.reg = this.ncl.getDataProvider()
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, src), ncl);
    }

    @Override
    public void close() {
        if (this.reg != null) {
            this.reg.close();
            this.reg = null;
        }
    }
}
