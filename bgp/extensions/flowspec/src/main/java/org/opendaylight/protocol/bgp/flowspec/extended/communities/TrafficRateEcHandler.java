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
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.traffic.rate.extended.community.TrafficRateExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.traffic.rate.extended.community.TrafficRateExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yangtools.yang.common.Uint32;

public class TrafficRateEcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {

    private static final int TYPE = 128;

    private static final int SUBTYPE = 6;

    private static final int TRAFFIC_RATE_SIZE = 4;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
            .flowspec.rev180329.TrafficRateExtendedCommunity,
                "The extended community %s is not TrafficRateExtendedCommunity type.", extendedCommunity);
        final TrafficRateExtendedCommunity trafficRate = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bgp.flowspec.rev200120.TrafficRateExtendedCommunity) extendedCommunity)
                .getTrafficRateExtendedCommunity();
        byteAggregator.writeShort(trafficRate.getInformativeAs().getValue().intValue());
        byteAggregator.writeBytes(trafficRate.getLocalAdministrator().getValue());
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
        return new TrafficRateExtendedCommunityCaseBuilder()
                .setTrafficRateExtendedCommunity(new TrafficRateExtendedCommunityBuilder()
                    .setInformativeAs(new ShortAsNumber(Uint32.valueOf(buffer.readUnsignedShort())))
                    .setLocalAdministrator(new Bandwidth(ByteArray.readBytes(buffer, TRAFFIC_RATE_SIZE)))
                    .build())
                .build();
    }

}
