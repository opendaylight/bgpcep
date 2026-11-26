/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2026 PANTHEON.tech, s.r.o.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.topology;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

/**
 * A reference to a {@link Topology} instance.
 */
@NonNullByDefault
public interface TopologyReference {
    /**
     * {@return the {@link DataObjectIdentifier} of the topology instance}
     */
    DataObjectIdentifier.WithKey<Topology, TopologyKey> instanceIdentifier();

    /**
     * {@return the equivalent of {@code instanceIdentifier().key()}}
     *
     * @since 3.0.0
     */
    default TopologyKey topologyKey() {
        return instanceIdentifier().key();
    }

    /**
     * {@return a new {@link TopologyReference} pointing to specified instance}
     *
     * @since 3.0.0
     */
    static TopologyReference of(final TopologyKey topologyKey) {
        return of(DataObjectIdentifier.builder(NetworkTopology.class).child(Topology.class, topologyKey).build());
    }

    /**
     * {@return a new {@link TopologyReference} pointing to specified instance}
     *
     * @since 3.0.0
     */
    static TopologyReference of(final DataObjectIdentifier.WithKey<Topology, TopologyKey> instanceIdentifier) {
        return new DefaultTopologyReference(instanceIdentifier);
    }
}
