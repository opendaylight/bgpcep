/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.topology.tunnel.pcep.type.TopologyTunnelPcepBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PCEPTunnelTopologyProvider extends DefaultTopologyReference implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTunnelTopologyProvider.class);

    private final NodeChangedListener ncl;
    private final DataObjectReference<Node> src;
    private final DefaultTopologyReference ref;
    private final DataBroker dataBroker;
    private final TopologyId tunneltopologyId;
    @GuardedBy("this")
    private Registration reg;

    public PCEPTunnelTopologyProvider(
            final DataBroker dataBroker,
            final WithKey<Topology, TopologyKey> pcepTopology,
            final TopologyId pcepTopologyId,
            final WithKey<Topology, TopologyKey> tunnelTopology,
            final TopologyId tunneltopologyId) {
        super(tunnelTopology);
        this.dataBroker = dataBroker;
        this.tunneltopologyId = tunneltopologyId;
        ncl = new NodeChangedListener(dataBroker, pcepTopologyId, tunnelTopology);
        src = pcepTopology.toBuilder().toReferenceBuilder().child(Node.class).build();
        ref = new DefaultTopologyReference(tunnelTopology);
    }

    synchronized void init() {
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.mergeParentStructurePut(LogicalDatastoreType.OPERATIONAL, getTopologyReference().getInstanceIdentifier(),
                new TopologyBuilder().setTopologyId(tunneltopologyId)
                        .setTopologyTypes(new TopologyTypesBuilder()
                                .addAugmentation(new TopologyTypes1Builder()
                                        .setTopologyTunnelPcep(
                                                new TopologyTunnelPcepBuilder().build()).build()).build())
                        .build());
        try {
            tx.commit().get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Failed to create Tunnel Topology root", e);
        }
        reg = ncl.getDataProvider().registerTreeChangeListener(LogicalDatastoreType.OPERATIONAL, src, ncl);
    }

    public TopologyReference getTopologyReference() {
        return ref;
    }

    @Override
    public synchronized void close() {
        if (reg != null) {
            reg.close();
            reg = null;
        }
    }
}
