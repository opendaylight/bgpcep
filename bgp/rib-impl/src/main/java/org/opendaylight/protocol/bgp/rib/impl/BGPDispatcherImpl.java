/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Preconditions;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import org.opendaylight.bgpcep.tcpmd5.KeyMapping;
import org.opendaylight.bgpcep.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.bgpcep.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.bgpcep.tcpmd5.netty.MD5ServerChannelFactory;
import org.opendaylight.protocol.bgp.parser.BGPSessionListener;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;

/**
 * Implementation of BGPDispatcher.
 */
public final class BGPDispatcherImpl extends AbstractDispatcher<BGPSessionImpl, BGPSessionListener> implements BGPDispatcher, AutoCloseable {
    private final MD5ServerChannelFactory<?> scf;
    private final MD5ChannelFactory<?> cf;
    private final BGPHandlerFactory hf;
    private final Timer timer;
    private KeyMapping keys;

    public BGPDispatcherImpl(final MessageRegistry messageRegistry, final Timer timer, final EventLoopGroup bossGroup,
            final EventLoopGroup workerGroup) {
        this(messageRegistry, timer, bossGroup, workerGroup, null, null);
    }

    public BGPDispatcherImpl(final MessageRegistry messageRegistry, final Timer timer, final EventLoopGroup bossGroup,
            final EventLoopGroup workerGroup, final MD5ChannelFactory<?> cf, final MD5ServerChannelFactory<?> scf) {
        super(bossGroup, workerGroup);
        this.timer = Preconditions.checkNotNull(timer);
        this.hf = new BGPHandlerFactory(messageRegistry);
        this.cf = cf;
        this.scf = scf;
    }

    @Override
    public synchronized Future<BGPSessionImpl> createClient(final InetSocketAddress address, final BGPSessionPreferences preferences,
            final AsNumber remoteAs, final BGPSessionListener listener, final ReconnectStrategy strategy) {
        final BGPSessionNegotiatorFactory snf = new BGPSessionNegotiatorFactory(this.timer, preferences, remoteAs);
        final SessionListenerFactory<BGPSessionListener> slf = new SessionListenerFactory<BGPSessionListener>() {
            @Override
            public BGPSessionListener getSessionListener() {
                return listener;
            }
        };
        return super.createClient(address, strategy, new PipelineInitializer<BGPSessionImpl>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<BGPSessionImpl> promise) {
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast("negotiator", snf.getSessionNegotiator(slf, ch, promise));
                ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getEncoders());
            }
        });
    }

    @Override
    public Future<Void> createReconnectingClient(final InetSocketAddress address, final BGPSessionPreferences preferences,
            final AsNumber remoteAs, final BGPSessionListener listener, final ReconnectStrategyFactory connectStrategyFactory,
            final ReconnectStrategyFactory reestablishStrategyFactory) {
        return this.createReconnectingClient(address, preferences, remoteAs, listener, connectStrategyFactory, reestablishStrategyFactory,
                null);
    }

    @Override
    public void close() {
    }

    @Override
    public synchronized Future<Void> createReconnectingClient(final InetSocketAddress address, final BGPSessionPreferences preferences,
            final AsNumber remoteAs, final BGPSessionListener listener, final ReconnectStrategyFactory connectStrategyFactory,
            final ReconnectStrategyFactory reestablishStrategyFactory, final KeyMapping keys) {
        final BGPSessionNegotiatorFactory snf = new BGPSessionNegotiatorFactory(this.timer, preferences, remoteAs);
        final SessionListenerFactory<BGPSessionListener> slf = new SessionListenerFactory<BGPSessionListener>() {
            @Override
            public BGPSessionListener getSessionListener() {
                return listener;
            }
        };

        this.keys = keys;
        final Future<Void> ret = super.createReconnectingClient(address, connectStrategyFactory,
                reestablishStrategyFactory.createReconnectStrategy(), new PipelineInitializer<BGPSessionImpl>() {
                    @Override
                    public void initializeChannel(final SocketChannel ch, final Promise<BGPSessionImpl> promise) {
                        ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getDecoders());
                        ch.pipeline().addLast("negotiator", snf.getSessionNegotiator(slf, ch, promise));
                        ch.pipeline().addLast(BGPDispatcherImpl.this.hf.getEncoders());
                    }
                });
        this.keys = null;

        return ret;
    }

    @Override
    protected void customizeBootstrap(final Bootstrap b) {
        if (keys != null && !keys.isEmpty()) {
            if (cf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }
            b.channelFactory(cf);
            b.option(MD5ChannelOption.TCP_MD5SIG, keys);
        }
    }

    @Override
    protected void customizeBootstrap(final ServerBootstrap b) {
        if (keys != null && !keys.isEmpty()) {
            if (scf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }
            b.channelFactory(scf);
            b.option(MD5ChannelOption.TCP_MD5SIG, keys);
        }
    }

}
