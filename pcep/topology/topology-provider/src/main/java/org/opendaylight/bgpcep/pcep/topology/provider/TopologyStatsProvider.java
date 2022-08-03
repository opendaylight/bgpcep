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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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

    private final Set<Task> tasks = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor;
    private final Timer timer;

    TopologyStatsProvider(final Timer timer) {
        this.timer = requireNonNull(timer);
        executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("odl-pcep-stats-%d")
            .build());

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
            state = timer.newTimeout(this, updateIntervalNanos, TimeUnit.NANOSECONDS);
        }

        @Override
        public void run(final Timeout timeout) {
            if (notClosed()) {
                LOG.debug("Task {} is closed, ignoring timeout {}", this, timeout);
                return;
            }

            final var witness = STATE.compareAndExchange(this, timeout, null);
            if (witness != timeout) {
                LOG.debug("Task {} ignoring unexpected timeout {} in state {}", this, timeout, witness);
                return;
            }

            final var sw = Stopwatch.createStarted();
            state = executor.submit(() -> updateStatistics(sw));
        }

        private void updateStatistics(final Stopwatch sw) {
            LOG.debug("Resumed processing task {} after {}", this, sw);
            if (isClosed()) {
                // Already closed
                return;
            }

            final var prevState = state;
            if (prevState instanceof Future<?> execFuture && !execFuture.isDone()) {
                final var future = getInstance().updateStatistics();
                LOG.debug("Task {} update submitted in {}", this, sw);
                state = future;
                future.addCallback(new FutureCallback<CommitInfo>() {
                    @Override
                    public void onSuccess(final CommitInfo result) {
                        LOG.debug("Task {} update completed in {}", this, sw);
                        reschedule(future, sw.elapsed(TimeUnit.NANOSECONDS));
                    }

                    @Override
                    public void onFailure(final Throwable cause) {
                        LOG.debug("Task {} update failed in {}", this, sw, cause);
                        reschedule(future, 0);
                    }
                }, executor);
            } else {
                LOG.debug("Task {} ignoring unexpected update in state {}", this, prevState);
            }
        }

        private void reschedule(final Object expectedState, final long elapsedNanos) {
            if (isClosed()) {
                // Already closed
                return;
            }
            var witness = STATE.compareAndExchange(this, expectedState, null);
            if (witness != expectedState) {
                LOG.debug("Task {} ignoring reschedule in unexpected state {}", this, witness);
                return;
            }

            long remainingNanos = updateIntervalNanos - elapsedNanos;
            if (remainingNanos < 0) {
                remainingNanos = updateIntervalNanos;
            }
            state = timer.newTimeout(this, remainingNanos, TimeUnit.NANOSECONDS);
        }

        @Override
        protected void removeRegistration() {
            tasks.remove(this);

            final var prevState = state;
            if (prevState instanceof Timeout timeout) {
                timeout.cancel();
                STATE.compareAndSet(this, prevState, null);
            } else if (prevState instanceof Future<?> future) {
                if (!(future instanceof FluentFuture)) {
                    future.cancel(false);
                    STATE.compareAndSet(this, prevState, null);
                }
            } else {
                LOG.warn("Task {} in unexpected state {}", this, prevState);
            }
            getInstance().removeStatistics().addCallback(new FutureCallback<CommitInfo>() {
                @Override
                public void onSuccess(final CommitInfo result) {
                    LOG.debug("Task {} removed state", this);
                }

                @Override
                public void onFailure(final Throwable cause) {
                    LOG.warn("Task {} failed to remove state", this, cause);
                }
            }, MoreExecutors.directExecutor());
        }
    }
}
