/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.parser.message;


import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvRegistry;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerUpNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.Information;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.InformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.ReceivedOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.up.SentOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.informations.StringInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.informations.StringInformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.tlv.StringTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.tlv.StringTlvBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class PeerUpHandler extends AbstractBmpPerPeerMessageParser<InformationBuilder> {

    private static final int MESSAGE_TYPE = 3;
    private final MessageRegistry msgRegistry;

    public PeerUpHandler(final MessageRegistry bgpMssageRegistry, final BmpTlvRegistry tlvRegistry) {
        super(bgpMssageRegistry, tlvRegistry);
        this.msgRegistry = getBgpMessageRegistry();
    }

    @Override
    public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
        super.serializeMessageBody(message, buffer);
        Preconditions.checkArgument(message instanceof PeerUpNotification,
            "An instance of Peer Up notification is required");
        final PeerUpNotification peerUp = (PeerUpNotification) message;

        if (peerUp.getLocalAddress().getIpv4Address() != null) {
            buffer.writeZero(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
            ByteBufWriteUtil.writeIpv4Address(peerUp.getLocalAddress().getIpv4Address(), buffer);
        } else {
            ByteBufWriteUtil.writeIpv6Address(peerUp.getLocalAddress().getIpv6Address(), buffer);
        }
        ByteBufWriteUtil.writeUnsignedShort(peerUp.getLocalPort().getValue(), buffer);
        ByteBufWriteUtil.writeUnsignedShort(peerUp.getRemotePort().getValue(), buffer);

        this.msgRegistry.serializeMessage(new OpenBuilder(peerUp.getSentOpen()).build(), buffer);
        this.msgRegistry.serializeMessage(new OpenBuilder(peerUp.getReceivedOpen()).build(), buffer);
        serializeTlvs(peerUp.getInformation(), buffer);
    }

    private void serializeTlvs(final Information tlvs, final ByteBuf output) {
        if (tlvs != null && tlvs.getStringInformation() != null) {
            for (final StringInformation stringInfo : tlvs.getStringInformation()) {
                if (stringInfo.getStringTlv() != null) {
                    serializeTlv(stringInfo.getStringTlv(), output);
                }
            }
        }
    }

    @Override
    public Notification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
        final PeerUpNotificationBuilder peerUpNot = new PeerUpNotificationBuilder()
            .setPeerHeader(parsePerPeerHeader(bytes));

        if (peerUpNot.getPeerHeader().isIpv4()) {
            bytes.skipBytes(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
            peerUpNot.setLocalAddress(new IpAddress(Ipv4Util.addressForByteBuf(bytes)));
        } else {
            peerUpNot.setLocalAddress(new IpAddress(Ipv6Util.addressForByteBuf(bytes)));
        }
        peerUpNot.setLocalPort(new PortNumber(bytes.readUnsignedShort()));
        peerUpNot.setRemotePort(new PortNumber(bytes.readUnsignedShort()));
        try {
            final Notification opSent = this.msgRegistry
                .parseMessage(bytes.readSlice(getBgpMessageLength(bytes)), null);
            requireNonNull(opSent,
                "Error on parse Sent OPEN Message, Sent OPEN Message is null");
            Preconditions.checkArgument(opSent instanceof OpenMessage,
                "An instance of OpenMessage notification is required");
            final OpenMessage sent = (OpenMessage) opSent;

            final Notification opRec = this.msgRegistry
                .parseMessage(bytes.readSlice(getBgpMessageLength(bytes)), null);
            requireNonNull(opRec,
                "Error on parse Received  OPEN Message, Received  OPEN Message is null");
            Preconditions.checkArgument(opRec instanceof OpenMessage,
                "An instance of OpenMessage notification is required");
            final OpenMessage received = (OpenMessage) opRec;

            peerUpNot.setSentOpen(new SentOpenBuilder(sent).build());
            peerUpNot.setReceivedOpen(new ReceivedOpenBuilder(received).build());

            final InformationBuilder infos = new InformationBuilder();
            if ( bytes.isReadable() ) {
                parseTlvs(infos, bytes);
                peerUpNot.setInformation(infos.build());
            }

        } catch (final BGPDocumentedException | BGPParsingException e) {
            throw new BmpDeserializationException("Error while parsing BGP Open Message.", e);
        }

        return peerUpNot.build();
    }

    @Override
    protected void addTlv(final InformationBuilder builder, final Tlv tlv) {
        if (tlv instanceof StringTlv) {
            final ImmutableList.Builder<StringInformation> stringInfoListBuilder = ImmutableList.builder();
            if ( builder.getStringInformation() != null ) {
                stringInfoListBuilder.addAll(builder.getStringInformation());
            }
            builder.setStringInformation(stringInfoListBuilder.add(new StringInformationBuilder().setStringTlv(
                new StringTlvBuilder((StringTlv) tlv).build()).build()).build());
        }
    }

    @Override
    public int getBmpMessageType() {
        return MESSAGE_TYPE;
    }

    private static int getBgpMessageLength(final ByteBuf buffer) {
        return buffer.getUnsignedShort(buffer.readerIndex() + MessageUtil.MARKER_LENGTH);
    }
}
