/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.NoOpObjectRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TopologyStatsProvider implements SessionStateRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyStatsProvider.class);
    private static final ThreadFactory THREAD_FACTORY =
        Thread.ofVirtual().name("odl-pcep-topology-stats-", 0).factory();

    private final Set<Task> tasks = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor;
    private final Timer timer;

    TopologyStatsProvider(final Timer timer) {
        this.timer = requireNonNull(timer);
        executor = Executors.newSingleThreadExecutor(THREAD_FACTORY);
        LOG.info("TopologyStatsProvider started");
    }

    void shutdown() {
        if (executor.isShutdown()) {
            LOG.debug("TopologyStatsProvider already shut down");
            return;
        }

        LOG.info("Closing TopologyStatsProvider service.");
        final var toRun = executor.shutdownNow();
        while (!tasks.isEmpty()) {
            tasks.forEach(Task::close);
        }
        toRun.forEach(Runnable::run);
    }

    @Override
    public ObjectRegistration<SessionStateUpdater> bind(final SessionStateUpdater sessionState) {
        if (executor.isShutdown()) {
            LOG.debug("Ignoring bind of Pcep Node {}", sessionState);
            return NoOpObjectRegistration.of(sessionState);
        }

        final var task = new Task(sessionState);
        tasks.add(task);
        return task;
    }

    private final class Task extends AbstractObjectRegistration<SessionStateUpdater> implements TimerTask {
        // Singleton state objects, used when we do not have no underlying state
        private enum SimpleState {
            /**
             * The task has been cancelled via {@link Task#close()}.
             */
            CANCELLED,
            /**
             * The task is awaiting scheduling with the timer.
             */
            UNSCHEDULED,
            /**
             * The task is awaiting submission to the executor.
             */
            UNSUBMITTED,
        }

        private static final VarHandle STATE;

        static {
            try {
                STATE = MethodHandles.lookup().findVarHandle(Task.class, "state", Object.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private volatile Object state;

        Task(final @NonNull SessionStateUpdater instance) {
            super(instance);

            final long updateInterval = instance.updateInterval();
            if (updateInterval < 1) {
                LOG.debug("Task {} has non-positive interval {}, not scheduling it", this, updateInterval);
                state = SimpleState.UNSCHEDULED;
            } else {
                schedule(updateInterval);
            }
        }

        // run() may only access state once we exit this method
        private synchronized void schedule(final long delay) {
            state = timer.newTimeout(this, delay, TimeUnit.NANOSECONDS);
        }

        @Override
        public void run(final Timeout timeout) {
            if (isClosed()) {
                LOG.debug("Task {} is closed, ignoring timeout {}", this, timeout);
                return;
            }

            // ensure schedule() resulting in 'timeout' has finished
            final Object witness;
            synchronized (this) {
                witness = STATE.compareAndExchange(this, timeout, SimpleState.UNSUBMITTED);
            }
            if (witness != timeout) {
                LOG.debug("Task {} ignoring unexpected timeout {} in state {}", this, timeout, witness);
                return;
            }

            final var sw = Stopwatch.createStarted();

            // atomic submit and state update
            synchronized (this) {
                state = executor.submit(() -> updateStatistics(sw));
            }
        }

        private void updateStatistics(final Stopwatch sw) {
            LOG.debug("Resumed processing task {} after {}", this, sw);
            if (isClosed()) {
                // Already closed
                return;
            }

            // ensure run() resulting it this invocation has finished updating state before reading it
            final Object prevState;
            synchronized (this) {
                prevState = state;
            }
            updateStatistics(prevState, sw);
        }

        private void updateStatistics(final Object prevState, final Stopwatch sw) {
            if (!(prevState instanceof Future<?> execFuture)) {
                LOG.debug("Task {} ignoring unexpected update in state {}", this, prevState);
                return;
            }
            if (execFuture.isDone()) {
                LOG.debug("Task {} ignoring unexpected update when {} is done", this, execFuture);
                return;
            }

            final var future = getInstance().updateStatistics();
            LOG.debug("Task {} update submitted in {}", this, sw);
            state = future;

            future.addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    LOG.debug("Task {} update completed in {}", Task.this, sw);
                    reschedule(future, sw.elapsed(TimeUnit.NANOSECONDS));
                }

                @Override
                public void onFailure(final Throwable cause) {
                    LOG.debug("Task {} update failed in {}", Task.this, sw, cause);
                    reschedule(future, 0);
                }
            }, executor);
        }

        private void reschedule(final Object expectedState, final long elapsedNanos) {
            if (isClosed()) {
                // Already closed
                return;
            }
            var witness = STATE.compareAndExchange(this, expectedState, SimpleState.UNSCHEDULED);
            if (witness != expectedState) {
                LOG.debug("Task {} ignoring reschedule in unexpected state {}", this, witness);
                return;
            }

            final var updateInterval = getInstance().updateInterval();
            if (updateInterval < 1) {
                LOG.debug("Task {} has non-positive interval {}, skipping reschedule", this, updateInterval);
                return;
            }

            long remainingNanos = updateInterval - elapsedNanos;
            if (remainingNanos < 0) {
                remainingNanos = updateInterval;
            }
            schedule(remainingNanos);
        }

        @Override
        protected void removeRegistration() {
            tasks.remove(this);

            final var prevState = state;
            switch (prevState) {
                case Timeout timeout -> {
                    // cancel timeout
                    timeout.cancel();
                    setCancelled(prevState);
                }
                case Future<?> future -> {
                    // do not cancel datastore commit futures
                    if (!(future instanceof FluentFuture)) {
                        future.cancel(false);
                        setCancelled(prevState);
                    }
                }
                case SimpleState simple ->
                    // this is fine:
                    // - CANCELLED should not be observable, as it set only from here and this method runs at most once
                    // - UNSCHEDULED will circle back to run(), which is a no-op with isClosed()
                    // - UNSUBMITTED will circle back to updateStatistics(), which is a no-op with isClosed()
                    LOG.debug("Task {} closed in state {}", this, prevState);
                case null, default -> LOG.warn("Task {} in unexpected state {}", this, prevState);
            }

            getInstance().removeStatistics().addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    LOG.debug("Task {} removed state", Task.this);
                }

                @Override
                public void onFailure(final Throwable cause) {
                    LOG.warn("Task {} failed to remove state", Task.this, cause);
                }
            }, MoreExecutors.directExecutor());
        }

        private void setCancelled(final Object expected) {
            final var witness = STATE.compareAndExchange(this, expected, SimpleState.CANCELLED);
            if (witness != expected) {
                LOG.warn("Task {} failed to cancel due to unexpected move from {} to {}", this, expected, witness);
            }
        }
    }
}
