/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider.config;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Dictionary;
import java.util.Hashtable;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.gaul.modernizer_maven_annotations.SuppressModernizer;
import org.opendaylight.bgpcep.pcep.topology.provider.PCEPTopologyProvider;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PCEPTopologyProviderSingleton implements ClusterSingletonService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProviderSingleton.class);

    private final ServiceGroupIdentifier sgi;
    private final PCEPTopologyProvider pcepTopoProvider;
    private final InstructionScheduler scheduler;

    private ServiceRegistration<?> serviceRegistration;
    private ClusterSingletonServiceRegistration cssRegistration;

    @GuardedBy("this")
    private boolean serviceInstantiated;

    PCEPTopologyProviderSingleton(final PCEPTopologyConfiguration configDependencies,
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
    public synchronized void close() {
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

    @SuppressModernizer
    private static Dictionary<String, String> props(final PCEPTopologyConfiguration configDependencies) {
        final Dictionary<String, String> properties = new Hashtable<>();
        properties.put(PCEPTopologyProvider.class.getName(), configDependencies.getTopologyId().getValue());
        return properties;
    }
}