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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.bgpcep.pcep.topology.spi.AbstractInstructionExecutor;
import org.opendaylight.bgpcep.programming.topology.TopologyProgrammingUtil;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadOperations;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.LspFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.AddLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.add.lsp.args.Arguments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.programming.rev181109.PcepCreateP2pTunnelInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.programming.rev130930.TpReference;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.TerminationPointType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.Ip;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

final class CreateTunnelInstructionExecutor extends AbstractInstructionExecutor {
    private final DataBroker dataProvider;
    private final AddLsp addLsp;
    private final PcepCreateP2pTunnelInput p2pTunnelInput;

    CreateTunnelInstructionExecutor(final PcepCreateP2pTunnelInput p2pTunnelInput, final DataBroker dataProvider,
            final RpcService rpcService) {
        super(p2pTunnelInput);
        this.p2pTunnelInput = p2pTunnelInput;
        this.dataProvider = dataProvider;
        addLsp = rpcService.getRpc(AddLsp.class);
    }

    private static void checkLinkIsnotExistent(final DataObjectIdentifier<Topology> tii,
            final AddLspInputBuilder addLspInput, final ReadOperations rt) {
        final var lii = NodeChangedListener.linkIdentifier(tii, addLspInput.getNode(), addLspInput.getName());
        try {
            Preconditions.checkState(!rt.exists(LogicalDatastoreType.OPERATIONAL, lii).get());
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
        return ret.orElseThrow(() -> new IllegalArgumentException("Failed to find like Endpoint addresses"));
    }

    private static Optional<AddressFamily> findIpv4(final Set<IpAddress> srcs, final Set<IpAddress> dsts) {
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

    private static Optional<AddressFamily> findIpv6(final Set<IpAddress> srcs, final Set<IpAddress> dsts) {
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
        try (var transaction = dataProvider.newReadOnlyTransaction()) {
            return Futures.transform(addLsp.invoke(createAddLspInput(transaction)), RpcResult::getResult,
                MoreExecutors.directExecutor());
        }
    }

    private AddLspInput createAddLspInput(final ReadOperations transaction) {
        final DataObjectIdentifier<Topology> tii = TopologyProgrammingUtil.topologyForInput(p2pTunnelInput);
        final TpReader dr = new TpReader(transaction, tii, p2pTunnelInput.getDestination());
        final TerminationPoint dp = requireNonNull(dr.getTp());

        final TpReader sr = new TpReader(transaction, tii, p2pTunnelInput.getSource());
        final TerminationPoint sp = requireNonNull(sr.getTp());

        final Node sn = requireNonNull(sr.getNode());
        final AddLspInputBuilder ab = new AddLspInputBuilder();
        ab.setNode(requireNonNull(TunelProgrammingUtil.supportingNode(sn)));
        ab.setName(p2pTunnelInput.getSymbolicPathName());

        checkLinkIsnotExistent(tii, ab, transaction);

        ab.setArguments(buildArguments(sp, dp));
        return ab.build();
    }

    private Arguments buildArguments(final TerminationPoint sp, final TerminationPoint dp) {
        final ArgumentsBuilder args = new ArgumentsBuilder();
        if (p2pTunnelInput.getBandwidth() != null) {
            args.setBandwidth(new BandwidthBuilder().setBandwidth(p2pTunnelInput.getBandwidth()).build());
        }
        if (p2pTunnelInput.getClassType() != null) {
            args.setClassType(new ClassTypeBuilder().setClassType(p2pTunnelInput.getClassType()).build());
        }
        args.setEndpointsObj(new EndpointsObjBuilder().setAddressFamily(buildAddressFamily(sp, dp)).build());
        args.setEro(TunelProgrammingUtil.buildEro(p2pTunnelInput.getExplicitHops()));
        args.setLspa(new LspaBuilder(p2pTunnelInput).build());

        if (p2pTunnelInput.getAdministrativeStatus() != null) {
            args.setLsp(new LspBuilder()
                .setLspFlags(new LspFlagsBuilder()
                    .setAdministrative(p2pTunnelInput.getAdministrativeStatus() == AdministrativeStatus.Active).build())
                .build());
        }
        return args.build();
    }

    private static final class TpReader {
        private final ReadOperations rt;
        private final DataObjectIdentifier<Node> nii;
        private final DataObjectIdentifier<TerminationPoint> tii;

        TpReader(final ReadOperations rt, final DataObjectIdentifier<Topology> topo, final TpReference ref) {
            this.rt = requireNonNull(rt);

            nii = topo.toBuilder().child(Node.class, new NodeKey(ref.getNode())).build();
            tii = nii.toBuilder().child(TerminationPoint.class, new TerminationPointKey(ref.getTp())).build();
        }

        private <T extends DataObject> T read(final DataObjectIdentifier<T> id) {
            try {
                return rt.read(LogicalDatastoreType.OPERATIONAL, id).get().orElseThrow();
            } catch (final InterruptedException | ExecutionException e) {
                throw new IllegalStateException("Failed to read data.", e);
            }
        }

        Node getNode() {
            return read(nii);
        }

        TerminationPoint getTp() {
            return read(tii);
        }
    }
}
