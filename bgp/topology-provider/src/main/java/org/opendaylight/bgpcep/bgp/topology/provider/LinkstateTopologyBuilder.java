/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.bgp.rib.rib.loc.rib.tables.routes.LinkstateRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.attribute.SrAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.routes.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.prefix.state.SrPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.prefix.sid.tlv.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.prefix.sid.tlv.flags.IsisPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.SidLabelIndex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.LocalLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.bgp.linkstate.topology.type.BgpLinkstateTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.SegmentId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.sr.node.attributes.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.sr.node.attributes.SegmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.sr.node.attributes.SegmentsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.sr.node.attributes.segments.segment.specification.AdjacencyCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.sr.node.attributes.segments.segment.specification.PrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.sr.node.attributes.segments.segment.specification.adjacency._case.AdjacencyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819.topology.sr.type.TopologySrBuilder;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.link.attributes.IgpLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.IgpNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.Prefix;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.node.attributes.igp.node.attributes.PrefixKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.IgpTerminationPointAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.TerminationPointType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.IpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.igp.termination.point.attributes.igp.termination.point.attributes.termination.point.type.UnnumberedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkstateTopologyBuilder extends AbstractTopologyBuilder<LinkstateRoute> {
    @VisibleForTesting
    static final TopologyTypes LINKSTATE_TOPOLOGY_TYPE = new TopologyTypesBuilder()
            .addAugmentation(new TopologyTypes1Builder()
                    .setBgpLinkstateTopology(new BgpLinkstateTopologyBuilder().build()).build()).build();
    @VisibleForTesting
    static final TopologyTypes SR_AWARE_LINKSTATE_TOPOLOGY_TYPE = new TopologyTypesBuilder(LINKSTATE_TOPOLOGY_TYPE)
            .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr.rev130819
                .TopologyTypes1Builder().setTopologySr(new TopologySrBuilder().build()).build())
            .build();

    private static final String UNHANDLED_OBJECT_CLASS = "Unhandled object class {}";

    private static final class TpHolder {
        private final Set<LinkId> local = new HashSet<>();
        private final Set<LinkId> remote = new HashSet<>();

        private final TerminationPoint tp;

        TpHolder(final TerminationPoint tp) {
            this.tp = requireNonNull(tp);
        }

        synchronized void addLink(final LinkId id, final boolean isRemote) {
            if (isRemote) {
                remote.add(id);
            } else {
                local.add(id);
            }
        }

        synchronized boolean removeLink(final LinkId id, final boolean isRemote) {
            final boolean removed;
            if (isRemote) {
                removed = remote.remove(id);
            } else {
                removed = local.remove(id);
            }
            if (!removed) {
                LOG.warn("Removed non-reference link {} from TP {} isRemote {}", tp.getTpId().getValue(),
                        id.getValue(), isRemote);
            }

            return local.isEmpty() && remote.isEmpty();
        }

        TerminationPoint getTp() {
            return tp;
        }
    }

    private final class NodeHolder {
        private final Map<PrefixKey, Prefix> prefixes = new HashMap<>();
        private final Map<TpId, TpHolder> tps = new HashMap<>();
        private boolean advertized = false;
        private IgpNodeAttributesBuilder inab;
        private NodeBuilder nb;
        private NodeSrHolder sr;

        NodeHolder(final NodeId id) {
            inab = new IgpNodeAttributesBuilder();
            nb = new NodeBuilder().withKey(new NodeKey(id)).setNodeId(id);
        }

        /**
         * Synchronized in-core state of a node into the backing store using the transaction.
         *
         * @param trans data modification transaction which to use
         * @return True if the node has been purged, false otherwise.
         */
        boolean syncState(final WriteTransaction trans) {
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(nb.key());

            /*
             * Transaction's putOperationalData() does a merge. Force it onto a replace
             * by removing the data. If we decide to remove the node -- we just skip the put.
             */
            trans.delete(LogicalDatastoreType.OPERATIONAL, nid);

            if (!advertized) {
                if (tps.isEmpty() && prefixes.isEmpty()) {
                    LOG.trace("Removing unadvertized unused node {}", nb.getNodeId().getValue());
                    return true;
                }

                LOG.trace("Node {} is still implied by {} TPs and {} prefixes", nb.getNodeId().getValue(),
                        tps.size(), prefixes.size());
            }

            // Re-generate termination points
            nb.setTerminationPoint(BindingMap.ordered(Collections2.transform(tps.values(), TpHolder::getTp)));

            // Re-generate prefixes
            inab.setPrefix(BindingMap.ordered(prefixes.values()));

            // Write the node out
            if (sr != null && sr.getSegmentCount() > 0) {
                nb.addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr
                    .rev130819.Node1Builder().setSegments(BindingMap.ordered(sr.getSegments())).build());
            }
            final Node n = nb
                    .addAugmentation(new Node1Builder().setIgpNodeAttributes(inab.build()).build())
                    .build();
            trans.put(LogicalDatastoreType.OPERATIONAL, nid, n);
            LOG.trace("Created node {} at {}", n, nid);
            return false;
        }

        boolean checkForRemoval(final WriteTransaction trans) {
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(nb.key());

            if (!advertized) {
                if (tps.isEmpty() && prefixes.isEmpty()) {
                    trans.delete(LogicalDatastoreType.OPERATIONAL, nid);
                    LOG.trace("Removing unadvertized unused node {}", nb.getNodeId().getValue());
                    return true;
                }

                LOG.trace("Node {} is still implied by {} TPs and {} prefixes", nb.getNodeId().getValue(),
                        tps.size(), prefixes.size());
            }
            return false;
        }

        synchronized void removeTp(final TpId tp, final LinkId link, final boolean isRemote) {
            final TpHolder h = tps.get(tp);
            if (h != null) {
                if (h.removeLink(link, isRemote)) {
                    tps.remove(tp);
                    LOG.trace("Removed TP {}", tp.getValue());
                }
            } else {
                LOG.warn("Removed non-present TP {} by link {}", tp.getValue(), link.getValue());
            }
        }

        void addTp(final TerminationPoint tp, final LinkId link, final boolean isRemote) {
            final TpHolder h = tps.computeIfAbsent(tp.getTpId(), k -> new TpHolder(tp));
            h.addLink(link, isRemote);
        }

        void addPrefix(final Prefix pfx) {
            prefixes.put(pfx.key(), pfx);
        }

        void removePrefix(final PrefixCase prefixCase) {
            prefixes.remove(new PrefixKey(prefixCase.getPrefixDescriptors().getIpReachabilityInformation()));
        }

        void unadvertized() {
            inab = new IgpNodeAttributesBuilder();
            nb = new NodeBuilder().withKey(nb.key()).setNodeId(nb.getNodeId());
            advertized = false;
            LOG.debug("Node {} is unadvertized", nb.getNodeId().getValue());
        }

        void advertized(final NodeBuilder nodeBuilder, final IgpNodeAttributesBuilder igpNodeAttBuilder) {
            nb = requireNonNull(nodeBuilder);
            inab = requireNonNull(igpNodeAttBuilder);
            advertized = true;
            LOG.debug("Node {} is advertized", nodeBuilder.getNodeId().getValue());
        }

        NodeId getNodeId() {
            return nb.getNodeId();
        }

        NodeSrHolder getSrHolder() {
            return sr;
        }

        NodeSrHolder createSrHolderIfRequired() {
            if (sr == null) {
                sr = new NodeSrHolder(nb.getNodeId());
            }
            return sr;
        }
    }

    private final class NodeSrHolder {
        private final NodeId nodeId;
        private Long srgbFirstValue = null;
        private Integer srgbRangeSize = null;
        private final List<Segments> segments = new ArrayList<>();
        private final Map<IpPrefix, SrPrefix> srPrefixes = new HashMap<>();
        private final Map<IpPrefix, Segments> prefixSegments = new HashMap<>();
        private final Map<LinkId, Segments> adjSegments = new HashMap<>();

        NodeSrHolder(final NodeId nodeId) {
            this.nodeId = nodeId;
        }

        void addSrgb(final WriteTransaction trans, final boolean updateNode, final Long srgbFirstVal,
                final Integer srgbRangeSz) {
            srgbFirstValue = srgbFirstVal;
            srgbRangeSize = srgbRangeSz;
            srPrefixes.entrySet().forEach(entry -> {
                final IpPrefix ippfx = entry.getKey();
                final SrPrefix srPrefix = entry.getValue();
                final SidLabelIndex sidLabelIndex = srPrefix.getSidLabelIndex();
                if (sidLabelIndex instanceof SidCase) {
                    final Long sidIndex = ((SidCase) sidLabelIndex).getSid().longValue();
                    if (sidIndex >= srgbRangeSize) {
                        LOG.warn("Prefix SID index {} is outside the SRGB range of {} for node {}", sidIndex,
                                srgbRangeSize, nodeId.getValue());
                        return;
                    }
                    final Long prefixSid = srgbFirstValue + sidIndex;
                    final boolean isNodeSid = isAssociatedWithNodeSid(ippfx, srPrefix);
                    addPrefixSid(trans, updateNode, ippfx, prefixSid, isNodeSid);
                }
            });
        }

        void removeSrgb(final WriteTransaction trans) {
            srgbFirstValue = null;
            srgbRangeSize = null;
            srPrefixes.entrySet().forEach(entry -> {
                final IpPrefix ippfx = entry.getKey();
                final SrPrefix srPrefix = entry.getValue();
                final SidLabelIndex sidLabelIndex = srPrefix.getSidLabelIndex();
                if (sidLabelIndex instanceof SidCase) {
                    removePrefixSid(trans, false, ippfx);
                }
            });
        }

        void addSrPrefix(final WriteTransaction trans, final boolean updateNode, final IpPrefix ippfx,
                final SrPrefix srPrefix) {
            srPrefixes.put(ippfx, srPrefix);
            final SidLabelIndex sidLabelIndex = srPrefix.getSidLabelIndex();
            Long prefixSid = null;
            if (sidLabelIndex instanceof LocalLabelCase) {
                prefixSid = ((LocalLabelCase) sidLabelIndex).getLocalLabel().getValue().longValue();
            } else if (sidLabelIndex instanceof SidCase) {
                if (srgbFirstValue != null && srgbRangeSize != null) {
                    final Long sidIndex = ((SidCase) sidLabelIndex).getSid().longValue();
                    if (sidIndex >= srgbRangeSize) {
                        LOG.warn("Prefix SID index {} is outside the SRGB range of {} for node {}", sidIndex,
                                srgbRangeSize, nodeId.getValue());
                        return;
                    }
                    prefixSid = srgbFirstValue + sidIndex;
                }
            }
            if (prefixSid != null) {
                final boolean isNodeSid = isAssociatedWithNodeSid(ippfx, srPrefix);
                addPrefixSid(trans, updateNode, ippfx, prefixSid, isNodeSid);
            }
        }

        void removeSrPrefix(final WriteTransaction trans, final IpPrefix ippfx) {
            if (!srPrefixes.containsKey(ippfx)) {
                return;
            }
            removePrefixSid(trans, true, ippfx);
            srPrefixes.remove(ippfx);
        }

        void addPrefixSid(final WriteTransaction trans, final boolean updateNode, final IpPrefix ippfx,
                final Long prefixSid, final boolean isNodeSid) {
            LOG.trace("Adding prefix SID {} for prefix {} on node {}", prefixSid, ippfx.stringValue(),
                    nodeId.getValue());
            final SegmentId segmentId = new SegmentId(Uint32.valueOf(prefixSid));
            final Segments prefixSegment = new SegmentsBuilder()
                    .setSegmentId(segmentId)
                    .withKey(new SegmentsKey(segmentId))
                    .setSegmentSpecification(new PrefixCaseBuilder()
                        .setPrefix(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr
                            .rev130819.sr.node.attributes.segments.segment.specification.prefix._case.PrefixBuilder()
                                .setPrefix(ippfx).setNodeSid(isNodeSid ? isNodeSid : null).build())
                        .build())
                    .build();
            prefixSegments.put(ippfx, prefixSegment);
            segments.add(prefixSegment);
            addSegment(trans, updateNode, prefixSegment);
        }

        void removePrefixSid(final WriteTransaction trans, final boolean updateNode, final IpPrefix ippfx) {
            if (!prefixSegments.containsKey(ippfx)) {
                return;
            }
            LOG.trace("Removing prefix SID for prefix {} on node {}", ippfx.stringValue(),
                    nodeId.getValue());
            final Segments prefixSegment = prefixSegments.remove(ippfx);
            segments.remove(prefixSegment);
            removeSegment(trans, updateNode, prefixSegment);
        }

        void addAdjacencySid(final WriteTransaction trans, final boolean updateNode, final LinkId linkId,
                final Long adjSid) {
            LOG.trace("Adding adjacency SID {} for link {} on node {}", adjSid, linkId.getValue(),
                    nodeId.getValue());
            final SegmentId segmentId = new SegmentId(Uint32.valueOf(adjSid));
            final SegmentsBuilder sb = new SegmentsBuilder();
            sb.setSegmentId(segmentId);
            sb.withKey(new SegmentsKey(segmentId));
            sb.setSegmentSpecification(new AdjacencyCaseBuilder()
                    .setAdjacency(new AdjacencyBuilder().setAdjacency(linkId).build()).build());
            final Segments adjSegment = sb.build();
            adjSegments.put(linkId, adjSegment);
            segments.add(adjSegment);
            addSegment(trans, updateNode, adjSegment);
        }

        void removeAdjacencySid(final WriteTransaction trans, final LinkId linkId) {
            if (!adjSegments.containsKey(linkId)) {
                return;
            }
            LOG.trace("Removing adjacency SID for link {} on node {}", linkId.getValue(),
                    nodeId.getValue());
            final Segments adjSegment = adjSegments.remove(linkId);
            segments.remove(adjSegment);
            removeSegment(trans, true, adjSegment);
        }

        void addSegment(final WriteTransaction trans, final boolean updateNode, final Segments segment) {
            if (updateNode) {
                final InstanceIdentifier<Node> nodeIId = getNodeInstanceIdentifier(new NodeKey(nodeId));
                final InstanceIdentifier<Segments> segmentIId = nodeIId.builder()
                        .augmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr
                            .rev130819.Node1.class)
                        .child(Segments.class, segment.key()).build();
                trans.put(LogicalDatastoreType.OPERATIONAL, segmentIId, segment);
            }
            addSrAwareTopologyType(trans);
        }

        void removeSegment(final WriteTransaction trans, final boolean updateNode, final Segments segment) {
            if (updateNode) {
                final InstanceIdentifier<Node> nodeIId = getNodeInstanceIdentifier(new NodeKey(nodeId));
                final InstanceIdentifier<Segments> segmentIId = nodeIId.builder()
                        .augmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr
                            .rev130819.Node1.class)
                        .child(Segments.class, segment.key()).build();
                trans.delete(LogicalDatastoreType.OPERATIONAL, segmentIId);
            }
            removeSrAwareTopologyTypeIfRequired(trans);
        }

        boolean isAssociatedWithNodeSid(final IpPrefix ippfx, final SrPrefix srPrefix) {
            if (ippfx.getIpv4Prefix() != null && !ippfx.stringValue().endsWith("/32")
                    || ippfx.getIpv6Prefix() != null && !ippfx.stringValue().endsWith("/128")) {
                return false;
            }
            final Flags prefixFlags = srPrefix.getFlags();
            if (prefixFlags instanceof IsisPrefixFlagsCase) {
                return !Boolean.FALSE.equals(((IsisPrefixFlagsCase) prefixFlags).getIsisPrefixFlags().getNodeSid());
            }
            return true;
        }

        List<Segments> getSegments() {
            return segments;
        }

        int getSegmentCount() {
            return segments.size();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LinkstateTopologyBuilder.class);
    private final Map<NodeId, NodeHolder> nodes = new HashMap<>();
    private boolean srAwareTopologyTypeAdded;

    public LinkstateTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId) {
        super(dataProvider, locRibReference, topologyId, LINKSTATE_TOPOLOGY_TYPE, LinkstateAddressFamily.VALUE,
                LinkstateSubsequentAddressFamily.VALUE);
    }

    @VisibleForTesting
    LinkstateTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final long listenerResetLimitInMillsec,
            final int listenerResetEnforceCounter) {
        super(dataProvider, locRibReference, topologyId, LINKSTATE_TOPOLOGY_TYPE, LinkstateAddressFamily.VALUE,
                LinkstateSubsequentAddressFamily.VALUE,
                listenerResetLimitInMillsec, listenerResetEnforceCounter);
    }

    private static LinkId buildLinkId(final UriBuilder base, final LinkCase link) {
        return new LinkId(new UriBuilder(base, "link").add(link).toString());
    }

    private static NodeId buildNodeId(final UriBuilder base, final org.opendaylight.yang.gen.v1.urn.opendaylight
            .params.xml.ns.yang.bgp.linkstate.rev200120.NodeIdentifier node) {
        return new NodeId(new UriBuilder(base, "node").addPrefix("", node).toString());
    }

    private static TpId buildTpId(final UriBuilder base, final TopologyIdentifier topologyIdentifier,
            final Ipv4InterfaceIdentifier ipv4InterfaceIdentifier,
            final Ipv6InterfaceIdentifier ipv6InterfaceIdentifier, final Uint32 id) {
        final UriBuilder b = new UriBuilder(base, "tp");
        if (topologyIdentifier != null) {
            b.add("mt", topologyIdentifier.getValue());
        }
        if (ipv4InterfaceIdentifier != null) {
            b.add("ipv4", ipv4InterfaceIdentifier.getValue());
        }
        if (ipv6InterfaceIdentifier != null) {
            b.add("ipv6", ipv6InterfaceIdentifier.getValue());
        }

        return new TpId(b.add("id", id).toString());
    }

    private static TpId buildLocalTpId(final UriBuilder base, final LinkDescriptors linkDescriptors) {
        return buildTpId(base, linkDescriptors.getMultiTopologyId(), linkDescriptors.getIpv4InterfaceAddress(),
                linkDescriptors.getIpv6InterfaceAddress(), linkDescriptors.getLinkLocalIdentifier());
    }

    private static TerminationPoint buildTp(final TpId id, final TerminationPointType type) {
        final TerminationPointBuilder stpb = new TerminationPointBuilder();
        stpb.withKey(new TerminationPointKey(id));
        stpb.setTpId(id);

        if (type != null) {
            stpb.addAugmentation(new TerminationPoint1Builder()
                    .setIgpTerminationPointAttributes(new IgpTerminationPointAttributesBuilder()
                        .setTerminationPointType(type)
                        .build())
                    .build());
        }

        return stpb.build();
    }

    private static TerminationPointType getTpType(final Ipv4InterfaceIdentifier ipv4InterfaceIdentifier,
            final Ipv6InterfaceIdentifier ipv6InterfaceIdentifier, final Uint32 id) {
        // Order of preference: Unnumbered first, then IP
        if (id != null) {
            LOG.debug("Unnumbered termination point type: {}", id);
            return new UnnumberedBuilder().setUnnumberedId(id).build();
        }

        final IpAddress ip;
        if (ipv6InterfaceIdentifier != null) {
            ip = new IpAddress(ipv6InterfaceIdentifier);
        } else if (ipv4InterfaceIdentifier != null) {
            ip = new IpAddress(ipv4InterfaceIdentifier);
        } else {
            ip = null;
        }

        if (ip != null) {
            LOG.debug("IP termination point type: {}", ip);
            return new IpBuilder().setIpAddress(Set.of(ip)).build();
        }

        return null;
    }

    private static TerminationPoint buildLocalTp(final UriBuilder base, final LinkDescriptors linkDescriptors) {
        final TpId id = buildLocalTpId(base, linkDescriptors);
        final TerminationPointType t = getTpType(linkDescriptors.getIpv4InterfaceAddress(),
                linkDescriptors.getIpv6InterfaceAddress(),
                linkDescriptors.getLinkLocalIdentifier());

        return buildTp(id, t);
    }

    private static TpId buildRemoteTpId(final UriBuilder base, final LinkDescriptors linkDescriptors) {
        return buildTpId(base, linkDescriptors.getMultiTopologyId(), linkDescriptors.getIpv4NeighborAddress(),
                linkDescriptors.getIpv6NeighborAddress(), linkDescriptors.getLinkRemoteIdentifier());
    }

    private static TerminationPoint buildRemoteTp(final UriBuilder base, final LinkDescriptors linkDescriptors) {
        final TpId id = buildRemoteTpId(base, linkDescriptors);
        final TerminationPointType t = getTpType(linkDescriptors.getIpv4NeighborAddress(),
                linkDescriptors.getIpv6NeighborAddress(),
                linkDescriptors.getLinkRemoteIdentifier());

        return buildTp(id, t);
    }

    private InstanceIdentifier<Link> buildLinkIdentifier(final LinkId id) {
        return getInstanceIdentifier().child(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology
                        .topology.Link.class, new LinkKey(id));
    }

    private NodeHolder getNode(final NodeId id) {
        return nodes.computeIfAbsent(id, NodeHolder::new);
    }

    private void putNode(final WriteTransaction trans, final NodeHolder holder) {
        if (holder.syncState(trans)) {
            nodes.remove(holder.getNodeId());
        }
    }

    private void checkNodeForRemoval(final WriteTransaction trans, final NodeHolder holder) {
        if (holder.checkForRemoval(trans)) {
            nodes.remove(holder.getNodeId());
        }
    }

    private void createLink(final WriteTransaction trans, final UriBuilder base,
            final LinkstateRoute value, final LinkCase linkCase, final Attributes attributes) {
        // defensive lookup
        final LinkAttributes la;
        final Attributes1 attr = attributes.augmentation(Attributes1.class);
        if (attr != null) {
            final LinkStateAttribute attrType = attr.getLinkStateAttribute();
            if (attrType != null) {
                la = ((LinkAttributesCase)attrType).getLinkAttributes();
            } else {
                LOG.debug("Missing attribute type in link {} route {}, skipping it", linkCase, value);
                la = null;
            }
        } else {
            LOG.debug("Missing attributes in link {} route {}, skipping it", linkCase, value);
            la = null;
        }

        final IgpLinkAttributesBuilder ilab = new IgpLinkAttributesBuilder();
        Long adjSid = null;
        if (la != null) {
            if (la.getMetric() != null) {
                ilab.setMetric(la.getMetric().getValue());
            }
            ilab.setName(la.getLinkName());
            if (la.getSrAdjIds() != null && !la.getSrAdjIds().isEmpty()) {
                final SrAdjIds srAdjIds = la.getSrAdjIds().get(0);
                if (srAdjIds != null) {
                    final SidLabelIndex sidLabelIndex = srAdjIds.getSidLabelIndex();
                    if (sidLabelIndex instanceof LocalLabelCase) {
                        adjSid = ((LocalLabelCase) sidLabelIndex).getLocalLabel().getValue().longValue();
                    }
                }
            }
        }
        ProtocolUtil.augmentProtocolId(value, ilab, la, linkCase.getLinkDescriptors());

        final LinkBuilder lb = new LinkBuilder()
                .setLinkId(buildLinkId(base, linkCase))
                .addAugmentation(new Link1Builder().setIgpLinkAttributes(ilab.build()).build());

        final NodeId srcNode = buildNodeId(base, linkCase.getLocalNodeDescriptors());
        LOG.trace("Link {} implies source node {}", linkCase, srcNode);

        final NodeId dstNode = buildNodeId(base, linkCase.getRemoteNodeDescriptors());
        LOG.trace("Link {} implies destination node {}", linkCase, dstNode);

        final TerminationPoint srcTp = buildLocalTp(base, linkCase.getLinkDescriptors());
        LOG.trace("Link {} implies source TP {}", linkCase, srcTp);

        final TerminationPoint dstTp = buildRemoteTp(base, linkCase.getLinkDescriptors());
        LOG.trace("Link {} implies destination TP {}", linkCase, dstTp);

        lb.setSource(new SourceBuilder().setSourceNode(srcNode).setSourceTp(srcTp.getTpId()).build());
        lb.setDestination(new DestinationBuilder().setDestNode(dstNode).setDestTp(dstTp.getTpId()).build());

        LOG.trace("Created TP {} as link source", srcTp);
        NodeHolder snh = nodes.get(srcNode);
        if (snh == null) {
            snh = getNode(srcNode);
            snh.addTp(srcTp, lb.getLinkId(), false);
            if (adjSid != null) {
                snh.createSrHolderIfRequired().addAdjacencySid(trans, false, lb.getLinkId(), adjSid);
            }
            putNode(trans, snh);
        } else {
            snh.addTp(srcTp, lb.getLinkId(), false);
            if (adjSid != null) {
                snh.createSrHolderIfRequired().addAdjacencySid(trans, true, lb.getLinkId(), adjSid);
            }
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(new NodeKey(snh.getNodeId()));
            trans.put(LogicalDatastoreType.OPERATIONAL, nid.child(TerminationPoint.class, srcTp.key()), srcTp);
        }
        if (adjSid != null) {
            lb.addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.sr
                .rev130819.Link1Builder().setSegment(new SegmentId(Uint32.valueOf(adjSid))).build());
        }

        LOG.debug("Created TP {} as link destination", dstTp);
        NodeHolder dnh = nodes.get(dstNode);
        if (dnh == null) {
            dnh = getNode(dstNode);
            dnh.addTp(dstTp, lb.getLinkId(), true);
            putNode(trans, dnh);
        } else {
            dnh.addTp(dstTp, lb.getLinkId(), true);
            final InstanceIdentifier<Node> nid = getInstanceIdentifier().child(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network
                            .topology.topology.Node.class, new NodeKey(dnh.getNodeId()));
            trans.put(LogicalDatastoreType.OPERATIONAL, nid.child(TerminationPoint.class, dstTp.key()), dstTp);
        }

        final InstanceIdentifier<Link> lid = buildLinkIdentifier(lb.getLinkId());
        final Link link = lb.build();

        trans.put(LogicalDatastoreType.OPERATIONAL, lid, link);
        LOG.debug("Created link {} at {} for {}", link, lid, linkCase);
    }

    private void removeTp(final WriteTransaction trans, final NodeId node, final TpId tp,
            final LinkId link, final boolean isRemote) {
        final NodeHolder nh = nodes.get(node);
        if (nh != null) {
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(new NodeKey(nh.getNodeId()));
            trans.delete(LogicalDatastoreType.OPERATIONAL, nid.child(TerminationPoint.class,
                    new TerminationPointKey(tp)));
            nh.removeTp(tp, link, isRemote);
            if (!isRemote) {
                nh.createSrHolderIfRequired().removeAdjacencySid(trans, link);
            }
            checkNodeForRemoval(trans, nh);
        } else {
            LOG.warn("Removed non-existent node {}", node.getValue());
        }
    }

    private void removeLink(final WriteTransaction trans, final UriBuilder base, final LinkCase linkCase) {
        final LinkId id = buildLinkId(base, linkCase);
        final InstanceIdentifier<?> lid = buildLinkIdentifier(id);
        trans.delete(LogicalDatastoreType.OPERATIONAL, lid);
        LOG.debug("Removed link {}", lid);

        removeTp(trans, buildNodeId(base, linkCase.getLocalNodeDescriptors()),
                buildLocalTpId(base, linkCase.getLinkDescriptors()), id, false);
        removeTp(trans, buildNodeId(base, linkCase.getRemoteNodeDescriptors()),
                buildRemoteTpId(base, linkCase.getLinkDescriptors()), id, true);
    }

    private void createNode(final WriteTransaction trans, final UriBuilder base,
            final LinkstateRoute value, final NodeCase nodeCase, final Attributes attributes) {
        final NodeAttributes na;
        //defensive lookup
        final Attributes1 attr = attributes.augmentation(Attributes1.class);
        if (attr != null) {
            final LinkStateAttribute attrType = attr.getLinkStateAttribute();
            if (attrType != null) {
                na = ((NodeAttributesCase)attrType).getNodeAttributes();
            } else {
                LOG.debug("Missing attribute type in node {} route {}, skipping it", nodeCase, value);
                na = null;
            }
        } else {
            LOG.debug("Missing attributes in node {} route {}, skipping it", nodeCase, value);
            na = null;
        }
        final IgpNodeAttributesBuilder inab = new IgpNodeAttributesBuilder();
        final var idsBuilder = ImmutableSet.<IpAddress>builder();
        Long srgbFirstValue = null;
        Integer srgbRangeSize = null;
        if (na != null) {
            if (na.getIpv4RouterId() != null) {
                idsBuilder.add(new IpAddress(na.getIpv4RouterId()));
            }
            if (na.getIpv6RouterId() != null) {
                idsBuilder.add(new IpAddress(na.getIpv6RouterId()));
            }
            if (na.getDynamicHostname() != null) {
                inab.setName(new DomainName(na.getDynamicHostname()));
            }
            if (na.getSrCapabilities() != null) {
                final SidLabelIndex sidLabelIndex = na.getSrCapabilities().getSidLabelIndex();
                if (sidLabelIndex instanceof LocalLabelCase) {
                    srgbFirstValue = ((LocalLabelCase) sidLabelIndex).getLocalLabel().getValue().longValue();
                }
                srgbRangeSize = na.getSrCapabilities().getRangeSize() != null
                        ? na.getSrCapabilities().getRangeSize().getValue().intValue()
                        : null;
            }
        }
        final var ids = idsBuilder.build();
        if (!ids.isEmpty()) {
            inab.setRouterId(ids);
        }
        ProtocolUtil.augmentProtocolId(value, inab, na, nodeCase.getNodeDescriptors());

        final NodeId nid = buildNodeId(base, nodeCase.getNodeDescriptors());
        final NodeHolder nh = getNode(nid);
        /*
         *  Eventhough the the holder creates a dummy structure, we need to duplicate it here,
         *  as that is the API requirement. The reason for it is the possible presence of supporting
         *  node -- something which the holder does not track.
         */
        final NodeBuilder nb = new NodeBuilder();
        nb.setNodeId(nid);
        nb.withKey(new NodeKey(nb.getNodeId()));

        nh.advertized(nb, inab);
        if (srgbFirstValue != null && srgbRangeSize != null) {
            nh.createSrHolderIfRequired().addSrgb(trans, false, srgbFirstValue, srgbRangeSize);
        }
        putNode(trans, nh);
    }

    private void removeNode(final WriteTransaction trans, final UriBuilder base, final NodeCase nodeCase) {
        final NodeId id = buildNodeId(base, nodeCase.getNodeDescriptors());
        final NodeHolder nh = nodes.get(id);
        if (nh != null) {
            nh.unadvertized();
            nh.createSrHolderIfRequired().removeSrgb(trans);
            putNode(trans, nh);
        } else {
            LOG.warn("Node {} does not have a holder", id.getValue());
        }
    }

    private void createPrefix(final WriteTransaction trans, final UriBuilder base,
            final LinkstateRoute value, final PrefixCase prefixCase, final Attributes attributes) {
        final IpPrefix ippfx = prefixCase.getPrefixDescriptors().getIpReachabilityInformation();
        if (ippfx == null) {
            LOG.warn("IP reachability not present in prefix {} route {}, skipping it", prefixCase, value);
            return;
        }
        final PrefixBuilder pb = new PrefixBuilder();
        final PrefixKey pk = new PrefixKey(ippfx);
        pb.withKey(pk);
        pb.setPrefix(ippfx);

        final PrefixAttributes pa;
        // Very defensive lookup
        final Attributes1 attr = attributes.augmentation(Attributes1.class);
        if (attr != null) {
            final LinkStateAttribute attrType = attr.getLinkStateAttribute();
            if (attrType != null) {
                pa = ((PrefixAttributesCase)attrType).getPrefixAttributes();
            } else {
                LOG.debug("Missing attribute type in IP {} prefix {} route {}, skipping it", ippfx, prefixCase, value);
                pa = null;
            }
        } else {
            LOG.debug("Missing attributes in IP {} prefix {} route {}, skipping it", ippfx, prefixCase, value);
            pa = null;
        }
        SrPrefix srPrefix = null;
        if (pa != null) {
            if (pa.getPrefixMetric() != null) {
                pb.setMetric(pa.getPrefixMetric().getValue());
            }
            if (pa.getSrPrefix() != null) {
                srPrefix = pa.getSrPrefix();
            }
        }
        ProtocolUtil.augmentProtocolId(value, pa, pb);

        final Prefix pfx = pb.build();
        LOG.debug("Created prefix {} for {}", pfx, prefixCase);

        /*
         * All set, but... the hosting node may not exist, we may need to fake it.
         */
        final NodeId node = buildNodeId(base, prefixCase.getAdvertisingNodeDescriptors());
        NodeHolder nh = nodes.get(node);
        if (nh == null) {
            nh = getNode(node);
            nh.addPrefix(pfx);
            if (srPrefix != null) {
                nh.createSrHolderIfRequired().addSrPrefix(trans, false, ippfx, srPrefix);
            }
            putNode(trans, nh);
        } else {
            nh.addPrefix(pfx);
            if (srPrefix != null) {
                nh.createSrHolderIfRequired().addSrPrefix(trans, true, ippfx, srPrefix);
            }
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(new NodeKey(nh.getNodeId()));
            final InstanceIdentifier<IgpNodeAttributes> inaId = nid.builder().augmentation(Node1.class)
                    .child(IgpNodeAttributes.class).build();
            trans.put(LogicalDatastoreType.OPERATIONAL, inaId.child(Prefix.class, pk), pfx);
        }
    }

    private void removePrefix(final WriteTransaction trans, final UriBuilder base, final PrefixCase prefixCase) {
        final NodeId node = buildNodeId(base, prefixCase.getAdvertisingNodeDescriptors());
        final NodeHolder nh = nodes.get(node);
        if (nh != null) {
            LOG.debug("Removed prefix {}", prefixCase);
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(new NodeKey(nh.getNodeId()));
            final InstanceIdentifier<IgpNodeAttributes> inaId = nid.builder().augmentation(Node1.class)
                    .child(IgpNodeAttributes.class).build();
            final IpPrefix ippfx = prefixCase.getPrefixDescriptors().getIpReachabilityInformation();
            if (ippfx == null) {
                LOG.warn("IP reachability not present in prefix {}, skipping it", prefixCase);
                return;
            }
            final PrefixKey pk = new PrefixKey(ippfx);
            trans.delete(LogicalDatastoreType.OPERATIONAL, inaId.child(Prefix.class, pk));
            nh.removePrefix(prefixCase);
            nh.createSrHolderIfRequired().removeSrPrefix(trans, ippfx);
            checkNodeForRemoval(trans, nh);
        } else {
            LOG.warn("Removing prefix from non-existing node {}", node.getValue());
        }
    }

    private InstanceIdentifier<Node> getNodeInstanceIdentifier(final NodeKey nodeKey) {
        return getInstanceIdentifier().child(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network
                        .topology.topology.Node.class, nodeKey);
    }

    protected void addSrAwareTopologyType(final WriteTransaction trans) {
        if (srAwareTopologyTypeAdded) {
            return;
        }
        LOG.debug("Adding SR-aware topology-type for topology {}",
                getInstanceIdentifier().firstKeyOf(Topology.class).getTopologyId().getValue());
        trans.put(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier().child(TopologyTypes.class),
                SR_AWARE_LINKSTATE_TOPOLOGY_TYPE);
        srAwareTopologyTypeAdded = true;
    }

    protected void removeSrAwareTopologyTypeIfRequired(final WriteTransaction trans) {
        if (!srAwareTopologyTypeAdded) {
            return;
        }
        final boolean isSidPresent = nodes.values().stream().filter(nh -> nh.getSrHolder() != null)
                .map(nh -> nh.getSrHolder().getSegmentCount()).anyMatch(cnt -> cnt != 0);
        if (isSidPresent) {
            return;
        }
        LOG.debug("Removing SR-aware topology-type from topology {}",
                getInstanceIdentifier().firstKeyOf(Topology.class).getTopologyId().getValue());
        trans.put(LogicalDatastoreType.OPERATIONAL, getInstanceIdentifier().child(TopologyTypes.class),
                LINKSTATE_TOPOLOGY_TYPE);
        srAwareTopologyTypeAdded = false;
    }

    @Override
    protected void createObject(final ReadWriteTransaction trans,
            final InstanceIdentifier<LinkstateRoute> id, final LinkstateRoute value) {
        final UriBuilder base = new UriBuilder(value);

        final ObjectType t = value.getObjectType();
        Preconditions.checkArgument(t != null, "Route %s value %s has null object type", id, value);

        if (t instanceof LinkCase) {
            createLink(trans, base, value, (LinkCase) t, value.getAttributes());
        } else if (t instanceof NodeCase) {
            createNode(trans, base, value, (NodeCase) t, value.getAttributes());
        } else if (t instanceof PrefixCase) {
            createPrefix(trans, base, value, (PrefixCase) t, value.getAttributes());
        } else {
            LOG.debug(UNHANDLED_OBJECT_CLASS, t.implementedInterface());
        }
    }

    @Override
    protected void removeObject(final ReadWriteTransaction trans,
            final InstanceIdentifier<LinkstateRoute> id, final LinkstateRoute value) {
        if (value == null) {
            LOG.error("Empty before-data received in delete data change notification for instance id {}", id);
            return;
        }

        final UriBuilder base = new UriBuilder(value);

        final ObjectType t = value.getObjectType();
        if (t instanceof LinkCase) {
            removeLink(trans, base, (LinkCase) t);
        } else if (t instanceof NodeCase) {
            removeNode(trans, base, (NodeCase) t);
        } else if (t instanceof PrefixCase) {
            removePrefix(trans, base, (PrefixCase) t);
        } else {
            LOG.debug(UNHANDLED_OBJECT_CLASS, t.implementedInterface());
        }
    }

    @Override
    protected InstanceIdentifier<LinkstateRoute> getRouteWildcard(final InstanceIdentifier<Tables> tablesId) {
        return tablesId.child(LinkstateRoutesCase.class, LinkstateRoutes.class).child(LinkstateRoute.class);
    }

    @Override
    protected void clearTopology() {
        nodes.clear();
        srAwareTopologyTypeAdded = false;
    }
}
