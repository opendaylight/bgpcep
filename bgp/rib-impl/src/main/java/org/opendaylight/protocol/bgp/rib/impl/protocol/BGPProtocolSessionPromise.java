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
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BGPProtocolSessionPromise<S extends BGPSession> extends DefaultPromise<S> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPProtocolSessionPromise.class);
    private final ReconnectStrategy strategy;
    private final Bootstrap bootstrap;

    private InetSocketAddress address;
    @GuardedBy("this")
    private Future<?> pending;

    public BGPProtocolSessionPromise(EventExecutor executor, InetSocketAddress address, ReconnectStrategy strategy, Bootstrap bootstrap) {
        super(executor);
        this.strategy = Preconditions.checkNotNull(strategy);
        this.address = Preconditions.checkNotNull(address);
        this.bootstrap = Preconditions.checkNotNull(bootstrap);
    }

    public synchronized void connect() {
        final BGPProtocolSessionPromise lock = this;

        try {
            int timeout = this.strategy.getConnectTimeout();
            LOG.debug("Promise {} attempting connect for {}ms", lock, Integer.valueOf(timeout));
            if (this.address.isUnresolved()) {
                this.address = new InetSocketAddress(this.address.getHostName(), this.address.getPort());
            }

            this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
            final ChannelFuture connectFuture = this.bootstrap.connect(this.address);
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
        this.strategy.reconnectSuccessful();
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
                    final Future reconnectFuture = BGPProtocolSessionPromise.this.strategy.scheduleReconnect(channelFuture.cause());
                    reconnectFuture.addListener(new BGPProtocolSessionPromise.BootstrapConnectListener.ReconnectingStrategyListener());
                    BGPProtocolSessionPromise.this.pending = reconnectFuture;
                }
            }
        }

        private final class ReconnectingStrategyListener implements FutureListener<Void> {
            private ReconnectingStrategyListener() {
            }

            @Override
            public void operationComplete(final Future<Void> sessionFuture) {
                synchronized (BootstrapConnectListener.this.lock) {
                    Preconditions.checkState(BGPProtocolSessionPromise.this.pending.equals(sessionFuture));
                    if (!BGPProtocolSessionPromise.this.isCancelled()) {
                        if (sessionFuture.isSuccess()) {
                            BGPProtocolSessionPromise.this.connect();
                        } else {
                            BGPProtocolSessionPromise.this.setFailure(sessionFuture.cause());
                        }
                    }

                }
            }
        }
    }
}