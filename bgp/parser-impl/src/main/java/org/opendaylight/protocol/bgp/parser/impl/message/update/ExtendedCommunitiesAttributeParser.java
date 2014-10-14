/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.AsSpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.AsSpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.OpaqueExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.OpaqueExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as.specific.extended.community._case.AsSpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as.specific.extended.community._case.AsSpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.opaque.extended.community._case.OpaqueExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.opaque.extended.community._case.OpaqueExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.extended.community._case.RouteTargetExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.extended.community._case.RouteTargetExtendedCommunityBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class ExtendedCommunitiesAttributeParser implements AttributeParser,AttributeSerializer {

    public static final int TYPE = 16;

    private static final int EXTENDED_COMMUNITY_LENGTH = 8;

    private static final int AS_LOCAL_ADMIN_LENGTH = 4;

    private static final int INET_LOCAL_ADMIN_LENGTH = 2;

    private static final short AS_TYPE_TRANS = 0;

    private static final short AS_TYPE_NON_TRANS = 40;

    private static final short INET_TYPE_TRANS = 1;

    private static final short INET_TYPE_NON_TRANS = 41;

    private static final short OPAQUE_TYPE_TRANS = 3;

    private static final short OPAQUE_TYPE_NON_TRANS = 43;

    private static final short ROUTE_TYPE_ONLY = 2;

    private static final short ROUTE_TARGET_SUBTYPE = 2;

    private static final short ROUTE_ORIGIN_SUBTYPE = 3;

    private final ReferenceCache refCache;

    public ExtendedCommunitiesAttributeParser(final ReferenceCache refCache) {
        this.refCache = Preconditions.checkNotNull(refCache);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) throws BGPDocumentedException {
        final List<ExtendedCommunities> set = new ArrayList<>();
        while (buffer.isReadable()) {
            final ExtendedCommunities comm = parseExtendedCommunity(this.refCache, buffer.slice(buffer.readerIndex(), EXTENDED_COMMUNITY_LENGTH));
            buffer.skipBytes(EXTENDED_COMMUNITY_LENGTH);
            set.add(comm);
        }
        builder.setExtendedCommunities(set);
    }

    /**
     * Parse Extended Community according to their type.
     *
     * @param bytes byte array to be parsed
     * @return new Specific Extended Community
     * @throws BGPDocumentedException if the type is not recognized
     */
    @VisibleForTesting
    public static ExtendedCommunities parseExtendedCommunity(final ReferenceCache refCache, final ByteBuf buffer)
            throws BGPDocumentedException {
        final ExtendedCommunitiesBuilder comm = new ExtendedCommunitiesBuilder();
        comm.setCommType((short) UnsignedBytes.toInt(buffer.readByte()));
        comm.setCommSubType((short) UnsignedBytes.toInt(buffer.readByte()));
        ExtendedCommunity c = null;
        switch (comm.getCommType()) {
        case AS_TYPE_TRANS:
            ShortAsNumber as = new ShortAsNumber((long) buffer.readUnsignedShort());
            byte[] value = ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH);
            if (comm.getCommSubType() == ROUTE_TARGET_SUBTYPE) {
                c = new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
                    new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
            } else if (comm.getCommSubType() == ROUTE_ORIGIN_SUBTYPE) {
                c = new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
                    new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
            } else {
                c = new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
                    new AsSpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
            }
            break;
        case AS_TYPE_NON_TRANS:
            as = new ShortAsNumber((long) buffer.readUnsignedShort());
            value = ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH);
            c = new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
                new AsSpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
            break;
        case ROUTE_TYPE_ONLY:
            as = new ShortAsNumber((long) buffer.readUnsignedShort());
            value = ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH);
            if (comm.getCommSubType() == ROUTE_TARGET_SUBTYPE) {
                c = new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
                    new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
            } else if (comm.getCommSubType() == ROUTE_ORIGIN_SUBTYPE) {
                c = new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
                    new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
            } else {
                throw new BGPDocumentedException("Could not parse Extended Community subtype: " + comm.getCommSubType(), BGPError.OPT_ATTR_ERROR);
            }
            break;
        case INET_TYPE_TRANS:
            if (comm.getCommSubType() == ROUTE_TARGET_SUBTYPE) {
                c = new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
                    new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber((long) buffer.readUnsignedShort()))
                        .setLocalAdministrator(ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH)).build()).build();
            } else if (comm.getCommSubType() == ROUTE_ORIGIN_SUBTYPE) {
                c = new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
                    new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber((long) buffer.readUnsignedShort()))
                        .setLocalAdministrator(ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH)).build()).build();
            } else {
                c = new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                    new Inet4SpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(
                            Ipv4Util.addressForBytes(ByteArray.readBytes(buffer, Ipv4Util.IP4_LENGTH))).setLocalAdministrator(
                            ByteArray.readBytes(buffer, INET_LOCAL_ADMIN_LENGTH)).build()).build();
            }
            break;
        case INET_TYPE_NON_TRANS:
            c = new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                new Inet4SpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(
                        Ipv4Util.addressForBytes(ByteArray.readBytes(buffer, Ipv4Util.IP4_LENGTH))).setLocalAdministrator(
                        ByteArray.readBytes(buffer, INET_LOCAL_ADMIN_LENGTH)).build()).build();
            break;
        case OPAQUE_TYPE_TRANS:
            c = new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(
                new OpaqueExtendedCommunityBuilder().setTransitive(false).setValue(ByteArray.readAllBytes(buffer)).build()).build();
            break;
        case OPAQUE_TYPE_NON_TRANS:
            c = new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(
                new OpaqueExtendedCommunityBuilder().setTransitive(true).setValue(ByteArray.readAllBytes(buffer)).build()).build();
            break;
        default:
            throw new BGPDocumentedException("Could not parse Extended Community type: " + comm.getCommType(), BGPError.OPT_ATTR_ERROR);
        }
        return comm.setExtendedCommunity(c).build();
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof PathAttributes, "Attribute parameter is not a PathAttribute object.");
        final List<ExtendedCommunities> communitiesList = ((PathAttributes) attribute).getExtendedCommunities();
        if (communitiesList == null || communitiesList.isEmpty()) {
            return;
        }
        final ByteBuf extendedCommunitiesBuffer = Unpooled.buffer();
        for (final ExtendedCommunities extendedCommunities : communitiesList) {
            serializeExtendedCommunity(extendedCommunities, extendedCommunitiesBuffer);
            AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL | AttributeUtil.TRANSITIVE, TYPE, extendedCommunitiesBuffer, byteAggregator);
        }
    }

    @VisibleForTesting
    public static void serializeExtendedCommunity(final ExtendedCommunities extendedCommunities, final ByteBuf extendedCommunitiesBuffer) {
        if (extendedCommunities.getCommType() != null) {
            extendedCommunitiesBuffer.writeByte(extendedCommunities.getCommType());
        }
        if (extendedCommunities.getCommSubType() != null) {
            extendedCommunitiesBuffer.writeByte(extendedCommunities.getCommSubType());
        }
        final ExtendedCommunity ex = extendedCommunities.getExtendedCommunity();
        if (ex instanceof AsSpecificExtendedCommunityCase) {
            final AsSpecificExtendedCommunity asSpecificExtendedCommunity = ((AsSpecificExtendedCommunityCase) ex).getAsSpecificExtendedCommunity();
            extendedCommunitiesBuffer.writeShort(asSpecificExtendedCommunity.getGlobalAdministrator().getValue().shortValue());
            extendedCommunitiesBuffer.writeBytes(asSpecificExtendedCommunity.getLocalAdministrator());
        }
        else if (ex instanceof Inet4SpecificExtendedCommunityCase) {
            final Inet4SpecificExtendedCommunity inet4SpecificExtendedCommunity = ((Inet4SpecificExtendedCommunityCase) ex).getInet4SpecificExtendedCommunity();
            extendedCommunitiesBuffer.writeBytes(Ipv4Util.bytesForAddress(inet4SpecificExtendedCommunity.getGlobalAdministrator()));
            extendedCommunitiesBuffer.writeBytes(inet4SpecificExtendedCommunity.getLocalAdministrator());
        }
        else if (ex instanceof OpaqueExtendedCommunityCase) {
            final OpaqueExtendedCommunity opaqueExtendedCommunity = ((OpaqueExtendedCommunityCase) ex).getOpaqueExtendedCommunity();
            extendedCommunitiesBuffer.writeBytes(opaqueExtendedCommunity.getValue());
        }
        else if (ex instanceof RouteTargetExtendedCommunityCase) {
            final RouteTargetExtendedCommunity routeTargetExtendedCommunity = ((RouteTargetExtendedCommunityCase) ex).getRouteTargetExtendedCommunity();
            extendedCommunitiesBuffer.writeShort(routeTargetExtendedCommunity.getGlobalAdministrator().getValue().shortValue());
            extendedCommunitiesBuffer.writeBytes(routeTargetExtendedCommunity.getLocalAdministrator());
        }
        else if (ex instanceof RouteOriginExtendedCommunityCase) {
            final RouteOriginExtendedCommunity routeOriginExtendedCommunity = ((RouteOriginExtendedCommunityCase) ex).getRouteOriginExtendedCommunity();
            extendedCommunitiesBuffer.writeShort(routeOriginExtendedCommunity.getGlobalAdministrator().getValue().shortValue());
            extendedCommunitiesBuffer.writeBytes(routeOriginExtendedCommunity.getLocalAdministrator());
        }
    }
}
