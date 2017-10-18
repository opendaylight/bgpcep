/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.flowspec.extended.communities;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.traffic.rate.extended.community.TrafficRateExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.traffic.rate.extended.community.TrafficRateExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;

public class TrafficRateEcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {

    private static final int TYPE = 128;

    private static final int SUBTYPE = 6;

    private static final int TRAFFIC_RATE_SIZE = 4;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.TrafficRateExtendedCommunity,
                "The extended community %s is not TrafficRateExtendedCommunity type.", extendedCommunity);
        final TrafficRateExtendedCommunity trafficRate = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.TrafficRateExtendedCommunity) extendedCommunity).getTrafficRateExtendedCommunity();
        ByteBufWriteUtil.writeShort(trafficRate.getInformativeAs().getValue().shortValue(), byteAggregator);
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
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        final ShortAsNumber as = new ShortAsNumber((long) buffer.readUnsignedShort());
        final Bandwidth value = new Bandwidth(ByteArray.readBytes(buffer, TRAFFIC_RATE_SIZE));
        return new TrafficRateExtendedCommunityCaseBuilder().setTrafficRateExtendedCommunity(
                new TrafficRateExtendedCommunityBuilder()
                    .setInformativeAs(as)
                    .setLocalAdministrator(value)
                    .build()).build();
    }

}
