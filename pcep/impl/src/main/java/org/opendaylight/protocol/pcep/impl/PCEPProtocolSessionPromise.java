/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public final class PCEPProtocolSessionPromise<S extends PCEPSession> extends DefaultPromise<S> {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPProtocolSessionPromise.class);
    private final ReconnectStrategy strategy;
    private final Bootstrap b;
    private InetSocketAddress address;
    @GuardedBy("this")
    private Future<?> pending;

    PCEPProtocolSessionPromise(final EventExecutor executor, final InetSocketAddress address, final ReconnectStrategy
        strategy, final Bootstrap b) {
        super(executor);
        this.strategy = Preconditions.checkNotNull(strategy);
        this.address = Preconditions.checkNotNull(address);
        this.b = Preconditions.checkNotNull(b);
    }

    synchronized void connect() {
        final PCEPProtocolSessionPromise lock = this;

        try {
            final int e = this.strategy.getConnectTimeout();
            LOG.debug("Promise {} attempting connect for {}ms", lock, Integer.valueOf(e));
            if (this.address.isUnresolved()) {
                this.address = new InetSocketAddress(this.address.getHostName(), this.address.getPort());
            }

            this.b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, e);
            final ChannelFuture connectFuture = this.b.connect(this.address);
            connectFuture.addListener(new PCEPProtocolSessionPromise.BootstrapConnectListener(lock));
            this.pending = connectFuture;
        } catch (Exception e) {
            LOG.info("Failed to connect to {}", this.address, e);
            this.setFailure(e);
        }

    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            this.pending.cancel(mayInterruptIfRunning);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized Promise<S> setSuccess(final S result) {
        LOG.debug("Promise {} completed", this);
        this.strategy.reconnectSuccessful();
        return super.setSuccess(result);
    }

    private class BootstrapConnectListener implements ChannelFutureListener {
        private final Object lock;

        public BootstrapConnectListener(final Object lock) {
            this.lock = lock;
        }

        @Override
        public void operationComplete(final ChannelFuture cf) throws Exception {
            synchronized (this.lock) {
                PCEPProtocolSessionPromise.LOG.debug("Promise {} connection resolved", this.lock);
                Preconditions.checkState(PCEPProtocolSessionPromise.this.pending.equals(cf));
                if (PCEPProtocolSessionPromise.this.isCancelled()) {
                    if (cf.isSuccess()) {
                        PCEPProtocolSessionPromise.LOG.debug("Closing channel for cancelled promise {}", this.lock);
                        cf.channel().close();
                    }

                } else if (cf.isSuccess()) {
                    PCEPProtocolSessionPromise.LOG.debug("Promise {} connection successful", this.lock);
                } else {
                    PCEPProtocolSessionPromise.LOG.debug("Attempt to connect to {} failed", PCEPProtocolSessionPromise.this.address, cf.cause());
                    final Future rf = PCEPProtocolSessionPromise.this.strategy.scheduleReconnect(cf.cause());
                    rf.addListener(new PCEPProtocolSessionPromise.BootstrapConnectListener.ReconnectingStrategyListener());
                    PCEPProtocolSessionPromise.this.pending = rf;
                }
            }
        }

        private final class ReconnectingStrategyListener implements FutureListener<Void> {
            private ReconnectingStrategyListener() {
            }

            @Override
            public void operationComplete(final Future<Void> sf) {
                synchronized (BootstrapConnectListener.this.lock) {
                    Preconditions.checkState(PCEPProtocolSessionPromise.this.pending.equals(sf));
                    if (!PCEPProtocolSessionPromise.this.isCancelled()) {
                        if (sf.isSuccess()) {
                            PCEPProtocolSessionPromise.this.connect();
                        } else {
                            PCEPProtocolSessionPromise.this.setFailure(sf.cause());
                        }
                    }

                }
            }
        }
    }
}
