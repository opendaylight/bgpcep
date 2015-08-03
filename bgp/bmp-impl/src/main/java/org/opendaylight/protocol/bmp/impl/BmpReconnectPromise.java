/*
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BmpReconnectPromise extends DefaultPromise<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(BmpReconnectPromise.class);

    private final InetSocketAddress address;
    private final Bootstrap b;
    private final ReconnectStrategyFactory rcsf;
    private final ReconnectStrategy rcs;

    public BmpReconnectPromise(final InetSocketAddress address, final ReconnectStrategyFactory rcsf,
                                     final Bootstrap b) {
        this.address = address;
        this.b = b;
        this.rcsf = rcsf;
        this.rcs = rcsf.createReconnectStrategy();

    }
    public synchronized ChannelFuture connect() {
        try {
            /* set bootstrap with configurations in reconnectStrategy */
            int t = this.rcs.getConnectTimeout();
            this.b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, t);
            /* make the very first connection attempt and let bootstrap listener to handle future return */
        } catch (Exception e) {
            LOG.info("Failed to connect to {}", this.address, e);
            this.setFailure(e);
        }
        final ChannelFuture cf = b.connect(this.address);
        cf.addListener(new BootstrapConnectListener(BmpReconnectPromise.this));
        return cf;
    }

    /* bootstrap listener that handles bmp connect promise */
    private final class BootstrapConnectListener implements ChannelFutureListener {
        private final Object lock;

        public BootstrapConnectListener(final Object lock) {
            this.lock = lock;
        }

        @Override
        public void operationComplete(ChannelFuture cf) throws Exception {

            synchronized (this.lock) {
                if (BmpReconnectPromise.this.isCancelled()) {
                    if (cf.isSuccess()) {
                        BmpReconnectPromise.LOG.debug("Closing channels for cancelled promise {}");
                        cf.channel().close();
                    }
                } else if (cf.isSuccess()) {
                    BmpReconnectPromise.LOG.debug("Promise connection is successful.");
                } else {
                    BmpReconnectPromise.LOG.debug("Attempt to reconnect using reconnect strategy ...");
                    final Future rf = BmpReconnectPromise.this.rcs.scheduleReconnect(cf.cause());
                    rf.addListener(new BmpReconnectPromise.BootstrapConnectListener.ReconnectStrategyListener());
                }
            }
        }

        private final class ReconnectStrategyListener implements FutureListener<Void> {

            @Override
            public void operationComplete(final Future<Void> f ) {
                synchronized (BootstrapConnectListener.this.lock) {
                    if (!BmpReconnectPromise.this.isCancelled()) {
                        if (f.isSuccess()) {
                            BmpReconnectPromise.LOG.debug("ReconnectStrategy has scheduled a retry.");
                            BmpReconnectPromise.this.connect();
                        } else {
                            BmpReconnectPromise.LOG.debug("ReconnectStrategy has failed. No attempts will be made.");
                            BmpReconnectPromise.this.setFailure(f.cause());
                        }
                    }
                }
            }
        }
    }

    private static final class ReconnectHandler extends ChannelInboundHandlerAdapter {
        private final BmpReconnectPromise promise;

        public ReconnectHandler(BmpReconnectPromise promise) {
            this.promise = promise;
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            if (promise.isCancelled()) {
                return;
            }
            if (!promise.isSuccess()) {
                LOG.debug("Connection to {} was dropped.", promise.address);
            }
            LOG.debug("Reconnecting to {} ...", promise.address);
            promise.connect();
        }
    }
}
