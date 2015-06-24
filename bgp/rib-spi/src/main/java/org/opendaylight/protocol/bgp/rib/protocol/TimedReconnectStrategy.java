/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.rib.protocol;

import com.google.common.base.Preconditions;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 22.6.2015.
 */
@ThreadSafe
public final class TimedReconnectStrategy implements ReconnectStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(TimedReconnectStrategy.class);
    private final EventExecutor executor;
    private final Long deadline;
    private final Long maxAttempts;
    private final Long maxSleep;
    private final double sleepFactor;
    private final int connectTime;
    private final long minSleep;
    @GuardedBy("this")
    private long attempts;
    @GuardedBy("this")
    private long lastSleep;
    @GuardedBy("this")
    private boolean scheduled;

    public TimedReconnectStrategy(EventExecutor executor, int connectTime, long minSleep, double sleepFactor, Long maxSleep, Long maxAttempts, Long deadline) {
        Preconditions.checkArgument(maxSleep == null || minSleep <= maxSleep.longValue());
        Preconditions.checkArgument(sleepFactor >= 1.0D);
        Preconditions.checkArgument(connectTime >= 0);
        this.executor = (EventExecutor)Preconditions.checkNotNull(executor);
        this.deadline = deadline;
        this.maxAttempts = maxAttempts;
        this.minSleep = minSleep;
        this.maxSleep = maxSleep;
        this.sleepFactor = sleepFactor;
        this.connectTime = connectTime;
    }

    public synchronized Future<Void> scheduleReconnect(Throwable cause) {
        LOG.debug("Connection attempt failed", cause);
        Preconditions.checkState(!this.scheduled);
        long now = System.nanoTime();
        if(this.maxAttempts != null && this.attempts >= this.maxAttempts.longValue()) {
            return this.executor.newFailedFuture(new Throwable("Maximum reconnection attempts reached"));
        } else if(this.deadline != null && this.deadline.longValue() <= now) {
            return this.executor.newFailedFuture(new TimeoutException("Reconnect deadline reached"));
        } else {
            if(this.attempts != 0L) {
                this.lastSleep = (long)((double)this.lastSleep * this.sleepFactor);
            } else {
                this.lastSleep = this.minSleep;
            }

            if(this.maxSleep != null && this.lastSleep > this.maxSleep.longValue()) {
                LOG.debug("Capped sleep time from {} to {}", Long.valueOf(this.lastSleep), this.maxSleep);
                this.lastSleep = this.maxSleep.longValue();
            }

            ++this.attempts;
            if(this.deadline != null && this.deadline.longValue() <= now + TimeUnit.MILLISECONDS.toNanos(this.lastSleep)) {
                return this.executor.newFailedFuture(new TimeoutException("Next reconnect would happen after deadline"));
            } else {
                LOG.debug("Connection attempt {} sleeping for {} milliseconds", Long.valueOf(this.attempts), Long.valueOf(this.lastSleep));
                if(this.lastSleep == 0) {
                    return this.executor.newSucceededFuture(null);
                } else {
                    this.scheduled = true;
                    return this.executor.schedule(new Callable() {
                        public Void call() throws TimeoutException {
                            Object var1 = TimedReconnectStrategy.this;
                            synchronized(TimedReconnectStrategy.this) {
                                Preconditions.checkState(TimedReconnectStrategy.this.scheduled);
                                TimedReconnectStrategy.this.scheduled = false;
                                return null;
                            }
                        }
                    }, this.lastSleep, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    public synchronized void reconnectSuccessful() {
        Preconditions.checkState(!this.scheduled);
        this.attempts = 0L;
    }

    public int getConnectTimeout() throws TimeoutException {
        int timeout = this.connectTime;
        if(this.deadline != null) {
            long now = System.nanoTime();
            if(now >= this.deadline.longValue()) {
                throw new TimeoutException("Reconnect deadline already passed");
            }

            long left = TimeUnit.NANOSECONDS.toMillis(this.deadline.longValue() - now);
            if(left < 1L) {
                throw new TimeoutException("Connect timeout too close to deadline");
            }

            if((long)timeout > left) {
                timeout = (int)left;
            } else if(timeout == 0) {
                timeout = left <= 2147483647L?(int)left:2147483647;
            }
        }

        return timeout;
    }
}