/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.address.family.Ipv6CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;

public final class TeLspIpv6NlriParser implements NlriTypeCaseParser {

    @Override
    public ObjectType parseTypeNlri(ByteBuf nlri, NlriType type, NodeIdentifier localdescriptor, ByteBuf restNlri) throws BGPParsingException {
        final TeLspCaseBuilder telspipv6builder = new TeLspCaseBuilder();

        final Ipv6CaseBuilder ipv6Builder = new Ipv6CaseBuilder();
        ipv6Builder.setIpv6TunnelSenderAddress(Ipv6Util.addressForByteBuf(nlri));
        final TunnelId tunnelId6 = new TunnelId(nlri.readUnsignedShort());
        final LspId lspId6 = new LspId((long)nlri.readUnsignedShort());
        ipv6Builder.setIpv6TunnelEndpointAddress(Ipv6Util.addressForByteBuf(nlri));
        TeLspCase telspipv6Case = telspipv6builder.setAddressFamily(ipv6Builder.build()).setLspId(lspId6).setTunnelId(tunnelId6).build();
        return telspipv6Case;
    }

}
