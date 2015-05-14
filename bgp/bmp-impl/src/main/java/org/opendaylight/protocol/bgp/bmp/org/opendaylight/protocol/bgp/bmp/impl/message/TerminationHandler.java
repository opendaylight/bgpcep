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
import io.netty.buffer.Unpooled;

import java.util.List;

import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Termination.Reason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessageBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 13.5.2015.
 */
public class TerminationHandler extends AbstractBmpMessageParser {
    private static final Logger LOG = LoggerFactory.getLogger(TerminationHandler.class);
    private static final int TYPE_STRING = 0;
    private static final int TYPE_REASON = 1;

    @Override
    public Notification parseMessage(ByteBuf bytes) {
        this.checkByteBufMotNull(bytes);
        final TerminationMessageBuilder tMessage = new TerminationMessageBuilder();
        final List<String> TLVstring = Lists.newArrayList();
        while (bytes.isReadable()) {
            final int tMessageType = bytes.readUnsignedShort();
            final int tMessageLength = bytes.readUnsignedShort();
            switch (tMessageType) {
            case 0:
                final String reason = TlvUtil.parseUTF8(bytes, tMessageLength);
                if (reason != null) {
                    TLVstring.add(reason);
                }
                break;
            case 1:
                tMessage.setReason(Reason.forValue(bytes.readUnsignedShort()));
                break;
            }
        }
        return tMessage.setStringInfo(TLVstring).build();
    }


    @Override
    public void serializeMessage(Notification message, ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof TerminationMessage, "BMP Notification message cannot be null");
        final TerminationMessage tMessage = (TerminationMessage) message;
        if (tMessage.getReason() != null) {
            TlvUtil.formatTlv(TYPE_REASON, Unpooled.buffer().writeShort(tMessage.getReason().getIntValue()), buffer);
        }

        for (String reason : tMessage.getStringInfo()) {
            TlvUtil.formatTlvUtf8(TYPE_STRING, reason, buffer);
        }
    }
}
