/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.index.qual.NonNegative;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class dedicated to driving statistics updates for PCEP sessions.
 */
// FIXME: This effectively is sampling the datastore: we will need this sort of timer for subscribed notifications on
//        dynamic data, hence this really should live in mdsal-common.
final class TopologyStatsScheduler {
    @FunctionalInterface
    interface StatsUpdateCallback {

        @NonNull FluentFuture<? extends CommitInfo> updateStats();
    }

    private final class StatsUpdateTask extends AbstractObjectRegistration<StatsUpdateCallback>
            implements TimerTask, FutureCallback<CommitInfo>, Runnable {
        private long expectedNow = System.nanoTime();
        private Timeout nextTimeout;

        StatsUpdateTask(final @NonNull StatsUpdateCallback callback) {
            super(callback);
        }

        @Override
        public void run(final Timeout timeout) {
            executor.execute(this);
        }

        @Override
        public void run() {
            final var callback = getInstance();

            FluentFuture<? extends CommitInfo> finish;
            try {
                finish = callback.updateStats();
            } catch (RuntimeException e) {
                LOG.warn("Callback {} failed", callback, e);
                finish = FluentFutures.immediateFailedFluentFuture(e);
            }

            Futures.addCallback(finish, this, MoreExecutors.directExecutor());
        }

        @Override
        public void onSuccess(final CommitInfo result) {
            LOG.trace("Statistics update task {} succeeded", getInstance());
            if (notClosed()) {
                final long period = periodNanos;
                final long now = System.nanoTime();
                long targetPeriod = expectedNow - now + period;
                // TODO: this certainly can be smarter: we really want to recover the time lost
                while (targetPeriod < 0) {
                    targetPeriod += period;
                }
                schedule(now, targetPeriod);
            }
        }

        @Override
        public void onFailure(final Throwable cause) {
            LOG.warn("Statistics update task {} failed", getInstance(), cause);
            if (notClosed()) {
                // Delay a full update period
                schedule(System.nanoTime(), periodNanos);
            }
        }

        @Override
        protected void removeRegistration() {
            tasks.remove(this);
            if (!nextTimeout.cancel()) {
                LOG.debug("Update task {} did not cancel", getInstance());
            }
        }

        private void schedule(final long now, final @NonNegative long period) {
            expectedNow = now + period;
            schedule(period);
        }

        void schedule(final @NonNegative long period) {
            LOG.trace("Scheduling update task {} scheduled {}ns in future", getInstance(), period);
            nextTimeout = timer.newTimeout(this, period, TimeUnit.NANOSECONDS);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(TopologyStatsScheduler.class);

    // Tasks currently registered
    private final Set<StatsUpdateTask> tasks = ConcurrentHashMap.newKeySet();
    // Timer for scheduling timeouts, which in turn drive statistics update tasks
    private final Timer timer;
    // Executor for invoking statistics update tasks
    private final Executor executor;

    private volatile @NonNegative long periodNanos;

    TopologyStatsScheduler(final Timer timer, final Executor executor, final int periodSeconds) {
        this.timer = requireNonNull(timer);
        this.executor = requireNonNull(executor);
        setPeriod(periodSeconds);
    }

    void setPeriod(final int periodSeconds) {
        final long newPeriodNanos = TimeUnit.SECONDS.toNanos(periodSeconds);
        checkArgument(newPeriodNanos >= 0, "Negative period %s ns", newPeriodNanos);
        periodNanos = newPeriodNanos;
    }

    Registration registerTask(final @NonNull StatsUpdateCallback callback) {
        final var ret = new StatsUpdateTask(requireNonNull(callback));
        tasks.add(ret);
        ret.schedule(0);
        return ret;
    }
}
