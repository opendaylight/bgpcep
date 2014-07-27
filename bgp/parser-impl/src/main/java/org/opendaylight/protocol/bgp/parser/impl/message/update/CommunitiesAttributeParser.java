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
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.PathAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Communities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.PathAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Community;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class CommunitiesAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 8;

    private final ReferenceCache refCache;

    public CommunitiesAttributeParser(final ReferenceCache refCache) {
        this.refCache = Preconditions.checkNotNull(refCache);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final PathAttributesBuilder builder) throws BGPDocumentedException {
        final List<Communities> set = Lists.newArrayList();
        while (buffer.isReadable()) {
            set.add((Communities) CommunitiesParser.parseCommunity(this.refCache, buffer.slice(buffer.readerIndex(),
                CommunitiesParser.COMMUNITY_LENGTH)));
            buffer.skipBytes(CommunitiesParser.COMMUNITY_LENGTH);
        }
        builder.setCommunities(set);
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof PathAttributes, "Attribute parameter is not a PathAttribute object.");
        final PathAttributes pathAttributes = (PathAttributes) attribute;
        final List<Communities> communities = pathAttributes.getCommunities();
        if (communities == null || communities.isEmpty()) {
            return;
        }
        final ByteBuf communitiesBuffer = Unpooled.buffer();
        for (final Community community : communities) {
            communitiesBuffer.writeShort(community.getAsNumber().getValue().shortValue());
            communitiesBuffer.writeShort(community.getSemantics().shortValue());
        }
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL | AttributeUtil.TRANSITIVE, TYPE, communitiesBuffer, byteAggregator);
    }
}
