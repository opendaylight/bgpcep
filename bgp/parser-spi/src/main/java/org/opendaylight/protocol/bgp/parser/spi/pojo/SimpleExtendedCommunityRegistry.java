/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityUtil.isTransitive;
import static org.opendaylight.protocol.util.Values.UNSIGNED_BYTE_MAX_VALUE;

import com.google.common.primitives.Shorts;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunitiesBuilder;
import org.opendaylight.yangtools.binding.DataContainer;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleExtendedCommunityRegistry implements ExtendedCommunityRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleExtendedCommunityRegistry.class);

    private static final int EXTENDED_COMMUNITY_LENGTH = 6;

    private final HandlerRegistry<DataContainer, ExtendedCommunityParser, ExtendedCommunitySerializer> handlers =
            new HandlerRegistry<>();

    private static int createKey(final int type, final int subtype) {
        return type << Byte.SIZE | subtype;
    }

    @Override
    public ExtendedCommunities parseExtendedCommunity(final ByteBuf buffer)
            throws BGPDocumentedException, BGPParsingException {
        final short type = buffer.readUnsignedByte();
        final short subtype = buffer.readUnsignedByte();
        final ExtendedCommunityParser parser = this.handlers.getParser(createKey(type, subtype));
        if (parser == null) {
            buffer.skipBytes(EXTENDED_COMMUNITY_LENGTH);
            LOG.info("Skipping unknown extended-community type/sub-type {}/{}.", type, subtype);
            return null;
        }
        return new ExtendedCommunitiesBuilder()
            .setTransitive(isTransitive(type))
            .setExtendedCommunity(parser.parseExtendedCommunity(buffer))
            .build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunities extendedCommunity, final ByteBuf byteAggregator) {
        final ExtendedCommunitySerializer serializer = this.handlers.getSerializer(extendedCommunity
            .getExtendedCommunity().implementedInterface());
        if (serializer == null) {
            return;
        }
        byteAggregator.writeByte(Shorts.checkedCast(serializer.getType(extendedCommunity.getTransitive())));
        byteAggregator.writeByte(Shorts.checkedCast(serializer.getSubType()));
        serializer.serializeExtendedCommunity(extendedCommunity.getExtendedCommunity(), byteAggregator);
    }

    synchronized Registration registerExtendedCommunitySerializer(
            final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120
                    .extended.community.ExtendedCommunity> extendedCommunityClass,
            final ExtendedCommunitySerializer serializer) {
        return this.handlers.registerSerializer(extendedCommunityClass, serializer);
    }

    synchronized Registration registerExtendedCommunityParser(final int type, final int subtype,
            final ExtendedCommunityParser parser) {
        checkTypes(type, subtype);
        return this.handlers.registerParser(createKey(type, subtype), parser);
    }

    private static void checkTypes(final int type, final int subtype) {
        checkArgument(type >= 0 && type <= UNSIGNED_BYTE_MAX_VALUE, "Illegal extended-community type %s", type);
        checkArgument(subtype >= 0 && subtype <= UNSIGNED_BYTE_MAX_VALUE, "Illegal extended-community sub-type %s",
                subtype);
    }

}
