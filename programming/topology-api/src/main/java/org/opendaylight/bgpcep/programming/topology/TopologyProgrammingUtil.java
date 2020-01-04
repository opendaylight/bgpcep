/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.topology;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.programming.rev131102.TopologyInstructionInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class TopologyProgrammingUtil {
    private TopologyProgrammingUtil() {
        // Hidden on purpose
    }

    @SuppressWarnings("unchecked")
    public static InstanceIdentifier<Topology> topologyForInput(final TopologyInstructionInput input) {
        requireNonNull(input.getNetworkTopologyRef());
        return (InstanceIdentifier<Topology>) input.getNetworkTopologyRef().getValue();
    }
}
