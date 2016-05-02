/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.ipv6.DestinationFlowspecIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class FlowspecIpv6RIBSupport extends AbstractFlowspecRIBSupport {
    // define route related classes
    private static final Class<? extends DataObject> CONTAINER_CLASS = FlowspecIpv6Routes.class;
    private static final Class<? extends Route> LIST_CLASS = FlowspecRoute.class;
    private static final Class<? extends Routes> ROUTES_CASE_CLASS = FlowspecIpv6RoutesCase.class;

    private SimpleFlowspecIpv6NlriParser FS_PARSER;

    public FlowspecIpv6RIBSupport(SimpleFlowspecExtensionProviderContext context) {
        super(ROUTES_CASE_CLASS, CONTAINER_CLASS, LIST_CLASS, DestinationFlowspecIpv6.QNAME);
        FS_PARSER = new SimpleFlowspecIpv6NlriParser(context.getFlowspecIpv6TypeRegistry());
    }

    static FlowspecIpv6RIBSupport getInstance(SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecIpv6RIBSupport(context);
    }

    @Override
    protected AbstractFlowspecNlriParser getParser() {
        return FS_PARSER;
    }

    @Override
    protected Class<? extends AddressFamily> getAfiClass() {
        return Ipv6AddressFamily.class;
    }

}
