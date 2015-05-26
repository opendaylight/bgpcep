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
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Timestamp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.AdjRibInType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.PeerType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.peer.header.PeerHeaderBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Created by cgasparini on 18.5.2015.
 */
public class AbstractBmpPerPeerMessageParserTest {
    protected BGPExtensionProviderContext ctx;
    private AbstractBmpPerPeerMessageParser<?> parser;

    @Before
    public void setUp() {
        this.ctx = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance();
        final MessageRegistry msgRegistry = this.ctx.getMessageRegistry();
        parser = new AbstractBmpPerPeerMessageParser<Builder<?>>(msgRegistry) {
            @Override
            public Notification parseMessageBody(final ByteBuf bytes) {
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
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, //IPV4
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC0, (byte) 0xA8, (byte) 0x01, (byte) 0x01, //192.168.1.1
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xA8,  //AS 168
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,  //Peer BGP ID 1.1.1.1
            (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, // Timestamp
        };

        final PeerHeader perHeader = this.parser.parsePerPeerHeader(Unpooled.wrappedBuffer(msgBytes));

        final PeerHeaderBuilder phBuilder = new PeerHeaderBuilder();
        phBuilder.setType(PeerType.forValue(0));
        phBuilder.setAdjRibInType(AdjRibInType.forValue(1));
        phBuilder.setIpv4(true);
        phBuilder.setAddress(new IpAddress(new Ipv4Address("192.168.1.1")));
        phBuilder.setAs(new AsNumber(168L));
        phBuilder.setBgpId(new Ipv4Address("1.1.1.1"));
        phBuilder.setTimestampSec(new Timestamp(16909060L));
        phBuilder.setTimestampMicro(new Timestamp(16909060L));
        assertEquals(perHeader, phBuilder.build());

        ByteBuf aggregator = Unpooled.buffer();
        this.parser.serializePerPeerHeader(perHeader, aggregator);
        assertArrayEquals(msgBytes, ByteArray.getAllBytes(aggregator));
    }
}
