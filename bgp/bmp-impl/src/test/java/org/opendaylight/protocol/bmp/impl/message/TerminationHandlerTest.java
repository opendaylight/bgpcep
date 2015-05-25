/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.message;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opendaylight.protocol.bmp.impl.message.InitiationHandlerTest.createStringInfo;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bmp.impl.BmpActivator;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.protocol.bmp.spi.registry.BmpMessageRegistry;
import org.opendaylight.protocol.bmp.spi.registry.SimpleBmpExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.TerminationMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.reason.tlv.ReasonTlv.Reason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.reason.tlv.ReasonTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.termination.TlvsBuilder;

public class TerminationHandlerTest {

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

    private BmpMessageRegistry messageRegistry;

    @Before
    public void init() {
        final BGPActivator bgpActivator = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        bgpActivator.start(context);
        final SimpleBmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        BmpActivator act = new BmpActivator(context.getMessageRegistry());
        act.start(ctx);
        this.messageRegistry = ctx.getBmpMessageRegistry();
    }

    @Test
    public void testSerializeTerminationMessage() throws BmpDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        this.messageRegistry.serializeMessage(createTerminationMsg(), buffer);
        assertArrayEquals(TERMINATION_MESSAGE, ByteArray.readAllBytes(buffer));
    }


    @Test
    public void testParseTerminationMessage() throws BmpDeserializationException {
        final TerminationMessage parsedInitMsg = (TerminationMessage) this.messageRegistry.parseMessage(Unpooled.copiedBuffer(TERMINATION_MESSAGE));
        assertEquals(createTerminationMsg(), parsedInitMsg);
    }

    private static TerminationMessage createTerminationMsg() {
        final TerminationMessageBuilder terminatMsgBuilder = new TerminationMessageBuilder();
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        tlvsBuilder.setReasonTlv(new ReasonTlvBuilder().setReason(Reason.AdministrativelyClosed).build());
        tlvsBuilder.setStringInformation(Lists.newArrayList(createStringInfo("error1"), createStringInfo("error1")));
        return terminatMsgBuilder.setTlvs(tlvsBuilder.build()).build();
    }
}
