/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.tunnel.provider;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.topology.TopologyProgrammingUtil;
import org.opendaylight.bgpcep.programming.tunnel.TunnelProgrammingUtil;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepDestroyTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DestroyTunnelInstructionExecutor extends AbstractInstructionExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(DestroyTunnelInstructionExecutor.class);
    private final PcepDestroyTunnelInput pcepDestroyTunnelInput;
    private final DataBroker dataProvider;
    private final NetworkTopologyPcepService topologyService;

    DestroyTunnelInstructionExecutor(final PcepDestroyTunnelInput pcepDestroyTunnelInput, final DataBroker dataProvider,
            final NetworkTopologyPcepService topologyService) {
        super(pcepDestroyTunnelInput);
        this.pcepDestroyTunnelInput = pcepDestroyTunnelInput;
        this.dataProvider = dataProvider;
        this.topologyService = topologyService;
    }

    @Override
    protected ListenableFuture<OperationResult> invokeOperation() {
        final InstanceIdentifier<Topology> tii = TopologyProgrammingUtil.topologyForInput(this.pcepDestroyTunnelInput);
        final InstanceIdentifier<Link> lii = TunnelProgrammingUtil.linkIdentifier(tii, this.pcepDestroyTunnelInput);
        try (ReadTransaction t = this.dataProvider.newReadOnlyTransaction()) {
            final Node node;
            final Link link;
            try {
                // The link has to exist
                link = t.read(LogicalDatastoreType.OPERATIONAL, lii).get().get();
                // The source node has to exist
                node = TunelProgrammingUtil.sourceNode(t, tii, link).get();
            } catch (final InterruptedException | ExecutionException e) {
                LOG.debug("Link or node does not exist.", e);
                return TunelProgrammingUtil.RESULT;
            }
            final RemoveLspInputBuilder ab = new RemoveLspInputBuilder();
            ab.setName(link.augmentation(Link1.class).getSymbolicPathName());
            ab.setNode(node.nonnullSupportingNode().values().iterator().next().key().getNodeRef());
            return Futures.transform(
                    this.topologyService.removeLsp(ab.build()),
                    RpcResult::getResult, MoreExecutors.directExecutor());
        }
    }
}
