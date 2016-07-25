/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.bgp.topology.provider;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyProvider;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.protocol.bgp.rib.DefaultRibReference;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.config.rev160726.Topology1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.topology.types.rev160524.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBgpTopologyProvider implements BgpTopologyProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBgpTopologyProvider.class);

    private final Map<TopologyId, Entry<TopologyReferenceAutoCloseable, AbstractRegistration>> topologyBuilders = new HashMap<>();
    private final AbstractRegistration registration;
    private final DataBroker dataBroker;
    private final BgpTopologyDeployer deployer;

    public AbstractBgpTopologyProvider(final BgpTopologyDeployer deployer) {
        this.deployer = deployer;
        this.registration = deployer.registerTopologyProvider(this);
        this.dataBroker = deployer.getDataBroker();
    }

    @Override
    public final void onTopologyBuilderCreated(final Topology topology) {
        LOG.debug("Cretaing topology builder instance {}", topology);
        final TopologyReferenceAutoCloseable topologyBuilder = createInstance(topology);
        final AbstractRegistration serviceRegistration = this.deployer.registerTopologyReference(topologyBuilder);
        this.topologyBuilders.put(getTopologyId(topologyBuilder), Maps.immutableEntry(topologyBuilder, serviceRegistration));
        LOG.debug("Topology builder instance created {}", topologyBuilder);
    }

    @Override
    public final void onTopologyBuilderRemoved(final Topology topology) {
        LOG.debug("Removing topology builder instance {}", topology);
        final Entry<TopologyReferenceAutoCloseable, AbstractRegistration> topologyBuilder = this.topologyBuilders.remove(topology.getTopologyId());
        if (topologyBuilder != null) {
            topologyBuilder.getValue().close();
            topologyBuilder.getKey().close();
            LOG.debug("Topology builder instance removed {}", topologyBuilder);
        }
    }

    @Override
    public final void close() {
        this.registration.close();
    }

    @Override
    public final boolean topologyTypeFilter(final Topology topology) {
        final TopologyTypes1 topologyTypes = topology.getTopologyTypes().getAugmentation(TopologyTypes1.class);
        if (topologyTypes == null) {
            return false;
        }
        return topologyTypeFilter(topologyTypes);
    }

    TopologyReferenceAutoCloseable createInstance(final Topology topology) {
        final RibReference ribReference = new DefaultRibReference(InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(topology.getAugmentation(Topology1.class).getRibId())));
        return initiate(this.dataBroker, ribReference, topology.getTopologyId());
    }

    abstract TopologyReferenceAutoCloseable initiate(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId);

    abstract boolean topologyTypeFilter(TopologyTypes1 topology);

    private static TopologyId getTopologyId(final TopologyReferenceAutoCloseable topologyBuilder) {
        return topologyBuilder.getInstanceIdentifier().firstKeyOf(Topology.class).getTopologyId();
    }

}
