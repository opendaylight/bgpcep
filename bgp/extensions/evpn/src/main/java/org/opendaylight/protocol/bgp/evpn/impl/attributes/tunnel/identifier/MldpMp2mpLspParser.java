/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier;

import static org.opendaylight.protocol.bgp.evpn.impl.attributes.tunnel.identifier.TunnelIdentifierHandler.NO_TUNNEL_INFORMATION_PRESENT;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.Opaque;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.TunnelIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.MldpMp2mpLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi.tunnel.tunnel.identifier.MldpMp2mpLspBuilder;

final class MldpMp2mpLspParser implements TunnelIdentifierSerializer, TunnelIdentifierParser {
    @Override
    public int serialize(final TunnelIdentifier tunnelIdentifier, final ByteBuf buffer) {
        Preconditions.checkArgument(tunnelIdentifier instanceof MldpMp2mpLsp,
                "The tunnelIdentifier %s is not MldpMp2mpLsp type.", tunnelIdentifier);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.mldp.mp2mp.lsp.MldpMp2mpLsp mldpMp2mpLsp =
                ((MldpMp2mpLsp) tunnelIdentifier).getMldpMp2mpLsp();
        if (!OpaqueUtil.serializeOpaque(mldpMp2mpLsp, buffer)) {
            return NO_TUNNEL_INFORMATION_PRESENT;
        }
        return TunnelType.M_LDP_MP_2_MP_LSP.getIntValue();
    }

    @Override
    public TunnelIdentifier parse(final ByteBuf buffer) {
        final Opaque opaque = OpaqueUtil.parseOpaque(buffer);
        if (opaque == null) {
            return null;
        }
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel.pmsi
                .tunnel.tunnel.identifier.mldp.mp2mp.lsp.MldpMp2mpLsp mldpMp2mpLsp =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pmsi.tunnel.rev160812.pmsi.tunnel
                        .pmsi.tunnel.tunnel.identifier.mldp.mp2mp.lsp.MldpMp2mpLspBuilder(opaque).build();
        return new MldpMp2mpLspBuilder().setMldpMp2mpLsp(mldpMp2mpLsp).build();
    }
}
