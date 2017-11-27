/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.topology.TopologyProgrammingUtil;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Arguments2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Arguments2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PcepCreateP2pTunnelInput1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.add.lsp.args.Arguments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev131030.PcepCreateP2pTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.programming.rev130930.TpReference;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.TerminationPointType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.Ip;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

final class CreateTunnelInstructionExecutor extends AbstractInstructionExecutor {
    private final DataBroker dataProvider;
    private final NetworkTopologyPcepService topologyService;
    private final PcepCreateP2pTunnelInput p2pTunnelInput;

    CreateTunnelInstructionExecutor(final PcepCreateP2pTunnelInput p2pTunnelInput, final DataBroker dataProvider,
            final NetworkTopologyPcepService topologyService) {
        super(p2pTunnelInput);
        this.p2pTunnelInput = p2pTunnelInput;
        this.dataProvider = dataProvider;
        this.topologyService = topologyService;
    }

    private static void checkLinkIsnotExistent(final InstanceIdentifier<Topology> tii,
            final AddLspInputBuilder addLspInput, final ReadOnlyTransaction rt) {
        final InstanceIdentifier<Link> lii = NodeChangedListener.linkIdentifier(tii, addLspInput.getNode(),
                addLspInput.getName());
        try {
            Preconditions.checkState(!rt.read(LogicalDatastoreType.OPERATIONAL, lii).checkedGet().isPresent());
        } catch (final ReadFailedException e) {
            throw new IllegalStateException("Failed to ensure link existence.", e);
        }
    }

    private static AddressFamily buildAddressFamily(final TerminationPoint sp, final TerminationPoint dp) {
        // We need the IGP augmentation -- it has IP addresses
        final TerminationPoint1 sp1 = requireNonNull(sp.getAugmentation(TerminationPoint1.class));
        final TerminationPoint1 dp1 = requireNonNull(dp.getAugmentation(TerminationPoint1.class));

        // Get the types
        final TerminationPointType spt = sp1.getIgpTerminationPointAttributes().getTerminationPointType();
        final TerminationPointType dpt = dp1.getIgpTerminationPointAttributes().getTerminationPointType();

        // The types have to match
        Preconditions.checkArgument(spt.getImplementedInterface().equals(dpt.getImplementedInterface()));

        // And they have to actually be Ip
        final Ip sips = (Ip) spt;
        final Ip dips = (Ip) dpt;

        /*
         * Now a bit of magic. We need to find 'like' addresses, e.g. both
         * IPv4 or both IPv6. We are in IPv6-enabled world now, so let's
         * prefer that.
         */
        Optional<AddressFamily> ret = findIpv6(sips.getIpAddress(), dips.getIpAddress());
        if (!ret.isPresent()) {
            ret = findIpv4(sips.getIpAddress(), dips.getIpAddress());
        }

        // We need to have a ret now
        Preconditions.checkArgument(ret != null, "Failed to find like Endpoint addresses");

        return ret.get();
    }

    private static Optional<AddressFamily> findIpv4(final List<IpAddress> srcs, final List<IpAddress> dsts) {
        for (final IpAddress sc : srcs) {
            if (sc.getIpv4Address() != null) {
                for (final IpAddress dc : dsts) {
                    if (dc.getIpv4Address() != null) {
                        return Optional.of(new Ipv4CaseBuilder().setIpv4(new Ipv4Builder()
                                .setSourceIpv4Address(sc.getIpv4Address())
                                .setDestinationIpv4Address(dc.getIpv4Address()).build()).build());
                    }
                }
            }
        }

        return Optional.absent();
    }

    private static Optional<AddressFamily> findIpv6(final List<IpAddress> srcs, final List<IpAddress> dsts) {
        for (final IpAddress sc : srcs) {
            if (sc.getIpv6Address() != null) {
                for (final IpAddress dc : dsts) {
                    if (dc.getIpv6Address() != null) {
                        return Optional.of(new Ipv6CaseBuilder().setIpv6(new Ipv6Builder()
                                .setSourceIpv6Address(sc.getIpv6Address())
                                .setDestinationIpv6Address(dc.getIpv6Address()).build()).build());
                    }
                }
            }
        }

        return Optional.absent();
    }

