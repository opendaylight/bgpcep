/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import com.google.common.util.concurrent.ListeningExecutorService;
import io.netty.util.Timer;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.InstructionSchedulerFactory;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
// FIXME: unify with DefaultInstructionSchedulerFactory once we have constructor injection
public final class OSGiInstructionSchedulerFactory implements InstructionSchedulerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(OSGiInstructionSchedulerFactory.class);

    @Reference
    DataBroker dataBroker;
    @Reference
    NotificationPublishService notifs;
    @Reference
    ListeningExecutorService executor;
    @Reference
    RpcProviderService rpcProviderRegistry;
    @Reference
    ClusterSingletonServiceProvider cssp;
    @Reference(target = "type=global-timer")
    Timer timer;

    private DefaultInstructionSchedulerFactory delegate;

    @Override
    public InstructionScheduler createInstructionScheduler(final String instructionId) {
        return delegate.createInstructionScheduler(instructionId);
    }

    @Activate
    void activate() {
        delegate = new DefaultInstructionSchedulerFactory(dataBroker, rpcProviderRegistry, notifs, timer, cssp);
        LOG.info("Instruction Scheduler support activated");
    }

    @Deactivate
    void deactivate() {
        delegate.close();
        delegate = null;
        LOG.info("Instruction Scheduler support deactivated");
    }
}
