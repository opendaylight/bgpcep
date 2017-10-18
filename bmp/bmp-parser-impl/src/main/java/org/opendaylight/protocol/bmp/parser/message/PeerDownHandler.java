/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.parser.message;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bmp.parser.message.PeerDownHandler.Reason.REASON_FOUR;
import static org.opendaylight.protocol.bmp.parser.message.PeerDownHandler.Reason.REASON_ONE;
import static org.opendaylight.protocol.bmp.parser.message.PeerDownHandler.Reason.REASON_THREE;
import static org.opendaylight.protocol.bmp.parser.message.PeerDownHandler.Reason.REASON_TWO;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.NotifyMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.PeerDownNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.Data;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.FsmEventCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.FsmEventCodeBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class PeerDownHandler extends AbstractBmpPerPeerMessageParser<PeerDownNotificationBuilder> {

    private static final int MESSAGE_TYPE = 2;
    private final MessageRegistry msgRegistry;

    public PeerDownHandler(final MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
        this.msgRegistry = getBgpMessageRegistry();
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        super.serializeMessageBody(message, buffer);
        Preconditions.checkArgument(message instanceof PeerDownNotification, "An instance of PeerDownNotification is required");
        final PeerDownNotification peerDown = (PeerDownNotification) message;
        if (peerDown.isLocalSystemClosed()) {
            if (peerDown.getData() instanceof FsmEventCode) {
                ByteBufWriteUtil.writeUnsignedByte(REASON_TWO.getValue(), buffer);
                ByteBufWriteUtil.writeUnsignedShort(((FsmEventCode) peerDown.getData()).getFsmEventCode(), buffer);
            } else if (peerDown.getData() instanceof
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.Notification) {
                ByteBufWriteUtil.writeUnsignedByte(REASON_ONE.getValue(), buffer);
                serializePDU(peerDown.getData(), buffer);
            }
        } else {
            if (peerDown.getData() instanceof
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.Notification) {
                ByteBufWriteUtil.writeUnsignedByte(REASON_THREE.getValue(), buffer);
                serializePDU(peerDown.getData(), buffer);
            } else {
                ByteBufWriteUtil.writeUnsignedByte(REASON_FOUR.getValue(), buffer);
            }
        }
    }

    private void serializePDU(final Data data, final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.Notification notification
            = (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.Notification) data;
        this.msgRegistry.serializeMessage(new NotifyBuilder(notification.getNotification()).build(), buffer);
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
        final PeerDownNotificationBuilder peerDown = new PeerDownNotificationBuilder().setPeerHeader(parsePerPeerHeader(bytes));
        final Reason reason = Reason.forValue(bytes.readUnsignedByte());
        if (reason != null) {
            switch (reason) {
            case REASON_ONE:
                peerDown.setLocalSystemClosed(true);
                peerDown.setData(parseBgpNotificationMessage(bytes));
                break;
            case REASON_TWO:
                peerDown.setLocalSystemClosed(true);
                peerDown.setData(new FsmEventCodeBuilder().setFsmEventCode(bytes.readUnsignedShort()).build());
                break;
            case REASON_THREE:
            case REASON_FOUR:
                peerDown.setLocalSystemClosed(false);
                peerDown.setData(parseBgpNotificationMessage(bytes));
                break;
            case REASON_FIVE:
                peerDown.setLocalSystemClosed(false);
                break;
            default:
                break;
            }
        }

        return peerDown.build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.Notification parseBgpNotificationMessage(final ByteBuf bytes) throws BmpDeserializationException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.NotificationBuilder notificationCBuilder
            = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.NotificationBuilder();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.notification.NotificationBuilder notificationBuilder
            = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.peer.down.data.notification.NotificationBuilder();
        try {
            final Notification not = this.msgRegistry.parseMessage(bytes, null);
            requireNonNull(not, "Notify message may not be null.");
            Preconditions.checkArgument(not instanceof NotifyMessage, "An instance of NotifyMessage is required");
            notificationBuilder.fieldsFrom((NotifyMessage) not);
            notificationCBuilder.setNotification(notificationBuilder.build());
        } catch (final BGPDocumentedException | BGPParsingException e) {
            throw new BmpDeserializationException("Error while parsing BGP Notification message.", e);
        }

        return notificationCBuilder.build();
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }

    public enum Reason {

        REASON_ONE((short) 1), REASON_TWO((short) 2), REASON_THREE((short) 3), REASON_FOUR((short) 4), REASON_FIVE((short) 5);

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
