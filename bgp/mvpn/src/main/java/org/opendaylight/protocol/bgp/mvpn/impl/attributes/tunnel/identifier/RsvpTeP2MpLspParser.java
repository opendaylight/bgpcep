/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier;

import static org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.PAddressPMulticastGroupUtil.parseIpAddress;
import static org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier.PAddressPMulticastGroupUtil.serializeIpAddress;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.mvpn.spi.attributes.tunnel.identifier.AbstractTunnelIdentifier;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.PmsiTunnelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.RsvpTeP2mpLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.RsvpTeP2mpLspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.rsvp.te.p2mp.lsp.RsvpTeP2mpLps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev180329.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.rsvp.te.p2mp.lsp.RsvpTeP2mpLpsBuilder;

public final class RsvpTeP2MpLspParser extends AbstractTunnelIdentifier<RsvpTeP2mpLsp> {

    private static final int RESERVED = 2;

    @Override
    public Class<? extends TunnelIdentifier> getClazz() {
        return RsvpTeP2mpLsp.class;
    }

    @Override
    public int serialize(final RsvpTeP2mpLsp tunnelIdentifier, final ByteBuf buffer) {
        final RsvpTeP2mpLps rsvpTeP2mpLsp = tunnelIdentifier.getRsvpTeP2mpLps();
        ByteBufWriteUtil.writeUnsignedInt(rsvpTeP2mpLsp.getP2mpId(), buffer);
        buffer.writeZero(RESERVED);
        ByteBufWriteUtil.writeUnsignedShort(rsvpTeP2mpLsp.getTunnelId(), buffer);
        serializeIpAddress(rsvpTeP2mpLsp.getExtendedTunnelId(), buffer);
        return getType();
    }

    @Override
    public RsvpTeP2mpLsp parse(final ByteBuf buffer) {
        final RsvpTeP2mpLpsBuilder rsvpTeP2mpLps = new RsvpTeP2mpLpsBuilder();
        rsvpTeP2mpLps.setP2mpId(buffer.readUnsignedInt());
        buffer.skipBytes(2);
        rsvpTeP2mpLps.setTunnelId(buffer.readUnsignedShort());
        final int ipLength = buffer.readableBytes();
        rsvpTeP2mpLps.setExtendedTunnelId(parseIpAddress(ipLength, buffer));
        return new RsvpTeP2mpLspBuilder().setRsvpTeP2mpLps(rsvpTeP2mpLps.build()).build();
    }

    @Override
    public int getType() {
        return PmsiTunnelType.RsvpTeP2mpLps.getIntValue();
    }
}
