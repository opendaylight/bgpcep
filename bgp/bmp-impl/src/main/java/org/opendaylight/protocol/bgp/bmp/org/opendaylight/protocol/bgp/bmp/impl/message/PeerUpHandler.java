package org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;

import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
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

    public PeerUpHandler(MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
    }

    @Override
    public void serializeMessage(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof PeerUpNotification, "BMP Notification message cannot be null");
        final PeerUpNotification peerUp = (PeerUpNotification) message;
        this.serializePerPeerHeader(peerUp.getPeerHeader(), buffer);
        if (peerUp.getLocalAddress().getIpv4Address() != null) {
            ByteBufWriteUtil.writeIpv4Address(peerUp.getLocalAddress().getIpv4Address(), buffer);
        } else {
            ByteBufWriteUtil.writeIpv6Address(peerUp.getLocalAddress().getIpv6Address(), buffer);
        }
        ByteBufWriteUtil.writeUnsignedShort(peerUp.getLocalPort().getValue(), buffer);
        ByteBufWriteUtil.writeUnsignedShort(peerUp.getRemotePort().getValue(), buffer);

        this.getBmpMessageRegistry().serializeMessage(new OpenBuilder(peerUp.getReceivedOpen()).build(), buffer);
        this.getBmpMessageRegistry().serializeMessage(new OpenBuilder(peerUp.getSentOpen()).build(), buffer);
    }

    @Override
    public Notification parseMessage(final ByteBuf bytes) {
        this.checkByteBufMotNull(bytes);
        final PeerHeader header = this.parsePerPeerHeader(bytes);
        final PeerUpNotificationBuilder peerUpNot = new PeerUpNotificationBuilder().setPeerHeader(header);

        if (header.getAddress().getIpv4Address() != null) {
            peerUpNot.setLocalAddress(new IpAddress(Ipv4Util.addressForByteBuf(bytes)));
        } else {
            peerUpNot.setLocalAddress(new IpAddress(Ipv6Util.addressForByteBuf(bytes)));
        }
        final int localPortNumber = bytes.readUnsignedShort();
        peerUpNot.setLocalPort(new PortNumber(localPortNumber));
        final int remotePortNumber = bytes.readUnsignedShort();
        peerUpNot.setLocalPort(new PortNumber(remotePortNumber));
        OpenMessage sent;
        OpenMessage received;
        try {
            sent = (OpenMessage) this.getBmpMessageRegistry().parseMessage(bytes);
            received = (OpenMessage) this.getBmpMessageRegistry().parseMessage(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Error on Parse notification message");
        }
        peerUpNot.setSentOpen(new SentOpenBuilder(sent).build());
        peerUpNot.setReceivedOpen(new ReceivedOpenBuilder(received).build());
        return peerUpNot.build();
    }
}
