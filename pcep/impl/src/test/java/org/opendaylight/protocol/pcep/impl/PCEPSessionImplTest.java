/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.impl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.error.messages.grouping.ErrorMessages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.CloseMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;

public class PCEPSessionImplTest extends AbstractPCEPSessionTest {

    private PCEPSessionImpl session;

    @Before
    public void setup() {
        this.session = new PCEPSessionImpl(this.listener, 0, this.channel, this.openMsg.getOpenMessage().getOpen(), this.openMsg.getOpenMessage().getOpen());
        this.session.sessionUp();
    }

    @After
    public void tearDown() {
        this.session.close();
    }

    @Test
    public void testPcepSessionImpl() throws InterruptedException {
        Assert.assertTrue(this.listener.up);

        this.session.handleMessage(this.kaMsg);
        Assert.assertEquals(1, this.session.getMessages().getReceivedMsgCount().intValue());

        this.session.handleMessage(new PcreqBuilder().build());
        Assert.assertEquals(2, this.session.getMessages().getReceivedMsgCount().intValue());
        Assert.assertEquals(1, this.listener.messages.size());
        Assert.assertTrue(this.listener.messages.get(0) instanceof Pcreq);
        Assert.assertEquals(2, this.session.getMessages().getReceivedMsgCount().intValue());

        this.session.handleMessage(this.closeMsg);
        Assert.assertEquals(3, this.session.getMessages().getReceivedMsgCount().intValue());
        Assert.assertEquals(1, this.listener.messages.size());
        Assert.assertTrue(this.channel.isActive());
        Mockito.verify(this.channel, Mockito.times(1)).close();
    }

    @Test
    public void testAttemptSecondSession() {
        this.session.handleMessage(this.openMsg);
        Assert.assertEquals(1, this.session.getMessages().getReceivedMsgCount().intValue());
        Assert.assertEquals(1, this.msgsSend.size());
        Assert.assertTrue(this.msgsSend.get(0) instanceof Pcerr);
        final Pcerr pcErr = (Pcerr) this.msgsSend.get(0);
        final ErrorObject errorObj = pcErr.getPcerrMessage().getErrors().get(0).getErrorObject();
        Assert.assertEquals(PCEPErrors.ATTEMPT_2ND_SESSION, PCEPErrors.forValue(errorObj.getType(), errorObj.getValue()));
    }

    @Test
    public void testClosedByNode() {
        this.session.handleMessage(this.closeMsg);
        Mockito.verify(this.channel).close();
    }

    @Test
    public void testCapabilityNotSupported() {
        this.session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        Assert.assertEquals(2, this.msgsSend.size());
        Assert.assertTrue(this.msgsSend.get(0) instanceof Pcerr);
        final Pcerr pcErr = (Pcerr) this.msgsSend.get(0);
        final ErrorObject errorObj = pcErr.getPcerrMessage().getErrors().get(0).getErrorObject();
        Assert.assertEquals(PCEPErrors.CAPABILITY_NOT_SUPPORTED, PCEPErrors.forValue(errorObj.getType(), errorObj.getValue()));
        Assert.assertEquals(1, this.session.getMessages().getUnknownMsgReceived().intValue());
        // exceeded max. unknown messages count - terminate session
        Assert.assertTrue(this.msgsSend.get(1) instanceof CloseMessage);
        final CloseMessage closeMsg = (CloseMessage) this.msgsSend.get(1);
        Assert.assertEquals(TerminationReason.TOO_MANY_UNKNOWN_MSGS, TerminationReason.forValue(closeMsg.getCCloseMessage().getCClose().getReason()));
        Mockito.verify(this.channel, Mockito.times(1)).close();
    }

    @Test
    public void testEndoOfInput() {
        Assert.assertTrue(this.listener.up);
        this.session.endOfInput();
        Assert.assertFalse(this.listener.up);
    }

    @Test
    public void testCloseSessionWithReason() {
        this.session.close(TerminationReason.UNKNOWN);
        Assert.assertEquals(1, this.msgsSend.size());
        Assert.assertTrue(this.msgsSend.get(0) instanceof CloseMessage);
        final CloseMessage closeMsg = (CloseMessage) this.msgsSend.get(0);
        Assert.assertEquals(TerminationReason.UNKNOWN, TerminationReason.forValue(closeMsg.getCCloseMessage().getCClose().getReason()));
        Mockito.verify(this.channel, Mockito.times(1)).close();
    }

