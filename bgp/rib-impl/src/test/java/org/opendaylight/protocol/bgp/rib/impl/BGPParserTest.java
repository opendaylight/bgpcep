/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Notify;

class BGPParserTest {
    private final MessageRegistry registry = ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst()
        .orElseThrow().getMessageRegistry();

    @Test
    void testMessageToByteEncoding() {
        final BGPMessageToByteEncoder encoder = new BGPMessageToByteEncoder(this.registry);
        final ByteBuf out = Unpooled.buffer();
        encoder.encode(null, new KeepaliveBuilder().build(), out);
        assertArrayEquals(new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            0x00, 0x13, 0x04}, ByteArray.readAllBytes(out));
    }

    @Test
    void testByteToMessageEncoding() throws Exception {
        final BGPByteToMessageDecoder decoder = new BGPByteToMessageDecoder(this.registry);
        final List<Object> out = new ArrayList<>();
        decoder.decode(null, Unpooled.wrappedBuffer(new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            0x00, 0x13, 0x04 }), out);
        assertInstanceOf(Keepalive.class, out.get(0));
        decoder.decode(null, Unpooled.wrappedBuffer(new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            0x00, 0x17, 0x03, 0x02, 0x04, 0x04, 0x09 }), out);
        assertInstanceOf(Notify.class, out.get(1));
    }

    @Test
    void testHandlerFactory() {
        final BGPHandlerFactory handlers = new BGPHandlerFactory(this.registry);
        assertEquals(1, handlers.getEncoders().length);
        assertInstanceOf(BGPMessageToByteEncoder.class, handlers.getEncoders()[0]);
        assertEquals(2, handlers.getDecoders().length);
        assertInstanceOf(BGPMessageHeaderDecoder.class, handlers.getDecoders()[0]);
        assertInstanceOf(BGPByteToMessageDecoder.class, handlers.getDecoders()[1]);
    }
}
