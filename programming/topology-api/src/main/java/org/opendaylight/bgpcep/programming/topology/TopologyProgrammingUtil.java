/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.topology;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.programming.rev131102.TopologyInstructionInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;

public final class TopologyProgrammingUtil {
    private TopologyProgrammingUtil() {
        // Hidden on purpose
    }

    @SuppressWarnings("unchecked")
    public static WithKey<Topology, TopologyKey> topologyForInput(final TopologyInstructionInput input) {
        final var biid = input.requireNetworkTopologyRef().getValue();
        return switch(biid) {
            case WithKey<?, ?> id -> (WithKey<Topology, TopologyKey>) id;
            default -> throw new IllegalArgumentException("Unexpected non-object reference " + biid);
        };
    }
}
