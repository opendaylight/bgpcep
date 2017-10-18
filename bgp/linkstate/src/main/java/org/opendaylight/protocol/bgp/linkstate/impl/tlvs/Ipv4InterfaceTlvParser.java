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
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public final class Ipv4InterfaceTlvParser implements LinkstateTlvParser<Ipv4InterfaceIdentifier>, LinkstateTlvParser.LinkstateTlvSerializer<Ipv4InterfaceIdentifier> {

    private static final int IPV4_IFACE_ADDRESS = 259;

    public static final QName IPV4_IFACE_ADDRESS_QNAME = QName.create(LinkDescriptors.QNAME, "ipv4-interface-address").intern();


    @Override
    public void serializeTlvBody(final Ipv4InterfaceIdentifier tlv, final ByteBuf body) {
        ByteBufWriteUtil.writeIpv4Address(tlv, body);
    }

    @Override
    public Ipv4InterfaceIdentifier parseTlvBody(final ByteBuf value) {
        return new Ipv4InterfaceIdentifier(Ipv4Util.addressForByteBuf(value));
    }

    @Override
    public int getType() {
        return IPV4_IFACE_ADDRESS;
    }

    @Override
    public QName getTlvQName() {
        return IPV4_IFACE_ADDRESS_QNAME;
    }

}
