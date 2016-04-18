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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.link._case.LinkDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ipv6IFaceAddTlvParser implements LinkstateTlvParser<Ipv6InterfaceIdentifier>, LinkstateTlvSerializer<Ipv6InterfaceIdentifier> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv6IFaceAddTlvParser.class);

    public static final int IPV6_IFACE_ADDRESS = 261;

    public static final QName IPV6_IFACE_ADDRESS_QNAME = QName.create(LinkDescriptors.QNAME, "ipv6-interface-address").intern();


    @Override
    public void serializeTlvBody(Ipv6InterfaceIdentifier tlv, ByteBuf body, final QName qName) {
        if (tlv != null) {
            TlvUtil.writeTLV(IPV6_IFACE_ADDRESS, Ipv6Util.byteBufForAddress(tlv), body);
        }
    }

    @Override
    public Ipv6InterfaceIdentifier parseTlvBody(ByteBuf value) throws BGPParsingException {
        final Ipv6InterfaceIdentifier lipv6 = new Ipv6InterfaceIdentifier(Ipv6Util.addressForByteBuf(value));
        LOG.debug("Parsed IPv6 interface address {}.", lipv6);
        return lipv6;
    }

}

