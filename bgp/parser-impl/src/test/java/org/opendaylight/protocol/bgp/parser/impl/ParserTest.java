/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.UnknownHostException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.ProtocolVersion;
import org.opendaylight.yangtools.yang.binding.Notification;

public class ParserTest {

    private static final byte[] openBMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0x00, (byte) 0x1d, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0xb4,
        (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x00 };

    private static final byte[] keepAliveBMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0x00, (byte) 0x13, (byte) 0x04 };

    private static final byte[] notificationBMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x17, (byte) 0x03, (byte) 0x02, (byte) 0x04, (byte) 0x04, (byte) 0x09 };

    private static final byte[] updMsgWithUnrecognizedAttribute = new byte[] {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x79, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x62, (byte) 0x90, (byte) 0x0e, (byte) 0x00, (byte) 0x34, (byte) 0x40, (byte) 0x04, (byte) 0x47, (byte) 0x04, (byte) 0x0a, (byte) 0x19,
        (byte) 0x02, (byte) 0x1b, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x27, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte)0x00,
        (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x1a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x02, (byte)0x01,
        (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte)0x00,
        (byte) 0x43, (byte) 0x40, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x40, (byte) 0x02, (byte) 0x00, (byte) 0x40, (byte) 0x05, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte)0x64,
        (byte) 0x00, (byte) 0x63, (byte) 0x19, (byte) 0x04, (byte) 0x02, (byte) 0x00, (byte) 0x08, (byte) 0x4f, (byte) 0x66, (byte) 0x2d, (byte) 0x39, (byte) 0x6b, (byte) 0x2d, (byte) 0x30, (byte)0x33,
        (byte) 0x04, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x72, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0x2b, (byte) 0x2b, (byte) 0x2b, (byte) 0x2b
    };

    static MessageRegistry reg;

    @BeforeClass
    public static void setupClass() throws Exception {
        reg = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry();
    }

    @Test
    public void testHeaderErrors() throws BGPParsingException, BGPDocumentedException {
        byte[] wrong = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00 };
        wrong = ByteArray.cutBytes(wrong, 16);
        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(wrong));
            fail("Exception should have occcured.");
        } catch (final IllegalArgumentException e) {
            assertEquals("Too few bytes in passed array. Passed: " + wrong.length + ". Expected: >= 19.", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void testBadMsgType() throws BGPParsingException {
        final byte[] bytes = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x13, (byte) 0x08 };
        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bytes));
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.BAD_MSG_TYPE, e.getError());
            return;
        }
        fail();
    }

    @Test
    public void testKeepAliveMsg() throws BGPParsingException, BGPDocumentedException {
        final Notification keepAlive = new KeepaliveBuilder().build();
        final ByteBuf buffer = Unpooled.buffer();
        ParserTest.reg.serializeMessage(keepAlive, buffer);
        assertArrayEquals(keepAliveBMsg, ByteArray.getAllBytes(buffer));

        final Notification m = ParserTest.reg.parseMessage(Unpooled.copiedBuffer(ByteArray.getAllBytes(buffer)));

        assertTrue(m instanceof Keepalive);
    }

    @Test
    public void testBadKeepAliveMsg() throws BGPParsingException {
        final byte[] bytes = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x14, (byte) 0x04, (byte) 0x05 };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bytes));
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertThat(e.getMessage(), containsString("Message length field not within valid range."));
            assertEquals(BGPError.BAD_MSG_LENGTH, e.getError());
            return;
        }
        fail();
    }

    @Test
    public void testOpenMessage() throws UnknownHostException, BGPParsingException, BGPDocumentedException {
        final Notification open = new OpenBuilder().setMyAsNumber(100).setHoldTimer(180).setBgpIdentifier(new Ipv4Address("20.20.20.20")).setVersion(
                new ProtocolVersion((short) 4)).build();
        final ByteBuf bytes = Unpooled.buffer();
        ParserTest.reg.serializeMessage(open, bytes);
        assertArrayEquals(openBMsg, ByteArray.getAllBytes(bytes));

        final Notification m = ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bytes));

        assertTrue(m instanceof Open);
        assertEquals(100, ((Open) m).getMyAsNumber().intValue());
        assertEquals(180, ((Open) m).getHoldTimer().intValue());
        assertEquals(new Ipv4Address("20.20.20.20"), ((Open) m).getBgpIdentifier());
        assertTrue(((Open) m).getBgpParameters().isEmpty());
    }

    @Test
    public void testBadHoldTimeError() throws BGPParsingException {
        final byte[] bMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x1d, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0x01, (byte) 0x14,
            (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x00 };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bMsg));
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Hold time value not acceptable.", e.getMessage());
            assertEquals(BGPError.HOLD_TIME_NOT_ACC, e.getError());
            return;
        }
        fail();
    }

    @Test
    public void testBadMsgLength() throws BGPParsingException {
        final byte[] bMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x1b, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0xb4, (byte) 0xff,
            (byte) 0xff, (byte) 0xff };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bMsg));
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Open message too small.", e.getMessage());
            assertEquals(BGPError.BAD_MSG_LENGTH, e.getError());
        }
    }

    @Test
    public void testBadVersion() throws BGPParsingException {
        final byte[] bMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x1d, (byte) 0x01, (byte) 0x08, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0xb4, (byte) 0x14,
            (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x00 };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bMsg));
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals("BGP Protocol version 8 not supported.", e.getMessage());
            assertEquals(BGPError.VERSION_NOT_SUPPORTED, e.getError());
            return;
        }
        fail();
    }

    @Test
    public void testNotificationMsg() throws BGPParsingException, BGPDocumentedException {
        Notification notMsg = new NotifyBuilder().setErrorCode(BGPError.OPT_PARAM_NOT_SUPPORTED.getCode()).setErrorSubcode(
                BGPError.OPT_PARAM_NOT_SUPPORTED.getSubcode()).setData(new byte[] { 4, 9 }).build();
        final ByteBuf bytes = Unpooled.buffer();
        ParserTest.reg.serializeMessage(notMsg, bytes);
        assertArrayEquals(notificationBMsg, ByteArray.subByte(bytes.array(),0,bytes.writerIndex()));

        Notification m = ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bytes));

        assertTrue(m instanceof Notify);
        assertEquals(BGPError.OPT_PARAM_NOT_SUPPORTED, BGPError.forValue(((Notify) m).getErrorCode(), ((Notify) m).getErrorSubcode()));
        assertArrayEquals(new byte[] { 4, 9 }, ((Notify) m).getData());

        notMsg = new NotifyBuilder().setErrorCode(BGPError.CONNECTION_NOT_SYNC.getCode()).setErrorSubcode(
            BGPError.CONNECTION_NOT_SYNC.getSubcode()).build();

        bytes.clear();

        ParserTest.reg.serializeMessage(notMsg, bytes);

        m = ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bytes));

        assertTrue(m instanceof Notify);
        assertEquals(BGPError.CONNECTION_NOT_SYNC, BGPError.forValue(((Notify) m).getErrorCode(), ((Notify) m).getErrorSubcode()));
        assertNull(((Notify) m).getData());
    }

    @Test
    public void testWrongLength() throws BGPParsingException {
        final byte[] bMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x14, (byte) 0x03, (byte) 0x02 };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bMsg));
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Notification message too small.", e.getMessage());
            assertEquals(BGPError.BAD_MSG_LENGTH, e.getError());
            return;
        }
        fail();
    }

    @Test
    public void testUnrecognizedError() throws BGPParsingException, BGPDocumentedException {
        final byte[] bMsg = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x15, (byte) 0x03, (byte) 0x02, (byte) 0xaa };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bMsg));
            fail("Exception should have occured.");
        } catch (final IllegalArgumentException e) {
            assertEquals("BGP Error code 2 and subcode 170 not recognized.", e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void testParseUpdMsgWithUnrecognizedAttribute() throws BGPDocumentedException, BGPParsingException {
        try {
            reg.parseMessage(Unpooled.copiedBuffer(updMsgWithUnrecognizedAttribute));
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Well known attribute not recognized.", e.getMessage());
            assertEquals(BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED, e.getError());
            return;
        }
        fail();
    }
}
