/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.algo.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.algo.PathComputationAlgorithm;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.edge.EdgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.edge.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ConstrainedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.PathConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.constraints.ExcludeRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.constraints.IncludeRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.descriptions.PathDescription;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.path.descriptions.PathDescriptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPathComputation implements PathComputationAlgorithm {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPathComputation.class);

    protected final ConnectedGraph graph;

    /* Source and Destination of the path */
    protected CspfPath pathSource = null;
    protected CspfPath pathDestination = null;

    /* Constraints that the path must respect */
    protected PathConstraints constraints = null;

    /* Priority Queue and HashMap to manage path computation */
    protected final PriorityQueue<CspfPath> priorityQueue = new PriorityQueue<>();
    protected final HashMap<Long, CspfPath> processedPath = new HashMap<>();

    protected AbstractPathComputation(final ConnectedGraph graph) {
        this.graph = graph;
    }

    /**
     * Initialize the various parameters for Path Computation, in particular the
     * Source and Destination CspfPath.
     *
     * @param src
     *            Source Vertex Identifier in the Connected Graph
     * @param dst
     *            Destination Vertex Identifier in the Connected Graph
     *
     * @return Constrained Path Builder with status set to 'OnGoing' if
     *         initialization success, 'Failed' otherwise
     */
    protected ConstrainedPathBuilder initializePathComputation(final VertexKey src, final VertexKey dst) {
        ConstrainedPathBuilder cpathBuilder = new ConstrainedPathBuilder().setStatus(ComputationStatus.InProgress);

        /* Check that source and destination vertexKey are not identical */
        if (src.equals(dst)) {
            LOG.warn("Source and Destination are equal: Abort!");
            return cpathBuilder.setStatus(ComputationStatus.EqualEndpoints);
        }

        /*
         * Get the Connected Vertex from the Graph to initialize the source of
         * the Cspf Path
         */
        ConnectedVertex vertex = graph.getConnectedVertex(src.getVertexId().longValue());
        if (vertex == null) {
            LOG.warn("Found no source for Vertex Key {}", src);
            return cpathBuilder.setStatus(ComputationStatus.NoSource);
        }
        LOG.debug("Create Path Source with Vertex {}", vertex);
        pathSource = new CspfPath(vertex).setCost(0).setDelay(0);
        cpathBuilder.setSource(vertex.getVertex().getVertexId());

        /*
         * Get the Connected Vertex from the Graph to initialize the destination
         * of the Cspf Path
         */
        vertex = graph.getConnectedVertex(dst.getVertexId().longValue());
        if (vertex == null) {
            LOG.warn("Found no destination for Vertex Key {}", src);
            return cpathBuilder.setStatus(ComputationStatus.NoDestination);
        }
        LOG.debug("Create Path Destination with Vertex {}", vertex);
        pathDestination = new CspfPath(vertex);
        cpathBuilder.setDestination(vertex.getVertex().getVertexId());

        /* Initialize the Priority Queue, HashMap */
        priorityQueue.clear();
        priorityQueue.add(pathSource);
        processedPath.clear();
        processedPath.put(pathSource.getVertexKey(), pathSource);
        processedPath.put(pathDestination.getVertexKey(), pathDestination);

        return cpathBuilder;
    }

    private boolean verifyAddressFamily(final ConnectedEdge edge, final EdgeAttributes attributes) {
        /* Check that Edge belongs to the Address Family of the requested path */
        switch (constraints.getAddressFamily()) {
            case Ipv4:
                if (attributes.getRemoteAddress() == null) {
                    LOG.debug("No Ipv4 address");
                    return true;
                }
                break;
            case Ipv6:
                if (attributes.getRemoteAddress6() == null) {
                    LOG.debug("No Ipv6 address");
                    return true;
                }
                break;
            case SrIpv4:
                if (getIpv4NodeSid(edge.getDestination()) == null) {
                    LOG.debug("No Node-SID for IPv4");
                    return true;
                }
                if (attributes.getAdjSid() == null) {
                    LOG.debug("No SR Adjacency-SID for IPv4");
                    return true;
                }
                break;
            case SrIpv6:
                if (getIpv6NodeSid(edge.getDestination()) == null) {
                    LOG.debug("No Node-SID for IPv6");
                    return true;
                }
                if (attributes.getAdjSid6() == null) {
                    LOG.debug("No SR Adjacency-SID for IPv6");
                    return true;
                }
                break;
            default:
                return true;
        }

        return false;
    }

    private boolean verifyMetrics(final EdgeAttributes attributes, final CspfPath path) {
        /* If specified, check that total TE Metric up to this edge respects the initial constraints */
        if (constraints.getTeMetric() != null) {
            if (attributes.getTeMetric() == null) {
                return true;
            } else {
                int totalCost = attributes.getTeMetric().intValue() + path.getCost();
                if (totalCost > constraints.getTeMetric().intValue()) {
                    LOG.debug("TeMetric {} exceed constraint {}", totalCost, constraints.getTeMetric().intValue());
                    return true;
                }
            }
        }

        /* If specified, check that total Delay up to this edge respects the initial constraints */
        if (constraints.getDelay() != null) {
            if (attributes.getDelay() == null) {
                return true;
            } else {
                int totalDelay = attributes.getDelay().getValue().intValue() + path.getDelay();
                if (totalDelay > constraints.getDelay().getValue().intValue()) {
                    LOG.debug("Delay {} exceed constraint {}", totalDelay,
                            constraints.getDelay().getValue().intValue());
                    return true;
                }
            }
        }

        /* Check that Edge respect Loss constraint */
        if (constraints.getLoss() != null) {
            if (attributes.getLoss() == null
                    || attributes.getLoss().getValue().intValue() > constraints.getLoss().getValue().intValue()) {
                return true;
            }
        }

        return false;
    }

    private boolean verifyBandwidth(final ConnectedEdge edge, final EdgeAttributes attributes) {
        if (constraints.getBandwidth() == null) {
            return false;
        }

        int cos = constraints.getClassType() != null ? constraints.getClassType().intValue() : 0;
        if (attributes.getMaxLinkBandwidth() == null
                || attributes.getMaxResvLinkBandwidth() == null
                || attributes.getUnreservedBandwidth() == null
                || attributes.getUnreservedBandwidth().get(cos) == null) {
            return true;
        }

        /* Get Unreserved Bandwidth for the given Class of Service / Priority */
        Long bandwidth = constraints.getBandwidth().getValue().longValue();
        Long unrsv = 0L;
        for (UnreservedBandwidth unResBw : attributes.getUnreservedBandwidth()) {
            if (unResBw.getClassType().intValue() == cos) {
                unrsv = unResBw.getBandwidth().getValue().longValue();
                break;
            }
        }
        Long maxBW = attributes.getMaxLinkBandwidth().getValue().longValue();
        if (bandwidth > List.of(unrsv,
                // maxBW might be on the list but will always be greater
                // than the next items
                maxBW - edge.getCosResvBandwidth(cos), maxBW - edge.getGlobalResvBandwidth(),
                attributes.getMaxResvLinkBandwidth().getValue().longValue()).stream().mapToLong(v -> v).min()
                .getAsLong()) {
            LOG.debug("Bandwidth constraint is not met");
            return true;
        }

        return false;
    }

    private boolean verifyExcludeRoute(final ConnectedEdge edge, final EdgeAttributes attributes) {
        final var xro = constraints.getExcludeRoute();
        if (xro == null || xro.isEmpty()) {
            return false;
        }

        switch (constraints.getAddressFamily()) {
            case Ipv4, SrIpv4:
                for (ExcludeRoute element : xro) {
                    final Ipv4Address address = element.getIpv4();
                    if (address.equals(attributes.getRemoteAddress())
                            || address.equals(attributes.getLocalAddress())
                            || address.equals(edge.getSource().getVertex().getRouterId())
                            || address.equals(edge.getDestination().getVertex().getRouterId())) {
                        return true;
                    }
                }
                break;
            case Ipv6, SrIpv6:
                for (ExcludeRoute element : xro) {
                    final Ipv6Address address = element.getIpv6();
                    if (address.equals(attributes.getRemoteAddress6())
                            || address.equals(attributes.getLocalAddress6())
                            || address.equals(edge.getSource().getVertex().getRouterId6())
                            || address.equals(edge.getDestination().getVertex().getRouterId6())) {
                        return true;
                    }
                }
                break;
            default:
                return true;
        }

        return false;
    }

    /**
     * Check if Edge need to be prune regarding all constraints including
     * address family.
     *
     * @return True if Edge must be prune, False if Edge must be keep
     */
    protected boolean pruneEdge(final ConnectedEdge edge, final CspfPath path) {
        /* Check that Constraints are initialized */
        if (constraints == null) {
            LOG.warn("Constraints not set");
            return true;
        }

        /* Edge could point to an unknown Vertex e.g. with inter-domain link */
        final var destination = edge.getDestination();
        if (destination == null || destination.getVertex() == null) {
            LOG.debug("No Destination");
            return true;
        }

        /* Check that Edge have attributes */
        final var graphEdge = edge.getEdge();
        final var attributes = graphEdge != null ? graphEdge.getEdgeAttributes() : null;
        if (attributes == null) {
            LOG.debug("No attributes");
            return true;
        }

        /* Check that Edge belongs to the requested address family */
        if (verifyAddressFamily(edge, attributes)) {
            return true;
        }

        /* Check only IGP Metric, if specified, for simple SPF algorithm */
        if (this instanceof ShortestPathFirst) {
            final var metricConstraint = constraints.getMetric();
            if (metricConstraint != null) {
                final var metric = attributes.getMetric();
                if (metric == null) {
                    return true;
                }
                final int totalCost = metric.intValue() + path.getCost();
                final var maxCost = metricConstraint.intValue();
                if (totalCost > maxCost) {
                    LOG.debug("Metric {} exceed constraint {}", totalCost, maxCost);
                    return true;
                }
            }
            LOG.trace("Edge {} is valid for Simple Path Computation", edge);
            return false;
        }

        /* Check that Edge respect Metric constraints */
        if (verifyMetrics(attributes, path)) {
            return true;
        }

        /* Check that Edge meet Bandwidth constraint */
        if (verifyBandwidth(edge, attributes)) {
            return true;
        }

        /* Check that Edge belongs to admin group */
        final var adminGroup = constraints.getAdminGroup();
        if (adminGroup != null && !adminGroup.equals(attributes.getAdminGroup())) {
            LOG.debug("Not in the requested admin-group");
            return true;
        }

        /* Check that Edge is not part of Exclude Route */
        if (verifyExcludeRoute(edge, attributes)) {
            return true;
        }

        /*
         * OK. All is fine. We can consider this Edge valid, so not to be prune
         */
        LOG.trace("Edge {} is valid for Constrained Path Computation", edge);
        return false;
    }

    /**
     * Return the MPLS Label corresponding to the Node SID for IPv4 when the
     * Connected Vertex is Segment Routing aware.
     *
     * @return MPLS Label if Connected Vertex is Segment Routing aware, Null
     *         otherwise
     */
    protected @Nullable MplsLabel getIpv4NodeSid(final ConnectedVertex cvertex) {
        /*
         * Check that Vertex is Segment Routing aware
         */
        final var vertex = cvertex.getVertex();
        if (vertex == null || vertex.getSrgb() == null) {
            return null;
        }
        /*
         * Find in Prefix List Node SID attached to the IPv4 of the next Vertex
         * and return the MPLS Label that corresponds to the index in the SRGB
         */
        final var prefixes = cvertex.getPrefixes();
        if (prefixes != null) {
            for (Prefix prefix : prefixes) {
                final var prefixSid = prefix.getPrefixSid();
                final var nodeSid = prefix.getNodeSid();
                if (prefixSid != null && nodeSid != null) {
                    if (nodeSid && prefix.getPrefix().getIpv4Prefix() != null) {
                        return new MplsLabel(prefixSid);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return the MPLS Label corresponding to the Node SID for IPv6 when the
     * Connected Vertex is Segment Routing aware.
     *
     * @return MPLS Label if Connected Vertex is Segment Routing aware, Null
     *         otherwise
     */
    protected @Nullable MplsLabel getIpv6NodeSid(final ConnectedVertex cvertex) {
        /*
         * Check that Vertex is Segment Routing aware
         */
        final var vertex = cvertex.getVertex();
        if (cvertex == null || vertex.getSrgb() == null) {
            return null;
        }
        /*
         * Find in Prefix List Node SID attached to the IPv6 of the next Vertex
         * and return the MPLS Label that corresponds to the index in the SRGB
         */
        final var prefixes = cvertex.getPrefixes();
        if (prefixes != null) {
            for (var prefix : cvertex.getPrefixes()) {
                final var prefixSid = prefix.getPrefixSid();
                final var nodeSid = prefix.getNodeSid();
                if (prefixSid != null && nodeSid != null) {
                    if (nodeSid && prefix.getPrefix().getIpv6Prefix() != null) {
                        return new MplsLabel(prefixSid);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Convert List of Connected Edges into a Path Description as a List of
     * IPv4, IPv6 or MPLS Label depending of the requested Address Family.
     *
     * @param edges
     *            List of Connected Edges
     *
     * @return Path Description
     */
    protected List<PathDescription> getPathDescription(final List<ConnectedEdge> edges) {
        final var list = new ArrayList<PathDescription>(edges.size());
        for (var edge : edges) {
            final var pathDesc = switch (constraints.getAddressFamily()) {
                case Ipv4 -> new PathDescriptionBuilder()
                            .setIpv4(edge.getEdge().getEdgeAttributes().getLocalAddress())
                            .setRemoteIpv4(edge.getEdge().getEdgeAttributes().getRemoteAddress())
                            .build();
                case Ipv6 -> new PathDescriptionBuilder()
                            .setIpv6(edge.getEdge().getEdgeAttributes().getLocalAddress6())
                            .setRemoteIpv6(edge.getEdge().getEdgeAttributes().getRemoteAddress6())
                            .build();
                case SrIpv4 -> new PathDescriptionBuilder()
                            .setIpv4(edge.getEdge().getEdgeAttributes().getLocalAddress())
                            .setRemoteIpv4(edge.getEdge().getEdgeAttributes().getRemoteAddress())
                            .setSid(edge.getEdge().getEdgeAttributes().getAdjSid())
                            .build();
                case SrIpv6 -> new PathDescriptionBuilder()
                            .setIpv6(edge.getEdge().getEdgeAttributes().getLocalAddress6())
                            .setRemoteIpv6(edge.getEdge().getEdgeAttributes().getRemoteAddress6())
                            .setSid(edge.getEdge().getEdgeAttributes().getAdjSid6())
                            .build();
            };
            list.add(pathDesc);
        }
        return list;
    }

    private VertexKey getVertexKey(final IncludeRoute iro, final AddressFamily af) {
        final var address = switch (af) {
            case Ipv4, SrIpv4 -> new IpAddress(iro.getIpv4());
            case Ipv6, SrIpv6 -> new IpAddress(iro.getIpv6());
        };

        final var vertex = graph.getConnectedVertex(address);
        return vertex != null ? vertex.getVertex().key() : null;
    }

    private static ConstrainedPath mergePath(final ConstrainedPath cp1, final ConstrainedPath cp2) {
        final var mergePathDesc = new ArrayList<>(cp1.getPathDescription());
        mergePathDesc.addAll(cp2.getPathDescription());

        final var cp1status = cp1.getStatus();
        final var cp2status = cp2.getStatus();
        return new ConstrainedPathBuilder(cp1)
            .setPathDescription(mergePathDesc)
            .setStatus(cp1status.equals(cp2status) ? cp1status : cp2status)
            .build();
    }

    @Override
    public ConstrainedPath computeP2pPath(final VertexKey source, final VertexKey destination,
            final PathConstraints cts) {
        constraints = cts;

        final var includeRoute = constraints.getIncludeRoute();
        if (includeRoute == null || includeRoute.isEmpty()) {
            return computeSimplePath(source, destination);
        }

        final var it = includeRoute.iterator();
        /* Start by computing Path from source to the first Include Route address */
        VertexKey key = getVertexKey(it.next(), constraints.getAddressFamily());
        ConstrainedPath ctsPath = computeSimplePath(source, key);
        if (ctsPath.getStatus() != ComputationStatus.Completed) {
            return ctsPath;
        }

        /* Then, loop other subsequent Include Route address */
        while (it.hasNext()) {
            VertexKey next = getVertexKey(it.next(), constraints.getAddressFamily());
            ctsPath = mergePath(ctsPath, computeSimplePath(key, next));
            if (ctsPath.getStatus() != ComputationStatus.Completed) {
                return ctsPath;
            }
            key = next;
        }

        /* Finish path up to the destination */
        return mergePath(ctsPath, computeSimplePath(key, destination));
    }

    protected abstract ConstrainedPath computeSimplePath(VertexKey source, VertexKey destination);
}
