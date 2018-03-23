/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.labeled.unicast;

import java.util.Collection;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.bgp.rib.rib.loc.rib.tables.routes.LabeledUnicastIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.bgp.rib.rib.peer.effective.rib.in.tables.routes.LabeledUnicastIpv6RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.labeled.unicast.ipv6.routes.LabeledUnicastIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.labeled.unicast.ipv6.routes.LabeledUnicastIpv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicast;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

final class LabeledUnicastIpv6RIBSupport extends AbstractLabeledUnicastRIBSupport {
    private static final LabeledUnicastIpv6RIBSupport SINGLETON = new LabeledUnicastIpv6RIBSupport();

    private LabeledUnicastIpv6RIBSupport() {
        super(LabeledUnicastIpv6RoutesCase.class, LabeledUnicastIpv6Routes.class, LabeledUnicastRoute.class,
                Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class, DestinationIpv6LabeledUnicast.QNAME);
    }

    static LabeledUnicastIpv6RIBSupport getInstance() {
        return SINGLETON;
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                new DestinationIpv6LabeledUnicastBuilder().setCLabeledUnicastDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.update.attributes.mp.unreach
                .nlri.withdrawn.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180323.update.attributes.mp
                        .unreach.nlri.withdrawn.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder()
                        .setCLabeledUnicastDestination(extractRoutes(routes)).build()).build();
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
    public Routes emptyRoutesContainer() {
        return new LabeledUnicastIpv6RoutesCaseBuilder()
                .setLabeledUnicastIpv6Routes(new LabeledUnicastIpv6RoutesBuilder().build()).build();
    }
}
