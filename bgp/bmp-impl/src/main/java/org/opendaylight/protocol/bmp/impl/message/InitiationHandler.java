/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.message;

import static org.opendaylight.protocol.bmp.impl.message.InitiationHandler.TlvType.STRING;
import static org.opendaylight.protocol.bmp.impl.message.InitiationHandler.TlvType.SYS_DESCR;
import static org.opendaylight.protocol.bmp.impl.message.InitiationHandler.TlvType.SYS_NAME;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.protocol.bmp.parser.BMPDeserializationException;
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

    private static final int MESSAGE_TYPE = 4;

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) throws BMPDeserializationException {
        Preconditions.checkArgument(message instanceof InitiationMessage, "Incorrect instance of BGP message. The Initiation Message is expected.");
        final InitiationMessage initiation = (InitiationMessage) message;
        Preconditions.checkArgument(initiation.getName() != null, "The name (sysName) is mandatory field.");
        Preconditions.checkArgument(initiation.getDescription() != null, "The description (sysDescr) is mandatory field.");

        TlvUtil.formatTlvASCII(SYS_NAME.getValue(), initiation.getName(), buffer);
        TlvUtil.formatTlvASCII(SYS_DESCR.getValue(), initiation.getDescription(), buffer);

        if (initiation.getStringInfo() != null) {
            for (final String reason : initiation.getStringInfo()) {
                TlvUtil.formatTlvUtf8(STRING.getValue(), reason, buffer);
            }
        }
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BMPDeserializationException {
        final InitiationMessageBuilder initiationBuilder = new InitiationMessageBuilder();
        final List<String> tlvStrings = new ArrayList<>();
        while (bytes.isReadable()) {
            TlvUtil.Tlv tlv = TlvUtil.Tlv.fromByteBuf(bytes);
            switch (TlvType.forValue(tlv.getType())) {
            case STRING:
                tlvStrings.add(tlv.valueToUtf8String());
                break;
            case SYS_DESCR:
                initiationBuilder.setDescription(tlv.valueToAsciiString());
                break;
            case SYS_NAME:
                initiationBuilder.setName(tlv.valueToAsciiString());
                break;
            default:
                LOG.warn("Could not parse Initiation Message type");
            }
        }

        Preconditions.checkArgument(initiationBuilder.getDescription() != null && !initiationBuilder.getDescription()
            .isEmpty(), "Inclusion of sysDescr is mandatory");
        Preconditions.checkArgument(initiationBuilder.getName() != null && !initiationBuilder.getName().isEmpty(),
            "Inclusion of sysName is mandatory");

        if (!tlvStrings.isEmpty()) {
            initiationBuilder.setStringInfo(tlvStrings);
        }

        return initiationBuilder.build();
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }

    enum TlvType {
        STRING(0), SYS_DESCR(1), SYS_NAME(2);

        private static final Map<Integer, TlvType> VALUE_MAP;

        static {
            final ImmutableMap.Builder<Integer, TlvType> b = ImmutableMap.builder();
            for (final TlvType enumItem : TlvType.values()) {
                b.put(enumItem.getValue(), enumItem);
            }
            VALUE_MAP = b.build();
        }

        private final int value;

        TlvType(final int value) {
            this.value = value;
        }

        public static TlvType forValue(final int value) {
            return VALUE_MAP.get(value);
        }

        int getValue() {
            return this.value;
        }
    }
}
