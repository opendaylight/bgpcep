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
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.Layer2AttributesExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.Layer2AttributesExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.layer._2.attributes.extended.community.Layer2AttributesExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.layer._2.attributes.extended.community.Layer2AttributesExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

public class Layer2AttributesExtCom extends AbstractExtendedCommunities {
    // TODO: TBD BY IANA
    private static final int SUBTYPE = 4;
    private static final int FLAGS_SIZE = 16;
    private static final int PRIMARY_PE_OFFSET = 15;
    private static final int CONTROL_WORD_OFFSET = 13;
    private static final int BACKUP_PE_OFFSET = 14;
    private static final int RESERVED = 2;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf body) throws BGPDocumentedException, BGPParsingException {
        final Layer2AttributesExtendedCommunityBuilder builder = new Layer2AttributesExtendedCommunityBuilder();
        final BitArray flags = BitArray.valueOf(body, FLAGS_SIZE);
        builder.setPrimaryPe(flags.get(PRIMARY_PE_OFFSET));
        builder.setBackupPe(flags.get(BACKUP_PE_OFFSET));
        builder.setControlWord(flags.get(CONTROL_WORD_OFFSET));
        builder.setL2Mtu(body.readUnsignedShort());
        body.skipBytes(RESERVED);
        return new Layer2AttributesExtendedCommunityCaseBuilder().setLayer2AttributesExtendedCommunity(builder.build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf body) {
        Preconditions.checkArgument(extendedCommunity instanceof Layer2AttributesExtendedCommunityCase,
            "The extended community %s is not EsImportRouteExtendedCommunityCaseCase type.", extendedCommunity);
        final Layer2AttributesExtendedCommunity extCom = ((Layer2AttributesExtendedCommunityCase) extendedCommunity).getLayer2AttributesExtendedCommunity();
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(PRIMARY_PE_OFFSET, extCom.isPrimaryPe());
        flags.set(BACKUP_PE_OFFSET, extCom.isBackupPe());
        flags.set(CONTROL_WORD_OFFSET, extCom.isControlWord());
        flags.toByteBuf(body);
        ByteBufWriteUtil.writeUnsignedShort(extCom.getL2Mtu(), body);
        body.writeZero(RESERVED);
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
