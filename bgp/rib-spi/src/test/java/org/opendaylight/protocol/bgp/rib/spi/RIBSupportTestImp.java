/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.Ipv4Prefixes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.ipv4.routes.Ipv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.test.rev180515.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;

public final class RIBSupportTestImp extends AbstractRIBSupport<Ipv4RoutesCase, Ipv4Routes, Ipv4Route, Ipv4RouteKey> {
    private static final String ROUTE_KEY = "prefix";
    private static final String PREFIX = "1.2.3.4/32";

    private static final NodeIdentifierWithPredicates PREFIX_NII = new NodeIdentifierWithPredicates(Ipv4Route.QNAME,
            ImmutableMap.of(QName.create(Ipv4Route.QNAME, ROUTE_KEY).intern(), PREFIX));

    public RIBSupportTestImp(final BindingNormalizedNodeSerializer mappingService) {
        super(mappingService, Ipv4RoutesCase.class, Ipv4Routes.class, Ipv4Route.class, Ipv4AddressFamily.class,
                UnicastSubsequentAddressFamily.class, Ipv4Prefixes.QNAME);
    }

    @Override
    protected DestinationType buildDestination(final Collection<MapEntryNode> routes) {
        return null;
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection<MapEntryNode> routes) {
        return null;
    }

    @Override
    protected Collection<NodeIdentifierWithPredicates> processDestination(final DOMDataWriteTransaction tx,
                                                                          final YangInstanceIdentifier routesPath,
                                                                          final ContainerNode destination,
                                                                          final ContainerNode attributes,
                                                                          final ApplyRoute applyFunction) {
        applyFunction.apply(tx, routesPath.node(Ipv4Route.QNAME), PREFIX_NII, destination, attributes);
        return Collections.emptySet();
    }

    @Override
    public Ipv4Route createRoute(final Ipv4Route route, final String routeKey, final long pathId,
            final Attributes attributes) {
        return null;
    }

    @Override
    public Ipv4RoutesCase emptyRoutesCase() {
        return new Ipv4RoutesCaseBuilder().setIpv4Routes(emptyRoutesContainer()).build();
    }

    @Override
    public Ipv4Routes emptyRoutesContainer() {
        return new Ipv4RoutesBuilder().setIpv4Route(Collections.emptyList()).build();
    }

    @Override
    public Ipv4RouteKey createRouteListKey(final long pathId, final String routeKey) {
        return new Ipv4RouteKey(new PathId(pathId), routeKey);
    }

    @Override
    public List<Ipv4Route> routesFromContainer(final Ipv4Routes container) {
        return container.getIpv4Route();
    }
}