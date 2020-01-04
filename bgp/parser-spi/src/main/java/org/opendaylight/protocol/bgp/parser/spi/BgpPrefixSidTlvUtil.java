/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;

public final class BgpPrefixSidTlvUtil {
    private BgpPrefixSidTlvUtil() {
        // Hidden on purpose
    }

    /**
     * Utilized method for serialization of BGP prefix SID TLV.
     *
     * @param type of TLV
     * @param value of TLV
     * @param buffer output aggregator
     */
    public static void formatBgpPrefixSidTlv(final int type, final ByteBuf value, final ByteBuf buffer) {
        buffer.writeByte(type);
        buffer.writeShort(value.writerIndex());
        buffer.writeBytes(value);
    }
}
