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
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuLabelIndexTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuLabelIndexTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;

public final class LabelIndexTlvParser implements BgpPrefixSidTlvParser, BgpPrefixSidTlvSerializer {

    static final int LABEL_INDEX_TYPE = 1;
    private static final int LABEL_INDEX_VALUE_LENGHT = 4;
    private static final int RESERVED = 1;
    private static final int LABEL_INDEX_FLAGS_BYTES = 2;

    @Override
    public void serializeBgpPrefixSidTlv(final BgpPrefixSidTlv tlv, final ByteBuf aggregator) {
        Preconditions.checkArgument(tlv instanceof LuLabelIndexTlv, "Incoming TLV is not LuLabelIndexTlv");
        final ByteBuf valueBuf = Unpooled.buffer(RESERVED + LABEL_INDEX_FLAGS_BYTES + LABEL_INDEX_VALUE_LENGHT);
        valueBuf.writeZero(RESERVED);
        valueBuf.writeZero(LABEL_INDEX_FLAGS_BYTES);
        valueBuf.writeInt(((LuLabelIndexTlv) tlv).getLabelIndexTlv().intValue());
        BgpPrefixSidTlvUtil.formatBgpPrefixSidTlv(LABEL_INDEX_TYPE, valueBuf, aggregator);
    }

    @Override
    public BgpPrefixSidTlv parseBgpPrefixSidTlv(final ByteBuf buffer) {
        buffer.readBytes(RESERVED);
        buffer.readBytes(LABEL_INDEX_FLAGS_BYTES);
        final Long value = buffer.readUnsignedInt();
        return new LuLabelIndexTlvBuilder().setLabelIndexTlv(value).build();
    }

}
