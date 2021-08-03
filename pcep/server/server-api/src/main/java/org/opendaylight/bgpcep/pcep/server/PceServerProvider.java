/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.server;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.manager.rev210720.managed.path.ManagedNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

public interface PceServerProvider {

    /**
     * Return the instance of the Path Computation server.
     *
     * @return  Path Computation Object
     */
    PathComputation getPathComputation();

    /**
     * Return the instance of the Path Manager.
     *
     * @return  Path Manager Object
     */
    PathManager getPathManager();

    /**
     * Register a PCC identified by its Node ID into the PCE Server.
     *
     * @param id    Node ID of the PCC to be registered.
     *
     * @return      New Managed Node.
     */
    ManagedNode registerPCC(NodeId id);

    /**
     * Mark the PCC is synchronous i.e. all LSPs have been reported to the PCE.
     *
     * @param id    Node ID of the PCC
     */
    void syncPCC(NodeId id);

    /**
     * Register the Reported LSP into the PCE Server.
     *
     * @param id    Node ID of the PCC that reports this LSP.
     * @param rl    Reported LSP.
     */

    void registerReportedLSP(NodeId id, ReportedLsp rl);

    /**
     * Unregister a PCC identified by its Node ID into the PCE Server.
     *
     * @param id    Node ID of the PCC to be registered.
     */
    void unRegisterPCC(NodeId id);

    /**
     * Unregister the Reported LSP into the PCE Server.
     *
     * @param id    Node ID of the PCC that reports this LSP.
     * @param name  Name of the LSP to be removed.
     */
    void unRegisterLSP(NodeId id, String name);
}
