/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractIpv4ExtendedCommunity;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.Inet4SpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunityBuilder;

public final class Ipv4SpecificEcHandler extends AbstractIpv4ExtendedCommunity {

    private static final int SUBTYPE = 0;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        return new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                new Inet4SpecificExtendedCommunityBuilder().setTransitive(true).setGlobalAdministrator(
                        Ipv4Util.addressForByteBuf(buffer)).setLocalAdministrator(
                        ByteArray.readBytes(buffer, INET_LOCAL_ADMIN_LENGTH)).build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof Inet4SpecificExtendedCommunityCase,
                "The extended community %s is not Inet4SpecificExtendedCommunityCase type.", extendedCommunity);
        final Inet4SpecificExtendedCommunity inet4SpecificExtendedCommunity = ((Inet4SpecificExtendedCommunityCase) extendedCommunity).getInet4SpecificExtendedCommunity();
        ByteBufWriteUtil.writeIpv4Address(inet4SpecificExtendedCommunity.getGlobalAdministrator(), byteAggregator);
        byteAggregator.writeBytes(inet4SpecificExtendedCommunity.getLocalAdministrator());
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }

}
