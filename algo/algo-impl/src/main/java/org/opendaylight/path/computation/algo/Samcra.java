/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.path.computation.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Delay;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.graph.VertexKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ComputationStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.ConstrainedPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.Constraints;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class implements the Self Adaptive Multiple Constraints Routing Algorithm (SAMCRA) a Path Computation Algorithm.
 * Details of SAMCRA algorithm could be find in article "Concepts of Exact QoS Routing Algorithms",
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

    HashMap<Long, SamcraPath> samcraPaths;

    public Samcra(ConnectedGraph graph) {
        super(graph);
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
    public ConstrainedPath computeP2pPath(VertexKey src, VertexKey dst, Constraints cts) {
        ConstrainedPathBuilder cpathBuilder;
        List<ConnectedEdge> edges;
        CspfPath tmp;
        float currentPathLength = 1.0F;

        /* Initialize SAMCRA variables */
        cpathBuilder = initializePathComputation(src, dst);
        if (cpathBuilder.getStatus() == ComputationStatus.Failed) {
            return cpathBuilder.build();
        }
        cpathBuilder.setBandwitdh(cts.getBandwitdh())
                .setClassType(cts.getClassType())
                .setStatus(ComputationStatus.OnGoing);

        samcraPaths = new HashMap<Long, SamcraPath>(graph.getVerticesSize());
        samcraPaths.put(pathSource.getVertexKey(), new SamcraPath(pathSource.getVertex()));
        samcraPaths.put(pathDestination.getVertexKey(), new SamcraPath(pathDestination.getVertex()));

        /* Exploration of the priority queue:
         * Each connected vertex is represented only once in the priority queue associated to the path
         * with the minimal length (other path are stored in the SamcraPath object).
         * The top of the queue, i.e. the element with the minimal key( path weight), is processed at each loop
         */
        while (priorityQueue.size() != 0) {
            tmp = priorityQueue.poll();

            if (!(tmp.getVertexKey().equals(pathSource.getVertexKey()))) {
                SamcraPath queueVertexPath = samcraPaths.get(tmp.getVertexKey());
                CspfPath queueVertexPathCurrent = queueVertexPath.getCurrentPath();
                float queuePathLength = queueVertexPathCurrent.getPathLength();
                LOG.info("CSPFSamcra: priority Queue output queueVertexPaths:  {}  queueVertexPathCurrent:  {}"
                        + "  queuePathLength:  {}",
                        queueVertexPath, queueVertexPathCurrent, queuePathLength);
            }

            /* Connected Vertex's edges processing:
             * Prune the connected edges that do not satisfy the BW, metric and delay constraints
             * For each remaining edge process the path to the remote vertex using the "relaxSamcra" procedure
             *
             * If the return path length is positive, the destination is reached and the
             * obtained route satisfies the TE and Delay constraints.
             * The path length is checked to record only the optimal route (i.e. the route with
             * the minimal path length) info obtained from the destination vertex
             */
            edges = tmp.getVertex().getOutputEdges(cts.getBandwitdh(), cts.getClassType().intValue(), cts.getDelay(),
                    cts.getTeMetric());
            if (edges.isEmpty()) {
                LOG.debug("No edges available for vertex:  {}", tmp.getVertex().toString());
            } else {
                for (ConnectedEdge edge : edges) {
                    LOG.debug("Samcra: edge processing:  {}", edge.toString());
                    float pathLength = relaxSamcraMetricDelay(edge, tmp, pathSource,
                            cts.getMetric().intValue(), cts.getDelay().getValue().intValue());

                    /* Check if we found a valid and better path */
                    if ((pathLength > 0F) && (pathLength <= currentPathLength)) {
                        final SamcraPath finalPath = samcraPaths.get(pathDestination.getVertexKey());
                        cpathBuilder.setEdge(getPathList(finalPath.getCurrentPath().getPath()))
                                .setMetric(Uint32.valueOf(pathDestination.getCost()))
                                .setDelay(new Delay(Uint32.valueOf(pathDestination.getDelay())))
                                .setStatus(ComputationStatus.Active);
                        LOG.debug("Samcra: path to destination found and registered: {}", cpathBuilder);
                        currentPathLength = pathLength;
                    }
                }
            }

            /* The connected vertex that has been removed from the priority queue may have to be re-inserted with
             * the minimal length non-dominated path associated to the connected vertex if it exists (to be done
             * except for the source). Otherwise, the current path associated to the connected vertex is reset to
             * null to allow the connected vertex addition to the priority queue later on with a new path
             * (refer to "relaxSamcra" for addition of a connected vertex to the priority queue).
             */
            int index = 0;
            float previousLength  = 1.0F;
            boolean pathFound = false;
            CspfPath selectedPath = null;

            if (!(tmp.getVertexKey().equals(pathSource.getVertexKey()))) {
                LOG.debug("Samcra: priority queue output processing for connected vertex:  {}", tmp.getVertexKey());
                SamcraPath currentSamcraPath = samcraPaths.get(tmp.getVertexKey());
                currentSamcraPath.decrementPathCount();
                /*
                 * The list of paths associated to the connected vertex is retrieved
                 * The path used to represent the connected vertex in the Priority Queue is marked from "selected"
                 * to "processed". The list of paths is analyzed to check if other "active" path(s) exist(s).
                 * If it is the case the shortest length is used to re-inject the connected vertex in the Priority Queue
                 */
                ArrayList<CspfPath> pathList = currentSamcraPath.getPathList();
                LOG.debug("Samcra: priority queue output check vertex paths");
                ListIterator<CspfPath> it = pathList.listIterator();
                while (it.hasNext()) {
                    CspfPath testedPath = it.next();
                    LOG.info("Samcra: priority queue output: testedPath: {} status: {} ", testedPath,
                            testedPath.getPathStatus());
                    if (testedPath.getPathStatus() == CspfPath.SELECTED) {
                        index = pathList.indexOf(testedPath);
                        testedPath.setPathStatus(CspfPath.PROCESSED);
                        pathList.set(index, testedPath);
                    } else {
                        if ((testedPath.getPathStatus() == CspfPath.ACTIVE)
                                && (testedPath.getPathLength() < previousLength)) {
                            pathFound = true;
                            selectedPath = testedPath;
                            previousLength = testedPath.getPathLength();
                        }
                    }
                }
                /* If a path is found it is marked as "selected", used as "current path" for the connected vertex
                 * and added to the priority queue
                 */
                if (pathFound) {
                    index = pathList.indexOf(selectedPath);
                    selectedPath.setPathStatus(CspfPath.SELECTED);
                    pathList.set(index, selectedPath);
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
                || (cpathBuilder.getEdge().size() == 0)) {
            cpathBuilder.setStatus(ComputationStatus.Failed);
        } else {
            cpathBuilder.setStatus(ComputationStatus.Completed);
        }
        return cpathBuilder.build();
    }

    private float relaxSamcraMetricDelay(ConnectedEdge edge, CspfPath current, CspfPath source, int metric, int delay) {
        boolean pathCostDominated = false;
        boolean pathDelayDominated = false;
        boolean pathDominated = false;
        boolean testedPathCostDominated = false;
        boolean testedPathDelayDominated = false;
        float teLength = 0.0F;
        float delayLength = 0.0F;
        float pathLength = 1.0F;
        float float1;
        float float2;
        int pathWeight = 0;
        int index  = 0;
        Long predecessorId = 0L;

        /* Connected Edge to remote connected vertex processing (on contrast to CSPF algorithm, the already processed
         * connected vertex are not zapped as a connected vertex may be associated to multiple paths).
         * Compute the TE metric and Delay costs to the remote end-point connected vertex.
         * Check if the computed values are acceptable according to the end-to-end constraints.
         * If relevant, update the computed path on the remote end-point connected vertex.
         * If the connected vertex has not already been processed the CspfPath object is created.
         */
        int teCost = edge.getEdge().getEdgeAttributes().getTeMetric().intValue() + current.getCost();
        int delayCost = edge.getEdge().getEdgeAttributes().getDelay().getValue().intValue() + current.getDelay();
        if (!(((metric != -1) && (teCost > metric)) || ((delay != -1) && (delayCost > delay)))) {
            CspfPath nextVertexPath = processedPath.get(edge.getDestination().getKey());
            if (nextVertexPath == null) {
                nextVertexPath = new CspfPath(edge.getDestination());
                processedPath.put(nextVertexPath.getVertexKey(), nextVertexPath);
                SamcraPath nextSamcraPath = new SamcraPath(edge.getDestination());
                samcraPaths.put(nextVertexPath.getVertexKey(), nextSamcraPath);
                LOG.debug("relaxSamcra: next connected vertex does not exists, create it: {} with new Vertex Path: {}",
                        nextVertexPath.toString(), nextSamcraPath);
            }

            /* Connected Vertex's paths management using samcraPath object.
             * The predecessor connected vertex is checked to avoid unnecessary processing.
             */
            SamcraPath samcraPath = samcraPaths.get(nextVertexPath.getVertexKey());
            LOG.debug("relaxSamcra: start connected vertex processing next samcraPath: {}", samcraPath);
            if (!((current.getVertexKey()).equals(source.getVertexKey()))) {
                LOG.debug("relaxSamcra: check predecessor");
                SamcraPath currentSamcraPath = samcraPaths.get(current.getVertexKey());
                CspfPath currentVertexPath = currentSamcraPath.getCurrentPath();
                predecessorId = currentVertexPath.getPredecessor();
            }

            /* Connected Vertex's paths management using CspfPath object.
             * The paths list is explored and the paths dominated by the new path are marked as dominated.
             * The new path is also check and if it is dominated by an existing path it is omitted.
             */
            if (!(predecessorId.equals(nextVertexPath.getVertexKey()))) {
                LOG.debug("relaxSamcra: check path domination");
                ArrayList<CspfPath> pathList = samcraPath.getPathList();
                ListIterator<CspfPath> it = pathList.listIterator();

                while ((it.hasNext()) && !(pathDominated)) {
                    CspfPath testedPath = it.next();
                    index = pathList.indexOf(testedPath);

                    pathCostDominated = false;
                    pathDelayDominated = false;
                    testedPathCostDominated = false;
                    testedPathDelayDominated = false;

                    LOG.debug("relaxSamcra: path check testedPath: {} index:  {} ", testedPath, index);
                    if (testedPath.getPathStatus() != CspfPath.DOMINATED) {
                        if (metric != -1) {
                            if (teCost >= testedPath.getCost()) {
                                pathCostDominated = true;
                            } else {
                                testedPathCostDominated = true;
                            }
                        }
                        if (delay != -1) {
                            if (delayCost >= testedPath.getDelay()) {
                                pathDelayDominated = true;
                            } else {
                                testedPathDelayDominated = true;
                            }
                        }

                        if ((((metric != -1) && (pathCostDominated)) && (((delay != -1) && (pathDelayDominated))
                                || (delay == -1))) || ((metric == -1) &&  ((delay != -1) && (pathDelayDominated)))) {
                            pathDominated = true;
                            float1 = (float) teCost / metric;
                            float2 = (float) delayCost / delay;
                            LOG.debug("relaxSamcra: new path is dominated teCost:  {} delayCost:  {}", float1, float2);
                        } else if ((((metric != -1) && (testedPathCostDominated)) && (((delay != -1)
                                && (testedPathDelayDominated)) || (delay == -1))) || ((metric == -1) &&  ((delay != -1)
                                        && (testedPathDelayDominated)))) {
                            testedPath.setPathStatus(CspfPath.DOMINATED);
                            pathList.set(index, testedPath);
                            samcraPath.decrementPathCount();
                            float1 = (float) testedPath.getCost() / metric;
                            float2 = (float) testedPath.getDelay() / delay;
                            LOG.info("relaxSamcra: new path dominates existing path: {} teCost:  {} "
                                    + "delayCost:  {} ", testedPath, float1, float2);
                        }
                    }
                }

                /* If the new path is not dominated by an already existing path, a new "CspfPath" object
                 * is created with predecessor set to connected vertex, path length and path status information,
                 * marked as "active" and added to the connected vertex's path list.
                 * The weight attribute, used as classification key by the priority queue, is an integer value computed
                 * from the TE and delay length.
                 */
                if (!pathDominated) {
                    LOG.debug("relaxSamcra: non dominated path processing");
                    /* Compute TE, Delay and Path lenght as well as path Weight */
                    if ((metric != -1) && (metric > 0.0)) {
                        teLength = (float) teCost / metric;
                        pathLength = teLength;
                    }
                    if ((delay != -1) && (delay > 0.0)) {
                        delayLength = (float) delayCost / delay;
                        if (delayLength > teLength) {
                            pathLength = delayLength;
                        }
                    }
                    LOG.debug("relaxSamcra: Length computation TE:  {}  Delay:  {}  Path:  {}", teLength, delayLength,
                            pathLength);
                    pathWeight = (int) (100 * pathLength);

                    CspfPath newPath = new CspfPath(nextVertexPath.getVertex())
                            .setCost(teCost)
                            .setDelay(delayCost)
                            .setKey(pathWeight)
                            .setPathStatus(CspfPath.ACTIVE)
                            .setPathLength(pathLength)
                            .setPredecessor(current.getVertexKey());
                    newPath.addAll(current.getPath());
                    newPath.addConnectedEdge(edge);

                    LOG.debug("relaxSamcra: creation of newPath: {} path length: {} predecessor connected vertex {}",
                            newPath, pathLength, newPath.getPredecessor());

                    /* The new path is check versus the path currently representing the connected vertex in the priority
                     * queue. If there is not yet a path for the connected vertex or if the new path length is shorter
                     * than the length of the path currently selected, the new path is used as current path, marked as
                     * "selected" and is added to the priority queue.
                     * The previously current path status is changed from "selected" to "active" and can be re-selected
                     * later on. If the new path is associated to the destination connected vertex it is not added to
                     * the priority queue.
                     */
                    CspfPath currentPath = samcraPath.getCurrentPath();
                    if (currentPath == null) {
                        LOG.debug("relaxSamcra: add new Path: {}", newPath);
                        if (!(newPath.getVertexKey().equals(pathDestination.getVertexKey()))) {
                            priorityQueue.add(newPath);
                        }
                        newPath.setPathStatus(CspfPath.SELECTED);
                        samcraPath.setCurrentPath(newPath);
                    } else if (newPath.getPathLength() < currentPath.getPathLength()) {
                        LOG.debug("relaxSamcra: update current Path: {} with new Path: {}", currentPath, newPath);
                        ListIterator<CspfPath> it2 = pathList.listIterator();
                        while (it2.hasNext()) {
                            CspfPath testedPath = it2.next();
                            if (testedPath.getPathStatus() == CspfPath.SELECTED) {
                                index = pathList.indexOf(testedPath);
                                testedPath.setPathStatus(CspfPath.ACTIVE);
                                pathList.set(index, testedPath);
                            }
                        }

                        /* It is not possible to directly update the CspfPath in the Priority Queue. Indeed, if we
                         * modify the path weight, the Priority Queue must be re-ordered. So, we need fist to remove
                         * the CspfPath if it is present in the Priority Queue, then, update the Path Weight,
                         * and finally (re-)insert it in the Priority Queue.
                         */
                        if (!(newPath.getVertexKey().equals(pathDestination.getVertexKey()))) {
                            priorityQueue.removeIf((path) -> path.getVertexKey().equals(newPath.getVertexKey()));
                            priorityQueue.add(newPath);
                        }
                        newPath.setPathStatus(CspfPath.SELECTED);
                        samcraPath.setCurrentPath(newPath);
                    }

                    /* In all cases the new path is added to the list of paths associated to the vertex
                     */
                    samcraPath.addPath(newPath);
                    samcraPath.incrementPathCount();

                    LOG.debug("relaxSamcra: number of paths  {} ", samcraPath.getPathCount());
                    LOG.debug("relaxSamcra: add vertex Paths to samcraPath {} with index {}", samcraPath,
                            samcraPath.getVertex().getKey());
                    LOG.debug("relaxSamcra: current Path {}  predecessor {}",  samcraPath.getCurrentPath(),
                            samcraPath.getCurrentPath().getPredecessor());
                    samcraPaths.put(samcraPath.getVertex().getKey(), samcraPath);
                }

                /* If the destination is reached the computed path length */
                if ((samcraPath.getVertex().getKey()).equals(pathDestination.getVertexKey())) {
                    return pathLength;
                }
            }
            else {
                LOG.debug("relaxSamcra: exit without processing because next vertex: {} is predecessor: {}",
                        nextVertexPath.getVertexKey(), predecessorId);
            }
        }
        else {
            LOG.debug("relaxSamcra: exit without processing because computed TE: {} and/or delay: {}"
                    + " exceed constraints", teCost, delayCost);
        }
        return 0F;
    }

}
