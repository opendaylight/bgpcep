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
import org.mockito.Mockito;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcreqBuilder;
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
        this.session.tearDown();
    }

    @Test
    public void testPcepSessionImpl() throws InterruptedException {
        Assert.assertTrue(this.listener.up);

        Assert.assertEquals(45, this.session.getDeadTimerValue().intValue());
        Assert.assertEquals(15, this.session.getKeepAliveTimerValue().intValue());
        this.session.handleMessage(this.kaMsg);
        Assert.assertEquals(1, this.session.getReceivedMsgCount().intValue());

        this.session.handleMessage(new PcreqBuilder().build());
        Assert.assertEquals(2, this.session.getReceivedMsgCount().intValue());
        Assert.assertEquals(1, this.listener.messages.size());
        Assert.assertTrue(this.listener.messages.get(0) instanceof Pcreq);

        this.session.handleMessage(new CloseBuilder().build());
        Assert.assertEquals(3, this.session.getReceivedMsgCount().intValue());
        Assert.assertEquals(1, this.listener.messages.size());
        Assert.assertTrue(this.channel.isActive());
        Mockito.verify(this.channel, Mockito.times(1)).close();
    }

    @Test
    public void testAttemptSecondSession() {
        this.session.handleMessage(this.openMsg);
        Assert.assertEquals(1, this.session.getReceivedMsgCount().intValue());
        Assert.assertEquals(1, this.msgsSend.size());
        Assert.assertTrue(this.msgsSend.get(0) instanceof Pcerr);
        final Pcerr pcErr = (Pcerr) this.msgsSend.get(0);
        final ErrorObject errorObj = pcErr.getPcerrMessage().getErrors().get(0).getErrorObject();
        Assert.assertEquals(PCEPErrors.ATTEMPT_2ND_SESSION, PCEPErrors.forValue(errorObj.getType(), errorObj.getValue()));
    }

    @Test
    public void testCapabilityNotSupported() {
        this.session.handleMalformedMessage(PCEPErrors.CAPABILITY_NOT_SUPPORTED);
        Assert.assertEquals(2, this.msgsSend.size());
        Assert.assertTrue(this.msgsSend.get(0) instanceof Pcerr);
        final Pcerr pcErr = (Pcerr) this.msgsSend.get(0);
        final ErrorObject errorObj = pcErr.getPcerrMessage().getErrors().get(0).getErrorObject();
        Assert.assertEquals(PCEPErrors.CAPABILITY_NOT_SUPPORTED, PCEPErrors.forValue(errorObj.getType(), errorObj.getValue()));
        Assert.assertEquals(1, this.session.getUnknownMessagesTimes().size());
        // exceeded max. unknown messages count - terminate session
        Assert.assertTrue(this.msgsSend.get(1) instanceof CloseMessage);
        final CloseMessage closeMsg = (CloseMessage) this.msgsSend.get(1);
        Assert.assertEquals(TerminationReason.TooManyUnknownMsg, TerminationReason.forValue(closeMsg.getCCloseMessage().getCClose().getReason()));
        Mockito.verify(this.channel, Mockito.times(1)).close();
    }

    @Test
    public void testEndoOfInput() {
        Assert.assertTrue(this.listener.up);
        this.session.endOfInput();
        Assert.assertFalse(this.listener.up);
    }

    @Test
    public void voidTestCloseSessionWithReason() {
        this.session.close(TerminationReason.Unknown);
        Assert.assertEquals(1, this.msgsSend.size());
        Assert.assertTrue(this.msgsSend.get(0) instanceof CloseMessage);
        final CloseMessage closeMsg = (CloseMessage) this.msgsSend.get(0);
        Assert.assertEquals(TerminationReason.Unknown, TerminationReason.forValue(closeMsg.getCCloseMessage().getCClose().getReason()));
        Mockito.verify(this.channel, Mockito.times(1)).close();
    }
}
