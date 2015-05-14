package org.opendaylight.protocol.bgp.bmp.org.opendaylight.protocol.bgp.bmp.impl.message;/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;

import java.util.List;

import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessageBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 13.5.2015.
 */
public final class InitiationHandler extends AbstractBmpMessageParser {
    private static final Logger LOG = LoggerFactory.getLogger(InitiationHandler.class);

    @Override
    public void serializeMessage(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof InitiationMessage, "BMP Notification message cannot be null");
        final InitiationMessage tMessage = (InitiationMessage) message;

        if (tMessage.getName() != null) {
            TlvUtil.formatTlvASCII(Initiation_message_type.SYSNAME.ordinal(), tMessage.getName(), buffer);
        }

        if (tMessage.getDescription() != null) {
            TlvUtil.formatTlvASCII(Initiation_message_type.SYSDESCR.ordinal(), tMessage.getName(), buffer);
        }

        if (tMessage.getName() != null && tMessage.getDescription() != null) {
            for (String reason : tMessage.getStringInfo()) {
                TlvUtil.formatTlvUtf8(Initiation_message_type.STRING.ordinal(), reason, buffer);
            }
        }
    }

    @Override
    public Notification parseMessage(final ByteBuf bytes) {
        this.checkByteBufMotNull(bytes);
        final InitiationMessageBuilder iMessage = new InitiationMessageBuilder();
        final List<String> TLVstring = Lists.newArrayList();
        while (bytes.isReadable()) {
            final int tMessageType = bytes.readUnsignedShort();
            final int tMessageLength = bytes.readUnsignedShort();
            switch (Initiation_message_type.values()[tMessageType]) {
            case STRING:
                final String reason = TlvUtil.parseUTF8(bytes, tMessageLength);
                if (reason != null) {
                    TLVstring.add(reason);
                }
                break;
            case SYSDESCR:
                iMessage.setDescription(TlvUtil.parseASCII(bytes, tMessageLength));
                break;
            case SYSNAME:
                iMessage.setName(TlvUtil.parseASCII(bytes, tMessageLength));
                break;
            }
        }
        return iMessage.setStringInfo(TLVstring).build();
    }

    enum Initiation_message_type {
        STRING(0), SYSDESCR(1), SYSNAME(2);

        public final int value;

        Initiation_message_type(final int value) {
            this.value = value;
        }

    }
}
