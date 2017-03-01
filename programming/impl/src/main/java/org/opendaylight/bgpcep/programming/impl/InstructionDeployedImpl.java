/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.util.Timer;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionsQueueKey;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InstructionDeployedImpl implements IntructionDeployer, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(InstructionDeployedImpl.class);

    private final RpcProviderRegistry rpcProviderRegistry;
    private final ListeningExecutorService exec = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private final DataBroker dataProvider;
    private final NotificationProviderService notifs;
    private final Timer timer;
    private final BundleContext bundleContext;
    @GuardedBy("this")
    private final Map<String, ProgrammingServiceImpl> programmingServices = new HashMap<>();

    public InstructionDeployedImpl(final DataBroker dataProvider, final RpcProviderRegistry rpcProviderRegistry,
        final NotificationProviderService notifs, final Timer timer, final BundleContext bundleContext) {
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
        this.notifs = Preconditions.checkNotNull(notifs);
        this.timer = Preconditions.checkNotNull(timer);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        this.bundleContext = Preconditions.checkNotNull(bundleContext);
    }

    @Override
    public synchronized void createInstruction(final String instructionId) {
        if (this.programmingServices.containsKey(instructionId)) {
            LOG.warn("Instruction Scheduler {} already exist. New instance won't be created", instructionId);
            return;
        }
        LOG.debug("Creating Instruction Scheduler {}.", instructionId);

        final ProgrammingServiceImpl programmingInst =
            new ProgrammingServiceImpl(this.dataProvider, this.notifs, this.exec, this.rpcProviderRegistry,
                this.timer, new InstructionsQueueKey(instructionId));
        this.programmingServices.put(instructionId, programmingInst);
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(InstructionScheduler.class.getName(), instructionId);
        final ServiceRegistration<?> serviceRegistration = this.bundleContext
            .registerService(InstructionScheduler.class.getName(), programmingInst, properties);
        programmingInst.setServiceRegistration(serviceRegistration);
    }

    @Override
    public synchronized void removeInstruction(final String instructionId) {
        final ProgrammingServiceImpl service = this.programmingServices.remove(instructionId);
        if (service != null) {
            LOG.debug("Closing Instruction Scheduler {}.", instructionId);
            service.close();
        }
    }

    @Override
    public synchronized void close() throws Exception {
        this.exec.shutdown();
        this.programmingServices.values().forEach(ProgrammingServiceImpl::close);
    }
}
