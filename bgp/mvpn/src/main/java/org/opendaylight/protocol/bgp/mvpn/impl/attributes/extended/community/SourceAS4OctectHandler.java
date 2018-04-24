/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes.extended.community;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.Abstract4OctetAsExtendedCommunity;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.SourceAs4ExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.SourceAs4ExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.source.as._4.extended.community._case.SourceAs4ExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;

public final class SourceAS4OctectHandler extends Abstract4OctetAsExtendedCommunity {
    private static final short SUBTYPE = 209;
    private static final int LOCAL_ADMIN = 0;
    private static final short LOCAL_LENGTH = 2;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf body) {
        final SourceAs4ExtendedCommunityBuilder builder = new SourceAs4ExtendedCommunityBuilder();
        builder.setAsNumber(new AsNumber(body.readUnsignedInt()));
        body.skipBytes(LOCAL_LENGTH);
        return new SourceAs4ExtendedCommunityCaseBuilder().setSourceAs4ExtendedCommunity(
                builder.build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf body) {
        Preconditions.checkArgument(extendedCommunity instanceof SourceAs4ExtendedCommunityCase,
                "The extended community %s is not SourceAs4ExtendedCommunityCase type.",
                extendedCommunity);

        body.writeInt(((SourceAs4ExtendedCommunityCase) extendedCommunity).getSourceAs4ExtendedCommunity()
                .getAsNumber().getValue().intValue());
        ByteBufWriteUtil.writeUnsignedShort(LOCAL_ADMIN, body);
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
