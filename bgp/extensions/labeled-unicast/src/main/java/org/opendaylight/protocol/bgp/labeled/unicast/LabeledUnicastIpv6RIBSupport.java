/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import static com.google.common.base.Verify.verify;

import java.util.Collection;
import java.util.Map;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.bgp.rib.rib.loc.rib.tables.routes.LabeledUnicastIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.ipv6.routes.LabeledUnicastIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.ipv6.routes.LabeledUnicastIpv6RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationIpv6LabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicast;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.ipv6.labeled.unicast._case.DestinationIpv6LabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

final class LabeledUnicastIpv6RIBSupport
        extends AbstractLabeledUnicastRIBSupport<LabeledUnicastIpv6RoutesCase, LabeledUnicastIpv6Routes> {
    private static final LabeledUnicastIpv6Routes EMPTY_CONTAINER = new LabeledUnicastIpv6RoutesBuilder().build();
    private static LabeledUnicastIpv6RIBSupport SINGLETON;

    private LabeledUnicastIpv6RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                LabeledUnicastIpv6RoutesCase.class,
                LabeledUnicastIpv6Routes.class,
                Ipv6AddressFamily.class,
                DestinationIpv6LabeledUnicast.QNAME);
    }

    static synchronized LabeledUnicastIpv6RIBSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new LabeledUnicastIpv6RIBSupport(mappingService);
        }
        return SINGLETON;
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
    protected IpPrefix extractPrefix(final DataContainerNode<? extends YangInstanceIdentifier.PathArgument> route,
            final YangInstanceIdentifier.NodeIdentifier prefixTypeNid) {
        if (route.getChild(prefixTypeNid).isPresent()) {
            final String prefixType = (String) route.getChild(prefixTypeNid).get().getValue();
            return new IpPrefix(new Ipv6Prefix(prefixType));
        }
        return null;
    }

    @Override
    public LabeledUnicastIpv6Routes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    public Map<LabeledUnicastRouteKey, LabeledUnicastRoute> extractAdjRibInRoutes(final Routes routes) {
        verify(routes instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast
            .rev180329.bgp.rib.rib.peer.adj.rib.in.tables.routes.LabeledUnicastIpv6RoutesCase, "Unrecognized routes %s",
            routes);
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329
                .bgp.rib.rib.peer.adj.rib.in.tables.routes.LabeledUnicastIpv6RoutesCase) routes)
                .getLabeledUnicastIpv6Routes().nonnullLabeledUnicastRoute();
    }
}
