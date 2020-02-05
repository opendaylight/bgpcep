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
import org.opendaylight.protocol.bgp.flowspec.ipv4.FlowspecIpv4NlriParserHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.ipv4.DestinationFlowspecIpv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.ipv4.DestinationFlowspecIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

public final class SimpleFlowspecIpv4NlriParser extends AbstractFlowspecNlriParser {

    public SimpleFlowspecIpv4NlriParser(final SimpleFlowspecTypeRegistry flowspecTypeRegistry) {
        super(flowspecTypeRegistry);
    }

    @Override
    public DestinationType createWithdrawnDestinationType(final Object[] nlriFields, final PathId pathId) {
        final List<Flowspec> flowspecList = (List<Flowspec>) nlriFields[0];
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCaseBuilder()
            .setDestinationFlowspecIpv4(
                new DestinationFlowspecIpv4Builder()
                    .setFlowspec(flowspecList)
                    .setPathId(pathId)
                    .build()
            ).build();
    }

    @Override
    public DestinationType createAdvertizedRoutesDestinationType(final Object[] nlriFields, final PathId pathId) {
        final List<Flowspec> flowspecList = (List<Flowspec>) nlriFields[0];
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
                .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCaseBuilder()
                .setDestinationFlowspecIpv4(new DestinationFlowspecIpv4Builder()
                    .setFlowspec(flowspecList)
                    .setPathId(pathId)
                    .build()).build();
    }

    @Override
    protected void serializeMpReachNlri(final DestinationType dstType, final ByteBuf byteAggregator) {
        if (dstType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
                .update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecCase) {
            final DestinationFlowspecIpv4 destFlowspec = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                    .yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type
                    .DestinationFlowspecCase) dstType).getDestinationFlowspecIpv4();
            serializeNlri(new Object[] {destFlowspec.getFlowspec()}, destFlowspec.getPathId(), byteAggregator);
        }
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
    protected void serializeMpUnreachNlri(final DestinationType dstType, final ByteBuf byteAggregator) {
        if (dstType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
                .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecCase) {
            final DestinationFlowspecIpv4 destFlowspec = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                    .yang.bgp.flowspec.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .DestinationFlowspecCase) dstType).getDestinationFlowspecIpv4();
            serializeNlri(new Object[] {destFlowspec.getFlowspec()}, destFlowspec.getPathId(), byteAggregator);
        }
    }
}

