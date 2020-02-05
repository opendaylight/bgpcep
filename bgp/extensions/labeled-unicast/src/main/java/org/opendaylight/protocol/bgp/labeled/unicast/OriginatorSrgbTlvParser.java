/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.labeled.unicast;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.mdsal.uint24.netty.Uint24ByteBufUtils;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.Srgb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.originator.srgb.tlv.SrgbValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.originator.srgb.tlv.SrgbValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuOriginatorSrgbTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuOriginatorSrgbTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;

final class OriginatorSrgbTlvParser implements BgpPrefixSidTlvParser, BgpPrefixSidTlvSerializer {

    private static final int ORIGINATOR_SRGB_TYPE = 3;
    private static final int ORIGINATOR_FLAGS_BYTES = 2;
    private static final int SRGB_LENGTH = 6;

    @Override
    public LuOriginatorSrgbTlv parseBgpPrefixSidTlv(final ByteBuf buffer) {
        buffer.readBytes(ORIGINATOR_FLAGS_BYTES);
        final List<SrgbValue> srgbList = parseSrgbs(buffer);
        return new LuOriginatorSrgbTlvBuilder().setSrgbValue(srgbList).build();
    }

    private static List<SrgbValue> parseSrgbs(final ByteBuf buffer) {
        Preconditions.checkState(buffer.readableBytes() % SRGB_LENGTH == 0,
                "Number of SRGBs does not fit available bytes.");
        final List<SrgbValue> ret = new ArrayList<>();
        while (buffer.isReadable()) {
            ret.add(new SrgbValueBuilder().setBase(new Srgb(Uint24ByteBufUtils.readUint24(buffer)))
                .setRange(new Srgb(Uint24ByteBufUtils.readUint24(buffer))).build());
        }
        return ret;
    }

    @Override
    public void serializeBgpPrefixSidTlv(final BgpPrefixSidTlv tlv, final ByteBuf valueBuf) {
        Preconditions.checkArgument(tlv instanceof LuOriginatorSrgbTlv, "Incoming TLV is not LuOriginatorSrgbTlv");
        final LuOriginatorSrgbTlv luTlv = (LuOriginatorSrgbTlv) tlv;
        valueBuf.writeZero(ORIGINATOR_FLAGS_BYTES);
        for (final SrgbValue val : luTlv.getSrgbValue()) {
            Uint24ByteBufUtils.writeUint24(valueBuf, val.getBase());
            Uint24ByteBufUtils.writeUint24(valueBuf, val.getRange());
        }
    }

    @Override
    public int getType() {
        return ORIGINATOR_SRGB_TYPE;
    }
}
