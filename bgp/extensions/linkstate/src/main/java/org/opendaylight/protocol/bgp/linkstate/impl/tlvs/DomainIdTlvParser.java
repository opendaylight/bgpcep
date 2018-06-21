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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public final class DomainIdTlvParser implements LinkstateTlvParser<DomainIdentifier>, LinkstateTlvParser.LinkstateTlvSerializer<DomainIdentifier> {

    private static final int BGP_LS_ID = 513;

    public static final QName DOMAIN_ID_QNAME = QName.create(NodeDescriptors.QNAME, "domain-id").intern();

    @Override
    public void serializeTlvBody(final DomainIdentifier tlv, final ByteBuf body) {
        ByteBufWriteUtil.writeUnsignedInt(tlv.getValue(), body);
    }

    @Override
    public DomainIdentifier parseTlvBody(final ByteBuf value) {
        return new DomainIdentifier(value.readUnsignedInt());
    }

    @Override
    public int getType() {
        return BGP_LS_ID;
    }

    @Override
    public QName getTlvQName() {
        return DOMAIN_ID_QNAME;
    }

}
