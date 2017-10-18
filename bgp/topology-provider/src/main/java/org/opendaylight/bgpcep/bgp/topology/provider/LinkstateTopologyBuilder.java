/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.IsisAreaIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeFlagBits;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.TopologyIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.routes.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.IsisPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.OspfPseudonodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.node._case.IsisNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.isis.pseudonode._case.IsisPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.node.identifier.c.router.identifier.ospf.pseudonode._case.OspfPseudonode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.bgp.linkstate.topology.type.BgpLinkstateTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoNetId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoPseudonodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IsoSystemId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.IsisLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.IsisNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.IsoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.srlg.attributes.SrlgValues;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.srlg.attributes.SrlgValuesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.SrlgBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.ted.rev131021.ted.link.attributes.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.nt.l3.unicast.igp.topology.rev131021.TerminationPoint1;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.Prefix1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.Prefix1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.OspfLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.OspfNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.AbrBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.InternalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.router.type.PseudonodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.prefix.attributes.OspfPrefixAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkstateTopologyBuilder extends AbstractTopologyBuilder<LinkstateRoute> {
    public static final TopologyTypes LINKSTATE_TOPOLOGY_TYPE = new TopologyTypesBuilder()
    .addAugmentation(TopologyTypes1.class,
            new TopologyTypes1Builder()
    .setBgpLinkstateTopology(new BgpLinkstateTopologyBuilder().build()).build())
    .build();

    private static final String UNHANDLED_OBJECT_CLASS = "Unhandled object class {}";

    private static final class TpHolder {
        private final Set<LinkId> local = new HashSet<>();
        private final Set<LinkId> remote = new HashSet<>();

        private final TerminationPoint tp;

        private TpHolder(final TerminationPoint tp) {
            this.tp = requireNonNull(tp);
        }

        private synchronized void addLink(final LinkId id, final boolean isRemote) {
            if (isRemote) {
                this.remote.add(id);
            } else {
                this.local.add(id);
            }
        }

        private synchronized boolean removeLink(final LinkId id, final boolean isRemote) {
            final boolean removed;
            if (isRemote) {
                removed = this.remote.remove(id);
            } else {
                removed = this.local.remove(id);
            }
            if (!removed) {
                LOG.warn("Removed non-reference link {} from TP {} isRemote {}", this.tp.getTpId(), id, isRemote);
            }

            return this.local.isEmpty() && this.remote.isEmpty();
        }

        private TerminationPoint getTp() {
            return this.tp;
        }
    }

    private final class NodeHolder {
        private final Map<PrefixKey, Prefix> prefixes = new HashMap<>();
        private final Map<TpId, TpHolder> tps = new HashMap<>();
        private boolean advertized = false;
        private IgpNodeAttributesBuilder inab;
        private NodeBuilder nb;

        private NodeHolder(final NodeId id) {
            this.inab = new IgpNodeAttributesBuilder();
            this.nb = new NodeBuilder().setKey(new NodeKey(id)).setNodeId(id);
        }

        /**
         * Synchronized in-core state of a node into the backing store using the transaction
         *
         * @param trans data modification transaction which to use
         * @return True if the node has been purged, false otherwise.
         */
        private boolean syncState(final WriteTransaction trans) {
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(this.nb.getKey());

            /*
             * Transaction's putOperationalData() does a merge. Force it onto a replace
             * by removing the data. If we decide to remove the node -- we just skip the put.
             */
            trans.delete(LogicalDatastoreType.OPERATIONAL, nid);

            if (!this.advertized) {
                if (this.tps.isEmpty() && this.prefixes.isEmpty()) {
                    LOG.trace("Removing unadvertized unused node {}", this.nb.getNodeId());
                    return true;
                }

                LOG.trace("Node {} is still implied by {} TPs and {} prefixes", this.nb.getNodeId(), this.tps.size(), this.prefixes.size());
            }

            // Re-generate termination points
            this.nb.setTerminationPoint(Lists.newArrayList(Collections2.transform(this.tps.values(), TpHolder::getTp)));

            // Re-generate prefixes
            this.inab.setPrefix(Lists.newArrayList(this.prefixes.values()));

            // Write the node out
            final Node n = this.nb.addAugmentation(Node1.class, new Node1Builder().setIgpNodeAttributes(this.inab.build()).build()).build();
            trans.put(LogicalDatastoreType.OPERATIONAL, nid, n);
            LOG.trace("Created node {} at {}", n, nid);
            return false;
        }

        private boolean checkForRemoval(final WriteTransaction trans) {
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(this.nb.getKey());

                if (!this.advertized) {
                    if (this.tps.isEmpty() && this.prefixes.isEmpty()) {
                        trans.delete(LogicalDatastoreType.OPERATIONAL, nid);
                        LOG.trace("Removing unadvertized unused node {}", this.nb.getNodeId());
                        return true;
                    }

                    LOG.trace("Node {} is still implied by {} TPs and {} prefixes", this.nb.getNodeId(), this.tps.size(), this.prefixes.size());
                }
                return false;
        }

        private synchronized void removeTp(final TpId tp, final LinkId link, final boolean isRemote) {
            final TpHolder h = this.tps.get(tp);
            if (h != null) {
                if (h.removeLink(link, isRemote)) {
                    this.tps.remove(tp);
                    LOG.trace("Removed TP {}", tp);
                }
            } else {
                LOG.warn("Removed non-present TP {} by link {}", tp, link);
            }
        }

        private void addTp(final TerminationPoint tp, final LinkId link, final boolean isRemote) {
            final TpHolder h = this.tps.computeIfAbsent(tp.getTpId(), k -> new TpHolder(tp));
            h.addLink(link, isRemote);
        }

        private void addPrefix(final Prefix pfx) {
            this.prefixes.put(pfx.getKey(), pfx);
        }

        private void removePrefix(final PrefixCase p) {
            this.prefixes.remove(new PrefixKey(p.getPrefixDescriptors().getIpReachabilityInformation()));
        }

        private void unadvertized() {
            this.inab = new IgpNodeAttributesBuilder();
            this.nb = new NodeBuilder().setKey(this.nb.getKey()).setNodeId(this.nb.getNodeId());
            this.advertized = false;
            LOG.debug("Node {} is unadvertized", this.nb.getNodeId());
        }

        private void advertized(final NodeBuilder nb, final IgpNodeAttributesBuilder inab) {
            this.nb = requireNonNull(nb);
            this.inab = requireNonNull(inab);
            this.advertized = true;
            LOG.debug("Node {} is advertized", nb.getNodeId());
        }

        private NodeId getNodeId() {
            return this.nb.getNodeId();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LinkstateTopologyBuilder.class);
    private final Map<NodeId, NodeHolder> nodes = new HashMap<>();

    public LinkstateTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference, final TopologyId topologyId) {
        super(dataProvider, locRibReference, topologyId, LINKSTATE_TOPOLOGY_TYPE, LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class);
    }

    public LinkstateTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final long listenerResetLimitInMillsec, final int listenerResetEnforceCounter) {
        super(dataProvider, locRibReference, topologyId, LINKSTATE_TOPOLOGY_TYPE, LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
            listenerResetLimitInMillsec, listenerResetEnforceCounter);
    }

    private static LinkId buildLinkId(final UriBuilder base, final LinkCase link) {
        return new LinkId(new UriBuilder(base, "link").add(link).toString());
    }

    private static NodeId buildNodeId(final UriBuilder base,
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier node) {
        return new NodeId(new UriBuilder(base, "node").add("", node).toString());
    }

    private static TpId buildTpId(final UriBuilder base, final TopologyIdentifier topologyIdentifier,
            final Ipv4InterfaceIdentifier ipv4InterfaceIdentifier, final Ipv6InterfaceIdentifier ipv6InterfaceIdentifier, final Long id) {
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
        stpb.setKey(new TerminationPointKey(id));
        stpb.setTpId(id);

        if (type != null) {
            stpb.addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder().setIgpTerminationPointAttributes(
                    new IgpTerminationPointAttributesBuilder().setTerminationPointType(type).build()).build());
        }

        return stpb.build();
    }

    private static TerminationPointType getTpType(final Ipv4InterfaceIdentifier ipv4InterfaceIdentifier,
            final Ipv6InterfaceIdentifier ipv6InterfaceIdentifier, final Long id) {
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
            return new IpBuilder().setIpAddress(Lists.newArrayList(ip)).build();
        }

        return null;
    }

    private static TerminationPoint buildLocalTp(final UriBuilder base, final LinkDescriptors linkDescriptors) {
        final TpId id = buildLocalTpId(base, linkDescriptors);
        final TerminationPointType t = getTpType(linkDescriptors.getIpv4InterfaceAddress(), linkDescriptors.getIpv6InterfaceAddress(),
                linkDescriptors.getLinkLocalIdentifier());

        return buildTp(id, t);
    }

    private static TpId buildRemoteTpId(final UriBuilder base, final LinkDescriptors linkDescriptors) {
        return buildTpId(base, linkDescriptors.getMultiTopologyId(), linkDescriptors.getIpv4NeighborAddress(),
                linkDescriptors.getIpv6NeighborAddress(), linkDescriptors.getLinkRemoteIdentifier());
    }

    private static TerminationPoint buildRemoteTp(final UriBuilder base, final LinkDescriptors linkDescriptors) {
        final TpId id = buildRemoteTpId(base, linkDescriptors);
        final TerminationPointType t = getTpType(linkDescriptors.getIpv4NeighborAddress(), linkDescriptors.getIpv6NeighborAddress(),
                linkDescriptors.getLinkRemoteIdentifier());

        return buildTp(id, t);
    }

    private InstanceIdentifier<Link> buildLinkIdentifier(final LinkId id) {
        return getInstanceIdentifier().child(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link.class,
                new LinkKey(id));
    }

    private static Float bandwidthToFloat(final Bandwidth bandwidth) {
        return ByteBuffer.wrap(bandwidth.getValue()).getFloat();
    }

    private static BigDecimal bandwidthToBigDecimal(final Bandwidth bandwidth) {
        return BigDecimal.valueOf(bandwidthToFloat(bandwidth));
    }

    private static List<UnreservedBandwidth> unreservedBandwidthList(
            final List<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.UnreservedBandwidth> input) {
        final List<UnreservedBandwidth> ret = new ArrayList<>(input.size());

        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.UnreservedBandwidth i : input) {
            ret.add(new UnreservedBandwidthBuilder().setBandwidth(bandwidthToBigDecimal(i.getBandwidth())).setKey(
                    new UnreservedBandwidthKey(i.getPriority())).build());
        }

        return ret;
    }

    private static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1 isisLinkAttributes(
            final TopologyIdentifier topologyIdentifier, final LinkAttributes la) {
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.link.attributes.isis.link.attributes.TedBuilder();

        if (la != null) {
            if (la.getAdminGroup() != null) {
                tb.setColor(la.getAdminGroup().getValue());
            }
            if (la.getTeMetric() != null) {
                tb.setTeDefaultMetric(la.getTeMetric().getValue());
            }
            if (la.getUnreservedBandwidth() != null) {
                tb.setUnreservedBandwidth(unreservedBandwidthList(la.getUnreservedBandwidth()));
            }
            if (la.getMaxLinkBandwidth() != null) {
                tb.setMaxLinkBandwidth(bandwidthToBigDecimal(la.getMaxLinkBandwidth()));
            }
            if (la.getMaxReservableBandwidth() != null) {
                tb.setMaxResvLinkBandwidth(bandwidthToBigDecimal(la.getMaxReservableBandwidth()));
            }
            if (la.getSharedRiskLinkGroups() != null) {
                final List<SrlgValues> srlgs = new ArrayList<>();
                for (final SrlgId id : la.getSharedRiskLinkGroups()) {
                    srlgs.add(new SrlgValuesBuilder().setSrlgValue(id.getValue()).build());
                }
                tb.setSrlg(new SrlgBuilder().setSrlgValues(srlgs).build());
            }
        }

        final IsisLinkAttributesBuilder ilab = new IsisLinkAttributesBuilder();
        ilab.setTed(tb.build());
        if (topologyIdentifier != null) {
            ilab.setMultiTopologyId(topologyIdentifier.getValue().shortValue());
        }

        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1Builder().setIsisLinkAttributes(
                ilab.build()).build();
    }

    private static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1 ospfLinkAttributes(
            final TopologyIdentifier topologyIdentifier, final LinkAttributes la) {
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.ospf.link.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.link.attributes.ospf.link.attributes.TedBuilder();

        if (la != null) {
            if (la.getAdminGroup() != null) {
                tb.setColor(la.getAdminGroup().getValue());
            }
            if (la.getTeMetric() != null) {
                tb.setTeDefaultMetric(la.getTeMetric().getValue());
            }
            if (la.getUnreservedBandwidth() != null) {
                tb.setUnreservedBandwidth(unreservedBandwidthList(la.getUnreservedBandwidth()));
            }
            if (la.getMaxLinkBandwidth() != null) {
                tb.setMaxLinkBandwidth(bandwidthToBigDecimal(la.getMaxLinkBandwidth()));
            }
            if (la.getMaxReservableBandwidth() != null) {
                tb.setMaxResvLinkBandwidth(bandwidthToBigDecimal(la.getMaxReservableBandwidth()));
            }
            if (la.getSharedRiskLinkGroups() != null) {
                final List<SrlgValues> srlgs = new ArrayList<>();
                for (final SrlgId id : la.getSharedRiskLinkGroups()) {
                    srlgs.add(new SrlgValuesBuilder().setSrlgValue(id.getValue()).build());
                }
                tb.setSrlg(new SrlgBuilder().setSrlgValues(srlgs).build());
            }
        }

        final OspfLinkAttributesBuilder ilab = new OspfLinkAttributesBuilder();
        ilab.setTed(tb.build());
        if (topologyIdentifier != null) {
            ilab.setMultiTopologyId(topologyIdentifier.getValue().shortValue());
        }

        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1Builder().setOspfLinkAttributes(
                ilab.build()).build();
    }

    private NodeHolder getNode(final NodeId id) {
        if (this.nodes.containsKey(id)) {
            LOG.debug("Node {} is already present", id);
            return this.nodes.get(id);
        }

        final NodeHolder ret = new NodeHolder(id);
        this.nodes.put(id, ret);
        return ret;
    }

    private void putNode(final WriteTransaction trans, final NodeHolder holder) {
        if (holder.syncState(trans)) {
            this.nodes.remove(holder.getNodeId());
        }
    }

    private void checkNodeForRemoval(final WriteTransaction trans, final NodeHolder holder) {
        if (holder.checkForRemoval(trans)) {
            this.nodes.remove(holder.getNodeId());
        }
    }

    private static void augmentProtocolId(final LinkstateRoute value, final IgpLinkAttributesBuilder ilab,
            final LinkAttributes la, final LinkDescriptors ld) {
        switch (value.getProtocolId()) {
        case Direct:
        case Static:
        case IsisLevel1:
        case IsisLevel2:
            ilab.addAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpLinkAttributes1.class,
                    isisLinkAttributes(ld.getMultiTopologyId(), la));
            break;
        case OspfV3:
        case Ospf:
            ilab.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpLinkAttributes1.class,
                    ospfLinkAttributes(ld.getMultiTopologyId(), la));
            break;
        default:
            break;
        }
    }

    private void createLink(final WriteTransaction trans, final UriBuilder base,
            final LinkstateRoute value, final LinkCase l, final Attributes attributes) {
        // defensive lookup
        final LinkAttributes la;
        final Attributes1 attr = attributes.getAugmentation(Attributes1.class);
        if (attr != null) {
            final LinkStateAttribute attrType = attr.getLinkStateAttribute();
            if (attrType != null) {
                la = ((LinkAttributesCase)attrType).getLinkAttributes();
            } else {
                LOG.debug("Missing attribute type in link {} route {}, skipping it", l, value);
                la = null;
            }
        } else {
            LOG.debug("Missing attributes in link {} route {}, skipping it", l, value);
            la = null;
        }

        final IgpLinkAttributesBuilder ilab = new IgpLinkAttributesBuilder();
        if (la != null) {
            if (la.getMetric() != null) {
                ilab.setMetric(la.getMetric().getValue());
            }
            ilab.setName(la.getLinkName());
        }
        augmentProtocolId(value, ilab, la, l.getLinkDescriptors());

        final LinkBuilder lb = new LinkBuilder();
        lb.setLinkId(buildLinkId(base, l));
        lb.addAugmentation(Link1.class, new Link1Builder().setIgpLinkAttributes(ilab.build()).build());

        final NodeId srcNode = buildNodeId(base, l.getLocalNodeDescriptors());
        LOG.trace("Link {} implies source node {}", l, srcNode);

        final NodeId dstNode = buildNodeId(base, l.getRemoteNodeDescriptors());
        LOG.trace("Link {} implies destination node {}", l, dstNode);

        final TerminationPoint srcTp = buildLocalTp(base, l.getLinkDescriptors());
        LOG.trace("Link {} implies source TP {}", l, srcTp);

        final TerminationPoint dstTp = buildRemoteTp(base, l.getLinkDescriptors());
        LOG.trace("Link {} implies destination TP {}", l, dstTp);

        lb.setSource(new SourceBuilder().setSourceNode(srcNode).setSourceTp(srcTp.getTpId()).build());
        lb.setDestination(new DestinationBuilder().setDestNode(dstNode).setDestTp(dstTp.getTpId()).build());

        LOG.trace("Created TP {} as link source", srcTp);
        NodeHolder snh = this.nodes.get(srcNode);
        if (snh == null) {
            snh = getNode(srcNode);
            snh.addTp(srcTp, lb.getLinkId(), false);
            putNode(trans, snh);
        } else {
            snh.addTp(srcTp, lb.getLinkId(), false);
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(new NodeKey(snh.getNodeId()));
            trans.put(LogicalDatastoreType.OPERATIONAL, nid.child(TerminationPoint.class, srcTp.getKey()), srcTp);
        }

        LOG.debug("Created TP {} as link destination", dstTp);
        NodeHolder dnh = this.nodes.get(dstNode);
        if (dnh == null) {
            dnh = getNode(dstNode);
            dnh.addTp(dstTp, lb.getLinkId(), true);
            putNode(trans, dnh);
        } else {
            dnh.addTp(dstTp, lb.getLinkId(), true);
            final InstanceIdentifier<Node> nid = getInstanceIdentifier().child(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class,
                    new NodeKey(dnh.getNodeId()));
            trans.put(LogicalDatastoreType.OPERATIONAL, nid.child(TerminationPoint.class, dstTp.getKey()), dstTp);
        }

        final InstanceIdentifier<Link> lid = buildLinkIdentifier(lb.getLinkId());
        final Link link = lb.build();

        trans.put(LogicalDatastoreType.OPERATIONAL, lid, link);
        LOG.debug("Created link {} at {} for {}", link, lid, l);
    }

    private void removeTp(final WriteTransaction trans, final NodeId node, final TpId tp,
            final LinkId link, final boolean isRemote) {
        final NodeHolder nh = this.nodes.get(node);
        if (nh != null) {
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(new NodeKey(nh.getNodeId()));
            trans.delete(LogicalDatastoreType.OPERATIONAL, nid.child(TerminationPoint.class, new TerminationPointKey(tp)));
            nh.removeTp(tp, link, isRemote);
            checkNodeForRemoval(trans, nh);
        } else {
            LOG.warn("Removed non-existent node {}", node);
        }
    }

    private void removeLink(final WriteTransaction trans, final UriBuilder base, final LinkCase l) {
        final LinkId id = buildLinkId(base, l);
        final InstanceIdentifier<?> lid = buildLinkIdentifier(id);
        trans.delete(LogicalDatastoreType.OPERATIONAL, lid);
        LOG.debug("Removed link {}", lid);

        removeTp(trans, buildNodeId(base, l.getLocalNodeDescriptors()), buildLocalTpId(base, l.getLinkDescriptors()), id, false);
        removeTp(trans, buildNodeId(base, l.getRemoteNodeDescriptors()), buildRemoteTpId(base, l.getLinkDescriptors()), id, true);
    }

    private static List<Short> nodeMultiTopology(final List<TopologyIdentifier> list) {
        final List<Short> ret = new ArrayList<>(list.size());
        for (final TopologyIdentifier id : list) {
            ret.add(id.getValue().shortValue());
        }
        return ret;
    }

    private static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1 isisNodeAttributes(
            final NodeIdentifier node, final NodeAttributes na) {
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.isis.node.attributes.isis.node.attributes.TedBuilder();
        final IsisNodeAttributesBuilder ab = new IsisNodeAttributesBuilder();

        if (na != null) {
            if (na.getIpv4RouterId() != null) {
                tb.setTeRouterIdIpv4(na.getIpv4RouterId());
            }
            if (na.getIpv6RouterId() != null) {
                tb.setTeRouterIdIpv6(na.getIpv6RouterId());
            }
            if (na.getTopologyIdentifier() != null) {
                ab.setMultiTopologyId(nodeMultiTopology(na.getTopologyIdentifier()));
            }
        }

        final CRouterIdentifier ri = node.getCRouterIdentifier();
        if (ri instanceof IsisPseudonodeCase) {
            final IsisPseudonode pn = ((IsisPseudonodeCase) ri).getIsisPseudonode();
            final IsoBuilder b = new IsoBuilder();
            final String systemId = UriBuilder.isoId(pn.getIsIsRouterIdentifier().getIsoSystemId());
            b.setIsoSystemId(new IsoSystemId(systemId));
            b.setIsoPseudonodeId(new IsoPseudonodeId(BaseEncoding.base16().encode(new byte[] {pn.getPsn().byteValue()})));
            ab.setIso(b.build());
            if (na != null) {
                ab.setNet(toIsoNetIds(na.getIsisAreaId(), systemId));
            }
        } else if (ri instanceof IsisNodeCase) {
            final IsisNode in = ((IsisNodeCase) ri).getIsisNode();
            final String systemId = UriBuilder.isoId(in.getIsoSystemId());
            ab.setIso(new IsoBuilder().setIsoSystemId(new IsoSystemId(systemId)).build());
            if (na != null) {
                ab.setNet(toIsoNetIds(na.getIsisAreaId(), systemId));
            }
        }

        ab.setTed(tb.build());

        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1Builder().setIsisNodeAttributes(
                ab.build()).build();
    }

    private static List<IsoNetId> toIsoNetIds(final List<IsisAreaIdentifier> areaIds, final String systemId) {
        return Lists.transform(areaIds, input -> new IsoNetId(UriBuilder.toIsoNetId(input, systemId)));
    }

    private static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1 ospfNodeAttributes(
            final NodeIdentifier node, final NodeAttributes na) {
        final org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.TedBuilder tb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.ospf.node.attributes.ospf.node.attributes.TedBuilder();
        final OspfNodeAttributesBuilder ab = new OspfNodeAttributesBuilder();
        if (na != null) {
            if (na.getIpv4RouterId() != null) {
                tb.setTeRouterIdIpv4(na.getIpv4RouterId());
            }
            if (na.getIpv6RouterId() != null) {
                tb.setTeRouterIdIpv6(na.getIpv6RouterId());
            }
            if (na.getTopologyIdentifier() != null) {
                ab.setMultiTopologyId(nodeMultiTopology(na.getTopologyIdentifier()));
            }
            final CRouterIdentifier ri = node.getCRouterIdentifier();
            if (ri instanceof OspfPseudonodeCase) {
                final OspfPseudonode pn = ((OspfPseudonodeCase) ri).getOspfPseudonode();

                ab.setRouterType(new PseudonodeBuilder().setPseudonode(Boolean.TRUE).build());
                ab.setDrInterfaceId(pn.getLanInterface().getValue());
            } else if (ri instanceof OspfNodeCase && na.getNodeFlags() != null) {
                // TODO: what should we do with in.getOspfRouterId()?

                final NodeFlagBits nf = na.getNodeFlags();
                if (nf.isAbr() != null) {
                    ab.setRouterType(new AbrBuilder().setAbr(nf.isAbr()).build());
                } else if (nf.isExternal() != null) {
                    ab.setRouterType(new InternalBuilder().setInternal(!nf.isExternal()).build());
                }
            }
        }
        ab.setTed(tb.build());
        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1Builder().setOspfNodeAttributes(
                ab.build()).build();
    }

    private static void augmentProtocolId(final LinkstateRoute value, final IgpNodeAttributesBuilder inab,
            final NodeAttributes na, final NodeIdentifier nd) {
        switch (value.getProtocolId()) {
        case Direct:
        case Static:
        case IsisLevel1:
        case IsisLevel2:
            inab.addAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.isis.topology.rev131021.IgpNodeAttributes1.class,
                    isisNodeAttributes(nd, na));
            break;
        case Ospf:
            inab.addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.ospf.topology.rev131021.IgpNodeAttributes1.class,
                    ospfNodeAttributes(nd, na));
            break;
        default:
            break;
        }
    }

    private void createNode(final WriteTransaction trans, final UriBuilder base,
            final LinkstateRoute value, final NodeCase n, final Attributes attributes) {
        final NodeAttributes na;
        //defensive lookup
        final Attributes1 attr = attributes.getAugmentation(Attributes1.class);
        if (attr != null) {
            final LinkStateAttribute attrType = attr.getLinkStateAttribute();
            if (attrType != null) {
                na = ((NodeAttributesCase)attrType).getNodeAttributes();
            } else {
                LOG.debug("Missing attribute type in node {} route {}, skipping it", n, value);
                na = null;
            }
        } else {
            LOG.debug("Missing attributes in node {} route {}, skipping it", n, value);
            na = null;
        }
        final IgpNodeAttributesBuilder inab = new IgpNodeAttributesBuilder();
        final List<IpAddress> ids = new ArrayList<>();
        if (na != null) {
            if (na.getIpv4RouterId() != null) {
                ids.add(new IpAddress(na.getIpv4RouterId()));
            }
            if (na.getIpv6RouterId() != null) {
                ids.add(new IpAddress(na.getIpv6RouterId()));
            }
            if (na.getDynamicHostname() != null) {
                inab.setName(new DomainName(na.getDynamicHostname()));
            }
        }
        if (!ids.isEmpty()) {
            inab.setRouterId(ids);
        }
        augmentProtocolId(value, inab, na, n.getNodeDescriptors());

        final NodeId nid = buildNodeId(base, n.getNodeDescriptors());
        final NodeHolder nh = getNode(nid);
        /*
         *  Eventhough the the holder creates a dummy structure, we need to duplicate it here,
         *  as that is the API requirement. The reason for it is the possible presence of supporting
         *  node -- something which the holder does not track.
         */
        final NodeBuilder nb = new NodeBuilder();
        nb.setNodeId(nid);
        nb.setKey(new NodeKey(nb.getNodeId()));

        nh.advertized(nb, inab);
        putNode(trans, nh);
    }

    private void removeNode(final WriteTransaction trans, final UriBuilder base, final NodeCase n) {
        final NodeId id = buildNodeId(base, n.getNodeDescriptors());
        final NodeHolder nh = this.nodes.get(id);
        if (nh != null) {
            nh.unadvertized();
            putNode(trans, nh);
        } else {
            LOG.warn("Node {} does not have a holder", id);
        }
    }

    private static void augmentProtocolId(final LinkstateRoute value, final PrefixAttributes pa, final PrefixBuilder pb) {
        switch (value.getProtocolId()) {
        case Direct:
        case IsisLevel1:
        case IsisLevel2:
        case Static:
        case Ospf:
            if (pa != null && pa.getOspfForwardingAddress() != null) {
                pb.addAugmentation(
                        Prefix1.class,
                        new Prefix1Builder().setOspfPrefixAttributes(
                                new OspfPrefixAttributesBuilder().setForwardingAddress(pa.getOspfForwardingAddress().getIpv4Address()).build()).build());
            }
            break;
        default:
            break;
        }
    }

    private void createPrefix(final WriteTransaction trans, final UriBuilder base,
            final LinkstateRoute value, final PrefixCase p, final Attributes attributes) {
        final IpPrefix ippfx = p.getPrefixDescriptors().getIpReachabilityInformation();
        if (ippfx == null) {
            LOG.warn("IP reachability not present in prefix {} route {}, skipping it", p, value);
            return;
        }
        final PrefixBuilder pb = new PrefixBuilder();
        final PrefixKey pk = new PrefixKey(ippfx);
        pb.setKey(pk);
        pb.setPrefix(ippfx);

        final PrefixAttributes pa;
        // Very defensive lookup
        final Attributes1 attr = attributes.getAugmentation(Attributes1.class);
        if (attr != null) {
            final LinkStateAttribute attrType = attr.getLinkStateAttribute();
            if (attrType != null) {
                pa = ((PrefixAttributesCase)attrType).getPrefixAttributes();
            } else {
                LOG.debug("Missing attribute type in IP {} prefix {} route {}, skipping it", ippfx, p, value);
                pa = null;
            }
        } else {
            LOG.debug("Missing attributes in IP {} prefix {} route {}, skipping it", ippfx, p, value);
            pa = null;
        }
        if (pa != null) {
            pb.setMetric(pa.getPrefixMetric().getValue());
        }
        augmentProtocolId(value, pa, pb);

        final Prefix pfx = pb.build();
        LOG.debug("Created prefix {} for {}", pfx, p);

        /*
         * All set, but... the hosting node may not exist, we may need to fake it.
         */
        final NodeId node = buildNodeId(base, p.getAdvertisingNodeDescriptors());
        NodeHolder nh = this.nodes.get(node);
        if (nh == null) {
            nh = getNode(node);
            nh.addPrefix(pfx);
            putNode(trans, nh);
        } else {
            nh.addPrefix(pfx);
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(new NodeKey(nh.getNodeId()));
            final InstanceIdentifier<IgpNodeAttributes> inaId = nid.builder().augmentation(Node1.class).child(IgpNodeAttributes.class).build();
            trans.put(LogicalDatastoreType.OPERATIONAL, inaId.child(Prefix.class, pk), pfx);
        }
    }

    private void removePrefix(final WriteTransaction trans, final UriBuilder base, final PrefixCase p) {
        final NodeId node = buildNodeId(base, p.getAdvertisingNodeDescriptors());
        final NodeHolder nh = this.nodes.get(node);
        if (nh != null) {
            LOG.debug("Removed prefix {}", p);
            final InstanceIdentifier<Node> nid = getNodeInstanceIdentifier(new NodeKey(nh.getNodeId()));
            final InstanceIdentifier<IgpNodeAttributes> inaId = nid.builder().augmentation(Node1.class).child(IgpNodeAttributes.class).build();
            final IpPrefix ippfx = p.getPrefixDescriptors().getIpReachabilityInformation();
            if (ippfx == null) {
                LOG.warn("IP reachability not present in prefix {}, skipping it", p);
                return;
            }
            final PrefixKey pk = new PrefixKey(ippfx);
            trans.delete(LogicalDatastoreType.OPERATIONAL, inaId.child(Prefix.class, pk));
            nh.removePrefix(p);
            checkNodeForRemoval(trans, nh);
        } else {
            LOG.warn("Removing prefix from non-existing node {}", node);
        }
    }

    private InstanceIdentifier<Node> getNodeInstanceIdentifier(final NodeKey nodeKey) {
        return getInstanceIdentifier().child(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node.class,
                nodeKey);
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
            LOG.debug(UNHANDLED_OBJECT_CLASS, t.getImplementedInterface());
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
            LOG.debug(UNHANDLED_OBJECT_CLASS, t.getImplementedInterface());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected InstanceIdentifier<LinkstateRoute> getRouteWildcard(final InstanceIdentifier<Tables> tablesId) {
        return tablesId.child((Class)LinkstateRoutes.class).child(LinkstateRoute.class);
    }

    @Override
    protected void clearTopology() {
        this.nodes.clear();
    }
}
