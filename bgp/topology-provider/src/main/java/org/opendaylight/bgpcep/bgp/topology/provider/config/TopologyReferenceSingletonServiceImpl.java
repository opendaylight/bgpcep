/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider.config;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.bgpcep.bgp.topology.provider.AbstractTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.singleton.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TopologyReferenceSingletonServiceImpl implements TopologyReferenceSingletonService {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyReferenceSingletonServiceImpl.class);
    private final AbstractTopologyBuilder<?> topologyBuilder;
    private final Registration serviceRegistration;
    private final Topology configuration;

    TopologyReferenceSingletonServiceImpl(final AbstractTopologyBuilder<?> topologyBuilder,
            final BgpTopologyDeployer deployer, final Topology configuration) {
        this.configuration = requireNonNull(configuration);
        this.topologyBuilder = requireNonNull(topologyBuilder);
        serviceRegistration = deployer.registerService(this);
    }

    @Override
    public InstanceIdentifier<Topology> getInstanceIdentifier() {
        return topologyBuilder.getInstanceIdentifier();
    }

    @Override
    public void close() {
        serviceRegistration.close();
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Topology Singleton Service {} instantiated", getIdentifier());
        topologyBuilder.start();
    }

    @Override
    public FluentFuture<? extends CommitInfo> closeServiceInstance() {
        LOG.info("Close Topology Singleton Service {}", getIdentifier());
        return topologyBuilder.close();
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return new ServiceGroupIdentifier(
            getInstanceIdentifier().firstKeyOf(Topology.class).getTopologyId().getValue());
    }

    @Override
    public Topology getConfiguration() {
        return configuration;
    }

}
