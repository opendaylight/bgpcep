/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.protocol;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistrySessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BGPProtocolSessionPromise<S extends BGPSession> extends DefaultPromise<S> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPProtocolSessionPromise.class);
    private static final int CONNECT_TIMEOUT = 5000;
    private final int retryTimer;
    private final Bootstrap bootstrap;
    @GuardedBy("this")
    private final AutoCloseable listenerRegistration;
    @GuardedBy("this")
    private InetSocketAddress address;
    @GuardedBy("this")
    private ChannelFuture pending;
    @GuardedBy("this")
    private boolean peerSessionPresent;
    @GuardedBy("this")
    private boolean connectSkipped;


    public BGPProtocolSessionPromise(@Nonnull final InetSocketAddress remoteAddress, final int retryTimer,
            @Nonnull final Bootstrap bootstrap, @Nonnull final BGPPeerRegistry peerRegistry) {
        super(GlobalEventExecutor.INSTANCE);
        this.address = requireNonNull(remoteAddress);
        this.retryTimer = retryTimer;
        this.bootstrap = requireNonNull(bootstrap);
        this.listenerRegistration = requireNonNull(peerRegistry).registerPeerSessionListener(
                new PeerRegistrySessionListenerImpl(StrictBGPPeerRegistry.getIpAddress(this.address)));
    }

    public synchronized void connect() {
        if (this.peerSessionPresent) {
            LOG.debug("Connection to {} already exists", this.address);
            this.connectSkipped = true;
            return;
        }

        this.connectSkipped = false;

        final BGPProtocolSessionPromise<?> lock = this;
        try {
            LOG.debug("Promise {} attempting connect for {}ms", lock, CONNECT_TIMEOUT);
            if (this.address.isUnresolved()) {
                this.address = new InetSocketAddress(this.address.getHostName(), this.address.getPort());
            }

            this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT);
            this.bootstrap.remoteAddress(this.address);
            final ChannelFuture connectFuture = this.bootstrap.connect();
            connectFuture.addListener(new BootstrapConnectListener());
            this.pending = connectFuture;
        } catch (final Exception e) {
            LOG.warn("Failed to connect to {}", this.address, e);
            this.setFailure(e);
        }
    }

    synchronized void reconnect() {
        if (this.retryTimer == 0) {
            LOG.debug("Retry timer value is 0. Reconnection will not be attempted");
            this.setFailure(this.pending.cause());
            return;
        }

        final EventLoop loop = this.pending.channel().eventLoop();
        loop.schedule(() -> {
            synchronized (BGPProtocolSessionPromise.this) {
                if (BGPProtocolSessionPromise.this.peerSessionPresent) {
                    LOG.debug("Connection to {} already exists", BGPProtocolSessionPromise.this.address);
                    BGPProtocolSessionPromise.this.connectSkipped = true;
                    return;
                }

                BGPProtocolSessionPromise.this.connectSkipped = false;
                LOG.debug("Attempting to connect to {}", BGPProtocolSessionPromise.this.address);
                final ChannelFuture reconnectFuture = BGPProtocolSessionPromise.this.bootstrap.connect();
                reconnectFuture.addListener(new BootstrapConnectListener());
                BGPProtocolSessionPromise.this.pending = reconnectFuture;
            }
        }, this.retryTimer, TimeUnit.SECONDS);
        LOG.debug("Next reconnection attempt in {}s", this.retryTimer);
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        closePeerSessionListener();
        if (super.cancel(mayInterruptIfRunning)) {
            requireNonNull(this.pending);
            this.pending.cancel(mayInterruptIfRunning);
            return true;
        }

        return false;
    }

    private synchronized void closePeerSessionListener() {
        try {
            this.listenerRegistration.close();
        } catch (final Exception e) {
            LOG.debug("Exception encountered while closing peer registry session listener registration", e);
        }
    }

    @Override
    public synchronized Promise<S> setSuccess(final S result) {
        LOG.debug("Promise {} completed", this);
        return super.setSuccess(result);
    }

    private class BootstrapConnectListener implements ChannelFutureListener {
        @Override
        public void operationComplete(final ChannelFuture channelFuture) throws Exception {
            synchronized (BGPProtocolSessionPromise.this) {
                BGPProtocolSessionPromise.LOG.debug("Promise {} connection resolved", BGPProtocolSessionPromise.this);
                Preconditions.checkState(BGPProtocolSessionPromise.this.pending.equals(channelFuture));
                if (BGPProtocolSessionPromise.this.isCancelled()) {
                    if (channelFuture.isSuccess()) {
                        BGPProtocolSessionPromise.LOG.debug("Closing channel for cancelled promise {}",
                                BGPProtocolSessionPromise.this);
                        channelFuture.channel().close();
                    }
                } else if (channelFuture.isSuccess()) {
                    BGPProtocolSessionPromise.LOG.debug("Promise {} connection successful",
                            BGPProtocolSessionPromise.this);
                } else {
                    BGPProtocolSessionPromise.LOG.warn("Attempt to connect to {} failed",
                            BGPProtocolSessionPromise.this.address, channelFuture.cause());
                    BGPProtocolSessionPromise.this.reconnect();
                }
            }
        }
    }

    private class PeerRegistrySessionListenerImpl implements PeerRegistrySessionListener {
        private final IpAddress peerAddress;

        PeerRegistrySessionListenerImpl(final IpAddress peerAddress) {
            this.peerAddress = peerAddress;
        }

        @Override
        public void onSessionCreated(@Nonnull final IpAddress ip) {
            if (!ip.equals(this.peerAddress)) {
                return;
            }
            BGPProtocolSessionPromise.LOG.debug("Callback for session creation with peer {} received", ip);
            synchronized (BGPProtocolSessionPromise.this) {
                BGPProtocolSessionPromise.this.peerSessionPresent = true;
            }
        }

        @Override
        public void onSessionRemoved(@Nonnull final IpAddress ip) {
            if (!ip.equals(this.peerAddress)) {
                return;
            }
            BGPProtocolSessionPromise.LOG.debug("Callback for session removal with peer {} received", ip);
            synchronized (BGPProtocolSessionPromise.this) {
                BGPProtocolSessionPromise.this.peerSessionPresent = false;
                if (BGPProtocolSessionPromise.this.connectSkipped) {
                    BGPProtocolSessionPromise.this.connect();
                }
            }
        }
    }

}
