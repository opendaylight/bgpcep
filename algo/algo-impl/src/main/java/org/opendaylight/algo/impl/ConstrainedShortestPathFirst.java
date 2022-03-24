/*
 * Copyright (c) 2016 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.algo.impl;

import java.util.HashMap;
import java.util.List;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220324.ConstrainedPathBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class implements a simple Constrained Shortest Path First path computation algorithm that take into account
 * Bandwidth and TE Metric as constraints.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 * @author Philippe Cadro
 */
public class ConstrainedShortestPathFirst extends AbstractPathComputation {
    private static final Logger LOG = LoggerFactory.getLogger(ConstrainedShortestPathFirst.class);

    private final HashMap<Long, CspfPath> visitedVertices = new HashMap<>();

    public ConstrainedShortestPathFirst(final ConnectedGraph graph) {
        super(graph);
    }

    @Override
    protected ConstrainedPath computeSimplePath(final VertexKey src, final VertexKey dst) {
        LOG.info("Start CSPF Path Computation from {} to {} with constraints {}", src, dst, constraints);

        /* Initialize algorithm */
        ConstrainedPathBuilder cpathBuilder = initializePathComputation(src, dst);
        if (cpathBuilder.getStatus() != ComputationStatus.InProgress) {
            return cpathBuilder.build();
        }
        cpathBuilder.setBandwidth(constraints.getBandwidth()).setClassType(constraints.getClassType());

        visitedVertices.clear();

        /* Process all Connected Vertex until priority queue becomes empty. Connected Vertices are added into the
         * priority queue when processing the next Connected Vertex: see relaxMC() method */
        int currentCost = Integer.MAX_VALUE;
        while (priorityQueue.size() != 0) {
            CspfPath currentPath = priorityQueue.poll();
            visitedVertices.put(currentPath.getVertexKey(), currentPath);
            LOG.debug("Got path to Vertex {} from Priority Queue", currentPath.getVertex());
            List<ConnectedEdge> edges = currentPath.getVertex().getOutputConnectedEdges();

            for (ConnectedEdge edge : edges) {
                /* Skip Connected Edges that must be prune i.e. Edges that not satisfy the given constraints,
                 * in particular the Bandwidth, TE Metric and Delay. */
                if (pruneEdge(edge, currentPath)) {
                    LOG.trace("  Prune Edge {}", edge);
                    continue;
                }
                if (relaxMultiConstraints(edge, currentPath) && pathDestination.getCost() < currentCost) {
                    currentCost = pathDestination.getCost();
                    cpathBuilder.setPathDescription(getPathDescription(pathDestination.getPath()))
                            .setTeMetric(Uint32.valueOf(pathDestination.getCost()))
                            .setStatus(ComputationStatus.Active);
                    LOG.debug("  Found a valid path up to destination {}", cpathBuilder.getPathDescription());
                }
            }
        }
        /* The priority queue is empty => all the possible (vertex, path) elements have been explored
         * The "ConstrainedPathBuilder" object contains the optimal path if it exists
         * Otherwise an empty path with status failed is returned
         */
        return cpathBuilder
            .setStatus(
                cpathBuilder.getStatus() == ComputationStatus.InProgress
                        || cpathBuilder.getPathDescription().size() == 0
                   ? ComputationStatus.NoPath
                   : ComputationStatus.Completed)
            .build();
    }

    private boolean relaxMultiConstraints(final ConnectedEdge edge, final CspfPath currentPath) {
        LOG.debug("    Start relaxing Multi Constraints on Edge {} to Vertex {}", edge, edge.getDestination());
        final Long nextVertexKey = edge.getDestination().getKey();

        /* Verify if we have not visited this Vertex to avoid loop */
        if (visitedVertices.containsKey(nextVertexKey)) {
            return false;
        }

        /* Get Next Vertex from processedPath or create a new one if it has not yet processed */
        CspfPath nextPath = processedPath.get(nextVertexKey);
        if (nextPath == null) {
            nextPath = new CspfPath(edge.getDestination());
            processedPath.put(nextPath.getVertexKey(), nextPath);
        }

        /* Add or update the CspfPath in the Priority Queue if total path Cost is lower than cost associated
         * to this next Vertex. This could occurs if we process a Vertex that as not yet been visited in the Graph
         * or if we found a shortest path up to this Vertex. */
        int totalCost = edge.getEdge().getEdgeAttributes().getTeMetric().intValue() + currentPath.getCost();
        if (totalCost < nextPath.getCost()) {
            nextPath.setCost(totalCost)
                    .replacePath(currentPath.getPath())
                    .addConnectedEdge(edge);
            /* It is not possible to directly update the CspfPath in the Priority Queue. Indeed, if we modify the path
             * weight, the Priority Queue must be re-ordered. So, we need fist to remove the CspfPath if it is present
             * in the Priority Queue, then, update the Path Weight, and finally (re-)insert it in the Priority Queue.
             */
            priorityQueue.removeIf(path -> path.getVertexKey().equals(nextVertexKey));
            nextPath.setKey(totalCost);
            priorityQueue.add(nextPath);
            LOG.debug("    Added path to Vertex {} in the Priority Queue", nextPath.getVertex());
        }
        /* Return True if we reach the destination, false otherwise */
        return pathDestination.equals(nextPath);
    }
}
