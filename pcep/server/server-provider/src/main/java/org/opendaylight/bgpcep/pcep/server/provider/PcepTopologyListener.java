/*
 * Copyright (c) 2021 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.server.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Iterables;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.DecimalBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.descriptions.PathDescription;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.descriptions.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.sr.subobject.nai.IpAdjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev250402.sr.subobject.nai.IpNodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PathStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.ComputedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.IntendedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.intended.path.ConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250328.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class listen to the PCEP Topology in order to trigger Path Manager methods to control Managed TE Path.
 *
 * @author Olivier Dugeon
 */
public final class PcepTopologyListener implements DataTreeChangeListener<Node>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PcepTopologyListener.class);

    private final PathManagerProvider pathManager;

    private Registration listenerRegistration;

    public PcepTopologyListener(final DataBroker dataBroker, final WithKey<Topology, TopologyKey> topology,
            final PathManagerProvider pathManager) {
        this.pathManager = requireNonNull(pathManager);
        listenerRegistration = dataBroker.registerTreeChangeListener(LogicalDatastoreType.OPERATIONAL,
            topology.toBuilder().toReferenceBuilder().child(Node.class).build(), this);
        LOG.info("Registered PCE Server listener {} for Operational PCEP Topology {}",
                listenerRegistration, topology.key().getTopologyId().getValue());
    }

    /**
     * Close this Listener.
     */
    @Override
    public void close() {
        if (listenerRegistration != null) {
            LOG.info("Unregistered PCE Server listener {} for Operational PCEP Topology", listenerRegistration);
            listenerRegistration.close();
            listenerRegistration = null;
        }
    }

    /**
     * Handle reported LSP modifications.
     *
     * @param nodeId    Node Identifier to which the modified children belongs to.
     * @param lspMod    List of Reported LSP modifications.
     */
    private void handleLspChange(final NodeId nodeId, final List<DataObjectModification<?>> lspMod) {
        for (DataObjectModification<?> lsp : lspMod) {
            ReportedLsp rptLsp;

            switch (lsp.modificationType()) {
                case DELETE:
                    rptLsp = (ReportedLsp) lsp.dataBefore();
                    LOG.debug("Un-Register Managed TE Path: {}", rptLsp.getName());
                    pathManager.unregisterTePath(nodeId, new ConfiguredLspKey(rptLsp.getName()));
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    rptLsp = (ReportedLsp) lsp.dataAfter();
                    LOG.debug("Register Managed TE Path {}", rptLsp.getName());
                    pathManager.registerTePath(nodeId,  getConfiguredLsp(rptLsp), getPathType(rptLsp));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Parse Sub Tree modification. Given list has been filtered to get only Path Computation Client modifications.
     * This function first create, update or delete Managed TE Node that corresponds to the given NodeId. Then, it
     * filter the children to retain only the reported LSP modifications.
     *
     * @param nodeId    Node Identifier to which the modified children belongs to.
     * @param pccMod    List of Path Computation Client modifications.
     */
    private void handlePccChange(final NodeId nodeId, final List<DataObjectModification<?>> pccMod) {
        for (DataObjectModification<?> node : pccMod) {
            /* First, process PCC modification */
            switch (node.modificationType()) {
                case DELETE:
                    LOG.debug("Un-Register Managed TE Node: {}", nodeId);
                    pathManager.disableManagedTeNode(nodeId);
                    /* Should stop here to avoid deleting later the associated Managed TE Path */
                    return;
                case SUBTREE_MODIFIED:
                case WRITE:
                    /* First look if the PCC was already created or not yet */
                    if (pathManager.checkManagedTeNode(nodeId)) {
                        /* Check if PCC State is Synchronized */
                        if (node.modifiedChildren() == null || node.modifiedChildren().isEmpty()) {
                            PathComputationClient pcc = (PathComputationClient) node.dataAfter();
                            if (pcc.getStateSync() == PccSyncState.Synchronized) {
                                LOG.debug("Synchronize Managed TE Node {}", nodeId);
                                pathManager.syncManagedTeNode(nodeId);
                            }
                            return;
                        }
                    } else {
                        LOG.debug("Register new Managed TE Node {}", nodeId);
                        pathManager.registerManagedTeNode(nodeId);
                    }
                    break;
                default:
                    break;
            }

            /* Then, look to reported LSP modification */
            final List<DataObjectModification<?>> lspMod = node.modifiedChildren()
                    .stream().filter(mod -> mod.dataType().equals(ReportedLsp.class))
                    .collect(Collectors.toList());
            if (!lspMod.isEmpty()) {
                handleLspChange(nodeId, lspMod);
            }
        }
    }

    /**
     * Parse Sub Tree modification. Given children list has been filtered to get only Node1 modifications.
     * This function filter again this given list to retain only PathComputationClient modifications.
     *
     * @param nodeId    Node Identifier to which the modified children belongs to.
     * @param node1Mod  List of Node1 modifications.
     */
    private void handleNode1Change(final NodeId nodeId, final List<DataObjectModification<?>> node1Mod) {
        for (DataObjectModification<?> child : node1Mod) {
            /* Then, look only to PathComputationClient.class modification */
            final List<DataObjectModification<?>> pccMod = child.modifiedChildren()
                    .stream().filter(mod -> mod.dataType().equals(PathComputationClient.class))
                    .collect(Collectors.toList());
            if (!pccMod.isEmpty()) {
                handlePccChange(nodeId, pccMod);
            }
        }
    }

    @Override
    public void onDataTreeChanged(final List<DataTreeModification<Node>> changes) {
        for (var change : changes) {
            final var root = change.getRootNode();

            final NodeId nodeId =
                root.modificationType() == DataObjectModification.ModificationType.DELETE
                    ? root.dataBefore().getNodeId() : root.dataAfter().getNodeId();

            /* Look only to Node1.class modification */
            final List<DataObjectModification<?>> node1Mod = root.modifiedChildren().stream()
                    .filter(mod -> mod.dataType().equals(Node1.class))
                    .collect(Collectors.toList());
            if (!node1Mod.isEmpty()) {
                handleNode1Change(nodeId, node1Mod);
            }
        }
    }

    /**
     * Translate ERO Segment Routing SubOject i.e. NaiType into Path Description.
     *
     * @param srObj Segment Routing SubObject.
     * @param af    Address Family, SR-IPv4 or SR-IPv6.
     *
     * @return      Path Description of the corresponding ERO SubObject.
     */
    private static PathDescription getSrPath(final SrSubobject srObj, final AddressFamily af) {
        return switch (af) {
            case SrIpv4 ->
                switch (srObj.getNaiType()) {
                    case Ipv4Adjacency -> new PathDescriptionBuilder()
                        .setSid(srObj.getSid())
                        .setIpv4(((IpAdjacency)srObj.getNai()).getLocalIpAddress().getIpv4AddressNoZone())
                        .setRemoteIpv4(((IpAdjacency)srObj.getNai()).getRemoteIpAddress().getIpv4AddressNoZone())
                        .build();
                    case Ipv4NodeId -> new PathDescriptionBuilder()
                        .setSid(srObj.getSid())
                        .setRemoteIpv4(((IpNodeId)srObj.getNai()).getIpAddress().getIpv4AddressNoZone())
                        .build();
                    default -> null;
                };
            case SrIpv6 ->
                switch (srObj.getNaiType()) {
                    case Ipv6Adjacency -> new PathDescriptionBuilder()
                        .setSid(srObj.getSid())
                        .setIpv6(((IpAdjacency)srObj.getNai()).getLocalIpAddress().getIpv6AddressNoZone())
                        .setRemoteIpv6(((IpAdjacency)srObj.getNai()).getRemoteIpAddress().getIpv6AddressNoZone())
                        .build();
                    case Ipv6NodeId -> new PathDescriptionBuilder()
                        .setSid(srObj.getSid())
                        .setRemoteIpv6(((IpNodeId)srObj.getNai()).getIpAddress().getIpv6AddressNoZone())
                        .build();
                    default -> null;
                };
            default -> null;
        };
    }

    /**
     * Translate ERO RSVP-TE SubObject i.e. IpPrefixSubobject into Path Description.
     *
     * @param ipPrefix the subobject
     * @param af Address Family, SR-IPv4 or SR-IPv6
     *
     * @return Path Description of the corresponding ERO SubObject.
     */
    private static PathDescription getIpPath(final IpPrefixSubobject ipPrefix, final AddressFamily af) {
        return switch (af) {
            case Ipv4 -> new PathDescriptionBuilder()
                .setRemoteIpv4(IetfInetUtil.ipv4AddressFrom(ipPrefix.getIpPrefix().getIpv4Prefix()))
                .build();
            case Ipv6 -> new PathDescriptionBuilder()
                .setRemoteIpv6(IetfInetUtil.ipv6AddressFrom(ipPrefix.getIpPrefix().getIpv6Prefix()))
                .build();
            default -> null;
        };
    }

    /**
     * Translate Explicit Route Object (ERO) of the LSP into Path Description.
     *
     * @param ero   Explicit Route Object.
     * @param af    Address Family, IPv4 or IPv6.
     *
     * @return      Path Description of the corresponding TE Path.
     */
    private static List<PathDescription> getPathDescription(final Ero ero, final AddressFamily af) {
        final var pathDesc = new ArrayList<PathDescription>();
        for (var element : ero.nonnullSubobject()) {
            switch (element.getSubobjectType()) {
                case SrSubobject sr -> pathDesc.add(getSrPath(sr, af));
                case IpPrefixCase ip -> pathDesc.add(getIpPath(ip.getIpPrefix(), af));
                case null, default -> {
                    // no-op
                }
            }
        }
        return pathDesc.isEmpty() ? null : pathDesc;
    }

    /**
     * Translate Record Route Object (RRO) of the LSP into a Path Description.
     *
     * @param rro   Record Route Object of the reported LSP.
     * @param af    Address Family, IPv4, IPv6, SR-IPv4 or SR-IPv6
     *
     * @return      Path Description of the corresponding TE Path.
     */
    private static List<PathDescription> getPathDescription(final Rro rro, final AddressFamily af) {
        final var pathDesc = new ArrayList<PathDescription>();
        for (var element : rro.nonnullSubobject()) {
            switch (element.getSubobjectType()) {
                case SrSubobject sr -> pathDesc.add(getSrPath(sr, af));
                case org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route
                    .subobjects.subobject.type.IpPrefixCase ip -> pathDesc.add(getIpPath(ip.getIpPrefix(), af));
                case null, default -> {
                    // no-op
                }
            }
        }
        return pathDesc.isEmpty() ? null : pathDesc;
    }

    /**
     * Build a new TE Path from a reported LSP.
     *
     * @param rl    Reported LSP.
     *
     * @return      new TE Path.
     */
    private static ConfiguredLsp getConfiguredLsp(final ReportedLsp rl) {
        /* New reported LSP is always the last Path in the List i.e. old Paths are place before */
        Path path = Iterables.getLast(rl.getPath().values());
        Float convert;
        ConstraintsBuilder cb = new ConstraintsBuilder();

        /* Set Constraints */
        if (path.getClassType() != null) {
            cb.setClassType(path.getClassType().getClassType().getValue());
        }
        if (path.getBandwidth() != null) {
            convert = ByteBuffer.wrap(path.getBandwidth().getBandwidth().getValue()).getFloat();
            cb.setBandwidth(new DecimalBandwidth(Decimal64.valueOf(2, convert.longValue())));
        }
        if ((cb.getBandwidth() == null || cb.getBandwidth().getValue().equals(Decimal64.valueOf(2, 0)))
                && path.getReoptimizationBandwidth() != null) {
            convert = ByteBuffer.wrap(path.getReoptimizationBandwidth().getBandwidth().getValue()).getFloat();
            cb.setBandwidth(new DecimalBandwidth(Decimal64.valueOf(2, convert.longValue())));
        }
        if (path.getMetrics() != null) {
            for (var metric : path.getMetrics()) {
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
        final PsType pst = path.getPathSetupType() != null ? path.getPathSetupType().getPst() : PsType.RsvpTe;
        final Lsp lsp = path.getLsp();
        final var af = lsp.getTlvs().getLspIdentifiers().getAddressFamily();
        final IpAddress source;
        final IpAddress destination;

        switch (af) {
            case Ipv4Case v4 -> {
                final var ipv4 = v4.getIpv4();
                source = new IpAddress(ipv4.getIpv4TunnelSenderAddress());
                destination = new IpAddress(ipv4.getIpv4TunnelEndpointAddress());
                cb.setAddressFamily(pst == PsType.RsvpTe ? AddressFamily.Ipv4 : AddressFamily.SrIpv4);
            }
            case Ipv6Case v6 -> {
                final var ipv6 = v6.getIpv6();
                source = new IpAddress(ipv6.getIpv6TunnelSenderAddress());
                destination = new IpAddress(ipv6.getIpv6TunnelSenderAddress());
                cb.setAddressFamily(pst == PsType.RsvpTe ? AddressFamily.Ipv6 : AddressFamily.SrIpv6);
            }
            case null, default -> {
                return null;
            }
        }

        /* Build Intended Path */
        final IntendedPathBuilder ipb = new IntendedPathBuilder()
                .setSource(source)
                .setDestination(destination)
                .setConstraints(cb.build());

        /* Get a Valid Path Description for this TePath if any */
        List<PathDescription> pathDesc = null;
        if (path.getEro() != null
                && path.getEro().getSubobject() != null
                && path.getEro().getSubobject().size() > 0) {
            pathDesc = getPathDescription(path.getEro(), cb.getAddressFamily());
        }
        if (pathDesc == null
                && path.getRro() != null
                && path.getRro().getSubobject() != null
                && path.getRro().getSubobject().size() > 0) {
            pathDesc = getPathDescription(path.getRro(), cb.getAddressFamily());
        }

        ConfiguredLspBuilder clb =
            new ConfiguredLspBuilder()
                .setName(rl.getName())
                .setPathStatus(PathStatus.Reported)
                .setIntendedPath(ipb.build());

        /* Finally Build Actual Path and TE Path */
        if (pathDesc == null) {
            return clb.setComputedPath(
                    new ComputedPathBuilder().setComputationStatus(ComputationStatus.Failed).build())
                .build();
        }
        return clb.setComputedPath(
                new ComputedPathBuilder()
                    .setPathDescription(pathDesc)
                    .setComputationStatus(
                        lsp.getLspFlags().getOperational() == OperationalStatus.Down
                            ? ComputationStatus.Failed
                            : ComputationStatus.Completed)
                    .build())
            .build();
    }

    /**
     * get Path Type from a reported LSP.
     *
     * @param rl    Reported LSP.
     *
     * @return      Path Type.
     */
    private static PathType getPathType(final ReportedLsp rl) {
        /* New reported LSP is always the last Path in the List i.e. old Paths are place before */
        final Path p1 = Iterables.getLast(rl.getPath().values());
        if (!p1.getLsp().getLspFlags().getDelegate()) {
            return PathType.Pcc;
        }
        final Lsp lspCreateFlag = p1.getLsp();
        if (lspCreateFlag == null || !lspCreateFlag.getLspFlags().getCreate()) {
            return PathType.Delegated;
        }
        return PathType.Initiated;
    }
}

