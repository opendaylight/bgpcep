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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.MessageRegistry;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactory;
import org.opendaylight.protocol.pcep.PCEPSessionNegotiatorFactoryDependencies;
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
        final var bossTf = new ThreadFactoryBuilder().setNameFormat("pcep-boss-%d").build();
        final var workerTf = new ThreadFactoryBuilder().setNameFormat("pcep-worker-%d").build();

        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(bossThreads, bossTf);
            workerGroup = new EpollEventLoopGroup(workerThreads, workerTf);
        } else {
            bossGroup = new NioEventLoopGroup(bossThreads, bossTf);
            workerGroup = new NioEventLoopGroup(workerThreads, workerTf);
        }
        this.shutdownTimeSeconds = shutdownTimeSeconds;
    }

    @Override
    public final synchronized ChannelFuture createServer(final InetSocketAddress listenAddress,
            final KeyMapping tcpKeys, final MessageRegistry registry,
            final PCEPSessionNegotiatorFactory negotiatorFactory,
            final PCEPSessionNegotiatorFactoryDependencies negotiatorDependencies) {
        final var hf = new PCEPHandlerFactory(registry);

        final ChannelPipelineInitializer initializer = (ch, promise) -> {
            ch.pipeline().addLast(hf.getDecoders());
            ch.pipeline().addLast("negotiator",
                negotiatorFactory.getSessionNegotiator(negotiatorDependencies, ch, promise));
            ch.pipeline().addLast(hf.getEncoders());
        };

        final ServerBootstrap b = createServerBootstrap(initializer, tcpKeys);
        final ChannelFuture f = b.bind(listenAddress);
        LOG.debug("Initiated server {} at {}.", f, listenAddress);

        return f;
    }

    @VisibleForTesting
    ServerBootstrap createServerBootstrap(final ChannelPipelineInitializer initializer,
            final KeyMapping tcpKeys) {
        final ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                initializer.initializeChannel(ch, new DefaultPromise<>(executor));
            }
        });
        b.option(ChannelOption.SO_BACKLOG, 128);

        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        if (Epoll.isAvailable()) {
            b.channel(EpollServerSocketChannel.class);
            b.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.LEVEL_TRIGGERED);
        } else {
            b.channel(NioServerSocketChannel.class);
        }
        if (!tcpKeys.isEmpty()) {
            if (Epoll.isAvailable()) {
                b.option(EpollChannelOption.TCP_MD5SIG, tcpKeys.asMap());
            } else {
                throw new UnsupportedOperationException("Setting TCP-MD5 signatures is not supported",
                        Epoll.unavailabilityCause().getCause());
            }
        }

        // Make sure we are doing round-robin processing
        b.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(1));

        if (b.config().group() == null) {
            b.group(bossGroup, workerGroup);
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
