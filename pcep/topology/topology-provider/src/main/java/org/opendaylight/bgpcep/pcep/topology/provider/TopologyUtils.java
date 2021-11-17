/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Utilities for dealing with various PCEP topology-related constructs.
 */
final class TopologyUtils {
    private TopologyUtils() {
        // Hidden on purpose
    }

    static @NonNull String friendlyId(final KeyedInstanceIdentifier<Topology, TopologyKey> identifier) {
        return friendlyId(identifier.getKey());
    }

    static @NonNull String friendlyId(final TopologyKey key) {
        return friendlyId(key.getTopologyId());
    }

    static @NonNull String friendlyId(final TopologyId id) {
        return id.getValue();
    }
}
