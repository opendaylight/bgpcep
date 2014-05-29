/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;

public abstract class AbstractMessageRegistry implements MessageRegistry {

    private static final byte[] MARKER;

    protected abstract Notification parseBody(final int type, final ByteBuf body, final int messageLength) throws BGPDocumentedException;

    protected abstract ByteBuf serializeMessageImpl(final Notification message);

    static {
        MARKER = new byte[MessageUtil.MARKER_LENGTH];
        Arrays.fill(MARKER, UnsignedBytes.MAX_VALUE);
    }

    @Override
    public final Notification parseMessage(final ByteBuf buffer) throws BGPDocumentedException, BGPParsingException {
        Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes cannot be null or empty.");
        Preconditions.checkArgument(buffer.readableBytes() >= MessageUtil.COMMON_HEADER_LENGTH,
                "Too few bytes in passed array. Passed: %s. Expected: >= %s.", buffer.readableBytes(), MessageUtil.COMMON_HEADER_LENGTH);
        final byte[] marker = ByteArray.readBytes(buffer, MessageUtil.MARKER_LENGTH);

        if (!Arrays.equals(marker, MARKER)) {
            throw new BGPDocumentedException("Marker not set to ones.", BGPError.CONNECTION_NOT_SYNC);
        }
        final int messageLength = buffer.readUnsignedShort();
        // to be sent with Error message
        final byte typeBytes = buffer.readByte();
        final int messageType = UnsignedBytes.toInt(typeBytes);

        final ByteBuf msgBody = buffer.slice(buffer.readerIndex(), messageLength - MessageUtil.COMMON_HEADER_LENGTH);

        if (messageLength < MessageUtil.COMMON_HEADER_LENGTH) {
            throw BGPDocumentedException.badMessageLength("Message length field not within valid range.", messageLength);
        }
        if (msgBody.readableBytes() != messageLength - MessageUtil.COMMON_HEADER_LENGTH) {
            throw new BGPParsingException("Size doesn't match size specified in header. Passed: " + msgBody.readableBytes()
                    + "; Expected: " + (messageLength - MessageUtil.COMMON_HEADER_LENGTH) + ". ");
        }
        final Notification msg = parseBody(messageType, msgBody, messageLength);
        if (msg == null) {
            throw new BGPDocumentedException("Unhandled message type " + messageType, BGPError.BAD_MSG_TYPE, new byte[] { typeBytes });
        }
        buffer.skipBytes(messageLength - MessageUtil.COMMON_HEADER_LENGTH);
        return msg;
    }

    @Override
    public final ByteBuf serializeMessage(final Notification message) {
        Preconditions.checkNotNull(message, "BGPMessage is mandatory.");
        final ByteBuf ret = serializeMessageImpl(message);
        Preconditions.checkNotNull(ret, "Unknown instance of BGPMessage. Passed ", message.getClass());
        return ret;
    }
}
