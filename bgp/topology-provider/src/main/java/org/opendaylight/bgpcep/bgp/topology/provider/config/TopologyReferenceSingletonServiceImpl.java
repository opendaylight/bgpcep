/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider.config;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.function.Function;
import org.opendaylight.bgpcep.bgp.topology.provider.AbstractTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TopologyReferenceSingletonServiceImpl implements TopologyReferenceSingletonService {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyReferenceSingletonServiceImpl.class);
    private final AbstractTopologyBuilder<?> topologyBuilder;
    private final AbstractRegistration serviceRegistration;
    private final Topology configuration;
    private final Function<Topology, Void> writeFunction;

    TopologyReferenceSingletonServiceImpl(final AbstractTopologyBuilder<?> topologyBuilder, final BgpTopologyDeployer deployer,
            final Topology configuration, final Function<Topology, Void> writeFunction) {
        this.writeFunction = writeFunction;
        this.configuration = Preconditions.checkNotNull(configuration);
        this.topologyBuilder = Preconditions.checkNotNull(topologyBuilder);
        this.serviceRegistration = deployer.registerService(this);
    }

    @Override
    public InstanceIdentifier<Topology> getInstanceIdentifier() {
        return this.topologyBuilder.getInstanceIdentifier();
    }

    @Override
    public void close() {
        this.serviceRegistration.close();
    }

    @Override
    public void instantiateServiceInstance() {
        LOG.info("Topology Singleton Service {} instantiated", getIdentifier());
        this.topologyBuilder.start();
        if (this.writeFunction != null) {
            this.writeFunction.apply(this.configuration);
        }
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        LOG.info("Close Topology Singleton Service {}", getIdentifier());
        this.topologyBuilder.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return ServiceGroupIdentifier.create(getInstanceIdentifier().firstKeyOf(Topology.class).getTopologyId().getValue());
    }

    @Override
    public Topology getConfiguration() {
        return this.configuration;
    }

}
