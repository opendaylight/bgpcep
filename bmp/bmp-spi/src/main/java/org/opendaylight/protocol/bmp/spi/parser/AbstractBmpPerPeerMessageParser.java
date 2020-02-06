/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.spi.parser;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import org.opendaylight.bgp.concepts.RouteDistinguisherUtil;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Peer.PeerDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.header.PeerHeaderBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public abstract class AbstractBmpPerPeerMessageParser<T extends Builder<?>> extends AbstractBmpMessageWithTlvParser<T> {
    private static final int L_FLAG_POS = 1;
    private static final int V_FLAG_POS = 0;
    private static final int FLAGS_SIZE = 8;
    private static final int PEER_DISTINGUISHER_SIZE = 8;
    private static final int PER_PEER_HEADER_SIZE = 32;

    private final MessageRegistry bgpMssageRegistry;

    public AbstractBmpPerPeerMessageParser(final MessageRegistry bgpMssageRegistry) {
        this(bgpMssageRegistry, null);
    }

    public AbstractBmpPerPeerMessageParser(final MessageRegistry bgpMssageRegistry, final BmpTlvRegistry tlvRegistry) {
        super(tlvRegistry);
        this.bgpMssageRegistry = requireNonNull(bgpMssageRegistry);
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        if (message
                instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120
                .PeerHeader) {
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerHeader
                messageWithPerPeerHeader =
                (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerHeader)
                    message;
            serializePerPeerHeader(messageWithPerPeerHeader.getPeerHeader(), buffer);
        }
    }

    protected static PeerHeader parsePerPeerHeader(final ByteBuf bytes) {
        checkArgument(bytes.readableBytes() >= PER_PEER_HEADER_SIZE);
        final PeerHeaderBuilder phBuilder = new PeerHeaderBuilder();
        final PeerType peerType = PeerType.forValue(bytes.readByte());
        phBuilder.setType(peerType);
        final BitArray flags = BitArray.valueOf(bytes, FLAGS_SIZE);
        phBuilder.setAdjRibInType(AdjRibInType.forValue(flags.get(L_FLAG_POS) ? 1 : 0));
        phBuilder.setIpv4(!flags.get(V_FLAG_POS));
        switch (peerType) {
            case L3vpn:
                phBuilder.setPeerDistinguisher(
                        new PeerDistinguisher(RouteDistinguisherUtil.parseRouteDistinguisher(bytes)));
                break;
            case Local:
                phBuilder.setPeerDistinguisher(
                        new PeerDistinguisher(ByteArray.readBytes(bytes, PEER_DISTINGUISHER_SIZE)));
                break;
            case Global:
            default:
                bytes.skipBytes(PEER_DISTINGUISHER_SIZE);
                break;
        }
        if (phBuilder.isIpv4()) {
            bytes.skipBytes(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
            phBuilder.setAddress(new IpAddressNoZone(Ipv4Util.addressForByteBuf(bytes)));
        } else {
            phBuilder.setAddress(new IpAddressNoZone(Ipv6Util.addressForByteBuf(bytes)));
        }
        phBuilder.setAs(new AsNumber(ByteBufUtils.readUint32(bytes)));
        phBuilder.setBgpId(Ipv4Util.addressForByteBuf(bytes));
        phBuilder.setTimestampSec(new Timestamp(ByteBufUtils.readUint32(bytes)));
        phBuilder.setTimestampMicro(new Timestamp(ByteBufUtils.readUint32(bytes)));
        return phBuilder.build();
    }

    protected void serializePerPeerHeader(final PeerHeader peerHeader, final ByteBuf output) {
        checkArgument(peerHeader != null, "Per-peer header cannot be null.");
        final PeerType peerType = peerHeader.getType();
        output.writeByte(peerType.getIntValue());
        final BitArray flags = new BitArray(FLAGS_SIZE);
        flags.set(L_FLAG_POS, peerHeader.getAdjRibInType().getIntValue() != 0);
        flags.set(V_FLAG_POS, !peerHeader.isIpv4());
        flags.toByteBuf(output);
        final PeerDistinguisher peerDistinguisher = peerHeader.getPeerDistinguisher();
        switch (peerType) {
            case L3vpn:
                RouteDistinguisherUtil.serializeRouteDistinquisher(peerDistinguisher.getRouteDistinguisher(), output);
                break;
            case Local:
                output.writeBytes(peerDistinguisher.getBinary());
                break;
            case Global:
            default:
                output.writeZero(PEER_DISTINGUISHER_SIZE);
                break;
        }
        if (peerHeader.isIpv4()) {
            output.writeZero(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
            Ipv4Util.writeIpv4Address(peerHeader.getAddress().getIpv4AddressNoZone(), output);
        } else {
            Ipv6Util.writeIpv6Address(peerHeader.getAddress().getIpv6AddressNoZone(), output);
        }
        ByteBufUtils.write(output, peerHeader.getAs().getValue());
        Ipv4Util.writeIpv4Address(peerHeader.getBgpId(), output);

        final Timestamp stampSec = peerHeader.getTimestampSec();
        if (stampSec != null) {
            ByteBufUtils.write(output, stampSec.getValue());
        } else {
            output.writeInt(0);
        }
        final Timestamp stampMicro = peerHeader.getTimestampMicro();
        if (stampMicro != null) {
            ByteBufUtils.write(output, stampMicro.getValue());
        } else {
            output.writeInt(0);
        }
    }

    protected final MessageRegistry getBgpMessageRegistry() {
        return this.bgpMssageRegistry;
    }
}
