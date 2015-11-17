/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.bgp.rib.rib.loc.rib.tables.routes.FlowspecRoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.routes.FlowspecRoutes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.routes.flowspec.routes.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

final class FlowspecIpv4RIBSupport extends AbstractFlowspecRIBSupport {

    private SimpleFlowspecIpv4NlriParser FS_PARSER;

    private final NodeIdentifier destinationNid = new NodeIdentifier(DestinationFlowspec.QNAME);
    private final NodeIdentifier routeNid = new NodeIdentifier(FlowspecRoute.QNAME);
    private final ChoiceNode emptyRoutes = Builders.choiceBuilder()
        .withNodeIdentifier(new NodeIdentifier(Routes.QNAME))
        .addChild(Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(FlowspecRoutes.QNAME))
            .addChild(ImmutableNodes.mapNodeBuilder(FlowspecRoute.QNAME).build()).build()).build();

    public FlowspecIpv4RIBSupport(SimpleFlowspecExtensionProviderContext context) {
        super(FlowspecRoutesCase.class, FlowspecRoutes.class, FlowspecRoute.class);
        FS_PARSER = new SimpleFlowspecIpv4NlriParser(context.getFlowspecIpv4TypeRegistry());
    }

    static FlowspecIpv4RIBSupport getInstance(SimpleFlowspecExtensionProviderContext context) {
        return new FlowspecIpv4RIBSupport(context);
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
