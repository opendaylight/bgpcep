/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import static com.google.common.base.Verify.verify;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.bgp.rib.rib.loc.rib.tables.routes.LabeledUnicastRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.LabeledUnicastRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.LabeledUnicastRoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.labeled.unicast.routes.list.LabeledUnicastRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationLabeledUnicastCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicast;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.labeled.unicast._case.DestinationLabeledUnicastBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

public final class LabeledUnicastIpv4RIBSupport
        extends AbstractLabeledUnicastRIBSupport<LabeledUnicastRoutesCase, LabeledUnicastRoutes> {
    private static final LabeledUnicastRoutes EMPTY_CONTAINER
            = new LabeledUnicastRoutesBuilder().setLabeledUnicastRoute(Collections.emptyList()).build();
    private static LabeledUnicastIpv4RIBSupport SINGLETON;

    private LabeledUnicastIpv4RIBSupport(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService,
                LabeledUnicastRoutesCase.class,
                LabeledUnicastRoutes.class,
                Ipv4AddressFamily.class,
                DestinationLabeledUnicast.QNAME);
    }

    static synchronized LabeledUnicastIpv4RIBSupport getInstance(final BindingNormalizedNodeSerializer mappingService) {
        if (SINGLETON == null) {
            SINGLETON = new LabeledUnicastIpv4RIBSupport(mappingService);
        }
        return SINGLETON;
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
    public IpPrefix extractPrefix(
            final DataContainerNode<? extends PathArgument> route,
            final NodeIdentifier prefixTypeNid) {
        if (route.getChild(prefixTypeNid).isPresent()) {
            final String prefixType = (String) route.getChild(prefixTypeNid).get().getValue();
            return new IpPrefix(new Ipv4Prefix(prefixType));
        }
        return null;
    }

    @Override
    public LabeledUnicastRoutes emptyRoutesContainer() {
        return EMPTY_CONTAINER;
    }

    @Override
    public List<LabeledUnicastRoute> extractAdjRibInRoutes(final Routes routes) {
        verify(routes instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast
            .rev180329.bgp.rib.rib.peer.adj.rib.in.tables.routes.LabeledUnicastRoutesCase, "Unrecognized routes %s",
            routes);
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329
                .bgp.rib.rib.peer.adj.rib.in.tables.routes.LabeledUnicastRoutesCase) routes).getLabeledUnicastRoutes()
                .nonnullLabeledUnicastRoute();
    }
}
