/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mvpn.impl.attributes.tunnel.identifier;

import static org.opendaylight.protocol.bgp.mvpn.spi.pojo.attributes.tunnel.identifier.SimpleTunnelIdentifierRegistry.NO_TUNNEL_INFORMATION_PRESENT;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.mvpn.impl.attributes.OpaqueUtil;
import org.opendaylight.protocol.bgp.mvpn.spi.attributes.tunnel.identifier.AbstractTunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.Opaque;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.PmsiTunnelType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.MldpMp2mpLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.MldpMp2mpLspBuilder;

public final class MldpMp2mpLspParser extends AbstractTunnelIdentifier<MldpMp2mpLsp> {
    @Override
    public int serialize(final MldpMp2mpLsp tunnelIdentifier, final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.mldp.mp2mp.lsp.MldpMp2mpLsp mldpMp2mpLsp =
                tunnelIdentifier.getMldpMp2mpLsp();
        if (!OpaqueUtil.serializeOpaque(mldpMp2mpLsp, buffer)) {
            return NO_TUNNEL_INFORMATION_PRESENT;
        }
        return getType();
    }

    @Override
    public Class<? extends TunnelIdentifier> getClazz() {
        return MldpMp2mpLsp.class;
    }

    @Override
    public MldpMp2mpLsp parse(final ByteBuf buffer) {
        final Opaque opaque = OpaqueUtil.parseOpaque(buffer);
        if (opaque == null) {
            return null;
        }
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.mldp.mp2mp.lsp.MldpMp2mpLsp mldpMp2mpLsp =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev200120.pmsi.tunnel
                        .pmsi.tunnel.tunnel.identifier.mldp.mp2mp.lsp.MldpMp2mpLspBuilder(opaque).build();
        return new MldpMp2mpLspBuilder().setMldpMp2mpLsp(mldpMp2mpLsp).build();
    }

    @Override
    public int getType() {
        return PmsiTunnelType.MldpMp2mpLsp.getIntValue();
    }
}
