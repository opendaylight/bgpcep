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
import static org.opendaylight.protocol.util.CheckUtil.checkEquals;

import com.google.common.base.Ticker;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.Queue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.util.CheckUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.app.config.rev160707.pcep.dispatcher.config.TlsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Starttls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yangtools.yang.binding.Notification;

public class FiniteStateMachineTest extends AbstractPCEPSessionTest {

    private DefaultPCEPSessionNegotiator serverSession;
    private DefaultPCEPSessionNegotiator tlsSessionNegotiator;
    protected final TestTicker ticker = new TestTicker();

    @Before
    public void setup() {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open localPrefs = new OpenBuilder().setKeepalive(
                (short) 1).build();
        this.serverSession = new DefaultPCEPSessionNegotiator(new DefaultPromise<>(GlobalEventExecutor.INSTANCE),
                this.channel, this.listener, (short) 1, 20, localPrefs);
        this.tlsSessionNegotiator = new DefaultPCEPSessionNegotiator(new DefaultPromise<>(GlobalEventExecutor.INSTANCE),
                this.channel, this.listener, (short) 1, 20, localPrefs, new TlsBuilder().build());
    }

    /**
     * Both PCEs accept session characteristics. Also tests KeepAliveTimer and error message and when pce attempts to
     * establish pce session for the 2nd time.
     *
     * @throws Exception exception
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
     * Establish PCEPS TLS connection with peer
     */
    @Test
    public void testEstablishTLS() {
        final DefaultPCEPSessionNegotiator negotiator = new DefaultPCEPSessionNegotiator(new DefaultPromise<>(GlobalEventExecutor.INSTANCE),
                this.channel, this.listener, (short) 1, 20, new OpenBuilder().setKeepalive((short) 1).build(),
                SslContextFactoryTest.createTlsConfig());
        negotiator.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof Starttls);
        assertEquals(DefaultPCEPSessionNegotiator.State.START_TLS_WAIT, negotiator.getState());
        negotiator.handleMessage(this.startTlsMsg);
        assertEquals(DefaultPCEPSessionNegotiator.State.OPEN_WAIT, negotiator.getState());
        assertEquals(2, this.msgsSend.size());
        assertTrue(this.msgsSend.get(1) instanceof Open);
        negotiator.handleMessage(this.openMsg);
        assertEquals(DefaultPCEPSessionNegotiator.State.KEEP_WAIT, negotiator.getState());
    }

    /**
     * As Tls is not configured properly, PCE will send error PCEPErrors.NOT_POSSIBLE_WITHOUT_TLS
     *
     * @throws Exception exception
     */
    @Test
    public void testFailedToEstablishTLS() throws Exception {
        this.tlsSessionNegotiator.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof Starttls);
        assertEquals(DefaultPCEPSessionNegotiator.State.START_TLS_WAIT, this.tlsSessionNegotiator.getState());
        this.tlsSessionNegotiator.handleMessage(this.startTlsMsg);
        assertEquals(2, this.msgsSend.size());
        assertTrue(this.msgsSend.get(1) instanceof Pcerr);
        final Errors obj = ((Pcerr) this.msgsSend.get(1)).getPcerrMessage().getErrors().get(0);
        assertEquals(PCEPErrors.NOT_POSSIBLE_WITHOUT_TLS.getErrorType(), obj.getErrorObject().getType().shortValue());
        assertEquals(PCEPErrors.NOT_POSSIBLE_WITHOUT_TLS.getErrorValue(), obj.getErrorObject().getValue().shortValue());
        assertEquals(DefaultPCEPSessionNegotiator.State.FINISHED, this.tlsSessionNegotiator.getState());
    }

    /**
     * As PCE does not receive expected message (StartTLS), error PCEPErrors.NON_STARTTLS_MSG_RCVD is send
     */
    @Test
    public void testTLSUnexpectedMessage() {
        this.tlsSessionNegotiator.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof Starttls);
        assertEquals(DefaultPCEPSessionNegotiator.State.START_TLS_WAIT, this.tlsSessionNegotiator.getState());
        this.tlsSessionNegotiator.handleMessage(this.openMsg);
        assertEquals(2, this.msgsSend.size());
        assertTrue(this.msgsSend.get(1) instanceof Pcerr);
        final Errors obj = ((Pcerr) this.msgsSend.get(1)).getPcerrMessage().getErrors().get(0);
        assertEquals(PCEPErrors.NON_STARTTLS_MSG_RCVD.getErrorType(), obj.getErrorObject().getType().shortValue());
        assertEquals(PCEPErrors.NON_STARTTLS_MSG_RCVD.getErrorValue(), obj.getErrorObject().getValue().shortValue());
        assertEquals(this.tlsSessionNegotiator.getState(), DefaultPCEPSessionNegotiator.State.FINISHED);
    }

    /**
     * Mock PCE does not accept session characteristics the first time.
     *
     * @throws Exception exception
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
     * @throws Exception exception
     */
    @Test
    public void testErrorOneOne() throws Exception {
        this.serverSession.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof Open);
        this.serverSession.handleMessage(this.kaMsg);
        checkEquals(()-> {
            for (final Notification m : this.msgsSend) {
                if (m instanceof Pcerr) {
                    final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
                    assertEquals(new Short((short) 1), obj.getErrorObject().getType());
                    assertEquals(new Short((short) 1), obj.getErrorObject().getValue());
                }
            }
        });
    }

    /**
     * KeepWaitTimer expired.
     *
     * @throws Exception exception
     */
    @Test
    public void testErrorOneSeven() throws Exception {
        this.serverSession.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof Open);
        this.serverSession.handleMessage(this.openMsg);
        checkEquals(() -> {
            for (final Notification m : this.msgsSend) {
                if (m instanceof Pcerr) {
                    final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
                    assertEquals(new Short((short) 1), obj.getErrorObject().getType());
                    assertEquals(new Short((short) 7), obj.getErrorObject().getValue());
                }
            }
        });
    }

    /**
     * OpenWait timer expired.
     *
     * @throws InterruptedException exception
     */
    @Test
    public void testErrorOneTwo() throws Exception {
        this.serverSession.channelActive(null);
        assertEquals(1, this.msgsSend.size());
        assertTrue(this.msgsSend.get(0) instanceof OpenMessage);
        checkEquals(() -> {
            for (final Notification m : this.msgsSend) {
                if (m instanceof Pcerr) {
                    final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
                    assertEquals(new Short((short) 1), obj.getErrorObject().getType());
                    assertEquals(new Short((short) 2), obj.getErrorObject().getValue());
                }
            }
        });
    }

    /************* Tests commented because of their long duration (tested timers) **************/

    @Test
    public void testUnknownMessage() throws Exception {
        final SimpleSessionListener client = new SimpleSessionListener();
        final PCEPSessionImpl session = new PCEPSessionImpl(client, 5, this.channel,
            this.openMsg.getOpenMessage().getOpen(), this.openMsg.getOpenMessage().getOpen());
        session.setTicker(this.ticker);
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        final Queue<Long> qeue = session.getUnknownMessagesTimes();
        CheckUtil.checkEquals(()-> assertEquals(1, qeue.size()));
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        CheckUtil.checkEquals(()-> assertEquals(2, qeue.size()));
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        CheckUtil.checkEquals(()-> assertEquals(3, qeue.size()));
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        CheckUtil.checkEquals(()-> assertEquals(4, qeue.size()));
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        CheckUtil.checkEquals(()-> assertEquals(3, qeue.size()));
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        CheckUtil.checkEquals(()-> assertEquals(3, qeue.size()));
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        CheckUtil.checkEquals(()-> assertEquals(4, qeue.size()));
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        CheckUtil.checkEquals(()-> assertEquals(5, qeue.size()));
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        synchronized (client) {
            while (client.up) {
                client.wait();
            }
        }
        CheckUtil.checkEquals(()-> assertTrue(!client.up));
    }

    @After
    public void tearDown() {
    }

    private final class TestTicker extends Ticker {
        private long counter = 0L;

        TestTicker() {
        }

        public long read() {
            if (this.counter == 8) {
                this.counter++;
                return 60000000003L;
            } else if (this.counter == 10) {
                this.counter++;
                return 60000000006L;
            }
            return this.counter++;
        }
    }
}
