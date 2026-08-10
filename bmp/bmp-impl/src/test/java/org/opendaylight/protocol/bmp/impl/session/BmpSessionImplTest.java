/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.parser.message.TestUtil;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.InitiationMessage;

@ExtendWith(MockitoExtension.class)
class BmpSessionImplTest {

    private BmpSession session;
    private EmbeddedChannel channel;
    private BmpTestSessionListener listener;
    @Mock
    private ChannelHandlerContext mockedHandlerContext;

    private static InitiationMessage createInitMsg() {
        return TestUtil.createInitMsg("", "", "");
    }

    @BeforeEach
    void setUp() {
        listener = new BmpTestSessionListener();
        session = new BmpSessionImpl(listener);
        channel = new EmbeddedChannel(session);
        assertTrue(listener.isUp());
    }

    @Test
    void testOnInitiate() {
        channel.writeInbound(createInitMsg());
        assertEquals(listener.getListMsg().size(), 1);
        channel.writeInbound(createInitMsg());
        assertEquals(listener.getListMsg().size(), 2);
    }

    @Test
    void testOnTerminationAfterInitiated() {
        channel.writeInbound(createInitMsg());
        assertEquals(listener.getListMsg().size(), 1);
        channel.writeInbound(TestUtil.createTerminationMsg());
        assertEquals(listener.getListMsg().size(), 1);
        assertFalse(listener.isUp());
    }

    @Test
    void testOnTermination() {
        channel.writeInbound(TestUtil.createTerminationMsg());
        assertEquals(listener.getListMsg().size(), 0);
        assertFalse(listener.isUp());
    }

    @Test
    void testOnUnexpectedMessage() {
        channel.writeInbound(TestUtil.createPeerDownFSM());
        assertEquals(listener.getListMsg().size(), 0);
        assertFalse(listener.isUp());
    }

    @Test
    void testExceptionCaught() throws Exception {
        session.exceptionCaught(mockedHandlerContext, new BmpDeserializationException(""));
        assertEquals(listener.getListMsg().size(), 0);
        assertFalse(listener.isUp());
    }
}
