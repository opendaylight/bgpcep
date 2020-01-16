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

import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.PathConstraints;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class implements the Self Adaptive Multiple Constraints Routing Algorithm (SAMCRA) a Path Computation Algorithm.
 * The SAMCRA algorithm take into account the Bandwidth, TE Metric and Delay as composite constraints.
 * Details of SAMCRA algorithm could be found in the article "Concepts of Exact QoS Routing Algorithms",
 * Piet Van Mieghem and Fernando A. Kuipers, IEEE/ACM Transactions on Networking, Volume 12, Number 5, October 2004.
 *
 * @author Philippe Niger
 * @author Olivier Dugeon
 * @author Philippe Cadro
 *
 */

public class Samcra extends AbstractPathComputation {

    /*
     * This class stores the set of paths which has been computed for a given Connected Vertex:
     *     - pathCount        number of active paths
     *     - pathCurrent      node path currently in the priority queue (path with minimal length)
     *     - pathList         list of computed paths
     *
     * Each path is represented by a "CspfPath" class to encompass path predecessor, path status
     * and path length information
     */

    private static class SamcraPath {

        private ConnectedVertex cvertex;
        private int pathCount;
        private CspfPath currentPath;
        private ArrayList<CspfPath> pathList;

        SamcraPath(ConnectedVertex vertex) {
            this.cvertex = vertex;
            this.pathCount = 0;
            pathList = new ArrayList<CspfPath>();
        }

        public ConnectedVertex getVertex() {
            return this.cvertex;
        }

        public void decrementPathCount() {
            this.pathCount--;
        }

        public void incrementPathCount() {
            this.pathCount++;
        }

        public int getPathCount() {
            return this.pathCount;
        }

        public void setCurrentPath(CspfPath path) {
            this.currentPath = path;
        }

        public CspfPath getCurrentPath() {
            return this.currentPath;
        }

        public void addPath(CspfPath path) {
            this.pathList.add(path);
        }

