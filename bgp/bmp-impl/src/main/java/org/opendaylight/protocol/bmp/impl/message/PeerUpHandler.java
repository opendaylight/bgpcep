/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.message;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.parser.BMPDeserializationException;
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

    private static final int MESSAGE_TYPE = 3;

    public PeerUpHandler(MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof PeerUpNotification, "An instance of Peer Up notification is required");
        final PeerUpNotification peerUp = (PeerUpNotification) message;
        if (peerUp.getLocalAddress().getIpv4Address() != null) {
            ByteBufWriteUtil.writeIpv4Address(peerUp.getLocalAddress().getIpv4Address(), buffer);
            buffer.writeZero(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
        } else {
            ByteBufWriteUtil.writeIpv6Address(peerUp.getLocalAddress().getIpv6Address(), buffer);
        }
        ByteBufWriteUtil.writeUnsignedShort(peerUp.getLocalPort().getValue(), buffer);
        ByteBufWriteUtil.writeUnsignedShort(peerUp.getRemotePort().getValue(), buffer);

        getBmpMessageRegistry().serializeMessage(new OpenBuilder(peerUp.getReceivedOpen()).build(), buffer);
        getBmpMessageRegistry().serializeMessage(new OpenBuilder(peerUp.getSentOpen()).build(), buffer);

        LOG.trace("Peer Up notification serialized to: {}", ByteBufUtil.hexDump(buffer));
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BMPDeserializationException {
        final PeerUpNotificationBuilder peerUpNot = new PeerUpNotificationBuilder().setPeerHeader(parsePerPeerHeader(bytes));

        if (peerUpNot.getPeerHeader().isIpv4()) {
            peerUpNot.setLocalAddress(new IpAddress(Ipv4Util.addressForByteBuf(bytes)));
            bytes.skipBytes(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
        } else {
            peerUpNot.setLocalAddress(new IpAddress(Ipv6Util.addressForByteBuf(bytes)));
        }
        peerUpNot.setLocalPort(new PortNumber(bytes.readUnsignedShort()));
        peerUpNot.setRemotePort(new PortNumber(bytes.readUnsignedShort()));
        try {
            final OpenMessage sent = (OpenMessage) this.getBmpMessageRegistry().parseMessage(bytes);
            final OpenMessage received = (OpenMessage) this.getBmpMessageRegistry().parseMessage(bytes);
            Preconditions.checkNotNull(sent, "Error on parse Sent OPEN Message, Sent OPEN Message is null");
            Preconditions.checkNotNull(received, "Error on parse Received  OPEN Message, Received  OPEN Message is null");
            peerUpNot.setSentOpen(new SentOpenBuilder(sent).build());
            peerUpNot.setReceivedOpen(new ReceivedOpenBuilder(received).build());
        } catch (final BGPDocumentedException | BGPParsingException e) {
            throw new BMPDeserializationException("Error on Parse OPEN Message", e);
        }

        LOG.debug("Peer Up notification was parsed: err = {}, data = {}.", peerUpNot.getLocalAddress(),
            peerUpNot.getLocalPort(), peerUpNot.getRemotePort(), peerUpNot.getReceivedOpen());

        return peerUpNot.build();
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }
}
