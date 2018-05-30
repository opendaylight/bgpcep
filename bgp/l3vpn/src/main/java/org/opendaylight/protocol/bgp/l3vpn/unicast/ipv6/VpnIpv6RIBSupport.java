/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.unicast.ipv6;

import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.l3vpn.unicast.AbstractVpnRIBSupport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.bgp.rib.rib.loc.rib.tables.routes.VpnIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.bgp.rib.rib.loc.rib.tables.routes.VpnIpv6RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.l3vpn.ipv6.destination.VpnIpv6Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.l3vpn.ipv6.destination.VpnIpv6DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.l3vpn.ipv6.routes.VpnIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.l3vpn.ipv6.routes.VpnIpv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.destination.type.VpnDestination;

public final class VpnIpv6RIBSupport extends AbstractVpnRIBSupport<VpnIpv6RoutesCase, VpnIpv6Routes> {
    private static final VpnIpv6Routes EMPTY_CONTAINER
            = new VpnIpv6RoutesBuilder().setVpnRoute(Collections.emptyList()).build();
    private static final VpnIpv6RoutesCase EMPTY_CASE
            = new VpnIpv6RoutesCaseBuilder().setVpnIpv6Routes(EMPTY_CONTAINER).build();
    private static VpnIpv6RIBSupport SINGLETON;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     */
    private VpnIpv6RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                VpnIpv6RoutesCase.class,
                VpnIpv6Routes.class,
                Ipv6AddressFamily.class,
                VpnIpv6Destination.QNAME);
    }

    public static synchronized VpnIpv6RIBSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new VpnIpv6RIBSupport(mappingService);
        }
        return SINGLETON;
    }

    @Override
    protected IpPrefix createPrefix(final String prefix) {
        return new IpPrefix(new Ipv6Prefix(prefix));
    }

    @Override
    protected DestinationType getAdvertisedDestinationType(List<VpnDestination> dests) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.update
                .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv6CaseBuilder()
                .setVpnIpv6Destination(new VpnIpv6DestinationBuilder().setVpnDestination(dests).build()).build();
    }

    @Override
    protected DestinationType getWithdrawnDestinationType(List<VpnDestination> dests) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev180329.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv6CaseBuilder()
                .setVpnIpv6Destination(new VpnIpv6DestinationBuilder().setVpnDestination(dests).build()).build();
    }

    @Override
    public VpnIpv6RoutesCase emptyRoutesCase() {
        return EMPTY_CASE;
    }

    @Override
    public VpnIpv6Routes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }
}
