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
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NodeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.TeLspCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.te.lsp._case.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;

public final class TeLspIpv4NlriParser implements NlriTypeCaseParser {

    @Override
    public ObjectType parseTypeNlri(final ByteBuf nlri, final NlriType type, final NodeIdentifier localdescriptor, final ByteBuf restNlri) throws BGPParsingException {
        final TeLspCaseBuilder telspipv4builder = new TeLspCaseBuilder();

        final Ipv4CaseBuilder ipv4Builder = new Ipv4CaseBuilder();
        ipv4Builder.setIpv4TunnelSenderAddress(Ipv4Util.addressForByteBuf(nlri));
        final TunnelId tunnelId = new TunnelId(nlri.readUnsignedShort());
        final LspId lspId = new LspId((long)nlri.readUnsignedShort());
        ipv4Builder.setIpv4TunnelEndpointAddress(Ipv4Util.addressForByteBuf(nlri));
        TeLspCase telspipv4Case = telspipv4builder.setAddressFamily(ipv4Builder.build()).setLspId(lspId).setTunnelId(tunnelId).build();
        return telspipv4Case;
    }

}
