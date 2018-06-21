/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PAddressPMulticastGroupUtil.parseIpAddress;
import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.PAddressPMulticastGroupUtil.serializeIpAddress;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.RsvpTeP2mpLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.RsvpTeP2mpLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.rsvp.te.p2mp.lsp.RsvpTeP2mpLps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.rsvp.te.p2mp.lsp.RsvpTeP2mpLpsBuilder;

final class RsvpTeP2MpLspParser implements TunnelIdentifierSerializer, TunnelIdentifierParser {

    private static final int RESERVED = 2;

    @Override
    public int serialize(final TunnelIdentifier tunnelIdentifier, final ByteBuf buffer) {
        Preconditions.checkArgument(tunnelIdentifier instanceof RsvpTeP2mpLsp,
                "The tunnelIdentifier %s is not RsvpTeP2mpLps type.", tunnelIdentifier);
        final RsvpTeP2mpLps rsvpTeP2mpLsp = ((RsvpTeP2mpLsp) tunnelIdentifier).getRsvpTeP2mpLps();
        ByteBufWriteUtil.writeUnsignedInt(rsvpTeP2mpLsp.getP2mpId(), buffer);
        buffer.writeZero(RESERVED);
        ByteBufWriteUtil.writeUnsignedShort(rsvpTeP2mpLsp.getTunnelId(), buffer);
        serializeIpAddress(rsvpTeP2mpLsp.getExtendedTunnelId(), buffer);
        return TunnelType.RSVP_TE_P2MP_LSP.getIntValue();
    }

    @Override
    public TunnelIdentifier parse(final ByteBuf buffer) {
        final RsvpTeP2mpLpsBuilder rsvpTeP2mpLps = new RsvpTeP2mpLpsBuilder();
        rsvpTeP2mpLps.setP2mpId(buffer.readUnsignedInt());
        buffer.skipBytes(2);
        rsvpTeP2mpLps.setTunnelId(buffer.readUnsignedShort());
        final int ipLength = buffer.readableBytes();
        rsvpTeP2mpLps.setExtendedTunnelId(parseIpAddress(ipLength, buffer));
        return new RsvpTeP2mpLspBuilder().setRsvpTeP2mpLps(rsvpTeP2mpLps.build()).build();
    }
}
