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
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvParser;
import org.opendaylight.protocol.bgp.parser.spi.BgpPrefixSidTlvSerializer;
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
        aggregator.writeByte(LABEL_INDEX_TYPE);
        aggregator.writeShort(LABEL_INDEX_VALUE_LENGHT + RESERVED + LABEL_INDEX_FLAGS_BYTES);
        aggregator.writeZero(RESERVED);
        aggregator.writeZero(LABEL_INDEX_FLAGS_BYTES);
        aggregator.writeInt(((LuLabelIndexTlv) tlv).getLabelIndexTlv().intValue());
    }

    @Override
    public BgpPrefixSidTlv parseBgpPrefixSidTlv(final ByteBuf buffer) {
        final int length = buffer.readUnsignedShort();
        Preconditions.checkState(length <= buffer.readableBytes(), "Length of Label Index tlv exceeds readable bytes of income.");
        buffer.readBytes(RESERVED);
        buffer.readBytes(LABEL_INDEX_FLAGS_BYTES);
        final Long value = buffer.readUnsignedInt();
        return new LuLabelIndexTlvBuilder().setLabelIndexTlv(value).build();
    }

}
