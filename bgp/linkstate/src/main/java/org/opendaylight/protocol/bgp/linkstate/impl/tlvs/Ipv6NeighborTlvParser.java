/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.Ipv6InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public final class Ipv6NeighborTlvParser implements LinkstateTlvParser<Ipv6InterfaceIdentifier>, LinkstateTlvParser.LinkstateTlvSerializer<Ipv6InterfaceIdentifier> {

    private static final int IPV6_NEIGHBOR_ADDRESS = 262;

    public static final QName IPV6_NEIGHBOR_ADDRESS_QNAME = QName.create(LinkDescriptors.QNAME, "ipv6-neighbor-address").intern();


    @Override
    public void serializeTlvBody(final Ipv6InterfaceIdentifier tlv, final ByteBuf body) {
        ByteBufWriteUtil.writeIpv6Address(tlv, body);
    }

    @Override
    public Ipv6InterfaceIdentifier parseTlvBody(final ByteBuf value) {
        return new Ipv6InterfaceIdentifier(Ipv6Util.addressForByteBuf(value));
    }

    @Override
    public int getType() {
        return IPV6_NEIGHBOR_ADDRESS;
    }

    @Override
    public QName getTlvQName() {
        return IPV6_NEIGHBOR_ADDRESS_QNAME;
    }

}


