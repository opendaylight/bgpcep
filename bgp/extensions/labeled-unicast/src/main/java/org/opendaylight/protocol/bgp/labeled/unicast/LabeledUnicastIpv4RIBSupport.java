/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import java.util.Collection;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.bgp.rib.rib.loc.rib.tables.routes.LabeledUnicastRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicast;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

public final class LabeledUnicastIpv4RIBSupport
        extends AbstractLabeledUnicastRIBSupport<LabeledUnicastRoutesCase, LabeledUnicastRoutes> {
    public LabeledUnicastIpv4RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                LabeledUnicastRoutesCase.class,
                LabeledUnicastRoutes.class,
                Ipv4AddressFamily.VALUE,
                DestinationLabeledUnicast.QNAME);
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return new DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(
                new DestinationLabeledUnicastBuilder()
                        .setCLabeledUnicastDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329
                .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                .DestinationLabeledUnicastCaseBuilder().setDestinationLabeledUnicast(new org.opendaylight.yang.gen.v1
                .urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.unreach.nlri
                .withdrawn.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder()
                .setCLabeledUnicastDestination(extractRoutes(routes)).build()).build();
    }

    @Override
    protected IpPrefix createIpPrefix(final String prefixString) {
        return new IpPrefix(new Ipv4Prefix(prefixString));
    }
}
