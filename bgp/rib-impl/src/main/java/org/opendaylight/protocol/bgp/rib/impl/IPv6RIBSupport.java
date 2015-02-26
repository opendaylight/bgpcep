/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes._case.Ipv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.routes.ipv6.routes._case.ipv6.routes.Ipv6Route;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

final class IPv6RIBSupport implements RIBSupport {
    private static final IPv6RIBSupport SINGLETON = new IPv6RIBSupport();
    private static final ChoiceNode EMPTY_ROUTES = Builders.choiceBuilder()
            .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
            .addChild(Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(Ipv6Routes.QNAME))
                .withChild(ImmutableNodes.mapNodeBuilder(Ipv6Route.QNAME).build()).build()).build();

    private IPv6RIBSupport() {

    }

    static IPv6RIBSupport getInstance() {
        return SINGLETON;
    }

    @Override
    public ChoiceNode emptyRoutes() {
        return EMPTY_ROUTES;
    }
}
