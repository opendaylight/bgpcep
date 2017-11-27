/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.tunnel.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.AdministrativeStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Path1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv6._case.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.Link1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.SupportingNode1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.SupportingNode1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.tunnel.pcep.rev130820.tunnel.pcep.supporting.node.attributes.PathComputationClientBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NodeChangedListener implements ClusteredDataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeChangedListener.class);
    private final InstanceIdentifier<Topology> target;
    private final DataBroker dataProvider;
    private final TopologyId source;

    NodeChangedListener(final DataBroker dataProvider, final TopologyId source, final InstanceIdentifier<Topology> target) {
        this.dataProvider = requireNonNull(dataProvider);
        this.target = requireNonNull(target);
        this.source = requireNonNull(source);
    }

    private static void categorizeIdentifier(final InstanceIdentifier<?> i, final Set<InstanceIdentifier<ReportedLsp>> changedLsps,
            final Set<InstanceIdentifier<Node>> changedNodes) {
        final InstanceIdentifier<ReportedLsp> li = i.firstIdentifierOf(ReportedLsp.class);
        if (li == null) {
            final InstanceIdentifier<Node> ni = i.firstIdentifierOf(Node.class);
            if (ni == null) {
                LOG.warn("Ignoring uncategorized identifier {}", i);
            } else {
                changedNodes.add(ni);
            }
        } else {
            changedLsps.add(li);
        }
    }

    private static void enumerateLsps(final InstanceIdentifier<Node> id, final Node node, final Set<InstanceIdentifier<ReportedLsp>> lsps) {
        if (node == null) {
            LOG.trace("Skipping null node", id);
            return;
        }
        final Node1 pccnode = node.getAugmentation(Node1.class);
        if (pccnode == null) {
            LOG.trace("Skipping non-PCEP-enabled node {}", id);
            return;
        }

        for (final ReportedLsp l : pccnode.getPathComputationClient().getReportedLsp()) {
            lsps.add(id.builder().augmentation(Node1.class).child(PathComputationClient.class).child(ReportedLsp.class, l.getKey()).build());
        }
    }

    private static LinkId linkIdForLsp(final InstanceIdentifier<ReportedLsp> i, final ReportedLsp lsp) {
        return new LinkId(i.firstKeyOf(Node.class, NodeKey.class).getNodeId().getValue() + "/lsps/" + lsp.getName());
    }

    private InstanceIdentifier<Link> linkForLsp(final LinkId linkId) {
        return this.target.child(Link.class, new LinkKey(linkId));
    }

    private SupportingNode createSupportingNode(final NodeId sni, final Boolean inControl) {
        final SupportingNodeKey sk = new SupportingNodeKey(sni, this.source);
        final SupportingNodeBuilder snb = new SupportingNodeBuilder();
        snb.setNodeRef(sni);
        snb.setKey(sk);
        snb.addAugmentation(SupportingNode1.class, new SupportingNode1Builder().setPathComputationClient(
                new PathComputationClientBuilder().setControlling(inControl).build()).build());

        return snb.build();
    }

    private void handleSni(final InstanceIdentifier<Node> sni, final Node n, final Boolean inControl, final ReadWriteTransaction trans) {
        if (sni != null) {
            final NodeKey k = InstanceIdentifier.keyOf(sni);
            boolean have = false;
            /*
             * We may have found a termination point which has been created as a destination,
             * so it does not have a supporting node pointer. Since we now know what it is,
             * fill it in.
             */
            if (n.getSupportingNode() != null) {
                for (final SupportingNode sn : n.getSupportingNode()) {
                    if (sn.getNodeRef().equals(k.getNodeId())) {
                        have = true;
                        break;
                    }
                }
            }
            if (!have) {
                final SupportingNode sn = createSupportingNode(k.getNodeId(), inControl);
                trans.put(LogicalDatastoreType.OPERATIONAL, this.target.child(Node.class, n.getKey()).child(
                        SupportingNode.class, sn.getKey()), sn);
            }
        }
    }

    private InstanceIdentifier<TerminationPoint> getIpTerminationPoint(final ReadWriteTransaction trans, final IpAddress addr,
            final InstanceIdentifier<Node> sni, final Boolean inControl) throws ReadFailedException {
        final Topology topo = trans.read(LogicalDatastoreType.OPERATIONAL, this.target).checkedGet().get();
        if (topo.getNode() != null) {
            for (final Node n : topo.getNode()) {
                if(n.getTerminationPoint() != null) {
                    for (final TerminationPoint tp : n.getTerminationPoint()) {
                        final TerminationPoint1 tpa = tp.getAugmentation(TerminationPoint1.class);
                        if (tpa != null) {
                            final TerminationPointType tpt = tpa.getIgpTerminationPointAttributes().getTerminationPointType();
                            if (tpt instanceof Ip) {
                                for (final IpAddress a : ((Ip) tpt).getIpAddress()) {
                                    if (addr.equals(a)) {
                                        handleSni(sni, n, inControl, trans);
                                        return this.target.builder().child(Node.class, n.getKey()).child(TerminationPoint.class, tp.getKey()).build();
                                    }
                                }
                            } else {
                                LOG.debug("Ignoring termination point type {}", tpt);
                            }
                        }
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
        tpb.setKey(tpk).setTpId(tpk.getTpId());
        tpb.addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder().setIgpTerminationPointAttributes(
                new IgpTerminationPointAttributesBuilder().setTerminationPointType(
                        new IpBuilder().setIpAddress(Lists.newArrayList(addr)).build()).build()).build());

        final NodeKey nk = new NodeKey(new NodeId(url));
        final NodeBuilder nb = new NodeBuilder();
        nb.setKey(nk).setNodeId(nk.getNodeId());
        nb.setTerminationPoint(Lists.newArrayList(tpb.build()));
        if (sni != null) {
            nb.setSupportingNode(Lists.newArrayList(createSupportingNode(InstanceIdentifier.keyOf(sni).getNodeId(), inControl)));
        }
        final InstanceIdentifier<Node> nid = this.target.child(Node.class, nb.getKey());
        trans.put(LogicalDatastoreType.OPERATIONAL, nid, nb.build());
        return nid.child(TerminationPoint.class, tpb.getKey());
    }

    private void create(final ReadWriteTransaction trans, final InstanceIdentifier<ReportedLsp> i, final ReportedLsp value) throws ReadFailedException {
        final InstanceIdentifier<Node> ni = i.firstIdentifierOf(Node.class);

        final Path1 rl = value.getPath().get(0).getAugmentation(Path1.class);

        final AddressFamily af = rl.getLsp().getTlvs().getLspIdentifiers().getAddressFamily();

        /*
         * We are trying to ensure we have source and destination nodes.
         */
        final IpAddress srcIp, dstIp;
        if (af instanceof Ipv4Case) {
            final Ipv4 ipv4 = ((Ipv4Case) af).getIpv4();
            srcIp = new IpAddress(ipv4.getIpv4TunnelSenderAddress());
            dstIp = new IpAddress(ipv4.getIpv4TunnelEndpointAddress());
        } else if (af instanceof Ipv6Case) {
            final Ipv6 ipv6 = ((Ipv6Case) af).getIpv6();
            srcIp = new IpAddress(ipv6.getIpv6TunnelSenderAddress());
            dstIp = new IpAddress(ipv6.getIpv6TunnelSenderAddress());
        } else {
            throw new IllegalArgumentException("Unsupported address family: " + af.getImplementedInterface());
        }

        final Path path0 = value.getPath().get(0);
        final Link1Builder lab = new Link1Builder();
        if (path0.getBandwidth() != null) {
            lab.setBandwidth(path0.getBandwidth().getBandwidth());
        }
        if (path0.getClassType() != null) {
            lab.setClassType(path0.getClassType().getClassType());
        }
        lab.setSymbolicPathName(value.getName());

        final InstanceIdentifier<TerminationPoint> dst = getIpTerminationPoint(trans, dstIp, null, Boolean.FALSE);
        final InstanceIdentifier<TerminationPoint> src = getIpTerminationPoint(trans, srcIp, ni, rl.getLsp().isDelegate());

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Link1Builder slab = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Link1Builder();
        slab.setOperationalStatus(rl.getLsp().getOperational());
        slab.setAdministrativeStatus(rl.getLsp().isAdministrative() ? AdministrativeStatus.Active : AdministrativeStatus.Inactive);

        final LinkId id = linkIdForLsp(i, value);
        final LinkBuilder lb = new LinkBuilder();
        lb.setLinkId(id);

        lb.setSource(new SourceBuilder().setSourceNode(src.firstKeyOf(Node.class, NodeKey.class).getNodeId()).setSourceTp(
                src.firstKeyOf(TerminationPoint.class, TerminationPointKey.class).getTpId()).build());
        lb.setDestination(new DestinationBuilder().setDestNode(dst.firstKeyOf(Node.class, NodeKey.class).getNodeId()).setDestTp(
                dst.firstKeyOf(TerminationPoint.class, TerminationPointKey.class).getTpId()).build());
        lb.addAugmentation(Link1.class, lab.build());
        lb.addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Link1.class,
                slab.build());

        trans.put(LogicalDatastoreType.OPERATIONAL, linkForLsp(id), lb.build());
    }

    private InstanceIdentifier<TerminationPoint> tpIdentifier(final NodeId node, final TpId tp) {
        return this.target.builder().child(Node.class, new NodeKey(node)).child(TerminationPoint.class, new TerminationPointKey(tp)).build();
    }

    private InstanceIdentifier<Node> nodeIdentifier(final NodeId node) {
        return this.target.child(Node.class, new NodeKey(node));
    }

    private void remove(final ReadWriteTransaction trans, final InstanceIdentifier<ReportedLsp> i, final ReportedLsp value) throws ReadFailedException {
        final InstanceIdentifier<Link> li = linkForLsp(linkIdForLsp(i, value));

        final Optional<Link> ol = trans.read(LogicalDatastoreType.OPERATIONAL, li).checkedGet();
        if (!ol.isPresent()) {
            return;
        }

        final Link l = ol.get();
        LOG.debug("Removing link {} (was {})", li, l);
        trans.delete(LogicalDatastoreType.OPERATIONAL, li);

        LOG.debug("Searching for orphan links/nodes");
        final Optional<Topology> ot = trans.read(LogicalDatastoreType.OPERATIONAL, this.target).checkedGet();
        Preconditions.checkState(ot.isPresent());

        final Topology t = ot.get();
        final NodeId srcNode = l.getSource().getSourceNode();
        final NodeId dstNode = l.getDestination().getDestNode();
        final TpId srcTp = l.getSource().getSourceTp();
        final TpId dstTp = l.getDestination().getDestTp();

        boolean orphSrcNode = true, orphDstNode = true, orphDstTp = true, orphSrcTp = true;
        for (final Link lw : t.getLink()) {
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
        final ReadWriteTransaction trans = this.dataProvider.newReadWriteTransaction();

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

        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(trans.submit()), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.trace("Topology change committed successfully");
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to propagate a topology change, target topology became inconsistent", t);
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

    private void updateTransaction(final ReadWriteTransaction trans, final Set<InstanceIdentifier<ReportedLsp>> lsps,
        final Map<InstanceIdentifier<?>, ? extends DataObject> old, final Map<InstanceIdentifier<?>, DataObject> updated,
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
                } catch (final ReadFailedException e) {
                    LOG.warn("Failed to remove LSP {}", i, e);
                }
            }
            if (newValue != null) {
                try {
                    create(trans, i, newValue);
                } catch (final ReadFailedException e) {
                    LOG.warn("Failed to add LSP {}", i, e);
                }
            }
        }
    }

    public static InstanceIdentifier<Link> linkIdentifier(final InstanceIdentifier<Topology> topology, final NodeId node, final String name) {
        return topology.child(Link.class, new LinkKey(new LinkId(node.getValue() + "/lsp/" + name)));
    }

    DataBroker getDataProvider() {
        return dataProvider;
    }
}
