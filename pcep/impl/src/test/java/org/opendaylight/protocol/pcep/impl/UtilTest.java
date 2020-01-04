/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.SessionCase;

public class UtilTest {
    private static final Open OPEN = new OpenBuilder().build();

    @Test
    public void testCreateErrorMessageWithOpen() {
        final Message msg = Util.createErrorMessage(PCEPErrors.BAD_LABEL_VALUE, OPEN);
        Assert.assertTrue(msg instanceof Pcerr);
        final Pcerr errMsg = (Pcerr) msg;
        Assert.assertTrue(errMsg.getPcerrMessage().getErrorType() instanceof SessionCase);
        final SessionCase sessionCase = (SessionCase) errMsg.getPcerrMessage().getErrorType();
        Assert.assertEquals(OPEN, sessionCase.getSession().getOpen());
        final ErrorObject errorObject = errMsg.getPcerrMessage().getErrors().get(0).getErrorObject();
        Assert.assertEquals(PCEPErrors.BAD_LABEL_VALUE.getErrorType(), errorObject.getType());
        Assert.assertEquals(PCEPErrors.BAD_LABEL_VALUE.getErrorValue(), errorObject.getValue());
    }

    @Test
    public void testCreateErrorMessage() {
        final Message msg = Util.createErrorMessage(PCEPErrors.BAD_LABEL_VALUE, null);
        Assert.assertTrue(msg instanceof Pcerr);
        final Pcerr errMsg = (Pcerr) msg;
        Assert.assertNull(errMsg.getPcerrMessage().getErrorType());
        final ErrorObject errorObject = errMsg.getPcerrMessage().getErrors().get(0).getErrorObject();
        Assert.assertEquals(PCEPErrors.BAD_LABEL_VALUE.getErrorType(), errorObject.getType());
        Assert.assertEquals(PCEPErrors.BAD_LABEL_VALUE.getErrorValue(), errorObject.getValue());
    }
}
