package org.opendaylight.protocol.bgp.bmp.impl.message.message;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.ReceivedOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.SentOpenBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class PeerUpHandler extends AbstractBmpPerPeerMessageParser {
    private static final Logger LOG = LoggerFactory.getLogger(PeerUpHandler.class);
    private static final int IPV4_SIZE = 4;
    private static final int IPV6_SIZE = 16;

    public PeerUpHandler(MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
    }

    @Override
    public void serializeMessage(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof PeerUpNotification, "BMP Notification message cannot be null");
        final PeerUpNotification peerUp = (PeerUpNotification) message;
        this.serializePerPeerHeader(peerUp.getPeerHeader(), buffer);
        if (peerUp.getLocalAddress().getIpv4Address() != null) {
            TlvUtil.formatTvIPV(peerUp.getLocalAddress().getIpv4Address().getValue(), buffer);
        } else {
            TlvUtil.formatTvIPV(peerUp.getLocalAddress().getIpv6Address().getValue(), buffer);
        }
        buffer.writeShort(peerUp.getLocalPort().getValue());
        buffer.writeShort(peerUp.getRemotePort().getValue());

        this.getBmpMessageRegistry().serializeMessage(new OpenBuilder(peerUp.getReceivedOpen()).build(), buffer);
        this.getBmpMessageRegistry().serializeMessage(new OpenBuilder(peerUp.getSentOpen()).build(), buffer);
    }

    @Override
    public Notification parseMessage(final ByteBuf bytes) {
        Preconditions.checkArgument(bytes != null && bytes.readableBytes() != 0, "Byte buffer cannot be null.");
        LOG.trace("Started parsing of notification (PeerUp) message: {}", ByteBufUtil.hexDump(bytes));

        final PeerHeader header = this.parsePerPeerHeader(bytes);
        final PeerUpNotificationBuilder peerUpNot = new PeerUpNotificationBuilder().setPeerHeader(header);

        if (header.getAddress().getIpv4Address() != null) {
            peerUpNot.setLocalAddress(new IpAddress(new Ipv4Address(TlvUtil.parseIPV(ByteArray.readBytes(bytes, IPV4_SIZE)))));
        } else {
            peerUpNot.setLocalAddress(new IpAddress(new Ipv6Address(TlvUtil.parseIPV(ByteArray.readBytes(bytes, IPV6_SIZE)))));
        }
        final int localPortNumber = bytes.readUnsignedShort();
        peerUpNot.setLocalPort(new PortNumber(localPortNumber));
        final int remotePortNumber = bytes.readUnsignedShort();
        peerUpNot.setLocalPort(new PortNumber(remotePortNumber));
        OpenMessage sent = null;
        OpenMessage received = null;
        try {
            sent = (OpenMessage) this.getBmpMessageRegistry().parseMessage(bytes);
            received = (OpenMessage) this.getBmpMessageRegistry().parseMessage(bytes);
        } catch (Exception e) {
            LOG.warn("Error on Parse notification message", bytes);
        }
        peerUpNot.setSentOpen(new SentOpenBuilder(sent).build());
        peerUpNot.setReceivedOpen(new ReceivedOpenBuilder(received).build());
        return peerUpNot.build();
    }
}
