/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.ExtendedCommunities;
import org.opendaylight.yangtools.yang.binding.DataObject;

public final class ExtendedCommunitiesAttributeParser implements AttributeParser, AttributeSerializer {

    public static final int TYPE = 16;

    private final ExtendedCommunityRegistry ecReg;

    public ExtendedCommunitiesAttributeParser(final ExtendedCommunityRegistry ecReg) {
        this.ecReg = requireNonNull(ecReg);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder) throws BGPDocumentedException, BGPParsingException {
        final List<ExtendedCommunities> set = new ArrayList<>();
        while (buffer.isReadable()) {
            final ExtendedCommunities exComm = this.ecReg.parseExtendedCommunity(buffer);
            if (exComm != null) {
                set.add(exComm);
            }
        }
        builder.setExtendedCommunities(set);
    }

    @Override
    public void serializeAttribute(final DataObject attribute, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(attribute instanceof Attributes, "Attribute parameter is not a PathAttribute object.");
        final List<ExtendedCommunities> communitiesList = ((Attributes) attribute).getExtendedCommunities();
        if (communitiesList == null || communitiesList.isEmpty()) {
            return;
        }
        final ByteBuf extendedCommunitiesBuffer = Unpooled.buffer();
        for (final ExtendedCommunities extendedCommunities : communitiesList) {
            this.ecReg.serializeExtendedCommunity(extendedCommunities, extendedCommunitiesBuffer);
        }
        if (extendedCommunitiesBuffer.readableBytes() > 0) {
            AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL | AttributeUtil.TRANSITIVE, TYPE, extendedCommunitiesBuffer, byteAggregator);
        }
    }
}
