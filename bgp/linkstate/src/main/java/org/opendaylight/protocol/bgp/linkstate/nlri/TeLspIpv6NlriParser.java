/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;

public final class TeLspIpv6NlriParser extends AbstractTeLspNlriCodec {

    public TeLspIpv6NlriParser(final SimpleNlriTypeRegistry registry) {
        super(registry);
    }

    @Override
    protected ObjectType parseObjectType(final ByteBuf buffer) {
        final TeLspCaseBuilder builder = new TeLspCaseBuilder();
        final Ipv6CaseBuilder ipv4CaseBuilder = new Ipv6CaseBuilder();
        ipv4CaseBuilder.setIpv6TunnelSenderAddress(Ipv6Util.addressForByteBuf(buffer));
        builder.setTunnelId(new TunnelId(buffer.readUnsignedShort()));
        builder.setLspId(new LspId((long) buffer.readUnsignedShort()));
        ipv4CaseBuilder.setIpv6TunnelEndpointAddress(Ipv6Util.addressForByteBuf(buffer));
        return builder.setAddressFamily(ipv4CaseBuilder.build()).build();
    }

    @Override
    public int getNlriType() {
        return NlriType.Ipv6TeLsp.getIntValue();
    }

}
