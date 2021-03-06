/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Notify;

public class BGPParserTest {
    private final MessageRegistry registry = ServiceLoader.load(BGPExtensionConsumerContext.class).findFirst()
        .orElseThrow().getMessageRegistry();

    @Test
    public void testMessageToByteEncoding() {
        final BGPMessageToByteEncoder encoder = new BGPMessageToByteEncoder(this.registry);
        final ByteBuf out = Unpooled.buffer();
        encoder.encode(null, new KeepaliveBuilder().build(), out);
        assertArrayEquals(new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            0x00, 0x13, 0x04}, ByteArray.readAllBytes(out));
    }

    @Test
    public void testByteToMessageEncoding() throws Exception {
        final BGPByteToMessageDecoder decoder = new BGPByteToMessageDecoder(this.registry);
        final List<Object> out = new ArrayList<>();
        decoder.decode(null, Unpooled.wrappedBuffer(new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            0x00, 0x13, 0x04 }), out);
        assertTrue(out.get(0) instanceof Keepalive);
        decoder.decode(null, Unpooled.wrappedBuffer(new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            0x00, 0x17, 0x03, 0x02, 0x04, 0x04, 0x09 }), out);
        assertTrue(out.get(1) instanceof Notify);
    }

    @Test
    public void testHandlerFactory() {
        final BGPHandlerFactory handlers = new BGPHandlerFactory(this.registry);
        assertEquals(1, handlers.getEncoders().length);
        assertTrue(handlers.getEncoders()[0] instanceof BGPMessageToByteEncoder);
        assertEquals(2, handlers.getDecoders().length);
        assertTrue(handlers.getDecoders()[0] instanceof BGPMessageHeaderDecoder);
        assertTrue(handlers.getDecoders()[1] instanceof BGPByteToMessageDecoder);
    }
}
