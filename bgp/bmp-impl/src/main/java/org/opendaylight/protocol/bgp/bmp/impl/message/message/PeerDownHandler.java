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
import io.netty.buffer.Unpooled;

import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.FsmEventCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.FsmEventCodeBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by cgasparini on 13.5.2015.
 */
public class PeerDownHandler extends AbstractBmpMessageParser {
    private static final Logger LOG = LoggerFactory.getLogger(PeerDownHandler.class);
    private static final int REASON_2 = 2;
    private static final int REASON_1 = 1;
    private static final int REASON_4 = 4;
    private static final int REASON_3 = 3;
    private final MessageRegistry bgpMssageRegistry;

    public PeerDownHandler(final MessageRegistry bgpMssageRegistry) {
        this.bgpMssageRegistry = bgpMssageRegistry;
    }

    @Override
    public void serializeMessage(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof PeerDownNotification, "BMP Notification message cannot be null");
        final PeerDownNotification peerDown = (PeerDownNotification) message;

        if(peerDown.isLocalSystemClosed()){
            if(peerDown.getData() instanceof FsmEventCode ){
                TlvUtil.formatTv(REASON_2, Unpooled.buffer().writeByte(((FsmEventCode) peerDown.getData()).getFsmEventCode()), buffer);
            }else if (peerDown.getData() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bmp.message.rev150512.peer.down.data.Notification ){
                buffer.writeByte(REASON_1);
                serializePDU((org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                    .yang.bmp.message.rev150512.peer.down.data.Notification) peerDown.getData(),buffer);
            }
        }else{
            if (peerDown.getData() instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.bmp.message.rev150512.peer.down.data.Notification ){
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification data
                    = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification) peerDown.getData();
                if(data==null){
                    buffer.writeByte(REASON_4);
                    return;
                }
                buffer.writeByte(REASON_3);
                serializePDU(data, buffer);
            }
        }
    }

    private void serializePDU(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification data, ByteBuf buffer) {
        bgpMssageRegistry.serializeMessage(new NotifyBuilder(data.getNotification()).build(),buffer);
    }

    @Override
    public Notification parseMessage(final ByteBuf bytes) {
        Preconditions.checkArgument(bytes != null && bytes.readableBytes() != 0, "Byte buffer cannot be null.");
        LOG.trace("Started parsing of notification (Initialization) message: {}", ByteBufUtil.hexDump(bytes));
        final PeerDownNotificationBuilder peerDown = new PeerDownNotificationBuilder();
        final int reason = bytes.readUnsignedByte();
        switch (reason) {
        case REASON_1:
            peerDown.setLocalSystemClosed(true);
            peerDown.setData(parsePDU(bytes));
            break;
        case REASON_2:
            peerDown.setLocalSystemClosed(true);
            peerDown.setData(new FsmEventCodeBuilder().setFsmEventCode(new Integer(bytes.readShort())).build());
            break;
        case REASON_3:
            peerDown.setLocalSystemClosed(false);
            break;
        case REASON_4:
            peerDown.setLocalSystemClosed(false);
            peerDown.setData(parsePDU(bytes));
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
            temp = (Notify) this.bgpMssageRegistry.parseMessage(bytes);
        } catch (Exception e) {
            LOG.warn("Error on Parse notification message", bytes);
        }

        notBuilderCase.fieldsFrom(temp);
        return NotBuilder.setNotification(notBuilderCase.build()).build();
    }
}
