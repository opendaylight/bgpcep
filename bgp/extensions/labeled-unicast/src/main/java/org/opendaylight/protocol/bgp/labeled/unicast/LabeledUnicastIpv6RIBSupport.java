/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import java.util.Collection;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.bgp.rib.rib.loc.rib.tables.routes.LabeledUnicastIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.ipv6.routes.LabeledUnicastIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicast;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

final class LabeledUnicastIpv6RIBSupport
        extends AbstractLabeledUnicastRIBSupport<LabeledUnicastIpv6RoutesCase, LabeledUnicastIpv6Routes> {
    LabeledUnicastIpv6RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                LabeledUnicastIpv6RoutesCase.class,
                LabeledUnicastIpv6Routes.class,
                Ipv6AddressFamily.VALUE,
                DestinationIpv6LabeledUnicast.QNAME);
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                new DestinationIpv6LabeledUnicastBuilder()
                        .setCLabeledUnicastDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329
                .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                .DestinationIpv6LabeledUnicastCaseBuilder().setDestinationIpv6LabeledUnicast(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329
                        .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.ipv6
                        .labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder()
                        .setCLabeledUnicastDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected IpPrefix createIpPrefix(final String prefixString) {
        return new IpPrefix(new Ipv6Prefix(prefixString));
    }
}
