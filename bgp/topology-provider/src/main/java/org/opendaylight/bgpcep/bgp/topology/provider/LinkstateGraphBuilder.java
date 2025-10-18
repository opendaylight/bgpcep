/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.asla.tlv.AslaSubtlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.bgp.rib.rib.loc.rib.tables.routes.LinkstateRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.PerformanceMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.SrAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.StandardMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.NodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.PrefixCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.LinkStateAttribute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.LinkAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.NodeAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.PrefixAttributesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.link.attributes._case.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.node.attributes._case.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.path.attribute.link.state.attribute.prefix.attributes._case.PrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.routes.LinkstateRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.routes.linkstate.routes.LinkstateRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.routes.linkstate.routes.linkstate.route.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.FlexAlgoDefinition;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.node.state.SrCapabilities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.sr.attributes.SrLanAdjIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.ExtendedAdminGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.IsisAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.adj.flags.flags.OspfAdjFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.flex.algo.definitions.flex.algo.definition.tlv.FlexAlgoSubtlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.IsisPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.OspfPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.isis.prefix.flags._case.IsisPrefixFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.prefix.sid.tlv.flags.ospf.prefix.flags._case.OspfPrefixFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.sid.label.index.LabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev241219.sid.label.index.sid.label.index.SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.Algorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.DecimalBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.FlexMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.Loss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.NodeType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.asla.metric.ApplicationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.EdgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.EdgeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.attributes.AslaMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.attributes.AslaMetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.attributes.EgressPeerEngineering;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.attributes.EgressPeerEngineeringBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.attributes.ExtendedMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.attributes.ExtendedMetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.attributes.SrLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.attributes.SrLinkAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.attributes.TeMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.attributes.TeMetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.extended.metric.MinMaxDelay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.extended.metric.MinMaxDelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.Graph.DomainScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.EdgeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.EdgeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Vertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.VertexBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.prefix.SrPrefixAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.prefix.SrPrefixAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.link.attributes.LinkMsd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.link.attributes.LinkMsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.node.attributes.FlexAlgo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.node.attributes.FlexAlgoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.node.attributes.NodeMsd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.node.attributes.NodeMsdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.node.attributes.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.node.attributes.SrgbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.node.attributes.Srlb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.node.attributes.SrlbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.prefix.attributes.PrefixFlexAlgoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.sr.prefix.attributes.PrefixSrFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.te.metric.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.te.metric.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.te.metric.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.vertex.SrNodeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.vertex.SrNodeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.bgp.linkstate.topology.type.BgpLinkstateTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class build the Traffic Engineering Database as a Connected Graph
 * suitable to be used latter by Path Computation algorithms to compute end to
 * end path.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 */
public class LinkstateGraphBuilder extends AbstractTopologyBuilder<LinkstateRoute> {
    private static final Logger LOG = LoggerFactory.getLogger(LinkstateGraphBuilder.class);
    private static final TopologyTypes LINKSTATE_TOPOLOGY_TYPE = new TopologyTypesBuilder().addAugmentation(
            new TopologyTypes1Builder().setBgpLinkstateTopology(new BgpLinkstateTopologyBuilder().build()).build())
            .build();
    private static final int MAX_PRIORITY = 8;

    private final ConnectedGraph cgraph;

