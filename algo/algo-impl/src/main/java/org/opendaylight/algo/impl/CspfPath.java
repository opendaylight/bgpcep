/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.algo.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedVertex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev251022.ComputationStatus;

/**
 * This Class implements the Constrained Shortest Path First (CSPF) Path stored in the Priority Queue used by various
 * Path Computation Algorithms.
 *
 * <p>The path corresponds to the computed path between the Source Vertex and the Current Vertex. Cost (based
 * on TE Metric) and Delay are accumulated values from the source to the current vertex.
 *
 * <p>The class uses standard java "Comparable" interface to support "natural ordering" and thus implements
 * the compareTo() method based on the "key" value. However, the equals() method uses Vertex Key for comparison.
 * HashCode() method is also overridden by the Connected Vertex hashCode() method.
 *
 * @author Olivier Dugeon
 *
 */

public class CspfPath implements Comparable<CspfPath> {

    /* Associated Connected Vertex: i.e. the current vertex in the Path */
    private final ConnectedVertex cvertex;

    /* Path Length and associated cost and delay */
    private float pathLength = 0;
    private int cost = Integer.MAX_VALUE;
    /* Uint24 Max value */
    private int delay = 16777215;

    /* Path as Connected Edge list from the source up to the Connected Vertex */
    private final ArrayList<ConnectedEdge> currentPath = new ArrayList<>();

    /* Penultimate Connected Vertex in the current Path */
    private Long predecessor;

    protected enum CspfPathStatus {
        NoOp,
        InProgress,
        Selected,
        Processed,
        Dominated,
        Active,
        Completed,
        NoPath,
        Failed
    }
    /* Status of the Path */
    private CspfPathStatus status;

    /* Key used by the Priority Queue to sort the paths */
    private Integer key = Integer.MAX_VALUE;

    public CspfPath(final ConnectedVertex vertex) {
        this.cvertex = vertex;
    }

    public ConnectedVertex getVertex() {
        return this.cvertex;
    }

    public Long getVertexKey() {
        return this.cvertex.getKey();
    }

    public CspfPath setCost(final int cost) {
        this.cost = cost;
        return this;
    }

    public int getCost() {
        return this.cost;
    }

    public CspfPath setDelay(final int delay) {
        this.delay = delay;
        return this;
    }

    public int getDelay() {
        return this.delay;
    }

    public CspfPath addConnectedEdge(final ConnectedEdge edge) {
        this.currentPath.add(edge);
        return this;
    }

    public CspfPath addPath(final List<ConnectedEdge> list) {
        this.currentPath.addAll(list);
        return this;
    }

    public CspfPath replacePath(final List<ConnectedEdge> list) {
        if (list != null && list.size() != 0) {
            this.currentPath.clear();
        }
        this.currentPath.addAll(list);
        return this;
    }

    public List<ConnectedEdge> getPath() {
        return this.currentPath;
    }

    public int getPathCount() {
        return this.currentPath.size();
    }

    public CspfPath setStatus(final CspfPathStatus status) {
        this.status = status;
        return this;
    }

    public CspfPathStatus getStatus() {
        return this.status;
    }

    public CspfPath setPredecessor(final Long vertexId) {
        this.predecessor = vertexId;
        return this;
    }

    public Long getPredecessor() {
        return this.predecessor;
    }

    public CspfPath setPathLength(final float length) {
        this.pathLength = length;
        return this;
    }

    public float getPathLength() {
        return this.pathLength;
    }

    public void clearPath() {
        this.currentPath.clear();
    }

    /*
     * Definition of the comparator method to be used by the java Priority Queue:
     * "In the PQ the elements are classified relying on the "key" attribute. The "compareTo" method return a negative,
     * zero or positive integer as this object is less than, equal to or greater than the specified object"
     *
     * The "key" attribute is here represented by the weight of the associated Vertex in the Path. For example in
     * Shortest Path First algorithm, the key represent the cost between the source and the Vertex.
     *
     * The "equals" method performs the comparison on the Vertex Key. The "equals" method is used by the "remove"
     * method of the priority queue
     *
     */

    public CspfPath setKey(final Integer key) {
        this.key = key;
        return this;
    }

    public Integer getKey() {
        return this.key;
    }

    @Override
    public int compareTo(final CspfPath other) {
        return this.key.compareTo(other.getKey());
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof CspfPath)) {
            return false;
        }

        CspfPath cspfPath = (CspfPath) object;
        return this.getVertexKey().equals(cspfPath.getVertexKey());
    }

    @Override
    public int hashCode() {
        return this.cvertex.getKey().hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("_path={");
        for (ConnectedEdge edge : currentPath) {
            if (edge.getEdge() != null && edge.getEdge().getEdgeAttributes() != null) {
                sb.append(edge.getEdge().getEdgeAttributes().getRemoteAddress()).append(", ");
            }
        }
        sb.append("cost=").append(cost);
        sb.append(", delay=").append(delay);
        sb.append(", status=").append(status);
        return sb.append('}').toString();
    }
}
