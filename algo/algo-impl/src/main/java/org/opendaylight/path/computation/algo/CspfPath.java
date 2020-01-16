/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.path.computation.algo;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.graph.ConnectedEdge;
import org.opendaylight.graph.ConnectedVertex;

/**
 * This Class implements the Constraint Shortest Path First (CSPF) Path stored in the Priority Queue used by various
 * Path Computation Algorithms.
 *
 * <p>The path corresponds to the computed path between the Source Vertex and the Current Vertex. Cost and delay are
 * accumulated values from the source to the current vertex.
 *
 * <p>The class uses standard java "Comparable" interface to support "natural ordering" and thus implements
 * the "compareTo" method as well as the "equals" method to allow comparison based on Vertex Key. It also
 * overrides the "hasCode" method.
 *
 * @author Olivier Dugeon
 *
 */

public class CspfPath implements Comparable<CspfPath> {

    /* Associated Connected Vertex: i.e. the current vertex in the Path */
    private ConnectedVertex cvertex;

    /* Path Length and associated cost and delay */
    private float pathLength = 0;
    private int cost = Integer.MAX_VALUE;
    private int delay = Integer.MAX_VALUE;

    /* Path as Connected Edge list from the source up to the Connected Vertex */
    private ArrayList<ConnectedEdge> currentPath = new ArrayList<ConnectedEdge>();
    /* Penultimate Connected Vertex in the current Path */
    private Long predecessor;

    public static final byte UNKNOWN   = 0x00;
    public static final byte ACTIVE    = 0x01;
    public static final byte SELECTED  = 0x02;
    public static final byte DOMINATED = 0x03;
    public static final byte PROCESSED = 0x04;
    private byte pathStatus;
    /* Key used by the Priority Queue to sort the paths */
    private Integer key = Integer.MAX_VALUE;

    public CspfPath(ConnectedVertex vertex) {
        this.cvertex = vertex;
    }

    public ConnectedVertex getVertex() {
        return this.cvertex;
    }

    public Long getVertexKey() {
        return this.cvertex.getKey();
    }

    public CspfPath setCost(int cost) {
        this.cost = cost;
        return this;
    }

    public int getCost() {
        return this.cost;
    }

    public CspfPath setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    public int getDelay() {
        return this.delay;
    }

    public CspfPath addConnectedEdge(ConnectedEdge edge) {
        this.currentPath.add(edge);
        return this;
    }

    public CspfPath addAll(List<ConnectedEdge> list) {
        this.currentPath.addAll(list);
        return this;
    }

    public List<ConnectedEdge> getPath() {
        return this.currentPath;
    }

    public int getPathCount() {
        return this.currentPath.size();
    }

    public CspfPath setPathStatus(byte status) {
        this.pathStatus = status;
        return this;
    }

    public byte getPathStatus() {
        return this.pathStatus;
    }

    public CspfPath setPredecessor(Long vertexId) {
        this.predecessor = vertexId;
        return this;
    }

    public Long getPredecessor() {
        return this.predecessor;
    }

    public CspfPath setPathLength(float length) {
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

    public CspfPath setKey(Integer key) {
        this.key = key;
        return this;
    }

    public Integer getKey() {
        return this.key;
    }

    @Override
    public int compareTo(CspfPath other) {
        return this.key.compareTo(other.getKey());
    }

    @Override
    public boolean equals(Object object) {
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

}
