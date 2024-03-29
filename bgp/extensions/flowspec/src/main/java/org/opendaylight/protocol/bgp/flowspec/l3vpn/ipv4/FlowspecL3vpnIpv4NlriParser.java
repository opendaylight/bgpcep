/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.AFI;
import org.opendaylight.protocol.bgp.flowspec.FlowspecTypeRegistries.SAFI;
import org.opendaylight.protocol.bgp.flowspec.ipv4.FlowspecIpv4NlriParserHelper;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.AbstractFlowspecL3vpnNlriParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.l3vpn.destination.ipv4.DestinationFlowspecL3vpnIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

/**
 * L3 VPN IPv4 flowspec parser.
 *
 * @author Kevin Wang
 */
public final class FlowspecL3vpnIpv4NlriParser extends AbstractFlowspecL3vpnNlriParser {
    public FlowspecL3vpnIpv4NlriParser(final SAFI safi) {
        super(FlowspecTypeRegistries.getFlowspecTypeRegistry(AFI.IPV4, safi));
    }

    @Override
    protected DestinationType createAdvertizedRoutesDestinationType(final RouteDistinguisher rd,
            final List<Flowspec> flowspecList, final PathId pathId) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
            .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv4CaseBuilder()
            .setDestinationFlowspecL3vpnIpv4(new DestinationFlowspecL3vpnIpv4Builder()
                .setRouteDistinguisher(rd)
                .setFlowspec(flowspecList)
                .setPathId(pathId)
                .build()).build();
    }

    @Override
    protected DestinationType createWithdrawnDestinationType(final RouteDistinguisher rd,
            final List<Flowspec> flowspecList, final PathId pathId) {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
            .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv4CaseBuilder()
            .setDestinationFlowspecL3vpnIpv4(new DestinationFlowspecL3vpnIpv4Builder()
                .setRouteDistinguisher(rd)
                .setFlowspec(flowspecList)
                .setPathId(pathId)
                .build())
            .build();
    }

    @Override
    public void extractSpecificFlowspec(final ChoiceNode fsType, final FlowspecBuilder fsBuilder) {
        FlowspecIpv4NlriParserHelper.extractFlowspec(fsType, fsBuilder);
    }

    @Override
    protected void stringSpecificFSNlriType(final FlowspecType value, final StringBuilder buffer) {
        FlowspecIpv4NlriParserHelper.buildFlowspecString(value, buffer);
    }

    @Override
    protected void serializeMpReachNlri(final DestinationType dstType, final ByteBuf byteAggregator) {
        if (dstType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
                .update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecL3vpnIpv4Case) {
            final DestinationFlowspecL3vpnIpv4 destFlowspec = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml
                    .ns.yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type
                    .DestinationFlowspecL3vpnIpv4Case) dstType).getDestinationFlowspecL3vpnIpv4();
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
                .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecL3vpnIpv4Case) {
            final DestinationFlowspecL3vpnIpv4 destFlowspec = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml
                    .ns.yang.bgp.flowspec.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .DestinationFlowspecL3vpnIpv4Case) dstType).getDestinationFlowspecL3vpnIpv4();
            serializeNlri(
                destFlowspec.getRouteDistinguisher(),
                destFlowspec.getFlowspec(),
                destFlowspec.getPathId(),
                byteAggregator
            );
        }
    }
}
