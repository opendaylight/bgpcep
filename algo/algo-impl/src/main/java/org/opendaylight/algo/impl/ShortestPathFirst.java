/*
 * Copyright (c) 2016 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.algo.impl;

import java.util.HashMap;
import org.opendaylight.algo.impl.CspfPath.CspfPathStatus;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev250115.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.ComputationStatus;
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
    protected CspfPath computeSimplePath(final VertexKey src, final VertexKey dst) {
        LOG.info("Start SPF Path Computation from {} to {} with constraints {}", src, dst, constraints);

        /* Initialize algorithm */
        initializePathComputation(src, dst);
        if (status != ComputationStatus.InProgress) {
            LOG.warn("Initial configurations are not met. Abort!");
            return null;
        }

        /* Process all Connected Vertex until priority queue becomes empty. Connected Vertices are added into the
         * priority queue when processing the next Connected Vertex: see relax() method */
        final var finalPath = new CspfPath(pathDestination).setStatus(CspfPathStatus.InProgress);
        visitedVertices.clear();

        while (priorityQueue.size() != 0) {
            final var currentPath = priorityQueue.poll();
            visitedVertices.put(currentPath.getVertexKey(), currentPath);
            LOG.debug("Process path to Vertex {} from Priority Queue", currentPath.getVertex());
            final var edges = currentPath.getVertex().getOutputConnectedEdges();

            for (ConnectedEdge edge : edges) {
                /* Check that Edge point to a valid Vertex and is suitable for the Constraint Address Family */
                if (pruneEdge(edge, currentPath)) {
                    LOG.trace("  Prune Edge {}", edge);
                    continue;
                }
                /* Check if we reach the destination with a better cost */
                if (relax(edge, currentPath) && pathDestination.getCost() < finalPath.getCost()) {
                    finalPath.setCost(pathDestination.getCost());
                    finalPath.replacePath(pathDestination.getPath());
                    finalPath.setStatus(CspfPathStatus.Active);
                    LOG.debug("  Found a valid path up to destination {}", finalPath.getPath());
                }
            }
        }
        /* The priority queue is empty => all the possible (vertex, path) elements have been explored
         * The CspfPath "finalPath" object contains the optimal path if it exists
         */
        if (finalPath.getStatus() == CspfPathStatus.InProgress || finalPath.getPathCount() == 0) {
            status = ComputationStatus.NoPath;
            LOG.debug("No valid path found from {} to {} with constraints {}", src, dst, constraints);
            return null;
        }

        status = ComputationStatus.Completed;
        LOG.debug("SPF Computation ended. Found path {} with cost {}", finalPath.getPath(), finalPath.getCost());
        return finalPath;
    }

    private boolean relax(final ConnectedEdge edge, final CspfPath currentPath) {
        LOG.debug("    Start relaxing Edge {} to Vertex {}", edge, edge.getDestination());
        final Long nextVertexKey = edge.getDestination().getKey();

        /* Verify if we have not visited this Vertex to avoid loop */
        if (visitedVertices.containsKey(nextVertexKey)) {
            return false;
        }

        /* Get Next Vertex from processedPath or create a new one if it has not yet processed.
         * Note that processedPath is initialized with source and destination. Thus, when we reach
         * the destination Vertex during the processing, nextPath is equal to pathDestination.
         */
        CspfPath nextPath = processedPath.get(nextVertexKey);
        if (nextPath == null) {
            nextPath = new CspfPath(currentPath.getSource(), edge.getDestination());
            processedPath.put(nextPath.getVertexKey(), nextPath);
        }

        /* Compute Cost from source to this next Vertex and add or update it in the Priority Queue
         * if total path Cost is lower than cost associated to this next Vertex.
         * This could occurs if we process a Vertex that as not yet been visited in the Graph
         * or if we found a shortest path up to this Vertex.
         * Note that for path diversity, Suurballe Algorithm used negative metric for reverse link
         * i.e. edge which has reverse edge marked as divert. */
        final var revertEdge = edge.getReverse();
        int totalCost;
        if (revertEdge != null && revertEdge.isDivert()) {
            totalCost = currentPath.getCost() - edge.getEdge().getEdgeAttributes().getMetric().intValue();
        } else {
            totalCost = currentPath.getCost() + edge.getEdge().getEdgeAttributes().getMetric().intValue();
        }
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
