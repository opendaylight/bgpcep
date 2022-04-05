/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ServiceLoader;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Timestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RdTwoOctetAs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.RouteDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.Peer.PeerDistinguisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerUpNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.PeerUpNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.peer.header.PeerHeaderBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint32;

public class AbstractBmpPerPeerMessageParserTest {
    private static final String RD = "5:3";
    private MessageRegistry msgRegistry;
    private AbstractBmpPerPeerMessageParser<?> parser;
    private final byte[] ipv6MsgWithDistinguishergBytes = {
        (byte) 0x01, (byte) 0xc0,
        //Distinguisher
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03,
         //IPV6 //2001::01
        (byte) 0x20, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        //AS 168
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xA8,
        //Peer BGP ID 1.1.1.2
        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02,
        0, 0, 0, 0, 0, 0, 0, 0
    };

    @Before
    public void setUp() {
        msgRegistry = ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst().orElseThrow()
            .getMessageRegistry();
        parser = new AbstractBmpPerPeerMessageParser<>(msgRegistry) {
            @Override
            public Notification<?> parseMessageBody(final ByteBuf bytes) {
                return null;
            }

            @Override
            public int getBmpMessageType() {
                return 0;
            }
        };
    }

    @Test
    public void testPerPeerHeader() {
        final byte[] msgBytes = {
            (byte) 0x00, (byte) 0x40, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            //IPV4 192.168.1.1
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC0, (byte) 0xA8, (byte) 0x01, (byte) 0x01,
            //AS 168
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xA8,
            //Peer BGP ID 1.1.1.1
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
            // Timestamp
            (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
        };

        final PeerHeader perHeader = AbstractBmpPerPeerMessageParser
                .parsePerPeerHeader(Unpooled.wrappedBuffer(msgBytes));

        assertEquals(perHeader, new PeerHeaderBuilder()
            .setType(PeerType.forValue(0))
            .setAdjRibInType(AdjRibInType.forValue(1))
            .setIpv4(true)
            .setAddress(new IpAddressNoZone(new Ipv4AddressNoZone("192.168.1.1")))
            .setAs(new AsNumber(Uint32.valueOf(168)))
            .setBgpId(new Ipv4AddressNoZone("1.1.1.1"))
            .setTimestampSec(new Timestamp(Uint32.valueOf(16909060)))
            .setTimestampMicro(new Timestamp(Uint32.valueOf(16909060)))
            .build());

        final ByteBuf aggregator = Unpooled.buffer();
        parser.serializePerPeerHeader(perHeader, aggregator);
        assertArrayEquals(msgBytes, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testPerPeerHeaderIpv6() {

        final PeerHeader perHeader = AbstractBmpPerPeerMessageParser
                .parsePerPeerHeader(Unpooled.wrappedBuffer(ipv6MsgWithDistinguishergBytes));

        final PeerHeaderBuilder phBuilder = new PeerHeaderBuilder()
                .setType(PeerType.L3vpn)
                .setPeerDistinguisher(new PeerDistinguisher(
                    new RouteDistinguisher(new RdTwoOctetAs("0:" + RD))))
                .setAdjRibInType(AdjRibInType.forValue(1))
                .setIpv4(false)
                .setAddress(new IpAddressNoZone(new Ipv6AddressNoZone("2001::1")))
                .setAs(new AsNumber(Uint32.valueOf(168)))
                .setBgpId(new Ipv4AddressNoZone("1.1.1.2"))
                .setTimestampSec(new Timestamp(Uint32.ZERO))
                .setTimestampMicro(new Timestamp(Uint32.ZERO));

        assertEquals(phBuilder.build(), perHeader);

        final ByteBuf aggregator = Unpooled.buffer();
        phBuilder.setTimestampSec(null);
        phBuilder.setTimestampMicro(null);
        parser.serializePerPeerHeader(phBuilder.build(), aggregator);
        assertArrayEquals(ipv6MsgWithDistinguishergBytes, ByteArray.getAllBytes(aggregator));
    }

    @Test
    public void testBgpMessageRegistry() {
        assertEquals(msgRegistry, parser.getBgpMessageRegistry());
    }

    @Test
    public void testSerializeMessageBody() {
        final PeerHeader perHeader = AbstractBmpPerPeerMessageParser
                .parsePerPeerHeader(Unpooled.wrappedBuffer(ipv6MsgWithDistinguishergBytes));

        final PeerUpNotification peerNotif = new PeerUpNotificationBuilder().setPeerHeader(perHeader).build();

        final ByteBuf aggregator = Unpooled.buffer();
        parser.serializeMessageBody(peerNotif, aggregator);
        assertArrayEquals(ipv6MsgWithDistinguishergBytes, ByteArray.getAllBytes(aggregator));
    }
}
