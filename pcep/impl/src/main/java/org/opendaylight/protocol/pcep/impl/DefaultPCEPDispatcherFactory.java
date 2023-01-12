/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.kohsuke.MetaInfServices;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.protocol.pcep.PCEPDispatcherFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Singleton
@Component
@MetaInfServices
@Designate(ocd = DefaultPCEPDispatcherFactory.Configuration.class)
public final class DefaultPCEPDispatcherFactory implements PCEPDispatcherFactory {
    private static final int DEFAULT_SHUTDOWN_SECONDS = 10;

    @ObjectClassDefinition(description = "Configuration of the OSGiBgpDeployer")
    public @interface Configuration {
        @AttributeDefinition(
            description = "Maximum number of threads servicing the socket, 0 means as many as there are process cores")
        int bossThreads() default 0;

        @AttributeDefinition(
            description = "Maximum number of threads servicing sessions, 0 means as many as there are process cores")
        int workerThreads() default 0;

        @AttributeDefinition(description = "Maximum time (seconds) to wait for shutdown")
        int shutdownTimeSeconds() default DEFAULT_SHUTDOWN_SECONDS;
    }

    private final @NonNull EventLoopGroup bossGroup;
    private final @NonNull EventLoopGroup workerGroup;
    private final int shutdownTimeSeconds;

    public DefaultPCEPDispatcherFactory() {
        this(0, 0, DEFAULT_SHUTDOWN_SECONDS);
    }

    @Activate
    public DefaultPCEPDispatcherFactory(final Configuration config) {
        this(config.bossThreads(), config.workerThreads(), config.shutdownTimeSeconds());
    }

    @Inject
    public DefaultPCEPDispatcherFactory(final int bossThreads, final int workerThreads, final int shutdownTimeSeconds) {
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(bossThreads);
            workerGroup = new EpollEventLoopGroup(workerThreads);
        } else {
            bossGroup = new NioEventLoopGroup(bossThreads);
            workerGroup = new NioEventLoopGroup(workerThreads);
        }
        this.shutdownTimeSeconds = shutdownTimeSeconds;
    }

    @Override
    public PCEPDispatcher newDispatcher() {
        return new PCEPDispatcherImpl(null, null, bossGroup, workerGroup);
    }

    @Deactivate
    @PreDestroy
    public void close() {
        final long now = System.nanoTime();
        final long deadline = now + TimeUnit.SECONDS.toNanos(shutdownTimeSeconds);

        try {
            bossGroup.shutdownGracefully(0, deadline - now, TimeUnit.NANOSECONDS);
        } finally {
            workerGroup.shutdownGracefully(0, deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
        }
    }
}
