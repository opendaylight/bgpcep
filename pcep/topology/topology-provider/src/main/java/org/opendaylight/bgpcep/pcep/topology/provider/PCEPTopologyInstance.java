/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.common.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Glue between asynchronous lifecycle driven by {@link PCEPTopologyTracker} and asynchronous instantiation of a
 * {@link ClusterSingletonService} for a particular topology.
 */
final class PCEPTopologyInstance {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyInstance.class);
    private static final VarHandle CLEANUP_FUTURE;

    static {
        try {
            CLEANUP_FUTURE = MethodHandles.lookup()
                .findVarHandle(PCEPTopologyInstance.class, "cleanupFuture", SettableFuture.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final PCEPTopologyTracker tracker;
    private final TopologyKey topology;

    private volatile SettableFuture<Empty> cleanupFuture;

    PCEPTopologyInstance(final PCEPTopologyTracker tracker, final TopologyKey topology) {
        this.tracker = requireNonNull(tracker);
        this.topology = requireNonNull(topology);
    }

    void destroy() {
        final var future = SettableFuture.<Empty>create();
        final var witness = (SettableFuture<Empty>) CLEANUP_FUTURE.compareAndExchange(this, null, future);
        if (witness == null) {
            destroy(future);
        } else {
            LOG.debug("Topology {} instance {} is already being destroyed", topology, this);
        }
    }

    private void destroy(final SettableFuture<Empty> terminationFuture) {
        LOG.debug("Starting destruction of topology {} instance {}", topology, this);

        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    @NonNull PCEPTopologyInstance resurrect() {
        final var future = acquireCleanup();

        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    void awaitCleanup() {
        final var future = acquireCleanup();

        // FIXME: implement this
        throw new UnsupportedOperationException();
    }

    private @NonNull ListenableFuture<Empty> acquireCleanup() {
        return verifyNotNull(cleanupFuture, "Attepted to await cleanup of live instance %s", this);
    }
}
