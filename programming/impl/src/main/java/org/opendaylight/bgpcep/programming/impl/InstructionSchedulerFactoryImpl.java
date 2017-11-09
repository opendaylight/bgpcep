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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.InstructionSchedulerFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InstructionSchedulerFactoryImpl implements InstructionSchedulerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(InstructionSchedulerFactoryImpl.class);
    private final DataBroker dataProvider;
    private final NotificationPublishService notifs;
    private final Timer timer;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final BundleContext bundleContext;
    private final ClusterSingletonServiceProvider cssp;
    private final ListeningExecutorService exec = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    public InstructionSchedulerFactoryImpl(
            final DataBroker dataProvider,
            final RpcProviderRegistry rpcProviderRegistry,
            final NotificationPublishService notifs,
            final Timer timer,
            final ClusterSingletonServiceProvider cssp,
            final BundleContext bundleContext) {
        this.dataProvider = requireNonNull(dataProvider);
        this.notifs = requireNonNull(notifs);
        this.timer = requireNonNull(timer);
        this.rpcProviderRegistry = requireNonNull(rpcProviderRegistry);
        this.bundleContext = requireNonNull(bundleContext);
        this.cssp = requireNonNull(cssp);
    }

    @Override
    public InstructionScheduler createInstructionScheduler(final String instructionId) {
        LOG.debug("Creating Instruction Scheduler {}.", instructionId);

        final ProgrammingServiceImpl programmingInst = new ProgrammingServiceImpl(this.dataProvider, this.notifs,
                this.exec, this.rpcProviderRegistry, this.cssp, this.timer, instructionId);
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(InstructionScheduler.class.getName(), instructionId);
        final ServiceRegistration<?> serviceRegistration = this.bundleContext
                .registerService(InstructionScheduler.class.getName(), programmingInst, properties);
        programmingInst.setServiceRegistration(serviceRegistration);
        return programmingInst;
    }
}
