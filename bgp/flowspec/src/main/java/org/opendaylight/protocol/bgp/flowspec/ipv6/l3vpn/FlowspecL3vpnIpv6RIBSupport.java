/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.ipv6.l3vpn;

import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecNlriParser;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecRIBSupport;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecExtensionProviderContext;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecIpv6NlriParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.ipv6.routes.FlowspecL3vpnIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec.ipv6._case.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author Kevin Wang
 */
public class FlowspecL3vpnIpv6RIBSupport extends AbstractFlowspecRIBSupport {

    private SimpleFlowspecIpv6NlriParser FS_PARSER;

    private final NodeIdentifier destinationNid = new NodeIdentifier(DestinationFlowspec.QNAME);
    private final NodeIdentifier routeNid = new NodeIdentifier(FlowspecRoute.QNAME);
    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
        .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
        .addChild(Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(FlowspecL3vpnIpv6Routes.QNAME))
            .addChild(ImmutableNodes.mapNodeBuilder(FlowspecRoute.QNAME).build()).build()).build();

    public FlowspecL3vpnIpv6RIBSupport(SimpleFlowspecExtensionProviderContext context) {
        super(FlowspecL3vpnIpv6RoutesCase.class, FlowspecL3vpnIpv6Routes.class, FlowspecRoute.class);
        FS_PARSER = new SimpleFlowspecIpv6NlriParser(context.getFlowspecL3vpnIpv6TypeRegistry());
    }

    public static FlowspecL3vpnIpv6RIBSupport getInstance(SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecL3vpnIpv6RIBSupport(context);
    }

    @Override
    public ChoiceNode emptyRoutes() {
        return this.emptyRoutes;
    }

    @Override
    protected NodeIdentifier destinationContainerIdentifier() {
        return this.destinationNid;
    }

    @Override
    protected NodeIdentifier routeIdentifier() {
        return this.routeNid;
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
