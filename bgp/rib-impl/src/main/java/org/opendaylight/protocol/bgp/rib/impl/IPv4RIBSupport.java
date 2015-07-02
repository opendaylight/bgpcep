/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.DestinationIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.prefixes.destination.ipv4.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class supporting IPv4 unicast RIBs.
 */
final class IPv4RIBSupport extends AbstractIPRIBSupport {

    private static final Logger LOG = LoggerFactory.getLogger(IPv4RIBSupport.class);

    @VisibleForTesting
    static final QName PREFIX_QNAME = QName.cachedReference(QName.create(Ipv4Route.QNAME, "prefix"));
    private static final IPv4RIBSupport SINGLETON = new IPv4RIBSupport();
    private static final ImmutableCollection<Class<? extends DataObject>> CACHEABLE_NLRI_OBJECTS =
            ImmutableSet.<Class<? extends DataObject>>of(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.Ipv4Prefix.class);
    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
            .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
            .addChild(Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(Ipv4Routes.QNAME))
                .withChild(ImmutableNodes.mapNodeBuilder(Ipv4Route.QNAME).build()).build()).build();
    private final NodeIdentifier destination = new NodeIdentifier(DestinationIpv4.QNAME);
    private final NodeIdentifier route = new NodeIdentifier(Ipv4Route.QNAME);
    private final NodeIdentifier nlriRoutesList = new NodeIdentifier(Ipv4Prefixes.QNAME);
    private final NodeIdentifier routeKeyLeaf = new NodeIdentifier(PREFIX_QNAME);

    private IPv4RIBSupport() {
        super(Ipv4RoutesCase.class, Ipv4Routes.class, Ipv4Route.class);
    }

    static IPv4RIBSupport getInstance() {
        return SINGLETON;
    }

    @Override
    public ChoiceNode emptyRoutes() {
        return this.emptyRoutes;
    }

    @Override
    protected NodeIdentifier destinationContainerIdentifier() {
        return this.destination;
    }

    @Override
    protected NodeIdentifier routeIdentifier() {
        return this.route;
    }

    @Override
    protected NodeIdentifier routeKeyLeafIdentifier() {
        return this.routeKeyLeaf;
    }

    @Override
    protected NodeIdentifier nlriRoutesListIdentifier() {
        return this.nlriRoutesList;
    }

    @Override
    protected QName keyLeafQName() {
        return PREFIX_QNAME;
    }

    @Override
    protected QName routeQName() {
        return Ipv4Route.QNAME;
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return CACHEABLE_NLRI_OBJECTS;
    }

    @Override
    protected MpReachNlri buildReach(final Collection<MapEntryNode> routes, final CNextHop hop) {
        LOG.warn("Attempt to advertise using MP Reach");
        return new MpReachNlriBuilder().build();
    }

    @Override
    protected MpUnreachNlri buildUnreach(final Collection<MapEntryNode> routes) {
        LOG.warn("Attempt to withdraw using MP Reach");
        return  new MpUnreachNlriBuilder().build();
    }
}
