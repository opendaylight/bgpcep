/*
 * Copyright (c) 2016 AT&T Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.BgpExtendedMessageUtil;
import org.opendaylight.protocol.bgp.parser.impl.message.open.BgpExtendedMessageCapabilityHandler;

class BgpExtendedMessageCapabilityHandlerTest {

    @Test
    void testBgpExtendedMessageCapabilityHandler() throws BGPDocumentedException, BGPParsingException {
        final BgpExtendedMessageCapabilityHandler handler = new BgpExtendedMessageCapabilityHandler();

        final byte[] bgpExeBytes = {(byte) 0x06, (byte) 0x00};

        final ByteBuf buffer = Unpooled.buffer(bgpExeBytes.length);
        handler.serializeCapability(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY, buffer);
        Assertions.assertArrayEquals(bgpExeBytes, buffer.array());
        Assertions.assertEquals(handler.parseCapability(Unpooled.wrappedBuffer(bgpExeBytes)),
                BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY);

        final byte[] bgpExeBytes2 = {(byte) 0x40, (byte) 0x06};
        buffer.clear();
        handler.serializeCapability(BgpExtendedMessageUtil.EXTENDED_MESSAGE_CAPABILITY, buffer);
        Assertions.assertNotSame(bgpExeBytes2, buffer.array());
    }

}
