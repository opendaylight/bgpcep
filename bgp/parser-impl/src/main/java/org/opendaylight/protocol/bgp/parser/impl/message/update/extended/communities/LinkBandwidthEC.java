/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import static org.opendaylight.protocol.bgp.parser.impl.message.BGPOpenMessageParser.AS_TRANS;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunityParser;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.ExtendedCommunitySerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.LinkBandwidthCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.LinkBandwidthCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.link.bandwidth._case.LinkBandwidthExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.link.bandwidth._case.LinkBandwidthExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;

public class LinkBandwidthEC implements ExtendedCommunityParser, ExtendedCommunitySerializer {
    private static final int TYPE = 64;
    private static final int SUBTYPE = 4;
    private static final int BANDWIDTH_SIZE = 4;
    private static final int AS_TRANS_LENGTH = 2;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer)
            throws BGPDocumentedException, BGPParsingException {
        buffer.skipBytes(AS_TRANS_LENGTH);
        final LinkBandwidthExtendedCommunity lb = new LinkBandwidthExtendedCommunityBuilder()
            .setBandwidth(new Bandwidth(ByteArray.readBytes(buffer, BANDWIDTH_SIZE)))
            .build();
        return new LinkBandwidthCaseBuilder().setLinkBandwidthExtendedCommunity(lb).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf body) {
        Preconditions.checkArgument(extendedCommunity instanceof LinkBandwidthCase,
            "The extended community %s is not LinkBandwidthCase type.", extendedCommunity);
        final LinkBandwidthExtendedCommunity lb
                = ((LinkBandwidthCase) extendedCommunity).getLinkBandwidthExtendedCommunity();
        body.writeShort(AS_TRANS);
        ByteBufWriteUtil.writeFloat32(lb.getBandwidth(), body);
    }

    @Override
    public int getType(final boolean isTransitive) {
        return TYPE;
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
