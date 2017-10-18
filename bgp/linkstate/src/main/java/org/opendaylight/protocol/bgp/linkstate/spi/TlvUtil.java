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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.prefix._case.PrefixDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TlvUtil {

    public static final int TOPOLOGY_ID_OFFSET = 0x3fff;
    public static final int MULTI_TOPOLOGY_ID = 263;
    public static final NodeIdentifier MULTI_TOPOLOGY_NID = new NodeIdentifier(QName.create(PrefixDescriptors.QNAME, "topology-identifier").intern());
    public static final int LOCAL_IPV4_ROUTER_ID = 1028;
    public static final int LOCAL_IPV6_ROUTER_ID = 1029;
    private static final Logger LOG = LoggerFactory.getLogger(TlvUtil.class);
    private TlvUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Util method for writing TLV header.
     *
     * @param type TLV type (2B)
     * @param value TLV value (2B)
     * @param byteAggregator final ByteBuf where the tlv should be serialized
     */
    public static void writeTLV(final int type, final ByteBuf value, final ByteBuf byteAggregator) {
        byteAggregator.writeShort(type);
        byteAggregator.writeShort(value.writerIndex());
        byteAggregator.writeBytes(value);
        if (LOG.isDebugEnabled()) {
            value.readerIndex(0);
            LOG.debug("Serialized tlv type {} to: {}", type, ByteBufUtil.hexDump(value));
        }
    }
}
