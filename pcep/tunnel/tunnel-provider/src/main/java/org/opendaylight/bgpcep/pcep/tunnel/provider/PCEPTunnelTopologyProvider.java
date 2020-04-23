/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.topology.tunnel.pcep.type.TopologyTunnelPcepBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCEPTunnelTopologyProvider extends DefaultTopologyReference implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTunnelTopologyProvider.class);

    private final NodeChangedListener ncl;
    private final InstanceIdentifier<Node> src;
    private final DefaultTopologyReference ref;
    private final DataBroker dataBroker;
    private final TopologyId tunneltopologyId;
    @GuardedBy("this")
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

    synchronized void init() {
        final WriteTransaction tx = this.dataBroker.newWriteOnlyTransaction();
        tx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, getTopologyReference().getInstanceIdentifier(),
                new TopologyBuilder().setTopologyId(this.tunneltopologyId)
                        .setTopologyTypes(new TopologyTypesBuilder()
                                .addAugmentation(new TopologyTypes1Builder()
                                        .setTopologyTunnelPcep(
                                                new TopologyTunnelPcepBuilder().build()).build()).build())
                        .setNode(new ArrayList<>()).build());
        try {
            tx.commit().get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Failed to create Tunnel Topology root", e);
        }
        this.reg = this.ncl.getDataProvider()
                .registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, this.src),
                        this.ncl);
    }

    public TopologyReference getTopologyReference() {
        return this.ref;
    }

    @Override
    public synchronized void close() {
        if (this.reg != null) {
            this.reg.close();
            this.reg = null;
        }
    }
}
