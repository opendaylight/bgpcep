/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.spi;

import java.util.Collections;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;

/**
 * Utilities used in pcep-impl
 */
public final class Util {

    private Util() {
        throw new UnsupportedOperationException();
    }

    public static Message createErrorMessage(final PCEPErrors e, final Open t) {
        final PcerrBuilder errMessageBuilder = new PcerrBuilder();
        final ErrorObject err = new ErrorObjectBuilder().setType(e.getErrorType()).setValue(e.getErrorValue()).build();
        if (t == null) {
            return errMessageBuilder.setPcerrMessage(
                    new PcerrMessageBuilder().setErrors(Collections.singletonList(new ErrorsBuilder().setErrorObject(err).build())).build()).build();
        } else {
            final ErrorType type = new SessionCaseBuilder().setSession(new SessionBuilder().setOpen(t).build()).build();
            return errMessageBuilder.setPcerrMessage(
                    new PcerrMessageBuilder().setErrors(Collections.singletonList(new ErrorsBuilder().setErrorObject(err).build())).setErrorType(type).build()).build();
        }
    }
}
