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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.epe.rev150622.EpeNodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpRouterIdTlvParser implements LinkstateTlvParser<Ipv4Address>, LinkstateTlvSerializer<Ipv4Address> {

    private static final Logger LOG = LoggerFactory.getLogger(BgpRouterIdTlvParser.class);

    public static final int BGP_ROUTER_ID = 516;

    public static final QName BGP_ROUTER_ID_QNAME = QName.create(EpeNodeDescriptors.QNAME, "bgp-router-id").intern();


    @Override
    public void serializeTlvBody(Ipv4Address tlv, ByteBuf body, final QName qName) {
        if (tlv != null) {
            TlvUtil.writeTLV(BGP_ROUTER_ID, Ipv4Util.byteBufForAddress(tlv), body);
        }
    }

    @Override
    public Ipv4Address parseTlvBody(ByteBuf value) throws BGPParsingException {
        final Ipv4Address bgpRouterId = Ipv4Util.addressForByteBuf(value);
        LOG.debug("Parsed BGP Router Identifier {}", bgpRouterId);
        return bgpRouterId;
    }

}
