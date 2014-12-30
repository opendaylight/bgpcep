/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.bgp.topology.provider;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv4.routes._case.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class Ipv4ReachabilityTopologyBuilder extends AbstractReachabilityTopologyBuilder<Ipv4Route> {
    public Ipv4ReachabilityTopologyBuilder(final DataBroker dataProvider, final RibReference locRibReference,
            final TopologyId topologyId) {
        super(dataProvider, locRibReference, topologyId, Ipv4Route.class);
    }

    @Override
    protected Attributes getAttributes(final Ipv4Route value) {
        return value.getAttributes();
    }

    @Override
    protected IpPrefix getPrefix(final Ipv4Route value) {
        return new IpPrefix(value.getKey().getPrefix());
    }

    @Override
    public void register(final DataBroker dataBroker, final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi) {
        final InstanceIdentifier<?> i = tableInstanceIdentifier(afi, safi).child(Ipv4Routes.class).child(Ipv4Route.class);
        final ListenerRegistration<DataChangeListener> r = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, i, this, DataChangeScope.BASE);
        setRegistration(r);
    }
}
