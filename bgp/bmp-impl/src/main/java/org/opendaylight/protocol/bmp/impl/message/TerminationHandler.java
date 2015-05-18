/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.message;

import static org.opendaylight.protocol.bmp.impl.message.TerminationHandler.InformationType.REASON;
import static org.opendaylight.protocol.bmp.impl.message.TerminationHandler.InformationType.STRING;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static final int MESSAGE_TYPE = 5;

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) {
        final TerminationMessageBuilder terminationMessage = new TerminationMessageBuilder();
        final List<String> tlvStrings = new ArrayList<>();
        while (bytes.isReadable()) {
            final TlvUtil.Tlv tlv = TlvUtil.Tlv.fromByteBuf(bytes);
            switch (InformationType.forValue(tlv.getType())) {
            case STRING:
                tlvStrings.add(tlv.valueToUtf8String());
                break;
            case REASON:
                terminationMessage.setReason(Reason.forValue(tlv.getValue().readUnsignedShort()));
                break;
            default:
                break;
            }
        }

        Preconditions.checkNotNull(terminationMessage.getReason(), "Inclusion of sysDescr is mandatory");

        if (! tlvStrings.isEmpty()) {
            terminationMessage.setStringInfo(tlvStrings);
        }

        LOG.debug("Peer Up notification was parsed: err = {}, data = {}.", terminationMessage.getReason(),
            terminationMessage.getStringInfo());

        return terminationMessage.build();
    }


    @Override
    public void serializeMessageBody(Notification message, ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof TerminationMessage, "An instance of Termination message is required");
        final TerminationMessage tMessage = (TerminationMessage) message;
        Preconditions.checkArgument(tMessage.getReason() != null, "The reason is mandatory field.");
        TlvUtil.formatTlvUnsignedInt(REASON.getValue(), tMessage.getReason().getIntValue(), buffer);

        if (tMessage.getStringInfo() != null) {
            for (final String reason : tMessage.getStringInfo()) {
                TlvUtil.formatTlvUtf8(STRING.getValue(), reason, buffer);
            }
        }

        LOG.trace("Termination message serialized to: {}", ByteBufUtil.hexDump(buffer));
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }

    enum InformationType {

        STRING(0), REASON(1);

        private final int value;

        private InformationType(final int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        private static final Map<Integer, InformationType> VALUE_MAP;

        static {
            final ImmutableMap.Builder<Integer, InformationType> b = ImmutableMap.builder();
            for (final InformationType enumItem : InformationType.values()) {
                b.put(enumItem.getValue(), enumItem);
            }
            VALUE_MAP = b.build();
        }

        public static InformationType forValue(final int value) {
            return VALUE_MAP.get(value);
        }
    }
}
