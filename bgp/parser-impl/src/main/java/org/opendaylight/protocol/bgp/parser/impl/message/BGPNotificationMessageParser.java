/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message;

import static com.google.common.base.Preconditions.checkArgument;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraint;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.NotifyBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for BGPNotification message.
 */
public final class BGPNotificationMessageParser implements MessageParser, MessageSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(BGPNotificationMessageParser.class);

    public static final int TYPE = 3;

    private static final int ERROR_SIZE = 2;

    /**
     * Serializes BGP Notification message.
     *
     * @param msg to be serialized
     * @param bytes ByteBuf where the message will be serialized
     */
    @Override
    public void serializeMessage(final Notification<?> msg, final ByteBuf bytes) {
        checkArgument(msg instanceof Notify, "Message needs to be of type Notify");
        final Notify ntf = (Notify) msg;

        final ByteBuf msgBody = Unpooled.buffer()
                .writeByte(ntf.getErrorCode().toJava())
                .writeByte(ntf.getErrorSubcode().toJava());
        final byte[] data = ntf.getData();
        if (data != null) {
            msgBody.writeBytes(data);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Notification message serialized to: {}", ByteBufUtil.hexDump(msgBody));
        }
        MessageUtil.formatMessage(TYPE, msgBody, bytes);
    }

    /**
     * Parses BGP Notification message to bytes.
     *
     * @param body ByteBuf to be parsed
     * @param messageLength the length of the message
     * @return {@link Notify} which represents BGP notification message
     * @throws BGPDocumentedException if parsing goes wrong
     */
    @Override
    public Notify parseMessageBody(final ByteBuf body, final int messageLength,
            final PeerSpecificParserConstraint constraint) throws BGPDocumentedException {
        checkArgument(body != null, "Buffer cannot be null.");
        if (body.readableBytes() < ERROR_SIZE) {
            throw BGPDocumentedException.badMessageLength("Notification message too small.", messageLength);
        }
        final Uint8 errorCode = Uint8.valueOf(body.readUnsignedByte());
        final Uint8 errorSubcode = Uint8.valueOf(body.readUnsignedByte());
        final NotifyBuilder builder = new NotifyBuilder()
                .setErrorCode(errorCode).setErrorSubcode(errorSubcode);
        if (body.isReadable()) {
            builder.setData(ByteArray.readAllBytes(body));
        }

        final Notify result = builder.build();
        final BGPError err = BGPError.forValue(errorCode, errorSubcode);

        if (LOG.isDebugEnabled()) {
            LOG.debug("BGP Notification message was parsed: err = {}, data = {}.", err,
                Arrays.toString(result.getData()));
        }
        return result;
    }
}
