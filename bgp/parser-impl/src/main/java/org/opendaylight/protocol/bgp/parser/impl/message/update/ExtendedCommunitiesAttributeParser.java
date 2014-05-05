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
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.as.specific.extended.community._case.AsSpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.opaque.extended.community._case.OpaqueExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.origin.extended.community._case.RouteOriginExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.route.target.extended.community._case.RouteTargetExtendedCommunity;
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
        ExtendedCommunities extendedCommunities = (ExtendedCommunities) attribute;
        byteAggregator.writeShort(extendedCommunities.getCommType());
        byteAggregator.writeShort(extendedCommunities.getCommSubType());
        if (extendedCommunities.getExtendedCommunity() instanceof AsSpecificExtendedCommunity){
            AsSpecificExtendedCommunity asSpecificExtendedCommunity = (AsSpecificExtendedCommunity)extendedCommunities.getExtendedCommunity();
            byteAggregator.writeShort(asSpecificExtendedCommunity.getGlobalAdministrator().getValue().shortValue());
            byteAggregator.writeInt(asSpecificExtendedCommunity.getGlobalAdministrator().getValue().intValue());
            return;
        }
        if (extendedCommunities.getExtendedCommunity() instanceof Inet4SpecificExtendedCommunity){
            Inet4SpecificExtendedCommunity inet4SpecificExtendedCommunity = (Inet4SpecificExtendedCommunity)extendedCommunities.getExtendedCommunity();
            byteAggregator.writeBytes(inet4SpecificExtendedCommunity.getGlobalAdministrator().getValue().getBytes());
            byteAggregator.writeBytes(inet4SpecificExtendedCommunity.getLocalAdministrator());
            return;
        }
        if (extendedCommunities.getExtendedCommunity() instanceof OpaqueExtendedCommunity){
            OpaqueExtendedCommunity opaqueExtendedCommunity = (OpaqueExtendedCommunity)extendedCommunities.getExtendedCommunity();
            byteAggregator.writeBytes(opaqueExtendedCommunity.getValue());
            return;
        }
        if (extendedCommunities.getExtendedCommunity() instanceof RouteTargetExtendedCommunity){
            RouteTargetExtendedCommunity routeTargetExtendedCommunity = (RouteTargetExtendedCommunity)extendedCommunities.getExtendedCommunity();
            byteAggregator.writeShort(routeTargetExtendedCommunity.getGlobalAdministrator().getValue().shortValue());
            byteAggregator.writeBytes(routeTargetExtendedCommunity.getLocalAdministrator());
            return;
        }
        if (extendedCommunities.getExtendedCommunity() instanceof RouteOriginExtendedCommunity){
            RouteOriginExtendedCommunity routeOriginExtendedCommunity = (RouteOriginExtendedCommunity)extendedCommunities.getExtendedCommunity();
            byteAggregator.writeShort(routeOriginExtendedCommunity.getGlobalAdministrator().getValue().shortValue());
            byteAggregator.writeBytes(routeOriginExtendedCommunity.getLocalAdministrator());
            return;
        }
    }
}