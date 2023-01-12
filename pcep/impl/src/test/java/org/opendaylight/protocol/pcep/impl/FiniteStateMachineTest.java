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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.protocol.util.CheckTestUtil.checkEquals;

import com.google.common.base.Ticker;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.PcepSessionErrorPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.pcep.config.session.config.TlsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Keepalive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Starttls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.OpenMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint8;

public class FiniteStateMachineTest extends AbstractPCEPSessionTest {
    private PcepSessionErrorPolicy errorPolicy;
    private DefaultPCEPSessionNegotiator serverSession;
    private DefaultPCEPSessionNegotiator tlsSessionNegotiator;

    @Before
    public void setup() {
        doReturn(Uint16.valueOf(20)).when(errorPolicy).requireMaxUnknownMessages();

        final var localPrefs = new OpenBuilder().setKeepalive(Uint8.ONE).build();
        serverSession = new DefaultPCEPSessionNegotiator(new DefaultPromise<>(GlobalEventExecutor.INSTANCE),
                channel, listener, Uint8.ONE, localPrefs, errorPolicy);
        tlsSessionNegotiator = new DefaultPCEPSessionNegotiator(new DefaultPromise<>(GlobalEventExecutor.INSTANCE),
                channel, listener, Uint8.ONE, localPrefs, errorPolicy, new TlsBuilder().build());
    }

    /**
     * Both PCEs accept session characteristics. Also tests KeepAliveTimer and error message and when pce attempts to
     * establish pce session for the 2nd time.
     */
    @Test
    public void testSessionCharsAccBoth() {
        serverSession.channelActive(null);
        assertEquals(1, msgsSend.size());
        assertTrue(msgsSend.get(0) instanceof Open);
        serverSession.handleMessage(openMsg);
        assertEquals(2, msgsSend.size());
        assertTrue(msgsSend.get(1) instanceof Keepalive);
        serverSession.handleMessage(kaMsg);
        assertEquals(serverSession.getState(), DefaultPCEPSessionNegotiator.State.FINISHED);
    }

    /**
     * Establish PCEPS TLS connection with peer.
     */
    @Test
    public void testEstablishTLS() {
        final DefaultPCEPSessionNegotiator negotiator =
            new DefaultPCEPSessionNegotiator(new DefaultPromise<>(GlobalEventExecutor.INSTANCE),
                channel, listener, Uint8.ONE, new OpenBuilder().setKeepalive(Uint8.ONE).build(),
                errorPolicy, SslContextFactoryTest.createTlsConfig());
        negotiator.channelActive(null);
        assertEquals(1, msgsSend.size());
        assertTrue(msgsSend.get(0) instanceof Starttls);
        assertEquals(DefaultPCEPSessionNegotiator.State.START_TLS_WAIT, negotiator.getState());
        negotiator.handleMessage(startTlsMsg);
        assertEquals(DefaultPCEPSessionNegotiator.State.OPEN_WAIT, negotiator.getState());
        assertEquals(2, msgsSend.size());
        assertTrue(msgsSend.get(1) instanceof Open);
        negotiator.handleMessage(openMsg);
        assertEquals(DefaultPCEPSessionNegotiator.State.KEEP_WAIT, negotiator.getState());
    }

    /**
     * As Tls is not configured properly, PCE will send error PCEPErrors.NOT_POSSIBLE_WITHOUT_TLS.
     */
    @Test
    public void testFailedToEstablishTLS() {
        tlsSessionNegotiator.channelActive(null);
        assertEquals(1, msgsSend.size());
        assertTrue(msgsSend.get(0) instanceof Starttls);
        assertEquals(DefaultPCEPSessionNegotiator.State.START_TLS_WAIT, tlsSessionNegotiator.getState());
        tlsSessionNegotiator.handleMessage(startTlsMsg);
        assertEquals(2, msgsSend.size());
        assertTrue(msgsSend.get(1) instanceof Pcerr);
        final Errors obj = ((Pcerr) msgsSend.get(1)).getPcerrMessage().getErrors().get(0);
        assertEquals(PCEPErrors.NOT_POSSIBLE_WITHOUT_TLS.getErrorType(), obj.getErrorObject().getType());
        assertEquals(PCEPErrors.NOT_POSSIBLE_WITHOUT_TLS.getErrorValue(), obj.getErrorObject().getValue());
        assertEquals(DefaultPCEPSessionNegotiator.State.FINISHED, tlsSessionNegotiator.getState());
    }

