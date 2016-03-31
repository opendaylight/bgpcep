/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock.protocol;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCCReconnectPromise extends DefaultPromise<PCEPSession> {

    private static final Logger LOG = LoggerFactory.getLogger(PCCReconnectPromise.class);

    private final InetSocketAddress address;
    private final int retryTimer;
    private final int connectTimeout;
    private final Bootstrap b;

    @GuardedBy("this")
    private Future<?> pending;

    public PCCReconnectPromise(final InetSocketAddress address, final int retryTimer,
            final int connectTimeout, final Bootstrap b) {
        this.address = address;
        this.retryTimer = retryTimer;
        this.connectTimeout = connectTimeout;
        this.b = b;
    }

    public synchronized void connect() {
        try {
            this.b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout);
            this.b.remoteAddress(this.address);
            final ChannelFuture cf = this.b.connect();
            cf.addListener(new BootstrapConnectListener(PCCReconnectPromise.this));
            this.pending = cf;
        } catch (final Exception e) {
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

    @SuppressWarnings("deprecation")
    @Override
    public synchronized Promise<PCEPSession> setSuccess(final PCEPSession result) {
        LOG.debug("Promise {} completed", this);
        return super.setSuccess(result);
    }

    protected boolean isInitialConnectFinished() {
        Preconditions.checkNotNull(this.pending);
        return this.pending.isDone() && this.pending.isSuccess();
    }

    private final class BootstrapConnectListener implements ChannelFutureListener {

        private final Object lock;

        public BootstrapConnectListener(final Object lock) {
            this.lock = lock;
        }

        @Override
        public void operationComplete(final ChannelFuture cf) throws Exception {

            synchronized (this.lock) {
                if (PCCReconnectPromise.this.isCancelled()) {
                    if (cf.isSuccess()) {
                        PCCReconnectPromise.LOG.debug("Closing channels for cancelled promise {}");
                        cf.channel().close();
                    }
                } else if (cf.isSuccess()) {
                    PCCReconnectPromise.LOG.debug("Promise connection is successful.");
                } else {
                    PCCReconnectPromise.LOG.debug("Attempt to connect to {} failed", PCCReconnectPromise.this.address, cf.cause());

                    if (PCCReconnectPromise.this.retryTimer == 0) {
                        PCCReconnectPromise.LOG.debug("Retry timer value is 0. Reconnection will not be attempted");
                        PCCReconnectPromise.this.setFailure(cf.cause());
                        return;
                    }

                    final EventLoop loop = cf.channel().eventLoop();
                    loop.schedule(new Runnable() {
                        @Override
                        public void run() {
                            PCCReconnectPromise.LOG.debug("Attempting to connect to {}", PCCReconnectPromise.this.address);
                            final Future reconnectFuture = PCCReconnectPromise.this.b.connect();
                            reconnectFuture.addListener(PCCReconnectPromise.BootstrapConnectListener.this);
                            PCCReconnectPromise.this.pending = reconnectFuture;
                        }
                    }, PCCReconnectPromise.this.retryTimer, TimeUnit.SECONDS);
                    PCCReconnectPromise.LOG.debug("Next reconnection attempt in {}s", PCCReconnectPromise.this.retryTimer);
                }
            }
        }
    }
}
