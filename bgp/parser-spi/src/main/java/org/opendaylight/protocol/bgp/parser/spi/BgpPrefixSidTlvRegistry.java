/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171122.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;

/**
 * BGP prefix SID TLVs registry for encoding/decoding
 */
public interface BgpPrefixSidTlvRegistry {
    /**
     * Decode incoming TLV
     * @param type number of TLV
     * @param buffer contains bytes of TLV
     * @return instance of specific TLV
     */
    BgpPrefixSidTlv parseBgpPrefixSidTlv(int type, ByteBuf buffer);

    /**
     * Encode TLV instance
     * @param tlv instance
     * @param bytes encoded TLV outcome
     */
    void serializeBgpPrefixSidTlv(BgpPrefixSidTlv tlv, ByteBuf bytes);
}
