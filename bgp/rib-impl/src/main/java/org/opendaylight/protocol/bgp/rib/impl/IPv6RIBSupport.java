/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.destination.type.DestinationIpv6Case;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes._case.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes._case.ipv6.routes.Ipv6Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.route.Attributes;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

final class IPv6RIBSupport extends AbstractIPRIBSupport {
    private static final IPv6RIBSupport SINGLETON = new IPv6RIBSupport();
    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
            .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
            .addChild(Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(Ipv6Routes.QNAME))
                .withChild(ImmutableNodes.mapNodeBuilder(Ipv6Route.QNAME).build()).build()).build();
    private final NodeIdentifier destination = new NodeIdentifier(DestinationIpv6Case.QNAME);
    private final NodeIdentifier routes = new NodeIdentifier(Ipv6Routes.QNAME);
    private final NodeIdentifier route = new NodeIdentifier(Ipv6Route.QNAME);
    private final NodeIdentifier attributes = new NodeIdentifier(QName.create(Ipv6Route.QNAME, Attributes.QNAME.getLocalName()));

    private IPv6RIBSupport() {

    }

    static IPv6RIBSupport getInstance() {
        return SINGLETON;
    }

    @Override
    public ChoiceNode emptyRoutes() {
        return emptyRoutes;
    }

    @Override
    public NodeIdentifier routeAttributes() {
        return attributes;
    }

    @Override
    protected NodeIdentifier destinationIdentifier() {
        return destination;
    }

    @Override
    protected NodeIdentifier routeIdentifier() {
        return route;
    }

    @Override
    protected NodeIdentifier routesIdentifier() {
        return routes;
    }
}
