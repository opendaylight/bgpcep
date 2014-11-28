/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yangtools.yang.binding.Notification;

public class FiniteStateMachineTest extends AbstractPCEPSessionTest {

    private DefaultPCEPSessionNegotiator serverSession;

    @Before
    public void setup() {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open localPrefs = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder().setKeepalive(
                (short) 1).build();
        this.serverSession = new DefaultPCEPSessionNegotiator(new DefaultPromise<PCEPSessionImpl>(GlobalEventExecutor.INSTANCE), this.channel, this.listener, (short) 1, 20, localPrefs);
    }

    /**
     * Both PCEs accept session characteristics. Also tests KeepAliveTimer and error message and when pce attempts to
     * establish pce session for the 2nd time.
     *
     * @throws Exception
     */
    @Test
    public void testSessionCharsAccBoth() throws Exception {
        this.serverSession.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof Open);
        this.serverSession.handleMessage(this.openMsg);
        assertEquals(2, this.msgsSend.size());
        assertTrue(this.msgsSend.get(1) instanceof Keepalive);
        this.serverSession.handleMessage(this.kaMsg);
        assertEquals(this.serverSession.getState(), DefaultPCEPSessionNegotiator.State.FINISHED);
    }

    /**
     * Mock PCE does not accept session characteristics the first time.
     *
     * @throws Exception
     */
    @Test
    public void testSessionCharsAccMe() throws Exception {
        this.serverSession.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof Open);
        final Open remote = (Open) this.msgsSend.get(0);
        this.serverSession.handleMessage(this.openMsg);
        assertEquals(2, this.msgsSend.size());
        assertTrue(this.msgsSend.get(1) instanceof Keepalive);
        this.serverSession.handleMessage(Util.createErrorMessage(PCEPErrors.NON_ACC_NEG_SESSION_CHAR, remote.getOpenMessage().getOpen()));
        assertEquals(3, this.msgsSend.size());
        assertTrue(this.msgsSend.get(2) instanceof Open);
        this.serverSession.handleMessage(this.kaMsg);
        assertEquals(this.serverSession.getState(), DefaultPCEPSessionNegotiator.State.FINISHED);
    }

    /**
     * Sending different PCEP Message than Open in session establishment phase.
     *
     * @throws Exception
     */
    @Test
    public void testErrorOneOne() throws Exception {
        this.serverSession.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof Open);
        this.serverSession.handleMessage(this.kaMsg);
        for (final Notification m : this.msgsSend) {
            if (m instanceof Pcerr) {
                final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
                assertEquals(new Short((short) 1), obj.getErrorObject().getType());
                assertEquals(new Short((short) 1), obj.getErrorObject().getValue());
            }
        }
    }

    /**
     * KeepWaitTimer expired.
     *
     * @throws Exception
     */
    @Test
    public void testErrorOneSeven() throws Exception {
        this.serverSession.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof Open);
        this.serverSession.handleMessage(this.openMsg);
        Thread.sleep(1000);
        for (final Notification m : this.msgsSend) {
            if (m instanceof Pcerr) {
                final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
                assertEquals(new Short((short) 1), obj.getErrorObject().getType());
                assertEquals(new Short((short) 7), obj.getErrorObject().getValue());
            }
        }
    }

    /************* Tests commented because of their long duration (tested timers) **************/

    /**
     * OpenWait timer expired.
     *
     * @throws InterruptedException
     */
    @Test
    @Ignore
    public void testErrorOneTwo() throws InterruptedException {
        this.serverSession.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof OpenMessage);
        Thread.sleep(60 * 1000);
        for (final Notification m : this.msgsSend) {
            if (m instanceof Pcerr) {
                final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
                assertEquals(new Short((short) 1), obj.getErrorObject().getType());
                assertEquals(new Short((short) 2), obj.getErrorObject().getValue());
            }
        }
    }

    @Test
    @Ignore
    public void testUnknownMessage() throws InterruptedException {
        final SimpleSessionListener client = new SimpleSessionListener();
        final PCEPSessionImpl s = new PCEPSessionImpl(client, 5, this.channel, this.openMsg.getOpenMessage().getOpen(), this.openMsg.getOpenMessage().getOpen());
        s.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(1, s.getUnknownMessagesTimes().size());
        Thread.sleep(10000);
        s.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(2, s.getUnknownMessagesTimes().size());
        Thread.sleep(10000);
        s.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(3, s.getUnknownMessagesTimes().size());
        Thread.sleep(20000);
        s.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(4, s.getUnknownMessagesTimes().size());
        Thread.sleep(30000);
        s.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(3, s.getUnknownMessagesTimes().size());
        Thread.sleep(10000);
        s.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(3, s.getUnknownMessagesTimes().size());
        Thread.sleep(5000);
        s.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(4, s.getUnknownMessagesTimes().size());
        Thread.sleep(1000);
        s.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(5, s.getUnknownMessagesTimes().size());
        Thread.sleep(1000);
        s.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        synchronized (client) {
            while (client.up) {
                client.wait();
            }
        }
        assertTrue(!client.up);
    }

    @After
    public void tearDown() {
    }
}
