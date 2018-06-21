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
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.IetfYangUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.es._import.route.extended.community.EsImportRouteExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.es._import.route.extended.community.EsImportRouteExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.EsImportRouteExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171213.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.EsImportRouteExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

public final class ESImpRouteTargetExtCom extends AbstractExtendedCommunities {
    private static final int SUBTYPE = 2;
    private static final int MAC_ADDRESS_LENGTH = 6;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) {
        final MacAddress mac = IetfYangUtil.INSTANCE.macAddressFor(ByteArray.readBytes(buffer, MAC_ADDRESS_LENGTH));

        return new EsImportRouteExtendedCommunityCaseBuilder().setEsImportRouteExtendedCommunity(
                new EsImportRouteExtendedCommunityBuilder().setEsImport(mac).build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof EsImportRouteExtendedCommunityCase,
                "The extended community %s is not EsImportRouteExtendedCommunityCaseCase type.",
                extendedCommunity);
        final EsImportRouteExtendedCommunity extCom = ((EsImportRouteExtendedCommunityCase) extendedCommunity)
                .getEsImportRouteExtendedCommunity();
        byteAggregator.writeBytes(IetfYangUtil.INSTANCE.bytesFor(extCom.getEsImport()));
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
