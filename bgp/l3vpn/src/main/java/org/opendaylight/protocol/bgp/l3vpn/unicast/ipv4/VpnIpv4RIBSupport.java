/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.unicast.ipv4;

import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.protocol.bgp.l3vpn.unicast.AbstractVpnRIBSupport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.bgp.rib.rib.loc.rib.tables.routes.VpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.bgp.rib.rib.loc.rib.tables.routes.VpnIpv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.l3vpn.ipv4.destination.VpnIpv4Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.l3vpn.ipv4.destination.VpnIpv4DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.l3vpn.ipv4.routes.VpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.l3vpn.ipv4.routes.VpnIpv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev180329.l3vpn.ip.destination.type.VpnDestination;

final class VpnIpv4RIBSupport extends AbstractVpnRIBSupport<VpnIpv4RoutesCase, VpnIpv4Routes> {
    private static final VpnIpv4Routes EMPTY_CONTAINER
            = new VpnIpv4RoutesBuilder().setVpnRoute(Collections.emptyList()).build();
    private static final VpnIpv4RoutesCase EMPTY_CASE
            = new VpnIpv4RoutesCaseBuilder().setVpnIpv4Routes(EMPTY_CONTAINER).build();
    private static VpnIpv4RIBSupport SINGLETON;

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     */
    private VpnIpv4RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                VpnIpv4RoutesCase.class,
                VpnIpv4Routes.class,
                Ipv4AddressFamily.class,
                VpnIpv4Destination.QNAME);
    }

    public static synchronized VpnIpv4RIBSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new VpnIpv4RIBSupport(mappingService);
        }
        return SINGLETON;
    }

    @Override
    protected IpPrefix createPrefix(final String prefix) {
        return new IpPrefix(new Ipv4Prefix(prefix));
    }

    @Override
    protected DestinationType getAdvertisedDestinationType(List<VpnDestination> dests) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.update
                .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv4CaseBuilder()
                .setVpnIpv4Destination(new VpnIpv4DestinationBuilder().setVpnDestination(dests).build()).build();
    }

    @Override
    protected DestinationType getWithdrawnDestinationType(List<VpnDestination> dests) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv4.rev180329.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv4CaseBuilder()
                .setVpnIpv4Destination(new VpnIpv4DestinationBuilder().setVpnDestination(dests).build()).build();
    }

    @Override
    public VpnIpv4RoutesCase emptyRoutesCase() {
        return EMPTY_CASE;
    }

    @Override
    public VpnIpv4Routes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }
}
