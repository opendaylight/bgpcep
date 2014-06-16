/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.Ipv4Util;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.AsSpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.OpaqueExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteOriginExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.RouteTargetExtendedCommunityCase;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class ExtendedCommunitiesAttributeParser implements AttributeParser,AttributeSerializer {

    public static final int TYPE = 16;

    private final ReferenceCache refCache;

    public ExtendedCommunitiesAttributeParser(final ReferenceCache refCache) {
        this.refCache = Preconditions.checkNotNull(refCache);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) throws BGPDocumentedException {
        final List<ExtendedCommunities> set = Lists.newArrayList();
        while (buffer.isReadable()) {
            final ExtendedCommunities comm = CommunitiesParser.parseExtendedCommunity(this.refCache, buffer.slice(buffer.readerIndex(), CommunitiesParser.EXTENDED_COMMUNITY_LENGTH));
            buffer.skipBytes(CommunitiesParser.EXTENDED_COMMUNITY_LENGTH);
            set.add(comm);
        }
        builder.setExtendedCommunities(set);
    }

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        PathAttributes pathAttributes = (PathAttributes) attribute;
        List<ExtendedCommunities> communitiesList = pathAttributes.getExtendedCommunities();
        if (communitiesList == null) {
            return;
        }
        for (ExtendedCommunities extendedCommunities : communitiesList) {
            if (extendedCommunities.getCommSubType() != null) {
                byteAggregator.writeShort(extendedCommunities.getCommSubType());
            }
            if (extendedCommunities.getExtendedCommunity() instanceof AsSpecificExtendedCommunityCase) {
                AsSpecificExtendedCommunityCase asSpecificExtendedCommunity = (AsSpecificExtendedCommunityCase) extendedCommunities.getExtendedCommunity();

                //TODO resolve types correctly
                byteAggregator.writeByte(0);
                byteAggregator.writeByte(1);

                byteAggregator.writeShort(asSpecificExtendedCommunity.getAsSpecificExtendedCommunity().getGlobalAdministrator().getValue().shortValue());
                byteAggregator.writeBytes(asSpecificExtendedCommunity.getAsSpecificExtendedCommunity().getLocalAdministrator());
            }
            if (extendedCommunities.getExtendedCommunity() instanceof Inet4SpecificExtendedCommunityCase) {
                Inet4SpecificExtendedCommunityCase inet4SpecificExtendedCommunity = (Inet4SpecificExtendedCommunityCase) extendedCommunities.getExtendedCommunity();

                //TODO resolve types correctly
                byteAggregator.writeByte(1);
                byteAggregator.writeByte(4);

                byteAggregator.writeBytes(Ipv4Util.bytesForAddress(inet4SpecificExtendedCommunity.getInet4SpecificExtendedCommunity().getGlobalAdministrator()));
                byteAggregator.writeBytes(inet4SpecificExtendedCommunity.getInet4SpecificExtendedCommunity().getLocalAdministrator());
            }
            if (extendedCommunities.getExtendedCommunity() instanceof OpaqueExtendedCommunityCase) {
                OpaqueExtendedCommunityCase opaqueExtendedCommunity = (OpaqueExtendedCommunityCase) extendedCommunities.getExtendedCommunity();
                //TODO resolve types correctly
                byteAggregator.writeByte(3);
                byteAggregator.writeByte(4);

                byteAggregator.writeBytes(opaqueExtendedCommunity.getOpaqueExtendedCommunity().getValue());
            }
            if (extendedCommunities.getExtendedCommunity() instanceof RouteTargetExtendedCommunityCase) {
                RouteTargetExtendedCommunityCase routeTargetExtendedCommunity = (RouteTargetExtendedCommunityCase) extendedCommunities.getExtendedCommunity();
                //TODO how to determine, which numbering space global administrator number is originated from
                byteAggregator.writeByte(0);
                byteAggregator.writeByte(2);

                byteAggregator.writeShort(routeTargetExtendedCommunity.getRouteTargetExtendedCommunity().getGlobalAdministrator().getValue().shortValue());
                byteAggregator.writeBytes(routeTargetExtendedCommunity.getRouteTargetExtendedCommunity().getLocalAdministrator());
            }
            if (extendedCommunities.getExtendedCommunity() instanceof RouteOriginExtendedCommunityCase) {
                RouteOriginExtendedCommunityCase routeOriginExtendedCommunity = (RouteOriginExtendedCommunityCase) extendedCommunities.getExtendedCommunity();
                //TODO how to determine, which numbering space global administrator number is originated from
                byteAggregator.writeByte(2);
                byteAggregator.writeByte(3);
                byteAggregator.writeShort(routeOriginExtendedCommunity.getRouteOriginExtendedCommunity().getGlobalAdministrator().getValue().shortValue());
                byteAggregator.writeBytes(routeOriginExtendedCommunity.getRouteOriginExtendedCommunity().getLocalAdministrator());
            }
        }
    }
}
