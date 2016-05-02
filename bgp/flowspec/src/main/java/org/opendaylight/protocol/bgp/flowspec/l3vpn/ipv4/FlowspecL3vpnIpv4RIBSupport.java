/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4;

import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecNlriParser;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecRIBSupport;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecExtensionProviderContext;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecIpv4NlriParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * @author Kevin Wang
 */
public class FlowspecL3vpnIpv4RIBSupport extends AbstractFlowspecRIBSupport {
    // define route related classes
    private static final Class<? extends DataObject> CONTAINER_CLASS = FlowspecL3vpnIpv4Routes.class;
    private static final Class<? extends Route> LIST_CLASS = FlowspecRoute.class;
    private static final Class<? extends Routes> ROUTES_CASE_CLASS = FlowspecL3vpnIpv4RoutesCase.class;

    private final SimpleFlowspecIpv4NlriParser FS_PARSER;

    public FlowspecL3vpnIpv4RIBSupport(SimpleFlowspecExtensionProviderContext context) {
        super(ROUTES_CASE_CLASS, CONTAINER_CLASS, LIST_CLASS, DestinationFlowspecL3vpnIpv4.QNAME);
        FS_PARSER = new SimpleFlowspecIpv4NlriParser(context.getFlowspecL3vpnIpv4TypeRegistry());
    }

    public static FlowspecL3vpnIpv4RIBSupport getInstance(SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecL3vpnIpv4RIBSupport(context);
    }

    @Override
    protected AbstractFlowspecNlriParser getParser() {
        return FS_PARSER;
    }

    @Override
    protected Class<? extends AddressFamily> getAfiClass() {
        return Ipv4AddressFamily.class;
    }
}
