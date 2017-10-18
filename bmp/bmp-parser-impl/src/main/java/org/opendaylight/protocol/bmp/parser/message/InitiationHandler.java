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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.InitiationMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.description.tlv.DescriptionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.initiation.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.initiation.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.name.tlv.NameTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.informations.StringInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.informations.StringInformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.tlv.StringTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.string.tlv.StringTlvBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class InitiationHandler extends AbstractBmpMessageWithTlvParser<TlvsBuilder> {

    public InitiationHandler(final BmpTlvRegistry tlvRegistry) {
        super(tlvRegistry);
    }

    private static final int MESSAGE_TYPE = 4;

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        Preconditions.checkArgument(message instanceof InitiationMessage, "Incorrect instance of BGP message. The Initiation Message is expected.");
        final InitiationMessage initiation = (InitiationMessage) message;
        serializeTlvs(initiation.getTlvs(), buffer);
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
        final InitiationMessageBuilder initiationBuilder = new InitiationMessageBuilder();
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        tlvsBuilder.setStringInformation(ImmutableList.of());
        parseTlvs(tlvsBuilder, bytes);

        if (tlvsBuilder.getDescriptionTlv() == null || tlvsBuilder.getDescriptionTlv().getDescription() == null) {
            throw new BmpDeserializationException("Inclusion of sysDescr TLV is mandatory.");
        }
        if (tlvsBuilder.getNameTlv() == null || tlvsBuilder.getNameTlv().getName() == null) {
            throw new BmpDeserializationException("Inclusion of sysName TLV is mandatory.");
        }

        return initiationBuilder.setTlvs(tlvsBuilder.build()).build();
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }

    private void serializeTlvs(final Tlvs tlvs, final ByteBuf output) {
        serializeTlv(tlvs.getNameTlv(), output);
        serializeTlv(tlvs.getDescriptionTlv(), output);
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
        if (tlv instanceof DescriptionTlv) {
            builder.setDescriptionTlv((DescriptionTlv) tlv);
        } else if (tlv instanceof NameTlv) {
            builder.setNameTlv((NameTlv) tlv);
        } else if (tlv instanceof StringTlv) {
            builder.setStringInformation(ImmutableList.<StringInformation>builder()
                    .addAll(builder.getStringInformation())
                    .add(new StringInformationBuilder().setStringTlv(new StringTlvBuilder((StringTlv) tlv).build()).build()).build());
        }
    }
}
