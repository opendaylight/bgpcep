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

import com.google.common.base.VerifyException;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.api.ServiceGroupIdentifier;
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
    // Common interface for possible states
    private sealed interface State {
        // Nothing here
    }

    // The state for alive-and-kickin'. We are registered for cluster-wide instantiation
    private final class Active implements State, ClusterSingletonService {
        private final AtomicBoolean closed = new AtomicBoolean();
        private final InstructionScheduler scheduler;

        @GuardedBy("this")
        private SettableFuture<Empty> closeFuture;
        @GuardedBy("this")
        private PCEPTopologyInstance instance;
        // accessed once via terminate(), single-threadedness is guarded by 'closed' false->true CAS
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
            if (closed.getAcquire()) {
                LOG.trace("Topology {} instance {} is closed, instantiation skipped", topologyId(),
                    PCEPTopologySingleton.this);
                return;
            }

            LOG.trace("Topology {} instance {} instantiating", topologyId(), PCEPTopologySingleton.this);
            instance = new PCEPTopologyInstance(topology, tracker, scheduler);
        }

        @Override
        public ListenableFuture<?> closeServiceInstance() {
            // First adjust our state under the lock...
            final SettableFuture<Empty> close;
            final ListenableFuture<?> ret;

            synchronized (this) {
                LOG.trace("Topology {} instance {} closing", topologyId(), PCEPTopologySingleton.this);
                if (closeFuture == null) {
                    close = closeFuture = SettableFuture.create();
                } else {
                    close = closeFuture;
                }

                ret = verifyNotNull(instance).terminate();
                instance = null;
            }

            // ... and then add completion callback. Order of operations is significant, as we want to update our state
            // before we give anybody a chance to react to closeFuture.
            ret.addListener(() -> {
                LOG.trace("Topology {} instance {} completing close", topologyId(), PCEPTopologySingleton.this);
                synchronized (this) {
                    closeFuture = null;
                }
                LOG.trace("Topology {} instance {} closed", topologyId(), PCEPTopologySingleton.this);
                close.set(Empty.value());
            }, MoreExecutors.directExecutor());
            return close;
        }

        @NonNull ListenableFuture<?> terminate() {
            if (!closed.compareAndSet(false, true)) {
                throw new VerifyException(
                    "Topology %s instance %s already terminating".formatted(topologyId(), PCEPTopologySingleton.this));
            }

            // Close the registration, potentially triggering closeServiceInstance(), which may even run on this thread
            reg.close();
            reg = null;

            // Acquire the service termination future and shut down the scheduler once it completes
            final var future = lockedTerminate();
            future.addListener(scheduler::close, MoreExecutors.directExecutor());
            return future;
        }

        // This part of the shutdown procedure needs to synchronize with instantiation and closure
        private synchronized @NonNull ListenableFuture<?> lockedTerminate() {
            var ret = closeFuture;
            if (ret != null) {
                // Service close is already going on, reuse that future
                return ret;
            }

            // Service is not being closed, we need to create a future...
            closeFuture = ret = SettableFuture.create();
            if (instance == null) {
                // ... and there is no instance, hence we need to also compete it immediate
                ret.set(Empty.value());
            }
            return ret;
        }
    }

    // Instance terminating, we never get out of this state
    @NonNullByDefault
    private record Terminating(ListenableFuture<?> future) implements State {
        Terminating {
            requireNonNull(future);
        }
    }

    // Instance is waiting for a previous incarnation to finish terminating
    @NonNullByDefault
    private record Waiting(ListenableFuture<?> future) implements State {
        Waiting {
            requireNonNull(future);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologySingleton.class);

    private final @NonNull PCEPTopologyTracker tracker;
    private final @NonNull TopologyKey topology;

    @GuardedBy("this")
    private State state;

    @NonNullByDefault
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
        switch (state) {
            case Active active -> {
                LOG.trace("Starting destruction of topology {} instance {}", topologyId(), this);
                becomeTerminating(active.terminate());
            }
            case Waiting(var future) -> {
                LOG.trace("Topology {} instance {} destroyed while waiting", topologyId(), this);
                becomeTerminating(future);
            }
            case Terminating terminating ->
                LOG.trace("Topology {} instance {} is already being destroyed", topologyId(), this);
        }
    }

    @NonNull PCEPTopologySingleton resurrect() {
        return new PCEPTopologySingleton(this, acquireCleanup());
    }

    void awaitCleanup() {
        try {
            acquireCleanup().get();
        } catch (InterruptedException e) {
            LOG.info("Interrupted while waiting for topology {} cleanup", topologyId(), e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOG.error("Topology {} cleanup failed", topologyId(), e);
        }
    }

    private synchronized @NonNull ListenableFuture<?> acquireCleanup() {
        return verifyTerminating().future;
    }

    private synchronized void becomeActive() {
        if (state instanceof Waiting) {
            LOG.trace("Topology {} instance {} becoming active", topologyId(), this);
            state = new Active();
        } else {
            verifyTerminating();
            LOG.trace("Skipping activation of terminated topology {} instance {}", topologyId(), this);
        }
    }

    @GuardedBy("this")
    private void becomeTerminating(final @NonNull ListenableFuture<?> future) {
        state = new Terminating(future);
        future.addListener(() -> tracker.finishDestroy(topology, this), MoreExecutors.directExecutor());
    }

    @GuardedBy("this")
    private @NonNull Terminating verifyTerminating() {
        if (state instanceof Terminating terminating) {
            return terminating;
        }
        throw new VerifyException("Unexpected topology %s instance %s state %s".formatted(topologyId(), this, state));
    }

    private String topologyId() {
        return TopologyUtils.friendlyId(topology);
    }
}
