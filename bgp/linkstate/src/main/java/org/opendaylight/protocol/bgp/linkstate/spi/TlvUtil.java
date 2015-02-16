/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.spi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TlvUtil {

    private TlvUtil() {
        throw new UnsupportedOperationException();
    }

    private static final Logger LOG = LoggerFactory.getLogger(TlvUtil.class);

    public static final int TOPOLOGY_ID_OFFSET = 0x3fff;

    public static final int MULTI_TOPOLOGY_ID = 263;
    public static final int LOCAL_IPV4_ROUTER_ID = 1028;
    public static final int LOCAL_IPV6_ROUTER_ID = 1029;

    /**
     * Util method for writing TLV header.
     * @param type TLV type (2B)
     * @param value TLV value (2B)
     * @param byteAggregator final ByteBuf where the tlv should be serialized
     */
    public static void writeTLV(final int type, final ByteBuf value, final ByteBuf byteAggregator){
        byteAggregator.writeShort(type);
        byteAggregator.writeShort(value.writerIndex());
        byteAggregator.writeBytes(value);
        value.readerIndex(0);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Serialized tlv type {} to: {}", type, ByteBufUtil.hexDump(value));
        }
    }

    /**
     * Util method for writing Segment routing TLV header.
     * @param type TLV type (1B)
     * @param value TLV value (1B)
     * @param byteAggregator final ByteBuf where the tlv should be serialized
     */
    public static void writeSrTLV(final int type, final ByteBuf value, final ByteBuf byteAggregator){
        byteAggregator.writeByte(type);
        byteAggregator.writeByte(value.writerIndex());
        byteAggregator.writeBytes(value);
        value.readerIndex(0);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Serialized tlv type {} to: {}", type, ByteBufUtil.hexDump(value));
        }
    }
}
