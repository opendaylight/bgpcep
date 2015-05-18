/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.message;

import static org.opendaylight.protocol.bmp.impl.message.PeerDownHandler.Reason.REASON_FOUR;
import static org.opendaylight.protocol.bmp.impl.message.PeerDownHandler.Reason.REASON_ONE;
import static org.opendaylight.protocol.bmp.impl.message.PeerDownHandler.Reason.REASON_THREE;
import static org.opendaylight.protocol.bmp.impl.message.PeerDownHandler.Reason.REASON_TWO;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.util.Map;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.parser.BMPDocumentedException;
import org.opendaylight.protocol.bmp.parser.BMPError;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerDownNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.Data;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.FsmEventCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.FsmEventCodeBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class PeerDownHandler extends AbstractBmpPerPeerMessageParser {

    private static final Logger LOG = LoggerFactory.getLogger(PeerDownHandler.class);

    private static final int MESSAGE_TYPE = 2;

    public PeerDownHandler(final MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof PeerDownNotification, "An instance of PeerDownNotification is required");
        final PeerDownNotification peerDown = (PeerDownNotification) message;
        if (peerDown.isLocalSystemClosed()) {
            if (peerDown.getData() instanceof FsmEventCode) {
                ByteBufWriteUtil.writeUnsignedByte(REASON_TWO.getValue(), buffer);
                buffer.writeBytes(Unpooled.buffer().writeByte(((FsmEventCode) peerDown.getData()).getFsmEventCode()));
            } else if (peerDown.getData() instanceof
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification) {
                ByteBufWriteUtil.writeUnsignedByte(REASON_ONE.getValue(), buffer);
                serializePDU(peerDown.getData(), buffer);
            }
        } else {
            if (peerDown.getData() instanceof
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification) {
                ByteBufWriteUtil.writeUnsignedByte(REASON_THREE.getValue(), buffer);
                serializePDU(peerDown.getData(), buffer);
            } else {
                ByteBufWriteUtil.writeUnsignedByte(REASON_FOUR.getValue(), buffer);
            }
        }

        LOG.trace("Peer Down notification serialized to: {}", ByteBufUtil.hexDump(buffer));
    }

    private void serializePDU(final Data data, final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification notification
            = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification) data;
        this.getBmpMessageRegistry().serializeMessage(new NotifyBuilder(notification.getNotification()).build(), buffer);
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BMPDocumentedException {
        final PeerDownNotificationBuilder peerDown = new PeerDownNotificationBuilder().setPeerHeader(parsePerPeerHeader(bytes));
        switch (Reason.forValue(bytes.readUnsignedByte())) {
        case REASON_ONE:
            peerDown.setLocalSystemClosed(true);
            peerDown.setData(parsePDU(bytes));
            break;
        case REASON_TWO:
            peerDown.setLocalSystemClosed(true);
            peerDown.setData(new FsmEventCodeBuilder().setFsmEventCode(bytes.readUnsignedShort()).build());
            break;
        case REASON_THREE:
            peerDown.setLocalSystemClosed(false);
            peerDown.setData(parsePDU(bytes));
            break;
        case REASON_FOUR:
            peerDown.setLocalSystemClosed(false);
            break;
        default:
            throw new BMPDocumentedException("Could not parse Initiation Message type", BMPError.OPT_ATTR_ERROR);
        }

        LOG.debug("Peer Down notification was parsed: err = {}, data = {}.", peerDown.isLocalSystemClosed(),
            peerDown.getData());

        return peerDown.build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.Notification parsePDU(final ByteBuf bytes) throws BMPDocumentedException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.NotificationBuilder notificationCBuilder
            = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.NotificationBuilder();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.notification.NotificationBuilder notificationBuilder
            = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.down.data.notification.NotificationBuilder();
        try {
            final Notify notify = (Notify) this.getBmpMessageRegistry().parseMessage(bytes);
            Preconditions.checkNotNull(notify, "Notify message may not be null.");
            notificationBuilder.fieldsFrom(notify);
            notificationCBuilder.setNotification(notificationBuilder.build());
        } catch (final BGPDocumentedException | BGPParsingException e) {
            throw new BMPDocumentedException("Error on Parse PDU", BMPError.PDU_PARSE_ERROR);
        }

        return notificationCBuilder.build();
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }

    public enum Reason {

        REASON_ONE((short) 1), REASON_TWO((short) 2), REASON_THREE((short) 3), REASON_FOUR((short) 4);

        private static final Map<Short, Reason> VALUE_MAP;

        static {
            final ImmutableMap.Builder<Short, Reason> b = ImmutableMap.builder();
            for (final Reason enumItem : Reason.values()) {
                b.put(enumItem.getValue(), enumItem);
            }
            VALUE_MAP = b.build();
        }

        private final short value;

        Reason(final short value) {
            this.value = value;
        }

        public static Reason forValue(final short value) {
            return VALUE_MAP.get(value);
        }

        public short getValue() {
            return this.value;
        }
    }
}
