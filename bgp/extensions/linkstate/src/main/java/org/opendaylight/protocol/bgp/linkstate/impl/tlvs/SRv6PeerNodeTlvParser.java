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
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev200120.Ipv4InterfaceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.srv6.attributes.Srv6BgpPeerNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.srv6.attributes.Srv6BgpPeerNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.srv6.flags.FlagsBuilder;
import org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.rev200120.YangModuleInfoImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public final class SRv6PeerNodeTlvParser implements LinkstateTlvParser<Srv6BgpPeerNode>,
        LinkstateTlvParser.LinkstateTlvSerializer<Srv6BgpPeerNode> {

    private static final int SRV6_BGP_PEER_NODE = 1251;
    private static final int FLAGS_SIZE = 8;
    private static final int RESERVED = 2;
    private static final int BACKUP_FLAG = 0 ;
    private static final int SET_FLAG = 1;
    private static final int PERSISTENT_FLAG = 2;

    public static final QName SRV6_BGP_PEER_NODE_QNAME = YangModuleInfoImpl.qnameOf("srv6-bgp-peer-node");

    @Override
    public void serializeTlvBody(final Srv6BgpPeerNode tlv, final ByteBuf body) {
        final BitArray bs = new BitArray(FLAGS_SIZE);

        bs.set(BACKUP_FLAG, tlv.getFlags().getBackup());
        bs.set(SET_FLAG, tlv.getFlags().getSet());
        bs.set(PERSISTENT_FLAG, tlv.getFlags().getPersistent());
        bs.toByteBuf(body);
        writeUint8(body, tlv.getWeight());
        body.writeZero(RESERVED);
        ByteBufUtils.write(body, tlv.getPeerAsNumber().getValue());
        Ipv4Util.writeIpv4Address(tlv.getPeerBgpId(), body);
    }

    @Override
    public Srv6BgpPeerNode parseTlvBody(final ByteBuf value) {
        final Srv6BgpPeerNodeBuilder builder = new Srv6BgpPeerNodeBuilder();
        final BitArray flags = BitArray.valueOf(value, FLAGS_SIZE);

        builder.setFlags(new FlagsBuilder()
            .setBackup(flags.get(BACKUP_FLAG))
            .setSet(flags.get(SET_FLAG))
            .setPersistent(flags.get(PERSISTENT_FLAG))
            .build());
        builder.setWeight(readUint8(value));
        value.skipBytes(RESERVED);
        builder.setPeerAsNumber(new AsNumber(ByteBufUtils.readUint32(value)));
        builder.setPeerBgpId(new Ipv4InterfaceIdentifier(Ipv4Util.addressForByteBuf(value)));
        return builder.build();
    }

    @Override
    public int getType() {
        return SRV6_BGP_PEER_NODE;
    }

    @Override
    public QName getTlvQName() {
        return SRV6_BGP_PEER_NODE_QNAME;
    }
}
