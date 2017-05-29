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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCEPTopologyDeployerImpl implements PCEPTopologyDeployer, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyDeployerImpl.class);
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);

    @GuardedBy("this")
    private final Map<TopologyId, PCEPTopologyProviderBean> pcepTopologyServices = new HashMap<>();
    private final BlueprintContainer container;

    public PCEPTopologyDeployerImpl(final BlueprintContainer container) {
        this.container = Preconditions.checkNotNull(container);
    }

    @Override
    public synchronized void createTopologyProvider(final TopologyId topologyId,
        final InetSocketAddress inetSocketAddress, final short rpcTimeout, final KeyMapping keys,
        final InstructionScheduler schedulerDependency,
        final Optional<PCEPTopologyProviderRuntimeRegistrator> runtime) {
        if (this.pcepTopologyServices.containsKey(topologyId)) {
            LOG.warn("Topology Provider {} already exist. New instance won't be created", topologyId);
            return;
        }
        final PCEPTopologyProviderBean pcepTopologyProviderBean = (PCEPTopologyProviderBean) this.container
            .getComponentInstance(PCEPTopologyProviderBean.class.getSimpleName());
        this.pcepTopologyServices.put(topologyId, pcepTopologyProviderBean);
        final PCEPTopologyConfigDependencies configDependencies = new PCEPTopologyConfigDependencies(inetSocketAddress,
            keys, schedulerDependency, topologyId, runtime, rpcTimeout);
        pcepTopologyProviderBean.start(configDependencies);
    }

    @Override
    public synchronized void removeTopologyProvider(final TopologyId topologyID) {
        final PCEPTopologyProviderBean service = this.pcepTopologyServices.remove(topologyID);
        if (service != null) {
            try {
                service.closeServiceInstance().get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Failed to close Topology Provider {}.", topologyID.getValue(), e);
            }

            service.close();
        }
    }

    @Override
    public synchronized void close() throws Exception {
        this.pcepTopologyServices.values().forEach(PCEPTopologyProviderBean::closeServiceInstance);
        this.pcepTopologyServices.values().forEach(PCEPTopologyProviderBean::close);
    }
}
