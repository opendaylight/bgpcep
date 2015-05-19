/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;

public class BmpDispatcherImpl extends AbstractDispatcher<BmpSessionImpl, BmpSessionListener> implements AutoCloseable, BmpDispatcher {

    private final MD5ServerChannelFactory<?> scf;
    private final MD5ChannelFactory<?> cf;
    private BmpHandlerFactory hf;
    private KeyMapping keys;

    protected BmpDispatcherImpl(final BmpMessageRegistry messageRegistry, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this(messageRegistry, bossGroup, workerGroup, null, null);
    }

    public BmpDispatcherImpl(final BmpMessageRegistry messageRegistry, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final MD5ChannelFactory<?> cf, final MD5ServerChannelFactory<?> scf) {
        super(bossGroup, workerGroup);
        //TODO
        //this.hf = new BmpHandlerFactory(messageRegistry);
        this.cf = cf;
        this.scf = scf;
    }

    @Override
    public ChannelFuture createServer(final InetSocketAddress address, final SessionListenerFactory<BmpSessionListener> slf) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelFuture createServer(final InetSocketAddress address, final KeyMapping keys, final SessionListenerFactory<BmpSessionListener> slf) {
        this.keys = keys;
        final ChannelFuture ret = super.createServer(address, new PipelineInitializer<BmpSessionImpl>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<BmpSessionImpl> promise) {
                ch.pipeline().addLast(BmpDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast(new BmpSessionImpl(slf.getSessionListener(), ch));
                ch.pipeline().addLast(BmpDispatcherImpl.this.hf.getEncoders());
            }
        });
        this.keys = null;

        return ret;
    }

    @Override
    public Future<Void> createReconnectingClient(final InetSocketAddress address, final ReconnectStrategyFactory connectStrategyFactory,
            final ReconnectStrategyFactory reestablishStrategyFactory) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<Void> createReconnectingClient(final InetSocketAddress address, final ReconnectStrategyFactory connectStrategyFactory,
            final ReconnectStrategyFactory reestablishStrategyFactory, final KeyMapping keys) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() {
        //no-op
    }

}
