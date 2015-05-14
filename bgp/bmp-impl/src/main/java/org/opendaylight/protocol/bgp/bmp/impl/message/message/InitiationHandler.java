package org.opendaylight.protocol.bgp.bmp.impl.message.message;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessageBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by cgasparini on 13.5.2015.
 */
public final class InitiationHandler extends AbstractBmpMessageParser {
    private static final Logger LOG = LoggerFactory.getLogger(InitiationHandler.class);
    private final static int TYPE_STRING = 0;
    private final static int TYPE_SYSDESCR = 1;
    private final static int TYPE_SYSNAME = 2;

    @Override
    public void serializeMessage(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof InitiationMessage, "BMP Notification message cannot be null");
        final InitiationMessage tMessage = (InitiationMessage) message;

        if ( tMessage.getName() != null ) {
            TlvUtil.formatTlvASCII(TYPE_SYSNAME, tMessage.getName(), buffer);
        }

        if ( tMessage.getDescription() != null ) {
            TlvUtil.formatTlvASCII(TYPE_SYSDESCR, tMessage.getName(), buffer);
        }

        for (String reason : tMessage.getStringInfo()) {
            TlvUtil.formatTlvUtf8(TYPE_STRING, reason, buffer);
        }
    }

    @Override
    public Notification parseMessage(final ByteBuf bytes) {
        Preconditions.checkArgument(bytes != null && bytes.readableBytes() != 0, "Byte buffer cannot be null.");
        LOG.trace("Started parsing of notification (Initialization) message: {}", ByteBufUtil.hexDump(bytes));
        final InitiationMessageBuilder iMessage = new InitiationMessageBuilder();
        final List<String> TLVstring = Lists.newArrayList();
        while (bytes.isReadable()) {
            final int tMessageType = bytes.readUnsignedByte();
            final int tMessageLength = bytes.readUnsignedByte();
            switch (tMessageType) {
                case 0:
                    final String reason = TlvUtil.parseUTF8(bytes, tMessageLength);
                    if ( reason != null ) {
                        TLVstring.add(reason);
                    }
                    break;
                case 1:
                    iMessage.setDescription(TlvUtil.parseASCII(bytes, tMessageLength));
                    break;
                case 2:
                    iMessage.setName(TlvUtil.parseASCII(bytes, tMessageLength));
                    break;
            }
        }
        return iMessage.setStringInfo(TLVstring).build();
    }
}
