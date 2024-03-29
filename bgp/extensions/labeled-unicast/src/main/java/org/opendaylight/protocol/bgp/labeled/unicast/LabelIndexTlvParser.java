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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuLabelIndexTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev180329.update.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.bgp.prefix.sid.tlv.LuLabelIndexTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

final class LabelIndexTlvParser implements BgpPrefixSidTlvParser, BgpPrefixSidTlvSerializer {
    private static final int LABEL_INDEX_TYPE = 1;
    private static final int RESERVED = 1;
    private static final int LABEL_INDEX_FLAGS_BYTES = 2;

    @Override
    public void serializeBgpPrefixSidTlv(final BgpPrefixSidTlv tlv, final ByteBuf valueBuf) {
        Preconditions.checkArgument(tlv instanceof LuLabelIndexTlv, "Incoming TLV is not LuLabelIndexTlv");
        valueBuf.writeZero(RESERVED + LABEL_INDEX_FLAGS_BYTES);
        valueBuf.writeInt(((LuLabelIndexTlv) tlv).getLabelIndexTlv().intValue());
    }

    @Override
    public LuLabelIndexTlv parseBgpPrefixSidTlv(final ByteBuf buffer) {
        buffer.skipBytes(RESERVED + LABEL_INDEX_FLAGS_BYTES);
        return new LuLabelIndexTlvBuilder().setLabelIndexTlv(ByteBufUtils.readUint32(buffer)).build();
    }

    @Override
    public int getType() {
        return LABEL_INDEX_TYPE;
    }
}
