/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Timestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Peer.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader.InboundPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeaderBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public abstract class AbstractBmpPerPeerMessageParser extends AbstractBmpMessageParser {

    private static final int L_FLAG_POS = 1;
    private static final int V_FLAG_POS = 0;
    private static final int FLAGS_SIZE = 8;
    private static final int POST_FLAG_PADDING = 2;
    private static final int PEER_DISTINGUISHER_SIZE = 4;
    private static final int PER_PEER_HEADER_SIZE = 32;

    private final MessageRegistry bgpMssageRegistry;

    public AbstractBmpPerPeerMessageParser(final MessageRegistry bgpMssageRegistry) {
        this.bgpMssageRegistry = Preconditions.checkNotNull(bgpMssageRegistry);
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        if (message instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerHeader) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerHeader messageWithPerPeerHeader =
                (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerHeader) message;
            serializePerPeerHeader(messageWithPerPeerHeader.getPeerHeader(), buffer);
        }
    }

    protected final PeerHeader parsePerPeerHeader(final ByteBuf bytes) {
        Preconditions.checkArgument(bytes.readableBytes() >= PER_PEER_HEADER_SIZE);
        final PeerHeaderBuilder phBuilder = new PeerHeaderBuilder();
        phBuilder.setType(Type.forValue(bytes.readByte()));
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);
        phBuilder.setInboundPolicy(InboundPolicy.forValue(flags.get(L_FLAG_POS) ? 1 : 0));
        phBuilder.setIpv4(!flags.get(V_FLAG_POS));
        bytes.skipBytes(POST_FLAG_PADDING);
        if (phBuilder.getType().equals(Type.L3vpn)) {
            phBuilder.setDistinguisher(PeerDistinguisherUtil.parsePeerDistingisher(bytes));
        } else {
            bytes.skipBytes(PEER_DISTINGUISHER_SIZE);
        }
        if (phBuilder.isIpv4()) {
            bytes.skipBytes(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
            phBuilder.setAddress(new IpAddress(Ipv4Util.addressForByteBuf(bytes)));
        } else {
            phBuilder.setAddress(new IpAddress(Ipv6Util.addressForByteBuf(bytes)));
        }
        phBuilder.setAs(new AsNumber(bytes.readUnsignedInt()));
        phBuilder.setBgpId(Ipv4Util.addressForByteBuf(bytes));
        phBuilder.setTimestampSec(new Timestamp(bytes.readUnsignedInt()));
        phBuilder.setTimestampMicro(new Timestamp(bytes.readUnsignedInt()));
        return phBuilder.build();
    }

    protected void serializePerPeerHeader(final PeerHeader peerHeader, final ByteBuf output) {
        Preconditions.checkArgument(peerHeader != null, "Per-peer header cannot be null.");
        output.writeByte(peerHeader.getType().getIntValue());
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(L_FLAG_POS, peerHeader.getType().getIntValue() == 0 ? false : true);
        flags.set(V_FLAG_POS, !peerHeader.isIpv4());
        flags.toByteBuf(output);
        output.writeZero(POST_FLAG_PADDING);
        if (peerHeader.getType().equals(Type.L3vpn)) {
            PeerDistinguisherUtil.serializePeerDistinguisher(peerHeader.getDistinguisher(), output);
        } else {
            output.writeZero(PEER_DISTINGUISHER_SIZE);
        }
        if (!peerHeader.isIpv4()) {
            ByteBufWriteUtil.writeIpv6Address(peerHeader.getAddress().getIpv6Address(), output);
        } else {
            output.writeZero(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
            ByteBufWriteUtil.writeIpv4Address(peerHeader.getAddress().getIpv4Address(), output);
        }
        ByteBufWriteUtil.writeUnsignedInt(peerHeader.getAs().getValue(), output);
        ByteBufWriteUtil.writeIpv4Address(peerHeader.getBgpId(), output);
        if (peerHeader.getTimestampSec() != null) {
            ByteBufWriteUtil.writeUnsignedInt(peerHeader.getTimestampSec().getValue(), output);
        } else {
            output.writeZero(ByteBufWriteUtil.INT_BYTES_LENGTH);
        }
        if (peerHeader.getTimestampMicro() != null) {
            ByteBufWriteUtil.writeUnsignedInt(peerHeader.getTimestampMicro().getValue(), output);
        } else {
            output.writeZero(ByteBufWriteUtil.INT_BYTES_LENGTH);
        }
    }

    protected final MessageRegistry getBmpMessageRegistry() {
        return this.bgpMssageRegistry;
    }
}