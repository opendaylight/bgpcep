/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.FlowspecSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.bgp.rib.rib.loc.rib.tables.routes.FlowspecRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.ipv4.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv4.route.FlowspecRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.ipv4.route.FlowspecRouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;

public final class FlowspecIpv4RIBSupport extends AbstractFlowspecRIBSupport<SimpleFlowspecIpv4NlriParser,
        FlowspecRoute, FlowspecRouteKey> {

    public FlowspecIpv4RIBSupport(SimpleFlowspecExtensionProviderContext context) {
        super(
                FlowspecRoutesCase.class,
                FlowspecRoutes.class,
                FlowspecRoute.class,
                Ipv4AddressFamily.class,
                FlowspecSubsequentAddressFamily.class,
                DestinationFlowspec.QNAME,
                new SimpleFlowspecIpv4NlriParser(context
                        .getFlowspecTypeRegistry(SimpleFlowspecExtensionProviderContext.AFI.IPV4,
                                SimpleFlowspecExtensionProviderContext.SAFI.FLOWSPEC))
        );
    }

    static FlowspecIpv4RIBSupport getInstance(SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecIpv4RIBSupport(context);
    }

    @Override
    public FlowspecRoute createRoute(final FlowspecRoute route, final FlowspecRouteKey routeKey,
            final long pathId, final Attributes attributes) {
        final FlowspecRouteBuilder builder;
        if (route != null) {
            builder = new FlowspecRouteBuilder(route);
        } else {
            builder = new FlowspecRouteBuilder();
        }
        return builder.setRouteKey(routeKey.getRouteKey()).setPathId(new PathId(pathId))
                .setAttributes(attributes).build();
    }

    @Override
    public FlowspecRouteKey createNewRouteIdentifier(final long pathId, final FlowspecRouteKey routeKey) {
        return new FlowspecRouteKey(new PathId(pathId), routeKey.getRouteKey());
    }
}
