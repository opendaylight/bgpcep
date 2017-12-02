/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import java.util.ArrayList;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.topology.tunnel.pcep.type.TopologyTunnelPcepBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class PCEPTunnelTopologyProvider extends DefaultTopologyReference implements AutoCloseable {

    private final NodeChangedListener ncl;
    private final InstanceIdentifier<Node> src;
    private final DefaultTopologyReference ref;
    private final DataBroker dataBroker;
    private final TopologyId tunneltopologyId;
    private ListenerRegistration<NodeChangedListener> reg;

    public PCEPTunnelTopologyProvider(
            final DataBroker dataBroker,
            final InstanceIdentifier<Topology> pcepTopology,
            final TopologyId pcepTopologyId,
            final InstanceIdentifier<Topology> tunnelTopology,
            final TopologyId tunneltopologyId) {
        super(tunnelTopology);
        this.dataBroker = dataBroker;
        this.tunneltopologyId = tunneltopologyId;
        this.ncl = new NodeChangedListener(dataBroker, pcepTopologyId, tunnelTopology);
        this.src = pcepTopology.child(Node.class);
        this.ref = new DefaultTopologyReference(tunnelTopology);
    }

    void init() {
        final WriteTransaction tx = this.dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, getTopologyReference().getInstanceIdentifier(),
                new TopologyBuilder().setTopologyId(this.tunneltopologyId)
                        .setTopologyTypes(new TopologyTypesBuilder()
                                .addAugmentation(TopologyTypes1.class, new TopologyTypes1Builder()
                                        .setTopologyTunnelPcep(
                                                new TopologyTunnelPcepBuilder().build()).build()).build())
                        .setNode(new ArrayList<>()).build(), true);
        tx.submit();
        this.reg = this.ncl.getDataProvider()
                .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, this.src),
                        this.ncl);
    }

    public TopologyReference getTopologyReference() {
        return this.ref;
    }

    @Override
    public void close() {
        if (this.reg != null) {
            this.reg.close();
            this.reg = null;
        }
    }
}
