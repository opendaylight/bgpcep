/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import com.google.common.base.Preconditions;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.tcpmd5.netty.MD5ChannelFactory;
import org.opendaylight.tcpmd5.netty.MD5ServerChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PCEPDispatcher.
 */
public class PCEPDispatcherImpl extends AbstractPCEPDispatcher implements PCEPDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPDispatcherImpl.class);
    private final PCEPSessionNegotiatorFactory snf;
    private final MD5ChannelFactory<?> cf;
    private final PCEPHandlerFactory hf;


    /**
     * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
     *
     * @param registry          a message registry
     * @param negotiatorFactory a negotiation factory
     * @param bossGroup         accepts an incoming connection
     * @param workerGroup       handles the traffic of accepted connection
     */
    public PCEPDispatcherImpl(final MessageRegistry registry,
                              final PCEPSessionNegotiatorFactory negotiatorFactory,
                              final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this(registry, negotiatorFactory, bossGroup, workerGroup, null, null);
    }

    /**
     * Creates an instance of PCEPDispatcherImpl, gets the default selector and opens it.
     *
     * @param registry          a message registry
     * @param negotiatorFactory a negotiation factory
     * @param bossGroup         accepts an incoming connection
     * @param workerGroup       handles the traffic of accepted connection
     * @param cf                MD5ChannelFactory
     * @param scf               MD5ServerChannelFactory
     */
    public PCEPDispatcherImpl(final MessageRegistry registry,
                              final PCEPSessionNegotiatorFactory negotiatorFactory,
                              final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final MD5ChannelFactory<?> cf,
                              final MD5ServerChannelFactory<?> scf) {
        super(bossGroup, workerGroup,scf);
        this.cf = cf;
        this.snf = Preconditions.checkNotNull(negotiatorFactory);
        this.hf = new PCEPHandlerFactory(registry);
    }

    @Override
    public synchronized ChannelFuture createServer(final InetSocketAddress address,
                                                   final PCEPSessionListenerFactory listenerFactory) {
        return createServer(address, null, listenerFactory);
    }

    @Override
    public synchronized ChannelFuture createServer(final InetSocketAddress address, final KeyMapping keys,
                                                   final PCEPSessionListenerFactory listenerFactory) {
        this.keys = keys;
        final ChannelFuture ret = super.createServer(address, new ChannelPipelineInitializer() {
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