    @Test
    public void testSessionStatistics() {
        this.session.handleMessage(Util.createErrorMessage(PCEPErrors.LSP_RSVP_ERROR, null));
        Assert.assertEquals(this.ipAddress, this.session.getPeerPref().getIpAddress());
        final PeerPref peerPref = this.session.getPeerPref();
        Assert.assertEquals(this.ipAddress, peerPref.getIpAddress());
        Assert.assertEquals(DEADTIMER, peerPref.getDeadtimer().shortValue());
        Assert.assertEquals(KEEP_ALIVE, peerPref.getKeepalive().shortValue());
        Assert.assertEquals(0, peerPref.getSessionId().intValue());
        final LocalPref localPref = this.session.getLocalPref();
        Assert.assertEquals(this.ipAddress, localPref.getIpAddress());
        Assert.assertEquals(DEADTIMER, localPref.getDeadtimer().shortValue());
        Assert.assertEquals(KEEP_ALIVE, localPref.getKeepalive().shortValue());
        Assert.assertEquals(0, localPref.getSessionId().intValue());
        final Messages msgs = this.session.getMessages();
        Assert.assertEquals(1, msgs.getReceivedMsgCount().longValue());
        Assert.assertEquals(0, msgs.getSentMsgCount().longValue());
        Assert.assertEquals(0, msgs.getUnknownMsgReceived().longValue());
        final ErrorMessages errMsgs = msgs.getErrorMessages();
        Assert.assertEquals(1, errMsgs.getReceivedErrorMsgCount().intValue());
        Assert.assertEquals(0, errMsgs.getSentErrorMsgCount().intValue());
        Assert.assertEquals(PCEPErrors.LSP_RSVP_ERROR.getErrorType(), errMsgs.getLastReceivedError().getErrorType().shortValue());
        Assert.assertEquals(PCEPErrors.LSP_RSVP_ERROR.getErrorValue(), errMsgs.getLastReceivedError().getErrorValue().shortValue());

        this.session.sendMessage(Util.createErrorMessage(PCEPErrors.UNKNOWN_PLSP_ID, null));
        final Messages msgs2 = this.session.getMessages();
        Assert.assertEquals(1, msgs2.getReceivedMsgCount().longValue());
        Assert.assertEquals(1, msgs2.getSentMsgCount().longValue());
        Assert.assertEquals(0, msgs2.getUnknownMsgReceived().longValue());
        final ErrorMessages errMsgs2 = msgs2.getErrorMessages();
        Assert.assertEquals(1, errMsgs2.getReceivedErrorMsgCount().intValue());
        Assert.assertEquals(1, errMsgs2.getSentErrorMsgCount().intValue());
        Assert.assertEquals(PCEPErrors.UNKNOWN_PLSP_ID.getErrorType(), errMsgs2.getLastSentError().getErrorType().shortValue());
        Assert.assertEquals(PCEPErrors.UNKNOWN_PLSP_ID.getErrorValue(), errMsgs2.getLastSentError().getErrorValue().shortValue());
    }

    @Test
    public void testExceptionCaught() throws Exception {
        Assert.assertFalse(this.session.isClosed());
        Assert.assertTrue(this.listener.up);
        this.session.exceptionCaught(null, new Throwable("PCEP exception."));
        Assert.assertFalse(this.listener.up);
        Assert.assertTrue(this.session.isClosed());
    }

    @Test
    public void testSessionRecoveryOnException() throws Exception {
        this.listener = new SimpleExceptionSessionListener();
        this.session = Mockito.spy(new PCEPSessionImpl(this.listener, 0, this.channel,
                this.openMsg.getOpenMessage().getOpen(), this.openMsg.getOpenMessage().getOpen()));
        Mockito.verify(this.session, Mockito.never()).handleException(Matchers.any());
        Mockito.verify(this.session, Mockito.never()).sendMessage(Matchers.any());
        Mockito.verify(this.session, Mockito.never()).closeChannel();
        try {
            this.session.sessionUp();
            Assert.fail();  // expect the exception to be populated
        } catch (final RuntimeException ignored) {}
        Assert.assertFalse(this.listener.up);
        Mockito.verify(this.session).handleException(Matchers.any());
        Mockito.verify(this.session).sendMessage(Matchers.any(CloseMessage.class));
        Mockito.verify(this.session).closeChannel();
    }

    private static class SimpleExceptionSessionListener extends SimpleSessionListener {
        @Override
        public synchronized void onSessionUp(final PCEPSession session) {
            super.onSessionUp(session);
            throw new RuntimeException("Mocked runtime exception.");
        }
    }
}
