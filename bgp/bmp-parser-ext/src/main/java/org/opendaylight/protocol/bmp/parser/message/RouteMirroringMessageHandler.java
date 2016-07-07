/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.parser.message;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMirroringMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.RouteMirroringMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.mirror.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.mirror.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.mirror.information.tlv.MirrorInformationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.mirror.pdu.tlvs.PduOpenTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.mirror.pdu.tlvs.PduUpdateTlv;
import org.opendaylight.yangtools.yang.binding.Notification;

public class RouteMirroringMessageHandler extends AbstractBmpPerPeerMessageParser<TlvsBuilder> {

    private static final int MESSAGE_TYPE = 6;

    public RouteMirroringMessageHandler(final MessageRegistry bgpMssageRegistry, final BmpTlvRegistry tlvRegistry) {
        super(bgpMssageRegistry, tlvRegistry);
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        super.serializeMessageBody(message, buffer);
        Preconditions.checkArgument(message instanceof RouteMirroringMessage, "An instance of RouteMirroringMessage is required");
        final RouteMirroringMessage routeMirror = (RouteMirroringMessage) message;
        serializeTlvs(routeMirror.getTlvs(), buffer);
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
        final RouteMirroringMessageBuilder routeMirror = new RouteMirroringMessageBuilder().setPeerHeader(parsePerPeerHeader(bytes));
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        parseTlvs(tlvsBuilder, bytes);
        return routeMirror.setTlvs(tlvsBuilder.build()).build();
    }

    protected void serializeTlvs(final Tlvs tlvs, final ByteBuf output) {
        final ByteBuf tlvsBuffer = Unpooled.buffer();
        if (tlvs.getMirrorInformationTlv() != null) {
            serializeTlv(tlvs.getMirrorInformationTlv(), tlvsBuffer);
        }
        if (tlvs.getPduUpdateTlv() != null) {
            getBgpMessageRegistry().serializeMessage(new UpdateBuilder(tlvs.getPduUpdateTlv()).build(), tlvsBuffer);
        }
        if (tlvs.getPduOpenTlv() != null) {
            getBgpMessageRegistry().serializeMessage(new OpenBuilder(tlvs.getPduOpenTlv()).build(), tlvsBuffer);
        }

        output.writeBytes(tlvsBuffer);
    }


    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    protected void addTlv(final TlvsBuilder builder, final Tlv tlv) {
        if (tlv instanceof MirrorInformationTlv) {
            builder.setMirrorInformationTlv((MirrorInformationTlv) tlv);
        } else if (tlv instanceof PduUpdateTlv) {
            builder.setPduUpdateTlv((PduUpdateTlv) tlv);
        } else if (tlv instanceof PduOpenTlv) {
            builder.setPduOpenTlv((PduOpenTlv) tlv);
        }
    }
}
