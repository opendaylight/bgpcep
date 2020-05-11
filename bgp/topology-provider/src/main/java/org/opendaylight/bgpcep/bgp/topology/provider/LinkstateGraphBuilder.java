/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.LinkstateSubsequentAddressFamily;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.CRouterIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.IsisNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.node.identifier.c.router.identifier.OspfNodeCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.isis.adj.flags._case.IsisAdjFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.adj.flags.flags.ospf.adj.flags._case.OspfAdjFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.prefix.sid.tlv.flags.IsisPrefixFlagsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.LocalLabelCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev200120.sid.label.index.sid.label.index.SidCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.DecimalBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Loss;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Vertex.VertexType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.EdgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.EdgeAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.attributes.MinMaxDelay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.attributes.MinMaxDelayBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.attributes.UnreservedBandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.attributes.UnreservedBandwidthKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.Graph.DomainScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.EdgeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.EdgeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Vertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.vertex.SrgbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.bgp.linkstate.topology.type.BgpLinkstateTopologyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.SrlgId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class build the Traffic Engineering Database as a Connected Graph
 * suitable to be used latter by Path Computation algorithms to compute end to
 * end path.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 *
 */

public class LinkstateGraphBuilder extends AbstractTopologyBuilder<LinkstateRoute> {
    private static final TopologyTypes LINKSTATE_TOPOLOGY_TYPE = new TopologyTypesBuilder().addAugmentation(
            new TopologyTypes1Builder().setBgpLinkstateTopology(new BgpLinkstateTopologyBuilder().build()).build())
            .build();

    private static final String UNHANDLED_OBJECT_CLASS = "Unhandled object class {}";

    private static final Logger LOG = LoggerFactory.getLogger(LinkstateGraphBuilder.class);

    private final ConnectedGraphProvider graphProvider;
    private final ConnectedGraph cgraph;

