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
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.traffic.marking.extended.community.TrafficMarkingExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.traffic.marking.extended.community.TrafficMarkingExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.update.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

public class TrafficMarkingEcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {

    private static final int TYPE = 128;

    private static final int SUBTYPE = 9;

    private static final int RESERVED = 5;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.TrafficMarkingExtendedCommunity,
                "The extended community %s is not TrafficMarkingExtendedCommunity type.", extendedCommunity);
        final TrafficMarkingExtendedCommunity trafficMarking = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev171207.TrafficMarkingExtendedCommunity) extendedCommunity).getTrafficMarkingExtendedCommunity();
        byteAggregator.writeZero(RESERVED);
        ByteBufWriteUtil.writeUnsignedByte(trafficMarking.getGlobalAdministrator().getValue(), byteAggregator);
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
        buffer.skipBytes(RESERVED);
        final Dscp dscp = new Dscp(buffer.readUnsignedByte());
        return new TrafficMarkingExtendedCommunityCaseBuilder().setTrafficMarkingExtendedCommunity(
                new TrafficMarkingExtendedCommunityBuilder()
                    .setGlobalAdministrator(dscp).build()).build();
    }

}
