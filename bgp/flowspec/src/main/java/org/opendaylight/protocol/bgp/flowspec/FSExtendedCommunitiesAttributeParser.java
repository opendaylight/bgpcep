/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.impl.message.update.ExtendedCommunitiesAttributeParser;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.redirect.extended.community.RedirectExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.redirect.extended.community.RedirectExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.action.extended.community.TrafficActionExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.action.extended.community.TrafficActionExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.marking.extended.community.TrafficMarkingExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.marking.extended.community.TrafficMarkingExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.rate.extended.community.TrafficRateExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.traffic.rate.extended.community.TrafficRateExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.RedirectExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficActionExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficMarkingExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.update.attributes.extended.communities.extended.community.TrafficRateExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FSExtendedCommunitiesAttributeParser extends ExtendedCommunitiesAttributeParser {

    private static final Logger LOG = LoggerFactory.getLogger(FSExtendedCommunitiesAttributeParser.class);

    private static final short FS_TYPE = 128;

    private static final short TRAFFIC_RATE_SUBTYPE = 6;

    private static final short TRAFFIC_ACTION_SUBTYPE = 7;

    private static final short REDIRECT_SUBTYPE = 8;

    private static final short TRAFFIC_MARKING_SUBTYPE = 9;

    private static final int TRAFFIC_RATE_SIZE = 4;

    private static final int RESERVED = 5;

    private static final int FLAGS_SIZE = 8;

    private static final int SAMPLE_BIT = 6;

    private static final int TERMINAL_BIT = 7;

    public FSExtendedCommunitiesAttributeParser(final ReferenceCache refCache) {
        super(refCache);
    }

    @Override
    public ExtendedCommunities parseExtendedCommunity(final ReferenceCache refCache, final ExtendedCommunitiesBuilder communitiesBuilder, final ByteBuf buffer) throws BGPDocumentedException {
        ExtendedCommunity c = null;
        if (communitiesBuilder.getCommType().equals(FS_TYPE)) {
            switch (communitiesBuilder.getCommSubType()) {
            case TRAFFIC_RATE_SUBTYPE:
                final ShortAsNumber as = new ShortAsNumber((long) buffer.readUnsignedShort());
                final Bandwidth value = new Bandwidth(ByteArray.readBytes(buffer, TRAFFIC_RATE_SIZE));
                c = new TrafficRateExtendedCommunityCaseBuilder().setTrafficRateExtendedCommunity(new TrafficRateExtendedCommunityBuilder().setInformativeAs(as).setLocalAdministrator(value).build()).build();
                break;
            case TRAFFIC_ACTION_SUBTYPE:
                buffer.skipBytes(RESERVED);
                final BitArray flags = BitArray.valueOf(buffer, FLAGS_SIZE);
                final boolean sample = flags.get(SAMPLE_BIT);
                final boolean terminal = flags.get(TERMINAL_BIT);
                c = new TrafficActionExtendedCommunityCaseBuilder().setTrafficActionExtendedCommunity(new TrafficActionExtendedCommunityBuilder().setSample(sample).setTerminalAction(terminal).build()).build();
                break;
            case REDIRECT_SUBTYPE:
                final ShortAsNumber as1 = new ShortAsNumber((long) buffer.readUnsignedShort());
                final byte[] byteValue = ByteArray.readBytes(buffer, TRAFFIC_RATE_SIZE);
                c = new RedirectExtendedCommunityCaseBuilder().setRedirectExtendedCommunity(new RedirectExtendedCommunityBuilder().setGlobalAdministrator(as1).setLocalAdministrator(byteValue).build()).build();
                break;
            case TRAFFIC_MARKING_SUBTYPE:
                buffer.skipBytes(RESERVED);
                final Dscp dscp = new Dscp(buffer.readUnsignedByte());
                c = new TrafficMarkingExtendedCommunityCaseBuilder().setTrafficMarkingExtendedCommunity(new TrafficMarkingExtendedCommunityBuilder().setGlobalAdministrator(dscp).build()).build();
                break;
            default:
                throw new BGPDocumentedException("Could not parse Flowspec Extended Community type: " + communitiesBuilder.getCommSubType(), BGPError.OPT_ATTR_ERROR);
            }
        }
        if (c == null) {
            LOG.debug("Extended community is not from Flowspec, fallback to original communities.");
            return super.parseExtendedCommunity(refCache, communitiesBuilder, buffer);
        }
        return communitiesBuilder.setExtendedCommunity(c).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunities exCommunities, final ByteBuf buffer) {
        final ExtendedCommunity ex = exCommunities.getExtendedCommunity();
        if (ex instanceof TrafficRateExtendedCommunityCase) {
            final TrafficRateExtendedCommunity trafficRate = ((TrafficRateExtendedCommunityCase) ex).getTrafficRateExtendedCommunity();
            ByteBufWriteUtil.writeShort(trafficRate.getInformativeAs().getValue().shortValue(), buffer);
            buffer.writeBytes(trafficRate.getLocalAdministrator().getValue());
        }
        else if (ex instanceof TrafficActionExtendedCommunityCase) {
            final TrafficActionExtendedCommunity trafficAction = ((TrafficActionExtendedCommunityCase) ex).getTrafficActionExtendedCommunity();
            buffer.writeZero(RESERVED);
            final BitArray flags = new BitArray(FLAGS_SIZE);
            flags.set(SAMPLE_BIT, trafficAction.isSample());
            flags.set(TERMINAL_BIT, trafficAction.isTerminalAction());
            flags.toByteBuf(buffer);
        }
        else if (ex instanceof RedirectExtendedCommunityCase) {
            final RedirectExtendedCommunity redirect = ((RedirectExtendedCommunityCase) ex).getRedirectExtendedCommunity();
            ByteBufWriteUtil.writeUnsignedShort(redirect.getGlobalAdministrator().getValue().intValue(), buffer);
            buffer.writeBytes(redirect.getLocalAdministrator());
        }
        else if (ex instanceof TrafficMarkingExtendedCommunityCase) {
            final TrafficMarkingExtendedCommunity trafficMarking = ((TrafficMarkingExtendedCommunityCase) ex).getTrafficMarkingExtendedCommunity();
            buffer.writeZero(RESERVED);
            ByteBufWriteUtil.writeUnsignedByte(trafficMarking.getGlobalAdministrator().getValue().shortValue(), buffer);
        } else {
            super.serializeExtendedCommunity(exCommunities, buffer);
        }
    }
}
