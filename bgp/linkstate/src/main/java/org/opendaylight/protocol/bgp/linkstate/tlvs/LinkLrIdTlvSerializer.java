/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkIdentifier;
import org.opendaylight.yangtools.yang.common.QName;


public final class LinkLrIdTlvSerializer implements LinkstateTlvSerializer<LinkIdentifier> {

    public static final int LINK_LR_IDENTIFIERS = 258;

    @Override
    public void serializeTlvBody(LinkIdentifier tlv, ByteBuf body, final QName qName) {
        if (tlv.getLinkLocalIdentifier() != null && tlv.getLinkRemoteIdentifier() != null) {
            final ByteBuf identifierBuf = Unpooled.buffer();
            identifierBuf.writeInt(tlv.getLinkLocalIdentifier().intValue());
            identifierBuf.writeInt(tlv.getLinkRemoteIdentifier().intValue());
            TlvUtil.writeTLV(LINK_LR_IDENTIFIERS, identifierBuf, body);
        }
    }
}