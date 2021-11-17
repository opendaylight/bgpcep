/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Glue between asynchronous lifecycle driven by {@link PCEPTopologyTracker} and asynchronous instantiation of a
 * {@link ClusterSingletonService} for a particular topology.
 */
final class PCEPTopologyInstance {
    // Common class for possible states
    private abstract static class State {
        // Nothing here
    }

    // The state for alive-and-kickin'. We are registered for cluster-wide instantiation
    private final class Active extends State implements ClusterSingletonService {
        private final InstructionScheduler scheduler;

        @GuardedBy("this")
        private SettableFuture<Empty> future;
        @GuardedBy("this")
        private Registration reg;

        Active() {
            scheduler = tracker.instructionSchedulerFactory.createInstructionScheduler(
                topology.getTopologyId().getValue());
            reg = tracker.singletonService.registerClusterSingletonService(this);
        }

        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return scheduler.getIdentifier();
        }

        @Override
        public synchronized void instantiateServiceInstance() {
            if (reg == null) {
                LOG.debug("Topology {} instance {} is closed, instantiation skipped", topology,
                    PCEPTopologyInstance.this);
                return;
            }

            LOG.debug("Topology {} instance {} instantiating", topology, PCEPTopologyInstance.this);

            // FIXME: implement this
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized ListenableFuture<?> closeServiceInstance() {
            LOG.debug("Topology {} instance {} closing", topology, PCEPTopologyInstance.this);

            // FIXME: implement this
            // FIXME: also notice if 'future' is non-null and complete it when the returned future completes
            throw new UnsupportedOperationException();
        }

        synchronized ListenableFuture<Empty> terminate() {
            verifyNotNull(reg, "Topology %s instance %s already terminating", topology, PCEPTopologyInstance.this);
            reg.close();
            reg = null;




            final var ret = SettableFuture.<Empty>create();
            future = ret;

            // FIXME: we need to shut down the scheduler once we become inactive


            // FIXME: notice if we are already closed and short-circuit in that case

            return ret;
        }
    }

    // Instance terminating, we never get out of this state
    private static final class Terminating extends State {
        final @NonNull ListenableFuture<Empty> future;

        Terminating(final ListenableFuture<Empty> future) {
            this.future = requireNonNull(future);
        }
    }

    // Instance is waiting for a previous incarnation to finish terminating
    private static final class Waiting extends State {
        final @NonNull ListenableFuture<Empty> future;

        Waiting(final ListenableFuture<Empty> future) {
            this.future = requireNonNull(future);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyInstance.class);

    private final PCEPTopologyTracker tracker;
    private final TopologyKey topology;

    @GuardedBy("this")
    private State state;
    @GuardedBy("this")
    private SettableFuture<Empty> cleanupFuture;

    private PCEPTopologyInstance(final PCEPTopologyInstance previous, final ListenableFuture<Empty> future) {
        tracker = previous.tracker;
        topology = previous.topology;
        state = new Waiting(future);
        future.addListener(this::becomeActive, MoreExecutors.directExecutor());
    }

    PCEPTopologyInstance(final PCEPTopologyTracker tracker, final TopologyKey topology) {
        this.tracker = requireNonNull(tracker);
        this.topology = requireNonNull(topology);
        state = new Active();
    }

    synchronized void destroy() {
        if (state instanceof Active) {
            LOG.debug("Starting destruction of topology {} instance {}", topology, this);
            becomeTerminating(((Active) state).terminate());
        } else if (state instanceof Waiting) {
            LOG.debug("Topology {} instance {} destroyed while waiting", topology, this);
            becomeTerminating(((Waiting) state).future);
        } else {
            verify(state instanceof Terminating, "Unexpected state %s", state);
            LOG.debug("Topology {} instance {} is already being destroyed", topology, this);
        }
    }

    @NonNull PCEPTopologyInstance resurrect() {
        return new PCEPTopologyInstance(this, acquireCleanup());
    }

    void awaitCleanup() {
        try {
            acquireCleanup().get();
        } catch (InterruptedException e) {
            LOG.info("Interrupted while waiting for topology {} cleanup", topology, e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOG.error("Topology {} cleanup failed", topology, e);
        }
    }

    private synchronized @NonNull ListenableFuture<Empty> acquireCleanup() {
        return verifyTerminating().future;
    }

    private synchronized void becomeActive() {
        if (state instanceof Waiting) {
            LOG.debug("Topology {} instance {} becoming active", topology, this);
            state = new Active();
        } else {
            verifyTerminating();
            LOG.debug("Skipping activation of terminated topology {} instance {}", topology, this);
        }
    }

    @Holding("this")
    private void becomeTerminating(final ListenableFuture<Empty> future) {
        state = new Terminating(future);
        future.addListener(() -> tracker.finishDestroy(topology, this), MoreExecutors.directExecutor());
    }

    @Holding("this")
    private Terminating verifyTerminating() {
        verify(state instanceof Terminating, "Unexpected topology %s instance %s state %s", topology, this, state);
        return (Terminating) state;
    }
}
