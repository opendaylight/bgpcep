/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider.config;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.bgpcep.bgp.topology.provider.AbstractTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyProvider;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev180329.Topology1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractBgpTopologyProvider implements BgpTopologyProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBgpTopologyProvider.class);

    private final Map<TopologyId, TopologyReferenceSingletonService> topologyBuilders = new HashMap<>();
    private final DataBroker dataBroker;
    private final BgpTopologyDeployer deployer;

    private AbstractRegistration registration;

    AbstractBgpTopologyProvider(final BgpTopologyDeployer deployer) {
        this.deployer = requireNonNull(deployer);
        dataBroker = requireNonNull(deployer.getDataBroker());
    }

    final void register() {
        registration = deployer.registerTopologyProvider(this);
    }

    final void unregister() {
        if (registration != null) {
            registration.close();
            registration = null;
        }
    }

    @Override
    public final void onTopologyBuilderCreated(final Topology topology) {
        LOG.debug("Creating topology builder instance {}", topology);
        final TopologyReferenceSingletonService currentInstance = topologyBuilders.get(topology.getTopologyId());
        if (currentInstance == null || !currentInstance.getConfiguration().equals(topology)) {
            final TopologyReferenceSingletonService topologyBuilder = createInstance(topology);
            topologyBuilders.put(topology.getTopologyId(), topologyBuilder);
            LOG.debug("Topology builder instance created {}", topologyBuilder);
        }
    }

    @Override
    public final void onTopologyBuilderRemoved(final Topology topology) {
        LOG.debug("Removing topology builder instance {}", topology);
        final TopologyReferenceSingletonService topologyBuilder =
                topologyBuilders.remove(topology.getTopologyId());
        if (topologyBuilder != null) {
            topologyBuilder.close();
            LOG.debug("Topology builder instance removed {}", topologyBuilder);
        }
    }

    private TopologyReferenceSingletonService createInstance(final Topology topology) {
        final RibReference ribReference = new DefaultRibReference(InstanceIdentifier.create(BgpRib.class)
                .child(Rib.class, new RibKey(topology.augmentation(Topology1.class).getRibId())));
        final AbstractTopologyBuilder<?> topologyBuilder = createTopologyBuilder(dataBroker,
                ribReference, topology.getTopologyId());
        return new TopologyReferenceSingletonServiceImpl(topologyBuilder, deployer, topology);
    }

    abstract AbstractTopologyBuilder<?> createTopologyBuilder(DataBroker dataProvider, RibReference locRibReference,
            TopologyId topologyId);

    static final TopologyTypes1 getTopologyAug(final Topology topology) {
        final TopologyTypes topologyTypes = topology.getTopologyTypes();
        return topologyTypes == null ? null : topologyTypes.augmentation(TopologyTypes1.class);
    }
}
