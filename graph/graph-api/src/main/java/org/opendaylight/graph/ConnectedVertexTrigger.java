/*
 * Copyright (c) 2022 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.graph;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev220720.Vertex;

/**
 * Connected Vertex Trigger class aims to trigger action when major modification(s) takes place on a Vertex.
 * Once trigger registered (see ConnectedVertex interface), the verifyVertex() method is called in order to determine
 * if some corrections should be executed.
 *
 * <p>verifyVertex() method must be provided by the class which registered the trigger.
 *
 * <p>This class allows to implement close loop against modification on the Connected Graph e.g. Segment Routing SIDs
 * change on this Vertex which imposes to adjust SR path description that belongs to this Vertex.
 *
 * @author Olivier Dugeon
 */
public interface ConnectedVertexTrigger {

    /**
     * This method verifies the next Vertex attribute against the current one to determine if is necessary to launch
     * correction which are left at the discretion of the class which implements this method.
     *
     * <p>If current Vertex is null, this means that the Connected Vertex will be added in the Connected Graph.
     * If next Connected Vertex is null, this means that the Connect Vertex will be deleted from the Connected Graph.
     * Otherwise, this is an update of Vertex attributes.

     * @param next      Next Connected Vertex to be installed in the Connected Graph
     * @param current   Current Vertex installed in the Connected Graph
     *
     * @return          True if the Vertex need attention, false otherwise
     */
    boolean verifyVertex(@Nullable ConnectedVertex next, @Nullable Vertex current);
}
