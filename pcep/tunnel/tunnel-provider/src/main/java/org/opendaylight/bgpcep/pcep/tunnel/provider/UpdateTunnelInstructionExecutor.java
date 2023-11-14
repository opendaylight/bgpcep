/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.topology.TopologyProgrammingUtil;
import org.opendaylight.bgpcep.programming.tunnel.TunnelProgrammingUtil;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Arguments3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PcepUpdateTunnelInput1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepUpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UpdateTunnelInstructionExecutor extends AbstractInstructionExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateTunnelInstructionExecutor.class);
    private final PcepUpdateTunnelInput updateTunnelInput;
    private final DataBroker dataProvider;
    private final UpdateLsp updateLsp;

    UpdateTunnelInstructionExecutor(final PcepUpdateTunnelInput updateTunnelInput, final DataBroker dataProvider,
            final RpcConsumerRegistry rpcConsumerRegistry) {
        super(updateTunnelInput);
        this.updateTunnelInput = updateTunnelInput;
        this.dataProvider = dataProvider;
        updateLsp = rpcConsumerRegistry.getRpc(UpdateLsp.class);
    }

    @Override
    protected ListenableFuture<OperationResult> invokeOperation() {
        final InstanceIdentifier<Topology> tii = TopologyProgrammingUtil.topologyForInput(updateTunnelInput);
        final InstanceIdentifier<Link> lii = TunnelProgrammingUtil.linkIdentifier(tii, updateTunnelInput);
        try (ReadTransaction t = dataProvider.newReadOnlyTransaction()) {
            final Link link;
            final Node node;
            try {
                // The link has to exist
                link = t.read(LogicalDatastoreType.OPERATIONAL, lii).get().orElseThrow();
                // The source node has to exist
                node = TunelProgrammingUtil.sourceNode(t, tii, link).orElseThrow();
            } catch (final InterruptedException | ExecutionException e) {
                LOG.debug("Link or node does not exist.", e);
                return TunelProgrammingUtil.RESULT;
            }
            return Futures.transform(updateLsp.invoke(buildUpdateInput(link, node)), RpcResult::getResult,
                MoreExecutors.directExecutor());
        }
    }

    private UpdateLspInput buildUpdateInput(final Link link, final Node node) {
        final UpdateLspInputBuilder ab = new UpdateLspInputBuilder()
            .setName(link.augmentation(Link1.class).getSymbolicPathName())
            .setNode(requireNonNull(TunelProgrammingUtil.supportingNode(node)));

        final var args = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730
            .update.lsp.args.ArgumentsBuilder()
                .setBandwidth(new BandwidthBuilder().setBandwidth(updateTunnelInput.getBandwidth()).build())
                .setClassType(new ClassTypeBuilder().setClassType(updateTunnelInput.getClassType()).build())
                .setEro(TunelProgrammingUtil.buildEro(updateTunnelInput.getExplicitHops()))
                .setLspa(new LspaBuilder(updateTunnelInput).build());

        final AdministrativeStatus adminStatus = updateTunnelInput.augmentation(PcepUpdateTunnelInput1.class)
                .getAdministrativeStatus();
        if (adminStatus != null) {
            args.addAugmentation(new Arguments3Builder().setLsp(new LspBuilder()
                    .setAdministrative(adminStatus == AdministrativeStatus.Active).build()).build());
        }
        return ab.setArguments(args.build()).build();
    }
}
