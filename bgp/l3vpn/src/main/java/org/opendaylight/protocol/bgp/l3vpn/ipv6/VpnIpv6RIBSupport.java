/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.l3vpn.ipv6;

import java.util.List;
import org.opendaylight.protocol.bgp.l3vpn.AbstractVpnRIBSupport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev171207.bgp.rib.rib.loc.rib.tables.routes.VpnIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev171207.l3vpn.ipv6.destination.VpnIpv6Destination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev171207.l3vpn.ipv6.destination.VpnIpv6DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev171207.l3vpn.ipv6.routes.VpnIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev171207.l3vpn.ip.destination.type.VpnDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.rev171207.l3vpn.ip.route.VpnRoute;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;

final class VpnIpv6RIBSupport extends AbstractVpnRIBSupport {

    /**
     * Default constructor. Requires the QName of the container augmented under the routes choice
     * node in instantiations of the rib grouping. It is assumed that this container is defined by
     * the same model which populates it with route grouping instantiation, and by extension with
     * the route attributes container.
     */
    VpnIpv6RIBSupport() {
        super(VpnIpv6RoutesCase.class, VpnIpv6Routes.class, VpnRoute.class,
                Ipv6AddressFamily.class, VpnIpv6Destination.QNAME);
    }

    @Override
    protected IpPrefix extractPrefix(final DataContainerNode<? extends YangInstanceIdentifier.PathArgument> route,
            final YangInstanceIdentifier.NodeIdentifier prefixTypeNid) {
        if (route.getChild(prefixTypeNid).isPresent()) {
            final String prefixType = (String) route.getChild(prefixTypeNid).get().getValue();
            return new IpPrefix(new Ipv6Prefix(prefixType));
        }
        return null;
    }

    @Override
    protected DestinationType getAdvertisedDestinationType(List<VpnDestination> dests) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev171207.update
                .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationVpnIpv6CaseBuilder()
                .setVpnIpv6Destination(new VpnIpv6DestinationBuilder().setVpnDestination(dests).build()).build();
    }

    @Override
    protected DestinationType getWithdrawnDestinationType(List<VpnDestination> dests) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.vpn.ipv6.rev171207.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationVpnIpv6CaseBuilder()
                .setVpnIpv6Destination(new VpnIpv6DestinationBuilder().setVpnDestination(dests).build()).build();
    }
}
