/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.MessageRegistry;
import org.opendaylight.protocol.pcep.spi.pojo.ServiceLoaderPCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Close;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.keepalive.message.KeepaliveMessageBuilder;

public class PCEPParserTest {

    private final MessageRegistry registry =
        ServiceLoaderPCEPExtensionProviderContext.getSingletonInstance().getMessageHandlerRegistry();

    @Test
    public void testMessageToByteEncoding() {
        PCEPMessageToByteEncoder encoder = new PCEPMessageToByteEncoder(this.registry);
        ByteBuf out = Unpooled.buffer();
        encoder.encode(
            null, new KeepaliveBuilder().setKeepaliveMessage(new KeepaliveMessageBuilder().build()).build(), out);
        assertArrayEquals(new byte[] { 32, 2, 0, 4 }, ByteArray.readAllBytes(out));
    }

    @Test
    public void testByteToMessageEncoding() {
        PCEPByteToMessageDecoder decoder = new PCEPByteToMessageDecoder(this.registry);
        List<Object> out = new ArrayList<>();
        decoder.decode(null,
            Unpooled.wrappedBuffer(new byte[] {
                32, 2, 0, 4
            }),
            out);
        assertTrue(out.get(0) instanceof Keepalive);
        decoder.decode(null,
            Unpooled.wrappedBuffer(new byte[] {
                0x20, 0x07, 0, 0x0C, 0x0F, 0x10, 0, 8,
                0, 0, 0, 5
            }),
            out);
        assertTrue(out.get(1) instanceof Close);
        decoder.decode(null,
            Unpooled.wrappedBuffer(new byte[] {
                0x20, 06, 00, 0x18, 0x21, 0x10, 00, 0x0c,
                00, 00, 00, 00, 00, 00, 00, 01,
                0x0d, 0x10, 00, 0x08, 00, 00, 0x18, 02
            }),
            out);
        assertTrue(out.get(2) instanceof Pcerr);
    }

    @Test
    public void testHandlerFactory() {
        PCEPHandlerFactory handlers = new PCEPHandlerFactory(this.registry);
        assertEquals(1, handlers.getEncoders().length);
        assertTrue(handlers.getEncoders()[0] instanceof PCEPMessageToByteEncoder);
        assertEquals(2, handlers.getDecoders().length);
        assertTrue(handlers.getDecoders()[0] instanceof PCEPMessageHeaderDecoder);
        assertTrue(handlers.getDecoders()[1] instanceof PCEPByteToMessageDecoder);
    }
}
