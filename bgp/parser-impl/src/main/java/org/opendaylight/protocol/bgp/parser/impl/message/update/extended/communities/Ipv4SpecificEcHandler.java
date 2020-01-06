/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update.extended.communities;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.protocol.bgp.parser.spi.extended.community.Inet4SpecificExtendedCommunityCommonUtil.parseCommon;
import static org.opendaylight.protocol.bgp.parser.spi.extended.community.Inet4SpecificExtendedCommunityCommonUtil.serializeCommon;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractIpv4ExtendedCommunity;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.Inet4SpecificExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.Inet4SpecificExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.extended.community.inet4.specific.extended.community._case.Inet4SpecificExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.inet4.specific.extended.community.common.Inet4SpecificExtendedCommunityCommon;

public final class Ipv4SpecificEcHandler extends AbstractIpv4ExtendedCommunity {
    private static final int SUBTYPE = 0;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        return new Inet4SpecificExtendedCommunityCaseBuilder().setInet4SpecificExtendedCommunity(
                new Inet4SpecificExtendedCommunityBuilder()
                        .setInet4SpecificExtendedCommunityCommon(parseCommon(buffer))
                        .build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        checkArgument(extendedCommunity instanceof Inet4SpecificExtendedCommunityCase,
                "The extended community %s is not Inet4SpecificExtendedCommunityCase type.", extendedCommunity);
        final Inet4SpecificExtendedCommunity inet4SpecificExtendedCommunity
                = ((Inet4SpecificExtendedCommunityCase) extendedCommunity).getInet4SpecificExtendedCommunity();

        final Inet4SpecificExtendedCommunityCommon common
                = inet4SpecificExtendedCommunity.getInet4SpecificExtendedCommunityCommon();
        if (common != null) {
            serializeCommon(inet4SpecificExtendedCommunity.getInet4SpecificExtendedCommunityCommon(), byteAggregator);
        } else {
            Ipv4Util.writeIpv4Address(inet4SpecificExtendedCommunity.getGlobalAdministrator(), byteAggregator);
            byteAggregator.writeBytes(inet4SpecificExtendedCommunity.getLocalAdministrator());
        }
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
