/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IpReachTlvParser implements LinkstateTlvSerializer<IpPrefix> {

    private static final Logger LOG = LoggerFactory.getLogger(IpReachTlvParser.class);

    public static final int IP_REACHABILITY = 265;

    public static final QName IP_REACHABILITY_QNAME = QName.create(PrefixDescriptors.QNAME, "ip-reachability-information").intern();

    @Override
    public void serializeTlvBody(IpPrefix tlv, ByteBuf body, final QName qName) {
        if (tlv != null) {
            final ByteBuf buf;
            if (tlv.getIpv4Prefix() != null) {
                buf = Unpooled.buffer(Ipv4Util.IP4_LENGTH + 1);
                ByteBufWriteUtil.writeMinimalPrefix(tlv.getIpv4Prefix(), buf);
            } else if (tlv.getIpv6Prefix() != null) {
                buf = Unpooled.buffer(Ipv6Util.IPV6_LENGTH + 1);
                ByteBufWriteUtil.writeMinimalPrefix(tlv.getIpv6Prefix(), buf);
            } else {
                buf = null;
            }
            TlvUtil.writeTLV(IP_REACHABILITY, buf, body);
        }
    }

    public static IpPrefix parseTlvBody(final ByteBuf value, final boolean ipv4) throws BGPParsingException {
        final IpPrefix prefix = (ipv4) ? new IpPrefix(Ipv4Util.prefixForByteBuf(value)) : new IpPrefix(Ipv6Util.prefixForByteBuf(value));
        if (LOG.isTraceEnabled()) {
            LOG.trace("Parsed reachability info: {}", prefix);
        }
        return prefix;
    }
}
