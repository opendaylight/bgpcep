/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.inet;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvUtil;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.Ipv6SidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.Ipv6SidTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;

public final class Ipv6BgpPrefixSidParser implements BgpPrefixSidTlvParser, BgpPrefixSidTlvSerializer {

    static final int IPV6_SID_TYPE = 2;
    private static final int RESERVED = 1;
    private static final int FLAGS_SIZE = 2;
    private static final int PROCESS_IPV6_HEADER_FLAG = 0;

    @Override
    public void serializeBgpPrefixSidTlv(final BgpPrefixSidTlv tlv, final ByteBuf bytes) {
        Preconditions.checkArgument(tlv instanceof Ipv6SidTlv, "Incoming TLV is not Ipv6SidTlv");
        final Ipv6SidTlv ipv6Tlv = (Ipv6SidTlv) tlv;
        final ByteBuf valueBuf = Unpooled.buffer(RESERVED + FLAGS_SIZE);
        valueBuf.writeZero(RESERVED);
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(PROCESS_IPV6_HEADER_FLAG, ipv6Tlv.isProcessIpv6HeadAbility());
        flags.toByteBuf(valueBuf);
        BgpPrefixSidTlvUtil.formatBgpPrefixSidTlv(IPV6_SID_TYPE, valueBuf, bytes);
    }

    @Override
    public BgpPrefixSidTlv parseBgpPrefixSidTlv(final ByteBuf buffer) {
        buffer.readBytes(RESERVED);
        final boolean canProcessIpv6Header = BitArray.valueOf(buffer, FLAGS_SIZE).get(PROCESS_IPV6_HEADER_FLAG);
        return new Ipv6SidTlvBuilder().setProcessIpv6HeadAbility(canProcessIpv6Header).build();
    }
}
