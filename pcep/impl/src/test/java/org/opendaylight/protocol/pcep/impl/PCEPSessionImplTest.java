/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.error.messages.grouping.ErrorMessages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev250930.pcep.session.state.PeerPref;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PCEPSessionImplTest extends AbstractPCEPSessionTest {
    private PCEPSessionImpl session;

    @BeforeEach
    void setup() {
        session = new PCEPSessionImpl(listener, 0, channel, openMsg.getOpenMessage().getOpen(),
            openMsg.getOpenMessage().getOpen());
        session.sessionUp();
    }

    @AfterEach
    void tearDown() {
        session.close();
    }

    @Test
    void testPcepSessionImpl() {
        assertTrue(listener.up);

        session.handleMessage(kaMsg);
        assertEquals(1, session.getMessages().getReceivedMsgCount().intValue());

        session.handleMessage(new PcreqBuilder().build());
        assertEquals(2, session.getMessages().getReceivedMsgCount().intValue());
        assertEquals(1, listener.messages.size());
        assertInstanceOf(Pcreq.class, listener.messages.get(0));
        assertEquals(2, session.getMessages().getReceivedMsgCount().intValue());

        session.handleMessage(closeMsg);
        assertEquals(3, session.getMessages().getReceivedMsgCount().intValue());
        assertEquals(1, listener.messages.size());
        assertTrue(channel.isActive());
        verify(channel).close();
    }

    @Test
    void testAttemptSecondSession() {
        session.handleMessage(openMsg);
        assertEquals(1, session.getMessages().getReceivedMsgCount().intValue());
        assertEquals(1, msgsSend.size());
        final var pcErr = assertInstanceOf(Pcerr.class, msgsSend.get(0));
        final ErrorObject errorObj = pcErr.getPcerrMessage().getErrors().get(0).getErrorObject();
        assertEquals(PCEPErrors.ATTEMPT_2ND_SESSION, PCEPErrors.forValue(errorObj.getType(), errorObj.getValue()));
    }

    @Test
    void testClosedByNode() {
        session.handleMessage(closeMsg);
        verify(channel).close();
    }

    @Test
    void testCapabilityNotSupported() {
        session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        assertEquals(2, msgsSend.size());
        final var pcErr = assertInstanceOf(Pcerr.class, msgsSend.get(0));
        final ErrorObject errorObj = pcErr.getPcerrMessage().getErrors().get(0).getErrorObject();
        assertEquals(PCEPErrors.CAPABILITY_NOT_SUPPORTED, PCEPErrors.forValue(errorObj.getType(), errorObj.getValue()));
        assertEquals(1, session.getMessages().getUnknownMsgReceived().intValue());
        // exceeded max. unknown messages count - terminate session
        final var closeMsg = assertInstanceOf(CloseMessage.class, msgsSend.get(1));

        assertEquals(TerminationReason.TOO_MANY_UNKNOWN_MSGS,
            TerminationReason.forValue(closeMsg.getCCloseMessage().getCClose().getReason()));
        verify(channel).close();
    }

    @Test
    void testEndoOfInput() {
        assertTrue(listener.up);
        session.endOfInput();
        assertFalse(listener.up);
    }

    @Test
    void testCloseSessionWithReason() {
        session.close(TerminationReason.UNKNOWN);
        assertEquals(1, msgsSend.size());
        final var closeMsg = assertInstanceOf(CloseMessage.class, msgsSend.get(0));
        assertEquals(TerminationReason.UNKNOWN,
            TerminationReason.forValue(closeMsg.getCCloseMessage().getCClose().getReason()));
        verify(channel).close();
    }

    @Test
    void testSessionStatistics() {
        session.handleMessage(Util.createErrorMessage(PCEPErrors.LSP_RSVP_ERROR, null));
        assertEquals(ipAddress, session.getPeerPref().getIpAddress());
        final PeerPref peerPref = session.getPeerPref();
        assertEquals(ipAddress, peerPref.getIpAddress());
        assertEquals(DEADTIMER, peerPref.getDeadtimer());
        assertEquals(KEEP_ALIVE, peerPref.getKeepalive());
        assertEquals(0, peerPref.getSessionId().intValue());
        final LocalPref localPref = session.getLocalPref();
        assertEquals(ipAddress, localPref.getIpAddress());
        assertEquals(DEADTIMER, localPref.getDeadtimer());
        assertEquals(KEEP_ALIVE, localPref.getKeepalive());
        assertEquals(0, localPref.getSessionId().intValue());
        final Messages msgs = session.getMessages();
        assertEquals(1, msgs.getReceivedMsgCount().longValue());
        assertEquals(0, msgs.getSentMsgCount().longValue());
        assertEquals(0, msgs.getUnknownMsgReceived().longValue());
        final ErrorMessages errMsgs = msgs.getErrorMessages();
        assertEquals(1, errMsgs.getReceivedErrorMsgCount().intValue());
        assertEquals(0, errMsgs.getSentErrorMsgCount().intValue());
        assertEquals(PCEPErrors.LSP_RSVP_ERROR.getErrorType(), errMsgs.getLastReceivedError().getErrorType());
        assertEquals(PCEPErrors.LSP_RSVP_ERROR.getErrorValue(), errMsgs.getLastReceivedError().getErrorValue());

        session.sendMessage(Util.createErrorMessage(PCEPErrors.UNKNOWN_PLSP_ID, null));
        final Messages msgs2 = session.getMessages();
        assertEquals(1, msgs2.getReceivedMsgCount().longValue());
        assertEquals(1, msgs2.getSentMsgCount().longValue());
        assertEquals(0, msgs2.getUnknownMsgReceived().longValue());
        final ErrorMessages errMsgs2 = msgs2.getErrorMessages();
        assertEquals(1, errMsgs2.getReceivedErrorMsgCount().intValue());
        assertEquals(1, errMsgs2.getSentErrorMsgCount().intValue());
        assertEquals(PCEPErrors.UNKNOWN_PLSP_ID.getErrorType(), errMsgs2.getLastSentError().getErrorType());
        assertEquals(PCEPErrors.UNKNOWN_PLSP_ID.getErrorValue(), errMsgs2.getLastSentError().getErrorValue());
    }

    @Test
    void testExceptionCaught() {
        assertFalse(session.isClosed());
        assertTrue(listener.up);
        session.exceptionCaught(null, new Throwable("PCEP exception."));
        assertFalse(listener.up);
        assertTrue(session.isClosed());
    }

    @Test
    void testSessionRecoveryOnException() {
        listener = new SimpleExceptionSessionListener();
        session = spy(new PCEPSessionImpl(listener, 0, channel, openMsg.getOpenMessage().getOpen(),
            openMsg.getOpenMessage().getOpen()));
        verify(session, never()).handleException(any());
        verify(session, never()).sendMessage(any());
        verify(session, never()).closeChannel();

        final var ex = assertThrows(UnsupportedOperationException.class, session::sessionUp);
        assertEquals("Mocked runtime exception.", ex.getMessage());

        assertFalse(listener.up);
        verify(session).handleException(any());
        verify(session).sendMessage(any(CloseMessage.class));
        verify(session).closeChannel();
    }

    private static final class SimpleExceptionSessionListener extends SimpleSessionListener {
        @Override
        public synchronized void onSessionUp(final PCEPSession session) {
            super.onSessionUp(session);
            throw new UnsupportedOperationException("Mocked runtime exception.");
        }
    }
}
