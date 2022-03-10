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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.EdgeAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.edge.attributes.UnreservedBandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.PathConstraints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.path.descriptions.PathDescription;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.path.descriptions.PathDescriptionBuilder;
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
        if (edge.getDestination() == null || edge.getDestination().getVertex() == null) {
            LOG.debug("No Destination");
            return true;
        }

        /* Check that Edge have attributes */
        EdgeAttributes attributes = edge.getEdge() != null ? edge.getEdge().getEdgeAttributes() : null;
        if (attributes == null) {
            LOG.debug("No attributes");
            return true;
        }

        /* Check that Edge belongs to the requested address family */
        switch (constraints.getAddressFamily()) {
            case Ipv4:
                if (attributes.getRemoteAddress() == null
                        || attributes.getRemoteAddress().getIpv4Address() == null) {
                    LOG.debug("No Ipv4 address");
                    return true;
                }
                break;
            case Ipv6:
                if (attributes.getRemoteAddress() == null
                        || attributes.getRemoteAddress().getIpv6Address() == null) {
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
                    LOG.debug("No Adjacency-SID");
                    return true;
                }
                break;
            case SrIpv6:
                if (getIpv6NodeSid(edge.getDestination()) == null) {
                    LOG.debug("No Node-SID for IPv6");
                    return true;
                }
                if (attributes.getAdjSid() == null) {
                    LOG.debug("No SR Adjacency-SID");
                    return true;
                }
                break;
            default:
                return true;
        }

        /* Skip checking other Constraints for simple SPF algorithm */
        if (this instanceof ShortestPathFirst) {
            LOG.trace("Edge {} is valid for Simple Path Computation", edge);
            return false;
        }

        /*
         * If specified, check that total TE Metric up to this edge respects the
         * initial constraints
         */
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

        /*
         * If specified, check that total Delay up to this edge respects the
         * initial constraints
         */
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

        /* Check that Edge meet Bandwidth constraint */
        int cos = 0;
        if (constraints.getClassType() != null) {
            cos = constraints.getClassType().intValue();
        }
        if (constraints.getBandwidth() != null) {
            if (attributes.getMaxLinkBandwidth() == null || attributes.getMaxResvLinkBandwidth() == null
                    || attributes.getUnreservedBandwidth() == null
                    || attributes.getUnreservedBandwidth().get(cos) == null) {
                return true;
            } else {
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
                if (unrsv < bandwidth
                        || maxBW < bandwidth
                        || (maxBW - edge.getCosResvBandwidth(cos)) < bandwidth
                        || (maxBW - edge.getGlobalResvBandwidth()) < bandwidth
                        || attributes.getMaxResvLinkBandwidth().getValue().longValue() < bandwidth) {
                    LOG.debug("Bandwidth constraint is not met");
                    return true;
                }
            }
        }

        /* Check that Edge belongs to admin group */
        if (constraints.getAdminGroup() != null
                && !constraints.getAdminGroup().equals(attributes.getAdminGroup())) {
            LOG.debug("Not in the requested admin-group");
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
        if (cvertex.getVertex() == null || cvertex.getVertex().getSrgb() == null) {
            return null;
        }
        /*
         * Find in Prefix List Node SID attached to the IPv4 of the next Vertex
         * and return the MPLS Label that corresponds to the index in the SRGB
         */
        if (cvertex.getPrefixes() == null) {
            return null;
        }
        for (Prefix prefix : cvertex.getPrefixes()) {
            if (prefix.getPrefixSid() == null || prefix.getNodeSid() == null) {
                continue;
            }
            if (prefix.getNodeSid() && prefix.getPrefix().getIpv4Prefix() != null) {
                return new MplsLabel(Uint32.valueOf(prefix.getPrefixSid().intValue()));
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
        if (cvertex.getVertex() == null || cvertex.getVertex().getSrgb() == null) {
            return null;
        }
        /*
         * Find in Prefix List Node SID attached to the IPv6 of the next Vertex
         * and return the MPLS Label that corresponds to the index in the SRGB
         */
        if (cvertex.getPrefixes() == null) {
            return null;
        }
        for (Prefix prefix : cvertex.getPrefixes()) {
            if (prefix.getPrefixSid() == null || prefix.getNodeSid() == null) {
                continue;
            }
            if (prefix.getNodeSid() && prefix.getPrefix().getIpv6Prefix() != null) {
                return new MplsLabel(Uint32.valueOf(prefix.getPrefixSid().intValue()));
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
        ArrayList<PathDescription> list = new ArrayList<>();

        for (ConnectedEdge edge : edges) {
            PathDescription pathDesc = null;
            switch (constraints.getAddressFamily()) {
                case Ipv4:
                    pathDesc = new PathDescriptionBuilder()
                            .setIpv4(edge.getEdge().getEdgeAttributes().getLocalAddress().getIpv4Address())
                            .setRemoteIpv4(edge.getEdge().getEdgeAttributes().getRemoteAddress().getIpv4Address())
                            .build();
                    break;
                case Ipv6:
                    pathDesc = new PathDescriptionBuilder()
                            .setIpv6(edge.getEdge().getEdgeAttributes().getLocalAddress().getIpv6Address())
                            .setRemoteIpv6(edge.getEdge().getEdgeAttributes().getRemoteAddress().getIpv6Address())
                            .build();
                    break;
                case SrIpv4:
                    pathDesc = new PathDescriptionBuilder()
                            .setIpv4(edge.getEdge().getEdgeAttributes().getLocalAddress().getIpv4Address())
                            .setRemoteIpv4(edge.getEdge().getEdgeAttributes().getRemoteAddress().getIpv4Address())
                            .setSid(edge.getEdge().getEdgeAttributes().getAdjSid())
                            .build();
                    break;
                case SrIpv6:
                    pathDesc = new PathDescriptionBuilder()
                            .setIpv6(edge.getEdge().getEdgeAttributes().getLocalAddress().getIpv6Address())
                            .setRemoteIpv6(edge.getEdge().getEdgeAttributes().getRemoteAddress().getIpv6Address())
                            .setSid(edge.getEdge().getEdgeAttributes().getAdjSid())
                            .build();
                    break;
                default:
                    break;
            }
            list.add(pathDesc);
        }
        return list;
    }

    @Override
    public abstract ConstrainedPath computeP2pPath(VertexKey source, VertexKey destination,
            PathConstraints constraints);

}
