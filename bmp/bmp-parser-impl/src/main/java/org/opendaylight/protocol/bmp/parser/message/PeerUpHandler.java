/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.parser.message;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageUtil;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpPerPeerMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.parser.BmpTlvRegistry;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.protocol.util.Ipv6Util;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerUpNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.up.Information;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.up.InformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.up.ReceivedOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.up.SentOpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.string.informations.StringInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.string.informations.StringInformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.string.tlv.StringTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.string.tlv.StringTlvBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public class PeerUpHandler extends AbstractBmpPerPeerMessageParser<InformationBuilder> {
    private static final int MESSAGE_TYPE = 3;
    private final MessageRegistry msgRegistry;

    public PeerUpHandler(final MessageRegistry bgpMssageRegistry, final BmpTlvRegistry tlvRegistry) {
        super(bgpMssageRegistry, tlvRegistry);
        msgRegistry = getBgpMessageRegistry();
    }

    @Override
    public void serializeMessageBody(final Notification<?> message, final ByteBuf buffer) {
        super.serializeMessageBody(message, buffer);
        checkArgument(message instanceof PeerUpNotification, "An instance of Peer Up notification is required");
        final PeerUpNotification peerUp = (PeerUpNotification) message;

        if (peerUp.getLocalAddress().getIpv4AddressNoZone() != null) {
            buffer.writeZero(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
            Ipv4Util.writeIpv4Address(peerUp.getLocalAddress().getIpv4AddressNoZone(), buffer);
        } else {
            Ipv6Util.writeIpv6Address(peerUp.getLocalAddress().getIpv6AddressNoZone(), buffer);
        }
        ByteBufUtils.write(buffer, peerUp.getLocalPort().getValue());
        ByteBufUtils.write(buffer, peerUp.getRemotePort().getValue());

        msgRegistry.serializeMessage(new OpenBuilder(peerUp.getSentOpen()).build(), buffer);
        msgRegistry.serializeMessage(new OpenBuilder(peerUp.getReceivedOpen()).build(), buffer);
        serializeTlvs(peerUp.getInformation(), buffer);
    }

    private void serializeTlvs(final Information tlvs, final ByteBuf output) {
        if (tlvs != null) {
            for (final StringInformation stringInfo : tlvs.nonnullStringInformation()) {
                if (stringInfo.getStringTlv() != null) {
                    serializeTlv(stringInfo.getStringTlv(), output);
                }
            }
        }
    }

    @Override
    public PeerUpNotification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
        final PeerUpNotificationBuilder peerUpNot = new PeerUpNotificationBuilder()
                .setPeerHeader(parsePerPeerHeader(bytes));

        if (peerUpNot.getPeerHeader().getIpv4()) {
            bytes.skipBytes(Ipv6Util.IPV6_LENGTH - Ipv4Util.IP4_LENGTH);
            peerUpNot.setLocalAddress(new IpAddressNoZone(Ipv4Util.addressForByteBuf(bytes)));
        } else {
            peerUpNot.setLocalAddress(new IpAddressNoZone(Ipv6Util.addressForByteBuf(bytes)));
        }
        peerUpNot.setLocalPort(new PortNumber(ByteBufUtils.readUint16(bytes)));
        peerUpNot.setRemotePort(new PortNumber(ByteBufUtils.readUint16(bytes)));
        try {
            final Notification<?> opSent = msgRegistry
                    .parseMessage(bytes.readSlice(getBgpMessageLength(bytes)), null);
            requireNonNull(opSent, "Error on parse Sent OPEN Message, Sent OPEN Message is null");
            checkArgument(opSent instanceof OpenMessage, "An instance of OpenMessage notification is required");
            final OpenMessage sent = (OpenMessage) opSent;

            final Notification<?> opRec = msgRegistry
                    .parseMessage(bytes.readSlice(getBgpMessageLength(bytes)), null);
            requireNonNull(opRec, "Error on parse Received  OPEN Message, Received  OPEN Message is null");
            checkArgument(opRec instanceof OpenMessage, "An instance of OpenMessage notification is required");
            final OpenMessage received = (OpenMessage) opRec;

            peerUpNot.setSentOpen(new SentOpenBuilder(sent).build());
            peerUpNot.setReceivedOpen(new ReceivedOpenBuilder(received).build());

            final InformationBuilder infos = new InformationBuilder();
            if (bytes.isReadable()) {
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
            if (builder.getStringInformation() != null) {
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
