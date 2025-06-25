/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.error.type.SessionCase;

public class UtilTest {
    private static final Open OPEN = new OpenBuilder().build();

    @Test
    public void testCreateErrorMessageWithOpen() {
        final Pcerr errMsg = Util.createErrorMessage(PCEPErrors.BAD_LABEL_VALUE, OPEN);
        assertThat(errMsg.getPcerrMessage().getErrorType(), isA(SessionCase.class));
        final SessionCase sessionCase = (SessionCase) errMsg.getPcerrMessage().getErrorType();
        assertEquals(OPEN, sessionCase.getSession().getOpen());
        final ErrorObject errorObject = errMsg.getPcerrMessage().getErrors().get(0).getErrorObject();
        assertEquals(PCEPErrors.BAD_LABEL_VALUE.getErrorType(), errorObject.getType());
        assertEquals(PCEPErrors.BAD_LABEL_VALUE.getErrorValue(), errorObject.getValue());
    }

    @Test
    public void testCreateErrorMessage() {
        final Pcerr errMsg = Util.createErrorMessage(PCEPErrors.BAD_LABEL_VALUE, null);
        assertNull(errMsg.getPcerrMessage().getErrorType());
        final ErrorObject errorObject = errMsg.getPcerrMessage().getErrors().get(0).getErrorObject();
        assertEquals(PCEPErrors.BAD_LABEL_VALUE.getErrorType(), errorObject.getType());
        assertEquals(PCEPErrors.BAD_LABEL_VALUE.getErrorValue(), errorObject.getValue());
    }
}
