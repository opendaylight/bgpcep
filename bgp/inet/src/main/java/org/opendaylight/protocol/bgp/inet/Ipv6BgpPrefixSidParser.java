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
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvSerializer;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.Ipv6SidTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;

public final class Ipv6BgpPrefixSidParser implements BgpPrefixSidTlvParser, BgpPrefixSidTlvSerializer {

    public static final int IPV6_SID_TYPE = 2;
    private static final int RESERVED = 1;
    private static final int FLAGS_SIZE = 2;
    private static final int PROCESS_IPV6_HEADER_FLAG = 0;

    @Override
    public void serializeBgpPrefixSidTlv(final BgpPrefixSidTlv tlv, final ByteBuf bytes) {
        Preconditions.checkArgument(tlv instanceof Ipv6SidTlv, "Incoming TLV is not Ipv6SidTlv");
        final Ipv6SidTlv ipv6Tlv = (Ipv6SidTlv) tlv;
        bytes.writeByte(IPV6_SID_TYPE);
        bytes.writeShort(RESERVED + FLAGS_SIZE);
        bytes.writeZero(RESERVED);
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(PROCESS_IPV6_HEADER_FLAG, ipv6Tlv.isProcessIpv6HeadAbility());
        flags.toByteBuf(bytes);
    }

    @Override
    public BgpPrefixSidTlv parseBgpPrefixSidTlv(final int type, final ByteBuf buffer) {
        Preconditions.checkArgument(type == IPV6_SID_TYPE, "BGP prefix SID TLV type is not Ipv6 SID TLV.");
        final int length = buffer.readUnsignedShort();
        Preconditions.checkState(length <= buffer.readableBytes(), "Length of IPv6 SID tlv exceeds readable bytes of income.");
        buffer.readBytes(RESERVED);
        final boolean canProcessIpv6Header = BitArray.valueOf(buffer, FLAGS_SIZE).get(PROCESS_IPV6_HEADER_FLAG);
        return new Ipv6SidTlv() {
            @Override
            public Boolean isProcessIpv6HeadAbility() {
                return canProcessIpv6Header;
            }
            @Override
            public <E extends Augmentation<Ipv6SidTlv>> E getAugmentation(final Class<E> augmentationType) {
                return null;
            }
            @Override
            public Class<? extends DataContainer> getImplementedInterface() {
                return Ipv6SidTlv.class;
            }
        };
    }
}
