/*
 * Copyright (c) 2016 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.path.computation.algo;

import java.util.HashMap;
import java.util.List;

import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.Constraints;
import org.opendaylight.yangtools.yang.common.Uint32;


/**
 * This Class implements a simple Constraints Shortest Path First path computation algorithm.
 *
 * @author Olivier Dugeon
 * @author Philippe Niger
 * @author Philippe Cadro
 *
 */
public class ConstraintsShortestPathFirst extends AbstractPathComputation {

    private HashMap<Long, CspfPath> visitedVertices;

    public ConstraintsShortestPathFirst(ConnectedGraph graph) {
        super(graph);
        visitedVertices = new HashMap<>(graph.getVerticesSize());
    }

    public ConstrainedPath computeP2pPath(VertexKey src, VertexKey dst, Constraints cts) {
        ConstrainedPathBuilder cpathBuilder;
        List<ConnectedEdge> edges;
        CspfPath currentPath;
        int currentCost = Integer.MAX_VALUE;
        int teMetric = Integer.MAX_VALUE;
        int delay = Integer.MAX_VALUE;

        /* Initialize algorithm */
        cpathBuilder = initializePathComputation(src, dst);
        if (cpathBuilder.getStatus() == ComputationStatus.Failed) {
            return cpathBuilder.build();
        }

        cpathBuilder.setBandwitdh(cts.getBandwitdh())
                .setClassType(cts.getClassType())
                .setStatus(ComputationStatus.OnGoing);

        visitedVertices.clear();

        /* Check which Constraints are provided */
        if (cts.getTeMetric() != null) {
            teMetric = cts.getTeMetric().intValue();
        }
        if (cts.getDelay() != null) {
            teMetric = cts.getDelay().getValue().intValue();
        }

        while (priorityQueue.size() != 0) {
            currentPath = priorityQueue.poll();
            visitedVertices.put(currentPath.getVertexKey(), currentPath);
            /* Get all Edges that satisfy the given constraints, in particular the Bandwidth. TE Metric and Delay
             * constraints will be check again in the relaxMC method (see below). */
            edges = currentPath.getVertex().getOutputEdges(cts.getBandwitdh(), cts.getClassType().intValue(),
                    cts.getDelay(), cts.getTeMetric());

            for (ConnectedEdge edge : edges) {
                if ((relaxMC(edge, currentPath, teMetric, delay)) && (pathDestination.getCost() < currentCost)) {
                    currentCost = pathDestination.getCost();
                    cpathBuilder.setEdge(getPathList(pathDestination.getPath()))
                            .setMetric(Uint32.valueOf(pathDestination.getCost()))
                            .setStatus(ComputationStatus.Active);
                }
            }
        }
        /* The priority queue is empty => all the possible (vertex, path) elements have been explored
         * The "ConstrainedPathBuilder" object contains the optimal path if it exists
         * Otherwise an empty path with status failed is returned
         */
        if ((cpathBuilder.getStatus() == ComputationStatus.OnGoing)
                || (cpathBuilder.getEdge().size() == 0)) {
            cpathBuilder.setStatus(ComputationStatus.Failed);
        } else {
            cpathBuilder.setStatus(ComputationStatus.Completed);
        }
        return cpathBuilder.build();
    }

    private boolean relaxMC(ConnectedEdge edge, CspfPath currentPath, int teMetric, int delay) {
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

        /* Check that total Cost and total Delay respect the initial constraints */
        int totalCost = edge.getEdge().getEdgeAttributes().getTeMetric().intValue() + currentPath.getCost();
        int totalDelay = edge.getEdge().getEdgeAttributes().getDelay().getValue().intValue() + currentPath.getDelay();
        if ((teMetric < totalCost) || (delay < totalDelay)) {
            return false;
        }

        /* Add or update the CspfPath in the Priority Queue if total path Cost is lower than cost associated
         * to this next Vertex. This could occurs if we process a Vertex that as not yet been visited in the Graph
         * or if we found a shortest path up to this Vertex. */
        if (nextPath.getCost() > totalCost) {
            nextPath.setCost(totalCost);
            nextPath.addAll(currentPath.getPath());
            nextPath.addConnectedEdge(edge);
            /* It is not possible to directly update the CspfPath in the Priority Queue. Indeed, if we modify the path
             * weight, the Priority Queue must be re-ordered. So, we need fist to remove the CspfPath if it is present
             * in the Priority Queue, then, update the Path Weight, and finally (re-)insert it in the Priority Queue.
             */
            priorityQueue.removeIf((path) -> path.getVertexKey().equals(nextVertexKey));
            nextPath.setKey(totalCost);
            priorityQueue.add(nextPath);
        }
        /* Return True if we reach the destination, false otherwise */
        return pathDestination.equals(nextPath);
    }
}