    public LinkstateGraphBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final ConnectedGraphProvider provider) {
        super(dataProvider, locRibReference, topologyId, LINKSTATE_TOPOLOGY_TYPE, LinkstateAddressFamily.class,
                LinkstateSubsequentAddressFamily.class);
        this.graphProvider = requireNonNull(provider);
        this.cgraph = provider.createConnectedGraph("ted://" + topologyId.getValue(),
                DomainScope.IntraDomain);
        /* LinkStateGraphBuilder doesn't write information in the Network Topology tree of the Data Store.
         * This is performed by ConnectedGraphProvider which write element in Graph tree of the Data Store */
        this.networkTopologyTransaction = false;
        LOG.info("Started Traffic Engineering Graph Builder");
    }

    @VisibleForTesting
    LinkstateGraphBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId, final ConnectedGraphProvider provider, final long listenerResetLimitInMillsec,
            final int listenerResetEnforceCounter) {
        super(dataProvider, locRibReference, topologyId, LINKSTATE_TOPOLOGY_TYPE, LinkstateAddressFamily.class,
                LinkstateSubsequentAddressFamily.class, listenerResetLimitInMillsec, listenerResetEnforceCounter);
        this.graphProvider = requireNonNull(provider);
        this.cgraph = provider.createConnectedGraph("ted://" + topologyId.getValue(),
                DomainScope.IntraDomain);
        /* LinkStateGraphBuilder doesn't write information in the Network Topology tree of the Data Store.
         * This is performed by ConnectedGraphProvider which write element in Graph tree of the Data Store */
        this.networkTopologyTransaction = false;
        LOG.info("Started Traffic Engineering Graph Builder");
    }

    @Override
    protected void createObject(final ReadWriteTransaction trans, final InstanceIdentifier<LinkstateRoute> id,
            final LinkstateRoute value) {
        final ObjectType t = value.getObjectType();
        Preconditions.checkArgument(t != null, "Route %s value %s has null object type", id, value);

        if (t instanceof LinkCase) {
            createEdge(value, (LinkCase) t, value.getAttributes());
        } else if (t instanceof NodeCase) {
            createVertex(value, (NodeCase) t, value.getAttributes());
        } else if (t instanceof PrefixCase) {
            createPrefix(value, (PrefixCase) t, value.getAttributes());
        } else {
            LOG.debug(UNHANDLED_OBJECT_CLASS, t.implementedInterface());
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
        final LinkAttributes la;
        final Attributes1 attr = attributes.augmentation(Attributes1.class);
        if (attr != null) {
            final LinkStateAttribute attrType = attr.getLinkStateAttribute();
            if (attrType != null) {
                la = ((LinkAttributesCase) attrType).getLinkAttributes();
            } else {
                return null;
            }
        } else {
            return null;
        }
        return la;
    }

    /**
     * Determine the Source Edge Key from the link descriptor.
     * There is several case: IPv4, IPv6 address or Unnumbered Interface.
     *
     * @param linkCase The Link part of the Linkstate route
     *
     * @return Unique key
     */
    private static Uint64 getEdgeId(final LinkCase linkCase) {
        long key = 0;
        if (linkCase.getLinkDescriptors().getIpv4InterfaceAddress() != null) {
            key = ipv4ToKey(linkCase.getLinkDescriptors().getIpv4InterfaceAddress().getValue());
        }
        if (linkCase.getLinkDescriptors().getIpv6InterfaceAddress() != null) {
            key = ipv6ToKey(linkCase.getLinkDescriptors().getIpv6InterfaceAddress().getValue());
        }
        if (linkCase.getLinkDescriptors().getLinkLocalIdentifier() != null) {
            key = linkCase.getLinkDescriptors().getLinkLocalIdentifier().longValue();
        }
        return Uint64.valueOf(key);
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
        Preconditions.checkArgument(checkLinkState(linkCase), "Missing mandatory information in link {}", linkCase);

        final LinkAttributes la = getLinkAttributes(attributes);
        if (la == null) {
            LOG.warn("Missing attributes in link {} route {}, skipping it", linkCase, value);
            return;
        }

        /* Get Source and Destination Vertex from the graph */
        Uint64 srcId = getVertexId(linkCase.getLocalNodeDescriptors().getCRouterIdentifier());
        Uint64 dstId = getVertexId(linkCase.getRemoteNodeDescriptors().getCRouterIdentifier());
        if (srcId == Uint64.ZERO || dstId == Uint64.ZERO) {
            LOG.warn("Unable to get the Source or Destination Vertex Identifier from link {}, skipping it", linkCase);
            return;
        }

        /* Get Source and Destination Key for the corresponding Edge */
        Uint64 edgeId = getEdgeId(linkCase);
        if (edgeId == Uint64.ZERO) {
            LOG.warn("Unable to get the Edge Identifier from link {}, skipping it", linkCase);
            return;
        }

        /* Add associated Edge */
        Edge edge = new EdgeBuilder().setEdgeId(edgeId).setLocalVertexId(srcId).setRemoteVertexId(dstId)
                .setName(srcId + " - " + dstId)
                .setEdgeAttributes(createEdgeAttributes(la, linkCase.getLinkDescriptors())).build();

        /*
         * Add corresponding Prefix for the Local Address. Remote address will be added with the remote Edge */
        PrefixBuilder prefBuilder = new PrefixBuilder().setVertexId(srcId);
        if (edge.getEdgeAttributes().getLocalAddress().getIpv4Address() != null) {
            prefBuilder.setPrefix(new IpPrefix(
                    new Ipv4Prefix(edge.getEdgeAttributes().getLocalAddress().getIpv4Address().getValue() + "/32")));
        }
        if (edge.getEdgeAttributes().getLocalAddress().getIpv6Address() != null) {
            prefBuilder.setPrefix(new IpPrefix(
                    new Ipv6Prefix(edge.getEdgeAttributes().getLocalAddress().getIpv6Address().getValue() + "/128")));
        }
        Prefix prefix = prefBuilder.build();

        /* Add the Edge in the Connected Graph */
        LOG.info("Add Edge {} and associated Prefix {} in TED[{}]", edge.getName(), prefix.getPrefix(), cgraph);
        cgraph.addEdge(edge);
        cgraph.addPrefix(prefix);
    }

    private static final int MAX_PRIORITY = 8;

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

        if (linkDesc.getIpv4InterfaceAddress() != null) {
            builder.setLocalAddress(new IpAddress(new Ipv4Address(linkDesc.getIpv4InterfaceAddress())));
        }
        if (linkDesc.getIpv6InterfaceAddress() != null) {
            builder.setLocalAddress(new IpAddress(new Ipv6Address(linkDesc.getIpv6InterfaceAddress())));
        }
        if (linkDesc.getIpv4NeighborAddress() != null) {
            builder.setRemoteAddress(new IpAddress(new Ipv4Address(linkDesc.getIpv4NeighborAddress())));
        }
        if (linkDesc.getIpv6NeighborAddress() != null) {
            builder.setRemoteAddress(new IpAddress(new Ipv6Address(linkDesc.getIpv6NeighborAddress())));
        }
        if (linkDesc.getLinkLocalIdentifier() != null) {
            builder.setLocalIdentifier(linkDesc.getLinkLocalIdentifier());
        }
        if (linkDesc.getLinkRemoteIdentifier() != null) {
            builder.setRemoteIdentifier(linkDesc.getLinkRemoteIdentifier());
        }
        if (la.getMetric() != null) {
            builder.setMetric(la.getMetric().getValue());
        }
        if (la.getTeMetric() != null) {
            builder.setTeMetric(la.getTeMetric().getValue());
        }
        if (la.getMaxLinkBandwidth() != null) {
            builder.setMaxLinkBandwidth(bandwithToDecimalBandwidth(la.getMaxLinkBandwidth()));
        }
        if (la.getMaxReservableBandwidth() != null) {
            builder.setMaxResvLinkBandwidth(bandwithToDecimalBandwidth(la.getMaxReservableBandwidth()));
        }
        if (la.getUnreservedBandwidth() != null) {
            int upperBound = Math.min(la.getUnreservedBandwidth().size(), MAX_PRIORITY);
            final List<UnreservedBandwidth> unRsvBw = new ArrayList<>(upperBound);

            for (final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120
                    .UnreservedBandwidth bandwidth : la.nonnullUnreservedBandwidth().values()) {
                unRsvBw.add(new UnreservedBandwidthBuilder()
                        .setBandwidth(bandwithToDecimalBandwidth(bandwidth.getBandwidth()))
                        .withKey(new UnreservedBandwidthKey(bandwidth.getPriority())).build());
            }
            builder.setUnreservedBandwidth(unRsvBw);
        }
        if (la.getAdminGroup() != null) {
            builder.setAdminGroup(la.getAdminGroup().getValue());
        }
        if (la.getLinkDelay() != null) {
            builder.setDelay(new Delay(la.getLinkDelay().getValue()));
        }
        if (la.getLinkMinMaxDelay() != null && la.getLinkMinMaxDelay() != null) {
            MinMaxDelay mmDelay = new MinMaxDelayBuilder()
                    .setMaxDelay(new Delay(la.getLinkMinMaxDelay().getMaxDelay().getValue()))
                    .setMinDelay(new Delay(la.getLinkMinMaxDelay().getMinDelay().getValue())).build();
            builder.setMinMaxDelay(mmDelay);
        }
        if (la.getDelayVariation() != null) {
            builder.setJitter(new Delay(la.getDelayVariation().getValue()));
        }
        if (la.getLinkLoss() != null) {
            builder.setLoss(new Loss(la.getLinkLoss().getValue()));
        }
        if (la.getAvailableBandwidth() != null) {
            builder.setAvailableBandwidth(bandwithToDecimalBandwidth(la.getAvailableBandwidth()));
        }
        if (la.getResidualBandwidth() != null) {
            builder.setResidualBandwidth(bandwithToDecimalBandwidth(la.getResidualBandwidth()));
        }
        if (la.getUtilizedBandwidth() != null) {
            builder.setUtilizedBandwidth(bandwithToDecimalBandwidth(la.getUtilizedBandwidth()));
        }
        if (la.getSharedRiskLinkGroups() != null) {
            List<Uint32> srlgs = new ArrayList<>();
            for (SrlgId srlg : la.getSharedRiskLinkGroups()) {
                srlgs.add(srlg.getValue());
            }
            builder.setSrlgs(srlgs);
        }
        if (la.getSrAdjIds() != null) {
            for (SrAdjIds adj : la.getSrAdjIds()) {
                if (adj.getSidLabelIndex() instanceof LocalLabelCase) {
                    boolean backup = false;
                    if (adj.getFlags() instanceof OspfAdjFlags) {
                        backup = ((OspfAdjFlags) adj.getFlags()).isBackup();
                    }
                    if (adj.getFlags() instanceof IsisAdjFlags) {
                        backup = ((IsisAdjFlags) adj.getFlags()).isBackup();
                    }
                    if (!backup) {
                        builder.setAdjSid(((LocalLabelCase) adj.getSidLabelIndex()).getLocalLabel().getValue());
                    } else {
                        builder.setBackupAdjSid(((LocalLabelCase) adj.getSidLabelIndex()).getLocalLabel().getValue());
                    }
                }
            }
        }
        return builder.build();
    }

    /**
     * Get Node Attributes from Link State Route attributes.
     *
     * @param attributes  The attribute part from the Link State route
     *
     * @return Node Attributes
     */
    private static NodeAttributes getNodeAttributes(final Attributes attributes) {
        final NodeAttributes na;
        final Attributes1 attr = attributes.augmentation(Attributes1.class);
        if (attr != null) {
            final LinkStateAttribute attrType = attr.getLinkStateAttribute();
            if (attrType != null) {
                na = ((NodeAttributesCase) attrType).getNodeAttributes();
            } else {
                return null;
            }
        } else {
            return null;
        }
        return na;
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
    private static Vertex getVertex(final NodeAttributes na, final Uint64 id, final int as) {
        VertexBuilder builder = new VertexBuilder().setVertexId(id).setAsn(Uint32.valueOf(as));
        if (na.getIpv4RouterId() != null) {
            builder.setRouterId(new IpAddress(new Ipv4Address(na.getIpv4RouterId().getValue())));
        }
        if (na.getIpv6RouterId() != null) {
            builder.setRouterId(new IpAddress(new Ipv6Address(na.getIpv6RouterId().getValue())));
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
        if (na.getSrCapabilities() != null) {
            builder.setSrgb(new SrgbBuilder()
                    .setLowerBound(
                            ((LocalLabelCase) na.getSrCapabilities().getSidLabelIndex()).getLocalLabel().getValue())
                    .setRangeSize(na.getSrCapabilities().getRangeSize().getValue())
                    .build());
        }
        if (na.getNodeFlags() != null) {
            if (na.getNodeFlags().isAbr()) {
                builder.setVertexType(VertexType.Abr);
            }
            if (na.getNodeFlags().isExternal()) {
                builder.setVertexType(VertexType.AsbrOut);
            }
        } else {
            builder.setVertexType(VertexType.Standard);
        }
        return builder.build();
    }

    /**
     * Create new Connected Vertex in the Connected Graph.
     *
     * @param value       The complete Linkstate route information
     * @param nodeCase    The node part of the Linkstate route
     * @param attributes  The node attributes
     */
    private void createVertex(final LinkstateRoute value, final NodeCase nodeCase, final Attributes attributes) {
        Preconditions.checkArgument(nodeCase != null, "Missing Node Case. Skip this Node");
        Preconditions.checkArgument(nodeCase.getNodeDescriptors() != null, "Missing Node Descriptors. Skip this Node");

        Uint64 vertexId = getVertexId(nodeCase.getNodeDescriptors().getCRouterIdentifier());
        if (vertexId == Uint64.ZERO) {
            LOG.warn("Unable to get Vertex Identifier from descriptor {}, skipping it", nodeCase.getNodeDescriptors());
            return;
        }

        NodeAttributes na = getNodeAttributes(attributes);
        if (na == null) {
            LOG.warn("Missing attributes in node {} route {}, skipping it", nodeCase, value);
            return;
        }

        int asNumber = 0;
        if (nodeCase.getNodeDescriptors() != null) {
            asNumber = nodeCase.getNodeDescriptors().getAsNumber().getValue().intValue();
        }
        Vertex vertex = getVertex(na, vertexId, asNumber);

        /* Add the Connected Vertex and associated Vertex in the Graph */
        LOG.info("Add Vertex {} in TED[{}]", vertex.getName(), cgraph);
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
        final IpPrefix ippfx = prefixCase.getPrefixDescriptors().getIpReachabilityInformation();
        if (ippfx == null) {
            LOG.warn("IP reachability not present in prefix {} route {}, skipping it", prefixCase, value);
            return;
        }

        /* Verify that all mandatory information are present */
        final PrefixAttributes pa;
        final Attributes1 attr = attributes.augmentation(Attributes1.class);
        if (attr != null) {
            final LinkStateAttribute attrType = attr.getLinkStateAttribute();
            if (attrType != null) {
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
        Uint64 vertexId = getVertexId(prefixCase.getAdvertisingNodeDescriptors().getCRouterIdentifier());
        if (vertexId == Uint64.ZERO) {
            LOG.warn("Unable to get the Vertex Identifier from descriptor {}, skipping it",
                    prefixCase.getAdvertisingNodeDescriptors());
            return;
        }

        /* Create Prefix */
        PrefixBuilder builder = new PrefixBuilder().setVertexId(vertexId);
        if (pa.getSrPrefix() != null && pa.getSrPrefix().getSidLabelIndex() instanceof SidCase) {
            builder.setPrefixSid(((SidCase) pa.getSrPrefix().getSidLabelIndex()).getSid());
            if (pa.getSrPrefix().getFlags() instanceof IsisPrefixFlagsCase) {
                builder.setNodeSid(
                        ((IsisPrefixFlagsCase) pa.getSrPrefix().getFlags()).getIsisPrefixFlags().isNodeSid());
            } else {
                /*
                 * Seems that OSPF Flags are not accessible. Assuming that the
                 * Prefix is a Node SID
                 */
                builder.setNodeSid(true);
            }
        }
        if (ippfx.getIpv4Prefix() != null) {
            builder.setPrefix(new IpPrefix(ippfx.getIpv4Prefix()));
        }
        if (ippfx.getIpv6Prefix() != null) {
            builder.setPrefix(new IpPrefix(ippfx.getIpv6Prefix()));
        }
        Prefix prefix = builder.build();

        /* Add the Prefix to the Connected Vertex within the Connected Graph */
        LOG.info("Add prefix {} in TED[{}]", builder.getPrefix(), cgraph);
        cgraph.addPrefix(prefix);
    }

    @Override
    protected void removeObject(final ReadWriteTransaction trans, final InstanceIdentifier<LinkstateRoute> id,
            final LinkstateRoute value) {
        if (value == null) {
            LOG.error("Empty before-data received in delete data change notification for instance id {}", id);
            return;
        }

        final ObjectType t = value.getObjectType();
        if (t instanceof LinkCase) {
            removeEdge((LinkCase) t);
        } else if (t instanceof NodeCase) {
            removeVertex((NodeCase) t);
        } else if (t instanceof PrefixCase) {
            removePrefix((PrefixCase) t);
        } else {
            LOG.debug(UNHANDLED_OBJECT_CLASS, t.implementedInterface());
        }
    }

    private void removeEdge(final LinkCase linkCase) {
        /* Get Source and Destination Connected Vertex */
        if (linkCase.getLinkDescriptors() == null) {
            LOG.warn("Missing Link descriptor in link {}, skipping it", linkCase);
            return;
        }
        EdgeKey edgeKey = new EdgeKey(getEdgeId(linkCase));
        if (edgeKey == null || edgeKey.getEdgeId() == Uint64.ZERO) {
            LOG.warn("Unable to get the Edge Key from link {}, skipping it", linkCase);
            return;
        }

        LOG.info("Deleted Edge {} from TED[{}]", edgeKey, cgraph);
        cgraph.deleteEdge(edgeKey);
    }

    private void removeVertex(final NodeCase nodeCase) {
        VertexKey vertexKey = new VertexKey(getVertexId(nodeCase.getNodeDescriptors().getCRouterIdentifier()));
        if (vertexKey == null || vertexKey.getVertexId() == Uint64.ZERO) {
            LOG.warn("Unable to get Vertex Key from descriptor {}, skipping it", nodeCase.getNodeDescriptors());
            return;
        }

        LOG.info("Deleted Vertex {} in TED[{}]", vertexKey, cgraph);
        cgraph.deleteVertex(vertexKey);
    }

    private void removePrefix(final PrefixCase prefixCase) {
        final IpPrefix ippfx = prefixCase.getPrefixDescriptors().getIpReachabilityInformation();
        if (ippfx == null) {
            LOG.warn("IP reachability not present in prefix {}, skipping it", prefixCase);
            return;
        }

        LOG.info("Deleted prefix {} in TED[{}]", ippfx, cgraph);
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
        Long rid = 0L;

        if (routerID instanceof IsisNodeCase) {
            byte[] isoId = ((IsisNodeCase) routerID).getIsisNode().getIsoSystemId().getValue();
            final byte[] convert =  {0, 0, isoId[0], isoId[1], isoId[2], isoId[3], isoId[4], isoId[5]};
            rid = ByteBuffer.wrap(convert).getLong();
        }
        if (routerID instanceof OspfNodeCase) {
            rid = ((OspfNodeCase) routerID).getOspfNode().getOspfRouterId().longValue();
        }

        LOG.debug("Get Vertex Identifier {}", rid);
        return Uint64.valueOf(rid);
    }

    private static DecimalBandwidth bandwithToDecimalBandwidth(final Bandwidth bw) {
        return new DecimalBandwidth(BigDecimal.valueOf(ByteBuffer.wrap(bw.getValue()).getFloat()));
    }

    private static long ipv4ToKey(final String str) {
        byte[] ip;
        try {
            ip = ((Inet4Address) Inet4Address.getByName(str)).getAddress();
        } catch (UnknownHostException e) {
            return 0;
        }
        return (0xFF & ip[0]) << 24 | (0xFF & ip[1]) << 16 | (0xFF & ip[2]) << 8 | 0xFF & ip[3];
    }

    private static Long ipv6ToKey(final String str) {
        byte[] ip;
        try {
            ip = ((Inet6Address) Inet6Address.getByName(str)).getAddress();
        } catch (UnknownHostException e) {
            return 0L;
        }
        /* Keep only the lower 64bits from the IP address */
        byte[] lowerIP = {ip[0], ip[1], ip[2], ip[3], ip[4], ip[5], ip[6], ip[7]};
        return ByteBuffer.wrap(lowerIP).getLong();
    }

    @Override
    protected InstanceIdentifier<LinkstateRoute> getRouteWildcard(final InstanceIdentifier<Tables> tablesId) {
        return tablesId.child(LinkstateRoutesCase.class, LinkstateRoutes.class).child(LinkstateRoute.class);
    }

    @Override
    protected void clearTopology() {
        cgraph.clear();
    }
}
