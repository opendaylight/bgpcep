/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities.four.octect.as.specific;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.Abstract4OctetAsExtendedCommunity;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.FourOctAsCommonECUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4GenericSpecExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.As4GenericSpecExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.extended.community.extended.community.as._4.generic.spec.extended.community._case.As4GenericSpecExtendedCommunityBuilder;

public final class Generic4OctASEcHandler extends Abstract4OctetAsExtendedCommunity {
    private static final int SUBTYPE = 4;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf body)
            throws BGPDocumentedException, BGPParsingException {
        return new As4GenericSpecExtendedCommunityCaseBuilder()
                .setAs4GenericSpecExtendedCommunity(new As4GenericSpecExtendedCommunityBuilder()
                        .setAs4SpecificCommon(FourOctAsCommonECUtil.parseCommon(body)).build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf body) {
        Preconditions.checkArgument(extendedCommunity instanceof As4GenericSpecExtendedCommunityCase,
                "The extended community %s is not As4GenericSpecExtendedCommunityCase type.",
                extendedCommunity);
        FourOctAsCommonECUtil.serializeCommon(((As4GenericSpecExtendedCommunityCase) extendedCommunity)
                .getAs4GenericSpecExtendedCommunity()
                .getAs4SpecificCommon(), body);
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
