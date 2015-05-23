/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.spi.registry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.AbstractBmpMessageParser;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.protocol.util.ByteBufWriteUtil;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.Notification;

public class SimpleBmpMessageRegistryTest {

    private static final int MSG_TYPE = 15;

    private static final byte[] BMP_TEST_MESSAGE = {
        0x03,//version
        0x00, 0x00, 0x00, 0x0A,//message length
        0x0F,//msg type
        0x00, 0x00, 0x01, 0x01//payload
    };

    @Test
    public void testBmpMessageRegistry() throws BmpDeserializationException {
        final BmpMessageRegistry registry = new SimpleBmpMessageRegistry();
        final BmpTestParser bmpTestParser = new BmpTestParser();
        registry.registerBmpMessageParser(MSG_TYPE, bmpTestParser);
        registry.registerBmpMessageSerializer(BmpTestMessage.class, bmpTestParser);
        final BmpTestMessage message = new BmpTestMessage(257);

        assertEquals(message.toString(), ((BmpTestMessage)registry.parseMessage(Unpooled.copiedBuffer(BMP_TEST_MESSAGE))).toString());

        final ByteBuf aggregator = Unpooled.buffer(BMP_TEST_MESSAGE.length);
        registry.serializeMessage(message, aggregator);
        assertArrayEquals(BMP_TEST_MESSAGE, ByteArray.getAllBytes(aggregator));
    }

    private static final class BmpTestParser extends AbstractBmpMessageParser {

        @Override
        public void serializeMessageBody(final Notification message, final ByteBuf buffer) {
            ByteBufWriteUtil.writeUnsignedInt(((BmpTestMessage)message).getValue(), buffer);
        }

        @Override
        public Notification parseMessageBody(final ByteBuf bytes) throws BmpDeserializationException {
            return new BmpTestMessage(bytes.readUnsignedInt());
        }

        @Override
        public int getBmpMessageType() {
            return MSG_TYPE;
        }
    }

    private static final class BmpTestMessage implements Notification {

        private final long value;

        public BmpTestMessage(final long value) {
            this.value = value;
        }

        public long getValue() {
            return this.value;
        }

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return BmpTestMessage.class;
        }

        @Override
        public String toString() {
            return "BmpTestMessage [value=" + value + "]";
        }

    }

}
