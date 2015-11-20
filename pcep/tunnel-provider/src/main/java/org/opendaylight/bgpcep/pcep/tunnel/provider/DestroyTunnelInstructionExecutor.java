/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.tunnel.provider;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.topology.TopologyProgrammingUtil;
import org.opendaylight.bgpcep.programming.tunnel.TunnelProgrammingUtil;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepDestroyTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DestroyTunnelInstructionExecutor extends AbstractInstructionExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(DestroyTunnelInstructionExecutor.class);
    private final PcepDestroyTunnelInput input;
    private final DataBroker dataProvider;
    private final NetworkTopologyPcepService topologyService;

    protected DestroyTunnelInstructionExecutor(final PcepDestroyTunnelInput input, final DataBroker dataProvider, final NetworkTopologyPcepService topologyService) {
        super(input);
        this.input = input;
        this.dataProvider = dataProvider;
        this.topologyService = topologyService;
    }

    @Override
    protected ListenableFuture<OperationResult> invokeOperation() {
        final InstanceIdentifier<Topology> tii = TopologyProgrammingUtil.topologyForInput(input);
        final InstanceIdentifier<Link> lii = TunnelProgrammingUtil.linkIdentifier(tii, input);
        try (final ReadOnlyTransaction t = this.dataProvider.newReadOnlyTransaction()) {
            final Node node;
            final Link link;
            try {
                // The link has to exist
                link = t.read(LogicalDatastoreType.OPERATIONAL, lii).checkedGet().get();
                // The source node has to exist
                node = TunelProgrammingUtil.sourceNode(t, tii, link).get();
            } catch (IllegalStateException | ReadFailedException e) {
                LOG.debug("Link or node does not exist.", e);
                return TunelProgrammingUtil.RESULT;
            }
            final RemoveLspInputBuilder ab = new RemoveLspInputBuilder();
            ab.setName(link.getAugmentation(Link1.class).getSymbolicPathName());
            ab.setNode(node.getSupportingNode().get(0).getKey().getNodeRef());
            return Futures.transform(
                (ListenableFuture<RpcResult<RemoveLspOutput>>) this.topologyService.removeLsp(ab.build()),
                new Function<RpcResult<RemoveLspOutput>, OperationResult>() {
                    @Override
                    public OperationResult apply(final RpcResult<RemoveLspOutput> input) {
                        return input.getResult();
                    }
                });
        }
    }
}
