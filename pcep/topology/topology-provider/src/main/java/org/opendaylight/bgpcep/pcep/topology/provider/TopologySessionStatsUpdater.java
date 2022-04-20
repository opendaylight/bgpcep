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

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.common.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for periodically updating PCEP session statistics.
 */
final class TopologySessionStatsUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(TopologySessionStatsUpdater.class);

    private final DataBroker dataBroker;
    private final long periodMillis;

    private Timer timer;

    TopologySessionStatsUpdater(final DataBroker dataBroker, final long periodSeconds) {
        this.dataBroker = requireNonNull(dataBroker);
        checkArgument(periodSeconds > 0, "Invalid update period %s s", periodSeconds);
        periodMillis = TimeUnit.SECONDS.toMillis(periodSeconds);
        timer = new Timer("PCEP session statistics updater", true);
        timer.schedule(newTask(), 0);
        LOG.info("PCEP session statistics updater started");
    }

    synchronized void shutdown() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            LOG.info("PCEP session statistics updater stopped");
        }
    }

    private void startUpdate() {
        synchronized (this) {
            if (timer == null) {
                return;
            }
        }

        LOG.debug("Starting session statistics update");
        final var sw = Stopwatch.createStarted();
        final var updateFuture = SettableFuture.<Empty>create();
        final var tx = dataBroker.newWriteOnlyTransaction();

        // FIXME: visit all registered node states

        LOG.debug("Collected update participants in {}", sw);

        tx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.debug("Statistics updated in {}", sw);
                updateSuccessful(completeFuture());
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOG.warn("Statistics update failed after {}, retrying", sw, cause);
                updateFailed(completeFuture());
            }

            private long completeFuture() {
                updateFuture.set(Empty.value());
                return sw.elapsed(TimeUnit.MILLISECONDS);
            }
        }, MoreExecutors.directExecutor());
    }

    private void updateFailed(final long elapsedMillis) {
        // FIXME: perform a retry with gradual backoff
        updateSuccessful(elapsedMillis);
    }

    private synchronized void updateSuccessful(final long elapsedMillis) {
        if (timer == null) {
            return;
        }

        long nextMillis = periodMillis - elapsedMillis;
        if (nextMillis < periodMillis / 2) {
            LOG.info("Last update took {}ms, scheduling a full period {}ms", elapsedMillis, periodMillis);
            nextMillis = periodMillis;
        }
        timer.schedule(newTask(), nextMillis);
    }

    private TimerTask newTask() {
        return new TimerTask() {
            @Override
            public void run() {
                startUpdate();
            }
        };
    }
}
