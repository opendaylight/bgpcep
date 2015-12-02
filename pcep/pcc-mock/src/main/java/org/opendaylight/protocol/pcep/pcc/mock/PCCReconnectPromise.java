/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCCReconnectPromise extends DefaultPromise<PCEPSession> {

    private static final Logger LOG = LoggerFactory.getLogger(PCCReconnectPromise.class);

    private final InetSocketAddress address;
    private final Bootstrap b;
    private final ReconnectStrategy reconnectStrategy;

    @GuardedBy("this")
    private Future<?> pending;

    public PCCReconnectPromise(final InetSocketAddress address, final ReconnectStrategyFactory rsf, final Bootstrap b) {
        this.address = address;
        this.b = b;
        this.reconnectStrategy = rsf.createReconnectStrategy();
    }

    public synchronized void connect() {
        try {
            this.b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.reconnectStrategy.getConnectTimeout());
            final ChannelFuture cf = this.b.connect(this.address);
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
        this.reconnectStrategy.reconnectSuccessful();
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
                    PCCReconnectPromise.LOG.debug("Attempt to reconnect using reconnect strategy ...");
                    final Future<Void> rf = PCCReconnectPromise.this.reconnectStrategy.scheduleReconnect(cf.cause());
                    rf.addListener(new PCCReconnectPromise.BootstrapConnectListener.ReconnectStrategyListener());
                }
            }
        }

        private final class ReconnectStrategyListener implements FutureListener<Void> {

            @Override
            public void operationComplete(final Future<Void> f ) {
                synchronized (BootstrapConnectListener.this.lock) {
                    if (!PCCReconnectPromise.this.isCancelled()) {
                        if (f.isSuccess()) {
                            PCCReconnectPromise.LOG.debug("ReconnectStrategy has scheduled a retry.");
                            PCCReconnectPromise.this.connect();
                        } else {
                            PCCReconnectPromise.LOG.debug("ReconnectStrategy has failed. No attempts will be made.");
                            PCCReconnectPromise.this.setFailure(f.cause());
                        }
                    }
                }
            }
        }
    }
}
