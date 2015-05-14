package org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.FsmEventCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.FsmEventCodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by cgasparini on 13.5.2015.
 */
public class PeerDownHandler extends AbstractBmpPerPeerMessageParser {
    private static final Logger LOG = LoggerFactory.getLogger(PeerDownHandler.class);

    public PeerDownHandler(final MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
    }

    @Override
    public void serializeMessage(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof PeerDownNotification, "BMP Notification message cannot be null");
        final PeerDownNotification peerDown = (PeerDownNotification) message;
        this.serializePerPeerHeader(peerDown.getPeerHeader(), buffer);
        if (peerDown.isLocalSystemClosed()) {
            if (peerDown.getData() instanceof FsmEventCode) {
                ByteBufWriteUtil.writeUnsignedByte(Reason.TWO.value,buffer);
                buffer.writeBytes(Unpooled.buffer().writeByte(((FsmEventCode) peerDown.getData()).getFsmEventCode()));
            } else if (peerDown.getData() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bmp.message.rev150512.peer.down.data.Notification) {
                ByteBufWriteUtil.writeUnsignedByte(Reason.ONE.value,buffer);
                serializePDU((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                    .yang.bmp.message.rev150512.peer.down.data.Notification) peerDown.getData(), buffer);
            }
        } else {
            if (peerDown.getData() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bmp.message.rev150512.peer.down.data.Notification) {
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification data
                    = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification) peerDown.getData();
                serializePDU(data, buffer);
            } else {
                buffer.writeByte(Reason.FOUR.ordinal());
                ByteBufWriteUtil.writeUnsignedByte(Reason.FOUR.value, buffer);
            }
        }
    }

    private void serializePDU(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification data, ByteBuf buffer) {
        this.getBmpMessageRegistry().serializeMessage(new NotifyBuilder(data.getNotification()).build(), buffer);
    }

    @Override
    public Notification parseMessage(final ByteBuf bytes) {
        this.checkByteBufMotNull(bytes);
        final PeerHeader header = this.parsePerPeerHeader(bytes);
        final PeerDownNotificationBuilder peerDown = new PeerDownNotificationBuilder().setPeerHeader(header);
        switch (Reason.values()[bytes.readUnsignedByte()]) {
        case ONE:
            peerDown.setLocalSystemClosed(true);
            peerDown.setData(parsePDU(bytes));
            break;
        case TWO:
            peerDown.setLocalSystemClosed(true);
            peerDown.setData(new FsmEventCodeBuilder().setFsmEventCode(new Integer(bytes.readUnsignedShort())).build());
            break;
        case THREE:
            peerDown.setLocalSystemClosed(false);
            peerDown.setData(parsePDU(bytes));
            break;
        case FOUR:
            peerDown.setLocalSystemClosed(false);
            break;
        }
        return peerDown.build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification parsePDU(ByteBuf bytes) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.NotificationBuilder NotBuilder
            = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.NotificationBuilder();

        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.notification.NotificationBuilder notBuilderCase
            = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.notification.NotificationBuilder();
        Notify temp = null;
        try {
            temp = (Notify) this.getBmpMessageRegistry().parseMessage(bytes);
            Preconditions.checkNotNull(temp, "updateMessage may not be null");
            notBuilderCase.fieldsFrom(temp);
        } catch (Exception e) {
            throw new IllegalStateException("Error on Parse notification message");
        }

        return NotBuilder.setNotification(notBuilderCase.build()).build();
    }

    public enum Reason {
        ONE((short)1), TWO((short)2), THREE((short)3), FOUR((short)4);

        public final short value;

        Reason(final short value) {
            this.value = value;
        }
    }
}
