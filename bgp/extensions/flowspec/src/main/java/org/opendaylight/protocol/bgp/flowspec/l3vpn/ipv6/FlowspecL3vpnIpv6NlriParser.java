/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv6;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.AFI;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.SAFI;
import org.opendaylight.protocol.bgp.flowspec.ipv6.FlowspecIpv6NlriParserHelper;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.AbstractFlowspecL3vpnNlriParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.destination.ipv6.DestinationFlowspecL3vpnIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.destination.ipv6.DestinationFlowspecL3vpnIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

/**
 * L3 VPN IPv6 flowspec parser.
 *
 * @author Kevin Wang
 */
public final class FlowspecL3vpnIpv6NlriParser extends AbstractFlowspecL3vpnNlriParser {
    public FlowspecL3vpnIpv6NlriParser(final SAFI safi) {
        super(FlowspecTypeRegistries.getFlowspecTypeRegistry(AFI.IPV6, safi));
    }

    @Override
    protected DestinationType createAdvertizedRoutesDestinationType(final RouteDistinguisher rd,
            final List<Flowspec> flowspecList, final PathId pathId) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
            .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv6CaseBuilder()
            .setDestinationFlowspecL3vpnIpv6(new DestinationFlowspecL3vpnIpv6Builder()
                .setRouteDistinguisher(rd)
                .setFlowspec(flowspecList)
                .setPathId(pathId)
                .build())
            .build();
    }

    @Override
    protected DestinationType createWithdrawnDestinationType(final RouteDistinguisher rd,
            final List<Flowspec> flowspecList, final PathId pathId) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv6CaseBuilder()
            .setDestinationFlowspecL3vpnIpv6(new DestinationFlowspecL3vpnIpv6Builder()
                .setRouteDistinguisher(rd)
                .setFlowspec(flowspecList)
                .setPathId(pathId)
                .build())
            .build();
    }

    @Override
    public void extractSpecificFlowspec(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        FlowspecIpv6NlriParserHelper.extractFlowspec(fsType, fsBuilder);
    }

    @Override
    protected void stringSpecificFSNlriType(final FlowspecType value, final StringBuilder buffer) {
        FlowspecIpv6NlriParserHelper.buildFlowspecString(value, buffer);
    }

    @Override
    protected void serializeMpReachNlri(final DestinationType dstType, final ByteBuf byteAggregator) {
        if (dstType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
                .update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv6Case) {
            final DestinationFlowspecL3vpnIpv6 destFlowspec = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml
                    .ns.yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type
                    .DestinationFlowspecL3vpnIpv6Case) dstType).getDestinationFlowspecL3vpnIpv6();
            serializeNlri(
                destFlowspec.getRouteDistinguisher(),
                destFlowspec.getFlowspec(),
                destFlowspec.getPathId(),
                byteAggregator
            );
        }
    }

    @Override
    protected void serializeMpUnreachNlri(final DestinationType dstType, final ByteBuf byteAggregator) {
        if (dstType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
                .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv6Case) {
            final DestinationFlowspecL3vpnIpv6 destFlowspec = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml
                    .ns.yang.bgp.flowspec.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .DestinationFlowspecL3vpnIpv6Case) dstType).getDestinationFlowspecL3vpnIpv6();
            serializeNlri(
                destFlowspec.getRouteDistinguisher(),
                destFlowspec.getFlowspec(),
                destFlowspec.getPathId(),
                byteAggregator
            );
        }
    }
}
