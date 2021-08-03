/*
 * Copyright (c) 2021 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.server;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.ManagedNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.TePath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.managed.node.TePathKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

/**
 * Path Manager is a new service provided by the PCE Server feature to manage the Traffic Engineering paths.
 *
 * <p>
 * It allows to create, update and delete Traffic Engineering paths on an IP/MPLS network.
 * Paths could be enforced by means of RSVP-TE tunnel or Segment Routing path.
 * All paths characteristics are automatically stored in the DataStore.
 *
 * @author Olivier Dugeon
 *
 */
public interface PathManager {

    /**
     * Check if a Managed Node exist in the Path Manager.
     *
     * @param id    Node ID of the Managed Node.
     *
     * @return      True if Managed Node exist, false otherwise.
     */
    boolean existManagedNode(NodeId id);

    /**
     * Add Managed Node to the Path Manager.
     *
     * @param node  Managed Node to be added.
     *
     * @return      Added Managed Node.
     */
    ManagedNode addManagedNode(ManagedNode node);

    /**
     * Delete Managed Node from the Path Manager.
     *
     * @param node  Managed Node to be deleted.
     */
    void deleteManagedNode(ManagedNode node);

    /**
     * Add TE Path to the Path Manager.
     *
     * <p>The constrained path is computed from the TE Path constraints and corresponding LSP
     * is enforced in the PCC identified by the Node ID.
     * If TE Path already exist, it is updated.
     * The return TE Path is enriched with the computed ERO and the status of the TE path.</p>
     *
     * @param id        Node ID of the Managed Node where this TE Path is attached.
     *
     * @param tePath    TE Path to be added.
     *
     * @return          Added TE Path.
     */
    TePath addTePath(NodeId id, TePath tePath);

    /**
     * Delete TE Path from the Path Manager.
     *
     * <p>The corresponding LSP is deleted on the PCC identified by the Node ID.</p>
     *
     * @param id    Node ID of the Managed Node where this TE Path is attached.
     * @param key   TE Path Key to be deleted.
     */
    void deleteTePath(NodeId id, TePathKey key);
}
