/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.LinkLrIdentifiers;
import org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev241219.YangModuleInfoImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class LinkIdTlvParser implements LinkstateTlvParser.LinkstateTlvSerializer<LinkLrIdentifiers>,
        LinkstateTlvParser<LinkLrIdentifiers> {
    public static final int LINK_LR_IDENTIFIERS = 258;

    // FIXME: what exactly is this QName? it used to be LinkLrIdentifiers.QName, but that's a grouping, so what gives?
    @Deprecated
    public static final QName QNAME = YangModuleInfoImpl.qnameOf("link-lr-identifiers");

    @Override
    public void serializeTlvBody(final LinkLrIdentifiers tlv, final ByteBuf body) {
        ByteBufUtils.writeOrZero(body, tlv.getLinkLocalIdentifier());
        ByteBufUtils.writeOrZero(body, tlv.getLinkRemoteIdentifier());
    }

    @Override
    public LinkLrIdentifiers parseTlvBody(final ByteBuf value) {
        final Uint32 localId = ByteBufUtils.readUint32(value);
        final Uint32 remoteId = ByteBufUtils.readUint32(value);
        return new AnonymousLinkLrIdentifiers(localId, remoteId);
    }

    @Override
    public QName getTlvQName() {
        return QNAME;
    }

    @Override
    public int getType() {
        return LINK_LR_IDENTIFIERS;
    }

    private static final class AnonymousLinkLrIdentifiers implements LinkLrIdentifiers {
        private final Uint32 localId;
        private final Uint32 remoteId;

        AnonymousLinkLrIdentifiers(final Uint32 localId, final Uint32 remoteId) {
            this.localId = requireNonNull(localId);
            this.remoteId = requireNonNull(remoteId);
        }

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
    }
}
