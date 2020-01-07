/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import org.opendaylight.protocol.bgp.parser.impl.message.update.NextHopAttributeParser;
import org.opendaylight.protocol.bgp.parser.impl.message.update.OriginAttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.pojo.ServiceLoaderBGPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Notify;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.NotifyBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev180329.Update;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefresh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.RouteRefreshBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev180329.UnicastSubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

public class ParserTest {
    private static final byte[] OPEN_BMSG = new byte[] {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0x00, (byte) 0x1d, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0xb4,
        (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x00
    };

    private static final byte[] KEEPALIVE_BMSG = new byte[] {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0x00, (byte) 0x13, (byte) 0x04
    };

    private static final byte[] NOTIFICATION_BMSG = new byte[] {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0x00, (byte) 0x17, (byte) 0x03, (byte) 0x02, (byte) 0x04, (byte) 0x04, (byte) 0x09
    };

    private static final byte[] UPD_MSG_WITH_UNRECOGNIZED_ATTRIBUTE = new byte[] {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0x00, (byte) 0x79, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x62, (byte) 0x90, (byte) 0x0e, (byte) 0x00, (byte) 0x34, (byte) 0x40, (byte) 0x04, (byte) 0x47,
        (byte) 0x04, (byte) 0x0a, (byte) 0x19, (byte) 0x02, (byte) 0x1b, (byte) 0x00, (byte) 0x00, (byte) 0x01,
        (byte) 0x00, (byte) 0x27, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x1a, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x02,
        (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x28, (byte) 0x02,
        (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x43, (byte) 0x40, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x40, (byte) 0x02, (byte) 0x00,
        (byte) 0x40, (byte) 0x05, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte )0x64, (byte) 0x00,
        (byte) 0x63, (byte) 0x19, (byte) 0x04, (byte) 0x02, (byte) 0x00, (byte) 0x08, (byte) 0x4f, (byte) 0x66,
        (byte) 0x2d, (byte) 0x39, (byte) 0x6b, (byte) 0x2d, (byte) 0x30, (byte) 0x33, (byte) 0x04, (byte) 0x03,
        (byte) 0x00, (byte) 0x01, (byte) 0x72, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x04, (byte) 0x2b,
        (byte) 0x2b, (byte) 0x2b, (byte) 0x2b
    };

    // Update message with one IPv4 prefix NLRI and all mandatory path attributes present
    private static final byte[] UPD_MSG_WITH_MANDATORY_ATTRIBUTES_PRESENT = new byte[] {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0x00, (byte) 0x38, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1c, (byte) 0x40,
        (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x40, (byte) 0x02, (byte) 0x00, (byte) 0x40, (byte) 0x03,
        (byte) 0x04, (byte) 0x0a, (byte) 0x12, (byte) 0xa2, (byte) 0xf1, (byte) 0x80, (byte) 0x04, (byte) 0x04,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x05, (byte) 0x04, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x64, (byte) 0x20, (byte) 0xc0, (byte) 0xa8, (byte) 0xfe, (byte) 0x03
    };

    // Update message with one IPv4 prefix NLRI and all mandatory path attributes except ORIGIN present
    private static final byte[] UPD_MSG_WITH_ONE_MANDATORY_ATTRIBUTE_NOT_PRESENT = new byte[] {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0x00, (byte) 0x34, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x40,
        (byte) 0x02, (byte) 0x00, (byte) 0x40, (byte) 0x03, (byte) 0x04, (byte) 0x0a, (byte) 0x12, (byte) 0xa2,
        (byte) 0xf1, (byte) 0x80, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x40, (byte) 0x05, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64, (byte) 0x20,
        (byte) 0xc0, (byte) 0xa8, (byte) 0xfe, (byte) 0x03
    };

    // Update message with one IPv4 prefix NLRI and ORIGIN, AS_PATH and NEXT_HOP mandatory path attributes not present
    private static final byte[] UPD_MSG_WITH_MULTIPLE_MANDATORY_ATTRIBUTES_NOT_PRESENT = new byte[] {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0x00, (byte) 0x2a, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0e, (byte) 0x80,
        (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40, (byte) 0x05,
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x64, (byte) 0x20, (byte) 0xc0, (byte) 0xa8,
        (byte) 0xfe, (byte) 0x03
    };

    private static final byte[] RR_MSG = new byte[] {
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0x00, (byte) 0x17, (byte) 0x05, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01
    };

    private static MessageRegistry reg;

    @BeforeClass
    public static void setupClass() throws Exception {
        reg = ServiceLoaderBGPExtensionProviderContext.getSingletonInstance().getMessageRegistry();
    }

    @Test
    public void testHeaderErrors() throws BGPParsingException, BGPDocumentedException {
        byte[] wrong = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00
        };
        wrong = ByteArray.cutBytes(wrong, 16);
        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(wrong), null);
            fail("Exception should have occcured.");
        } catch (final IllegalArgumentException e) {
            assertEquals("Too few bytes in passed array. Passed: " + wrong.length + ". Expected: >= 19.",
                e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void testBadMsgType() throws BGPParsingException {
        final byte[] bytes = {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x13, (byte) 0x08
        };
        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bytes), null);
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
        assertArrayEquals(KEEPALIVE_BMSG, ByteArray.getAllBytes(buffer));

        final Notification m = ParserTest.reg.parseMessage(Unpooled.copiedBuffer(ByteArray.getAllBytes(buffer)), null);
        assertTrue(m instanceof Keepalive);
    }

    @Test
    public void testBadKeepAliveMsg() throws BGPParsingException {
        final byte[] bytes = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x14, (byte) 0x04, (byte) 0x05
        };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bytes), null);
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
        final Notification open = new OpenBuilder()
                .setMyAsNumber(Uint16.valueOf(100))
                .setHoldTimer(Uint16.valueOf(180))
                .setBgpIdentifier(new Ipv4AddressNoZone("20.20.20.20"))
                .setVersion(new ProtocolVersion(Uint8.valueOf(4)))
                .build();
        final ByteBuf bytes = Unpooled.buffer();
        ParserTest.reg.serializeMessage(open, bytes);
        assertArrayEquals(OPEN_BMSG, ByteArray.getAllBytes(bytes));

