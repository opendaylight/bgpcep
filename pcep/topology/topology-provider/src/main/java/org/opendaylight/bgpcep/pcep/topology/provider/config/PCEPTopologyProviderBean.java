/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import java.util.List;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.bgpcep.pcep.topology.provider.TopologySessionListenerFactory;
import org.opendaylight.bgpcep.pcep.topology.spi.stats.TopologySessionStatsRegistry;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCEPTopologyProviderBean implements PCEPTopologyProviderDependencies, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProviderBean.class);

    private final PCEPDispatcher pcepDispatcher;
    private final DataBroker dataBroker;
    private final TopologySessionListenerFactory sessionListenerFactory;
    private final RpcProviderService rpcProviderRegistry;
    private final TopologySessionStatsRegistry stateRegistry;
    private final PceServerProvider pceServerProvider;
    @GuardedBy("this")
    private PCEPTopologyProviderSingleton pcepTopoProviderCSS;

    PCEPTopologyProviderBean(
            final DataBroker dataBroker,
            final PCEPDispatcher pcepDispatcher,
            final RpcProviderService rpcProviderRegistry,
            final TopologySessionListenerFactory sessionListenerFactory,
            final TopologySessionStatsRegistry stateRegistry,
            final PceServerProvider pceServerProvider) {
        this.pcepDispatcher = requireNonNull(pcepDispatcher);
        this.dataBroker = requireNonNull(dataBroker);
        this.sessionListenerFactory = requireNonNull(sessionListenerFactory);
        this.rpcProviderRegistry = requireNonNull(rpcProviderRegistry);
        this.stateRegistry = requireNonNull(stateRegistry);
        this.pceServerProvider = requireNonNull(pceServerProvider);

        // FIXME: this check should happen before we attempt anything
        final List<PCEPCapability> capabilities = pcepDispatcher.getPCEPSessionNegotiatorFactory()
                .getPCEPSessionProposalFactory().getCapabilities();
        if (!capabilities.stream().anyMatch(PCEPCapability::isStateful)) {
            throw new IllegalStateException(
                "Stateful capability not defined, aborting PCEP Topology Deployer instantiation");
        }
    }

    synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
        if (pcepTopoProviderCSS != null) {
            return pcepTopoProviderCSS.closeServiceInstance();
        }
        return CommitInfo.emptyFluentFuture();
    }

    @Override
    public synchronized void close() throws Exception {
        if (pcepTopoProviderCSS != null) {
            pcepTopoProviderCSS.close();
            pcepTopoProviderCSS = null;
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    synchronized void start(final ClusterSingletonServiceProvider cssp,
            final PCEPTopologyConfiguration configDependencies, final InstructionScheduler instructionScheduler,
            final BundleContext bundleContext) {
        checkState(pcepTopoProviderCSS == null, "Previous instance %s was not closed.", this);
        try {
            pcepTopoProviderCSS = new PCEPTopologyProviderSingleton(configDependencies, this, instructionScheduler,
                cssp, bundleContext);
        } catch (final Exception e) {
            LOG.debug("Failed to create PCEPTopologyProvider {}", configDependencies.getTopologyId().getValue(), e);
        }
    }

    @Override
    public PCEPDispatcher getPCEPDispatcher() {
        return pcepDispatcher;
    }

    @Override
    public RpcProviderService getRpcProviderRegistry() {
        return rpcProviderRegistry;
    }

    @Override
    public DataBroker getDataBroker() {
        return dataBroker;
    }

    @Override
    public TopologySessionListenerFactory getTopologySessionListenerFactory() {
        return sessionListenerFactory;
    }

    @Override
    public TopologySessionStatsRegistry getStateRegistry() {
        return stateRegistry;
    }

    @Override
    public PceServerProvider getPceServerProvider() {
        return pceServerProvider;
    }
}
