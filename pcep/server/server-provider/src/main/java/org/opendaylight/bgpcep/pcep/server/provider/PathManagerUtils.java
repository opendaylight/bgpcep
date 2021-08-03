/*
 * Copyright (c) 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import com.google.common.collect.Iterables;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.DecimalBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.PathConstraints.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.PathStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.TePath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.TePathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.te.path.ComputedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.path.arguments.ConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Arguments2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Arguments3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Path1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv6._case.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ClassType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.classtype.object.ClassTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv6._case.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;


public final class PathManagerUtils {

    private PathManagerUtils() {
        // Hidden on purpose
    }

    public static AddLspInput getAddLspInput(final NodeId id, final TePath tePath) {
        // Create EndPoint Object
        final EndpointsObjBuilder epb = new EndpointsObjBuilder()
                .setIgnore(false)
                .setProcessingRule(true);
        if (tePath.getSource().getIpv4Address() != null) {
            final Ipv4Builder ipBuilder = new Ipv4Builder();
            ipBuilder.setDestinationIpv4Address(new Ipv4AddressNoZone(tePath.getDestination().getIpv4Address()));
            ipBuilder.setSourceIpv4Address(new Ipv4AddressNoZone(tePath.getSource().getIpv4Address()));
            epb.setAddressFamily((new Ipv4CaseBuilder().setIpv4(ipBuilder.build()).build()));
        } else if (tePath.getSource().getIpv6Address() != null) {
            final Ipv6Builder ipBuilder = new Ipv6Builder();
            ipBuilder.setDestinationIpv6Address(new Ipv6AddressNoZone(tePath.getDestination().getIpv6Address()));
            ipBuilder.setSourceIpv6Address(new Ipv6AddressNoZone(tePath.getSource().getIpv6Address()));
            epb.setAddressFamily((new Ipv6CaseBuilder().setIpv6(ipBuilder.build()).build()));
        } else {
            // In case of ...
            return null;
        }

        // Create Path Setup Type
        final PathSetupTypeBuilder pstBuilder = new PathSetupTypeBuilder();
        switch (tePath.getConstraints().getAddressFamily()) {
            case SrIpv4:
            case SrIpv6:
                pstBuilder.setPst(Uint8.ONE);
                break;
            default:
                pstBuilder.setPst(Uint8.ZERO);
                break;
        }

        // Create LSP
        final LspBuilder lspBuilder = new LspBuilder()
                .setAdministrative(true)
                .setDelegate(true);

        // Build Arguments
        final ArgumentsBuilder args = new ArgumentsBuilder()
                .setEndpointsObj(epb.build())
                .addAugmentation(new Arguments2Builder()
                        .setLsp(lspBuilder.build())
                        .setPathSetupType(pstBuilder.build())
                        .build());
        if (tePath.getStatus() == PathStatus.Setup) {
            args.setEro(tePath.getComputedPath().getEro());
        }

        // with Bandwidth if defined, but not other Metrics as some routers don't support them
        if (tePath.getConstraints().getBandwidth() != null) {
            final int ftoi = Float.floatToIntBits(tePath.getConstraints().getBandwidth().getValue().floatValue());
            final byte[] itob = { (byte) (0xFF & ftoi >> 24), (byte) (0xFF & ftoi >> 16), (byte) (0xFF & ftoi >> 8),
                (byte) (0xFF & ftoi) };
            args.setBandwidth(new BandwidthBuilder().setBandwidth(new Bandwidth(itob)).build());
        }
        // and with Class Type if defined
        if (tePath.getConstraints().getClassType() != null) {
            args.setClassType(
                new ClassTypeBuilder()
                    .setClassType(new ClassType(tePath.getConstraints().getClassType()))
                    .setIgnore(false)
                    .setProcessingRule(true)
                    .build());
        }

        // Finally, build addLSP input
        AddLspInputBuilder ab = new AddLspInputBuilder()
                .setNode(id)
                .setName(tePath.getName())
                .setArguments(args.build())
                .setNetworkTopologyRef(new NetworkTopologyRef(InstanceIdentifier.builder(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(new TopologyId("pcep-topology"))).build()));

        return ab.build();
    }

    public static UpdateLspInput getUpdateLspInput(final NodeId id, final TePath tePath) {
        // Create Path Setup Type
        final PathSetupTypeBuilder pstBuilder = new PathSetupTypeBuilder();
        switch (tePath.getConstraints().getAddressFamily()) {
            case SrIpv4:
            case SrIpv6:
                pstBuilder.setPst(Uint8.ONE);
                break;
            default:
                pstBuilder.setPst(Uint8.ZERO);
                break;
        }

        // Create LSP
        final LspBuilder lspBuilder = new LspBuilder()
                .setAdministrative(true)
                .setDelegate(true);

        // Build Arguments
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120
            .update.lsp.args.ArgumentsBuilder args;
        args = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120
            .update.lsp.args.ArgumentsBuilder()
                .addAugmentation(new Arguments3Builder()
                    .setLsp(lspBuilder.build())
                    .setPathSetupType(pstBuilder.build())
                    .build())
                .setEro(tePath.getComputedPath().getEro());

        // with Bandwidth if defined, but not other Metrics as some routers don't support them
        if (tePath.getConstraints().getBandwidth() != null) {
            final int ftoi = Float.floatToIntBits(tePath.getConstraints().getBandwidth().getValue().floatValue());
            final byte[] itob = { (byte) (0xFF & ftoi >> 24), (byte) (0xFF & ftoi >> 16), (byte) (0xFF & ftoi >> 8),
                (byte) (0xFF & ftoi) };
            args.setBandwidth(new BandwidthBuilder().setBandwidth(new Bandwidth(itob)).build());
        }

        // and with Class Type if defined
        if (tePath.getConstraints().getClassType() != null) {
            args.setClassType(
                new ClassTypeBuilder()
                    .setClassType(new ClassType(tePath.getConstraints().getClassType()))
                    .setIgnore(false)
                    .setProcessingRule(true)
                    .build());
        }

        // Finally, build updateLSP input
        UpdateLspInputBuilder ub = new UpdateLspInputBuilder()
                .setNode(id)
                .setName(tePath.getName())
                .setArguments(args.build());

        return ub.build();
    }

    public static TePath getTePath(ReportedLsp rl) {
        Path path = Iterables.getOnlyElement(rl.getPath().values());
        Float convert;
        ConstraintsBuilder cb = new ConstraintsBuilder();

        /* Set Constraints */
        if (path.getClassType() != null) {
            cb.setClassType(path.getClassType().getClassType().getValue());
        }
        if (path.getBandwidth() != null) {
            convert = ByteBuffer.wrap(path.getBandwidth().getBandwidth().getValue()).getFloat();
            cb.setBandwidth(new DecimalBandwidth(BigDecimal.valueOf(convert.longValue())));
        } else if (path.getReoptimizationBandwidth() != null) {
            convert = ByteBuffer.wrap(path.getReoptimizationBandwidth().getBandwidth().getValue()).getFloat();
            cb.setBandwidth(new DecimalBandwidth(BigDecimal.valueOf(convert.longValue())));
        }
        if (path.getMetrics() != null) {
            for (Metrics metric: path.getMetrics()) {
                convert = ByteBuffer.wrap(metric.getMetric().getValue().getValue()).getFloat();
                switch (metric.getMetric().getMetricType().intValue()) {
                    case MessagesUtil.IGP_METRIC:
                        cb.setMetric(Uint32.valueOf(convert.longValue()));
                        break;
                    case MessagesUtil.TE_METRIC:
                        cb.setTeMetric(Uint32.valueOf(convert.longValue()));
                        break;
                    case MessagesUtil.PATH_DELAY:
                        cb.setDelay(new Delay(Uint32.valueOf(convert.longValue())));
                        break;
                    default:
                        break;
                }
            }
        }

        /* Get Source and Destination addresses and family */
        if (path.augmentations() == null) {
            return null;
        }
        final Path1 p1 = path.augmentation(Path1.class);
        final Uint8 pst;
        if (p1.getPathSetupType() != null) {
            pst = p1.getPathSetupType().getPst();
        } else {
            pst = Uint8.ZERO;
        }
        final Lsp lsp = p1.getLsp();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp
            .identifiers.tlv.lsp.identifiers.AddressFamily af = lsp.getTlvs().getLspIdentifiers().getAddressFamily();
        IpAddress source = null;
        IpAddress destination = null;
        if (af instanceof Ipv4Case) {
            final Ipv4 ipv4 = ((Ipv4Case) af).getIpv4();
            source = new IpAddress(ipv4.getIpv4TunnelSenderAddress());
            destination = new IpAddress(ipv4.getIpv4TunnelEndpointAddress());
            if (pst == Uint8.ZERO) {
                cb.setAddressFamily(AddressFamily.Ipv4);
            } else {
                cb.setAddressFamily(AddressFamily.SrIpv4);
            }
        } else if (af instanceof Ipv6Case) {
            final Ipv6 ipv6 = ((Ipv6Case) af).getIpv6();
            source = new IpAddress(ipv6.getIpv6TunnelSenderAddress());
            destination = new IpAddress(ipv6.getIpv6TunnelSenderAddress());
            if (pst == Uint8.ZERO) {
                cb.setAddressFamily(AddressFamily.Ipv6);
            } else {
                cb.setAddressFamily(AddressFamily.SrIpv6);
            }
        } else {
            return null;
        }

        /* Get ERO */
        ComputedPathBuilder cpb = new ComputedPathBuilder();
        if (path.getEro() != null) {
            cpb.setEro(path.getEro());
        }
        // TODO: Process RRO when ERO is not provided

        /* Finally build TE Path */
        TePathBuilder tpb = new TePathBuilder()
                .setName(rl.getName())
                .setConstraints(cb.build())
                .setSource(source)
                .setDestination(destination)
                .setComputedPath(cpb.build());
        // TODO: Add Status

        return tpb.build();
    }
}
