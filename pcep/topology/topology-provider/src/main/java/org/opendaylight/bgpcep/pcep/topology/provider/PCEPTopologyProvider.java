/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCEPTopologyProvider extends DefaultTopologyReference {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProvider.class);

    private final KeyedInstanceIdentifier<Topology, TopologyKey> instanceIdentifier;
    private final PCEPTopologyProviderDependencies dependencies;
    private final InstructionScheduler scheduler;

    // High-level state bits: currently running asynchronous operation, current configuration and the next configuration
    // to apply after the async operation completes
    @GuardedBy("this")
    private ListenableFuture<?> asyncOperation;
    @GuardedBy("this")
    private PCEPTopologyConfiguration currentConfig;
    @GuardedBy("this")
    private Optional<PCEPTopologyConfiguration> nextConfig;







    private ServerSessionManager manager;
    private PCEPTopologyConfiguration configuration;
    private Registration networkReg;
    private Registration elementReg;
    private Channel channel;

    PCEPTopologyProvider(final KeyedInstanceIdentifier<Topology, TopologyKey> instanceIdentifier,
            final PCEPTopologyProviderDependencies dependencies, final InstructionScheduler scheduler) {
        super(instanceIdentifier);
        this.instanceIdentifier = requireNonNull(instanceIdentifier);
        this.dependencies = requireNonNull(dependencies);
        this.scheduler = requireNonNull(scheduler);
    }

    synchronized void updateConfiguration(final @Nullable PCEPTopologyConfiguration newConfiguration) {
        if (newConfiguration == null) {
            if (configuration != null) {
                LOG.info("No valid configuration, disabling PCEP Topology Provider configuration");
                disable();
            }
            return;
        }

        // FIXME: BGPCEP-960: this check should be a one-time thing in PCEPTopologyTracker startup once we have OSGi DS
        if (dependencies.getPCEPDispatcher().getPCEPSessionNegotiatorFactory().getPCEPSessionProposalFactory()
            .getCapabilities().stream().noneMatch(PCEPCapability::isStateful)) {
            if (configuration != null) {
                LOG.info("Dispatcher does not have stateful capability, disabling PCEP Topology Provider");
                disable();
            }
            return;
        }

        if (configuration == null) {
            enable(newConfiguration);
            return;
        }

        // FIXME: compare old and new configuration, propagate as needed
    }

    @Holding("this")
    private void applyConfiguration(final @Nullable PCEPTopologyConfiguration newConfiguration) {
        if (asyncOperation != null) {
            LOG.debug("Topology Provider {} is undergoing reconfiguration, delaying reconfiguration", topologyId());
            nextConfig = Optional.ofNullable(newConfiguration);
            return;
        }

        // Perform obvious enable/disable operations
        if (newConfiguration == null) {
            if (currentConfig != null) {
                disable();
            }
            return;
        }
        if (currentConfig == null) {
            enable(newConfiguration);
            return;
        }

        // FIXME: now we need to compare configurations and decide how to apply them
        throw new UnsupportedOperationException();
    }


    @Holding("this")
    private void enable(final PCEPTopologyConfiguration newConfiguration) {
        // Assert we are performing an asynchronous operation
        final var future = SettableFuture.<Empty>create();
        asyncOperation = future;

        // First start the manager
        manager = new ServerSessionManager(instanceIdentifier, dependencies, newConfiguration);
        final var managerInit = manager.initialize();
        managerInit.addListener(() -> enableChannel(future, Futures.getUnchecked(managerInit)),
            MoreExecutors.directExecutor());
    }

    private synchronized void enableChannel(final SettableFuture<Empty> future, final Boolean managerSuccess) {
        if (!managerSuccess) {
            manager = null;
            future.set(Empty.getInstance());
            return;
        }

        LOG.info("PCEP Topology Provider {} starting server channel", topologyId());
        final var channelFuture = dependencies.getPCEPDispatcher()
            .createServer(manager.getPCEPDispatcherDependencies());
        channelFuture.addListener(ignored -> enableRPCs(future, channelFuture));
    }

    private synchronized void enableRPCs(final SettableFuture<Empty> future, final ChannelFuture channelFuture) {
        final var channelFailure = channelFuture.cause();
        if (channelFailure != null) {
            LOG.error("Topology Provider {} failed to initialize server channel", topologyId(), channelFailure);
            disableManager();
            return;
        }

        channel = channelFuture.channel();

        // Register RPCs
        final RpcProviderService rpcRegistry = dependencies.getRpcProviderRegistry();
        elementReg = rpcRegistry.registerRpcImplementation(NetworkTopologyPcepService.class,
            new TopologyRPCs(manager), Set.of(instanceIdentifier));
        networkReg = rpcRegistry.registerRpcImplementation(NetworkTopologyPcepProgrammingService.class,
            new TopologyProgramming(scheduler, manager), Set.of(instanceIdentifier));

        // We are now completely initialized
        asyncOperation = null;
        future.set(Empty.getInstance());
    }

    @Holding("this")
    private void disableManager() {
        // TODO Auto-generated method stub

    }

    private @NonNull String topologyId() {
        return instanceIdentifier.getKey().getTopologyId().getValue();
    }








    @Holding("this")
    private void disable() {
        if (networkReg != null) {
            networkReg.close();
            networkReg = null;
        }
        if (elementReg != null) {
            elementReg.close();
            elementReg = null;
        }

        // FIXME: actually disable everything

        configuration = null;
    }



    private synchronized ListenableFuture<ChannelInit> initializeChannel(final ServerSessionManager manager) {
        if (manager == null) {
            return FluentFutures.immediateNullFluentFuture();
        }


        // TODO Auto-generated method stub
        return null;
    }

    private synchronized boolean initializeRpcs(final ChannelInit channelInit) {
        if (channelInit == null) {
            return false;
        }


        return true;
    }

    void instantiateServiceInstance() throws ExecutionException, InterruptedException {


        channelFuture.get();
        channel = channelFuture.channel();
    }

    FluentFuture<? extends CommitInfo> closeServiceInstance() {
        //FIXME return also channelClose once ListenableFuture implements wildcard
        channel.close().addListener((ChannelFutureListener) future ->
                checkArgument(future.isSuccess(), "Channel failed to close: %s", future.cause()));

        return manager.closeServiceInstance();
    }

}