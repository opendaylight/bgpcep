/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.rib.spi.AbstractRIBSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public final class RIBSupportTestImp extends AbstractRIBSupport {
    private static final String ROUTE_KEY = "prefix";
    private static final String PREFIX = "1.2.3.4/32";

    private static final NodeIdentifierWithPredicates PREFIX_NII = new NodeIdentifierWithPredicates(org.opendaylight
            .yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route.QNAME,
            ImmutableMap.of(QName.create(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet
                    .rev180329.ipv4.routes.ipv4.routes.Ipv4Route.QNAME, ROUTE_KEY).intern(), PREFIX));

    public RIBSupportTestImp() {
        super(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4
                        .routes.Ipv4RoutesCase.class,
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.
                        routes.Ipv4Routes.class, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
                        .inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route.class, Ipv4AddressFamily.class,
                UnicastSubsequentAddressFamily.class, org.opendaylight.yang.gen.v1.urn.opendaylight.params
                        .xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Prefixes.QNAME);
    }

    @Override
    protected DestinationType buildDestination(final Collection routes) {
        return null;
    }

    @Override
    protected DestinationType buildWithdrawnDestination(final Collection routes) {
        return null;
    }

    @Override
    protected void processDestination(final DOMDataWriteTransaction tx, final YangInstanceIdentifier routesPath,
            final ContainerNode destination, final ContainerNode attributes, final ApplyRoute applyFunction) {
        applyFunction.apply(tx, routesPath.node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                .bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route.QNAME), PREFIX_NII, destination, attributes);
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableAttributeObjects() {
        return null;
    }

    @Override
    public ImmutableCollection<Class<? extends DataObject>> cacheableNlriObjects() {
        return null;
    }

    @Override
    public boolean isComplexRoute() {
        return false;
    }

    @Override
    public Route createRoute(final Route route, final Identifier routeKey, final long pathId
            , final Attributes attributes) {
        return null;
    }

    @Override
    public Routes emptyRoutesContainer() {
        return null;
    }

    @Override
    public Identifier createNewRouteKey(final long pathId, final String routeKey) {
        return null;
    }
}