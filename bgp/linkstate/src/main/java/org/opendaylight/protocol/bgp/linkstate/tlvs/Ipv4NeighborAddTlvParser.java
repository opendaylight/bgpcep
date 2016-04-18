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
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.NlriType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.ObjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.LinkCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptorsBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ipv4NeighborAddTlvParser implements NlriSubTlvObjectParser, NlriSubTlvObjectSerializer, SubTlvLinkBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4NeighborAddTlvParser.class);

    public static final int IPV4_NEIGHBOR_ADDRESS = 260;

    @Override
    public Object parseNlriSubTlvObject(final ByteBuf value, final NlriType nlriType) throws BGPParsingException {
        final Ipv4InterfaceIdentifier ripv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForByteBuf(value));
        LOG.debug("Parsed IPv4 neighbor address {}.", ripv4);
        return ripv4;
    }

    @Override
    public void serializeNlriSubTlvObject(final ObjectType nlriTypeCase, final NodeIdentifier qNameId, final ByteBuf buffer) {
        final LinkDescriptors ldescriptor = ((LinkCase)nlriTypeCase).getLinkDescriptors();
        if (ldescriptor.getIpv4NeighborAddress() != null) {
            TlvUtil.writeTLV(IPV4_NEIGHBOR_ADDRESS, Ipv4Util.byteBufForAddress(ldescriptor.getIpv4NeighborAddress()), buffer);
        }
    }

    @Override
    public void buildLinkDescriptor(final Object subTlvObject, final Builder<?> tlvBuilder) {
        ((LinkDescriptorsBuilder) tlvBuilder).setIpv4NeighborAddress((Ipv4InterfaceIdentifier) subTlvObject);
    }
}
