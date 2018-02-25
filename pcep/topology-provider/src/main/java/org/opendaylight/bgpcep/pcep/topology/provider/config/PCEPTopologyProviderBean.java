/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.pcep.topology.provider.PCEPTopologyProvider;
import org.opendaylight.bgpcep.pcep.topology.provider.TopologySessionListenerFactory;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
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

public final class PCEPTopologyProviderBean implements PCEPTopologyProviderDependenciesProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProviderBean.class);

    private static final String STATEFUL_NOT_DEFINED = "Stateful capability not defined, aborting PCEP Topology " +
        "Deployer instantiation";
    private final PCEPDispatcher pcepDispatcher;
    private final DataBroker dataBroker;
    private final TopologySessionListenerFactory sessionListenerFactory;
    private final RpcProviderRegistry rpcProviderRegistry;
    private final BundleContext bundleContext;
    private final ClusterSingletonServiceProvider cssp;
    @GuardedBy("this")
    private PCEPTopologyProviderBeanCSS pcepTopoProviderCSS;

    public PCEPTopologyProviderBean(final ClusterSingletonServiceProvider cssp, final BundleContext bundleContext,
        final DataBroker dataBroker, final PCEPDispatcher pcepDispatcher, final RpcProviderRegistry rpcProviderRegistry,
        final TopologySessionListenerFactory sessionListenerFactory) {
        this.cssp = Preconditions.checkNotNull(cssp);
        this.bundleContext = Preconditions.checkNotNull(bundleContext);
        this.pcepDispatcher = Preconditions.checkNotNull(pcepDispatcher);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.sessionListenerFactory = Preconditions.checkNotNull(sessionListenerFactory);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
        final List<PCEPCapability> capabilities = this.pcepDispatcher.getPCEPSessionNegotiatorFactory()
            .getPCEPSessionProposalFactory().getCapabilities();
        final boolean statefulCapability = capabilities.stream().anyMatch(PCEPCapability::isStateful);
        if (!statefulCapability) {
            throw new IllegalStateException(STATEFUL_NOT_DEFINED);
        }
    }

    synchronized ListenableFuture<Void> closeServiceInstance() {
        if (this.pcepTopoProviderCSS != null) {
            return this.pcepTopoProviderCSS.closeServiceInstance();
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public synchronized void close() {
        if (this.pcepTopoProviderCSS != null) {
            this.pcepTopoProviderCSS.close();
            this.pcepTopoProviderCSS = null;
        }
    }

    synchronized void start(final PCEPTopologyConfigDependencies configDependencies) {
        Preconditions.checkState(this.pcepTopoProviderCSS == null,
            "Previous instance %s was not closed.", this);
        try {
            this.pcepTopoProviderCSS = new PCEPTopologyProviderBeanCSS(configDependencies);
        } catch (final Exception e) {
            LOG.debug("Failed to create PCEPTopologyProvider {}", configDependencies.getTopologyId().getValue(), e);
        }
    }

    @Override
    public PCEPDispatcher getPCEPDispatcher() {
        return this.pcepDispatcher;
    }

    @Override
    public RpcProviderRegistry getRpcProviderRegistry() {
        return this.rpcProviderRegistry;
    }

    @Override
    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    @Override
    public TopologySessionListenerFactory getTopologySessionListenerFactory() {
        return this.sessionListenerFactory;
    }

    private class PCEPTopologyProviderBeanCSS implements ClusterSingletonService, AutoCloseable {
        private final ServiceGroupIdentifier sgi;
        private ServiceRegistration<?> serviceRegistration;
        private ClusterSingletonServiceRegistration cssRegistration;
        private final PCEPTopologyProvider pcepTopoProvider;
        @GuardedBy("this")
        private boolean serviceInstantiated;

        PCEPTopologyProviderBeanCSS(final PCEPTopologyConfigDependencies configDependencies) throws Exception {
                this.sgi = configDependencies.getSchedulerDependency().getIdentifier();
                this.pcepTopoProvider = PCEPTopologyProvider.create(PCEPTopologyProviderBean.this, configDependencies);

                final Dictionary<String, String> properties = new Hashtable<>();
                properties.put(PCEPTopologyProvider.class.getName(), configDependencies.getTopologyId().getValue());
                this.serviceRegistration = PCEPTopologyProviderBean.this.bundleContext
                    .registerService(DefaultTopologyReference.class.getName(), this.pcepTopoProvider, properties);
            LOG.info("PCEP Topology Provider service {} registered", getIdentifier().getValue());
            this.cssRegistration = PCEPTopologyProviderBean.this.cssp.registerClusterSingletonService(this);
        }

        @Override
        public synchronized void instantiateServiceInstance() {
            LOG.info("PCEP Topology Provider Singleton Service {} instantiated", getIdentifier().getValue());
            this.pcepTopoProvider.instantiateServiceInstance();
            this.serviceInstantiated = true;
        }

        @Override
        public synchronized ListenableFuture<Void> closeServiceInstance() {
            LOG.info("Close PCEP Topology Provider Singleton Service {}", getIdentifier().getValue());
            if (this.serviceInstantiated) {
                this.serviceInstantiated = false;
                return this.pcepTopoProvider.closeServiceInstance();
            }
            return Futures.immediateFuture(null);
        }

        @Nonnull
        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return this.sgi;
        }

        @Override
        public synchronized void close() {
            if (this.cssRegistration != null) {
                try {
                    this.cssRegistration.close();
                } catch (final Exception e) {
                    LOG.debug("Failed to close PCEP Topology Provider service {}", this.sgi.getValue(), e);
                }
                this.cssRegistration = null;
            }
            if (this.serviceRegistration != null) {
                this.serviceRegistration.unregister();
                this.serviceRegistration = null;
            }
        }
    }
}
