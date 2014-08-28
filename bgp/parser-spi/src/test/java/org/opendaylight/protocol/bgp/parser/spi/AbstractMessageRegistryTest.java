/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yangtools.yang.binding.Notification;

public class AbstractMessageRegistryTest {

    public static final byte[] keepAliveBMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0x00, (byte) 0x13, (byte) 0x04 };

    private final AbstractMessageRegistry registry = new AbstractMessageRegistry() {

        @Override
        protected void serializeMessageImpl(final Notification message, final ByteBuf buffer) {
            buffer.writeBytes(keepAliveBMsg);
        }

        @Override
        protected Notification parseBody(int type, ByteBuf body, int messageLength) throws BGPDocumentedException {
            return new KeepaliveBuilder().build();
        }
    };

    @Test
    public void testRegistry() throws BGPDocumentedException, BGPParsingException {
        final Notification keepAlive = new KeepaliveBuilder().build();
        final ByteBuf buffer = Unpooled.buffer();
        this.registry.serializeMessage(keepAlive, buffer);
        assertArrayEquals(keepAliveBMsg, ByteArray.getAllBytes(buffer));

        final Notification not = this.registry.parseMessage(Unpooled.copiedBuffer(keepAliveBMsg));
        assertTrue(not instanceof Keepalive);
    }

    @Test
    public void testIncompleteMarker() {
        final byte[] testBytes = new byte[] { (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0x00, (byte) 0x13, (byte) 0x04 };
        try {
            this.registry.parseMessage(Unpooled.copiedBuffer(testBytes));
            Assert.fail();
        } catch (BGPDocumentedException | BGPParsingException e) {
            assertTrue(e instanceof BGPDocumentedException);
            Assert.assertEquals("Marker not set to ones.", e.getMessage());
        }
    }

    @Test
    public void testInvalidLength() {
        final byte[] testBytes = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0x00, (byte) 0x12, (byte) 0x04 };
        try {
            this.registry.parseMessage(Unpooled.copiedBuffer(testBytes));
            Assert.fail();
        } catch (BGPDocumentedException | BGPParsingException e) {
            assertTrue(e instanceof BGPDocumentedException);
            Assert.assertEquals("Message length field not within valid range.", e.getMessage());
        }
    }

    @Test
    public void testInvalidSpecifiedSize() {
        final byte[] testBytes = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0x00, (byte) 0x13, (byte) 0x04, (byte) 0x04 };
        try {
            this.registry.parseMessage(Unpooled.copiedBuffer(testBytes));
            Assert.fail();
        } catch (BGPDocumentedException | BGPParsingException e) {
            assertTrue(e instanceof BGPParsingException);
            Assert.assertTrue(e.getMessage().startsWith("Size doesn't match size specified in header."));
        }
    }
}
