/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.bgp.prefix.sid.bgp.prefix.sid.tlvs.BgpPrefixSidTlv;

/**
 * Interface for BGP prefix SID TLVs serializers implementations.
 */
public interface BgpPrefixSidTlvSerializer {
    /**
     * Method for serializing specific types of TLVs from incoming buffer
     *
     * @param tlv instance
     * @param bytes outcome serialized TLV
     */
    void serializeBgpPrefixSidTlv(BgpPrefixSidTlv tlv, ByteBuf bytes);

    int getType();
}
