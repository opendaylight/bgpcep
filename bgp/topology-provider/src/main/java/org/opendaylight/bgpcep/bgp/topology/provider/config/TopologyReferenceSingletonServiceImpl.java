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
import org.opendaylight.bgpcep.bgp.topology.provider.AbstractTopologyBuilder;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.BgpTopologyDeployer;
import org.opendaylight.bgpcep.bgp.topology.provider.spi.TopologyReferenceSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.concepts.AbstractRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class TopologyReferenceSingletonServiceImpl implements TopologyReferenceSingletonService {

    private final AbstractTopologyBuilder<?> topologyBuilder;
    private final BgpTopologyDeployer deployer;
    private final Class<? extends AddressFamily> afi;
    private final Class<? extends SubsequentAddressFamily> safi;
    private final AbstractRegistration serviceRegistration;

    TopologyReferenceSingletonServiceImpl(final AbstractTopologyBuilder<?> topologyBuilder, final BgpTopologyDeployer deployer,
            final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        this.topologyBuilder = Preconditions.checkNotNull(topologyBuilder);
        this.deployer = Preconditions.checkNotNull(deployer);
        this.afi = Preconditions.checkNotNull(afi);
        this.safi = Preconditions.checkNotNull(safi);
        this.serviceRegistration = this.deployer.registerService(this);
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
        this.topologyBuilder.start(this.deployer.getDataBroker(), this.afi, this.safi);
    }

    @Override
    public ListenableFuture<Void> closeServiceInstance() {
        this.topologyBuilder.close();
        return Futures.immediateFuture(null);
    }

    @Override
    public ServiceGroupIdentifier getIdentifier() {
        return ServiceGroupIdentifier.create(getInstanceIdentifier().firstKeyOf(Topology.class).getTopologyId().getValue());
    }

}
