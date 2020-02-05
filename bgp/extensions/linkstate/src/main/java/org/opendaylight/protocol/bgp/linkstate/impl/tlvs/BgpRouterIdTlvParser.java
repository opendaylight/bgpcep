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
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.epe.rev200120.EpeNodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public final class BgpRouterIdTlvParser implements LinkstateTlvParser<Ipv4AddressNoZone>,
        LinkstateTlvParser.LinkstateTlvSerializer<Ipv4AddressNoZone> {
    private static final int BGP_ROUTER_ID = 516;

    public static final QName BGP_ROUTER_ID_QNAME = QName.create(EpeNodeDescriptors.QNAME, "bgp-router-id").intern();

    @Override
    public void serializeTlvBody(final Ipv4AddressNoZone tlv, final ByteBuf body) {
        Ipv4Util.writeIpv4Address(tlv, body);
    }

    @Override
    public Ipv4AddressNoZone parseTlvBody(final ByteBuf value) {
        return Ipv4Util.addressForByteBuf(value);
    }

    @Override
    public int getType() {
        return BGP_ROUTER_ID;
    }

    @Override
    public QName getTlvQName() {
        return BGP_ROUTER_ID_QNAME;
    }
}
