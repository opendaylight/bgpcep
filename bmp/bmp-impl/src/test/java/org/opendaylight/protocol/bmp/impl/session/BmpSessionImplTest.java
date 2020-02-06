/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.parser.message.TestUtil;
import org.opendaylight.protocol.bmp.spi.parser.BmpDeserializationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.message.rev200120.InitiationMessage;

public class BmpSessionImplTest {

    private BmpSession session;
    private EmbeddedChannel channel;
    private BmpTestSessionListener listener;
    @Mock
    private ChannelHandlerContext mockedHandlerContext;

    private static InitiationMessage createInitMsg() {
        return TestUtil.createInitMsg("", "", "");
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(null).when(this.mockedHandlerContext).channel();
        doReturn(null).when(this.mockedHandlerContext).fireChannelInactive();
        this.listener = new BmpTestSessionListener();
        this.session = new BmpSessionImpl(this.listener);
        this.channel = new EmbeddedChannel(this.session);
        assertTrue(this.listener.isUp());
    }

    @Test
    public void testOnInitiate() {
        this.channel.writeInbound(createInitMsg());
        assertEquals(this.listener.getListMsg().size(), 1);
        this.channel.writeInbound(createInitMsg());
        assertEquals(this.listener.getListMsg().size(), 2);
    }

    @Test
    public void testOnTerminationAfterInitiated() {
        this.channel.writeInbound(createInitMsg());
        assertEquals(this.listener.getListMsg().size(), 1);
        this.channel.writeInbound(TestUtil.createTerminationMsg());
        assertEquals(this.listener.getListMsg().size(), 1);
        assertFalse(this.listener.isUp());
    }

    @Test
    public void testOnTermination() {
        this.channel.writeInbound(TestUtil.createTerminationMsg());
        assertEquals(this.listener.getListMsg().size(), 0);
        assertFalse(this.listener.isUp());
    }

    @Test
    public void testOnUnexpectedMessage() {
        this.channel.writeInbound(TestUtil.createPeerDownFSM());
        assertEquals(this.listener.getListMsg().size(), 0);
        assertFalse(this.listener.isUp());
    }

    @Test
    public void testExceptionCaught() throws Exception {
        this.session.exceptionCaught(this.mockedHandlerContext, new BmpDeserializationException(""));
        assertEquals(this.listener.getListMsg().size(), 0);
        assertFalse(this.listener.isUp());
    }

}
