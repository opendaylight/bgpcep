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
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.topology.TopologyProgrammingUtil;
import org.opendaylight.bgpcep.programming.tunnel.TunnelProgrammingUtil;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Arguments3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Arguments3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PcepUpdateTunnelInput1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepUpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.Link1;
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
    private final NetworkTopologyPcepService topologyService;

    UpdateTunnelInstructionExecutor(final PcepUpdateTunnelInput updateTunnelInput, final DataBroker dataProvider,
            final NetworkTopologyPcepService topologyService) {
        super(updateTunnelInput);
        this.updateTunnelInput = updateTunnelInput;
        this.dataProvider = dataProvider;
        this.topologyService = topologyService;
    }

    @Override
    protected ListenableFuture<OperationResult> invokeOperation() {
        final InstanceIdentifier<Topology> tii = TopologyProgrammingUtil.topologyForInput(this.updateTunnelInput);
        final InstanceIdentifier<Link> lii = TunnelProgrammingUtil.linkIdentifier(tii, this.updateTunnelInput);
        try (ReadOnlyTransaction t = this.dataProvider.newReadOnlyTransaction()) {
            final Link link;
            final Node node;
            try {
                // The link has to exist
                link = t.read(LogicalDatastoreType.OPERATIONAL, lii).checkedGet().get();
                // The source node has to exist
                node = TunelProgrammingUtil.sourceNode(t, tii, link).get();
            } catch (IllegalStateException | ReadFailedException e) {
                LOG.debug("Link or node does not exist.", e);
                return TunelProgrammingUtil.RESULT;
            }
            return Futures.transform(
                    (ListenableFuture<RpcResult<UpdateLspOutput>>) this.topologyService
                            .updateLsp(buildUpdateInput(link, node)),
                    RpcResult::getResult, MoreExecutors.directExecutor());
        }
    }

    private UpdateLspInput buildUpdateInput(final Link link, final Node node) {
        final UpdateLspInputBuilder ab = new UpdateLspInputBuilder();
        ab.setName(link.getAugmentation(Link1.class).getSymbolicPathName());
        ab.setNode(requireNonNull(TunelProgrammingUtil.supportingNode(node)));

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.update.lsp
                .args.ArgumentsBuilder args = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.topology.pcep.rev171025.update.lsp.args.ArgumentsBuilder();
        args.setBandwidth(new BandwidthBuilder().setBandwidth(this.updateTunnelInput.getBandwidth()).build());
        args.setClassType(new ClassTypeBuilder().setClassType(this.updateTunnelInput.getClassType()).build());
        args.setEro(TunelProgrammingUtil.buildEro(this.updateTunnelInput.getExplicitHops()));
        args.setLspa(new LspaBuilder(this.updateTunnelInput).build());

        final AdministrativeStatus adminStatus = this.updateTunnelInput.getAugmentation(PcepUpdateTunnelInput1.class)
                .getAdministrativeStatus();
        if (adminStatus != null) {
            args.addAugmentation(Arguments3.class, new Arguments3Builder().setLsp(new LspBuilder()
                    .setAdministrative(adminStatus == AdministrativeStatus.Active).build()).build());
        }
        ab.setArguments(args.build());
        return ab.build();
    }
}
