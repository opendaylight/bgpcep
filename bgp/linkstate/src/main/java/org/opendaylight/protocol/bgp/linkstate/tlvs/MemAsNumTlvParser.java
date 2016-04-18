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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.epe.rev150622.EpeNodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MemAsNumTlvParser implements LinkstateTlvParser<AsNumber>, LinkstateTlvSerializer<AsNumber> {

    private static final Logger LOG = LoggerFactory.getLogger(MemAsNumTlvParser.class);

    public static final int MEMBER_AS_NUMBER = 517;

    public static final QName MEMBER_AS_NUMBER_QNAME = QName.create(EpeNodeDescriptors.QNAME, "member-asn").intern();


    @Override
    public void serializeTlvBody(AsNumber tlv, ByteBuf body, final QName qName) {
        if (tlv != null) {
            TlvUtil.writeTLV(MEMBER_AS_NUMBER, Unpooled.copyInt(UnsignedInteger.valueOf(tlv.getValue()).intValue()), body);
        }
    }

    @Override
    public AsNumber parseTlvBody(ByteBuf value) throws BGPParsingException {
        final AsNumber memberAsn = new AsNumber(value.readUnsignedInt());
        LOG.debug("Parsed Member AsNumber {}", memberAsn);
        return memberAsn;
    }

}
