/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.NetworkTopologyPcepService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.Registration;
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
    @GuardedBy("this")
    private SettableFuture<Empty> stopFuture;

    private ServerSessionManager manager;
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

    synchronized ListenableFuture<?> stop() {
        if (stopFuture != null) {
            // Already stopping, just return the future
            return stopFuture;
        }

        stopFuture = SettableFuture.create();
        applyConfiguration(null);
        if (asyncOperation == null) {
            stopFuture.set(Empty.getInstance());
        }
        return stopFuture;
    }

    synchronized void updateConfiguration(final @Nullable PCEPTopologyConfiguration newConfiguration) {
        // FIXME: BGPCEP-960: this check should be a one-time thing in PCEPTopologyTracker startup once we have OSGi DS
        final var effectiveConfig = dependencies.getPCEPDispatcher().getPCEPSessionNegotiatorFactory()
            .getPCEPSessionProposalFactory().getCapabilities().stream()
            .anyMatch(PCEPCapability::isStateful) ? newConfiguration : null;

        applyConfiguration(effectiveConfig);
    }

    @Holding("this")
    private void applyConfiguration(final @Nullable PCEPTopologyConfiguration newConfiguration) {
        if (asyncOperation != null) {
            LOG.debug("Topology Provider {} is undergoing reconfiguration, delaying reconfiguration", topologyId());
            nextConfig = Optional.ofNullable(newConfiguration);
        } else {
            doApplyConfiguration(newConfiguration);
        }
    }

    @Holding("this")
    private void doApplyConfiguration(final @Nullable PCEPTopologyConfiguration newConfiguration) {
        LOG.debug("Topology Provider {} applying configuration {}", topologyId(), newConfiguration);

        // Perform obvious enable/disable operations
        if (newConfiguration == null) {
            if (currentConfig != null) {
                LOG.info("Topology Provider {} lost configuration, disabling it", topologyId());
                disable();
            }
            return;
        }
        if (currentConfig == null) {
            LOG.info("Topology Provider {} received configuration, enabling it", topologyId());
            enable(newConfiguration);
            return;
        }

        // FIXME: now we need to compare configurations and decide how to apply them
        throw new UnsupportedOperationException();
    }

    @Holding("this")
    private void enable(final PCEPTopologyConfiguration newConfiguration) {
        // Assert we are performing an asynchronous operation
        final var future = startOperation();
        currentConfig = newConfiguration;

        // First start the manager
        manager = new ServerSessionManager(instanceIdentifier, dependencies, newConfiguration.getRpcTimeout(),
            newConfiguration.getSpeakerIds());
        final var managerStart = manager.start();
        managerStart.addListener(() -> enableChannel(future, Futures.getUnchecked(managerStart)),
            MoreExecutors.directExecutor());
    }

    private synchronized void enableChannel(final SettableFuture<Empty> future, final Boolean managerSuccess) {
        if (!managerSuccess) {
            manager = null;
            currentConfig = null;
            finishOperation(future);
            return;
        }

        LOG.info("PCEP Topology Provider {} starting server channel", topologyId());
        final var channelFuture = dependencies.getPCEPDispatcher().createServer(
            new PCEPDispatcherDependenciesImpl(manager, currentConfig));
        channelFuture.addListener(ignored -> enableRPCs(future, channelFuture));
    }

    private synchronized void enableRPCs(final SettableFuture<Empty> future, final ChannelFuture channelFuture) {
        final var channelFailure = channelFuture.cause();
        if (channelFailure != null) {
            LOG.error("Topology Provider {} failed to initialize server channel", topologyId(), channelFailure);
            disableManager(future);
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
        LOG.info("PCEP Topology Provider {} enabled", topologyId());
        finishOperation(future);
    }

    @Holding("this")
    private void disable() {
        // Unregister RPCs
        if (networkReg != null) {
            networkReg.close();
            networkReg = null;
        }
        if (elementReg != null) {
            elementReg.close();
            elementReg = null;
        }

        // Assert we are performing an asynchronous operation
        final var future = startOperation();

        // Disable channel
        channel.close().addListener(ignored -> disableManager(future));
    }

    @Holding("this")
    private void disableManager(final SettableFuture<Empty> future) {
        final var managerStop = manager.stop();
        manager = null;
        managerStop.addListener(() -> finishStopManager(future), MoreExecutors.directExecutor());
    }

    private synchronized void finishStopManager(final SettableFuture<Empty> future) {
        // We are now completely shut down
        currentConfig = null;
        finishOperation(future);
    }

    @Holding("this")
    private SettableFuture<Empty> startOperation() {
        verify(asyncOperation == null, "Operation %s has not finished yet", asyncOperation);
        final var future = SettableFuture.<Empty>create();
        asyncOperation = future;
        return future;
    }

    @Holding("this")
    private void finishOperation(final SettableFuture<Empty> future) {
        asyncOperation = null;
        future.set(Empty.getInstance());

        // Process next configuration change if there is one
        if (nextConfig != null) {
            final var config = nextConfig.orElse(null);
            nextConfig = null;
            doApplyConfiguration(config);
            return;
        }

        // Check if we are shutting down
        if (stopFuture != null) {
            stopFuture.set(Empty.getInstance());
        }
    }

    private @NonNull String topologyId() {
        return instanceIdentifier.getKey().getTopologyId().getValue();
    }
}