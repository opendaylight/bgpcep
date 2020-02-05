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
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.traffic.action.extended.community.TrafficActionExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.traffic.action.extended.community.TrafficActionExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev200120.update.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;

public class TrafficActionEcHandler implements ExtendedCommunityParser, ExtendedCommunitySerializer {

    private static final int TYPE = 128;

    private static final int SUBTYPE = 7;

    private static final int RESERVED = 5;

    private static final int FLAGS_SIZE = 8;

    private static final int SAMPLE_BIT = 6;

    private static final int TERMINAL_BIT = 7;

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        checkArgument(extendedCommunity instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp
            .flowspec.rev180329.TrafficActionExtendedCommunity,
                "The extended community %s is not TrafficActionExtendedCommunityCase type.", extendedCommunity);
        final TrafficActionExtendedCommunity trafficAction = ((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml
                .ns.yang.bgp.flowspec.rev200120.TrafficActionExtendedCommunity) extendedCommunity)
                .getTrafficActionExtendedCommunity();
        byteAggregator.writeZero(RESERVED);
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(SAMPLE_BIT, trafficAction.isSample());
        flags.set(TERMINAL_BIT, trafficAction.isTerminalAction());
        flags.toByteBuf(byteAggregator);
    }

    @Override
    public int getType(final boolean isTransitive) {
        //traffic-action is transitive only
        return TYPE;
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        buffer.skipBytes(RESERVED);
        final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
        final boolean sample = flags.get(SAMPLE_BIT);
        final boolean terminal = flags.get(TERMINAL_BIT);
        return new TrafficActionExtendedCommunityCaseBuilder().setTrafficActionExtendedCommunity(
                new TrafficActionExtendedCommunityBuilder()
                    .setSample(sample)
                    .setTerminalAction(terminal)
                    .build()).build();
    }
}
