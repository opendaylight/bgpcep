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
import org.opendaylight.algo.impl.CspfPath.CspfPathStatus;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.TeMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.edge.EdgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.te.metric.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.ConstrainedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.DiversityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.constrained.path.DivertPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.path.constraints.Constraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.path.constraints.ConstraintsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.path.constraints.constraints.ExcludeRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.path.constraints.constraints.IncludeRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.path.descriptions.PathDescription;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.path.descriptions.PathDescriptionBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPathComputation implements PathComputationAlgorithm {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPathComputation.class);

    protected final ConnectedGraph graph;

    /* Source and Destination of the path */
    protected CspfPath pathSource = null;
    protected CspfPath pathDestination = null;

    /* Constraints that the path must respect */
    protected Constraints constraints = null;

    /* Computation Status */
    protected ComputationStatus status = ComputationStatus.Idle;

    /* Priority Queue and HashMap to manage path computation */
    protected final PriorityQueue<CspfPath> priorityQueue = new PriorityQueue<>();
    protected final HashMap<Long, CspfPath> processedPath = new HashMap<>();

    protected AbstractPathComputation(final ConnectedGraph graph) {
        this.graph = graph;
    }

    /**
     * Initialize the various parameters for Path Computation, in particular the Source and Destination CspfPath.
     *
     * @param src   Source Vertex Identifier in the Connected Graph
     * @param dst   Destination Vertex Identifier in the Connected Graph
     *
     * @return Computation Status
     */
    protected ComputationStatus initializePathComputation(final VertexKey src, final VertexKey dst) {

        /* Check that source and destination vertexKey are not identical */
        if (src.equals(dst)) {
            LOG.warn("Source and Destination are equal: Abort!");
            status = ComputationStatus.EqualEndpoints;
            return status;
        }

        /*
         * Get the Connected Vertex from the Graph to initialize the source of
         * the Cspf Path
         */
        ConnectedVertex vertex = graph.getConnectedVertex(src.getVertexId().longValue());
        if (vertex == null) {
            LOG.warn("Found no source for Vertex Key {}", src);
            status = ComputationStatus.NoSource;
            return status;
        }
        LOG.debug("Create Path Source with Vertex {}", vertex);
        pathSource = new CspfPath(vertex, vertex).setStatus(CspfPathStatus.InProgress).setCost(0).setDelay(0);

        /*
         * Get the Connected Vertex from the Graph to initialize the destination
         * of the Cspf Path
         */
        vertex = graph.getConnectedVertex(dst.getVertexId().longValue());
        if (vertex == null) {
            LOG.warn("Found no destination for Vertex Key {}", src);
            status = ComputationStatus.NoDestination;
            return status;
        }
        LOG.debug("Create Path Destination with Vertex {}", vertex);
        pathDestination = new CspfPath(pathSource.getSource(), vertex).setStatus(CspfPathStatus.NoPath);

        /* Initialize the Priority Queue, HashMap */
        priorityQueue.clear();
        priorityQueue.add(pathSource);
        processedPath.clear();
        processedPath.put(pathSource.getVertexKey(), pathSource);
        processedPath.put(pathDestination.getVertexKey(), pathDestination);

        status = ComputationStatus.InProgress;
        return status;
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
                if (attributes.getSrLinkAttributes() == null || attributes.getSrLinkAttributes().getAdjSid() == null) {
                    LOG.debug("No SR Adjacency-SID for IPv4");
                    return true;
                }
                break;
            case SrIpv6:
                if (getIpv6NodeSid(edge.getDestination()) == null) {
                    LOG.debug("No Node-SID for IPv6");
                    return true;
                }
                if (attributes.getSrLinkAttributes() == null || attributes.getSrLinkAttributes().getAdjSid6() == null) {
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
                int totalCost = attributes.getTeMetric().getMetric().intValue() + path.getCost();
                if (totalCost > constraints.getTeMetric().intValue()) {
                    LOG.debug("TeMetric {} exceed constraint {}", totalCost, constraints.getTeMetric().intValue());
                    return true;
                }
            }
        }

        /* If specified, check that total Delay up to this edge respects the initial constraints */
        if (constraints.getDelay() != null) {
            if (attributes.getExtendedMetric().getDelay() == null) {
                return true;
            } else {
                int totalDelay = attributes.getExtendedMetric().getDelay().getValue().intValue() + path.getDelay();
                if (totalDelay > constraints.getDelay().getValue().intValue()) {
                    LOG.debug("Delay {} exceed constraint {}", totalDelay,
                            constraints.getDelay().getValue().intValue());
                    return true;
                }
            }
        }

        /* Check that Edge respect Loss constraint */
        if (constraints.getLoss() != null) {
            if (attributes.getExtendedMetric().getLoss() == null || attributes.getExtendedMetric().getLoss()
                    .getValue().intValue() > constraints.getLoss().getValue().intValue()) {
                return true;
            }
        }

        return false;
    }

    private boolean verifyBandwidth(final ConnectedEdge edge, final TeMetric teMetric) {
        if (constraints.getBandwidth() == null) {
            return false;
        }

        int cos = constraints.getClassType() != null ? constraints.getClassType().intValue() : 0;
        if (teMetric.getMaxLinkBandwidth() == null
                || teMetric.getMaxResvLinkBandwidth() == null
                || teMetric.getUnreservedBandwidth() == null
                || teMetric.getUnreservedBandwidth().get(cos) == null) {
            return true;
        }

        /* Get Unreserved Bandwidth for the given Class of Service / Priority */
        Long bandwidth = constraints.getBandwidth().getValue().longValue();
        Long unrsv = 0L;
        for (UnreservedBandwidth unResBw : teMetric.getUnreservedBandwidth()) {
            if (unResBw.getClassType().intValue() == cos) {
                unrsv = unResBw.getBandwidth().getValue().longValue();
                break;
            }
        }
        Long maxBW = teMetric.getMaxLinkBandwidth().getValue().longValue();
        if (bandwidth > List.of(unrsv,
                // maxBW might be on the list but will always be greater
                // than the next items
                maxBW - edge.getCosResvBandwidth(cos), maxBW - edge.getGlobalResvBandwidth(),
                teMetric.getMaxResvLinkBandwidth().getValue().longValue()).stream().mapToLong(v -> v).min()
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
     * Check if Edge need to be prune regarding all constraints including address family.
     *
     * @param edge      Connected Edge to be verified
     * @param path      Current Cspf Path up to this Edge
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

        /* Check that Edge belongs to admin group */
        final var adminGroup = constraints.getAdminGroup();
        if (adminGroup != null && !adminGroup.equals(attributes.getTeMetric().getAdminGroup())) {
            LOG.debug("Not in the requested admin-group");
            return true;
        }

        /* Check that Edge is not part of Exclude Route */
        if (verifyExcludeRoute(edge, attributes)) {
            return true;
        }

        /* Check for Diversity constraint */
        if (constraints.getPathDiversity() != null) {
            switch (constraints.getPathDiversity().getType()) {
                case Link, Srlg:
                    if (edge.isDivert()) {
                        LOG.debug("Edge {} is marked for Diversity", edge.getEdge().getName());
                        return true;
                    }
                    break;
                case Node:
                    if (edge.getDestination().isDivert()) {
                        LOG.debug("Node {} is marked for Diversity", edge.getDestination().getVertex().getName());
                        return true;
                    }
                    break;
                default:
                    break;
            }
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
        if (verifyBandwidth(edge, attributes.getTeMetric())) {
            return true;
        }

        /* OK. All is fine. We can consider this Edge valid, so not to be prune */
        LOG.trace("Edge {} is valid for Constrained Path Computation", edge);
        return false;
    }

    /**
     * Return the MPLS Label corresponding to the Node SID for IPv4 when the Connected Vertex is Segment Routing aware.
     *
     * @param cvertex       Connected Vertex
     *
     * @return MPLS Label if Connected Vertex is Segment Routing aware, null otherwise
     */
    protected @Nullable MplsLabel getIpv4NodeSid(final ConnectedVertex cvertex) {
        /*
         * Check that Vertex is Segment Routing aware
         */
        final var vertex = cvertex.getVertex();
        if (vertex == null || vertex.getSrNodeAttributes() == null || vertex.getSrNodeAttributes().getSrgb() == null) {
            return null;
        }
        /*
         * Find in Prefix List Node SID attached to the IPv4 of the next Vertex
         * and return the MPLS Label that corresponds to the index in the SRGB
         */
        final var prefixes = cvertex.getPrefixes();
        if (prefixes != null) {
            for (Prefix prefix : prefixes) {
                if (prefix.getSrPrefixAttributes() == null) {
                    continue;
                }
                final var prefixSid = prefix.getSrPrefixAttributes().getPrefixSid();
                final var nodeSid = prefix.getSrPrefixAttributes().getPrefixSrFlags().getNodeSid();
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
     * Return the MPLS Label corresponding to the Node SID for IPv6 when the Connected Vertex is Segment Routing aware.
     *
     * @param cvertex       Connected Vertex
     *
     * @return MPLS Label if Connected Vertex is Segment Routing aware, null otherwise
     */
    protected @Nullable MplsLabel getIpv6NodeSid(final ConnectedVertex cvertex) {
        /*
         * Check that Vertex is Segment Routing aware
         */
        final var vertex = cvertex.getVertex();
        if (cvertex == null || vertex.getSrNodeAttributes() == null || vertex.getSrNodeAttributes().getSrgb() == null) {
            return null;
        }
        /*
         * Find in Prefix List Node SID attached to the IPv6 of the next Vertex
         * and return the MPLS Label that corresponds to the index in the SRGB
         */
        final var prefixes = cvertex.getPrefixes();
        if (prefixes != null) {
            for (var prefix : cvertex.getPrefixes()) {
                if (prefix.getSrPrefixAttributes() == null) {
                    continue;
                }
                final var prefixSid = prefix.getSrPrefixAttributes().getPrefixSid();
                final var nodeSid = prefix.getSrPrefixAttributes().getPrefixSrFlags().getNodeSid();
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
     * @param edges List of Connected Edges
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
                            .setSid(edge.getEdge().getEdgeAttributes().getSrLinkAttributes().getAdjSid())
                            .build();
                case SrIpv6 -> new PathDescriptionBuilder()
                            .setIpv6(edge.getEdge().getEdgeAttributes().getLocalAddress6())
                            .setRemoteIpv6(edge.getEdge().getEdgeAttributes().getRemoteAddress6())
                            .setSid(edge.getEdge().getEdgeAttributes().getSrLinkAttributes().getAdjSid6())
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

    protected ConstrainedPath toConstrainedPath(final CspfPath path) {
        final var cbuilder = new ConstrainedPathBuilder()
            .setSource(pathSource.getVertex().getVertex().getVertexId())
            .setDestination(pathDestination.getVertex().getVertex().getVertexId())
            .setConstraints(constraints);

        if (path == null) {
            return cbuilder.setStatus(ComputationStatus.NoPath).build();
        }

        if (status != ComputationStatus.Completed) {
            return cbuilder.setStatus(status).build();
        }

        return cbuilder
            .setStatus(ComputationStatus.Completed)
            .setPathDescription(getPathDescription(path.getPath()))
            .setComputedMetric(constraints.getMetric() != null ? Uint32.valueOf(path.getCost()) : null)
            .setComputedTeMetric(constraints.getTeMetric() != null ? Uint32.valueOf(path.getCost()) : null)
            .setComputedDelay(constraints.getDelay() != null ? new Delay(Uint32.valueOf(path.getDelay())) : null)
            .build();
    }

    protected ConstrainedPath toConstrainedPath(final CspfPath primary, final CspfPath secondary) {
        final var cbuilder = new ConstrainedPathBuilder().setConstraints(constraints);

        if (primary == null) {
            return cbuilder
                .setSource(pathSource.getVertex().getVertex().getVertexId())
                .setDestination(pathDestination.getVertex().getVertex().getVertexId())
                .setStatus(ComputationStatus.NoPath).build();
        }
        cbuilder.setSource(primary.getSource().getVertex().getVertexId())
            .setDestination(primary.getVertex().getVertex().getVertexId());

        if (secondary == null || status != ComputationStatus.Completed) {
            return cbuilder.setStatus(ComputationStatus.Completed)
                .setPathDescription(getPathDescription(primary.getPath()))
                .setComputedMetric(constraints.getMetric() != null ? Uint32.valueOf(primary.getCost()) : null)
                .setComputedTeMetric(constraints.getTeMetric() != null ? Uint32.valueOf(primary.getCost()) : null)
                .setComputedDelay(constraints.getDelay() != null ? new Delay(Uint32.valueOf(primary.getDelay())) : null)
                .setDivertPath(new DivertPathBuilder()
                    .setStatus(ComputationStatus.NoPath)
                    .setSource(constraints.getPathDiversity().getSource())
                    .setDestination(constraints.getPathDiversity().getDestination())
                    .build())
                .build();
        }

        return cbuilder
            .setStatus(ComputationStatus.Completed)
            .setPathDescription(getPathDescription(primary.getPath()))
            .setComputedMetric(constraints.getMetric() != null ? Uint32.valueOf(primary.getCost()) : null)
            .setComputedTeMetric(constraints.getTeMetric() != null ? Uint32.valueOf(primary.getCost()) : null)
            .setComputedDelay(constraints.getDelay() != null ? new Delay(Uint32.valueOf(primary.getDelay())) : null)
            .setDivertPath(new DivertPathBuilder()
                .setStatus(ComputationStatus.Completed)
                .setSource(secondary.getSource().getVertex().getVertexId())
                .setDestination(secondary.getVertex().getVertex().getVertexId())
                .setPathDescription(getPathDescription(secondary.getPath()))
                .setComputedMetric(constraints.getMetric() != null ? Uint32.valueOf(secondary.getCost()) : null)
                .setComputedTeMetric(constraints.getTeMetric() != null ? Uint32.valueOf(secondary.getCost()) : null)
                .setComputedDelay(constraints.getDelay() != null
                    ? new Delay(Uint32.valueOf(secondary.getDelay())) : null)
                .build())
            .build();
    }

    /* Merge two constrained paths: paths are merged and additive metrics summed. */
    private static CspfPath mergePath(final CspfPath cp1, final CspfPath cp2) {

        if ((cp1 == null) || (cp2 == null)) {
            return cp2;
        }

        /* Add CP1 path to CP2 */
        cp2.addPath(cp1.getPath());

        /* Adjust Metrics */
        cp2.setCost(cp1.getCost() + cp2.getCost());
        cp2.setDelay(cp1.getDelay() + cp2.getDelay());

        return cp2;
    }

    /*
     * Adjust Metric, TE Metric and Delay (if set) constraints to remaining portion
     * i.e. substract what have been already consumed by the previous part of the Computed Path.
     */
    private static Constraints adjustConstraints(final CspfPath cp, final Constraints cts) {
        final ConstraintsBuilder ctsBuilder = new ConstraintsBuilder(cts);
        if (cts.getMetric() != null) {
            ctsBuilder.setMetric(Uint32.valueOf(cts.getMetric().intValue() - cp.getCost()));
        }
        if (cts.getTeMetric() != null) {
            ctsBuilder.setTeMetric(Uint32.valueOf(cts.getTeMetric().intValue() - cp.getCost()));
        }
        if (cts.getDelay() != null) {
            ctsBuilder.setDelay(new Delay(Uint32.valueOf(cts.getDelay().getValue().intValue() - cp.getDelay())));
        }
        return ctsBuilder.build();
    }

    @Override
    public ConstrainedPath computeP2pPath(final VertexKey source, final VertexKey destination,
            final Constraints cts) {

        /* Get Path Constraints */
        constraints = cts;

        /* Return Path Diversity computation if diversity is requested */
        if (constraints.getPathDiversity() != null) {
            return computeDivertPaths(source, destination, cts);
        }

        /* Return simple path computation if there is no Include Route defined */
        final var includeRoute = constraints.getIncludeRoute();
        if (includeRoute == null || includeRoute.isEmpty()) {
            return toConstrainedPath(computeSimplePath(source, destination));
        }

        /*
         * Start by computing Path from source to the first Include Route address,
         * then, loop on include route address list and finish by computing path
         * between last include route address to the destination.
         */
        final var it = includeRoute.iterator();
        var skey = source;
        CspfPath segmentPath = null;
        do {
            var dkey = getVertexKey(it.next(), constraints.getAddressFamily());
            segmentPath = mergePath(segmentPath, computeSimplePath(skey, dkey));
            if (status == ComputationStatus.NoPath || segmentPath == null) {
                LOG.warn("No path found for segment {} to {}", skey, dkey);
                return toConstrainedPath(segmentPath);
            }
            constraints = adjustConstraints(segmentPath, cts);
            skey = dkey;
        } while (it.hasNext());
        segmentPath = mergePath(segmentPath, computeSimplePath(skey, destination));
        return toConstrainedPath(segmentPath);
    }

    /* Utility functions for Path Diversity computation */
    protected void setEdgesDiversity(final List<ConnectedEdge> edges) {
        if (constraints.getPathDiversity() == null || constraints.getPathDiversity().getType() != DiversityType.Link) {
            return;
        }

        for (var edge: edges) {
            LOG.debug("Set Diversity to Edge {}", edge.getEdge().getName());
            edge.setDiversity(true);
        }
    }

    protected void resetEdgesDiversity(final List<ConnectedEdge> edges) {
        for (var edge: edges) {
            LOG.debug("Reset Diversity to Edge {}", edge.getEdge().getName());
            edge.setDiversity(false);
        }
    }

    protected void setVerticesDiversity(final List<ConnectedEdge> edges) {
        if (constraints.getPathDiversity() == null || constraints.getPathDiversity().getType() != DiversityType.Node) {
            return;
        }

        for (var edge: edges) {
            LOG.debug("Set Diversity to Node {}", edge.getDestination().getVertex().getName());
            if (edge.getDestination().getVertex().getVertexId().longValue() != pathDestination.getVertexKey()) {
                edge.getDestination().setDiversity(true);
            }
        }
    }

    protected void resetVerticesDiversity(final List<ConnectedEdge> edges) {
        for (var edge: edges) {
            LOG.debug("Reset Diversity to Node {}", edge.getDestination().getVertex().getName());
            edge.getDestination().setDiversity(false);
        }
    }

    protected int getComputedMetric(final List<ConnectedEdge> path) {
        if (constraints.getMetric() == null) {
            return 0;
        }
        int totalMetric = 0;
        for (var edge : path) {
            final var metric = edge.getEdge().getEdgeAttributes().getMetric();
            if (metric != null) {
                totalMetric += metric.intValue();
            }
        }
        return totalMetric;
    }

    protected int getComputedTeMetric(final List<ConnectedEdge> path) {
        if (constraints.getTeMetric() == null) {
            return 0;
        }
        int totalTeMetric = 0;
        for (var edge : path) {
            final var teMetric = edge.getEdge().getEdgeAttributes().getTeMetric().getMetric();
            if (teMetric != null) {
                totalTeMetric += teMetric.intValue();
            }
        }
        return totalTeMetric;
    }

    protected int getComputedDelay(final List<ConnectedEdge> path) {
        if (constraints.getDelay() == null) {
            return 0;
        }
        int totalDelay = 0;
        for (var edge : path) {
            final var delay = edge.getEdge().getEdgeAttributes().getExtendedMetric().getDelay();
            if (delay != null) {
                totalDelay += delay.getValue().intValue();
            }
        }
        return totalDelay;
    }

    protected ConstrainedPath combineDivertPaths(final CspfPath pcp, final CspfPath scp) {
        if (pcp == null || scp == null) {
            LOG.warn("One of the paths is null! Abort combining divert paths.");
            return toConstrainedPath(pcp, scp);
        }

        final var cp1 = new CspfPath(pcp.getSource(), pcp.getVertex());
        final var cp2 = new CspfPath(scp.getSource(), scp.getVertex());
        final var p1 = new ArrayList<>(pcp.getPath());
        final var p2 = new ArrayList<>(scp.getPath());

        cp1.addConnectedEdge(p1.removeFirst());
        cp2.addConnectedEdge(p2.removeFirst());
        boolean revert = false;

        while (!p1.isEmpty() && !p2.isEmpty()) {
            var e1 = p1.removeFirst();
            var e2 = p2.removeFirst();
            /* Skip Edges if P1 and P2 share the same reverse edge */
            if (e1.getEdge().getRemoteVertexId().equals(e2.getEdge().getLocalVertexId())) {
                LOG.debug("Found common reverse edge: {} and {}", e1.getEdge().getName(), e2.getEdge().getName());
                revert = !revert;
                e1 = p1.removeFirst();
                e2 = p2.removeFirst();
            }
            /* Add next Edges to the P'1 and P'2 regarding the revert flag */
            if (revert) {
                cp1.addConnectedEdge(e2);
                cp2.addConnectedEdge(e1);
            } else {
                cp1.addConnectedEdge(e1);
                cp2.addConnectedEdge(e2);
            }
        }
        /* Complete P'1 or P'2 with remaining part of P1 or P2 */
        if (!p1.isEmpty() && p2.isEmpty()) {
            if (revert) {
                cp2.addPath(p1);
            } else {
                cp1.addPath(p1);
            }
        }
        if (p1.isEmpty() && !p2.isEmpty()) {
            if (revert) {
                cp2.addPath(p2);
            } else {
                cp1.addPath(p2);
            }
        }

        /* Compute Metrics of new Paths */
        if (constraints.getTeMetric() != null) {
            cp1.setCost(getComputedTeMetric(cp1.getPath()));
            cp2.setCost(getComputedTeMetric(cp2.getPath()));
        } else {
            cp1.setCost(getComputedMetric(cp1.getPath()));
            cp2.setCost(getComputedMetric(cp2.getPath()));
        }
        cp1.setDelay(getComputedDelay(cp1.getPath()));
        cp2.setDelay(getComputedDelay(cp2.getPath()));

        return toConstrainedPath(cp1, cp2);
    }

    /**
     * Compute Divert path algorithms could be simple as: 1) compute first path, 2) remove edges and nodes in graph
     * and 3) compute the second path. Named Remove and Find this simple solution is not optimal and frequently failed.
     * Thus, we use here another approach based on Suurballe's algorithm:
     * - Step1: Compute the first path P1 from graph G(V,E)
     * - Step2: Replace P1 by -P1 in G(V,E) to form a new graph G'(V',E')
     * - Step3: Compute the second path P2 from graph G'(V',E')
     * - Step4: Take the union of P1 and P2, remove from the union the set of links consisting of those P1 links
     *          whose reversed links appear on P2, and vice versa; then group the remaining links into two paths
     *          P1' and P2'
     * In our case, step2 will consist to mark P1 edge's and/or vertice's as "diverted" in Connected Graph
     * as all edges are bi-directionnal and unmark them after step 3.
     */
    @Override
    public ConstrainedPath computeDivertPaths(final VertexKey source, final VertexKey destination,
        final Constraints cts) {

        // Get Path Constraints
        if (cts.getPathDiversity() == null && cts.getPathDiversity().getType() == null) {
            LOG.warn("Diversity constraints not specified. Abort divert path computation.");
            return new ConstrainedPathBuilder().setStatus(ComputationStatus.WrongDiversityType).build();
        }
        constraints = cts;

        // Step 1: compute the primary path
        LOG.debug("Step 1: compute the primary path");
        final CspfPath primaryPath = computeSimplePath(source, destination);
        if (status != ComputationStatus.Completed || primaryPath == null) {
            LOG.warn("Primary path computation failed. Abort divert path computation.");
            return toConstrainedPath(primaryPath);
        }

        // Step 2: mark edges or vertices used by the primary path as diverted
        LOG.debug("Step 2: mark edges or vertices used by the primary path as diverted");
        switch (cts.getPathDiversity().getType()) {
            case Link, Srlg -> setEdgesDiversity(primaryPath.getPath());
            case Node -> setVerticesDiversity(primaryPath.getPath());
            default -> {
                LOG.warn("Unsupported Diversity Type: {}", cts.getPathDiversity().getType());
                return new ConstrainedPathBuilder().setStatus(ComputationStatus.WrongDiversityType).build();
            }
        }

        // Step 3: compute the secondary path with new pair of source and destination if specified
        LOG.debug("Step 3: compute the secondary path");
        CspfPath secondaryPath;
        if (cts.getPathDiversity().getSource() != null) {
            if (cts.getPathDiversity().getDestination() != null) {
                secondaryPath = computeSimplePath(new VertexKey(cts.getPathDiversity().getSource()),
                    new VertexKey(cts.getPathDiversity().getDestination()));
            } else {
                secondaryPath = computeSimplePath(new VertexKey(cts.getPathDiversity().getSource()), destination);
            }
        } else if (cts.getPathDiversity().getDestination() != null) {
            secondaryPath = computeSimplePath(source, new VertexKey(cts.getPathDiversity().getDestination()));
        } else {
            secondaryPath = computeSimplePath(source, destination);
        }

        // Step 3b: reset diversity on edges or vertices used by the primary path
        LOG.debug("Step 3b: reset diversity on edges or vertices used by the primary path");
        switch (cts.getPathDiversity().getType()) {
            case Link, Srlg -> resetEdgesDiversity(primaryPath.getPath());
            case Node -> resetVerticesDiversity(primaryPath.getPath());
            default -> LOG.warn("Unsupported Diversity Type: {}", cts.getPathDiversity().getType());
        }

        // Step 4: combine paths if secondary path computation succeeded
        LOG.debug("Step 4: combine paths if secondary path computation succeeded");
        if (status == ComputationStatus.NoPath || secondaryPath == null) {
            LOG.warn("Secondary path computation failed. Abort divert path computation.");
            return toConstrainedPath(primaryPath, null);
        }
        return combineDivertPaths(primaryPath, secondaryPath);
    }

    protected abstract CspfPath computeSimplePath(VertexKey source, VertexKey destination);
}