    public LinkstateGraphBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final ConnectedGraphProvider provider) {
        super(dataProvider, locRibReference, topologyId, LINKSTATE_TOPOLOGY_TYPE, LinkstateAddressFamily.VALUE,
                LinkstateSubsequentAddressFamily.VALUE);
        cgraph = requireNonNull(provider).createConnectedGraph("ted://" + topologyId.getValue(),
                DomainScope.IntraDomain);
        /* LinkStateGraphBuilder doesn't write information in the Network Topology tree of the Data Store.
         * This is performed by ConnectedGraphProvider which write element in Graph tree of the Data Store */
        networkTopologyTransaction = false;
        LOG.info("Started Traffic Engineering Graph Builder");
    }

    @VisibleForTesting
    LinkstateGraphBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final ConnectedGraphProvider provider, final long listenerResetLimitInMillsec,
            final int listenerResetEnforceCounter) {
        super(dataProvider, locRibReference, topologyId, LINKSTATE_TOPOLOGY_TYPE, LinkstateAddressFamily.VALUE,
                LinkstateSubsequentAddressFamily.VALUE, listenerResetLimitInMillsec, listenerResetEnforceCounter);
        cgraph = requireNonNull(provider).createConnectedGraph("ted://" + topologyId.getValue(),
                DomainScope.IntraDomain);
        /* LinkStateGraphBuilder doesn't write information in the Network Topology tree of the Data Store.
         * This is performed by ConnectedGraphProvider which write element in Graph tree of the Data Store */
        networkTopologyTransaction = false;
        LOG.info("Started Traffic Engineering Graph Builder");
    }

    @Override
    protected void routeChanged(final DataTreeModification<LinkstateRoute> change, final ReadWriteTransaction trans) {
        final var root = change.getRootNode();
        switch (root.modificationType()) {
            case DELETE:
                removeObject(trans, change.path(), root.dataBefore());
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                createObject(trans, change.path(), root.dataAfter());
                break;
            default:
                throw new IllegalArgumentException("Unhandled modification type " + root.modificationType());
        }
    }

    @Override
    protected void createObject(final ReadWriteTransaction trans, final DataObjectIdentifier<LinkstateRoute> id,
            final LinkstateRoute value) {
        final var type = value.getObjectType();
        switch (type) {
            case LinkCase link -> createEdge(value, link, value.getAttributes());
            case NodeCase node -> createVertex(value, node, value.getAttributes());
            case PrefixCase prefix -> createPrefix(value, prefix, value.getAttributes());
            default -> LOG.debug("Unhandled created object class {}", type.implementedInterface());
        }
    }

    /**
     * Verify that mandatory information (Local & Remote Node and Link
     * descriptors) are present in the link.
     *
     * @param linkCase  The Link part of the Linkstate route
     *
     * @return True if all information are present, false otherwise
     */
    private static boolean checkLinkState(final LinkCase linkCase) {
        if (linkCase.getLocalNodeDescriptors() == null || linkCase.getRemoteNodeDescriptors() == null) {
            LOG.warn("Missing Local or Remote Node descriptor in link {}, skipping it", linkCase);
            return false;
        }
        if (linkCase.getLinkDescriptors() == null) {
            LOG.warn("Missing Link descriptor in link {}, skipping it", linkCase);
            return false;
        }
        return true;
    }

    /**
     * Get Link attributes from the Link State route.
     *
     * @param attributes  Link State Route Attributes
     *
     * @return Link Attributes
     */
    private static LinkAttributes getLinkAttributes(final Attributes attributes) {
        final var attr = attributes.augmentation(Attributes1.class);
        if (attr != null) {
            final var attrType = attr.getLinkStateAttribute();
            if (attrType instanceof LinkAttributesCase) {
                return ((LinkAttributesCase) attrType).getLinkAttributes();
            }
        }
        return null;
    }

    /**
     * Determine the Source Edge Key from the link descriptor.
     * There is several case: IPv4, IPv6 address or Unnumbered Interface.
     * For Multi-Topology i.e. IPv4 + IPv6, Edge Key is build from IPv4 address.
     *
     * @param linkCase The Link part of the Linkstate route
     *
     * @return Unique key
     */
    private static Uint64 getEdgeId(final LinkCase linkCase) {
        final var linkDescriptors = linkCase.getLinkDescriptors();
        if (linkDescriptors.getIpv4InterfaceAddress() != null) {
            return ipv4ToKey(linkDescriptors.getIpv4InterfaceAddress());
        }
        if (linkDescriptors.getIpv6InterfaceAddress() != null) {
            return ipv6ToKey(linkDescriptors.getIpv6InterfaceAddress());
        }
        if (linkDescriptors.getLinkLocalIdentifier() != null) {
            return linkDescriptors.getLinkLocalIdentifier().toUint64();
        }
        return Uint64.ZERO;
    }

    /**
     * Create new Connected Edge in the Connected Graph.
     *
     * @param value       The complete Linkstate route information
     * @param linkCase    The Link part of the Linkstate route
     * @param attributes  The Link attributes
     *
     */
    private void createEdge(final LinkstateRoute value, final LinkCase linkCase, final Attributes attributes) {
        checkArgument(checkLinkState(linkCase), "Missing mandatory information in link {}", linkCase);

        final var la = getLinkAttributes(attributes);
        if (la == null) {
            LOG.warn("Missing attributes in link {} route {}, skipping it", linkCase, value);
            return;
        }

        /* Get Source and Destination Vertex from the graph */
        final var srcId = getVertexId(linkCase.getLocalNodeDescriptors().getCRouterIdentifier());
        final var dstId = getVertexId(linkCase.getRemoteNodeDescriptors().getCRouterIdentifier());
        if (srcId == Uint64.ZERO || dstId == Uint64.ZERO) {
            LOG.warn("Unable to get the Source or Destination Vertex Identifier from link {}, skipping it", linkCase);
            return;
        }

        /* Get Source and Destination Key for the corresponding Edge */
        final var edgeId = getEdgeId(linkCase);
        if (edgeId == Uint64.ZERO) {
            LOG.warn("Unable to get the Edge Identifier from link {}, skipping it", linkCase);
            return;
        }

        /* Add associated Edge */
        final Edge edge = new EdgeBuilder().setEdgeId(edgeId).setLocalVertexId(srcId).setRemoteVertexId(dstId)
                .setName(srcId + " - " + dstId)
                .setEdgeAttributes(createEdgeAttributes(la, linkCase.getLinkDescriptors())).build();

        /*
         * Add corresponding Prefix for the Local Address. Remote address will be added with the remote Edge */
        final var attr = edge.getEdgeAttributes();
        PrefixBuilder prefBuilder = new PrefixBuilder().setVertexId(srcId);
        if (attr.getLocalAddress() != null) {
            prefBuilder.setPrefix(new IpPrefix(IetfInetUtil.ipv4PrefixFor(attr.getLocalAddress())));
        }
        if (attr.getLocalAddress6() != null) {
            prefBuilder.setPrefix(new IpPrefix(IetfInetUtil.ipv6PrefixFor(attr.getLocalAddress6())));
        }

        /* Add the Edge in the Connected Graph */
        LOG.debug("Add Edge {} and associated Prefix {} in TED[{}]", edge.getName(), prefBuilder.getPrefix(), cgraph);
        cgraph.addEdge(edge);
        cgraph.addPrefix(prefBuilder.build());
    }

    /**
     * Create Edge Attributes from Link attributes.
     *
     * @param la         Linkstate Attributes
     * @param linkDesc   Linkstate Descriptors
     *
     * @return EdgeAttributes
     */
    private static EdgeAttributes createEdgeAttributes(final LinkAttributes la, final LinkDescriptors linkDesc) {
        EdgeAttributesBuilder builder = new EdgeAttributesBuilder();

        // IPv4 and/or IPv6 Interfaces addresses and Link Identifier
        if (linkDesc.getIpv4InterfaceAddress() != null) {
            builder.setLocalAddress(linkDesc.getIpv4InterfaceAddress());
        }
        if (linkDesc.getIpv6InterfaceAddress() != null) {
            builder.setLocalAddress6(linkDesc.getIpv6InterfaceAddress());
        }
        if (linkDesc.getIpv4NeighborAddress() != null) {
            builder.setRemoteAddress(linkDesc.getIpv4NeighborAddress());
        }
        if (linkDesc.getIpv6NeighborAddress() != null) {
            builder.setRemoteAddress6(linkDesc.getIpv6NeighborAddress());
        }
        if (linkDesc.getLinkLocalIdentifier() != null) {
            builder.setLocalIdentifier(linkDesc.getLinkLocalIdentifier());
        } else if (la.getLinkLocalIdentifier() != null) {
            builder.setLocalIdentifier(la.getLinkLocalIdentifier());
        }
        if (linkDesc.getLinkRemoteIdentifier() != null) {
            builder.setRemoteIdentifier(linkDesc.getLinkRemoteIdentifier());
        } else if (la.getLinkRemoteIdentifier() != null) {
            builder.setRemoteIdentifier(la.getLinkRemoteIdentifier());
        }
        // IGP Metric
        if (la.getMetric() != null) {
            builder.setMetric(la.getMetric().getValue());
        }
        // Standard TE Metrics
        if (la.getStandardMetric() != null) {
            builder.setTeMetric(getStandardMetric(la.getStandardMetric()));
        }
        // Extended TE Metrics
        if (la.getPerformanceMetric() != null || la.getExtendedAdminGroup() != null) {
            builder.setExtendedMetric(getExtendedMetric(la.getPerformanceMetric(), la.getExtendedAdminGroup()));
        }
        // Shared Risk Link Group (SRLG)
        if (la.getSharedRiskLinkGroups() != null) {
            final var srlgs = ImmutableSet.<Uint32>builder();
            la.getSharedRiskLinkGroups().forEach(srlg -> srlgs.add(srlg.getValue()));
            builder.setSrlgs(srlgs.build());
        }
        // Segment Routing
        if (la.getSrAttribute() != null || la.getEgressPeerEngineering() != null) {
            builder.setSrLinkAttributes(getSrLinkAttributes(la.getSrAttribute()));
        }
        // Egress Peer Engineering
        if (la.getEgressPeerEngineering() != null) {
            builder.setEgressPeerEngineering(getEgreePeerEngineering(la.getEgressPeerEngineering()));
        }
        // Application Specific Link Attributes (ASLA)
        if (la.getAslaMetric() != null) {
            builder.setAslaMetric(getAslaAttributes(la.getAslaMetric()));
        }

        return builder.build();
    }

    private static TeMetric getStandardMetric(final StandardMetric sm) {
        final TeMetricBuilder tmBuilder = new TeMetricBuilder();

        if (sm.getTeMetric() != null) {
            tmBuilder.setMetric(sm.getTeMetric().getValue());
        }
        if (sm.getMaxLinkBandwidth() != null) {
            tmBuilder.setMaxLinkBandwidth(bandwithToDecimalBandwidth(sm.getMaxLinkBandwidth()));
        }
        if (sm.getMaxReservableBandwidth() != null) {
            tmBuilder.setMaxResvLinkBandwidth(bandwithToDecimalBandwidth(sm.getMaxReservableBandwidth()));
        }
        if (sm.getUnreservedBandwidth() != null) {
            int upperBound = Math.min(sm.getUnreservedBandwidth().size(), MAX_PRIORITY);
            final List<UnreservedBandwidth> unRsvBw = new ArrayList<>(upperBound);

            for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219
                    .UnreservedBandwidth bandwidth : sm.nonnullUnreservedBandwidth().values()) {
                unRsvBw.add(new UnreservedBandwidthBuilder()
                        .setBandwidth(bandwithToDecimalBandwidth(bandwidth.getBandwidth()))
                        .withKey(new UnreservedBandwidthKey(bandwidth.getPriority())).build());
            }
            tmBuilder.setUnreservedBandwidth(unRsvBw);
        }
        if (sm.getAdminGroup() != null) {
            tmBuilder.setAdminGroup(sm.getAdminGroup().getValue());
        }

        return tmBuilder.build();
    }

    private static ExtendedMetric getExtendedMetric(final PerformanceMetric pm, final Set<ExtendedAdminGroup> eags) {
        // Performance Metric
        final ExtendedMetricBuilder emBuilder = new ExtendedMetricBuilder();
        if (pm != null) {
            if (pm.getLinkDelay() != null) {
                emBuilder.setDelay(new Delay(pm.getLinkDelay().getValue()));
            }
            if (pm.getLinkMinMaxDelay() != null && pm.getLinkMinMaxDelay() != null) {
                MinMaxDelay mmDelay = new MinMaxDelayBuilder()
                        .setMaxDelay(new Delay(pm.getLinkMinMaxDelay().getMaxDelay().getValue()))
                        .setMinDelay(new Delay(pm.getLinkMinMaxDelay().getMinDelay().getValue())).build();
                emBuilder.setMinMaxDelay(mmDelay);
            }
            if (pm.getDelayVariation() != null) {
                emBuilder.setJitter(new Delay(pm.getDelayVariation().getValue()));
            }
            if (pm.getLinkLoss() != null) {
                emBuilder.setLoss(new Loss(pm.getLinkLoss().getValue()));
            }
            if (pm.getAvailableBandwidth() != null) {
                emBuilder.setAvailableBandwidth(bandwithToDecimalBandwidth(pm.getAvailableBandwidth()));
            }
            if (pm.getResidualBandwidth() != null) {
                emBuilder.setResidualBandwidth(bandwithToDecimalBandwidth(pm.getResidualBandwidth()));
            }
            if (pm.getUtilizedBandwidth() != null) {
                emBuilder.setUtilizedBandwidth(bandwithToDecimalBandwidth(pm.getUtilizedBandwidth()));
            }
        }

        // Extended Admin Group
        if (eags != null) {
            final var eagBuilder = ImmutableSet.<Uint32>builder();
            eags.forEach(eag -> eagBuilder.add(eag.getValue()));
            emBuilder.setExtendedAdminGroup(eagBuilder.build());
        }

        return emBuilder.build();
    }

    private static SrLinkAttributes getSrLinkAttributes(final SrAttribute sa) {
        final SrLinkAttributesBuilder srBuilder = new SrLinkAttributesBuilder();

        // Adjacency SID
        for (SrAdjIds adj : sa.nonnullSrAdjIds()) {
            boolean backup = false;
            boolean ipv6 = false;
            if (adj.getFlags() instanceof OspfAdjFlagsCase) {
                backup = ((OspfAdjFlagsCase) adj.getFlags()).getOspfAdjFlags().getBackup();
            }
            if (adj.getFlags() instanceof IsisAdjFlagsCase) {
                backup = ((IsisAdjFlagsCase) adj.getFlags()).getIsisAdjFlags().getBackup();
                ipv6 = ((IsisAdjFlagsCase) adj.getFlags()).getIsisAdjFlags().getAddressFamily();
            }
            final Uint32 adjSid;
            if (adj.getSidLabelIndex() instanceof LabelCase) {
                adjSid = ((LabelCase) adj.getSidLabelIndex()).getLabel().getValue();
            } else if (adj.getSidLabelIndex() instanceof SidCase) {
                // Should be mapped into SRGB or SRLB, but they may not yet be available
                adjSid = ((SidCase) adj.getSidLabelIndex()).getSid();
            } else {
                continue;
            }
            if (!backup) {
                if (!ipv6) {
                    srBuilder.setAdjSid(adjSid);
                } else {
                    srBuilder.setAdjSid6(adjSid);
                }
            } else if (!ipv6) {
                srBuilder.setBackupAdjSid(adjSid);
            } else {
                srBuilder.setBackupAdjSid6(adjSid);
            }
        }

        // Or Lan Adjacency SID
        for (SrLanAdjIds ladj : sa.nonnullSrLanAdjIds()) {
            boolean backup = false;
            boolean ipv6 = false;
            if (ladj.getFlags() instanceof OspfAdjFlagsCase) {
                backup = ((OspfAdjFlagsCase) ladj.getFlags()).getOspfAdjFlags().getBackup();
            }
            if (ladj.getFlags() instanceof IsisAdjFlagsCase) {
                backup = ((IsisAdjFlagsCase) ladj.getFlags()).getIsisAdjFlags().getBackup();
                ipv6 = ((IsisAdjFlagsCase) ladj.getFlags()).getIsisAdjFlags().getAddressFamily();
            }
            final Uint32 adjSid;
            if (ladj.getSidLabelIndex() instanceof LabelCase) {
                adjSid = ((LabelCase) ladj.getSidLabelIndex()).getLabel().getValue();
            } else if (ladj.getSidLabelIndex() instanceof SidCase) {
                // Should be mapped into SRGB or SRLB, but they may not yet be available
                adjSid = ((SidCase) ladj.getSidLabelIndex()).getSid();
            } else {
                continue;
            }
            if (!backup) {
                if (!ipv6) {
                    srBuilder.setAdjSid(adjSid);
                } else {
                    srBuilder.setAdjSid6(adjSid);
                }
            } else if (!ipv6) {
                srBuilder.setBackupAdjSid(adjSid);
            } else {
                srBuilder.setBackupAdjSid6(adjSid);
            }
        }

        // Link MSD
        if (sa.getLinkMsd() != null) {
            final List<LinkMsd> msds = new ArrayList<LinkMsd>();
            sa.getLinkMsd().forEach(msd -> msds.add(new LinkMsdBuilder()
                    .setMsdType(Uint8.valueOf(msd.getType().getIntValue()))
                    .setValue(msd.getValue())
                    .build()));
            srBuilder.setLinkMsd(msds);
        }

        return srBuilder.build();
    }

    private static EgressPeerEngineering getEgreePeerEngineering(final org.opendaylight.yang.gen.v1.urn.opendaylight
            .params.xml.ns.yang.bgp.linkstate.rev241219.linkstate.attribute.EgressPeerEngineering epe) {
        final EgressPeerEngineeringBuilder epeBuilder = new EgressPeerEngineeringBuilder();

        if (epe.getPeerAdjSid() != null) {
            if (epe.getPeerAdjSid().getSidLabelIndex() instanceof LabelCase) {
                epeBuilder.setPeerAdjSid(((LabelCase) epe.getPeerAdjSid().getSidLabelIndex()).getLabel().getValue());
            } else if (epe.getPeerAdjSid().getSidLabelIndex() instanceof SidCase) {
                // Should be mapped into SRGB or SRLB, but they may not yet be available
                epeBuilder.setPeerAdjSid(((SidCase) epe.getPeerAdjSid().getSidLabelIndex()).getSid());
            }
        }
        if (epe.getPeerNodeSid() != null) {
            if (epe.getPeerNodeSid().getSidLabelIndex() instanceof LabelCase) {
                epeBuilder.setPeerNodeSid(((LabelCase) epe.getPeerNodeSid().getSidLabelIndex()).getLabel().getValue());
            } else if (epe.getPeerNodeSid().getSidLabelIndex() instanceof SidCase) {
                // Should be mapped into SRGB or SRLB, but they may not yet be available
                epeBuilder.setPeerNodeSid(((SidCase) epe.getPeerNodeSid().getSidLabelIndex()).getSid());
            }
        }
        if (epe.getPeerSetSids() != null && !epe.getPeerSetSids().isEmpty()) {
            final var sids = ImmutableSet.<Uint32>builder();
            epe.getPeerSetSids().forEach(sid -> {
                if (sid.getSidLabelIndex() instanceof LabelCase) {
                    sids.add(((LabelCase) sid.getSidLabelIndex()).getLabel().getValue());
                } else if (sid.getSidLabelIndex() instanceof SidCase) {
                    // Should be mapped into SRGB or SRLB, but they may not yet be available
                    sids.add(((SidCase) sid.getSidLabelIndex()).getSid());
                }
            });
            epeBuilder.setPeerSetSids(sids.build());
        }

        return epeBuilder.build();
    }

    private static AslaMetric getAslaAttributes(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
            .bgp.linkstate.rev241219.linkstate.attribute.AslaMetric asla) {
        final AslaMetricBuilder aBuilder = new AslaMetricBuilder();

        // Get Application Type
        aBuilder.setApplications(new ApplicationsBuilder()
            .setRsvpTe(asla.getStandardBitMask().getRsvpTe())
            .setSegmentRouting(asla.getStandardBitMask().getSr())
            .setLfa(asla.getStandardBitMask().getLfa())
            .setFlexAlgo(asla.getStandardBitMask().getFlexAlgo())
            .build());

        // Get Metrics from Asla subTLVs
        final AslaSubtlvs aSubTlvs = asla.getAslaSubtlvs();
        if (aSubTlvs.getTeMetric() != null) {
            aBuilder.setTeMetric(aSubTlvs.getTeMetric().getValue());
        }
        if (aSubTlvs.getAdminGroup() != null) {
            aBuilder.setAdminGroup(aSubTlvs.getAdminGroup().getValue());
        }
        if (aSubTlvs.getLinkDelay() != null) {
            aBuilder.setDelay(new Delay(aSubTlvs.getLinkDelay().getValue()));
        }
        if (aSubTlvs.getLinkMinMaxDelay() != null && aSubTlvs.getLinkMinMaxDelay() != null) {
            MinMaxDelay mmDelay = new MinMaxDelayBuilder()
                    .setMaxDelay(new Delay(aSubTlvs.getLinkMinMaxDelay().getMaxDelay().getValue()))
                    .setMinDelay(new Delay(aSubTlvs.getLinkMinMaxDelay().getMinDelay().getValue())).build();
            aBuilder.setMinMaxDelay(mmDelay);
        }
        if (aSubTlvs.getDelayVariation() != null) {
            aBuilder.setJitter(new Delay(aSubTlvs.getDelayVariation().getValue()));
        }
        if (aSubTlvs.getLinkLoss() != null) {
            aBuilder.setLoss(new Loss(aSubTlvs.getLinkLoss().getValue()));
        }
        if (aSubTlvs.getAvailableBandwidth() != null) {
            aBuilder.setAvailableBandwidth(bandwithToDecimalBandwidth(aSubTlvs.getAvailableBandwidth()));
        }
        if (aSubTlvs.getResidualBandwidth() != null) {
            aBuilder.setResidualBandwidth(bandwithToDecimalBandwidth(aSubTlvs.getResidualBandwidth()));
        }
        if (aSubTlvs.getUtilizedBandwidth() != null) {
            aBuilder.setUtilizedBandwidth(bandwithToDecimalBandwidth(aSubTlvs.getUtilizedBandwidth()));
        }
        if (aSubTlvs.getSharedRiskLinkGroups() != null) {
            final var srlgs = ImmutableSet.<Uint32>builder();
            aSubTlvs.getSharedRiskLinkGroups().forEach(srlg -> srlgs.add(srlg.getValue()));
            aBuilder.setSrlgs(srlgs.build());
        }
        if (aSubTlvs.getExtendedAdminGroup() != null) {
            final var eags = ImmutableSet.<Uint32>builder();
            aSubTlvs.getExtendedAdminGroup().forEach(eag -> eags.add(eag.getValue()));
            aBuilder.setSrlgs(eags.build());
        }

        return aBuilder.build();
    }

    /**
     * Get Node Attributes from Link State Route attributes.
     *
     * @param attributes  The attribute part from the Link State route
     *
     * @return Node Attributes
     */
    private static NodeAttributes getNodeAttributes(final Attributes attributes) {
        final var attr = attributes.augmentation(Attributes1.class);
        if (attr != null) {
            final LinkStateAttribute attrType = attr.getLinkStateAttribute();
            if (attrType != null) {
                return ((NodeAttributesCase) attrType).getNodeAttributes();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Create Vertex from the Node Attributes.
     *
     * @param na       Node Attributes
     * @param cvertex  Connected Vertex associated to this Vertex
     * @param as       As number
     *
     * @return New Vertex
     */
    private static Vertex getVertex(final NodeAttributes na, final Uint64 id, final Uint32 as) {
        VertexBuilder builder = new VertexBuilder().setVertexId(id).setAsn(as);

        if (na.getIpv4RouterId() != null) {
            builder.setRouterId(na.getIpv4RouterId());
        }
        if (na.getIpv6RouterId() != null) {
            builder.setRouterId6(na.getIpv6RouterId());
        }
        /*
         * Set Router Name with dynamic hostname (IS-IS) or IPv4 address in dot decimal format (OSPF)
         */
        if (na.getDynamicHostname() != null) {
            builder.setName(na.getDynamicHostname());
        } else {
            int key = id.intValue();
            builder.setName(
                    (key << 24 & 0xFF) + "." + (key << 16 & 0xFF) + "." + (key << 8 & 0xFF) + "." + (key & 0xFF));
        }

        // Set Flags
        if (na.getNodeFlags() != null) {
            if (na.getNodeFlags().getAbr()) {
                builder.setType(NodeType.Abr);
            }
            if (na.getNodeFlags().getExternal()) {
                builder.setType(NodeType.AsbrOut);
            }
        } else {
            builder.setType(NodeType.Standard);
        }

        // Set Segment Routing if present
        if (na.getSrCapabilities() != null) {
            builder.setSrNodeAttributes(getSrNodeAttributes(na.getSrCapabilities(), na.getFlexAlgoDefinition()));
        }

        return builder.build();
    }

    /**
     * Fulfill Vertex Segment Routing Information from the Node attributes.
     *
     * @param sr    Node Attributes
     * @return      Segment Routing Node information
     */
    private static SrNodeAttributes getSrNodeAttributes(final SrCapabilities sr, final FlexAlgoDefinition fad) {
        final SrNodeAttributesBuilder sraBuilder = new SrNodeAttributesBuilder();

        // SR Flags
        sraBuilder.setMplsIpv4(sr.getMplsIpv4())
            .setMplsIpv6(sr.getMplsIpv6());
        // Algorithms
        if (sr.getAlgorithms() != null) {
            final var algos = ImmutableSet.<Algorithm>builder();
            sr.getAlgorithms().forEach(algo -> algos.add(Algorithm.forValue(algo.getIntValue())));
            sraBuilder.setAlgorithms(algos.build());
        }
        // MSD
        if (sr.getNodeMsd() != null) {
            final var msds = new ArrayList<NodeMsd>();
            sr.getNodeMsd().forEach(msd ->
                msds.add(new NodeMsdBuilder()
                    .setMsdType(Uint8.valueOf(msd.getType().getIntValue()))
                    .setValue(msd.getValue())
                    .build())
            );
            sraBuilder.setNodeMsd(msds);
        }
        // SRGB
        if (sr.getSrgb() != null) {
            final var srgbs = new ArrayList<Srgb>();
            sr.getSrgb().forEach(srgb -> {
                final var labelIndex = sr.getSrgb().getFirst().getSidLabelIndex();
                if (labelIndex instanceof LabelCase) {
                    srgbs.add(new SrgbBuilder()
                            .setLowerBound(((LabelCase) labelIndex).getLabel().getValue())
                            .setRangeSize(sr.getSrgb().getFirst().getRangeSize().getValue())
                            .build());
                } else if (labelIndex instanceof SidCase) {
                    srgbs.add(new SrgbBuilder()
                            .setLowerBound(((SidCase) labelIndex).getSid())
                            .setRangeSize(sr.getSrgb().getFirst().getRangeSize().getValue())
                            .build());
                }
            });
            sraBuilder.setSrgb(srgbs);
        }
        // SRLB
        if (sr.getSrlb() != null) {
            final var srlbs = new ArrayList<Srlb>();
            sr.getSrlb().forEach(srlb -> {
                final var labelIndex = sr.getSrlb().getFirst().getSidLabelIndex();
                if (labelIndex instanceof LabelCase) {
                    srlbs.add(new SrlbBuilder()
                            .setLowerBound(((LabelCase) labelIndex).getLabel().getValue())
                            .setRangeSize(sr.getSrlb().getFirst().getRangeSize().getValue())
                            .build());
                } else if (labelIndex instanceof SidCase) {
                    srlbs.add(new SrlbBuilder()
                            .setLowerBound(((SidCase) labelIndex).getSid())
                            .setRangeSize(sr.getSrlb().getFirst().getRangeSize().getValue())
                            .build());
                }
            });
            sraBuilder.setSrlb(srlbs);
        }
        // Flex Algo
        if (fad != null) {
            sraBuilder.setFlexAlgo(getFlexAlgoDefinition(fad));
        }

        return sraBuilder.build();
    }

    /**
     * Fulfill Vertex Flex Algo Information from the Node attributes.
     *
     * @param fad   Node Flex Algo Definition
     * @return      FlexAlgo
     */
    private static List<FlexAlgo> getFlexAlgoDefinition(final FlexAlgoDefinition fadList) {
        final var faList = new ArrayList<FlexAlgo>();

        fadList.getFlexAlgoDefinitionTlv().forEach(fad -> {
            final FlexAlgoBuilder fab = new FlexAlgoBuilder();

            fab.setCalcType(fad.getCalcType());
            fab.setFlexAlgo(fad.getFlexAlgo().getValue());
            fab.setMetricType(FlexMetric.forValue(fad.getMetricType().getIntValue()));
            fab.setPriority(fad.getPriority());

            // SubTlvs
            final FlexAlgoSubtlvs fas = fad.getFlexAlgoSubtlvs();
            final var excludes = ImmutableSet.<Uint32>builder();
            fas.getExcludeAny().forEach(eag -> excludes.add(eag.getValue()));
            final var includesAny = ImmutableSet.<Uint32>builder();
            fas.getIncludeAny().forEach(eag -> includesAny.add(eag.getValue()));
            final var includesAll = ImmutableSet.<Uint32>builder();
            fas.getIncludeAll().forEach(eag -> includesAny.add(eag.getValue()));
            final var srlgs = ImmutableSet.<Uint32>builder();
            fas.getExcludeSrlg().forEach(srlg -> srlgs.add(srlg.getValue()));
            fab.setExcludeAny(excludes.build())
                .setIncludeAny(includesAny.build())
                .setIncludeAll(includesAll.build())
                .setExcludeSrlg(srlgs.build());
            faList.add(fab.build());
        });

        return faList;
    }

    /**
     * Create new Connected Vertex in the Connected Graph.
     *
     * @param value       The complete Linkstate route information
     * @param nodeCase    The node part of the Linkstate route
     * @param attributes  The node attributes
     */
    private void createVertex(final LinkstateRoute value, final NodeCase nodeCase, final Attributes attributes) {
        checkArgument(nodeCase != null, "Missing Node Case. Skip this Node");
        checkArgument(nodeCase.getNodeDescriptors() != null, "Missing Node Descriptors. Skip this Node");

        final var vertexId = getVertexId(nodeCase.getNodeDescriptors().getCRouterIdentifier());
        if (vertexId == Uint64.ZERO) {
            LOG.warn("Unable to get Vertex Identifier from descriptor {}, skipping it", nodeCase.getNodeDescriptors());
            return;
        }

        final var na = getNodeAttributes(attributes);
        if (na == null) {
            LOG.warn("Missing attributes in node {} route {}, skipping it", nodeCase, value);
            return;
        }

        Uint32 asNumber = Uint32.ZERO;
        if (nodeCase.getNodeDescriptors() != null) {
            asNumber = nodeCase.getNodeDescriptors().getAsNumber().getValue();
        }
        Vertex vertex = getVertex(na, vertexId, asNumber);

        /* Add the Connected Vertex and associated Vertex in the Graph */
        LOG.debug("Add Vertex {} in TED[{}]", vertex.getName(), cgraph);
        cgraph.addVertex(vertex);
    }

    /**
     * Create new Prefix in the Connected Graph.
     *
     * @param value       The complete Linkstate route information
     * @param prefixCase  The Prefix part of the Linkstate route
     * @param attributes  The Prefix attributes
     */
    private void createPrefix(final LinkstateRoute value, final PrefixCase prefixCase, final Attributes attributes) {
        final var ippfx = prefixCase.getPrefixDescriptors().getIpReachabilityInformation();
        if (ippfx == null) {
            LOG.warn("IP reachability not present in prefix {} route {}, skipping it", prefixCase, value);
            return;
        }

        /* Verify that all mandatory information are present */
        final PrefixAttributes pa;
        final var attr = attributes.augmentation(Attributes1.class);
        if (attr != null) {
            final var attrType = attr.getLinkStateAttribute();
            if (attrType instanceof PrefixAttributesCase) {
                pa = ((PrefixAttributesCase) attrType).getPrefixAttributes();
            } else {
                LOG.warn("Missing attribute type in IP {} prefix {} route {}, skipping it", ippfx, prefixCase, value);
                return;
            }
        } else {
            LOG.warn("Missing attributes in IP {} prefix {} route {}, skipping it", ippfx, prefixCase, value);
            return;
        }

        /*
         * Get Connected Vertex from Connected Graph corresponding to the
         * Advertising Node Descriptor
         */
        final var vertexId = getVertexId(prefixCase.getAdvertisingNodeDescriptors().getCRouterIdentifier());
        if (vertexId == Uint64.ZERO) {
            LOG.warn("Unable to get the Vertex Identifier from descriptor {}, skipping it",
                    prefixCase.getAdvertisingNodeDescriptors());
            return;
        }

        /* Create Prefix */
        PrefixBuilder builder = new PrefixBuilder().setVertexId(vertexId).setPrefix(ippfx);
        if (pa.getSrPrefix() != null) {
            builder.setSrPrefixAttributes(getSrPrefixAttributes(pa));
        }

        /* Add the Prefix to the Connected Vertex within the Connected Graph */
        LOG.debug("Add prefix {} in TED[{}]", builder.getPrefix(), cgraph);
        cgraph.addPrefix(builder.build());
    }

    private static SrPrefixAttributes getSrPrefixAttributes(final PrefixAttributes pa) {
        final SrPrefixAttributesBuilder srpBuilder = new SrPrefixAttributesBuilder();

        // SID
        if (pa.getSrPrefix().getSidLabelIndex() instanceof SidCase) {
            srpBuilder.setPrefixSid(((SidCase) pa.getSrPrefix().getSidLabelIndex()).getSid());
        } else if (pa.getSrPrefix().getSidLabelIndex() instanceof LabelCase) {
            srpBuilder.setPrefixSid(((LabelCase) pa.getSrPrefix().getSidLabelIndex()).getLabel().getValue());
        }
        // Algo
        srpBuilder.setPrefixAlgo(Uint8.valueOf(pa.getSrPrefix().getAlgorithm().getIntValue()));
        // Flag
        final PrefixSrFlagsBuilder pFlagsBuilder = new PrefixSrFlagsBuilder();
        if (pa.getSrPrefix().getFlags() instanceof IsisPrefixFlagsCase) {
            final IsisPrefixFlags flags = ((IsisPrefixFlagsCase) pa.getSrPrefix().getFlags()).getIsisPrefixFlags();
            pFlagsBuilder.setNodeSid(flags.getNodeSid());
            pFlagsBuilder.setNoPhp(flags.getNoPhp());
            pFlagsBuilder.setExplicitNull(flags.getExplicitNull());
            pFlagsBuilder.setValue(flags.getValue());
            pFlagsBuilder.setLocal(flags.getValue());
            srpBuilder.setPrefixSrFlags(pFlagsBuilder.build());
        } else if (pa.getSrPrefix().getFlags() instanceof OspfPrefixFlagsCase) {
            final OspfPrefixFlags flags = ((OspfPrefixFlagsCase) pa.getSrPrefix().getFlags()).getOspfPrefixFlags();
            /*
             * Node SID flag is not available with OSPF Flags. Assuming that the Prefix is a Node SID
             */
            pFlagsBuilder.setNodeSid(true);
            pFlagsBuilder.setNoPhp(flags.getNoPhp());
            pFlagsBuilder.setExplicitNull(flags.getExplicitNull());
            pFlagsBuilder.setValue(flags.getValue());
            pFlagsBuilder.setLocal(flags.getValue());
            srpBuilder.setPrefixSrFlags(pFlagsBuilder.build());
        }
        // Flex Algo
        if (pa.getFlexAlgoPrefixMetric() != null) {
            final PrefixFlexAlgoBuilder pfaBuilder = new PrefixFlexAlgoBuilder();
            pfaBuilder.setFlexAlgo(pa.getFlexAlgoPrefixMetric().getFlexAlgo().getValue());
            pfaBuilder.setMetric(pa.getFlexAlgoPrefixMetric().getMetric());
            srpBuilder.setPrefixFlexAlgo(pfaBuilder.build());
        }
        return srpBuilder.build();
    }

    @Override
    protected void removeObject(final ReadWriteTransaction trans, final DataObjectIdentifier<LinkstateRoute> id,
            final LinkstateRoute value) {
        if (value == null) {
            LOG.error("Empty before-data received in delete data change notification for instance id {}", id);
            return;
        }

        final var type = value.getObjectType();
        switch (type) {
            case LinkCase link -> removeEdge(link);
            case NodeCase node -> removeVertex(node);
            case PrefixCase prefix -> removePrefix(prefix);
            default -> LOG.debug("Unhandled remove object class {}", type.implementedInterface());
        }
    }

    private void removeEdge(final LinkCase linkCase) {
        /* Get Source and Destination Connected Vertex */
        if (linkCase.getLinkDescriptors() == null) {
            LOG.warn("Missing Link descriptor in link {}, skipping it", linkCase);
            return;
        }
        final var edgeKey = new EdgeKey(getEdgeId(linkCase));
        if (edgeKey == null || edgeKey.getEdgeId() == Uint64.ZERO) {
            LOG.warn("Unable to get the Edge Key from link {}, skipping it", linkCase);
            return;
        }

        LOG.debug("Deleted Edge {} from TED[{}]", edgeKey, cgraph);
        cgraph.deleteEdge(edgeKey);
    }

    private void removeVertex(final NodeCase nodeCase) {
        final var vertexKey = new VertexKey(getVertexId(nodeCase.getNodeDescriptors().getCRouterIdentifier()));
        if (vertexKey == null || vertexKey.getVertexId() == Uint64.ZERO) {
            LOG.warn("Unable to get Vertex Key from descriptor {}, skipping it", nodeCase.getNodeDescriptors());
            return;
        }

        LOG.debug("Deleted Vertex {} in TED[{}]", vertexKey, cgraph);
        cgraph.deleteVertex(vertexKey);
    }

    private void removePrefix(final PrefixCase prefixCase) {
        final var ippfx = prefixCase.getPrefixDescriptors().getIpReachabilityInformation();
        if (ippfx == null) {
            LOG.warn("IP reachability not present in prefix {}, skipping it", prefixCase);
            return;
        }

        LOG.debug("Deleted prefix {} in TED[{}]", ippfx, cgraph);
        cgraph.deletePrefix(ippfx);
    }

    /**
     * Get Vertex in the Graph by the OSPF Router ID or IS-IS-System ID.
     *
     * @param routerID  The Router Identifier entry
     *
     * @return Vertex in the Connected Graph that corresponds to this Router ID. Vertex is created if not found.
     */
    private static Uint64 getVertexId(final CRouterIdentifier routerID) {
        Uint64 rid = Uint64.ZERO;

        if (routerID instanceof IsisNodeCase) {
            final byte[] isoId = ((IsisNodeCase) routerID).getIsisNode().getIsoSystemId().getValue();
            final byte[] convert = {0, 0, isoId[0], isoId[1], isoId[2], isoId[3], isoId[4], isoId[5]};
            rid = Uint64.fromLongBits(ByteBuffer.wrap(convert).getLong());
        }
        if (routerID instanceof OspfNodeCase) {
            rid = ((OspfNodeCase) routerID).getOspfNode().getOspfRouterId().toUint64();
        }

        LOG.debug("Get Vertex Identifier {}", rid);
        return rid;
    }

    private static DecimalBandwidth bandwithToDecimalBandwidth(final Bandwidth bw) {
        return new DecimalBandwidth(ProtocolUtil.bandwidthToDecimal64(bw));
    }

    private static Uint64 ipv4ToKey(final Ipv4InterfaceIdentifier ifId) {
        return Uint32.fromIntBits(IetfInetUtil.ipv4AddressNoZoneBits(ifId)).toUint64();
    }

    @VisibleForTesting
    static Uint64 ipv6ToKey(final Ipv6InterfaceIdentifier ifId) {
        final byte[] ip = IetfInetUtil.ipv6AddressNoZoneBytes(ifId);
        // Keep only the lower 64bits from the IP address, i.e. we skip first Long.BYTES bytes
        return Uint64.fromLongBits(ByteBuffer.wrap(ip, Long.BYTES, Long.BYTES).getLong());
    }

    @Override
    protected DataObjectReference<LinkstateRoute> getRouteWildcard(final DataObjectIdentifier<Tables> tablesId) {
        return tablesId.toBuilder().toReferenceBuilder()
            .child(LinkstateRoutesCase.class, LinkstateRoutes.class)
            .child(LinkstateRoute.class)
            .build();
    }

    @Override
    protected void clearTopology() {
        cgraph.clear();
    }
}
