/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.protocol;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BGPProtocolSessionPromise<S extends BGPSession> extends DefaultPromise<S> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPProtocolSessionPromise.class);
    private static final int CONNECT_TIMEOUT = 5000;

    private InetSocketAddress address;
    private final int retryTimer;
    private final Bootstrap bootstrap;
    @GuardedBy("this")
    private Future<?> pending;

    public BGPProtocolSessionPromise(InetSocketAddress remoteAddress, int retryTimer, Bootstrap bootstrap) {
        super(GlobalEventExecutor.INSTANCE);
        this.address = Preconditions.checkNotNull(remoteAddress);
        this.retryTimer = retryTimer;
        this.bootstrap = Preconditions.checkNotNull(bootstrap);
    }

    public synchronized void connect() {
        final BGPProtocolSessionPromise lock = this;

        try {
            LOG.debug("Promise {} attempting connect for {}ms", lock, Integer.valueOf(CONNECT_TIMEOUT));
            if (this.address.isUnresolved()) {
                this.address = new InetSocketAddress(this.address.getHostName(), this.address.getPort());
            }

            this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
            this.bootstrap.remoteAddress(this.address);
            final ChannelFuture connectFuture = this.bootstrap.connect();
            connectFuture.addListener(new BGPProtocolSessionPromise.BootstrapConnectListener(lock));
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
        return super.setSuccess(result);
    }

    private class BootstrapConnectListener implements ChannelFutureListener {
        private final Object lock;

        public BootstrapConnectListener(final Object lock) {
            this.lock = lock;
        }

        @Override
        public void operationComplete(final ChannelFuture channelFuture) throws Exception {
            synchronized (this.lock) {
                BGPProtocolSessionPromise.LOG.debug("Promise {} connection resolved", this.lock);
                Preconditions.checkState(BGPProtocolSessionPromise.this.pending.equals(channelFuture));
                if (BGPProtocolSessionPromise.this.isCancelled()) {
                    if (channelFuture.isSuccess()) {
                        BGPProtocolSessionPromise.LOG.debug("Closing channel for cancelled promise {}", this.lock);
                        channelFuture.channel().close();
                    }
                } else if (channelFuture.isSuccess()) {
                    BGPProtocolSessionPromise.LOG.debug("Promise {} connection successful", this.lock);
                } else {
                    BGPProtocolSessionPromise.LOG.debug("Attempt to connect to {} failed", BGPProtocolSessionPromise.this.address, channelFuture.cause());

                    if (BGPProtocolSessionPromise.this.retryTimer == 0) {
                        BGPProtocolSessionPromise.LOG.debug("Retry timer value is 0. Reconnection will not be attempted");
                        BGPProtocolSessionPromise.this.setFailure(channelFuture.cause());
                        return;
                    }

                    final EventLoop loop = channelFuture.channel().eventLoop();
                    loop.schedule(new Runnable() {
                        @Override
                        public void run() {
                            BGPProtocolSessionPromise.LOG.debug("Attempting to connect to {}", BGPProtocolSessionPromise.this.address);
                            final Future reconnectFuture = BGPProtocolSessionPromise.this.bootstrap.connect();
                            reconnectFuture.addListener(BGPProtocolSessionPromise.BootstrapConnectListener.this);
                            BGPProtocolSessionPromise.this.pending = reconnectFuture;
                        }
                    }, BGPProtocolSessionPromise.this.retryTimer, TimeUnit.SECONDS);
                    BGPProtocolSessionPromise.LOG.debug("Next reconnection attempt in {}s", BGPProtocolSessionPromise.this.retryTimer);
                }
            }
        }
    }
}