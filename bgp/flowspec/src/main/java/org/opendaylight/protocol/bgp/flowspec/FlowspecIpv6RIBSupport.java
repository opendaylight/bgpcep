/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecIpv6RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.routes.FlowspecIpv6Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv6.routes.flowspec.ipv6.routes.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec.ipv6._case.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public final class FlowspecIpv6RIBSupport extends AbstractFlowspecRIBSupport {

    private static final FSIpv6NlriParser FS_PARSER = new FSIpv6NlriParser();

    private static final FlowspecIpv6RIBSupport SINGLETON = new FlowspecIpv6RIBSupport();

    private final NodeIdentifier destinationNid = new NodeIdentifier(DestinationFlowspec.QNAME);
    private final NodeIdentifier routeNid = new NodeIdentifier(FlowspecRoute.QNAME);
    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
        .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
        .addChild(Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(FlowspecIpv6Routes.QNAME))
            .addChild(ImmutableNodes.mapNodeBuilder(FlowspecRoute.QNAME).build()).build()).build();

    private FlowspecIpv6RIBSupport() {
        super(FlowspecIpv6RoutesCase.class, FlowspecIpv6Routes.class, FlowspecRoute.class);
    }

    static FlowspecIpv6RIBSupport getInstance() {
        return SINGLETON;
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
    protected AbstractFSNlriParser getParser() {
        return FS_PARSER;
    }

    @Override
    protected Class<? extends AddressFamily> getAfiClass() {
        return Ipv6AddressFamily.class;
    }

}
