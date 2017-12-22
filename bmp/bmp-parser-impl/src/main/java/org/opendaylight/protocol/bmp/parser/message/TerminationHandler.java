/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.parser.message;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpMessageWithTlvParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.TerminationMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.reason.tlv.ReasonTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.informations.StringInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.informations.StringInformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.tlv.StringTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.tlv.StringTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.termination.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.termination.TlvsBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class TerminationHandler extends AbstractBmpMessageWithTlvParser<TlvsBuilder> {

    public TerminationHandler(final BmpTlvRegistry tlvRegistry) {
        super(tlvRegistry);
    }

    private static final int MESSAGE_TYPE = 5;

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
        final TerminationMessageBuilder terminationMessage = new TerminationMessageBuilder();
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        tlvsBuilder.setStringInformation(ImmutableList.of());
        parseTlvs(tlvsBuilder, bytes);
        if (tlvsBuilder.getReasonTlv() == null || tlvsBuilder.getReasonTlv().getReason() == null) {
            throw new BmpDeserializationException("Inclusion of Reason TLV is mandatory.");
        }
        return terminationMessage.setTlvs(tlvsBuilder.build()).build();
    }


    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof TerminationMessage,
                "An instance of Termination message is required");
        final TerminationMessage terminationMsg = (TerminationMessage) message;
        serializeTlvs(terminationMsg.getTlvs(), buffer);
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }

    protected void serializeTlvs(final Tlvs tlvs, final ByteBuf output) {
        serializeTlv(tlvs.getReasonTlv(), output);
        if (tlvs.getStringInformation() != null) {
            for (final StringInformation stringInfo : tlvs.getStringInformation()) {
                if (stringInfo.getStringTlv() != null) {
                    serializeTlv(stringInfo.getStringTlv(), output);
                }
            }
        }
    }

    @Override
    protected void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        if (tlv instanceof ReasonTlv) {
            builder.setReasonTlv((ReasonTlv) tlv);
        } else if (tlv instanceof StringTlv) {
            builder.setStringInformation(ImmutableList.<StringInformation>builder()
                    .addAll(builder.getStringInformation())
                    .add(new StringInformationBuilder()
                            .setStringTlv(new StringTlvBuilder((StringTlv) tlv).build()).build()).build());
        }
    }
}
