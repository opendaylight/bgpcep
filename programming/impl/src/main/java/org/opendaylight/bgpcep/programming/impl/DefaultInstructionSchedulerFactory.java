/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.InstructionSchedulerFactory;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = InstructionSchedulerFactory.class)
@Singleton
public final class DefaultInstructionSchedulerFactory implements InstructionSchedulerFactory, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultInstructionSchedulerFactory.class);
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
        .setNameFormat("programming-timer-%d")
        .setDaemon(true)
        .build();

    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Timer timer = new HashedWheelTimer(THREAD_FACTORY);
    private final DataBroker dataProvider;
    private final NotificationPublishService notifs;
    private final RpcProviderService rpcProviderRegistry;
    private final ClusterSingletonServiceProvider cssp;

    @Inject
    @Activate
    public DefaultInstructionSchedulerFactory(@Reference final DataBroker dataProvider,
            @Reference final RpcProviderService rpcProviderRegistry,
            @Reference final NotificationPublishService notifs,
            @Reference final ClusterSingletonServiceProvider cssp) {
        this.dataProvider = requireNonNull(dataProvider);
        this.notifs = requireNonNull(notifs);
        this.rpcProviderRegistry = requireNonNull(rpcProviderRegistry);
        this.cssp = requireNonNull(cssp);
    }

    @Override
    public InstructionScheduler createInstructionScheduler(final String instructionId) {
        LOG.info("Creating Instruction Scheduler {}.", instructionId);
        return new DefaultInstructionScheduler(dataProvider, notifs, exec, rpcProviderRegistry, cssp, timer,
            instructionId);
    }

    @Deactivate
    @PreDestroy
    @Override
    public void close() {
        // FIXME: This can have weird effects: should we keep track of all schedulers and refcount?
        exec.shutdown();
        timer.stop();
    }
}
