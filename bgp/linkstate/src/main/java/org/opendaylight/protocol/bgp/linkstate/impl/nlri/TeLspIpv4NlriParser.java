/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.AbstractTeLspNlriCodec;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.TeLspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.te.lsp._case.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;

public final class TeLspIpv4NlriParser extends AbstractTeLspNlriCodec {
    @Override
    protected ObjectType parseObjectType(final ByteBuf buffer) {
        final TeLspCaseBuilder builder = new TeLspCaseBuilder();
        final Ipv4CaseBuilder ipv4CaseBuilder = new Ipv4CaseBuilder();
        ipv4CaseBuilder.setIpv4TunnelSenderAddress(Ipv4Util.addressForByteBuf(buffer));
        builder.setTunnelId(new TunnelId(buffer.readUnsignedShort()));
        builder.setLspId(new LspId((long) buffer.readUnsignedShort()));
        ipv4CaseBuilder.setIpv4TunnelEndpointAddress(Ipv4Util.addressForByteBuf(buffer));
        return builder.setAddressFamily(ipv4CaseBuilder.build()).build();
    }

    @Override
    public int getNlriType() {
        return NlriType.Ipv4TeLsp.getIntValue();
    }

}
