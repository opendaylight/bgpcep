/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionValidator;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

/**
 * Implementation of BGPDispatcher.
 */
public final class BGPDispatcherImpl extends AbstractDispatcher<BGPSessionImpl, BGPSessionListener> implements BGPDispatcher, AutoCloseable {
    private final MD5ServerChannelFactory<?> scf;
    private final MD5ChannelFactory<?> cf;
    private final BGPHandlerFactory hf;
    private Optional<KeyMapping> keys;
    private static final String NEGOTIATOR = "negotiator";

    public BGPDispatcherImpl(final MessageRegistry messageRegistry, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this(messageRegistry, bossGroup, workerGroup, null, null);
    }

    public BGPDispatcherImpl(final MessageRegistry messageRegistry, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final MD5ChannelFactory<?> cf, final MD5ServerChannelFactory<?> scf) {
        super(bossGroup, workerGroup);
        this.hf = new BGPHandlerFactory(messageRegistry);
        this.cf = cf;
        this.scf = scf;
    }

    @Override
    public synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress address,
        final AsNumber remoteAs, final BGPPeerRegistry listener, final ReconnectStrategy strategy) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(remoteAs, listener);
        return super.createClient(address, strategy, new PipelineInitializer<BGPSessionImpl>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<BGPSessionImpl> promise) {
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(null, ch, promise));
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getEncoders());
            }
        });
    }

    @Override
    public Future<Void> createReconnectingClient(final InetSocketAddress address,
        final AsNumber remoteAs, final BGPPeerRegistry listener, final ReconnectStrategyFactory connectStrategyFactory,
        final ReconnectStrategyFactory reestablishStrategyFactory) {
        return this.createReconnectingClient(address, remoteAs, listener, connectStrategyFactory, reestablishStrategyFactory,
            Optional.<KeyMapping>absent());
    }

    @Override
    public void close() {
    }

    @Override
    public synchronized Future<Void> createReconnectingClient(final InetSocketAddress address,
        final AsNumber remoteAs, final BGPPeerRegistry peerRegistry, final ReconnectStrategyFactory connectStrategyFactory,
        final ReconnectStrategyFactory reestablishStrategyFactory, final Optional<KeyMapping> keys) {
        final BGPClientSessionNegotiatorFactory snf = new BGPClientSessionNegotiatorFactory(remoteAs, peerRegistry);

        this.keys = keys;
        final Future<Void> ret = super.createReconnectingClient(address, connectStrategyFactory, reestablishStrategyFactory.createReconnectStrategy(), new PipelineInitializer<BGPSessionImpl>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<BGPSessionImpl> promise) {
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(null, ch, promise));
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getEncoders());
            }
        });
        this.keys = Optional.absent();

        return ret;
    }

    @Override
    public ChannelFuture createServer(final BGPPeerRegistry registry, final InetSocketAddress address, final BGPSessionValidator sessionValidator) {
        return this.createServer(registry, address, sessionValidator, Optional.<KeyMapping>absent());
    }

    @Override
    public ChannelFuture createServer(final BGPPeerRegistry registry, final InetSocketAddress address,
                                      final BGPSessionValidator sessionValidator, final Optional<KeyMapping> keys) {
        final BGPServerSessionNegotiatorFactory snf = new BGPServerSessionNegotiatorFactory(sessionValidator, registry);

        this.keys = keys;
        final ChannelFuture ret = super.createServer(address, new PipelineInitializer<BGPSessionImpl>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<BGPSessionImpl> promise) {
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast(NEGOTIATOR, snf.getSessionNegotiator(null, ch, promise));
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getEncoders());
            }
        });
        this.keys = Optional.absent();

        return ret;
    }

    @Override
    protected void customizeBootstrap(final Bootstrap b) {
        if (this.keys.isPresent()) {
            if (this.cf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }
            b.channelFactory(this.cf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys.get());
        }

        // Make sure we are doing round-robin processing
        b.option(ChannelOption.MAX_MESSAGES_PER_READ, 1);
    }

    @Override
    protected void customizeBootstrap(final ServerBootstrap b) {
        if (this.keys.isPresent()) {
            if (this.scf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }
            b.channelFactory(this.scf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys.get());
        }

        // Make sure we are doing round-robin processing
        b.childOption(ChannelOption.MAX_MESSAGES_PER_READ, 1);
    }

}
