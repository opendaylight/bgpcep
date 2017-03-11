/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.opendaylight.bgpcep.pcep.topology.provider.PCEPTopologyProvider;
import org.opendaylight.bgpcep.pcep.topology.provider.TopologySessionListenerFactory;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.protocol.pcep.PCEPCapability;
import org.opendaylight.protocol.pcep.PCEPDispatcher;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
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
    private PCEPTopologyProvider pcepTopoProvider;

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

    @Override
    public void close() {
        if (this.pcepTopoProvider != null) {
            this.pcepTopoProvider.close();
        }
    }

    public void start(final InetSocketAddress inetSocketAddress, final Optional<KeyMapping> keys,
        final InstructionScheduler schedulerDependency, final TopologyId topologyId,
        final Optional<PCEPTopologyProviderRuntimeRegistrator> runtime, final short rpcTimeout) {
        Preconditions.checkState(this.pcepTopoProvider == null,
            "Previous instance %s was not closed.", this);
        try {
            this.pcepTopoProvider = PCEPTopologyProvider.create(this,
                inetSocketAddress, keys, schedulerDependency, topologyId, runtime, rpcTimeout);

            final Dictionary<String, String> properties = new Hashtable<>();
            properties.put(PCEPTopologyProvider.class.getName(), topologyId.getValue());
            final ServiceRegistration<?> serviceRegistration = this.bundleContext
                .registerService(DefaultTopologyReference.class.getName(), this.pcepTopoProvider, properties);
            this.pcepTopoProvider.setServiceRegistration(serviceRegistration);
        } catch (final Exception e) {
            LOG.debug("Failed to create PCEPTopologyProvider {}", topologyId.getValue());
        }
    }

    @Override
    public PCEPDispatcher getPCEPDispatcher() {
        return this.pcepDispatcher;
    }

    @Override
    public ClusterSingletonServiceProvider getClusterSingletonServiceProvider() {
        return this.cssp;
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
}