        final Notification m = ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bytes), null);
        assertTrue(m instanceof Open);

        final Open mo = (Open) m;
        assertEquals(100, mo.getMyAsNumber().intValue());
        assertEquals(180, mo.getHoldTimer().intValue());
        assertEquals(new Ipv4Address("20.20.20.20"), mo.getBgpIdentifier());
        assertTrue(mo.getBgpParameters().isEmpty());
    }

    @Test
    public void testBadHoldTimeError() throws BGPParsingException {
        final byte[] bMsg = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x1d, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0x01,
            (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x00
        };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bMsg), null);
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
        final byte[] bMsg = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x1b, (byte) 0x01, (byte) 0x04, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0xb4,
            (byte) 0xff, (byte) 0xff, (byte) 0xff
        };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bMsg), null);
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Open message too small.", e.getMessage());
            assertEquals(BGPError.BAD_MSG_LENGTH, e.getError());
        }
    }

    @Test
    public void testBadVersion() throws BGPParsingException {
        final byte[] bMsg = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x1d, (byte) 0x01, (byte) 0x08, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0xb4,
            (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x00
        };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bMsg), null);
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
        Notification notMsg = new NotifyBuilder().setErrorCode(BGPError.OPT_PARAM_NOT_SUPPORTED.getCode())
            .setErrorSubcode(BGPError.OPT_PARAM_NOT_SUPPORTED.getSubcode()).setData(new byte[] { 4, 9 }).build();
        final ByteBuf bytes = Unpooled.buffer();
        ParserTest.reg.serializeMessage(notMsg, bytes);
        assertArrayEquals(NOTIFICATION_BMSG, ByteArray.subByte(bytes.array(),0,bytes.writerIndex()));

        Notification msg = ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bytes), null);

        assertTrue(msg instanceof Notify);
        assertEquals(BGPError.OPT_PARAM_NOT_SUPPORTED, BGPError.forValue(((Notify) msg).getErrorCode(),
            ((Notify) msg).getErrorSubcode()));
        assertArrayEquals(new byte[] { 4, 9 }, ((Notify) msg).getData());

        notMsg = new NotifyBuilder().setErrorCode(BGPError.CONNECTION_NOT_SYNC.getCode()).setErrorSubcode(
            BGPError.CONNECTION_NOT_SYNC.getSubcode()).build();

        bytes.clear();

        ParserTest.reg.serializeMessage(notMsg, bytes);

        msg = ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bytes), null);

        assertTrue(msg instanceof Notify);
        assertEquals(BGPError.CONNECTION_NOT_SYNC, BGPError.forValue(((Notify) msg).getErrorCode(),
            ((Notify) msg).getErrorSubcode()));
        assertNull(((Notify) msg).getData());
    }

    @Test
    public void testWrongLength() throws BGPParsingException {
        final byte[] bMsg = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x14, (byte) 0x03, (byte) 0x02
        };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bMsg), null);
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
        final byte[] bMsg = new byte[] {
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0x00, (byte) 0x15, (byte) 0x03, (byte) 0x02, (byte) 0xaa
        };

        try {
            ParserTest.reg.parseMessage(Unpooled.copiedBuffer(bMsg), null);
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
            reg.parseMessage(Unpooled.copiedBuffer(UPD_MSG_WITH_UNRECOGNIZED_ATTRIBUTE), null);
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals("Well known attribute not recognized.", e.getMessage());
            assertEquals(BGPError.WELL_KNOWN_ATTR_NOT_RECOGNIZED, e.getError());
            return;
        }
        fail();
    }

    @Test
    public void testParseUpdMsgWithMandatoryAttributesPresent() throws BGPDocumentedException, BGPParsingException {
        try {
            final Notification msg = reg.parseMessage(Unpooled.copiedBuffer(UPD_MSG_WITH_MANDATORY_ATTRIBUTES_PRESENT),
                null);
            assertTrue(msg instanceof Update);
        } catch (final BGPDocumentedException e) {
            fail("Exception should not have occured.");
        }
    }

    @Test
    public void testParseUpdMsgWithOneMandatoryAttributeNotPresent()
        throws BGPDocumentedException, BGPParsingException {
        try {
            reg.parseMessage(Unpooled.copiedBuffer(UPD_MSG_WITH_ONE_MANDATORY_ATTRIBUTE_NOT_PRESENT), null);
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.MANDATORY_ATTR_MISSING_MSG + "ORIGIN", e.getMessage());
            assertEquals(BGPError.WELL_KNOWN_ATTR_MISSING, e.getError());
            assertArrayEquals(new byte[] { OriginAttributeParser.TYPE }, e.getData());
            return;
        }
        fail();
    }

    @Test
    public void testParseUpdMsgWithMultipleMandatoryAttributesNotPresent()
        throws BGPDocumentedException, BGPParsingException {
        try {
            reg.parseMessage(Unpooled.copiedBuffer(UPD_MSG_WITH_MULTIPLE_MANDATORY_ATTRIBUTES_NOT_PRESENT), null);
            fail("Exception should have occured.");
        } catch (final BGPDocumentedException e) {
            assertEquals(BGPError.MANDATORY_ATTR_MISSING_MSG + "NEXT_HOP", e.getMessage());
            assertEquals(BGPError.WELL_KNOWN_ATTR_MISSING, e.getError());
            assertArrayEquals(new byte[] { NextHopAttributeParser.TYPE }, e.getData());
            return;
        }
        fail();
    }

    @Test
    public void testRouteRefreshMsg() throws BGPDocumentedException, BGPParsingException {
        final Notification rrMsg = new RouteRefreshBuilder().setAfi(Ipv4AddressFamily.class)
            .setSafi(UnicastSubsequentAddressFamily.class).build();
        final ByteBuf buffer = Unpooled.buffer();
        ParserTest.reg.serializeMessage(rrMsg, buffer);
        assertArrayEquals(RR_MSG, ByteArray.getAllBytes(buffer));

        final Notification m = ParserTest.reg.parseMessage(
            Unpooled.copiedBuffer(ByteArray.getAllBytes(buffer)), null);

        assertTrue(m instanceof RouteRefresh);
    }
}
