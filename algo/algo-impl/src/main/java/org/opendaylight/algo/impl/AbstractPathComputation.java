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

public abstract class AbstractPathComputation implements PathComputationAlgorithm {

    protected final ConnectedGraph graph;

    /* Source and Destination of the path */
    protected CspfPath pathSource = null;
    protected CspfPath pathDestination = null;
    /* Constraints that the path must respect */
    protected PathConstraints constraints = null;
    /* Priority Queue and HashMap to manage path computation */
    protected PriorityQueue<CspfPath> priorityQueue;
    protected HashMap<Long, CspfPath> processedPath;

    protected AbstractPathComputation(ConnectedGraph graph) {
        this.graph = graph;
        priorityQueue = new PriorityQueue<CspfPath>();
        processedPath = new HashMap<Long, CspfPath>();
    }

    /**
     * Initialize the various parameters for Path Computation, in particular the Source and Destination CspfPath.
     *
     * @param src Source Vertex Identifier in the Connected Graph
     * @param dst Destination Vertex Identifier in the Connected Graph
     *
     * @return Constrained Path Builder with status set to 'OnGoing' if initialization success, 'Failed' otherwise
     */
    protected ConstrainedPathBuilder initializePathComputation(VertexKey src, VertexKey dst) {
        ConstrainedPathBuilder cpathBuilder = new ConstrainedPathBuilder().setStatus(ComputationStatus.OnGoing);

        /* Check that source and destination vertexKey are not identical */
        if (src.equals(dst)) {
            cpathBuilder.setStatus(ComputationStatus.Failed);
            return cpathBuilder;
        }

        /* Get the Connected Vertex from the Graph to initialize the source of the Cspf Path */
        ConnectedVertex vertex = null;
        vertex = graph.getConnectedVertex(src.getVertexId().longValue());
        if (vertex == null) {
            cpathBuilder.setStatus(ComputationStatus.Failed);
            return cpathBuilder;
        }
        pathSource = new CspfPath(vertex).setCost(0).setDelay(0);
        cpathBuilder.setSource(vertex.getVertex().getVertexId());

        /* Get the Connected Vertex from the Graph to initialize the destination of the Cspf Path */
        vertex = graph.getConnectedVertex(dst.getVertexId().longValue());
        if (vertex == null) {
            cpathBuilder.setStatus(ComputationStatus.Failed);
            return cpathBuilder;
        }
        pathDestination = new CspfPath(vertex).setCost(-1).setDelay(-1);
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
     * Check if Edge need to be prune regarding all constraints including address family.
     *
     * @return True if Edge must be prune, False if Edge must be keep
     */
    protected boolean pruneEdge(ConnectedEdge edge, CspfPath path) {
        /* Check that Constraints are initialized */
        if (constraints == null) {
            return false;
        }

        /* Check that Edge have attributes */
        if (edge.getEdge().getEdgeAttributes() == null) {
            return true;
        }
        EdgeAttributes attributes = edge.getEdge().getEdgeAttributes();

        /* Check that total Cost respect the initial constraints */
        int totalCost = attributes.getTeMetric().intValue() + path.getCost();
        if ((constraints.getTeMetric() != null) && (totalCost > constraints.getTeMetric().intValue())) {
            return true;
        }

        /* Check that total Delay respect the initial constraints */
        int totalDelay = attributes.getDelay().getValue().intValue() + path.getDelay();
        if ((constraints.getDelay() != null) && (totalDelay > constraints.getDelay().getValue().intValue())) {
            return true;
        }

        /* Check that Edge respect Loss constraint */
        if ((constraints.getLoss() != null)
                && (attributes.getLoss().getValue().intValue() > constraints.getLoss().getValue().intValue())) {
            return true;
        }
        /* Check that Edge meet Bandwidth constraint */
        Long bandwidth = constraints.getBandwidth().getValue().longValue();
        if (attributes.getUnreservedBandwidth().get(constraints.getClassType().intValue()).getBandwidth().getValue()
                .longValue() < bandwidth
                || attributes.getMaxLinkBandwidth().getValue().longValue() < bandwidth
                || attributes.getMaxResvLinkBandwidth().getValue().longValue() < bandwidth) {
            return true;
        }

        /* Check that Edge belongs to the requested address family */
        switch (constraints.getAddressFamily()) {
            case Ipv4:
                if (attributes.getRemoteAddress().getIpv4Address() == null) {
                    return true;
                }
                break;
            case Ipv6:
                if (attributes.getRemoteAddress().getIpv6Address() == null) {
                    return true;
                }
                break;
            case SrIpv4:
                if (getIpv4NodeSid(edge.getDestination()) == null) {
                    return true;
                }
                break;
            case SrIpv6:
                if (getIpv6NodeSid(edge.getDestination()) == null) {
                    return true;
                }
                break;
            default:
                return true;
        }

        /* Check that Edge belongs to admin group */
        if ((constraints.getAdminGroup() != null)
                && !(constraints.getAdminGroup().equals(attributes.getAdminGroup()))) {
            return true;
        }

        /* OK. All is fine. We can consider this Edge valid, so not to be prune */
        return false;
    }

    /**
     * Return the MPLS Label corresponding to the Node SID for IPv4 when the Connected Vertex is Segment Routing aware.
     *
     * @return MPLS Label if Connected Vertex is Segment Routing aware, Null otherwise
     */
    protected @Nullable MplsLabel getIpv4NodeSid(ConnectedVertex cvertex) {
        /* Check that current Vertex and next Vertex are Segment Routing aware */
        if (cvertex.getVertex().getSrgb() == null) {
            return null;
        }
        /* Find in Prefix List Node SID attached to the IPv4 of the next Vertex
         * and return the MPLS Label that corresponds to the index in the SRGB */
        for (Prefix prefix : cvertex.getPrefixes()) {
            if (prefix.isNodeSid() && prefix.getPrefix().getIpv4Prefix() != null) {
                return new MplsLabel(
                        Uint32.valueOf(cvertex.getVertex().getSrgb().getLowerBound().intValue()
                                + prefix.getPrefixSid().intValue()));
            }
        }
        return null;
    }

    /**
     * Return the MPLS Label corresponding to the Node SID for IPv6 when the Connected Vertex is Segment Routing aware.
     *
     * @return MPLS Label if Connected Vertex is Segment Routing aware, Null otherwise
     */
    protected @Nullable MplsLabel getIpv6NodeSid(ConnectedVertex cvertex) {
        /* Check that current Vertex and next Vertex are Segment Routing aware */
        if (cvertex.getVertex().getSrgb() == null) {
            return null;
        }
        /* Find in Prefix List Node SID attached to the IPv6 of the next Vertex
         * and return the MPLS Label that corresponds to the index in the SRGB */
        for (Prefix prefix : cvertex.getPrefixes()) {
            if (prefix.isNodeSid() && prefix.getPrefix().getIpv6Prefix() != null) {
                return new MplsLabel(
                        Uint32.valueOf(cvertex.getVertex().getSrgb().getLowerBound().intValue()
                                + prefix.getPrefixSid().intValue()));
            }
        }
        return null;
    }

    /**
     * Convert List of Connected Edges into a Path Description as a List of IPv4, IPv6 or MPLS Label depending of
     * the requested Address Family.
     *
     * @param edges List of Connected Edges
     *
     * @return Path Description
     */
    protected List<PathDescription> getPathDescription(List<ConnectedEdge> edges) {
        ArrayList<PathDescription> list = new ArrayList<PathDescription>();

        for (ConnectedEdge edge : edges) {
            switch (constraints.getAddressFamily()) {
                case Ipv4:
                    list.add(new PathDescriptionBuilder()
                            .setIpv4(edge.getEdge().getEdgeAttributes().getRemoteAddress().getIpv4Address()).build());
                    break;
                case Ipv6:
                    list.add(new PathDescriptionBuilder()
                            .setIpv6(edge.getEdge().getEdgeAttributes().getRemoteAddress().getIpv6Address())
                            .build());
                    break;
                case SrIpv4:
                    list.add(new PathDescriptionBuilder().setLabel(getIpv4NodeSid(edge.getDestination())).build());
                    break;
                case SrIpv6:
                    list.add(new PathDescriptionBuilder().setLabel(getIpv6NodeSid(edge.getDestination())).build());
                    break;
                default:
                    break;
            }
        }
        return list;
    }

    @Override
    public abstract ConstrainedPath computeP2pPath(VertexKey source, VertexKey destination,
            PathConstraints constraints);

}
