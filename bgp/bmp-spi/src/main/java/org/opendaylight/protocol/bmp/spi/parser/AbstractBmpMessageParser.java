/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import static org.opendaylight.protocol.bmp.spi.parser.BmpMessageConstants.BMP_VERSION;
import static org.opendaylight.protocol.bmp.spi.parser.BmpMessageConstants.COMMON_HEADER_LENGTH;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBmpMessageParser implements BmpMessageParser, BmpMessageSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBmpMessageParser.class);

    @Override
    final public void serializeMessage(final Notification message, final ByteBuf buffer) {
        //TODO trace message
        Preconditions.checkArgument(message != null, "BMP message is mandatory.");
        final ByteBuf bodyBuffer = Unpooled.buffer();
        serializeMessageBody(message, bodyBuffer);
        formatMessage(bodyBuffer, buffer);
    }

    @Override
    final public Notification parseMessage(final ByteBuf bytes) {
        //TODO trace message
        Preconditions.checkArgument(bytes != null && bytes.isReadable());
        return parseMessageBody(bytes);
    }

    private void formatMessage(final ByteBuf body, final ByteBuf output) {
        ByteBufWriteUtil.writeUnsignedByte(BMP_VERSION, output);
        ByteBufWriteUtil.writeInt(body.writerIndex() + COMMON_HEADER_LENGTH, output);
        ByteBufWriteUtil.writeUnsignedByte((short) getBmpMessageType(), output);
        output.writeBytes(body);
    }

    abstract public void serializeMessageBody(Notification message, ByteBuf buffer);

    abstract public Notification parseMessageBody(ByteBuf bytes);

    abstract public int getBmpMessageType();

}
