/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev180329.LinkLrIdentifiers;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class LinkIdTlvParser implements LinkstateTlvParser.LinkstateTlvSerializer<LinkLrIdentifiers>,
        LinkstateTlvParser<LinkLrIdentifiers> {

    private static final int LINK_LR_IDENTIFIERS = 258;

    @Override
    public void serializeTlvBody(final LinkLrIdentifiers tlv, final ByteBuf body) {
        ByteBufWriteUtil.writeUnsignedInt(tlv.getLinkLocalIdentifier(), body);
        ByteBufWriteUtil.writeUnsignedInt(tlv.getLinkRemoteIdentifier(), body);
    }

    @Override
    public LinkLrIdentifiers parseTlvBody(final ByteBuf value) {
        final Uint32 localId = Uint32.valueOf(value.readUnsignedInt());
        final Uint32 remoteId = Uint32.valueOf(value.readUnsignedInt());
        return new LinkLrIdentifiers() {
            @Override
            public Class<LinkLrIdentifiers> implementedInterface() {
                return LinkLrIdentifiers.class;
            }

            @Override
            public Uint32 getLinkRemoteIdentifier() {
                return remoteId;
            }

            @Override
            public Uint32 getLinkLocalIdentifier() {
                return localId;
            }
        };
    }

    @Override
    public QName getTlvQName() {
        return LinkLrIdentifiers.QNAME;
    }

    @Override
    public int getType() {
        return LINK_LR_IDENTIFIERS;
    }
}
