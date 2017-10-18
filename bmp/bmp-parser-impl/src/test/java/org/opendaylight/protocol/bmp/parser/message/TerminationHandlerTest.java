/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.parser.message;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bmp.parser.message.TestUtil.createTerminationMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev171207.TerminationMessage;

public class TerminationHandlerTest extends AbstractBmpMessageTest {

    private static final byte[] TERMINATION_MESSAGE = {
        /*
         * 03 <- bmp version
         * 00 00 00 20 <- total length of termination message + common header lenght
         * 05 <- bmp message type - termination
         * 00 01 <- type REASON
         * 00 02 <- length
         * 00 00 <- reason = 0 (Session administratively closed)
         * 00 00 <- type STRING
         * 00 06 <- length
         * 65 72 72 6F 72 31 <- value error1
         * 00 00 <- type STRING
         * 00 06 <- length
         * 65 72 72 6F 72 31 <- value error1
         */
        (byte) 0x03,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20,
        (byte) 0x05,
        (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x02,
        (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x06,
        (byte) 0x65, (byte) 0x72, (byte) 0x72, (byte) 0x6F, (byte) 0x72, (byte) 0x31,
        (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x06,
        (byte) 0x65, (byte) 0x72, (byte) 0x72, (byte) 0x6F, (byte) 0x72, (byte) 0x31
    };

    @Test
    public void testSerializeTerminationMessage() throws BmpDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        getBmpMessageRegistry().serializeMessage(createTerminationMsg(), buffer);
        assertArrayEquals(TERMINATION_MESSAGE, ByteArray.readAllBytes(buffer));
    }


    @Test
    public void testParseTerminationMessage() throws BmpDeserializationException {
        final TerminationMessage parsedInitMsg = (TerminationMessage) getBmpMessageRegistry().parseMessage(Unpooled.copiedBuffer(TERMINATION_MESSAGE));
        assertEquals(createTerminationMsg(), parsedInitMsg);
    }
}
