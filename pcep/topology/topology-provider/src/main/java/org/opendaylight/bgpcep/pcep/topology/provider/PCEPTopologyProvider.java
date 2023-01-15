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
import io.netty.channel.epoll.EpollChannelOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.Holding;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.NetworkTopologyPcepProgrammingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.NetworkTopologyPcepService;
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

    // Future indicating shutdown in progress
    @GuardedBy("this")
    private SettableFuture<Empty> stopFuture;

    // Low-level state bits
    @GuardedBy("this")
    private ServerSessionManager manager;
    @GuardedBy("this")
    private TopologyPCEPSessionNegotiatorFactory negotiatorFactory;
    @GuardedBy("this")
    private Channel channel;
    @GuardedBy("this")
    private Registration networkReg;
    @GuardedBy("this")
    private Registration elementReg;

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
            stopFuture.set(Empty.value());
        }
        return stopFuture;
    }

    synchronized void updateConfiguration(final @Nullable PCEPTopologyConfiguration newConfiguration) {
        // FIXME: BGPCEP-960: this check should be a one-time thing in PCEPTopologyTracker startup once we have OSGi DS
        final var effectiveConfig = dependencies.getCapabilities().stream().anyMatch(PCEPCapability::isStateful)
            ? newConfiguration : null;

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

        // We need to perform a complete restart if the listen address changes
        final var currentAddress = currentConfig.getAddress();
        final var newAddress = newConfiguration.getAddress();
        if (!currentAddress.equals(newAddress)) {
            LOG.info("Topology Provider {} listen address changed from {} to {}, restarting", topologyId(),
                currentAddress, newAddress);
            applyConfiguration(null);
            applyConfiguration(newConfiguration);
            return;
        }

        // FIXME: can we apply this less aggressively to just routing it through manager to the negotiator factory?
        final var currentTimerProposal = currentConfig.getTimerProposal();
        final var newTimerProposal = newConfiguration.getTimerProposal();
        if (!currentTimerProposal.equals(newTimerProposal)) {
            LOG.info("Topology Provider {} timer proposal changed from {} to {}, restarting", topologyId(),
                currentTimerProposal, newTimerProposal);
            applyConfiguration(null);
            applyConfiguration(newConfiguration);
            return;
        }

        // FIXME: can we apply this less aggressively to just routing it through manager to the negotiator factory?
        final var currentMaxUnkownMessages = currentConfig.getMaxUnknownMessages();
        final var newMaxUnknownMessages = newConfiguration.getMaxUnknownMessages();
        if (!currentMaxUnkownMessages.equals(newMaxUnknownMessages)) {
            LOG.info("Topology Provider {} max-unknown-messages changed from {} to {}, restarting", topologyId(),
                currentMaxUnkownMessages, newMaxUnknownMessages);
            applyConfiguration(null);
            applyConfiguration(newConfiguration);
            return;
        }

        // FIXME: can we apply this less aggressively to just routing it through manager to the negotiator factory?
        final var currentTls = currentConfig.getTls();
        final var newTls = newConfiguration.getTls();
        if (!Objects.equals(currentTls, newTls)) {
            LOG.info("Topology Provider {} TLS changed from {} to {}, restarting", topologyId(), currentTls, newTls);
            applyConfiguration(null);
            applyConfiguration(newConfiguration);
            return;
        }

        // TCP-MD5 configuration is propagated from the server channel to individual channels. For any node that has
        // changed this configuration we need to tear down any existing session.
        final var currentKeys = currentConfig.getKeys().asMap();
        final var newKeys = newConfiguration.getKeys().asMap();
        final var outdatedNodes = Stream.concat(currentKeys.keySet().stream(), newKeys.keySet().stream())
            .distinct()
            .filter(nodeId -> !Arrays.equals(currentKeys.get(nodeId), newKeys.get(nodeId)))
            .collect(Collectors.toUnmodifiableList());

        manager.setRpcTimeout(newConfiguration.getRpcTimeout());
        manager.setUpdateInterval(newConfiguration.getUpdateInterval());
        if (!outdatedNodes.isEmpty()) {
            LOG.info("Topology Provider {} updating {} TCP-MD5 keys", topologyId(), outdatedNodes.size());
            if (channel.config().setOption(EpollChannelOption.TCP_MD5SIG, newKeys)) {
                manager.tearDownSessions(outdatedNodes);
            } else {
                LOG.warn("Topology Provider {} failed to update TCP-MD5 keys", topologyId());
            }
        }

        currentConfig = newConfiguration;
        LOG.info("Topology Provider {} configuration updated", topologyId());
    }

    @Holding("this")
    private void enable(final PCEPTopologyConfiguration newConfiguration) {
        // Assert we are performing an asynchronous operation
        final var future = startOperation();
        currentConfig = newConfiguration;

        // First start the manager
        manager = new ServerSessionManager(instanceIdentifier, dependencies, newConfiguration.getGraphKey(),
            newConfiguration.getRpcTimeout(), newConfiguration.getUpdateInterval());
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

        negotiatorFactory = new TopologyPCEPSessionNegotiatorFactory(manager, currentConfig.getTimerProposal(),
            dependencies.getCapabilities(), currentConfig.getMaxUnknownMessages(), currentConfig.getTls(),
            dependencies.getDataBroker(), instanceIdentifier);

        LOG.info("PCEP Topology Provider {} starting server channel", topologyId());
        final var channelFuture = dependencies.getPCEPDispatcher().createServer(currentConfig.getAddress(),
            currentConfig.getKeys(), dependencies.getMessageRegistry(), negotiatorFactory);
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
        final var channelFuture = channel.close();
        channel = null;
        channelFuture.addListener(ignored -> disableManager(future));
    }

    @Holding("this")
    private void disableManager(final SettableFuture<Empty> future) {
        negotiatorFactory.close();
        negotiatorFactory = null;
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
        future.set(Empty.value());

        // Process next configuration change if there is one
        if (nextConfig != null) {
            final var config = nextConfig.orElse(null);
            nextConfig = null;
            doApplyConfiguration(config);
            return;
        }

        // Check if we are shutting down
        if (stopFuture != null) {
            stopFuture.set(Empty.value());
        }
    }

    private @NonNull String topologyId() {
        return TopologyUtils.friendlyId(instanceIdentifier);
    }
}