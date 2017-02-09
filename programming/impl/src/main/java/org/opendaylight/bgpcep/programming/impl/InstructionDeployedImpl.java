/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.util.Timer;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.programming.spi.SchedulerException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionsQueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.ProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.SubmitInstructionInput;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InstructionDeployedImpl implements IntructionDeployer, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(InstructionDeployedImpl.class);

    private final static ServiceGroupIdentifier SGI = ServiceGroupIdentifier.create("rib-test-service-group");
    private final RpcProviderRegistry rpcProviderRegistry;
    private final ListeningExecutorService exec = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private final DataBroker dataProvider;
    private final NotificationPublishService notifs;
    private final Timer timer;
    private final BundleContext bundleContext;
    @GuardedBy("this")
    private final Map<String, ProgrammingServiceImplSingletonService> programmingServices = new HashMap<>();
    private final ClusterSingletonServiceProvider cssp;

    public InstructionDeployedImpl(final DataBroker dataProvider, final RpcProviderRegistry rpcProviderRegistry,
        final NotificationPublishService notifs, final Timer timer, final ClusterSingletonServiceProvider cssp,
        final BundleContext bundleContext) {
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
        this.notifs = Preconditions.checkNotNull(notifs);
        this.timer = Preconditions.checkNotNull(timer);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        this.cssp = Preconditions.checkNotNull(cssp);
        this.bundleContext = Preconditions.checkNotNull(bundleContext);
    }

    @Override
    public synchronized void createInstruction(final String instructionId) {
        if (this.programmingServices.containsKey(instructionId)) {
            return;
        }
        final ProgrammingServiceImplSingletonService programmingInst =
            new ProgrammingServiceImplSingletonService(instructionId);
        this.programmingServices.put(instructionId, programmingInst);
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(InstructionScheduler.class.getName(), instructionId);
        final ServiceRegistration<?> serviceRegistration = this.bundleContext
            .registerService(InstructionScheduler.class.getName(), programmingInst, properties);
        programmingInst.setServiceRegistration(serviceRegistration);
    }

    @Override
    public synchronized void removeInstruction(final String instructionId) {
        final ProgrammingServiceImplSingletonService service = this.programmingServices.remove(instructionId);
        if (service != null) {
            service.close();
        }
    }

    @Override
    public void close() throws Exception {
        this.exec.shutdown();
        this.programmingServices.values().forEach(ProgrammingServiceImplSingletonService::close);
    }

    private final class ProgrammingServiceImplSingletonService implements InstructionScheduler, ClusterSingletonService, AutoCloseable {
        private final String instructionId;
        private RpcRegistration<ProgrammingService> reg;
        private ProgrammingServiceImpl inst;
        private ClusterSingletonServiceRegistration csspReg;
        private ServiceRegistration<?> serviceRegistration;

        private ProgrammingServiceImplSingletonService(final String instructionId) {
            this.instructionId = instructionId;
            this.csspReg = InstructionDeployedImpl.this.cssp.registerClusterSingletonService(this);
        }

        @Override
        public void instantiateServiceInstance() {
            this.inst = new ProgrammingServiceImpl(InstructionDeployedImpl.this.dataProvider,
                InstructionDeployedImpl.this.notifs, InstructionDeployedImpl.this.exec, InstructionDeployedImpl.this.timer,
                new InstructionsQueueKey(this.instructionId));
            this.reg = InstructionDeployedImpl.this.rpcProviderRegistry.addRpcImplementation(ProgrammingService.class,
                this.inst);
        }

        @Override
        public ListenableFuture<Void> closeServiceInstance() {
            try {
                this.reg.close();
            } finally {
                this.inst.close();
            }

            return Futures.immediateFuture(null);
        }

        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return SGI;
        }

        @Override
        public void close() {
            if (this.csspReg != null) {
                try {
                    this.csspReg.close();
                } catch (Exception e) {
                    LOG.debug("Failed to close Instruction Scheduler service");
                }
            }
            if (this.serviceRegistration != null) {
                this.serviceRegistration.unregister();
                this.serviceRegistration = null;
            }
        }

        @Override
        public ListenableFuture<Instruction> scheduleInstruction(final SubmitInstructionInput input)
            throws SchedulerException {
            return this.inst.scheduleInstruction(input);
        }

        void setServiceRegistration(final ServiceRegistration<?> serviceRegistration) {
            this.serviceRegistration = serviceRegistration;
        }
    }
}
