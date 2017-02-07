/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.extended.communities;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.MacMobilityExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.MacMobilityExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.mac.mobility.extended.community.MacMobilityExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.mac.mobility.extended.community.MacMobilityExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

public final class MACMobExtCom extends AbstractExtendedCommunities {
    private static final int SUBTYPE = 0;
    private static final int RESERVED = 1;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer)
            throws BGPDocumentedException, BGPParsingException {
        final boolean isStatic = buffer.readBoolean();
        buffer.skipBytes(RESERVED);
        final long seqNumber = buffer.readUnsignedInt();
        return new MacMobilityExtendedCommunityCaseBuilder()
                .setMacMobilityExtendedCommunity(new MacMobilityExtendedCommunityBuilder()
            .setStatic(isStatic).setSeqNumber(seqNumber).build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof MacMobilityExtendedCommunityCase,
            "The extended community %s is not MacMobilityExtendedCommunityCase type.", extendedCommunity);
        final MacMobilityExtendedCommunity extCom = ((MacMobilityExtendedCommunityCase) extendedCommunity)
                .getMacMobilityExtendedCommunity();
        byteAggregator.writeBoolean(extCom.isStatic());
        byteAggregator.writeZero(RESERVED);
        byteAggregator.writeInt(extCom.getSeqNumber().intValue());
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
