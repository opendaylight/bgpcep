/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.util.Timer;
import java.util.concurrent.Executors;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.InstructionSchedulerFactory;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class DefaultInstructionSchedulerFactory implements InstructionSchedulerFactory, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultInstructionSchedulerFactory.class);

    private final DataBroker dataProvider;
    private final NotificationPublishService notifs;
    private final Timer timer;
    private final RpcProviderService rpcProviderRegistry;
    private final ClusterSingletonServiceProvider cssp;
    private final ListeningExecutorService exec = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    @Inject
    public DefaultInstructionSchedulerFactory(
            final DataBroker dataProvider,
            final RpcProviderService rpcProviderRegistry,
            final NotificationPublishService notifs,
            final Timer timer,
            final ClusterSingletonServiceProvider cssp) {
        this.dataProvider = requireNonNull(dataProvider);
        this.notifs = requireNonNull(notifs);
        this.timer = requireNonNull(timer);
        this.rpcProviderRegistry = requireNonNull(rpcProviderRegistry);
        this.cssp = requireNonNull(cssp);
    }

    @Override
    public InstructionScheduler createInstructionScheduler(final String instructionId) {
        LOG.info("Creating Instruction Scheduler {}.", instructionId);
        return new ProgrammingServiceImpl(dataProvider, notifs, exec, rpcProviderRegistry, cssp, timer, instructionId);
    }

    @PreDestroy
    @Override
    public void close() {
        // FIXME: This can have weird effects: should we keep track of all schedulers and refcount?
        exec.shutdown();
    }
}
