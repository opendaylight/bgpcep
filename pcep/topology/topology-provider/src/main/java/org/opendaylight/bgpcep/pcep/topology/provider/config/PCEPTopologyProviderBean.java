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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.opendaylight.bgpcep.pcep.server.PceServerProvider;
import org.opendaylight.bgpcep.pcep.topology.provider.PCEPTopologyProvider;
import org.opendaylight.bgpcep.pcep.topology.provider.TopologySessionListenerFactory;
import org.opendaylight.bgpcep.pcep.topology.spi.stats.TopologySessionStatsRegistry;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
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
    private PCEPTopologyProviderBeanCSS pcepTopoProviderCSS;

    public PCEPTopologyProviderBean(
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
            pcepTopoProviderCSS = new PCEPTopologyProviderBeanCSS(configDependencies, this, instructionScheduler, cssp,
                bundleContext);
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

    private static class PCEPTopologyProviderBeanCSS implements ClusterSingletonService, AutoCloseable {
        private final ServiceGroupIdentifier sgi;
        private final PCEPTopologyProvider pcepTopoProvider;
        private final InstructionScheduler scheduler;
        private ServiceRegistration<?> serviceRegistration;
        private ClusterSingletonServiceRegistration cssRegistration;
        @GuardedBy("this")
        private boolean serviceInstantiated;

        PCEPTopologyProviderBeanCSS(final PCEPTopologyConfiguration configDependencies,
                final PCEPTopologyProviderDependencies dependenciesProvider,
                final InstructionScheduler instructionScheduler, final ClusterSingletonServiceProvider cssp,
                // FIXME: this should not be needed
                final BundleContext bundleContext) {
            scheduler = instructionScheduler;
            sgi = scheduler.getIdentifier();
            pcepTopoProvider = PCEPTopologyProvider.create(dependenciesProvider, scheduler, configDependencies);

            serviceRegistration = bundleContext.registerService(DefaultTopologyReference.class.getName(),
                pcepTopoProvider, props(configDependencies));
            LOG.info("PCEP Topology Provider service {} registered", getIdentifier().getName());
            cssRegistration = cssp.registerClusterSingletonService(this);
        }

        @SuppressModernizer
        private static Dictionary<String, String> props(final PCEPTopologyConfiguration configDependencies) {
            final Dictionary<String, String> properties = new Hashtable<>();
            properties.put(PCEPTopologyProvider.class.getName(), configDependencies.getTopologyId().getValue());
            return properties;
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        public synchronized void instantiateServiceInstance() {
            LOG.info("PCEP Topology Provider Singleton Service {} instantiated", getIdentifier().getName());
            try {
                pcepTopoProvider.instantiateServiceInstance();
            } catch (final Exception e) {
                LOG.error("Failed to instantiate PCEP Topology provider", e);
            }
            serviceInstantiated = true;
        }

        @Override
        public synchronized FluentFuture<? extends CommitInfo> closeServiceInstance() {
            LOG.info("Close PCEP Topology Provider Singleton Service {}", getIdentifier().getName());
            if (serviceInstantiated) {
                serviceInstantiated = false;
                return pcepTopoProvider.closeServiceInstance();
            }
            return CommitInfo.emptyFluentFuture();
        }

        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return sgi;
        }

        @Override
        public synchronized void close() throws Exception {
            if (cssRegistration != null) {
                cssRegistration.close();
                cssRegistration = null;
            }
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
            scheduler.close();
        }
    }
}
