/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.nlri;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ipv6IFaceAddTlvParser implements NlriTlvObjectParser, NlriTlvObjectSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv6IFaceAddTlvParser.class);

    private static final int IPV6_IFACE_ADDRESS = 261;

    @Override
    public void parseNlriTlvObject(final ByteBuf value, final NlriTlvTypeBuilderContext context, final NodeDescriptorsTlvBuilderParser builderparser, final NlriType nlriType) throws BGPParsingException {
        final LinkDescriptorsBuilder linkbuilder = context.getLinkDescriptorsBuilder();
        final Ipv6InterfaceIdentifier lipv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForByteBuf(value));
        linkbuilder.setIpv6InterfaceAddress(lipv6);
        LOG.debug("Parsed IPv6 interface address {}.", lipv6);
    }

    @Override
    public void serializeTlvObject(final ObjectType nlriTypeCase, final NlriType nlriType, final ByteBuf buffer) {
        final LinkDescriptors ldescriptor = ((LinkCase)nlriTypeCase).getLinkDescriptors();
        if (ldescriptor.getIpv6InterfaceAddress() != null) {
            TlvUtil.writeTLV(IPV6_IFACE_ADDRESS, Ipv6Util.byteBufForAddress(ldescriptor.getIpv6InterfaceAddress()), buffer);
        }
    }
}
