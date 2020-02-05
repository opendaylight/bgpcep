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
import org.opendaylight.protocol.bgp.flowspec.ipv6.FlowspecIpv6NlriParserHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.ipv6.DestinationFlowspecIpv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.flowspec.destination.ipv6.DestinationFlowspecIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.destination.DestinationType;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

public final class SimpleFlowspecIpv6NlriParser extends AbstractFlowspecNlriParser {

    public SimpleFlowspecIpv6NlriParser(final SimpleFlowspecTypeRegistry flowspecTypeRegistry) {
        super(flowspecTypeRegistry);
    }

    @Override
    public DestinationType createWithdrawnDestinationType(final Object[] nlriFields, final PathId pathId) {
        final List<Flowspec> flowspecList = (List<Flowspec>) nlriFields[0];
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
                .attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6CaseBuilder()
            .setDestinationFlowspecIpv6(
                new DestinationFlowspecIpv6Builder()
                    .setFlowspec(flowspecList)
                    .setPathId(pathId)
                    .build()
            ).build();
    }

    @Override
    public DestinationType createAdvertizedRoutesDestinationType(final Object[] nlriFields, final PathId pathId) {
        final List<Flowspec> flowspecList = (List<Flowspec>) nlriFields[0];
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update
                .attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6CaseBuilder()
            .setDestinationFlowspecIpv6(
                new DestinationFlowspecIpv6Builder()
                    .setFlowspec(flowspecList)
                    .setPathId(pathId)
                    .build()
            ).build();
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
                .update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case) {
            final DestinationFlowspecIpv6 destFlowspec = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                    .yang.bgp.flowspec.rev200120.update.attributes.mp.reach.nlri.advertized.routes.destination.type
                    .DestinationFlowspecIpv6Case) dstType).getDestinationFlowspecIpv6();
            serializeNlri(new Object[] {destFlowspec.getFlowspec()}, destFlowspec.getPathId(), byteAggregator);
        }
    }

    @Override
    protected void serializeMpUnreachNlri(final DestinationType dstType, final ByteBuf byteAggregator) {
        if (dstType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120
                .update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6Case) {
            final DestinationFlowspecIpv6 destFlowspec = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                    .yang.bgp.flowspec.rev200120.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type
                    .DestinationFlowspecIpv6Case) dstType).getDestinationFlowspecIpv6();
            serializeNlri(new Object[] {destFlowspec.getFlowspec()}, destFlowspec.getPathId(), byteAggregator);
        }
    }
}

