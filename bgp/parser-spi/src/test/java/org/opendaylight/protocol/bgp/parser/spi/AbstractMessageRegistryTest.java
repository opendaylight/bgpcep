/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.pojo.DefaultBGPExtensionConsumerContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.KeepaliveBuilder;
import org.opendaylight.yangtools.binding.Notification;

class AbstractMessageRegistryTest {

    public static final byte[] KEEPALIVE_BMSG = new byte[] {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0x00, (byte) 0x13, (byte) 0x04
    };

    private final AbstractMessageRegistry registry = new AbstractMessageRegistry() {
        @Override
        protected void serializeMessageImpl(final Notification<?> message, final ByteBuf buffer) {
            buffer.writeBytes(KEEPALIVE_BMSG);
        }

        @Override
        protected Notification<?> parseBody(final int type, final ByteBuf body, final int messageLength,
                final PeerSpecificParserConstraint constraint) throws BGPDocumentedException {
            return new KeepaliveBuilder().build();
        }
    };

    @Test
    void testRegistry() throws BGPDocumentedException, BGPParsingException {
        final Notification<?> keepAlive = new KeepaliveBuilder().build();
        final ByteBuf buffer = Unpooled.buffer();
        registry.serializeMessage(keepAlive, buffer);
        assertArrayEquals(KEEPALIVE_BMSG, ByteArray.getAllBytes(buffer));

        final Notification<?> not = registry.parseMessage(Unpooled.copiedBuffer(KEEPALIVE_BMSG), null);
        assertInstanceOf(Keepalive.class, not);
    }

    @Test
    void testIncompleteMarker() {
        final byte[] testBytes = new byte[] {
            (byte) 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x13, (byte) 0x04
        };

        final BGPDocumentedException ex = assertThrows(BGPDocumentedException.class,
            () -> registry.parseMessage(Unpooled.copiedBuffer(testBytes), null));
        assertEquals("Marker not set to ones.", ex.getMessage());
    }

    @Test
    void testInvalidLength() {
        final byte[] testBytes = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x12, (byte) 0x04
        };
        final BGPDocumentedException ex = assertThrows(BGPDocumentedException.class,
            () -> registry.parseMessage(Unpooled.copiedBuffer(testBytes), null));
        assertEquals("Message length field not within valid range.", ex.getMessage());
    }

    @Test
    void testInvalidSpecifiedSize() {
        final byte[] testBytes = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x13, (byte) 0x04, (byte) 0x04
        };
        final BGPParsingException ex = assertThrows(BGPParsingException.class,
            () -> registry.parseMessage(Unpooled.copiedBuffer(testBytes), null));
        assertThat(ex.getMessage()).startsWith("Size doesn't match size specified in header.");
    }

    @Test
    void testBGPHeaderParser() {
        final MessageRegistry msgReg = new DefaultBGPExtensionConsumerContext().getMessageRegistry();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> msgReg.parseMessage(Unpooled.copiedBuffer(new byte[] { (byte) 0, (byte) 0 }), null));
        assertEquals("Too few bytes in passed array. Passed: 2. Expected: >= 19.", ex.getMessage());
    }

    @Test
    void testMessageParser() {
        final MessageRegistry msgReg = new DefaultBGPExtensionConsumerContext().getMessageRegistry();
        final NullPointerException ex = assertThrows(NullPointerException.class,
            () -> msgReg.serializeMessage(null, Unpooled.EMPTY_BUFFER));
        assertEquals("BGPMessage is mandatory.", ex.getMessage());
    }
}
