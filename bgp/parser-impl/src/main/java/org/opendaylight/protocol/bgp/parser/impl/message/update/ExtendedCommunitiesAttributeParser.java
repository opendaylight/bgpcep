/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;
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
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.ExtendedCommunitiesBuilder;
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

public class ExtendedCommunitiesAttributeParser implements AttributeParser,AttributeSerializer {

    public static final int TYPE = 16;

    private static final int EXTENDED_COMMUNITY_LENGTH = 6;

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
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) throws BGPDocumentedException {
        final List<ExtendedCommunities> set = new ArrayList<>();
        while (buffer.isReadable()) {
            final ExtendedCommunitiesBuilder exBuilder = new ExtendedCommunitiesBuilder();
            parseHeader(exBuilder, buffer);
            final ExtendedCommunities comm = parseExtendedCommunity(this.refCache, exBuilder, buffer.readSlice(EXTENDED_COMMUNITY_LENGTH));
            set.add(comm);
        }
        builder.setExtendedCommunities(set);
    }

    protected void parseHeader(final ExtendedCommunitiesBuilder exBuilder, final ByteBuf buffer) {
        exBuilder.setCommType(buffer.readUnsignedByte());
        exBuilder.setCommSubType(buffer.readUnsignedByte());
    }

    /**
     * Parse Extended Community according to their type.
     *
     * @param refCache
     * @param comm ExtendedCommunitiesBuilder based on which community type will be new ExtendedCommunity created
     * @param buffer byte array to be parsed
     * @return new Specific Extended Community
     * @throws BGPDocumentedException if the type is not recognized
     */
    public ExtendedCommunities parseExtendedCommunity(final ReferenceCache refCache, final ExtendedCommunitiesBuilder comm, final ByteBuf buffer)
            throws BGPDocumentedException {
        ExtendedCommunity c = null;
        switch (comm.getCommType()) {
        case AS_TYPE_TRANS:
            c = parseAsTransCommunity(comm, buffer);
            break;
        case AS_TYPE_NON_TRANS:
            final ShortAsNumber as = new ShortAsNumber((long) buffer.readUnsignedShort());
            final byte[] value = ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH);
            c = new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
                new AsSpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
            break;
        case ROUTE_TYPE_ONLY:
            c =  parseRouteTypeOnlyCommunity(buffer, comm.getCommSubType());
            break;
        case INET_TYPE_TRANS:
            c = parseInetTypeCommunity(comm, buffer);
            break;
        case INET_TYPE_NON_TRANS:
            c = new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                new Inet4SpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(
                        Ipv4Util.addressForByteBuf(buffer)).setLocalAdministrator(
                        ByteArray.readBytes(buffer, INET_LOCAL_ADMIN_LENGTH)).build()).build();
            break;
        case OPAQUE_TYPE_TRANS:
            c = new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(new OpaqueExtendedCommunityBuilder().setTransitive(false).setValue(ByteArray.readAllBytes(buffer)).build()).build();
            break;
        case OPAQUE_TYPE_NON_TRANS:
            c = new OpaqueExtendedCommunityCaseBuilder().setOpaqueExtendedCommunity(new OpaqueExtendedCommunityBuilder().setTransitive(true).setValue(ByteArray.readAllBytes(buffer)).build()).build();
            break;
        default:
            throw new BGPDocumentedException("Could not parse Extended Community type: " + comm.getCommType(), BGPError.OPT_ATTR_ERROR);
        }
        return comm.setExtendedCommunity(c).build();
    }

    private static ExtendedCommunity parseRouteTypeOnlyCommunity(final ByteBuf buffer, final int subtype) throws BGPDocumentedException {
        final ShortAsNumber as = new ShortAsNumber((long) buffer.readUnsignedShort());
        final byte[] value = ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH);
        if (subtype == ROUTE_TARGET_SUBTYPE) {
            return new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
        } else if (subtype == ROUTE_ORIGIN_SUBTYPE) {
            return new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
        } else {
            throw new BGPDocumentedException("Could not parse Extended Community subtype: " + subtype, BGPError.OPT_ATTR_ERROR);
        }
    }

    private static ExtendedCommunity parseAsTransCommunity(final ExtendedCommunitiesBuilder comm, final ByteBuf buffer) {
        final ShortAsNumber as = new ShortAsNumber((long) buffer.readUnsignedShort());
        final byte[] value = ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH);
        if (comm.getCommSubType() == ROUTE_TARGET_SUBTYPE) {
            return new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
                new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
        }
        if (comm.getCommSubType() == ROUTE_ORIGIN_SUBTYPE) {
            return new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
                new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
        }
        return new AsSpecificExtendedCommunityCaseBuilder().setAsSpecificExtendedCommunity(
            new AsSpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(as).setLocalAdministrator(value).build()).build();
    }

    private static ExtendedCommunity parseInetTypeCommunity(final ExtendedCommunitiesBuilder comm, final ByteBuf buffer) {
        if (comm.getCommSubType() == ROUTE_TARGET_SUBTYPE) {
            return new RouteTargetExtendedCommunityCaseBuilder().setRouteTargetExtendedCommunity(
                new RouteTargetExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber((long) buffer.readUnsignedShort()))
                    .setLocalAdministrator(ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH)).build()).build();
        }
        if (comm.getCommSubType() == ROUTE_ORIGIN_SUBTYPE) {
            return new RouteOriginExtendedCommunityCaseBuilder().setRouteOriginExtendedCommunity(
                new RouteOriginExtendedCommunityBuilder().setGlobalAdministrator(new ShortAsNumber((long) buffer.readUnsignedShort()))
                    .setLocalAdministrator(ByteArray.readBytes(buffer, AS_LOCAL_ADMIN_LENGTH)).build()).build();
        }
        return new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
            new Inet4SpecificExtendedCommunityBuilder().setTransitive(false).setGlobalAdministrator(
                    Ipv4Util.addressForByteBuf(buffer)).setLocalAdministrator(
                    ByteArray.readBytes(buffer, INET_LOCAL_ADMIN_LENGTH)).build()).build();
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final List<ExtendedCommunities> communitiesList = ((Attributes) attribute).getExtendedCommunities();
        if (communitiesList == null) {
            return;
        }
        final ByteBuf extendedCommunitiesBuffer = Unpooled.buffer();
        for (final ExtendedCommunities extendedCommunities : communitiesList) {
            final ByteBuf ecBuffer = Unpooled.buffer(EXTENDED_COMMUNITY_LENGTH);
            serializeExtendedCommunity(extendedCommunities, ecBuffer);
            //check if extended community value was serialized
            if (ecBuffer.readableBytes() >= EXTENDED_COMMUNITY_LENGTH) {
                serializeHeader(extendedCommunities, extendedCommunitiesBuffer);
                extendedCommunitiesBuffer.writeBytes(ecBuffer);
            }
        }
        //check if some extended communities were serialized
        if (extendedCommunitiesBuffer.readableBytes() > 0) {
            AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL | AttributeUtil.TRANSITIVE, TYPE, extendedCommunitiesBuffer, byteAggregator);
        }
    }

    protected void serializeHeader(final ExtendedCommunities extendedCommunities, final ByteBuf extendedCommunitiesBuffer) {
        ByteBufWriteUtil.writeUnsignedByte(extendedCommunities.getCommType(), extendedCommunitiesBuffer);
        ByteBufWriteUtil.writeUnsignedByte(extendedCommunities.getCommSubType(), extendedCommunitiesBuffer);
    }

    public void serializeExtendedCommunity(final ExtendedCommunities extendedCommunities, final ByteBuf buffer) {
        final ExtendedCommunity ex = extendedCommunities.getExtendedCommunity();
        if (ex instanceof AsSpecificExtendedCommunityCase) {
            final AsSpecificExtendedCommunity asSpecificExtendedCommunity = ((AsSpecificExtendedCommunityCase) ex).getAsSpecificExtendedCommunity();
            ByteBufWriteUtil.writeUnsignedShort(asSpecificExtendedCommunity.getGlobalAdministrator().getValue().intValue(), buffer);
            buffer.writeBytes(asSpecificExtendedCommunity.getLocalAdministrator());
        } else if (ex instanceof Inet4SpecificExtendedCommunityCase) {
            final Inet4SpecificExtendedCommunity inet4SpecificExtendedCommunity = ((Inet4SpecificExtendedCommunityCase) ex).getInet4SpecificExtendedCommunity();
            ByteBufWriteUtil.writeIpv4Address(inet4SpecificExtendedCommunity.getGlobalAdministrator(), buffer);
            buffer.writeBytes(inet4SpecificExtendedCommunity.getLocalAdministrator());
        } else if (ex instanceof OpaqueExtendedCommunityCase) {
            final OpaqueExtendedCommunity opaqueExtendedCommunity = ((OpaqueExtendedCommunityCase) ex).getOpaqueExtendedCommunity();
            buffer.writeBytes(opaqueExtendedCommunity.getValue());
        } else if (ex instanceof RouteTargetExtendedCommunityCase) {
            final RouteTargetExtendedCommunity routeTarget = ((RouteTargetExtendedCommunityCase) ex).getRouteTargetExtendedCommunity();
            ByteBufWriteUtil.writeUnsignedShort(routeTarget.getGlobalAdministrator().getValue().intValue(), buffer);
            buffer.writeBytes(routeTarget.getLocalAdministrator());
        } else if (ex instanceof RouteOriginExtendedCommunityCase) {
            final RouteOriginExtendedCommunity routeOriginExtendedCommunity = ((RouteOriginExtendedCommunityCase) ex).getRouteOriginExtendedCommunity();
            ByteBufWriteUtil.writeUnsignedShort(routeOriginExtendedCommunity.getGlobalAdministrator().getValue().intValue(), buffer);
            buffer.writeBytes(routeOriginExtendedCommunity.getLocalAdministrator());
        }
    }
}