    @Override
    protected ListenableFuture<OperationResult> invokeOperation() {
        try (ReadOnlyTransaction transaction = this.dataProvider.newReadOnlyTransaction()) {
            AddLspInput addLspInput = createAddLspInput(transaction);

            return Futures.transform(
                    (ListenableFuture<RpcResult<AddLspOutput>>) this.topologyService.addLsp(addLspInput),
                    RpcResult::getResult, MoreExecutors.directExecutor());
        }
    }

    private AddLspInput createAddLspInput(final ReadOnlyTransaction transaction) {
        final InstanceIdentifier<Topology> tii = TopologyProgrammingUtil.topologyForInput(this.p2pTunnelInput);
        final TpReader dr = new TpReader(transaction, tii, this.p2pTunnelInput.getDestination());
        final TerminationPoint dp = requireNonNull(dr.getTp());

        final TpReader sr = new TpReader(transaction, tii, this.p2pTunnelInput.getSource());
        final TerminationPoint sp = requireNonNull(sr.getTp());

        final Node sn = requireNonNull(sr.getNode());
        final AddLspInputBuilder ab = new AddLspInputBuilder();
        ab.setNode(requireNonNull(TunelProgrammingUtil.supportingNode(sn)));
        ab.setName(this.p2pTunnelInput.getSymbolicPathName());

        checkLinkIsnotExistent(tii, ab, transaction);

        ab.setArguments(buildArguments(sp, dp));
        return ab.build();
    }

    private Arguments buildArguments(final TerminationPoint sp, final TerminationPoint dp) {
        final ArgumentsBuilder args = new ArgumentsBuilder();
        if (this.p2pTunnelInput.getBandwidth() != null) {
            args.setBandwidth(new BandwidthBuilder().setBandwidth(this.p2pTunnelInput.getBandwidth()).build());
        }
        if (this.p2pTunnelInput.getClassType() != null) {
            args.setClassType(new ClassTypeBuilder().setClassType(this.p2pTunnelInput.getClassType()).build());
        }
        args.setEndpointsObj(new EndpointsObjBuilder().setAddressFamily(buildAddressFamily(sp, dp)).build());
        args.setEro(TunelProgrammingUtil.buildEro(this.p2pTunnelInput.getExplicitHops()));
        args.setLspa(new LspaBuilder(this.p2pTunnelInput).build());

        final AdministrativeStatus adminStatus = this.p2pTunnelInput.getAugmentation(PcepCreateP2pTunnelInput1.class)
                .getAdministrativeStatus();
        if (adminStatus != null) {
            args.addAugmentation(Arguments2.class, new Arguments2Builder().setLsp(new LspBuilder()
                    .setAdministrative(adminStatus == AdministrativeStatus.Active).build()).build());
        }
        return args.build();
    }

    private static final class TpReader {
        private final ReadTransaction rt;
        private final InstanceIdentifier<Node> nii;
        private final InstanceIdentifier<TerminationPoint> tii;

        TpReader(final ReadTransaction rt, final InstanceIdentifier<Topology> topo, final TpReference ref) {
            this.rt = requireNonNull(rt);

            this.nii = topo.child(Node.class, new NodeKey(ref.getNode()));
            this.tii = this.nii.child(TerminationPoint.class, new TerminationPointKey(ref.getTp()));
        }

        private DataObject read(final InstanceIdentifier<?> id) {
            try {
                return this.rt.read(LogicalDatastoreType.OPERATIONAL, id).checkedGet().get();
            } catch (ReadFailedException | IllegalStateException e) {
                throw new IllegalStateException("Failed to read data.", e);
            }
        }

        private Node getNode() {
            return (Node) read(this.nii);
        }

        private TerminationPoint getTp() {
            return (TerminationPoint) read(this.tii);
        }
    }
}
