/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.SimpleFlowspecTypeRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.Attributes2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.mp.reach.nlri.AdvertizedRoutes;

public final class SimpleFlowspecIpv4NlriParser extends AbstractFlowspecNlriParser {

    public SimpleFlowspecIpv4NlriParser(SimpleFlowspecTypeRegistry flowspecTypeRegistry) {
        this.flowspecTypeRegistry = flowspecTypeRegistry;
    }

    @Override
    DestinationType createWidthdrawnRoutesDestinationType(final List<Flowspec> dst) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCaseBuilder().setDestinationFlowspec(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder().setFlowspec(
                dst).build()).build();
    }

    @Override
    DestinationType createAdvertizedRoutesDestinationType(final List<Flowspec> dst) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder()
            .setDestinationFlowspec(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.destination.flowspec._case.DestinationFlowspecBuilder()
                .setFlowspec(dst).build()).build();
    }

    @Override
    protected void serializeMpReachNlri(final Attributes1 pathAttributes, final ByteBuf byteAggregator) {
        if (pathAttributes == null) {
            return;
        }
        final AdvertizedRoutes routes = (pathAttributes.getMpReachNlri()).getAdvertizedRoutes();
        if (routes != null && routes.getDestinationType() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase flowspecCase = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) routes.getDestinationType();
            serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
        }
    }

    @Override
    protected void serializeMpUnreachNlri(final Attributes2 pathAttributes, final ByteBuf byteAggregator) {
        if (pathAttributes == null) {
            return;
        }
        final MpUnreachNlri mpUnreachNlri = pathAttributes.getMpUnreachNlri();
        if (mpUnreachNlri.getWithdrawnRoutes() != null && mpUnreachNlri.getWithdrawnRoutes().getDestinationType() instanceof DestinationFlowspecCase) {
            final DestinationFlowspecCase flowspecCase = (DestinationFlowspecCase) mpUnreachNlri.getWithdrawnRoutes().getDestinationType();
            serializeNlri(flowspecCase.getDestinationFlowspec().getFlowspec(), byteAggregator);
        }
    }
}
