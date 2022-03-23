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
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.DecimalBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220310.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220310.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220310.path.descriptions.PathDescription;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220310.path.descriptions.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Lsp1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Path1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv6._case.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.SrSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.nai.IpAdjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev200720.sr.subobject.nai.IpNodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PathStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.ConfiguredLspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.ComputedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.IntendedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.server.rev220321.pcc.configured.lsp.configured.lsp.intended.path.ConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.IpPrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class listen to the PCEP Topology in order to trigger Path Manager methods to control Managed TE Path.
 *
 * @author Olivier Dugeon
 */

public final class PcepTopologyListener implements DataTreeChangeListener<Node>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PcepTopologyListener.class);
    private ListenerRegistration<PcepTopologyListener> listenerRegistration;
    private final PathManagerProvider pathManager;

    public PcepTopologyListener(final DataBroker dataBroker, KeyedInstanceIdentifier<Topology, TopologyKey> topology,
            final PathManagerProvider pathManager) {
        requireNonNull(dataBroker);
        requireNonNull(topology);
        this.pathManager = requireNonNull(pathManager);
        final InstanceIdentifier<Node> nodeTopology = topology.child(Node.class);
        this.listenerRegistration = dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, nodeTopology), this);
        LOG.info("Registered PCE Server listener {} for Operational PCEP Topology {}",
                listenerRegistration, topology.getKey().getTopologyId().getValue());
    }

    /**
     * Close this Listener.
     */
    @Override
    public void close() {
        if (this.listenerRegistration != null) {
            LOG.info("Unregistered PCE Server listener {} for Operational PCEP Topology", listenerRegistration);
            this.listenerRegistration.close();
            this.listenerRegistration = null;
        }
    }

    /**
     * Handle reported LSP modifications.
     *
     * @param nodeId    Node Identifier to which the modified children belongs to.
     * @param lspMod    List of Reported LSP modifications.
     */
    private void handleLspChange(NodeId nodeId, List<? extends DataObjectModification<? extends DataObject>> lspMod) {
        for (DataObjectModification<? extends DataObject> lsp : lspMod) {
            ReportedLsp rptLsp;

            switch (lsp.getModificationType()) {
                case DELETE:
                    rptLsp = (ReportedLsp) lsp.getDataBefore();
                    LOG.debug("Un-Register Managed TE Path: {}", rptLsp.getName());
                    pathManager.unregisterTePath(nodeId, new ConfiguredLspKey(rptLsp.getName()));
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    rptLsp = (ReportedLsp) lsp.getDataAfter();
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
    private void handlePccChange(NodeId nodeId, List<? extends DataObjectModification<? extends DataObject>> pccMod) {
        for (DataObjectModification<? extends DataObject> node : pccMod) {
            /* First, process PCC modification */
            switch (node.getModificationType()) {
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
                        if (node.getModifiedChildren() == null || node.getModifiedChildren().isEmpty()) {
                            PathComputationClient pcc = (PathComputationClient) node.getDataAfter();
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
            final List<DataObjectModification<? extends DataObject>> lspMod = node.getModifiedChildren()
                    .stream().filter(mod -> mod.getDataType().equals(ReportedLsp.class))
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
    private void handleNode1Change(NodeId nodeId, List<DataObjectModification<? extends DataObject>> node1Mod) {
        for (DataObjectModification<? extends DataObject> child : node1Mod) {
            /* Then, look only to PathComputationClient.class modification */
            final List<DataObjectModification<? extends DataObject>> pccMod = child.getModifiedChildren()
                    .stream().filter(mod -> mod.getDataType().equals(PathComputationClient.class))
                    .collect(Collectors.toList());
            if (!pccMod.isEmpty()) {
                handlePccChange(nodeId, pccMod);
            }
        }
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            DataObjectModification<Node> root = change.getRootNode();

            final NodeId nodeId =
                root.getModificationType() == DataObjectModification.ModificationType.DELETE
                    ? root.getDataBefore().getNodeId()
                    : root.getDataAfter().getNodeId();

            /* Look only to Node1.class modification */
            final List<DataObjectModification<? extends DataObject>> node1Mod =
                root.getModifiedChildren().stream()
                    .filter(mod -> mod.getDataType().equals(Node1.class))
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
    private static PathDescription getSrPath(SrSubobject srObj, final AddressFamily af) {
        switch (af) {
            case SrIpv4:
                switch (srObj.getNaiType()) {
                    case Ipv4Adjacency:
                        return new PathDescriptionBuilder()
                            .setSid(srObj.getSid())
                            .setIpv4(((IpAdjacency)(srObj).getNai()).getLocalIpAddress().getIpv4AddressNoZone())
                            .setRemoteIpv4(((IpAdjacency)(srObj).getNai()).getRemoteIpAddress().getIpv4AddressNoZone())
                            .build();
                    case Ipv4NodeId:
                        return new PathDescriptionBuilder()
                            .setSid(srObj.getSid())
                            .setRemoteIpv4(((IpNodeId)(srObj).getNai()).getIpAddress().getIpv4AddressNoZone())
                            .build();
                    default:
                        return null;
                }
            case SrIpv6:
                switch (srObj.getNaiType()) {
                    case Ipv6Adjacency:
                        return new PathDescriptionBuilder()
                            .setSid(srObj.getSid())
                            .setIpv6(((IpAdjacency)(srObj).getNai()).getLocalIpAddress().getIpv6AddressNoZone())
                            .setRemoteIpv6(((IpAdjacency)(srObj).getNai()).getRemoteIpAddress().getIpv6AddressNoZone())
                            .build();
                    case Ipv6NodeId:
                        return new PathDescriptionBuilder()
                            .setSid(srObj.getSid())
                            .setRemoteIpv6(((IpNodeId)(srObj).getNai()).getIpAddress().getIpv6AddressNoZone())
                            .build();
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    /**
     * Translate ERO RSVP-TE SubObject i.e. IpPrefixCase into Path Description.
     *
     * @param srObj Segment Routing SubObject.
     * @param af    Address Family, SR-IPv4 or SR-IPv6.
     *
     * @return      Path Description of the corresponding ERO SubObject.
     */
    private static PathDescription getIpPath(IpPrefixCase ipc, final AddressFamily af) {
        switch (af) {
            case Ipv4:
                return new PathDescriptionBuilder().setRemoteIpv4(
                        new Ipv4Address(ipc.getIpPrefix().getIpPrefix().getIpv4Prefix().getValue().split("/")[0]))
                        .build();
            case Ipv6:
                return new PathDescriptionBuilder().setRemoteIpv6(
                        new Ipv6Address(ipc.getIpPrefix().getIpPrefix().getIpv6Prefix().getValue().split("/")[0]))
                        .build();
            default:
                return null;
        }
    }

    /**
     * Translate RRO RSVP-TE SubObject i.e. IpPrefixCase into Path Description.
     *
     * @param srObj Segment Routing SubObject.
     * @param af    Address Family, SR-IPv4 or SR-IPv6.
     *
     * @return      Path Description of the corresponding RRO SubObject.
     */
    private static PathDescription getIpPath(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .rsvp.rev150820._record.route.subobjects.subobject.type.IpPrefixCase ipc, final AddressFamily af) {
        switch (af) {
            case Ipv4:
                return new PathDescriptionBuilder().setRemoteIpv4(
                        new Ipv4Address(ipc.getIpPrefix().getIpPrefix().getIpv4Prefix().getValue().split("/")[0]))
                        .build();
            case Ipv6:
                return new PathDescriptionBuilder().setRemoteIpv6(
                        new Ipv6Address(ipc.getIpPrefix().getIpPrefix().getIpv6Prefix().getValue().split("/")[0]))
                        .build();
            default:
                return null;
        }
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
        final ArrayList<PathDescription> pathDesc = new ArrayList<PathDescription>();
        for (int i = 0; i < ero.getSubobject().size(); i++) {
            final SubobjectType sbt = ero.getSubobject().get(i).getSubobjectType();
            if (sbt instanceof SrSubobject) {
                pathDesc.add(getSrPath((SrSubobject) sbt, af));
            } else if (sbt instanceof IpPrefixCase) {
                pathDesc.add(getIpPath((IpPrefixCase) sbt, af));
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
        final ArrayList<PathDescription> pathDesc = new ArrayList<PathDescription>();
        for (int i = 0; i < rro.getSubobject().size(); i++) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820
                ._record.route.subobjects.SubobjectType sbt = rro.getSubobject().get(i).getSubobjectType();
            if (sbt instanceof SrSubobject) {
                pathDesc.add(getSrPath((SrSubobject) sbt, af));
            } else if (sbt instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820
                    ._record.route.subobjects.subobject.type.IpPrefixCase) {
                pathDesc.add(getIpPath((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820
                        ._record.route.subobjects.subobject.type.IpPrefixCase)sbt, af));
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
    private static ConfiguredLsp getConfiguredLsp(ReportedLsp rl) {
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
            cb.setBandwidth(new DecimalBandwidth(BigDecimal.valueOf(convert.longValue())));
        }
        if ((cb.getBandwidth() == null || cb.getBandwidth().getValue() == BigDecimal.ZERO)
                && path.getReoptimizationBandwidth() != null) {
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
        final Uint8 pst = p1.getPathSetupType() != null ? p1.getPathSetupType().getPst() : Uint8.ZERO;
        final Lsp lsp = p1.getLsp();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp
            .identifiers.tlv.lsp.identifiers.AddressFamily af = lsp.getTlvs().getLspIdentifiers().getAddressFamily();
        IpAddress source = null;
        IpAddress destination = null;
        if (af instanceof Ipv4Case) {
            final Ipv4 ipv4 = ((Ipv4Case) af).getIpv4();
            source = new IpAddress(ipv4.getIpv4TunnelSenderAddress());
            destination = new IpAddress(ipv4.getIpv4TunnelEndpointAddress());
            cb.setAddressFamily(pst == Uint8.ZERO ? AddressFamily.Ipv4 : AddressFamily.SrIpv4);
        } else if (af instanceof Ipv6Case) {
            final Ipv6 ipv6 = ((Ipv6Case) af).getIpv6();
            source = new IpAddress(ipv6.getIpv6TunnelSenderAddress());
            destination = new IpAddress(ipv6.getIpv6TunnelSenderAddress());
            cb.setAddressFamily(pst == Uint8.ZERO ? AddressFamily.Ipv6 : AddressFamily.SrIpv6);
        } else {
            return null;
        }

        /* Build Intended Path */
        final IntendedPathBuilder ipb = new IntendedPathBuilder()
                .setSource(source)
                .setDestination(destination)
                .setConstraints(cb.build());

        /* Build Actual Path */
        ComputedPathBuilder cpb = new ComputedPathBuilder();

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
        if (pathDesc != null) {
            cpb.setPathDescription(pathDesc);
            if (lsp.getOperational() == OperationalStatus.Down) {
                cpb.setComputationStatus(ComputationStatus.Failed);
            } else {
                cpb.setComputationStatus(ComputationStatus.Completed);
            }
        } else {
            cpb.setComputationStatus(ComputationStatus.Failed);
        }

        /* Finally build TE Path */
        return new ConfiguredLspBuilder()
                .setName(rl.getName())
                .setPathStatus(PathStatus.Reported)
                .setIntendedPath(ipb.build())
                .setComputedPath(cpb.build())
                .build();
    }

    /**
     * get Path Type from a reported LSP.
     *
     * @param rl    Reported LSP.
     *
     * @return      Path Type.
     */
    private static PathType getPathType(ReportedLsp rl) {
        /* New reported LSP is always the last Path in the List i.e. old Paths are place before */
        final Path1 p1 = Iterables.getLast(rl.getPath().values()).augmentation(Path1.class);
        if (!p1.getLsp().getDelegate()) {
            return PathType.Pcc;
        }
        final Lsp1 lspCreateFlag = p1.getLsp().augmentation(Lsp1.class);
        if (lspCreateFlag == null || !lspCreateFlag.getCreate()) {
            return PathType.Delegated;
        }
        return PathType.Initiated;
    }

}

