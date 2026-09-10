/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock.protocol;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCCReconnectPromise extends DefaultPromise<PCEPSession> {
    private static final Logger LOG = LoggerFactory.getLogger(PCCReconnectPromise.class);

    private final InetSocketAddress address;
    private final int retryTimer;
    private final int connectTimeout;
    private final Bootstrap bootstrap;

    @GuardedBy("this")
    private Future<?> pending;

    PCCReconnectPromise(final InetSocketAddress address, final int retryTimer,
                        final int connectTimeout, final Bootstrap bootstrap) {
        super(GlobalEventExecutor.INSTANCE);
        this.address = address;
        this.retryTimer = retryTimer;
        this.connectTimeout = connectTimeout;
        this.bootstrap = bootstrap;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    synchronized void connect() {
        try {
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
            bootstrap.remoteAddress(address);
            final var cf = bootstrap.connect();
            cf.addListener(new BootstrapConnectListener(this));
            pending = cf;
        } catch (final Exception e) {
            LOG.info("Failed to connect to {}", address, e);
            setFailure(e);
        }
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            pending.cancel(mayInterruptIfRunning);
            return true;
        }

        return false;
    }

    @Override
    public synchronized Promise<PCEPSession> setSuccess(final PCEPSession result) {
        final var promise = super.setSuccess(result);
        LOG.debug("Promise {} completed", this);
        return promise;
    }

    synchronized boolean isInitialConnectFinished() {
        requireNonNull(pending);
        return pending.isDone() && pending.isSuccess();
    }

    private final class BootstrapConnectListener implements ChannelFutureListener {
        @GuardedBy("this")
        private final Object lock;

        BootstrapConnectListener(final Object lock) {
            this.lock = lock;
        }

        @Override
        public void operationComplete(final ChannelFuture cf) {
            synchronized (lock) {
                if (isCancelled()) {
                    if (cf.isSuccess()) {
                        LOG.debug("Closing channels for cancelled promise {}", PCCReconnectPromise.this);
                        cf.channel().close();
                    }
                } else if (cf.isSuccess()) {
                    LOG.debug("Promise connection is successful.");
                } else {
                    LOG.debug("Attempt to connect to {} failed", address, cf.cause());

                    if (retryTimer == 0) {
                        LOG.debug("Retry timer value is 0. Reconnection will not be attempted");
                        setFailure(cf.cause());
                        return;
                    }

                    final var loop = cf.channel().eventLoop();
                    loop.schedule(() -> {
                        synchronized (PCCReconnectPromise.this) {
                            LOG.debug("Attempting to connect to {}", address);
                            final var reconnectFuture = bootstrap.connect();
                            reconnectFuture.addListener(this);
                            pending = reconnectFuture;
                        }
                    }, retryTimer, TimeUnit.SECONDS);
                    LOG.debug("Next reconnection attempt in {}s", retryTimer);
                }
            }
        }
    }
}
