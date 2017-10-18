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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.protocol.bgp.flowspec.ipv6.FlowspecIpv6NlriParserHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.FlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.flowspec.FlowspecType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.ipv6.DestinationFlowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.flowspec.destination.ipv6.DestinationFlowspecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev171207.destination.DestinationType;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;

public final class SimpleFlowspecIpv6NlriParser extends AbstractFlowspecNlriParser {

    public SimpleFlowspecIpv6NlriParser(SimpleFlowspecTypeRegistry flowspecTypeRegistry) {
        super(flowspecTypeRegistry);
    }

    @Override
    public DestinationType createWithdrawnDestinationType(@Nonnull final Object[] nlriFields, @Nullable final PathId pathId) {
        final List<Flowspec> flowspecList = (List<Flowspec>) nlriFields[0];
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6CaseBuilder()
            .setDestinationFlowspec(
                new DestinationFlowspecBuilder()
                    .setFlowspec(flowspecList)
                    .setPathId(pathId)
                    .build()
            ).build();
    }

    @Override
    public DestinationType createAdvertizedRoutesDestinationType(@Nonnull final Object[] nlriFields, @Nullable final PathId pathId) {
        final List<Flowspec> flowspecList = (List<Flowspec>) nlriFields[0];
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6CaseBuilder()
            .setDestinationFlowspec(
                new DestinationFlowspecBuilder()
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
        if (dstType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case) {
            final DestinationFlowspec destFlowspec = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.reach.nlri.advertized.routes.destination.type.DestinationFlowspecIpv6Case) dstType)
                .getDestinationFlowspec();
            serializeNlri(new Object[] {destFlowspec.getFlowspec()}, destFlowspec.getPathId(), byteAggregator);
        }
    }

    @Override
    protected void serializeMpUnreachNlri(final DestinationType dstType, final ByteBuf byteAggregator) {
        if (dstType instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6Case) {
            final DestinationFlowspec destFlowspec = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.mp.unreach.nlri.withdrawn.routes.destination.type.DestinationFlowspecIpv6Case) dstType)
                .getDestinationFlowspec();
            serializeNlri(new Object[] {destFlowspec.getFlowspec()}, destFlowspec.getPathId(), byteAggregator);
        }
    }
}

