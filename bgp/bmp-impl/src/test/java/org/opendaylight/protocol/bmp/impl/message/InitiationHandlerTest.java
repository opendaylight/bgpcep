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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.InitiationMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.description.tlv.DescriptionTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.initiation.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.name.tlv.NameTlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.informations.StringInformation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.informations.StringInformationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev150512.string.tlv.StringTlvBuilder;

public class InitiationHandlerTest {

    private static final String STR_INFO = "The information field type 0";
    private static final String SYS_DESCR = "SysDescr type 1";
    private static final String SYS_NAME = "SysName type 2";

    private static final byte[] INIT_MSG_DATA = {
        /*
         * 03 <- bmp version
         * 00 00 00 4B <- total length of initiation message + common header lenght
         * 04 <- bmp message type
         * 00 02 <- initiation message type SYS_NAME
         * 00 0E <- the length of SYS_NAME
         * 53 79 73 4E 61 6D 65 20 74 79 70 65 20 32 <- value of SYS_NAME
         * 00 01 <- initiation message type SYS_DESCR
         * 00 0F <- the lenght of SYS_DESCR
         * 53 79 73 44 65 73 63 72 20 74 79 70 65 20 31 <- value of SYS_DESCR
         * 00 00 <- initiation message type STRING
         * 00 1C <- the length of STRING
         * 54 68 65 20 69 6E 66 6F 72 6D 61 74 69 6F 6E 20 66 69 65 6C 64 20 74 79 70 65 20 30 <- value of STRING
         */
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x4B, (byte) 0x04, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x0E,
        (byte) 0x53, (byte) 0x79, (byte) 0x73, (byte) 0x4E, (byte) 0x61, (byte) 0x6D, (byte) 0x65, (byte) 0x20, (byte) 0x74, (byte) 0x79,
        (byte) 0x70, (byte) 0x65, (byte) 0x20, (byte) 0x32, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x0F, (byte) 0x53, (byte) 0x79,
        (byte) 0x73, (byte) 0x44, (byte) 0x65, (byte) 0x73, (byte) 0x63, (byte) 0x72, (byte) 0x20, (byte) 0x74, (byte) 0x79, (byte) 0x70,
        (byte) 0x65, (byte) 0x20, (byte) 0x31, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1C, (byte) 0x54, (byte) 0x68, (byte) 0x65,
        (byte) 0x20, (byte) 0x69, (byte) 0x6E, (byte) 0x66, (byte) 0x6F, (byte) 0x72, (byte) 0x6D, (byte) 0x61, (byte) 0x74, (byte) 0x69,
        (byte) 0x6F, (byte) 0x6E, (byte) 0x20, (byte) 0x66, (byte) 0x69, (byte) 0x65, (byte) 0x6C, (byte) 0x64, (byte) 0x20, (byte) 0x74,
        (byte) 0x79, (byte) 0x70, (byte) 0x65, (byte) 0x20, (byte) 0x30
    };

    private BmpMessageRegistry messageRegistry;

    @Before
    public void setUp() {
        final BGPActivator bgpActivator = new BGPActivator();
        final BGPExtensionProviderContext context = new SimpleBGPExtensionProviderContext();
        bgpActivator.start(context);
        final SimpleBmpExtensionProviderContext ctx = new SimpleBmpExtensionProviderContext();
        BmpActivator act = new BmpActivator(context.getMessageRegistry());
        act.start(ctx);
        this.messageRegistry = ctx.getBmpMessageRegistry();
    }

    @Test
    public void testSerializeInitiationMessage() throws BmpDeserializationException {
        final ByteBuf buffer = Unpooled.buffer();
        messageRegistry.serializeMessage(createInitMsg(), buffer);
        assertArrayEquals(INIT_MSG_DATA, ByteArray.readAllBytes(buffer));
    }

    @Test
    public void testParseInitiationMessage() throws BmpDeserializationException {
        final InitiationMessage parsedInitMsg = (InitiationMessage) this.messageRegistry.parseMessage(Unpooled.copiedBuffer(INIT_MSG_DATA));
        assertEquals(createInitMsg(), parsedInitMsg);
    }

    private static InitiationMessage createInitMsg() {
        final InitiationMessageBuilder initMsgBuilder = new InitiationMessageBuilder();
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        tlvsBuilder.setDescriptionTlv(new DescriptionTlvBuilder().setDescription(SYS_DESCR).build());
        tlvsBuilder.setNameTlv(new NameTlvBuilder().setName(SYS_NAME).build());
        tlvsBuilder.setStringInformation(Lists.newArrayList(createStringInfo(STR_INFO)));
        return initMsgBuilder.setTlvs(tlvsBuilder.build()).build();
    }

    protected static StringInformation createStringInfo(final String string) {
        return new StringInformationBuilder().setStringTlv(new StringTlvBuilder().setStringInfo(string).build()).build();
    }
}
