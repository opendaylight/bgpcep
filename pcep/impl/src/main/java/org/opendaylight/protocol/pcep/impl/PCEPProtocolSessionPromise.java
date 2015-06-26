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

/**
 * Created by cgasparini on 25.6.2015.
 */
@ThreadSafe
public class PCEPProtocolSessionPromise<S extends PCEPSession> extends DefaultPromise<S> {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPProtocolSessionPromise.class);
    private final ReconnectStrategy strategy;
    private InetSocketAddress address;
    private final Bootstrap b;
    @GuardedBy("this")
    private Future<?> pending;

    PCEPProtocolSessionPromise(EventExecutor executor, InetSocketAddress address, ReconnectStrategy strategy, Bootstrap b) {
        super(executor);
        this.strategy = (ReconnectStrategy) Preconditions.checkNotNull(strategy);
        this.address = (InetSocketAddress)Preconditions.checkNotNull(address);
        this.b = (Bootstrap)Preconditions.checkNotNull(b);
    }

    synchronized void connect() {
        PCEPProtocolSessionPromise lock = this;

        try {
            int e = this.strategy.getConnectTimeout();
            LOG.debug("Promise {} attempting connect for {}ms", lock, Integer.valueOf(e));
            if(this.address.isUnresolved()) {
                this.address = new InetSocketAddress(this.address.getHostName(), this.address.getPort());
            }

            this.b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.valueOf(e));
            ChannelFuture connectFuture = this.b.connect(this.address);
            connectFuture.addListener(new PCEPProtocolSessionPromise.BootstrapConnectListener(lock));
            this.pending = connectFuture;
        } catch (Exception var4) {
            LOG.info("Failed to connect to {}", this.address, var4);
            this.setFailure(var4);
        }

    }

    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if(super.cancel(mayInterruptIfRunning)) {
            this.pending.cancel(mayInterruptIfRunning);
            return true;
        } else {
            return false;
        }
    }

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

        public void operationComplete(ChannelFuture cf) throws Exception {
            Object var2 = this.lock;
            synchronized(this.lock) {
                PCEPProtocolSessionPromise.LOG.debug("Promise {} connection resolved", this.lock);
                Preconditions.checkState(PCEPProtocolSessionPromise.this.pending.equals(cf));
                if(PCEPProtocolSessionPromise.this.isCancelled()) {
                    if(cf.isSuccess()) {
                        PCEPProtocolSessionPromise.LOG.debug("Closing channel for cancelled promise {}", this.lock);
                        cf.channel().close();
                    }

                } else if(cf.isSuccess()) {
                    PCEPProtocolSessionPromise.LOG.debug("Promise {} connection successful", this.lock);
                } else {
                    PCEPProtocolSessionPromise.LOG.debug("Attempt to connect to {} failed", PCEPProtocolSessionPromise.this.address, cf.cause());
                    Future rf = PCEPProtocolSessionPromise.this.strategy.scheduleReconnect(cf.cause());
                    rf.addListener(new PCEPProtocolSessionPromise.BootstrapConnectListener.ReconnectingStrategyListener());
                    PCEPProtocolSessionPromise.this.pending = rf;
                }
            }
        }

        private class ReconnectingStrategyListener implements FutureListener<Void> {
            private ReconnectingStrategyListener() {
            }

            public void operationComplete(Future<Void> sf) {
                synchronized(BootstrapConnectListener.this.lock) {
                    Preconditions.checkState(PCEPProtocolSessionPromise.this.pending.equals(sf));
                    if(!PCEPProtocolSessionPromise.this.isCancelled()) {
                        if(sf.isSuccess()) {
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
