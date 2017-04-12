/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.util.Timer;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.OdlProgramming;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.OdlProgrammingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.odl.programming.OdlProgrammingConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.odl.programming.OdlProgrammingConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.programming.config.rev170301.odl.programming.OdlProgrammingConfigKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InstructionDeployerImpl implements IntructionDeployer,
    ClusteredDataTreeChangeListener<OdlProgramming>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(InstructionDeployerImpl.class);

    private final RpcProviderRegistry rpcProviderRegistry;
    private final ListeningExecutorService exec = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    private final DataBroker dataProvider;
    private final NotificationPublishService notifs;
    private final Timer timer;
    private final BundleContext bundleContext;
    @GuardedBy("this")
    private final Map<String, ProgrammingServiceImpl> programmingServices = new HashMap<>();
    private final ListenerRegistration<InstructionDeployerImpl> registration;
    private final InstanceIdentifier<OdlProgramming> iid;
    private final ClusterSingletonServiceProvider cssp;


    class WriteConfiguration {
        private final String instructionId;

        WriteConfiguration(final String instructionId) {
            this.instructionId = instructionId;
        }

        void create() {
            final OdlProgrammingConfig instruction = new OdlProgrammingConfigBuilder()
                .setInstructionQueueId(this.instructionId).build();
            final WriteTransaction wTx = InstructionDeployerImpl.this.dataProvider.newWriteOnlyTransaction();
            wTx.put(LogicalDatastoreType.CONFIGURATION, InstructionDeployerImpl.this.iid.child(
                OdlProgrammingConfig.class, new OdlProgrammingConfigKey(this.instructionId)), instruction, true);
            final CheckedFuture<Void, TransactionCommitFailedException> future = wTx.submit();
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.debug("Instruction Instance {} initialized successfully.", WriteConfiguration.this.instructionId);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Failed to initialize Instruction Instance {}.", WriteConfiguration.this.instructionId, t);
                }
            });
        }

        void remove() {
            final WriteTransaction wTx = InstructionDeployerImpl.this.dataProvider.newWriteOnlyTransaction();
            wTx.delete(LogicalDatastoreType.CONFIGURATION, InstructionDeployerImpl.this.iid.child(
                OdlProgrammingConfig.class, new OdlProgrammingConfigKey(this.instructionId)));
            final CheckedFuture<Void, TransactionCommitFailedException> future = wTx.submit();
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(final Void result) {
                    LOG.debug("Instruction Instance {} removed successfully.", WriteConfiguration.this.instructionId);
                }

                @Override
                public void onFailure(final Throwable t) {
                    LOG.error("Failed to remove Instruction Instance {}.", WriteConfiguration.this.instructionId, t);
                }
            });
        }
    }

    public InstructionDeployerImpl(final DataBroker dataProvider, final RpcProviderRegistry rpcProviderRegistry,
        final NotificationPublishService notifs, final Timer timer, final ClusterSingletonServiceProvider cssp,
        final BundleContext bundleContext) {
        this.dataProvider = Preconditions.checkNotNull(dataProvider);
        this.notifs = Preconditions.checkNotNull(notifs);
        this.timer = Preconditions.checkNotNull(timer);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        this.bundleContext = Preconditions.checkNotNull(bundleContext);
        this.cssp = Preconditions.checkNotNull(cssp);
        this.iid = InstanceIdentifier.create(OdlProgramming.class);

        final WriteTransaction wTx = dataProvider.newWriteOnlyTransaction();
        wTx.merge(LogicalDatastoreType.CONFIGURATION, this.iid, new OdlProgrammingBuilder()
            .setOdlProgrammingConfig(Collections.emptyList()).build());
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("Instruction Instance {} initialized successfully.", InstructionDeployerImpl.this.iid);
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("Failed to initialize Instruction Instance {}.", InstructionDeployerImpl.this.iid, t);
            }
        });

        this.registration = dataProvider.registerDataTreeChangeListener(
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, this.iid), this);
    }

    @VisibleForTesting
    InstanceIdentifier<OdlProgramming> getInstructionIID(){
        return this.iid;
    }


    private synchronized void createInstruction(final String instructionId,
        final WriteConfiguration writeConfiguration) {
        if (this.programmingServices.containsKey(instructionId)) {
            LOG.warn("Instruction Scheduler {} already exist. New instance won't be created", instructionId);
            return;
        }
        LOG.debug("Creating Instruction Scheduler {}.", instructionId);

        final ProgrammingServiceImpl programmingInst =
            new ProgrammingServiceImpl(this.dataProvider, this.notifs, this.exec, this.rpcProviderRegistry, this.cssp,
                this.timer, instructionId, writeConfiguration);
        this.programmingServices.put(instructionId, programmingInst);
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(InstructionScheduler.class.getName(), instructionId);
        final ServiceRegistration<?> serviceRegistration = this.bundleContext
            .registerService(InstructionScheduler.class.getName(), programmingInst, properties);
        programmingInst.setServiceRegistration(serviceRegistration);
    }

    private synchronized void removeInstruction(final String instructionId) {
        final ProgrammingServiceImpl service = this.programmingServices.remove(instructionId);
        if (service != null) {
            LOG.debug("Closing Instruction Scheduler {}.", instructionId);
            service.close();
        }
    }

    @Override
    public void writeConfiguration(final String instructionId) {
        createInstruction(instructionId, new WriteConfiguration(instructionId));
    }

    @Override
    public void removeConfiguration(final String instructionId) {
        removeInstruction(instructionId);
    }

    @Override
    public synchronized void close() throws Exception {
        this.registration.close();
        this.exec.shutdown();
        this.programmingServices.values().forEach(ProgrammingServiceImpl::close);
    }

    @Override
    public synchronized void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<OdlProgramming>> changes) {
        final DataTreeModification<OdlProgramming> dataTreeModification = Iterables.getOnlyElement(changes);
        final Collection<DataObjectModification<? extends DataObject>> rootNode = dataTreeModification.getRootNode()
            .getModifiedChildren();
        if(rootNode.isEmpty()) {
            return;
        }
        rootNode.forEach(dto->handleModification((DataObjectModification<OdlProgrammingConfig>) dto));
    }

    private void handleModification(final DataObjectModification<OdlProgrammingConfig> config) {
        final ModificationType modificationType = config.getModificationType();
        LOG.trace("Programming configuration has changed: {}, type modification {}", config, modificationType);
        switch (modificationType) {
            case DELETE:
                removeInstruction(config.getDataBefore().getInstructionQueueId());
                break;
            case SUBTREE_MODIFIED:
            case WRITE:
                createInstruction(config.getDataAfter().getInstructionQueueId(), null);
                break;
            default:
                break;
        }
    }
}
