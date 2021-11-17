/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.SettableFuture;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * Glue between asynchronous lifecycle driven by {@link PCEPTopologyTracker} and asynchronous instantiation of a
 * {@link ClusterSingletonService} for a particular topology.
 */
final class PCEPTopologyInstance {
    private final SettableFuture<Empty> terminationFuture = SettableFuture.create();
    private final PCEPTopologyTracker tracker;
    private final TopologyKey topology;

    PCEPTopologyInstance(final PCEPTopologyTracker tracker, final TopologyKey topology) {
        this.tracker = requireNonNull(tracker);
        this.topology = requireNonNull(topology);
    }

    void destroy() {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    @NonNull PCEPTopologyInstance resurrect() {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }
}
