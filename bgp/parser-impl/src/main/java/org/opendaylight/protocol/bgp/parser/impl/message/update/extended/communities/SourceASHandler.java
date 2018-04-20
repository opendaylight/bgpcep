/*
 *  Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractTwoOctetAsExtendedCommunity;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.ShortAsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.SourceAsExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.SourceAsExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.source.as.extended.community._case.SourceAsExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.source.as.extended.community._case.SourceAsExtendedCommunityBuilder;

public final class SourceASHandler extends AbstractTwoOctetAsExtendedCommunity {
    private static final short SUBTYPE = 9;
    private static final int LOCAL_ADMIN = 0;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        final SourceAsExtendedCommunityBuilder builder = new SourceAsExtendedCommunityBuilder();
        builder.setGlobalAdministrator(new ShortAsNumber((long) buffer.readUnsignedShort()));
        buffer.skipBytes(AS_LOCAL_ADMIN_LENGTH);
        return new SourceAsExtendedCommunityCaseBuilder().setSourceAsExtendedCommunity(builder.build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf body) {
        Preconditions.checkArgument(extendedCommunity instanceof SourceAsExtendedCommunityCase,
                "The extended community %s is not SourceAsExtendedCommunityCase type.",
                extendedCommunity);
        final SourceAsExtendedCommunity excomm = ((SourceAsExtendedCommunityCase) extendedCommunity)
                .getSourceAsExtendedCommunity();
        ByteBufWriteUtil.writeUnsignedShort(Ints.checkedCast(excomm.getGlobalAdministrator().getValue()), body);
        ByteBufWriteUtil.writeInt(LOCAL_ADMIN, body);
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
