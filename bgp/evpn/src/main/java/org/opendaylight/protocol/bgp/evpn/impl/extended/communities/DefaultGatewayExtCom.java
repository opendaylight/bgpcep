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
import org.opendaylight.protocol.bgp.parser.spi.extended.community.AbstractOpaqueExtendedCommunity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207._default.gateway.extended.community.DefaultGatewayExtendedCommunityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.DefaultGatewayExtendedCommunityCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev171207.evpn.routes.evpn.routes.evpn.route.attributes.extended.communities.extended.community.DefaultGatewayExtendedCommunityCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.extended.community.ExtendedCommunity;

public final class DefaultGatewayExtCom extends AbstractOpaqueExtendedCommunity {
    private static final int SUBTYPE = 13;
    private static final int RESERVED = 6;

    @Override
    public ExtendedCommunity parseExtendedCommunity(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        Preconditions.checkArgument(buffer.readableBytes() == RESERVED, "Wrong length of array of bytes. Passed: %s .", buffer.readableBytes());
        buffer.skipBytes(RESERVED);
        return new DefaultGatewayExtendedCommunityCaseBuilder().setDefaultGatewayExtendedCommunity(
            new DefaultGatewayExtendedCommunityBuilder().build()).build();
    }

    @Override
    public void serializeExtendedCommunity(final ExtendedCommunity extendedCommunity, final ByteBuf byteAggregator) {
        Preconditions.checkArgument(extendedCommunity instanceof DefaultGatewayExtendedCommunityCase,
            "The extended community %s is not DefaultGatewayExtendedCommunityCase type.", extendedCommunity);
        byteAggregator.writeZero(RESERVED);
    }

    @Override
    public int getSubType() {
        return SUBTYPE;
    }
}
