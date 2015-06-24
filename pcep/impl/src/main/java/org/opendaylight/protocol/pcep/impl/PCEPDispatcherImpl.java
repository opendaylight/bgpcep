/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5ChannelOption;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PCEPDispatcher.
 */
public class PCEPDispatcherImpl extends PCEPAbstractDispatcher implements PCEPDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPDispatcherImpl.class);
    private final PCEPSessionNegotiatorFactory snf;
    private final MD5ServerChannelFactory<?> scf;
    private final MD5ChannelFactory<?> cf;
    private final PCEPHandlerFactory hf;
    private KeyMapping keys;

    /**
     * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
     *
     * @param registry a message registry
     * @param negotiatorFactory a negotiation factory
     * @param bossGroup accepts an incoming connection
     * @param workerGroup handles the traffic of accepted connection
     */
    public PCEPDispatcherImpl(final MessageRegistry registry,
        final PCEPSessionNegotiatorFactory negotiatorFactory,
        final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this(registry, negotiatorFactory, bossGroup, workerGroup, null, null);
    }

    /**
     * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
     *
     * @param registry a message registry
     * @param negotiatorFactory a negotiation factory
     * @param bossGroup accepts an incoming connection
     * @param workerGroup handles the traffic of accepted connection
     * @param cf MD5ChannelFactory
     * @param scf MD5ServerChannelFactory
     */
    public PCEPDispatcherImpl(final MessageRegistry registry,
        final PCEPSessionNegotiatorFactory negotiatorFactory,
        final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final MD5ChannelFactory<?> cf,
        final MD5ServerChannelFactory<?> scf) {
        super(bossGroup, workerGroup);
        this.cf = cf;
        this.scf = scf;
        this.snf = Preconditions.checkNotNull(negotiatorFactory);
        this.hf = new PCEPHandlerFactory(registry);
    }

    @Override
    public synchronized ChannelFuture createServer(final InetSocketAddress address,
        final PCEPSessionListenerFactory listenerFactory) {
        return createServer(address, null, listenerFactory);
    }

    @Override
    public void close() {
    }

    protected void customizeBootstrap(final Bootstrap b) {
        if (this.keys != null && !this.keys.isEmpty()) {
            if (this.cf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }

            LOG.debug("Adding MD5 keys {} to boostrap {}", this.keys, b);
            b.channelFactory(this.cf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys);
        }
    }

    @Override
    protected void customizeBootstrap(final ServerBootstrap b) {
        if (this.keys != null && !this.keys.isEmpty()) {
            if (this.scf == null) {
                throw new UnsupportedOperationException("No key access instance available, cannot use key mapping");
            }

            LOG.debug("Adding MD5 keys {} to boostrap {}", this.keys, b);
            b.channelFactory(this.scf);
            b.option(MD5ChannelOption.TCP_MD5SIG, this.keys);
        }

        // Make sure we are doing round-robin processing
        b.childOption(ChannelOption.MAX_MESSAGES_PER_READ, 1);
    }

    @Override
    public synchronized ChannelFuture createServer(final InetSocketAddress address, final KeyMapping keys,
        final PCEPSessionListenerFactory listenerFactory) {
        this.keys = keys;
        final ChannelFuture ret = super.createServer(address, new PipelineInitializer() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<PCEPSessionImpl> promise) {
                ch.pipeline().addLast(PCEPDispatcherImpl.this.hf.getDecoders());
                ch.pipeline().addLast("negotiator", PCEPDispatcherImpl.this.snf.getSessionNegotiator(listenerFactory, ch, promise));
                ch.pipeline().addLast(PCEPDispatcherImpl.this.hf.getEncoders());
            }
        });

        this.keys = null;
        return ret;
    }
}
