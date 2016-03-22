/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.concepts.KeyMapping;

public class TestClientDispatcher {

    private final BGPHandlerFactory hf;
    private final InetSocketAddress defaulAddress;
    private InetSocketAddress localAddress;
    private final BGPDispatcherImpl disp;

    protected TestClientDispatcher(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final MessageRegistry messageRegistry,
            final InetSocketAddress locaAddress) {
        this.disp = new BGPDispatcherImpl(messageRegistry, bossGroup, workerGroup) {
            @Override
            protected Bootstrap createClientBootStrap(final Optional<KeyMapping> keys, final EventLoopGroup workerGroup) {
                final Bootstrap bootstrap = new Bootstrap();
                if (Epoll.isAvailable()) {
                    bootstrap.channel(EpollSocketChannel.class);
                } else {
                    bootstrap.channel(NioSocketChannel.class);
                }
                // Make sure we are doing round-robin processing
                bootstrap.option(ChannelOption.MAX_MESSAGES_PER_READ, 1);
                bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);

                if (bootstrap.group() == null) {
                    bootstrap.group(workerGroup);
                }
                bootstrap.localAddress(locaAddress);
                bootstrap.option(ChannelOption.SO_REUSEADDR, true);
                return bootstrap;
            }
        };
        this.hf = new BGPHandlerFactory(messageRegistry);
        this.localAddress = locaAddress;
        this.defaulAddress = locaAddress;
    }

    public synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress remoteAddress,
            final BGPPeerRegistry listener, final int retryTimer, final Optional<InetSocketAddress> localAddress) {
        setLocalAddress(localAddress);
        return this.disp.createClient(remoteAddress, listener, retryTimer);
    }

    public synchronized Future<Void> createReconnectingClient(final InetSocketAddress address, final BGPPeerRegistry peerRegistry,
            final int retryTimer, final Optional<InetSocketAddress> localAddress) {
        setLocalAddress(localAddress);
        return this.disp.createReconnectingClient(address, peerRegistry, retryTimer, Optional.<KeyMapping>absent());
    }

    private synchronized void setLocalAddress(final Optional<InetSocketAddress> localAddress) {
        if (localAddress.isPresent()) {
            this.localAddress = localAddress.get();
        } else {
            this.localAddress = this.defaulAddress;
        }
    }
}
