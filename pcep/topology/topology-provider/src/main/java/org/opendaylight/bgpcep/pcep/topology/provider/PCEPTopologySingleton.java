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
final class PCEPTopologySingleton {
    // Common class for possible states
    private abstract static class State {
        // Nothing here
    }

    // The state for alive-and-kickin'. We are registered for cluster-wide instantiation
    private final class Active extends State implements ClusterSingletonService {
        private final InstructionScheduler scheduler;

        @GuardedBy("this")
        private SettableFuture<Empty> closeFuture;
        @GuardedBy("this")
        private PCEPTopologyInstance instance;
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
                LOG.trace("Topology {} instance {} is closed, instantiation skipped", topology,
                    PCEPTopologySingleton.this);
                return;
            }

            LOG.trace("Topology {} instance {} instantiating", topology, PCEPTopologySingleton.this);
            instance = new PCEPTopologyInstance(topology, tracker, scheduler, tracker.bundleContext);
        }

        @Override
        public ListenableFuture<?> closeServiceInstance() {
            // First adjust our state under the lock...
            final SettableFuture<Empty> close;
            final ListenableFuture<?> ret;

            synchronized (this) {
                LOG.trace("Topology {} instance {} closing", topology, PCEPTopologySingleton.this);
                if (closeFuture == null) {
                    close = closeFuture = SettableFuture.create();
                } else {
                    close = closeFuture;
                }

                ret = instance.terminate();
                instance = null;
            }

            // ... and then add completion callback. Order of operations is significant, as we want to update our state
            // before we give anybody a chance to react to closeFuture.
            ret.addListener(() -> {
                LOG.trace("Topology {} instance {} completing close", topology, PCEPTopologySingleton.this);
                synchronized (this) {
                    closeFuture = null;
                }
                LOG.trace("Topology {} instance {} closed", topology, PCEPTopologySingleton.this);
                close.set(Empty.getInstance());
            }, MoreExecutors.directExecutor());
            return close;
        }

        ListenableFuture<?> terminate() {
            // Acquire the service termination future and shut down the scheduler once it completes
            final var future = lockedTerminate();
            future.addListener(scheduler::close, MoreExecutors.directExecutor());
            return future;
        }

        // This part of the shutdown procedure needs to synchronize with instantiation and closure
        private synchronized ListenableFuture<?> lockedTerminate() {
            verifyNotNull(reg, "Topology %s instance %s already terminating", topology, PCEPTopologySingleton.this);

            final ListenableFuture<?> ret;
            if (closeFuture == null) {
                // Service is not being closed, we need to create a future...
                ret = closeFuture = SettableFuture.create();
                if (instance == null) {
                    // ... and there is no instance, hence we need to also compete it immediate
                    closeFuture.set(Empty.getInstance());
                }
            } else {
                // Service close is already going on, reuse that future
                ret = closeFuture;
            }

            // Close the registration, potentially triggering closeServiceInstance(), which may even run on this thread
            reg.close();
            reg = null;
            return ret;
        }
    }

    // Instance terminating, we never get out of this state
    private static final class Terminating extends State {
        final @NonNull ListenableFuture<?> future;

        Terminating(final ListenableFuture<?> future) {
            this.future = requireNonNull(future);
        }
    }

    // Instance is waiting for a previous incarnation to finish terminating
    private static final class Waiting extends State {
        final @NonNull ListenableFuture<?> future;

        Waiting(final ListenableFuture<?> future) {
            this.future = requireNonNull(future);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologySingleton.class);

    private final PCEPTopologyTracker tracker;
    private final TopologyKey topology;

    @GuardedBy("this")
    private State state;
    @GuardedBy("this")
    private SettableFuture<?> cleanupFuture;

    private PCEPTopologySingleton(final PCEPTopologySingleton previous, final ListenableFuture<?> future) {
        tracker = previous.tracker;
        topology = previous.topology;
        state = new Waiting(future);
        future.addListener(this::becomeActive, MoreExecutors.directExecutor());
    }

    PCEPTopologySingleton(final PCEPTopologyTracker tracker, final TopologyKey topology) {
        this.tracker = requireNonNull(tracker);
        this.topology = requireNonNull(topology);
        state = new Active();
    }

    synchronized void destroy() {
        if (state instanceof Active) {
            LOG.trace("Starting destruction of topology {} instance {}", topology, this);
            becomeTerminating(((Active) state).terminate());
        } else if (state instanceof Waiting) {
            LOG.trace("Topology {} instance {} destroyed while waiting", topology, this);
            becomeTerminating(((Waiting) state).future);
        } else {
            verify(state instanceof Terminating, "Unexpected state %s", state);
            LOG.trace("Topology {} instance {} is already being destroyed", topology, this);
        }
    }

    @NonNull PCEPTopologySingleton resurrect() {
        return new PCEPTopologySingleton(this, acquireCleanup());
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

    private synchronized @NonNull ListenableFuture<?> acquireCleanup() {
        return verifyTerminating().future;
    }

    private synchronized void becomeActive() {
        if (state instanceof Waiting) {
            LOG.trace("Topology {} instance {} becoming active", topology, this);
            state = new Active();
        } else {
            verifyTerminating();
            LOG.trace("Skipping activation of terminated topology {} instance {}", topology, this);
        }
    }

    @Holding("this")
    private void becomeTerminating(final ListenableFuture<?> future) {
        state = new Terminating(future);
        future.addListener(() -> tracker.finishDestroy(topology, this), MoreExecutors.directExecutor());
    }

    @Holding("this")
    private Terminating verifyTerminating() {
        verify(state instanceof Terminating, "Unexpected topology %s instance %s state %s", topology, this, state);
        return (Terminating) state;
    }
}
