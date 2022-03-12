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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220310.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220310.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220310.ConstrainedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev220310.PathConstraints;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class implements a simple Shortest Path First path computation algorithm based on standard IGP Metric.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 * @author Philippe Cadro
 */
public class ShortestPathFirst extends AbstractPathComputation {
    private static final Logger LOG = LoggerFactory.getLogger(ShortestPathFirst.class);

    private final HashMap<Long, CspfPath> visitedVertices = new HashMap<>();

    public ShortestPathFirst(final ConnectedGraph graph) {
        super(graph);
    }

    @Override
    public ConstrainedPath computeP2pPath(final VertexKey src, final VertexKey dst, final PathConstraints cts) {
        ConstrainedPathBuilder cpathBuilder;
        List<ConnectedEdge> edges;
        CspfPath currentPath;
        int currentCost = Integer.MAX_VALUE;

        LOG.info("Start SPF Path Computation from {} to {} with constraints {}", src, dst, cts);

        /* Initialize algorithm */
        this.constraints = cts;
        cpathBuilder = initializePathComputation(src, dst);
        if (cpathBuilder.getStatus() != ComputationStatus.InProgress) {
            LOG.warn("Initial configurations are not met. Abort!");
            return cpathBuilder.build();
        }

        visitedVertices.clear();

        while (priorityQueue.size() != 0) {
            currentPath = priorityQueue.poll();
            visitedVertices.put(currentPath.getVertexKey(), currentPath);
            LOG.debug("Process path to Vertex {} from Priority Queue", currentPath.getVertex());
            edges = currentPath.getVertex().getOutputConnectedEdges();

            for (ConnectedEdge edge : edges) {
                /* Check that Edge point to a valid Vertex and is suitable for the Constraint Address Family */
                if (pruneEdge(edge, currentPath)) {
                    LOG.trace("  Prune Edge {}", edge);
                    continue;
                }
                if (relax(edge, currentPath) && pathDestination.getCost() < currentCost) {
                    currentCost = pathDestination.getCost();
                    cpathBuilder.setPathDescription(getPathDescription(pathDestination.getPath()))
                            .setMetric(Uint32.valueOf(pathDestination.getCost()))
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

    private boolean relax(final ConnectedEdge edge, final CspfPath currentPath) {
        LOG.debug("    Start relaxing Edge {} to Vertex {}", edge, edge.getDestination());
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

        /* Compute Cost from source to this next Vertex and add or update it in the Priority Queue
         * if total path Cost is lower than cost associated to this next Vertex.
         * This could occurs if we process a Vertex that as not yet been visited in the Graph
         * or if we found a shortest path up to this Vertex. */
        int totalCost = edge.getEdge().getEdgeAttributes().getMetric().intValue() + currentPath.getCost();
        if (nextPath.getCost() > totalCost) {
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
            LOG.debug("    Added path to Vertex {} in the Priority Queue with weight {}",
                    nextPath.getVertex(), nextPath.getKey());
        }
        /* Return True if we reach the destination, false otherwise */
        return pathDestination.equals(nextPath);
    }
}
