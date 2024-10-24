/*
 * Copyright (c) 2024 Orange. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.linkstate.impl.tlvs;

import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.readUint8;
import static org.opendaylight.yangtools.yang.common.netty.ByteBufUtils.writeUint8;

import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.LinkstateTlvParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.srv6.sid.subtlvs.Srv6SidStructure;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.srv6.sid.subtlvs.Srv6SidStructureBuilder;
import org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.YangModuleInfoImpl;
import org.opendaylight.yangtools.yang.common.QName;

public class SRv6SidStructureTlvParser implements LinkstateTlvParser<Srv6SidStructure>,
        LinkstateTlvParser.LinkstateTlvSerializer<Srv6SidStructure> {
    private static final int SRV6_SID_STRUCTURE = 1252;

    public static final QName SRV6_SID_STRUCTURE_QNAME = YangModuleInfoImpl.qnameOf("srv6-sid-structure");

    @Override
    public void serializeTlvBody(final Srv6SidStructure tlv, final ByteBuf body) {
        writeUint8(body, tlv.getLocatorBlockLength());
        writeUint8(body, tlv.getLocatorNodeLength());
        writeUint8(body, tlv.getFunctionLength());
        writeUint8(body, tlv.getArgumentLength());
    }

    @Override
    public Srv6SidStructure parseTlvBody(final ByteBuf value) {
        return new Srv6SidStructureBuilder()
            .setLocatorBlockLength(readUint8(value))
            .setLocatorNodeLength(readUint8(value))
            .setFunctionLength(readUint8(value))
            .setArgumentLength(readUint8(value))
            .build();
    }

    @Override
    public int getType() {
        return SRV6_SID_STRUCTURE;
    }

    @Override
    public QName getTlvQName() {
        return SRV6_SID_STRUCTURE_QNAME;
    }
}
