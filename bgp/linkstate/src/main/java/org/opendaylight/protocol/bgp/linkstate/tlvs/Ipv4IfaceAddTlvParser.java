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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ipv4IfaceAddTlvParser implements LinkstateTlvParser<Ipv4InterfaceIdentifier>, LinkstateTlvSerializer<Ipv4InterfaceIdentifier> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4IfaceAddTlvParser.class);

    public static final int IPV4_IFACE_ADDRESS = 259;

    public static final QName IPV4_IFACE_ADDRESS_QNAME = QName.create(LinkDescriptors.QNAME, "ipv4-interface-address").intern();


    @Override
    public void serializeTlvBody(Ipv4InterfaceIdentifier tlv, ByteBuf body, final QName qName) {
        if (tlv != null) {
            TlvUtil.writeTLV(IPV4_IFACE_ADDRESS, Ipv4Util.byteBufForAddress(tlv), body);
        }
    }

    @Override
    public Ipv4InterfaceIdentifier parseTlvBody(ByteBuf value) throws BGPParsingException {
        final Ipv4InterfaceIdentifier lipv4 = new Ipv4InterfaceIdentifier(Ipv4Util.addressForByteBuf(value));
        LOG.debug("Parsed IPv4 interface address {}.", lipv4);
        return lipv4;
    }

}
