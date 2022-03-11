/*
 * Copyright (c) 2022 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.graph;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.Edge;

/**
 * Connected Edge Trigger class aims to trigger action when major modification(s) take place on an Edge.
 * Once trigger registered (see ConnectedEdge interface), the verifyEdge() method is called in order to determine
 * if some corrections should be executed.
 *
 * <p>verifyEdge() method must be provided by the class which registered the trigger.
 *
 * <p>This class allows to implement close loop against Edge modification on the Connected Graph e.g. delay attributes
 * goes upper a certain threshold which imposes to re-route all paths that belongs to this Edge.
 *
 * @author Olivier Dugeon
 */
public interface ConnectedEdgeTrigger {

    /**
     * This method verifies the next Edge attributes against the current one to determine if is necessary to launch
     * correction which are left at the discretion of the class which implements this method.
     *
     * <p>If current Edge is null, this means that the Connected Edge will be added in the Connected Graph.
     * If next Edge is null, this means that the Connected Edge will be deleted from the Connected Graph.
     * Otherwise, this is an update of Edge attributes.
     *
     * @param next      Next Connected Edge to be installed in the Connected Graph
     * @param current   Current Edge attributes in the Connected Graph
     *
     * @return          True if the Edge need attention, false otherwise
     */
    boolean verifyEdge(@Nullable ConnectedEdge next, @Nullable Edge current);
}
