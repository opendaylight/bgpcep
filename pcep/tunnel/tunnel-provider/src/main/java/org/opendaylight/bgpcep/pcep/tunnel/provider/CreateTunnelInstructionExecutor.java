/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.topology.TopologyProgrammingUtil;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Arguments2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PcepCreateP2pTunnelInput1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.endpoints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev210825.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.add.lsp.args.Arguments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepCreateP2pTunnelInput;
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
            final AddLspInputBuilder addLspInput, final ReadTransaction rt) {
        final InstanceIdentifier<Link> lii = NodeChangedListener.linkIdentifier(tii, addLspInput.getNode(),
                addLspInput.getName());
        try {
            Preconditions.checkState(!rt.read(LogicalDatastoreType.OPERATIONAL, lii).get().isPresent());
        } catch (final InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Failed to ensure link existence.", e);
        }
    }

    private static AddressFamily buildAddressFamily(final TerminationPoint sp, final TerminationPoint dp) {
        // We need the IGP augmentation -- it has IP addresses
        final TerminationPoint1 sp1 = requireNonNull(sp.augmentation(TerminationPoint1.class));
        final TerminationPoint1 dp1 = requireNonNull(dp.augmentation(TerminationPoint1.class));

        // Get the types
        final TerminationPointType spt = sp1.getIgpTerminationPointAttributes().getTerminationPointType();
        final TerminationPointType dpt = dp1.getIgpTerminationPointAttributes().getTerminationPointType();

        // The types have to match
        Preconditions.checkArgument(spt.implementedInterface().equals(dpt.implementedInterface()));

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
                                .setSourceIpv4Address(new Ipv4AddressNoZone(sc.getIpv4Address()))
                                .setDestinationIpv4Address(new Ipv4AddressNoZone(dc.getIpv4Address()))
                                .build()).build());
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<AddressFamily> findIpv6(final List<IpAddress> srcs, final List<IpAddress> dsts) {
        for (final IpAddress sc : srcs) {
            if (sc.getIpv6Address() != null) {
                for (final IpAddress dc : dsts) {
                    if (dc.getIpv6Address() != null) {
                        return Optional.of(new Ipv6CaseBuilder().setIpv6(new Ipv6Builder()
                                .setSourceIpv6Address(new Ipv6AddressNoZone(sc.getIpv6Address()))
                                .setDestinationIpv6Address(new Ipv6AddressNoZone(dc.getIpv6Address()))
                                .build()).build());
                    }
                }
            }
        }

        return Optional.empty();
    }

    @Override
    protected ListenableFuture<OperationResult> invokeOperation() {
        try (ReadTransaction transaction = this.dataProvider.newReadOnlyTransaction()) {
            AddLspInput addLspInput = createAddLspInput(transaction);

            return Futures.transform(
                    this.topologyService.addLsp(addLspInput),
                    RpcResult::getResult, MoreExecutors.directExecutor());
        }
    }

    private AddLspInput createAddLspInput(final ReadTransaction transaction) {
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

        final AdministrativeStatus adminStatus = this.p2pTunnelInput.augmentation(PcepCreateP2pTunnelInput1.class)
                .getAdministrativeStatus();
        if (adminStatus != null) {
            args.addAugmentation(new Arguments2Builder().setLsp(new LspBuilder()
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
                return this.rt.read(LogicalDatastoreType.OPERATIONAL, id).get().get();
            } catch (final InterruptedException | ExecutionException e) {
                throw new IllegalStateException("Failed to read data.", e);
            }
        }

        Node getNode() {
            return (Node) read(this.nii);
        }

        TerminationPoint getTp() {
            return (TerminationPoint) read(this.tii);
        }
    }
}
