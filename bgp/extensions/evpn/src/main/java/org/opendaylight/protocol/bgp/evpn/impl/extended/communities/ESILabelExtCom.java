/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.evpn.impl.extended.communities;

import static org.opendaylight.protocol.util.MplsLabelUtil.byteBufForMplsLabel;
import static org.opendaylight.protocol.util.MplsLabelUtil.mplsLabelForByteBuf;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.label.extended.community.EsiLabelExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.esi.label.extended.community.EsiLabelExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.EsiLabelExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.EsiLabelExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.MplsLabel;

public final class ESILabelExtCom extends AbstractExtendedCommunities {
    private static final int SUBTYPE = 1;
    private static final int RESERVED = 2;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer)
            throws BGPDocumentedException, BGPParsingException {
        final boolean singleActive = buffer.readBoolean();
        buffer.skipBytes(RESERVED);
        final MplsLabel label = mplsLabelForByteBuf(buffer);
        return new EsiLabelExtendedCommunityCaseBuilder().setEsiLabelExtendedCommunity(
            new EsiLabelExtendedCommunityBuilder().setEsiLabel(label)
                    .setSingleActiveMode(singleActive).build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof EsiLabelExtendedCommunityCase,
            "The extended community %s is not EsiLabelExtendedCommunityCaseCase type.", extendedCommunity);
        final EsiLabelExtendedCommunity extCom = ((EsiLabelExtendedCommunityCase) extendedCommunity)
                .getEsiLabelExtendedCommunity();
        byteAggregator.writeBoolean(extCom.isSingleActiveMode());
        byteAggregator.writeZero(RESERVED);
        byteAggregator.writeBytes(byteBufForMplsLabel(extCom.getEsiLabel()));
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
