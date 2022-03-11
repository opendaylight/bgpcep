/*
 * Copyright (c) 2022 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.graph;

import java.util.Collection;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Edge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Vertex;

/**
 * Connected Graph Trigger class aims to trigger action when major modification(s) takes place on a Vertex or a Edge.
 * Once trigger registered (see ConnectedGraph interface), the verify() method is called in order to determine
 * if some corrections should be executed.
 *
 * <p>verifyVertex() and verifyEdge() method must be provided by the class which registered the trigger.
 * These methods take as argument the global list of triggers registered for Vertices or Edges.
 *
 * <p>This class allows to implement close loop against modification on the Connected Graph e.g. Segment Routing SIDs
 * change on a Vertex which imposes to adjust SR path description that belongs to this Vertex or Delay modification
 * that goes upper a certain threshold that imposes to re-compute the constrained Path.
 *
 * @author Olivier Dugeon
 */
public interface ConnectedGraphTrigger {

    /**
     * This method verifies the next Vertex attributes against the current one to determine if is necessary
     * to launch correction which are left at the discretion of the class which implements this method.
     *
     * <p>If current Vertex is null, this means that the Connected Vertex will be added in the Connected Graph.
     * If next Connected Vertex is null, this means that the Connected Vertex will be deleted from the Connected Graph.
     * Otherwise, this is an update of Vertex attributes.

     * @param next      Next Connected Vertex to be installed in the Connected Graph
     * @param current   Current Vertex installed in the Connected Graph
     */
    void verifyVertex(Collection<ConnectedVertexTrigger> triggers, @Nullable ConnectedVertex next,
            @Nullable Vertex current);

    /**
     * This method verifies the next Edge attributes against the current one to determine if is necessary
     * to launch correction which are left at the discretion of the class which implements this method.
     *
     * <p>If current Edge is null, this means that the Connected Edge will be added in the Connected Graph.
     * If next Connected Edge is null, this means that the Edge will be deleted from the Connected Graph.
     * Otherwise, this is an update of Edge attributes.

     * @param next      Next Edge to be installed in the Connected Graph
     * @param current   Current Edge installed in the Connected Graph
     */
    void verifyEdge(Collection<ConnectedEdgeTrigger> triggers, @Nullable ConnectedEdge next, @Nullable Edge current);
}
