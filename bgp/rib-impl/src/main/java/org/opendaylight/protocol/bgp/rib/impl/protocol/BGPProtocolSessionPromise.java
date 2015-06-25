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

/**
 * Created by cgasparini on 25.6.2015.
 */
public class BGPProtocolSessionPromise<S extends BGPSession> extends DefaultPromise<S> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPProtocolSessionPromise.class);
    private final ReconnectStrategy strategy;
    private InetSocketAddress address;
    private final Bootstrap b;
    @GuardedBy("this")
    private Future<?> pending;

    public BGPProtocolSessionPromise(EventExecutor executor, InetSocketAddress address, ReconnectStrategy strategy, Bootstrap b) {
        super(executor);
        this.strategy = (ReconnectStrategy) Preconditions.checkNotNull(strategy);
        this.address = (InetSocketAddress)Preconditions.checkNotNull(address);
        this.b = (Bootstrap)Preconditions.checkNotNull(b);
    }

    public synchronized void connect() {
        BGPProtocolSessionPromise lock = this;

        try {
            int e = this.strategy.getConnectTimeout();
            LOG.debug("Promise {} attempting connect for {}ms", lock, Integer.valueOf(e));
            if(this.address.isUnresolved()) {
                this.address = new InetSocketAddress(this.address.getHostName(), this.address.getPort());
            }

            this.b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.valueOf(e));
            ChannelFuture connectFuture = this.b.connect(this.address);
            connectFuture.addListener(new BGPProtocolSessionPromise.BootstrapConnectListener(lock));
            this.pending = connectFuture;
        } catch (Exception var4) {
            LOG.info("Failed to connect to {}", this.address, var4);
            this.setFailure(var4);
        }

    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if(super.cancel(mayInterruptIfRunning)) {
            this.pending.cancel(mayInterruptIfRunning);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized Promise<S> setSuccess(S result) {
        LOG.debug("Promise {} completed", this);
        this.strategy.reconnectSuccessful();
        return super.setSuccess(result);
    }

    private class BootstrapConnectListener implements ChannelFutureListener {
        private final Object lock;

        public BootstrapConnectListener(Object lock) {
            this.lock = lock;
        }

        @Override
        public void operationComplete(ChannelFuture cf) throws Exception {
            synchronized(this.lock) {
                BGPProtocolSessionPromise.LOG.debug("Promise {} connection resolved", this.lock);
                Preconditions.checkState(BGPProtocolSessionPromise.this.pending.equals(cf));
                if(BGPProtocolSessionPromise.this.isCancelled()) {
                    if(cf.isSuccess()) {
                        BGPProtocolSessionPromise.LOG.debug("Closing channel for cancelled promise {}", this.lock);
                        cf.channel().close();
                    }

                } else if(cf.isSuccess()) {
                    BGPProtocolSessionPromise.LOG.debug("Promise {} connection successful", this.lock);
                } else {
                    BGPProtocolSessionPromise.LOG.debug("Attempt to connect to {} failed", BGPProtocolSessionPromise.this.address, cf.cause());
                    Future rf = BGPProtocolSessionPromise.this.strategy.scheduleReconnect(cf.cause());
                    rf.addListener(new BGPProtocolSessionPromise.BootstrapConnectListener.ReconnectingStrategyListener());
                    BGPProtocolSessionPromise.this.pending = rf;
                }
            }
        }

        private class ReconnectingStrategyListener implements FutureListener<Void> {
            private ReconnectingStrategyListener() {
            }

            @Override
            public void operationComplete(Future<Void> sf) {
                synchronized(BootstrapConnectListener.this.lock) {
                    Preconditions.checkState(BGPProtocolSessionPromise.this.pending.equals(sf));
                    if(!BGPProtocolSessionPromise.this.isCancelled()) {
                        if(sf.isSuccess()) {
                            BGPProtocolSessionPromise.this.connect();
                        } else {
                            BGPProtocolSessionPromise.this.setFailure(sf.cause());
                        }
                    }

                }
            }
        }
    }
}