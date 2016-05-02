/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.ipv4.l3vpn;

import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecNlriParser;
import org.opendaylight.protocol.bgp.flowspec.AbstractFlowspecRIBSupport;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecExtensionProviderContext;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecIpv4NlriParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecL3vpnIpv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.l3vpn.ipv4.routes.FlowspecL3vpnIpv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * @author Kevin Wang
 */
public class FlowspecL3vpnIpv4RIBSupport extends AbstractFlowspecRIBSupport {

    private SimpleFlowspecIpv4NlriParser FS_PARSER;

    private final NodeIdentifier destinationNid = new NodeIdentifier(DestinationFlowspec.QNAME);
    private final NodeIdentifier routeNid = new NodeIdentifier(FlowspecRoute.QNAME);
    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
        .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
        .addChild(Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(FlowspecL3vpnIpv4Routes.QNAME))
            .addChild(ImmutableNodes.mapNodeBuilder(FlowspecRoute.QNAME).build()).build()).build();

    public FlowspecL3vpnIpv4RIBSupport(SimpleFlowspecExtensionProviderContext context) {
        super(FlowspecL3vpnIpv4RoutesCase.class, FlowspecL3vpnIpv4Routes.class, FlowspecRoute.class);
        FS_PARSER = new SimpleFlowspecIpv4NlriParser(context.getFlowspecIpv4TypeRegistry());
    }

    public static FlowspecL3vpnIpv4RIBSupport getInstance(SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecL3vpnIpv4RIBSupport(context);
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
        return Ipv4AddressFamily.class;
    }
}
