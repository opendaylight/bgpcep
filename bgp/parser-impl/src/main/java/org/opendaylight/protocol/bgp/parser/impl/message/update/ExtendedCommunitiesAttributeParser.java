/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;
import org.opendaylight.protocol.bgp.parser.spi.AbstractAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandling;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.ExtendedCommunities;

public final class ExtendedCommunitiesAttributeParser extends AbstractAttributeParser implements AttributeSerializer {

    public static final int TYPE = 16;

    private final ExtendedCommunityRegistry ecReg;

    public ExtendedCommunitiesAttributeParser(final ExtendedCommunityRegistry ecReg) {
        this.ecReg = requireNonNull(ecReg);
    }

    @Override
    public void parseAttribute(final ByteBuf buffer, final AttributesBuilder builder,
            final RevisedErrorHandling errorHandling, final PeerSpecificParserConstraint constraint)
                    throws BGPDocumentedException, BGPTreatAsWithdrawException {
        final int readable = buffer.readableBytes();
        if (errorHandling != RevisedErrorHandling.NONE) {
            if (readable == 0) {
                throw new BGPTreatAsWithdrawException(BGPError.ATTR_LENGTH_ERROR, "Empty Extended Community attribute");
            }
        }
        if (readable % 8 != 0) {
            throw errorHandling.reportError(BGPError.ATTR_LENGTH_ERROR,
                "Extended Community attribute length must be a multiple of 8, have %s", readable);
        }
        final List<ExtendedCommunities> set = new ArrayList<>(readable / 8);
        while (buffer.isReadable()) {
            final ExtendedCommunities exComm;
            try {
                // FIXME: BGPCEP-359: revise API contract here
                exComm = this.ecReg.parseExtendedCommunity(buffer);
            } catch (BGPParsingException e) {
                throw errorHandling.reportError(BGPError.MALFORMED_ATTR_LIST, e, "Failed to parse extended community");
            }
            if (exComm != null) {
                set.add(exComm);
            }
        }
        builder.setExtendedCommunities(set);
    }

    @Override
    public void serializeAttribute(final Attributes pathAttributes, final ByteBuf byteAggregator) {
        final List<ExtendedCommunities> communitiesList = pathAttributes.getExtendedCommunities();
        if (communitiesList == null || communitiesList.isEmpty()) {
            return;
        }
        final ByteBuf extendedCommunitiesBuffer = Unpooled.buffer();
        for (final ExtendedCommunities extendedCommunities : communitiesList) {
            this.ecReg.serializeExtendedCommunity(extendedCommunities, extendedCommunitiesBuffer);
        }
        if (extendedCommunitiesBuffer.readableBytes() > 0) {
            AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL | AttributeUtil.TRANSITIVE, TYPE,
                    extendedCommunitiesBuffer, byteAggregator);
        }
    }
}
