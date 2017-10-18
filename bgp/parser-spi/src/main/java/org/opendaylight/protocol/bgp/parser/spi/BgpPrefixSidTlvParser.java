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
 * Common interface for BGP prefix SID TLVs parser implementations.
 */
public interface BgpPrefixSidTlvParser {
    /**
     * Method for parsing specific types of TLVs from incoming buffer
     *
     * @param buffer with TLV bytes
     * @return instance of specific TLV
     */
    BgpPrefixSidTlv parseBgpPrefixSidTlv(ByteBuf buffer);
}
