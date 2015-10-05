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
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

public class TestClientDispatcher extends AbstractDispatcher<BGPSessionImpl, BGPSessionListener> {

    private static final String NEGOTIATOR = "negotiator";

    private final BGPHandlerFactory hf;
    private InetSocketAddress localAddress;
    private final InetSocketAddress defaulAddress;

    protected TestClientDispatcher(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final MessageRegistry messageRegistry,
            final InetSocketAddress locaAddress) {
        super(bossGroup, workerGroup);
        this.hf = new BGPHandlerFactory(messageRegistry);
        this.localAddress = locaAddress;
        this.defaulAddress = locaAddress;
    }

    public synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress remoteAddress,
            final AsNumber remoteAs, final BGPPeerRegistry listener, final ReconnectStrategy strategy, final Optional<InetSocketAddress> localAddress) {
        setLocalAddress(localAddress);
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(remoteAs, listener);
        return super.createClient(remoteAddress, strategy, new PipelineInitializer<BGPSessionImpl>() {

            @Override
            public void initializeChannel(SocketChannel ch, Promise<BGPSessionImpl> promise) {
                ch.pipeline().addLast(TestClientDispatcher.this.hf.getDecoders());
                ch.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(null, ch, promise));
                ch.pipeline().addLast(TestClientDispatcher.this.hf.getEncoders());
            }
        });
    }

    public synchronized Future<Void> createReconnectingClient(final InetSocketAddress address,
        final AsNumber remoteAs, final BGPPeerRegistry peerRegistry, final ReconnectStrategyFactory reconnectStrategyFactory,
        final Optional<InetSocketAddress> localAddress) {
        setLocalAddress(localAddress);
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(remoteAs, peerRegistry);
        final Future<Void> ret = super.createReconnectingClient(address, reconnectStrategyFactory, new PipelineInitializer<BGPSessionImpl>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<BGPSessionImpl> promise) {
                ch.pipeline().addLast(TestClientDispatcher.this.hf.getDecoders());
                ch.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(null, ch, promise));
                ch.pipeline().addLast(TestClientDispatcher.this.hf.getEncoders());
            }
        });

        return ret;
    }

    @Override
    protected void customizeBootstrap(Bootstrap b) {
        b.localAddress(this.localAddress);
        b.option(ChannelOption.SO_REUSEADDR, true);
        super.customizeBootstrap(b);
    }

    private synchronized void setLocalAddress(final Optional<InetSocketAddress> localAddress) {
        if (localAddress.isPresent()) {
            this.localAddress = localAddress.get();
        } else {
            this.localAddress = defaulAddress;
        }
    }
}
