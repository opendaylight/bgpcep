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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.NotifyMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerDownNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerDownNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.down.Data;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.down.data.FsmEventCode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.down.data.FsmEventCodeBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class PeerDownHandler extends AbstractBmpPerPeerMessageParser<PeerDownNotificationBuilder> {
    private static final int MESSAGE_TYPE = 2;
    private final MessageRegistry msgRegistry;

    public PeerDownHandler(final MessageRegistry bgpMssageRegistry) {
        super(bgpMssageRegistry);
        msgRegistry = getBgpMessageRegistry();
    }

    @Override
    public void serializeMessageBody(final Notification<?> message, final ByteBuf buffer) {
        super.serializeMessageBody(message, buffer);
        Preconditions.checkArgument(message instanceof PeerDownNotification,
                "An instance of PeerDownNotification is required");
        final PeerDownNotification peerDown = (PeerDownNotification) message;
        if (peerDown.getLocalSystemClosed()) {
            if (peerDown.getData() instanceof FsmEventCode) {
                buffer.writeByte(REASON_TWO.getValue());
                ByteBufUtils.writeOrZero(buffer, ((FsmEventCode) peerDown.getData()).getFsmEventCode());
            } else if (peerDown.getData()
                    instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120
                    .peer.down.data.Notification) {
                buffer.writeByte(REASON_ONE.getValue());
                serializePDU(peerDown.getData(), buffer);
            }
        } else {
            if (peerDown.getData()
                    instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120
                    .peer.down.data.Notification) {
                buffer.writeByte(REASON_THREE.getValue());
                serializePDU(peerDown.getData(), buffer);
            } else {
                buffer.writeByte(REASON_FOUR.getValue());
            }
        }
    }

    private void serializePDU(final Data data, final ByteBuf buffer) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.down.data
            .Notification notification =
            (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.down.data
            .Notification) data;
        msgRegistry.serializeMessage(new NotifyBuilder(notification.getNotification()).build(), buffer);
    }

    @Override
    public PeerDownNotification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
        final PeerDownNotificationBuilder peerDown = new PeerDownNotificationBuilder()
                .setPeerHeader(parsePerPeerHeader(bytes));
        final Reason reason = Reason.forValue(bytes.readUnsignedByte());
        if (reason != null) {
            switch (reason) {
                case REASON_ONE:
                    peerDown.setLocalSystemClosed(Boolean.TRUE);
                    peerDown.setData(parseBgpNotificationMessage(bytes));
                    break;
                case REASON_TWO:
                    peerDown.setLocalSystemClosed(Boolean.TRUE);
                    peerDown.setData(new FsmEventCodeBuilder().setFsmEventCode(ByteBufUtils.readUint16(bytes)).build());
                    break;
                case REASON_THREE:
                case REASON_FOUR:
                    peerDown.setLocalSystemClosed(Boolean.FALSE);
                    peerDown.setData(parseBgpNotificationMessage(bytes));
                    break;
                case REASON_FIVE:
                    peerDown.setLocalSystemClosed(Boolean.FALSE);
                    break;
                default:
                    break;
            }
        }

        return peerDown.build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.down.data
            .Notification parseBgpNotificationMessage(final ByteBuf bytes) throws BmpDeserializationException {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.down.data
                .NotificationBuilder notificationCBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params
                .xml.ns.yang.bmp.message.rev200120.peer.down.data.NotificationBuilder();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.down.data
                .notification.NotificationBuilder notificationBuilder = new org.opendaylight.yang.gen.v1.urn
                .opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.down.data.notification
                .NotificationBuilder();
        try {
            final Notification<?> not = msgRegistry.parseMessage(bytes, null);
            requireNonNull(not, "Notify message may not be null.");
            Preconditions.checkArgument(not instanceof NotifyMessage,
                    "An instance of NotifyMessage is required");
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
        REASON_ONE((short) 1),
        REASON_TWO((short) 2),
        REASON_THREE((short) 3),
        REASON_FOUR((short) 4),
        REASON_FIVE((short) 5);

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
            return value;
        }
    }
}
