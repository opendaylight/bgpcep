/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

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
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ipv6NeighborAddTlvParser implements NlriSubTlvObjectParser, NlriSubTlvObjectSerializer, SubTlvLinkBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv6NeighborAddTlvParser.class);

    public static final int IPV6_NEIGHBOR_ADDRESS = 262;

    @Override
    public Object parseNlriSubTlvObject(final ByteBuf value, final NlriType nlriType) throws BGPParsingException {
        final Ipv6InterfaceIdentifier ripv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForByteBuf(value));
        LOG.debug("Parsed IPv6 neighbor address {}.", ripv6);
        return ripv6;
    }

    @Override
    public void serializeNlriSubTlvObject(final ObjectType nlriTypeCase, final NodeIdentifier qNameId, final ByteBuf buffer) {
        final LinkDescriptors ldescriptor = ((LinkCase)nlriTypeCase).getLinkDescriptors();
        if (ldescriptor.getIpv6NeighborAddress() != null) {
            TlvUtil.writeTLV(IPV6_NEIGHBOR_ADDRESS, Ipv6Util.byteBufForAddress(ldescriptor.getIpv6NeighborAddress()), buffer);
        }
    }

    @Override
    public void buildLinkDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((LinkDescriptorsBuilder) tlvBuilder).setIpv6NeighborAddress((Ipv6InterfaceIdentifier) subTlvObject);
    }
}
