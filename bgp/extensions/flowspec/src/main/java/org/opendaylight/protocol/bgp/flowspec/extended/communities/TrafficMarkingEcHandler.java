/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec.extended.communities;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.traffic.marking.extended.community.TrafficMarkingExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.traffic.marking.extended.community.TrafficMarkingExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class TrafficMarkingEcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {
    private static final int TYPE = 128;
    private static final int SUBTYPE = 9;
    private static final int RESERVED = 5;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
            .flowspec.rev180329.TrafficMarkingExtendedCommunity,
                "The extended community %s is not TrafficMarkingExtendedCommunity type.", extendedCommunity);
        final TrafficMarkingExtendedCommunity trafficMarking = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params
                .xml.ns.yang.bgp.flowspec.rev200120.TrafficMarkingExtendedCommunity) extendedCommunity)
                .getTrafficMarkingExtendedCommunity();
        byteAggregator.writeZero(RESERVED);
        ByteBufUtils.write(byteAggregator, trafficMarking.getGlobalAdministrator().getValue());
    }

    @Override
    public int getType(final boolean isTransitive) {
        //traffic-rate is transitive only
        return TYPE;
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        buffer.skipBytes(RESERVED);
        return new TrafficMarkingExtendedCommunityCaseBuilder()
            .setTrafficMarkingExtendedCommunity(new TrafficMarkingExtendedCommunityBuilder()
                .setGlobalAdministrator(new Dscp(ByteBufUtils.readUint8(buffer)))
                .build())
            .build();
    }
}