    /**
     * As PCE does not receive expected message (StartTLS), error PCEPErrors.NON_STARTTLS_MSG_RCVD is send.
     */
    @Test
    public void testTLSUnexpectedMessage() {
        tlsSessionNegotiator.channelActive(null);
        assertEquals(1, msgsSend.size());
        assertTrue(msgsSend.get(0) instanceof Starttls);
        assertEquals(DefaultPCEPSessionNegotiator.State.START_TLS_WAIT, tlsSessionNegotiator.getState());
        tlsSessionNegotiator.handleMessage(openMsg);
        assertEquals(2, msgsSend.size());
        assertTrue(msgsSend.get(1) instanceof Pcerr);
        final Errors obj = ((Pcerr) msgsSend.get(1)).getPcerrMessage().getErrors().get(0);
        assertEquals(PCEPErrors.NON_STARTTLS_MSG_RCVD.getErrorType(), obj.getErrorObject().getType());
        assertEquals(PCEPErrors.NON_STARTTLS_MSG_RCVD.getErrorValue(), obj.getErrorObject().getValue());
        assertEquals(tlsSessionNegotiator.getState(), DefaultPCEPSessionNegotiator.State.FINISHED);
    }

    /**
     * Mock PCE does not accept session characteristics the first time.
     */
    @Test
    public void testSessionCharsAccMe() {
        serverSession.channelActive(null);
        assertEquals(1, msgsSend.size());
        assertTrue(msgsSend.get(0) instanceof Open);
        final Open remote = (Open) msgsSend.get(0);
        serverSession.handleMessage(openMsg);
        assertEquals(2, msgsSend.size());
        assertTrue(msgsSend.get(1) instanceof Keepalive);
        serverSession.handleMessage(Util.createErrorMessage(PCEPErrors.NON_ACC_NEG_SESSION_CHAR,
            remote.getOpenMessage().getOpen()));
        assertEquals(3, msgsSend.size());
        assertTrue(msgsSend.get(2) instanceof Open);
        serverSession.handleMessage(kaMsg);
        assertEquals(serverSession.getState(), DefaultPCEPSessionNegotiator.State.FINISHED);
    }

    /**
     * Sending different PCEP Message than Open in session establishment phase.
     *
     * @throws Exception exception
     */
    @Test
    public void testErrorOneOne() throws Exception {
        serverSession.channelActive(null);
        assertEquals(1, msgsSend.size());
        assertTrue(msgsSend.get(0) instanceof Open);
        serverSession.handleMessage(kaMsg);
        checkEquals(() -> {
            for (final Notification<?> m : msgsSend) {
                if (m instanceof Pcerr) {
                    final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
                    assertEquals(Uint8.ONE, obj.getErrorObject().getType());
                    assertEquals(Uint8.ONE, obj.getErrorObject().getValue());
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
        serverSession.channelActive(null);
        assertEquals(1, msgsSend.size());
        assertTrue(msgsSend.get(0) instanceof Open);
        serverSession.handleMessage(openMsg);
        checkEquals(() -> {
            for (final Notification<?> m : msgsSend) {
                if (m instanceof Pcerr) {
                    final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
                    assertEquals(Uint8.ONE, obj.getErrorObject().getType());
                    assertEquals(Uint8.valueOf(7), obj.getErrorObject().getValue());
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
        serverSession.channelActive(null);
        assertEquals(1, msgsSend.size());
        assertTrue(msgsSend.get(0) instanceof OpenMessage);
        checkEquals(() -> {
            for (final Notification<?> m : msgsSend) {
                if (m instanceof Pcerr) {
                    final Errors obj = ((Pcerr) m).getPcerrMessage().getErrors().get(0);
                    assertEquals(Uint8.ONE, obj.getErrorObject().getType());
                    assertEquals(Uint8.TWO, obj.getErrorObject().getValue());
                }
            }
        });
    }

    @Test
    public void testUnknownMessage() {
        final Ticker ticker = mock(Ticker.class);
        doReturn(
            // session create
            0L,
            // first four receive/send
            1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L,
            // a minute has passed since second receive/send
            60000000004L, 60000000005L,
            // a minute has passed since third receive/send and the the time gets stuck :)
            60000000006L, 60000000007L).when(ticker).read();

        final var listener = mock(PCEPSessionListener.class);
        final var session = new PCEPSessionImpl(listener, 5, channel, openMsg.getOpenMessage().getOpen(),
            openMsg.getOpenMessage().getOpen(), ticker);

        final var qeue = session.getUnknownMessagesTimes();
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(1, qeue.size());
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(2, qeue.size());
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(3, qeue.size());
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(4, qeue.size());
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(3, qeue.size());
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(3, qeue.size());
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(4, qeue.size());
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(5, qeue.size());

        final var captor = ArgumentCaptor.forClass(PCEPTerminationReason.class);
        doNothing().when(listener).onSessionTerminated(eq(session), captor.capture());
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        verify(ticker, times(20)).read();
        assertEquals("TOO_MANY_UNKNOWN_MSGS", captor.getValue().getErrorMessage());
    }
}
