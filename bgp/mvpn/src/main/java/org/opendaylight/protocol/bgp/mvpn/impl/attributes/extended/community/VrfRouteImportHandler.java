/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes.extended.community;

import static org.opendaylight.protocol.bgp.parser.spi.extended.community.Inet4SpecificExtendedCommunityCommonUtil.parseCommon;
import static org.opendaylight.protocol.bgp.parser.spi.extended.community.Inet4SpecificExtendedCommunityCommonUtil.serializeCommon;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractIpv4ExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.VrfRouteImportExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.VrfRouteImportExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.vrf.route._import.extended.community._case.VrfRouteImportExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.mvpn.rev180417.bgp.rib.route.attributes.extended.communities.extended.community.vrf.route._import.extended.community._case.VrfRouteImportExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.extended.community.ExtendedCommunity;

public final class VrfRouteImportHandler extends AbstractIpv4ExtendedCommunity {
    private static final short SUBTYPE = 11;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        return new VrfRouteImportExtendedCommunityCaseBuilder().setVrfRouteImportExtendedCommunity(
                new VrfRouteImportExtendedCommunityBuilder()
                        .setInet4SpecificExtendedCommunityCommon(parseCommon(buffer))
                        .build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof VrfRouteImportExtendedCommunityCase,
                "The extended community %s is not VrfRouteImportExtendedCommunityCase type.",
                extendedCommunity);
        final VrfRouteImportExtendedCommunity inet4SpecificExtendedCommunity
                = ((VrfRouteImportExtendedCommunityCase) extendedCommunity).getVrfRouteImportExtendedCommunity();
        serializeCommon(inet4SpecificExtendedCommunity.getInet4SpecificExtendedCommunityCommon(), byteAggregator);
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
