/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.netconf.transport.api.UnsupportedConfigurationException;
import org.opendaylight.netconf.transport.spi.NettyTransportSupport;
import org.opendaylight.netconf.transport.spi.TcpMd5Secrets;
import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PCEPDispatcher.
 */
@Singleton
@Component(service = PCEPDispatcher.class)
@MetaInfServices
@Designate(ocd = PCEPDispatcherImpl.Configuration.class)
public class PCEPDispatcherImpl implements PCEPDispatcher, AutoCloseable {
    @ObjectClassDefinition(description = "Configuration of the OSGiBgpDeployer")
    public @interface Configuration {
        @AttributeDefinition(
            description = "Maximum number of threads servicing the socket, 0 means as many as there are process cores",
            min = "0")
        int bossThreads() default 0;

        @AttributeDefinition(
            description = "Maximum number of threads servicing sessions, 0 means as many as there are process cores",
            min = "0")
        int workerThreads() default 0;

        @AttributeDefinition(description = "Maximum time (seconds) to wait for shutdown", min = "0")
        int shutdownTimeSeconds() default DEFAULT_SHUTDOWN_SECONDS;
    }

    private static final Logger LOG = LoggerFactory.getLogger(PCEPDispatcherImpl.class);
    private static final int DEFAULT_SHUTDOWN_SECONDS = 10;

    private static final ThreadFactory BOSS_TF = Thread.ofPlatform().name("pcep-boss-", 0).factory();
    private static final ThreadFactory WORKER_TF = Thread.ofPlatform().name("pcep-boss-", 0).factory();

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventExecutor executor  = requireNonNull(GlobalEventExecutor.INSTANCE);
    private final int shutdownTimeSeconds;

    public PCEPDispatcherImpl() {
        this(0, 0, DEFAULT_SHUTDOWN_SECONDS);
    }

    @Activate
    public PCEPDispatcherImpl(final Configuration config) {
        this(config.bossThreads(), config.workerThreads(), config.shutdownTimeSeconds());
    }

    @Inject
    public PCEPDispatcherImpl(final int bossThreads, final int workerThreads, final int shutdownTimeSeconds) {
        bossGroup = NettyTransportSupport.newEventLoopGroup(bossThreads, BOSS_TF);
        workerGroup = NettyTransportSupport.newEventLoopGroup(workerThreads, WORKER_TF);
        this.shutdownTimeSeconds = shutdownTimeSeconds;
    }

    @Override
    public final synchronized ChannelFuture createServer(final InetSocketAddress listenAddress,
            final TcpMd5Secrets secrets, final MessageRegistry registry,
            final PCEPSessionNegotiatorFactory negotiatorFactory) {
        final var hf = new PCEPHandlerFactory(registry);

        final ChannelPipelineInitializer initializer = (ch, promise) -> {
            ch.pipeline().addLast(hf.getDecoders());
            ch.pipeline().addLast("negotiator", negotiatorFactory.getSessionNegotiator(ch, promise));
            ch.pipeline().addLast(hf.getEncoders());
        };

        final var b = createServerBootstrap(initializer, secrets);
        final var f = b.bind(listenAddress);
        LOG.debug("Initiated server {} at {}.", f, listenAddress);
        return f;
    }

    @VisibleForTesting
    ServerBootstrap createServerBootstrap(final ChannelPipelineInitializer initializer,
            final TcpMd5Secrets secrets) {
        final var b = NettyTransportSupport.newServerBootstrap()
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            // Make sure we are doing round-robin processing
            .childOption(ChannelOption.RECVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1))
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel ch) {
                    initializer.initializeChannel(ch, new DefaultPromise<>(executor));
                }
            });

        if (b.config().group() == null) {
            b.group(bossGroup, workerGroup);
        }

        if (!secrets.isEmpty()) {
            try {
                NettyTransportSupport.setTcpMd5(b, secrets);
            } catch (UnsupportedConfigurationException e) {
                throw new UnsupportedOperationException(e);
            }
        }

        return b;
    }

    @Override
    @Deactivate
    @PreDestroy
    public final void close() {
        final long now = System.nanoTime();
        final long deadline = now + TimeUnit.SECONDS.toNanos(shutdownTimeSeconds);

        try {
            bossGroup.shutdownGracefully(0, deadline - now, TimeUnit.NANOSECONDS);
        } finally {
            workerGroup.shutdownGracefully(0, deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
        }
    }

    protected interface ChannelPipelineInitializer {
        void initializeChannel(SocketChannel socketChannel, Promise<PCEPSession> promise);
    }
}
