/*
 * Copyright (c) 2024 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.srv6.sid._case.Srv6SidInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.linkstate.object.type.srv6.sid._case.Srv6SidInformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.Srv6Sid;
import org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.YangModuleInfoImpl;
import org.opendaylight.yangtools.yang.common.QName;

public final class SRv6SidInformationTlvParser implements LinkstateTlvParser<Srv6SidInformation>,
        LinkstateTlvParser.LinkstateTlvSerializer<Srv6SidInformation> {

    private static final int SRV6_SID_INFORMATION = 518;
    private static final int SRV6_SID_LENGTH = 16;

    public static final QName SRV6_SID_INFORMATION_QNAME = YangModuleInfoImpl.qnameOf("srv6-sid-information");

    @Override
    public void serializeTlvBody(final Srv6SidInformation tlv, final ByteBuf body) {
        body.writeBytes(tlv.getSrv6SidTlv().getValue());
    }

    @Override
    public Srv6SidInformation parseTlvBody(final ByteBuf value) {
        return new Srv6SidInformationBuilder()
            .setSrv6SidTlv(new Srv6Sid(ByteArray.readBytes(value, SRV6_SID_LENGTH)))
            .build();
    }

    @Override
    public int getType() {
        return SRV6_SID_INFORMATION;
    }

    @Override
    public QName getTlvQName() {
        return SRV6_SID_INFORMATION_QNAME;
    }
}