        public ArrayList<CspfPath> getPathList() {
            return this.pathList;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(Samcra.class);

    /* List of potential Samcra Path that satisfy given constraints */
    private HashMap<Long, SamcraPath> samcraPaths;

    /* TE Metric cost and Delay cost for the current selected Path */
    int teCost = Integer.MAX_VALUE;
    int delayCost = Integer.MAX_VALUE;

    public Samcra(ConnectedGraph graph) {
        super(graph);
        samcraPaths = new HashMap<Long, SamcraPath>();
    }

    /* Samcra Algo:
     *
     * To limit the modification outside the Samcra method the same set of parameters as
     * the CSPF method is used (related to pseudo code, the path length is computed inside
     * the method based on the individual constraint parameters).
     *
     * On contrast to a simple CSPF algo, with Samcra a connected vertex might be associated to several
     * metric vectors from which different path lengths are computed. However a connected vertex is only
     * present once in the priority queue, associated to the minimal path weight, which is used as key
     * to address the priority queue.
     *
     * For a given metric the path weight is an integer value computed as the entire part of
     * the quantity:
     *      100 * (vector_path_metric/target_metric)
     * The path weight correspond to the maximum length computed from either the delay or TE metric.
     *
     * To maintain the priority queue behavior unchanged, a "SamcraPath" classes is created to manage
     * the set of possible paths associated to a given vertex (see above).
     *
     */

    @Override
    public ConstrainedPath computeP2pPath(VertexKey src, VertexKey dst, PathConstraints cts) {
        ConstrainedPathBuilder cpathBuilder;
        List<ConnectedEdge> edges;
        CspfPath currentPath;

        /* Initialize SAMCRA variables */
        this.constraints = cts;
        cpathBuilder = initializePathComputation(src, dst);
        if (cpathBuilder.getStatus() == ComputationStatus.Failed) {
            return cpathBuilder.build();
        }
        cpathBuilder.setBandwidth(cts.getBandwidth()).setClassType(cts.getClassType());

        samcraPaths.clear();
        samcraPaths.put(pathSource.getVertexKey(), new SamcraPath(pathSource.getVertex()));
        samcraPaths.put(pathDestination.getVertexKey(), new SamcraPath(pathDestination.getVertex()));

        /* Exploration of the priority queue:
         * Each connected vertex is represented only once in the priority queue associated to the path
         * with the minimal length (other path are stored in the SamcraPath object).
         * The top of the queue, i.e. the element with the minimal key( path weight), is processed at each loop
         */
        while (priorityQueue.size() != 0) {
            currentPath = priorityQueue.poll();

            /* Prepare Samcra Path from current CSP Path except for the source */
            if (!(currentPath.equals(pathSource))) {
                SamcraPath currentSamcraPath = samcraPaths.get(currentPath.getVertexKey());
                CspfPath currentCspfPath = currentSamcraPath.getCurrentPath();
                float queuePathLength = currentCspfPath.getPathLength();
                LOG.debug("Samcra: priority Queue output SamcraPaths: {} CurrentPath: {} PathLength: {}",
                        currentSamcraPath, currentCspfPath, queuePathLength);
            }

            edges = currentPath.getVertex().getOutputConnectedEdges();
            float currentPathLength = 1.0F;
            for (ConnectedEdge edge : edges) {
                /* Connected Vertex's edges processing:
                 * Prune the connected edges that do not satisfy the constraints (Bandwidth, TE Metric, Delay, Loss)
                 * For each remaining edge process the path to the remote vertex using the "relaxSamcra" procedure
                 *
                 * If the return path length is positive, the destination is reached and the
                 * obtained route satisfies the requested constraints.
                 * The path length is checked to record only the optimal route (i.e. the route with
                 * the minimal path length) info obtained from the destination vertex
                 */
                if (pruneEdge(edge, currentPath)) {
                    continue;
                }
                LOG.debug("Samcra: edge processing:  {}", edge.toString());
                float pathLength = relaxSamcra(edge, currentPath, pathSource);

                /* Check if we found a valid and better path */
                if ((pathLength > 0F) && (pathLength <= currentPathLength)) {
                    final SamcraPath finalPath = samcraPaths.get(pathDestination.getVertexKey());
                    cpathBuilder.setPathDescription(getPathDescription(finalPath.getCurrentPath().getPath()))
                            .setMetric(Uint32.valueOf(pathDestination.getCost()))
                            .setDelay(new Delay(Uint32.valueOf(pathDestination.getDelay())))
                            .setStatus(ComputationStatus.Active);
                    LOG.debug("Samcra: path to destination found and registered: {}", cpathBuilder);
                    currentPathLength = pathLength;
                }
            }

            /* The connected vertex that has been removed from the priority queue may have to be re-inserted with
             * the minimal length non-dominated path associated to the connected vertex if it exists (to be done
             * except for the source). Otherwise, the current path associated to the connected vertex is reset to
             * null to allow the connected vertex addition to the priority queue later on with a new path
             * (refer to "relaxSamcra" for addition of a connected vertex to the priority queue).
             */
            float previousLength  = 1.0F;
            CspfPath selectedPath = null;

            if (!(currentPath.equals(pathSource))) {
                LOG.debug("Samcra: priority queue output processing for connected vertex:  {}",
                        currentPath.getVertexKey());
                SamcraPath currentSamcraPath = samcraPaths.get(currentPath.getVertexKey());
                currentSamcraPath.decrementPathCount();
                /*
                 * The list of paths associated to the connected vertex is retrieved
                 * The path used to represent the connected vertex in the Priority Queue is marked from "selected"
                 * to "processed". The list of paths is analyzed to check if other "active" path(s) exist(s).
                 * If it is the case the shortest length is used to re-inject the connected vertex in the Priority Queue
                 */
                for (CspfPath testedPath : currentSamcraPath.getPathList()) {
                    LOG.info("Samcra: priority queue output: testedPath: {} status: {} ", testedPath,
                            testedPath.getPathStatus());
                    if (testedPath.getPathStatus() == CspfPath.SELECTED) {
                        testedPath.setPathStatus(CspfPath.PROCESSED);
                    } else if ((testedPath.getPathStatus() == CspfPath.ACTIVE)
                            && (testedPath.getPathLength() < previousLength)) {
                        selectedPath = testedPath;
                        previousLength = testedPath.getPathLength();
                    }
                }
                /* If a path is found it is marked as "selected", used as "current path" for the connected vertex
                 * and added to the priority queue
                 */
                if (selectedPath != null) {
                    selectedPath.setPathStatus(CspfPath.SELECTED);
                    currentSamcraPath.setCurrentPath(selectedPath);
                    priorityQueue.add(selectedPath);
                    LOG.debug("Samcra priority queue output: add path to the priority queue: {} path count: {} ",
                            selectedPath, currentSamcraPath.getPathCount());
                } else {
                    currentSamcraPath.setCurrentPath(null);
                }
            }
        }
        /* The priority queue is empty => all the possible (vertex, path) elements have been explored
         * The "ConstrainedPathBuilder" object contains the optimal path if it exists
         * Otherwise an empty path with status failed is returned
         */
        if ((cpathBuilder.getStatus() == ComputationStatus.OnGoing)
                || (cpathBuilder.getPathDescription().size() == 0)) {
            cpathBuilder.setStatus(ComputationStatus.Failed);
        } else {
            cpathBuilder.setStatus(ComputationStatus.Completed);
        }
        return cpathBuilder.build();
    }

    /* Connected Edge to remote connected vertex processing (on contrast to CSPF algorithm, the already processed
     * connected vertex are not zapped as a connected vertex may be associated to multiple paths). This method
     * computes the TE metric and Delay costs up to the remote end-point connected vertex and checks if the computed
     * values are acceptable according to the end-to-end constraints.
     * If relevant, update the computed path on the remote end-point connected vertex.
     * If the connected vertex has not already been processed, the corresponding CspfPath object is created.
     */
    private float relaxSamcra(ConnectedEdge edge, CspfPath currentPath, CspfPath source) {

        /* Process CspfPath including the next Vertex */
        CspfPath nextVertexPath = processedPath.get(edge.getDestination().getKey());
        if (nextVertexPath == null) {
            nextVertexPath = new CspfPath(edge.getDestination());
            processedPath.put(nextVertexPath.getVertexKey(), nextVertexPath);
            SamcraPath nextSamcraPath = new SamcraPath(edge.getDestination());
            samcraPaths.put(nextVertexPath.getVertexKey(), nextSamcraPath);
            LOG.debug("relaxSamcra: next connected vertex does not exists, create it: {} with new Samcra Path: {}",
                    nextVertexPath.toString(), nextSamcraPath);
        }

        /* Connected Vertex's paths management using SamcraPath object.
         * The predecessor connected vertex is checked to avoid unnecessary processing.
         */
        SamcraPath samcraPath = samcraPaths.get(nextVertexPath.getVertexKey());
        Long predecessorId = 0L;
        LOG.debug("relaxSamcra: start connected vertex processing next samcraPath: {}", samcraPath);
        if (!(currentPath.equals(source))) {
            LOG.debug("relaxSamcra: check predecessor");
            SamcraPath currentSamcraPath = samcraPaths.get(currentPath.getVertexKey());
            CspfPath currentVertexPath = currentSamcraPath.getCurrentPath();
            predecessorId = currentVertexPath.getPredecessor();
        }
        if (predecessorId.equals(nextVertexPath.getVertexKey())) {
            LOG.debug("relaxSamcra: Skip Edge because next vertex: {} is predecessor of: {}",
                    nextVertexPath.getVertexKey(), predecessorId);
            return 0F;
        }

        /* Connected Vertex's paths management using CspfPath object.
         * The paths list is explored and the paths dominated by the new path are marked as dominated.
         * The new path is also check and if it is dominated by an existing path it is omitted.
         */
        teCost = edge.getEdge().getEdgeAttributes().getTeMetric().intValue() + currentPath.getCost();
        delayCost = edge.getEdge().getEdgeAttributes().getDelay().getValue().intValue() + currentPath.getDelay();

        if (isPathDominated(samcraPath)) {
            LOG.debug("relaxSamcra: Skip Edge because new path is dominated");
            return 0F;
        }

        /* If the new path is not dominated by an already existing path, a new "CspfPath" object
         * is created with predecessor set to connected vertex, path length and path status information,
         * marked as "active" and added to the connected vertex's path list.
         * The weight attribute, used as classification key by the priority queue, is an integer value computed
         * from the TE and delay length.
         */
        CspfPath newPath = createNonDominatedPath(edge, nextVertexPath.getVertex(), currentPath);

        /* The new path is check versus the path currently representing the connected vertex in the priority
         * queue. If there is not yet a path for the connected vertex or if the new path length is shorter
         * than the length of the path currently selected, the new path is used as current path, marked as
         * "selected" and is added to the priority queue.
         * The previously current path status is changed from "selected" to "active" and can be re-selected
         * later on. If the new path is associated to the destination connected vertex it is not added to
         * the priority queue.
         */
        CspfPath currentSamcraPath = samcraPath.getCurrentPath();
        if (currentSamcraPath == null) {
            LOG.debug("relaxSamcra: add new Path: {}", newPath);
            if (!(newPath.equals(pathDestination))) {
                priorityQueue.add(newPath);
            }
            newPath.setPathStatus(CspfPath.SELECTED);
            samcraPath.setCurrentPath(newPath);
        } else if (newPath.getPathLength() < currentSamcraPath.getPathLength()) {
            LOG.debug("relaxSamcra: update current Path: {} with new Path: {}", currentSamcraPath, newPath);
            samcraPath.getPathList()
                .stream()
                .filter(path -> path.getPathStatus() == CspfPath.SELECTED)
                .forEach(path -> path.setPathStatus(CspfPath.ACTIVE));

            /* It is not possible to directly update the CspfPath in the Priority Queue. Indeed, if we
             * modify the path weight, the Priority Queue must be re-ordered. So, we need fist to remove
             * the CspfPath if it is present in the Priority Queue, then, update the Path Weight,
             * and finally (re-)insert it in the Priority Queue.
             */
            if (!(newPath.equals(pathDestination))) {
                priorityQueue.removeIf((path) -> path.getVertexKey().equals(newPath.getVertexKey()));
                priorityQueue.add(newPath);
            }
            newPath.setPathStatus(CspfPath.SELECTED);
            samcraPath.setCurrentPath(newPath);
        }

        /* In all cases the new path is added to the list of paths associated to the vertex */
        samcraPath.addPath(newPath);
        samcraPath.incrementPathCount();

        LOG.debug("relaxSamcra: number of paths  {} ", samcraPath.getPathCount());
        LOG.debug("relaxSamcra: add vertex Paths to samcraPath {} with index {}", samcraPath,
                samcraPath.getVertex().getKey());
        LOG.debug("relaxSamcra: current Path {}  predecessor {}",  samcraPath.getCurrentPath(),
                samcraPath.getCurrentPath().getPredecessor());
        samcraPaths.put(samcraPath.getVertex().getKey(), samcraPath);

        /* If the destination is reached, return the computed path length 0 otherwise */
        if ((samcraPath.getVertex().getKey()).equals(pathDestination.getVertexKey())) {
            return samcraPath.getCurrentPath().getPathLength();
        } else {
            return 0F;
        }
    }

    /**
     * Evaluate if the current path is dominated by an all one or dominates all previous computed path.
     *
     * @param samcraPath Current Samcra Path
     *
     * @return true if path is dominated false otherwise
     */
    private boolean isPathDominated(SamcraPath samcraPath) {
        /* Evaluate Path Domination */
        LOG.debug("Check path domination");
        Uint32 teMetric = constraints.getTeMetric();
        Uint32 delay = (constraints.getDelay() != null) ? constraints.getDelay().getValue() : null;

        for (CspfPath testedPath : samcraPath.getPathList()) {
            boolean pathCostDominated = false;
            boolean pathDelayDominated = false;
            boolean testedPathCostDominated = false;
            boolean testedPathDelayDominated = false;

            LOG.debug("Check if path {} is dominated or dominates", testedPath);
            if (testedPath.getPathStatus() != CspfPath.DOMINATED) {
                if (teMetric != null) {
                    if (teCost >= testedPath.getCost()) {
                        pathCostDominated = true;
                    } else {
                        testedPathCostDominated = true;
                    }
                }
                if (delay != null) {
                    if (delayCost >= testedPath.getDelay()) {
                        pathDelayDominated = true;
                    } else {
                        testedPathDelayDominated = true;
                    }
                }

                if ((((teMetric != null) && (pathCostDominated)) && ((pathDelayDominated) || (delay == null)))
                        || ((teMetric == null) && ((delay != null) && (pathDelayDominated)))) {
                    LOG.debug("New path is dominated with teCost: {} delayCost: {}", teCost, delayCost);
                    /* A path that dominates the current path has been found */
                    return true;
                } else if ((((teMetric != null) && (testedPathCostDominated))
                        && ((testedPathDelayDominated) || (delay == null)))
                        || ((teMetric == null) && ((delay != null) && (testedPathDelayDominated)))) {
                    /* Old Path is dominated by the new path. Mark it as Dominated and decrement number of valid Path */
                    testedPath.setPathStatus(CspfPath.DOMINATED);
                    samcraPath.decrementPathCount();
                    LOG.info("New path dominates existing path: {} teCost: {} delayCost:  {}", testedPath,
                            testedPath.getCost(), testedPath.getDelay());
                }
            }
        }
        return false;
    }

    private CspfPath createNonDominatedPath(ConnectedEdge edge, ConnectedVertex vertex, CspfPath cspfPath) {
        float pathLength = 1.0F;
        Uint32 metric = constraints.getTeMetric();
        Uint32 delay = (constraints.getDelay() != null) ? constraints.getDelay().getValue() : null;

        LOG.debug("Create new non dominated path");

        /* Compute Path length as key for the path Weight */
        float teLength = 0.0F;
        if ((metric != null) && (metric.intValue() > 0)) {
            teLength = (float) teCost / metric.intValue();
            pathLength = teLength;
        }
        float delayLength = 0.0F;
        if ((delay != null) && (delay.intValue() > 0)) {
            delayLength = (float) delayCost / delay.intValue();
            if (delayLength > teLength) {
                pathLength = delayLength;
            }
        }

        /* Create new Path with computed TE Metric, Delay and Path Length */
        CspfPath newPath = new CspfPath(vertex)
                .setCost(teCost)
                .setDelay(delayCost)
                .setKey((int) (100 * pathLength))
                .setPathStatus(CspfPath.ACTIVE)
                .setPathLength(pathLength)
                .setPredecessor(cspfPath.getVertexKey());
        newPath.addAll(cspfPath.getPath());
        newPath.addConnectedEdge(edge);

        LOG.debug("Creation of a new Path: {} path length: {} predecessor connected vertex {}",
                newPath, pathLength, newPath.getPredecessor());

        return cspfPath;
    }
}
