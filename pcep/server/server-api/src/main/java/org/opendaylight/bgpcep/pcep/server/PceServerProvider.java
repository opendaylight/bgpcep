/*
 * Copyright (c) 2020 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.server;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public interface PceServerProvider {

    /**
     * Return the instance of the Path Computation server.
     *
     * @return  Path Computation Object
     */
    PathComputation getPathComputation();

    /**
     * Register PCEP Topology into PCE Server to manage LSP.
     *
     * @param topology    Configured PCEP Topology
     */
    void registerPcepTopology(KeyedInstanceIdentifier<Topology, TopologyKey> topology);

    /**
     * Un Register current PCEP Topology into PCE Server.
     *
     */
    void unRegisterPcepTopology();
}
