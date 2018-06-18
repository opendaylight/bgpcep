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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.epe.rev150622.EpeNodeDescriptors;
import org.opendaylight.yangtools.yang.common.QName;

public final class MemAsNumTlvParser implements LinkstateTlvParser<AsNumber>, LinkstateTlvParser.LinkstateTlvSerializer<AsNumber> {

    private static final int MEMBER_AS_NUMBER = 517;

    public static final QName MEMBER_AS_NUMBER_QNAME = QName.create(EpeNodeDescriptors.QNAME, "member-asn").intern();

    @Override
    public void serializeTlvBody(final AsNumber tlv, final ByteBuf body) {
        ByteBufWriteUtil.writeUnsignedInt(tlv.getValue(), body);
    }

    @Override
    public AsNumber parseTlvBody(final ByteBuf value) {
        return new AsNumber(value.readUnsignedInt());
    }

    @Override
    public int getType() {
        return MEMBER_AS_NUMBER;
    }

    @Override
    public QName getTlvQName() {
        return MEMBER_AS_NUMBER_QNAME;
    }

}
