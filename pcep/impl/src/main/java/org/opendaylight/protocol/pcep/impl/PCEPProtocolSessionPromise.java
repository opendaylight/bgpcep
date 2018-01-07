/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
public final class PCEPProtocolSessionPromise<S extends PCEPSession> extends DefaultPromise<S> {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPProtocolSessionPromise.class);
    private InetSocketAddress address;
    private final int retryTimer;
    private final int connectTimeout;
    private final Bootstrap b;
    @GuardedBy("this")
    private Future<?> pending;

    PCEPProtocolSessionPromise(final EventExecutor executor, final InetSocketAddress address,
            final int retryTimer, final int connectTimeout, final Bootstrap b) {
        super(executor);
        this.address = requireNonNull(address);
        this.retryTimer = retryTimer;
        this.connectTimeout = connectTimeout;
        this.b = requireNonNull(b);
    }

    synchronized void connect() {
        final PCEPProtocolSessionPromise<?> lock = this;

        try {
            LOG.debug("Promise {} attempting connect for {}ms", lock, this.connectTimeout);
            if (this.address.isUnresolved()) {
                this.address = new InetSocketAddress(this.address.getHostName(), this.address.getPort());
            }

            this.b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout);
            this.b.remoteAddress(this.address);
            final ChannelFuture connectFuture = this.b.connect();
            connectFuture.addListener(new BootstrapConnectListener());
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
        }

        return false;
    }

    @Override
    public synchronized Promise<S> setSuccess(final S result) {
        LOG.debug("Promise {} completed", this);
        return super.setSuccess(result);
    }

    private class BootstrapConnectListener implements ChannelFutureListener {
        @Override
        public void operationComplete(final ChannelFuture cf) throws Exception {
            synchronized (PCEPProtocolSessionPromise.this) {
                PCEPProtocolSessionPromise.LOG.debug("Promise {} connection resolved",
                        PCEPProtocolSessionPromise.this);
                Preconditions.checkState(PCEPProtocolSessionPromise.this.pending.equals(cf));
                if (PCEPProtocolSessionPromise.this.isCancelled()) {
                    if (cf.isSuccess()) {
                        PCEPProtocolSessionPromise.LOG.debug("Closing channel for cancelled promise {}",
                                PCEPProtocolSessionPromise.this);
                        cf.channel().close();
                    }
                } else if (cf.isSuccess()) {
                    PCEPProtocolSessionPromise.LOG.debug("Promise {} connection successful",
                            PCEPProtocolSessionPromise.this);
                } else {
                    PCEPProtocolSessionPromise.LOG.debug("Attempt to connect to {} failed", 
                            PCEPProtocolSessionPromise.this.address, cf.cause());

                    if (PCEPProtocolSessionPromise.this.retryTimer == 0) {
                        PCEPProtocolSessionPromise.LOG
                                .debug("Retry timer value is 0. Reconnection will not be attempted");
                        PCEPProtocolSessionPromise.this.setFailure(cf.cause());
                        return;
                    }

                    final EventLoop loop = cf.channel().eventLoop();
                    loop.schedule(() -> {
                        synchronized (PCEPProtocolSessionPromise.this) {
                            PCEPProtocolSessionPromise.LOG.debug("Attempting to connect to {}",
                                    PCEPProtocolSessionPromise.this.address);
                            final Future<Void> reconnectFuture = PCEPProtocolSessionPromise.this.b.connect();
                            reconnectFuture.addListener(BootstrapConnectListener.this);
                            PCEPProtocolSessionPromise.this.pending = reconnectFuture;
                        }
                    }, PCEPProtocolSessionPromise.this.retryTimer, TimeUnit.SECONDS);
                    PCEPProtocolSessionPromise.LOG.debug("Next reconnection attempt in {}s",
                            PCEPProtocolSessionPromise.this.retryTimer);
                }
            }
        }
    }
}
