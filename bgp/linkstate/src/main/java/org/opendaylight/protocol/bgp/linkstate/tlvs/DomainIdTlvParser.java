/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.tlvs;

import com.google.common.primitives.UnsignedInteger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.DomainIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.linkstate.object.type.node._case.NodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DomainIdTlvParser implements LinkstateTlvParser<DomainIdentifier>, LinkstateTlvSerializer<DomainIdentifier> {

    private static final Logger LOG = LoggerFactory.getLogger(DomainIdTlvParser.class);

    public static final int BGP_LS_ID = 513;

    public static final QName DOMAIN_ID_QNAME = QName.create(NodeDescriptors.QNAME, "domain-id").intern();

    @Override
    public void serializeTlvBody(DomainIdentifier tlv, ByteBuf body, final QName qName) {
        if (tlv != null) {
            TlvUtil.writeTLV(BGP_LS_ID, Unpooled.copyInt(UnsignedInteger.valueOf(tlv.getValue()).intValue()), body);
        }
    }

    @Override
    public DomainIdentifier parseTlvBody(ByteBuf value) throws BGPParsingException {
        final DomainIdentifier bgpId = new DomainIdentifier(value.readUnsignedInt());
        LOG.debug("Parsed {}", bgpId);
        return bgpId;
    }

}
