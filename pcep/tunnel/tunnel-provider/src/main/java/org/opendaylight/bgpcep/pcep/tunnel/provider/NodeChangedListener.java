/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Path1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv6._case.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.SupportingNode1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev181109.tunnel.pcep.supporting.node.attributes.PathComputationClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.IgpTerminationPointAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.TerminationPointType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.Ip;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.IpBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NodeChangedListener implements ClusteredDataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeChangedListener.class);
    private final InstanceIdentifier<Topology> target;
    private final DataBroker dataProvider;
    private final TopologyId source;

    NodeChangedListener(final DataBroker dataProvider, final TopologyId source,
            final InstanceIdentifier<Topology> target) {
        this.dataProvider = requireNonNull(dataProvider);
        this.target = requireNonNull(target);
        this.source = requireNonNull(source);
    }

    private static void categorizeIdentifier(final InstanceIdentifier<?> identifier,
            final Set<InstanceIdentifier<ReportedLsp>> changedLsps,
            final Set<InstanceIdentifier<Node>> changedNodes) {
        final InstanceIdentifier<ReportedLsp> li = identifier.firstIdentifierOf(ReportedLsp.class);
        if (li == null) {
            final InstanceIdentifier<Node> ni = identifier.firstIdentifierOf(Node.class);
            if (ni == null) {
                LOG.warn("Ignoring uncategorized identifier {}", identifier);
            } else {
                changedNodes.add(ni);
            }
        } else {
            changedLsps.add(li);
        }
    }

    private static void enumerateLsps(final InstanceIdentifier<Node> id, final Node node,
            final Set<InstanceIdentifier<ReportedLsp>> lsps) {
        if (node == null) {
            LOG.trace("Skipping null node {}", id);
            return;
        }
        final Node1 pccnode = node.augmentation(Node1.class);
        if (pccnode == null) {
            LOG.trace("Skipping non-PCEP-enabled node {}", id);
            return;
        }

        for (final ReportedLsp l : pccnode.getPathComputationClient().nonnullReportedLsp().values()) {
            lsps.add(id.builder().augmentation(Node1.class).child(PathComputationClient.class)
                    .child(ReportedLsp.class, l.key()).build());
        }
    }

    private static LinkId linkIdForLsp(final InstanceIdentifier<ReportedLsp> identifier, final ReportedLsp lsp) {
        return new LinkId(identifier.firstKeyOf(Node.class).getNodeId().getValue() + "/lsps/" + lsp.getName());
    }

    public static InstanceIdentifier<Link> linkIdentifier(final InstanceIdentifier<Topology> topology,
            final NodeId node, final String name) {
        return topology.child(Link.class, new LinkKey(new LinkId(node.getValue() + "/lsp/" + name)));
    }

    private InstanceIdentifier<Link> linkForLsp(final LinkId linkId) {
        return target.child(Link.class, new LinkKey(linkId));
    }

    private SupportingNode createSupportingNode(final NodeId sni, final Boolean inControl) {
        return new SupportingNodeBuilder()
                .setNodeRef(sni)
                .withKey(new SupportingNodeKey(sni, source))
                .addAugmentation(new SupportingNode1Builder().setPathComputationClient(
                    new PathComputationClientBuilder().setControlling(inControl).build()).build())
                .build();
    }

    private void handleSni(final InstanceIdentifier<Node> sni, final Node node, final Boolean inControl,
            final ReadWriteTransaction trans) {
        if (sni != null) {
            final NodeKey k = InstanceIdentifier.keyOf(sni);
            boolean have = false;
            /*
             * We may have found a termination point which has been created as a destination,
             * so it does not have a supporting node pointer. Since we now know what it is,
             * fill it in.
             */
            for (final SupportingNode sn : node.nonnullSupportingNode().values()) {
                if (sn.getNodeRef().equals(k.getNodeId())) {
                    have = true;
                    break;
                }
            }
            if (!have) {
                final SupportingNode sn = createSupportingNode(k.getNodeId(), inControl);
                trans.put(LogicalDatastoreType.OPERATIONAL, target.child(Node.class, node.key()).child(
                        SupportingNode.class, sn.key()), sn);
            }
        }
    }

    private InstanceIdentifier<TerminationPoint> getIpTerminationPoint(final ReadWriteTransaction trans,
            final IpAddress addr, final InstanceIdentifier<Node> sni, final Boolean inControl)
            throws ExecutionException, InterruptedException {
        final Topology topo = trans.read(LogicalDatastoreType.OPERATIONAL, target).get().get();
        for (final Node n : topo.nonnullNode().values()) {
            for (final TerminationPoint tp : n.nonnullTerminationPoint().values()) {
                final TerminationPoint1 tpa = tp.augmentation(TerminationPoint1.class);
                if (tpa != null) {
                    final TerminationPointType tpt = tpa.getIgpTerminationPointAttributes()
                            .getTerminationPointType();
                    if (tpt instanceof Ip) {
                        for (final IpAddress address : ((Ip) tpt).getIpAddress()) {
                            if (addr.equals(address)) {
                                handleSni(sni, n, inControl, trans);
                                return target.builder().child(Node.class, n.key())
                                        .child(TerminationPoint.class, tp.key()).build();
                            }
                        }
                    } else {
                        LOG.debug("Ignoring termination point type {}", tpt);
                    }
                }
            }
        }
        LOG.debug("Termination point for {} not found, creating a new one", addr);
        return createTP(addr, sni, inControl, trans);
    }

    private InstanceIdentifier<TerminationPoint> createTP(final IpAddress addr, final InstanceIdentifier<Node> sni,
            final Boolean inControl, final ReadWriteTransaction trans) {
        final String url = "ip://" + addr.toString();
        final TerminationPointKey tpk = new TerminationPointKey(new TpId(url));
        final TerminationPointBuilder tpb = new TerminationPointBuilder();
        tpb.withKey(tpk).setTpId(tpk.getTpId());
        tpb.addAugmentation(new TerminationPoint1Builder()
            .setIgpTerminationPointAttributes(new IgpTerminationPointAttributesBuilder()
                .setTerminationPointType(new IpBuilder().setIpAddress(Set.of(addr)).build())
                .build())
            .build());

        final NodeKey nk = new NodeKey(new NodeId(url));
        final NodeBuilder nb = new NodeBuilder();
        nb.withKey(nk).setNodeId(nk.getNodeId());
        nb.setTerminationPoint(BindingMap.of(tpb.build()));
        if (sni != null) {
            nb.setSupportingNode(BindingMap.of(createSupportingNode(InstanceIdentifier.keyOf(sni).getNodeId(),
                    inControl)));
        }
        final InstanceIdentifier<Node> nid = target.child(Node.class, nb.key());
        trans.put(LogicalDatastoreType.OPERATIONAL, nid, nb.build());
        return nid.child(TerminationPoint.class, tpb.key());
    }

    private void create(final ReadWriteTransaction trans, final InstanceIdentifier<ReportedLsp> identifier,
            final ReportedLsp value) throws ExecutionException, InterruptedException {
        final InstanceIdentifier<Node> ni = identifier.firstIdentifierOf(Node.class);

        final Path1 rl = value.nonnullPath().values().iterator().next().augmentation(Path1.class);

        final AddressFamily af = rl.getLsp().getTlvs().getLspIdentifiers().getAddressFamily();

        /*
         * We are trying to ensure we have source and destination nodes.
         */
        final IpAddress srcIp;
        final IpAddress dstIp;
        if (af instanceof Ipv4Case) {
            final Ipv4 ipv4 = ((Ipv4Case) af).getIpv4();
            srcIp = new IpAddress(ipv4.getIpv4TunnelSenderAddress());
            dstIp = new IpAddress(ipv4.getIpv4TunnelEndpointAddress());
        } else if (af instanceof Ipv6Case) {
            final Ipv6 ipv6 = ((Ipv6Case) af).getIpv6();
            srcIp = new IpAddress(ipv6.getIpv6TunnelSenderAddress());
            dstIp = new IpAddress(ipv6.getIpv6TunnelSenderAddress());
        } else {
            throw new IllegalArgumentException("Unsupported address family: " + af.implementedInterface());
        }

        final Path path0 = value.nonnullPath().values().iterator().next();
        final Link1Builder lab = new Link1Builder();
        if (path0.getBandwidth() != null) {
            lab.setBandwidth(path0.getBandwidth().getBandwidth());
        }
        if (path0.getClassType() != null) {
            lab.setClassType(path0.getClassType().getClassType());
        }
        lab.setSymbolicPathName(value.getName());

        final InstanceIdentifier<TerminationPoint> dst = getIpTerminationPoint(trans, dstIp, null, Boolean.FALSE);
        final InstanceIdentifier<TerminationPoint> src = getIpTerminationPoint(trans, srcIp, ni,
                rl.getLsp().getDelegate());

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720
                .Link1Builder slab = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf
                .stateful.rev200720.Link1Builder();
        slab.setOperationalStatus(rl.getLsp().getOperational());
        slab.setAdministrativeStatus(rl.getLsp().getAdministrative() ? AdministrativeStatus.Active :
                AdministrativeStatus.Inactive);

        final LinkId id = linkIdForLsp(identifier, value);
        final LinkBuilder lb = new LinkBuilder();
        lb.setLinkId(id);

        lb.setSource(new SourceBuilder().setSourceNode(src.firstKeyOf(Node.class).getNodeId())
                .setSourceTp(src.firstKeyOf(TerminationPoint.class).getTpId()).build());
        lb.setDestination(new DestinationBuilder().setDestNode(dst.firstKeyOf(Node.class).getNodeId())
                .setDestTp(dst.firstKeyOf(TerminationPoint.class).getTpId()).build());
        lb.addAugmentation(lab.build());
        lb.addAugmentation(slab.build());

        trans.put(LogicalDatastoreType.OPERATIONAL, linkForLsp(id), lb.build());
    }

    private InstanceIdentifier<TerminationPoint> tpIdentifier(final NodeId node, final TpId tp) {
        return target.builder().child(Node.class, new NodeKey(node)).child(TerminationPoint.class,
                new TerminationPointKey(tp)).build();
    }

    private InstanceIdentifier<Node> nodeIdentifier(final NodeId node) {
        return target.child(Node.class, new NodeKey(node));
    }

    private void remove(final ReadWriteTransaction trans, final InstanceIdentifier<ReportedLsp> identifier,
            final ReportedLsp value) throws ExecutionException, InterruptedException {
        final InstanceIdentifier<Link> li = linkForLsp(linkIdForLsp(identifier, value));

        final Optional<Link> ol = trans.read(LogicalDatastoreType.OPERATIONAL, li).get();
        if (!ol.isPresent()) {
            return;
        }

        final Link l = ol.get();
        LOG.debug("Removing link {} (was {})", li, l);
        trans.delete(LogicalDatastoreType.OPERATIONAL, li);

        LOG.debug("Searching for orphan links/nodes");
        final Optional<Topology> ot = trans.read(LogicalDatastoreType.OPERATIONAL, target).get();
        Preconditions.checkState(ot.isPresent());

        final Topology topology = ot.get();
        final NodeId srcNode = l.getSource().getSourceNode();
        final NodeId dstNode = l.getDestination().getDestNode();
        final TpId srcTp = l.getSource().getSourceTp();
        final TpId dstTp = l.getDestination().getDestTp();

        boolean orphSrcNode = true;
        boolean orphDstNode = true;
        boolean orphDstTp = true;
        boolean orphSrcTp = true;
        for (final Link lw : topology.nonnullLink().values()) {
            LOG.trace("Checking link {}", lw);

            final NodeId sn = lw.getSource().getSourceNode();
            final NodeId dn = lw.getDestination().getDestNode();
            final TpId st = lw.getSource().getSourceTp();
            final TpId dt = lw.getDestination().getDestTp();

            // Source node checks
            if (srcNode.equals(sn)) {
                if (orphSrcNode) {
                    LOG.debug("Node {} held by source of link {}", srcNode, lw);
                    orphSrcNode = false;
                }
                if (orphSrcTp && srcTp.equals(st)) {
                    LOG.debug("TP {} held by source of link {}", srcTp, lw);
                    orphSrcTp = false;
                }
            }
            if (srcNode.equals(dn)) {
                if (orphSrcNode) {
                    LOG.debug("Node {} held by destination of link {}", srcNode, lw);
                    orphSrcNode = false;
                }
                if (orphSrcTp && srcTp.equals(dt)) {
                    LOG.debug("TP {} held by destination of link {}", srcTp, lw);
                    orphSrcTp = false;
                }
            }

            // Destination node checks
            if (dstNode.equals(sn)) {
                if (orphDstNode) {
                    LOG.debug("Node {} held by source of link {}", dstNode, lw);
                    orphDstNode = false;
                }
                if (orphDstTp && dstTp.equals(st)) {
                    LOG.debug("TP {} held by source of link {}", dstTp, lw);
                    orphDstTp = false;
                }
            }
            if (dstNode.equals(dn)) {
                if (orphDstNode) {
                    LOG.debug("Node {} held by destination of link {}", dstNode, lw);
                    orphDstNode = false;
                }
                if (orphDstTp && dstTp.equals(dt)) {
                    LOG.debug("TP {} held by destination of link {}", dstTp, lw);
                    orphDstTp = false;
                }
            }
        }

        if (orphSrcNode && !orphSrcTp) {
            LOG.warn("Orphan source node {} but not TP {}, retaining the node", srcNode, srcTp);
            orphSrcNode = false;
        }
        if (orphDstNode && !orphDstTp) {
            LOG.warn("Orphan destination node {} but not TP {}, retaining the node", dstNode, dstTp);
            orphDstNode = false;
        }

        if (orphSrcNode) {
            LOG.debug("Removing orphan node {}", srcNode);
            trans.delete(LogicalDatastoreType.OPERATIONAL, nodeIdentifier(srcNode));
        } else if (orphSrcTp) {
            LOG.debug("Removing orphan TP {} on node {}", srcTp, srcNode);
            trans.delete(LogicalDatastoreType.OPERATIONAL, tpIdentifier(srcNode, srcTp));
        }
        if (orphDstNode) {
            LOG.debug("Removing orphan node {}", dstNode);
            trans.delete(LogicalDatastoreType.OPERATIONAL, nodeIdentifier(dstNode));
        } else if (orphDstTp) {
            LOG.debug("Removing orphan TP {} on node {}", dstTp, dstNode);
            trans.delete(LogicalDatastoreType.OPERATIONAL, tpIdentifier(dstNode, dstTp));
        }
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Node>> changes) {
        final ReadWriteTransaction trans = dataProvider.newReadWriteTransaction();

        final Set<InstanceIdentifier<ReportedLsp>> lsps = new HashSet<>();
        final Set<InstanceIdentifier<Node>> nodes = new HashSet<>();

        final Map<InstanceIdentifier<?>, DataObject> original = new HashMap<>();
        final Map<InstanceIdentifier<?>, DataObject> updated = new HashMap<>();
        final Map<InstanceIdentifier<?>, DataObject> created = new HashMap<>();

        for (final DataTreeModification<?> change : changes) {
            final InstanceIdentifier<?> iid = change.getRootPath().getRootIdentifier();
            final DataObjectModification<?> rootNode = change.getRootNode();
            handleChangedNode(rootNode, iid, lsps, nodes, original, updated, created);
        }

        // Now walk all nodes, check for removals/additions and cascade them to LSPs
        for (final InstanceIdentifier<Node> iid : nodes) {
            enumerateLsps(iid, (Node) original.get(iid), lsps);
            enumerateLsps(iid, (Node) updated.get(iid), lsps);
            enumerateLsps(iid, (Node) created.get(iid), lsps);
        }

        // We now have list of all affected LSPs. Walk them create/remove them
        updateTransaction(trans, lsps, original, updated, created);

        trans.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Topology change committed successfully");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to propagate a topology change, target topology became inconsistent", throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    private void handleChangedNode(final DataObjectModification<?> changedNode, final InstanceIdentifier<?> iid,
            final Set<InstanceIdentifier<ReportedLsp>> lsps, final Set<InstanceIdentifier<Node>> nodes,
            final Map<InstanceIdentifier<?>, DataObject> original, final Map<InstanceIdentifier<?>, DataObject> updated,
            final Map<InstanceIdentifier<?>, DataObject> created) {

        // Categorize reported identifiers
        categorizeIdentifier(iid, lsps, nodes);

        // Get the subtrees
        switch (changedNode.getModificationType()) {
            case DELETE:
                original.put(iid, changedNode.getDataBefore());
                break;
            case SUBTREE_MODIFIED:
                original.put(iid, changedNode.getDataBefore());
                updated.put(iid, changedNode.getDataAfter());
                break;
            case WRITE:
                created.put(iid, changedNode.getDataAfter());
                break;
            default:
                throw new IllegalArgumentException("Unhandled modification type " + changedNode.getModificationType());
        }

        for (DataObjectModification<? extends DataObject> child : changedNode.getModifiedChildren()) {
            final List<PathArgument> pathArguments = new ArrayList<>();
            for (PathArgument pathArgument : iid.getPathArguments()) {
                pathArguments.add(pathArgument);
            }
            pathArguments.add(child.getIdentifier());
            final InstanceIdentifier<?> childIID = InstanceIdentifier.create(pathArguments);
            handleChangedNode(child, childIID, lsps, nodes, original, updated, created);
        }
    }

    private void updateTransaction(final ReadWriteTransaction trans,
            final Set<InstanceIdentifier<ReportedLsp>> lsps,
            final Map<InstanceIdentifier<?>, ? extends DataObject> old,
            final Map<InstanceIdentifier<?>, DataObject> updated,
            final Map<InstanceIdentifier<?>, DataObject> created) {

        for (final InstanceIdentifier<ReportedLsp> i : lsps) {
            final ReportedLsp oldValue = (ReportedLsp) old.get(i);
            ReportedLsp newValue = (ReportedLsp) updated.get(i);
            if (newValue == null) {
                newValue = (ReportedLsp) created.get(i);
            }

            LOG.debug("Updating lsp {} value {} -> {}", i, oldValue, newValue);
            if (oldValue != null) {
                try {
                    remove(trans, i, oldValue);
                } catch (final ExecutionException | InterruptedException e) {
                    LOG.warn("Failed to remove LSP {}", i, e);
                }
            }
            if (newValue != null) {
                try {
                    create(trans, i, newValue);
                } catch (final ExecutionException | InterruptedException e) {
                    LOG.warn("Failed to add LSP {}", i, e);
                }
            }
        }
    }

    DataBroker getDataProvider() {
        return dataProvider;
    }
}
